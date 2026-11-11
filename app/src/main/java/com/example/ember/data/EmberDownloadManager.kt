package com.example.ember.data

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.example.ember.engine.YouTubeMusicInnertubeEngine
import com.example.ember.model.Track
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.TimeUnit

class EmberDownloadManager private constructor(private val context: Context) {

    companion object {
        private const val TAG = "EmberDownloadManager"

        @Volatile
        private var INSTANCE: EmberDownloadManager? = null

        fun getInstance(context: Context): EmberDownloadManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: EmberDownloadManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    sealed class DownloadState {
        data class Idle(val trackId: String) : DownloadState()
        data class Queued(val track: Track) : DownloadState()
        data class Progress(val track: Track, val percent: Int, val bytesDownloaded: Long, val totalBytes: Long) : DownloadState()
        data class Completed(val track: Track) : DownloadState()
        data class Failed(val track: Track, val error: String) : DownloadState()
    }

    interface DownloadListener {
        fun onDownloadStateChanged(state: DownloadState)
    }

    private val dbManager = EmberDatabaseManager.getInstance(context)
    private val scope = CoroutineScope(Dispatchers.IO + Job())
    private val mainHandler = Handler(Looper.getMainLooper())
    private val listeners = CopyOnWriteArrayList<DownloadListener>()

    private val activeJobs = ConcurrentHashMap<String, Job>()
    private val activeProgress = ConcurrentHashMap<String, Int>()

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    private val musicDir: File
        get() {
            val dir = File(context.filesDir, "offline_music")
            if (!dir.exists()) dir.mkdirs()
            return dir
        }

    private val thumbsDir: File
        get() {
            val dir = File(context.filesDir, "offline_thumbnails")
            if (!dir.exists()) dir.mkdirs()
            return dir
        }

    fun addListener(listener: DownloadListener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener)
        }
    }

    fun removeListener(listener: DownloadListener) {
        listeners.remove(listener)
    }

    private fun notifyListeners(state: DownloadState) {
        mainHandler.post {
            for (l in listeners) {
                try {
                    l.onDownloadStateChanged(state)
                } catch (e: Exception) {
                    Log.w(TAG, "Error notifying listener: ${e.message}")
                }
            }
        }
    }

    fun isDownloaded(trackId: String): Boolean {
        return dbManager.isTrackDownloaded(trackId)
    }

    fun isDownloading(trackId: String): Boolean {
        return activeJobs.containsKey(trackId)
    }

    fun getDownloadProgress(trackId: String): Int {
        return activeProgress[trackId] ?: 0
    }

    fun getDownloadedTrack(trackId: String): Track? {
        return dbManager.getDownloadedTrack(trackId)
    }

    fun getDownloadedTracks(): List<Track> {
        return dbManager.getDownloadedTracks()
    }

    fun getFormattedTotalSize(): String {
        val bytes = dbManager.getTotalDownloadedBytes()
        return formatBytes(bytes)
    }

    fun formatBytes(bytes: Long): String {
        val mb = bytes / (1024.0 * 1024.0)
        return if (mb >= 1024.0) {
            String.format("%.1f GB", mb / 1024.0)
        } else {
            String.format("%.1f MB", mb)
        }
    }

    fun startDownload(track: Track) {
        val trackId = track.id
        if (isDownloaded(trackId)) {
            val existing = dbManager.getDownloadedTrack(trackId) ?: track
            notifyListeners(DownloadState.Completed(existing))
            return
        }

        if (activeJobs.containsKey(trackId)) {
            return
        }

        notifyListeners(DownloadState.Queued(track))

        val job = scope.launch {
            try {
                // 1. Resolve direct audio stream URL if not already valid progressive
                var streamUrl = track.audioUrl
                val vid = track.getResolvedVideoId() ?: track.id
                if (streamUrl.isBlank() || streamUrl.contains("itunes.apple.com") || com.example.ember.engine.EmberAudioEngine.isExpiredUrl(streamUrl)) {
                    val resolved = YouTubeMusicInnertubeEngine.resolveAudioStream(vid)
                    if (resolved.isNotBlank()) {
                        streamUrl = resolved
                    } else {
                        // Secondary query lookup
                        val altVid = YouTubeMusicInnertubeEngine.resolveTrackToVideoId("${track.title} audio", track.artist)
                        if (!altVid.isNullOrBlank()) {
                            val altUrl = YouTubeMusicInnertubeEngine.resolveAudioStream(altVid)
                            if (altUrl.isNotBlank()) streamUrl = altUrl
                        }
                    }
                }

                if (streamUrl.isBlank()) {
                    throw IllegalStateException("Unable to resolve audio stream for ${track.title}")
                }

                val safeId = trackId.replace(Regex("[^a-zA-Z0-9_-]"), "_")
                val tmpAudioFile = File(musicDir, "$safeId.tmp")
                val finalAudioFile = File(musicDir, "$safeId.m4a")

                // 2. Stream Audio Download
                val req = Request.Builder()
                    .url(streamUrl)
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .header("Accept", "*/*")
                    .header("Connection", "keep-alive")
                    .build()

                val res = client.newCall(req).execute()
                if (!res.isSuccessful) {
                    throw IllegalStateException("HTTP ${res.code} downloading audio")
                }

                val body = res.body ?: throw IllegalStateException("Empty response body")
                val totalBytes = body.contentLength()
                var bytesReadTotal = 0L

                val inputStream: InputStream = body.byteStream()
                val outputStream = FileOutputStream(tmpAudioFile)
                val buffer = ByteArray(8192)
                var read: Int

                var lastPercent = 0
                while (inputStream.read(buffer).also { read = it } != -1) {
                    outputStream.write(buffer, 0, read)
                    bytesReadTotal += read
                    if (totalBytes > 0) {
                        val percent = ((bytesReadTotal * 100) / totalBytes).toInt().coerceIn(0, 100)
                        if (percent != lastPercent) {
                            lastPercent = percent
                            activeProgress[trackId] = percent
                            notifyListeners(DownloadState.Progress(track, percent, bytesReadTotal, totalBytes))
                        }
                    }
                }

                outputStream.flush()
                outputStream.close()
                inputStream.close()

                if (tmpAudioFile.length() < 50_000L) {
                    val size = tmpAudioFile.length()
                    tmpAudioFile.delete()
                    throw IllegalStateException("Download truncated or invalid stream ($size bytes)")
                }

                if (finalAudioFile.exists()) finalAudioFile.delete()
                if (!tmpAudioFile.renameTo(finalAudioFile)) {
                    tmpAudioFile.copyTo(finalAudioFile, overwrite = true)
                    tmpAudioFile.delete()
                }
                finalAudioFile.setReadable(true, false)

                // 3. Download Thumbnail Image for Offline UI
                var localThumbPath: String? = null
                if (track.thumbnail.isNotBlank()) {
                    try {
                        val thumbFile = File(thumbsDir, "$safeId.jpg")
                        val thumbReq = Request.Builder()
                            .url(track.thumbnail)
                            .header("User-Agent", "Mozilla/5.0")
                            .build()
                        val thumbRes = client.newCall(thumbReq).execute()
                        if (thumbRes.isSuccessful && thumbRes.body != null) {
                            val tBytes = thumbRes.body!!.bytes()
                            File(thumbsDir, "$safeId.jpg").writeBytes(tBytes)
                            localThumbPath = thumbFile.absolutePath
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Thumbnail caching warning: ${e.message}")
                    }
                }

                // 4. Save metadata into persistent offline database
                track.localAudioPath = finalAudioFile.absolutePath
                track.localThumbnailPath = localThumbPath
                track.isDownloaded = true
                track.fileSize = finalAudioFile.length()
                track.downloadTimeMs = System.currentTimeMillis()

                dbManager.saveDownloadedTrack(track)
                activeProgress.remove(trackId)
                activeJobs.remove(trackId)

                Log.i(TAG, "Successfully downloaded track: ${track.title} -> ${finalAudioFile.absolutePath} (${formatBytes(finalAudioFile.length())})")
                notifyListeners(DownloadState.Completed(track))

            } catch (e: Exception) {
                activeProgress.remove(trackId)
                activeJobs.remove(trackId)
                val safeId = trackId.replace(Regex("[^a-zA-Z0-9_-]"), "_")
                val tmpAudioFile = File(musicDir, "$safeId.tmp")
                if (tmpAudioFile.exists()) tmpAudioFile.delete()

                Log.e(TAG, "Download failed for ${track.title}: ${e.message}", e)
                notifyListeners(DownloadState.Failed(track, e.message ?: "Download failed"))
            }
        }

        activeJobs[trackId] = job
    }

    fun cancelDownload(trackId: String) {
        val job = activeJobs.remove(trackId)
        job?.cancel()
        activeProgress.remove(trackId)
        val safeId = trackId.replace(Regex("[^a-zA-Z0-9_-]"), "_")
        val tmpAudioFile = File(musicDir, "$safeId.tmp")
        if (tmpAudioFile.exists()) tmpAudioFile.delete()
        notifyListeners(DownloadState.Idle(trackId))
    }

    fun deleteDownload(trackId: String) {
        cancelDownload(trackId)
        dbManager.removeDownloadedTrack(trackId)
        notifyListeners(DownloadState.Idle(trackId))
    }
}
