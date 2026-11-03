package com.example.ember.update

import android.app.Activity
import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import androidx.core.content.FileProvider
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import java.security.MessageDigest
import kotlin.coroutines.resume

/**
 * Custom APK Update Manager for EMBER Music Application.
 * Distributed directly outside Google Play Store.
 * 
 * Capabilities:
 * - Firebase Remote Config update discovery.
 * - Version code and name comparison.
 * - Native Android DownloadManager with live progress telemetry.
 * - Cryptographic APK signing certificate verification before installation.
 * - Secure FileProvider and Package Installer intent dispatch.
 */
object EmberUpdateManager {

    private const val TAG = "EmberUpdateManager"

    // Firebase Remote Config Keys
    const val KEY_LATEST_VERSION_CODE = "latest_version_code"
    const val KEY_LATEST_VERSION_NAME = "latest_version_name"
    const val KEY_APK_DOWNLOAD_URL = "apk_download_url"
    const val KEY_RELEASE_NOTES = "release_notes"
    const val KEY_FORCE_UPDATE = "force_update"

    // Fallback release repository on GitHub
    private const val DEFAULT_RELEASE_APK_URL =
        "https://github.com/krvijender29/ember-music-app/raw/main/release/app-debug.apk"

    data class UpdateInfo(
        val latestVersionCode: Long,
        val latestVersionName: String,
        val apkDownloadUrl: String,
        val releaseNotes: String,
        val isForceUpdate: Boolean,
        val currentVersionCode: Long,
        val currentVersionName: String,
        val isUpdateAvailable: Boolean
    )

    sealed class UpdateCheckResult {
        data class Available(val updateInfo: UpdateInfo) : UpdateCheckResult()
        data class UpToDate(val updateInfo: UpdateInfo) : UpdateCheckResult()
        data class Error(val message: String) : UpdateCheckResult()
    }

    sealed class DownloadState {
        object Idle : DownloadState()
        object Starting : DownloadState()
        data class Progress(val percent: Int, val bytesDownloaded: Long, val totalBytes: Long) : DownloadState()
        data class Completed(val apkFile: File, val updateInfo: UpdateInfo) : DownloadState()
        data class VerificationFailed(val reason: String) : DownloadState()
        data class Failed(val reason: String) : DownloadState()
    }

    private var currentDownloadId: Long = -1L
    private var progressPollingJob: Job? = null
    private var downloadReceiverRegistered = false
    private var currentPendingUpdateInfo: UpdateInfo? = null
    private var currentDownloadFile: File? = null

    // Listeners
    private val downloadStateListeners = mutableListOf<(DownloadState) -> Unit>()

    fun addDownloadListener(listener: (DownloadState) -> Unit) {
        if (!downloadStateListeners.contains(listener)) {
            downloadStateListeners.add(listener)
        }
    }

    fun removeDownloadListener(listener: (DownloadState) -> Unit) {
        downloadStateListeners.remove(listener)
    }

    private fun notifyDownloadState(state: DownloadState) {
        Handler(Looper.getMainLooper()).post {
            downloadStateListeners.forEach { it.invoke(state) }
        }
    }

    /**
     * Retrieves the installed app's versionCode across all Android versions.
     */
    fun getCurrentVersionCode(context: Context): Long {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo.longVersionCode
            } else {
                @Suppress("DEPRECATION")
                packageInfo.versionCode.toLong()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching installed versionCode: ${e.message}")
            1L
        }
    }

    /**
     * Retrieves the installed app's versionName.
     */
    fun getCurrentVersionName(context: Context): String {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            packageInfo.versionName ?: "2.4.5"
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching installed versionName: ${e.message}")
            "2.4.5"
        }
    }

    /**
     * Fetches update configuration from Firebase Remote Config with fallback to defaults.
     */
    suspend fun checkForUpdates(context: Context): UpdateCheckResult = withContext(Dispatchers.IO) {
        val currentCode = getCurrentVersionCode(context)
        val currentName = getCurrentVersionName(context)

        try {
            val remoteConfig = FirebaseRemoteConfig.getInstance()
            val configSettings = FirebaseRemoteConfigSettings.Builder()
                .setMinimumFetchIntervalInSeconds(0) // 0s on manual check for fresh fetch
                .setFetchTimeoutInSeconds(10)
                .build()
            remoteConfig.setConfigSettingsAsync(configSettings)

            // Setup local defaults
            val defaults = mapOf(
                KEY_LATEST_VERSION_CODE to currentCode,
                KEY_LATEST_VERSION_NAME to currentName,
                KEY_APK_DOWNLOAD_URL to DEFAULT_RELEASE_APK_URL,
                KEY_RELEASE_NOTES to "• Performance enhancements & smooth playback\n• Audio engine stability fixes\n• UI polished with obsidian-noir precision",
                KEY_FORCE_UPDATE to false
            )
            remoteConfig.setDefaultsAsync(defaults)

            // Fetch and activate
            try {
                suspendCancellableCoroutine<Boolean> { cont ->
                    remoteConfig.fetchAndActivate()
                        .addOnCompleteListener { task ->
                            if (cont.isActive) {
                                cont.resume(task.isSuccessful)
                            }
                        }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Firebase Remote Config fetch failed, falling back to cached/default: ${e.message}")
            }

            var latestCode = remoteConfig.getLong(KEY_LATEST_VERSION_CODE)
            var latestName = remoteConfig.getString(KEY_LATEST_VERSION_NAME)
            var apkUrl = remoteConfig.getString(KEY_APK_DOWNLOAD_URL)
            var releaseNotes = remoteConfig.getString(KEY_RELEASE_NOTES)
            val isForceUpdate = remoteConfig.getBoolean(KEY_FORCE_UPDATE)

            if (latestCode <= 0) latestCode = currentCode
            if (latestName.isNullOrBlank()) latestName = currentName
            if (apkUrl.isNullOrBlank()) apkUrl = DEFAULT_RELEASE_APK_URL
            if (releaseNotes.isNullOrBlank()) {
                releaseNotes = "• Bug fixes and general performance improvements."
            }

            val isAvailable = latestCode > currentCode

            val info = UpdateInfo(
                latestVersionCode = latestCode,
                latestVersionName = latestName,
                apkDownloadUrl = apkUrl,
                releaseNotes = releaseNotes,
                isForceUpdate = isForceUpdate,
                currentVersionCode = currentCode,
                currentVersionName = currentName,
                isUpdateAvailable = isAvailable
            )

            currentPendingUpdateInfo = info

            if (isAvailable) {
                UpdateCheckResult.Available(info)
            } else {
                UpdateCheckResult.UpToDate(info)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Check for updates encounter error: ${e.message}", e)
            UpdateCheckResult.Error("Unable to check for updates: ${e.localizedMessage ?: "Network error"}")
        }
    }

    /**
     * Starts APK download via native Android DownloadManager and registers live progress tracking.
     */
    fun startApkDownload(
        context: Context,
        updateInfo: UpdateInfo,
        scope: CoroutineScope
    ) {
        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as? DownloadManager
        if (downloadManager == null) {
            notifyDownloadState(DownloadState.Failed("Download Manager service is unavailable on this device."))
            return
        }

        try {
            val apkUri = Uri.parse(updateInfo.apkDownloadUrl)
            
            // App-specific external downloads directory (Requires NO storage permissions on Android 10-15+)
            val downloadsDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
            if (downloadsDir != null && !downloadsDir.exists()) {
                downloadsDir.mkdirs()
            }
            val destinationFile = File(downloadsDir, "EMBER_v${updateInfo.latestVersionName}_build${updateInfo.latestVersionCode}.apk")
            if (destinationFile.exists()) {
                destinationFile.delete()
            }

            currentDownloadFile = destinationFile
            currentPendingUpdateInfo = updateInfo

            val request = DownloadManager.Request(apkUri).apply {
                setTitle("EMBER Music v${updateInfo.latestVersionName}")
                setDescription("Downloading latest EMBER update...")
                setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                setDestinationUri(Uri.fromFile(destinationFile))
                setAllowedOverMetered(true)
                setAllowedOverRoaming(true)
                setMimeType("application/vnd.android.package-archive")
            }

            val downloadId = downloadManager.enqueue(request)
            currentDownloadId = downloadId
            notifyDownloadState(DownloadState.Starting)

            registerDownloadCompleteReceiver(context)

            // Start live progress polling
            progressPollingJob?.cancel()
            progressPollingJob = scope.launch(Dispatchers.IO) {
                var isDownloading = true
                while (isDownloading && isActive) {
                    val query = DownloadManager.Query().setFilterById(downloadId)
                    var cursor: Cursor? = null
                    try {
                        cursor = downloadManager.query(query)
                        if (cursor != null && cursor.moveToFirst()) {
                            val bytesSoFar = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
                            val totalBytes = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
                            val status = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))

                            when (status) {
                                DownloadManager.STATUS_RUNNING, DownloadManager.STATUS_PAUSED, DownloadManager.STATUS_PENDING -> {
                                    val percent = if (totalBytes > 0) ((bytesSoFar * 100) / totalBytes).toInt() else 0
                                    notifyDownloadState(DownloadState.Progress(percent, bytesSoFar, totalBytes))
                                }
                                DownloadManager.STATUS_SUCCESSFUL -> {
                                    isDownloading = false
                                    notifyDownloadState(DownloadState.Progress(100, totalBytes, totalBytes))
                                    handleDownloadSuccess(context, destinationFile, updateInfo)
                                }
                                DownloadManager.STATUS_FAILED -> {
                                    isDownloading = false
                                    val reason = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_REASON))
                                    val errorMsg = mapDownloadErrorReason(reason)
                                    notifyDownloadState(DownloadState.Failed(errorMsg))
                                }
                            }
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Error querying download progress: ${e.message}")
                    } finally {
                        cursor?.close()
                    }
                    delay(350)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start APK download: ${e.message}", e)
            notifyDownloadState(DownloadState.Failed("Download failed to initiate: ${e.localizedMessage}"))
        }
    }

    private fun mapDownloadErrorReason(reason: Int): String {
        return when (reason) {
            DownloadManager.ERROR_CANNOT_RESUME -> "Download connection interrupted and cannot be resumed."
            DownloadManager.ERROR_DEVICE_NOT_FOUND -> "No external storage detected."
            DownloadManager.ERROR_FILE_ALREADY_EXISTS -> "Destination file already exists."
            DownloadManager.ERROR_FILE_ERROR -> "File system I/O error encountered."
            DownloadManager.ERROR_HTTP_DATA_ERROR -> "HTTP network data transmission error."
            DownloadManager.ERROR_INSUFFICIENT_SPACE -> "Insufficient storage space on device."
            DownloadManager.ERROR_TOO_MANY_REDIRECTS -> "Too many HTTP redirects from APK server."
            DownloadManager.ERROR_UNHANDLED_HTTP_CODE -> "Server responded with unhandled HTTP status."
            else -> "Download failed (Error code: $reason). Please check your internet connection."
        }
    }

    private fun handleDownloadSuccess(context: Context, apkFile: File, updateInfo: UpdateInfo) {
        if (!apkFile.exists() || apkFile.length() == 0L) {
            notifyDownloadState(DownloadState.Failed("Downloaded APK file is empty or missing."))
            return
        }

        // Cryptographic Signature Verification
        val isVerified = verifyApkSignature(context, apkFile)
        if (!isVerified) {
            apkFile.delete()
            val errorMsg = "Security Verification Failed: The downloaded APK is signed with a different certificate than the currently installed application. Installation aborted to protect your device."
            Log.e(TAG, errorMsg)
            notifyDownloadState(DownloadState.VerificationFailed(errorMsg))
            return
        }

        Log.i(TAG, "APK Signature Verification Passed! File: ${apkFile.absolutePath} (${apkFile.length()} bytes)")
        notifyDownloadState(DownloadState.Completed(apkFile, updateInfo))
    }

    /**
     * Cryptographically verifies that the downloaded APK package name and signing certificate
     * match the currently installed app.
     */
    fun verifyApkSignature(context: Context, downloadedApkFile: File): Boolean {
        try {
            val pm = context.packageManager

            // 1. Extract installed app signatures
            val installedSignatures: List<ByteArray> = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val pkgInfo = pm.getPackageInfo(context.packageName, PackageManager.GET_SIGNING_CERTIFICATES)
                val signingInfo = pkgInfo.signingInfo ?: return false
                if (signingInfo.hasMultipleSigners()) {
                    signingInfo.apkContentsSigners.map { it.toByteArray() }
                } else {
                    signingInfo.signingCertificateHistory.map { it.toByteArray() }
                }
            } else {
                @Suppress("DEPRECATION")
                val pkgInfo = pm.getPackageInfo(context.packageName, PackageManager.GET_SIGNATURES)
                @Suppress("DEPRECATION")
                pkgInfo.signatures?.map { it.toByteArray() } ?: emptyList()
            }

            if (installedSignatures.isEmpty()) {
                Log.e(TAG, "Could not read installed package signatures.")
                return false
            }

            // 2. Extract downloaded APK signatures
            val archiveFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                PackageManager.GET_SIGNING_CERTIFICATES
            } else {
                @Suppress("DEPRECATION")
                PackageManager.GET_SIGNATURES
            }

            val archiveInfo: PackageInfo? = pm.getPackageArchiveInfo(downloadedApkFile.absolutePath, archiveFlags)
            if (archiveInfo == null) {
                Log.e(TAG, "PackageManager failed to parse downloaded APK archive.")
                return false
            }

            // Verify package name matches
            if (archiveInfo.packageName != context.packageName) {
                Log.e(TAG, "Package name mismatch! Expected: ${context.packageName}, Found: ${archiveInfo.packageName}")
                return false
            }

            val downloadedSignatures: List<ByteArray> = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val signingInfo = archiveInfo.signingInfo ?: return false
                if (signingInfo.hasMultipleSigners()) {
                    signingInfo.apkContentsSigners.map { it.toByteArray() }
                } else {
                    signingInfo.signingCertificateHistory.map { it.toByteArray() }
                }
            } else {
                @Suppress("DEPRECATION")
                archiveInfo.signatures?.map { it.toByteArray() } ?: emptyList()
            }

            if (downloadedSignatures.isEmpty()) {
                Log.e(TAG, "Could not extract signatures from downloaded APK.")
                return false
            }

            // 3. Compare SHA-256 digests
            val installedFingerprints = installedSignatures.map { computeSha256(it) }
            val downloadedFingerprints = downloadedSignatures.map { computeSha256(it) }

            Log.d(TAG, "Installed SHA-256: $installedFingerprints")
            Log.d(TAG, "Downloaded SHA-256: $downloadedFingerprints")

            val match = installedFingerprints.any { installed -> downloadedFingerprints.contains(installed) }
            return match
        } catch (e: Exception) {
            Log.e(TAG, "Exception during APK signature verification: ${e.message}", e)
            return false
        }
    }

    private fun computeSha256(data: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(data)
        return digest.joinToString(":") { "%02X".format(it) }
    }

    /**
     * Checks unknown app installation permissions and launches the Android Package Installer.
     */
    fun installApk(activity: Activity, apkFile: File) {
        try {
            // Check Unknown Sources Permission for Android 8.0 (API 26) and above
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (!activity.packageManager.canRequestPackageInstalls()) {
                    val permissionIntent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                        data = Uri.parse("package:${activity.packageName}")
                    }
                    activity.startActivity(permissionIntent)
                    return
                }
            }

            val apkUri = FileProvider.getUriForFile(
                activity,
                "${activity.packageName}.fileprovider",
                apkFile
            )

            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            activity.startActivity(installIntent)
            Log.i(TAG, "Android Package Installer launched for ${apkFile.name}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch package installer: ${e.message}", e)
        }
    }

    private fun registerDownloadCompleteReceiver(context: Context) {
        if (downloadReceiverRegistered) return
        val filter = IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                val id = intent?.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L) ?: -1L
                if (id == currentDownloadId && currentDownloadFile != null && currentPendingUpdateInfo != null) {
                    Log.i(TAG, "DownloadManager BroadcastReceiver received completion for ID: $id")
                    // Polling job will also handle it, but this guarantees completion handling
                }
            }
        }
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(receiver, filter, Context.RECEIVER_EXPORTED)
            } else {
                context.registerReceiver(receiver, filter)
            }
            downloadReceiverRegistered = true
        } catch (e: Exception) {
            Log.w(TAG, "Failed to register DownloadCompleteReceiver: ${e.message}")
        }
    }
}
