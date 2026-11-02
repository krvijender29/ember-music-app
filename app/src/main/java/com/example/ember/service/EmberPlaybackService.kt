package com.example.ember.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.app.KeyguardManager
import android.content.IntentFilter
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionResult
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.example.ember.MainActivity
import com.example.ember.R
import com.example.ember.data.EmberDatabaseManager
import com.example.ember.engine.EmberAudioEngine
import com.example.ember.engine.EmberRecommendationEngine
import com.example.ember.model.Track
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

/**
 * EmberPlaybackService - AndroidX Media3 Background Music Playback Service
 *
 * Implements persistent background audio streaming, native Android lock screen
 * controls, media notification center synchronization, audio focus / interruptions,
 * seamless ExoPlayer playlist transitions, state persistence across app reopens,
 * and intelligent queue recommendation autoplay.
 */
class EmberPlaybackService : MediaSessionService() {

    companion object {
        private const val TAG = "EmberPlaybackService"
        const val CHANNEL_ID = "ember_playback_channel_v4"
        const val NOTIFICATION_ID = 1001

        @Volatile
        var instance: EmberPlaybackService? = null
            private set
    }

    private var mediaSession: MediaSession? = null
    lateinit var player: ExoPlayer
        private set

    private val dbManager: EmberDatabaseManager by lazy { EmberDatabaseManager.getInstance(applicationContext) }
    private var lastPlaybackStartTimeMs: Long = 0L

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var resolveAudioJob: Job? = null
    private var positionPersistJob: Job? = null
    private var isAutoRecommending: Boolean = false
    private var wakeLock: PowerManager.WakeLock? = null

    // Queue & Track State synchronized across app, notification, and lock screen
    val playlistQueue = mutableListOf<Track>()
    var currentTrackIndex: Int = -1
    var currentTrack: Track? = null
    var isShuffleMode: Boolean = false

    interface PlaybackChangeListener {
        fun onTrackChanged(track: Track?)
        fun onPlaybackStateChanged(isPlaying: Boolean, state: Int)
        fun onQueueChanged()
    }

    private val listeners = mutableListOf<PlaybackChangeListener>()

    fun addListener(listener: PlaybackChangeListener) {
        if (!listeners.contains(listener)) listeners.add(listener)
    }

    fun removeListener(listener: PlaybackChangeListener) {
        listeners.remove(listener)
    }

    @OptIn(UnstableApi::class)
    override fun onCreate() {
        super.onCreate()
        instance = this

        createNotificationChannel()

        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .build()

        player = ExoPlayer.Builder(this)
            .setAudioAttributes(audioAttributes, true)
            .setHandleAudioBecomingNoisy(true)
            .build()

        player.addListener(object : Player.Listener {
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                val matchedIndex = playlistQueue.indexOfFirst { it.audioUrl == mediaItem?.mediaId || it.id == mediaItem?.mediaId }
                if (matchedIndex != -1) {
                    lastPlaybackStartTimeMs = System.currentTimeMillis()

                    currentTrackIndex = matchedIndex
                    val track = playlistQueue[matchedIndex]
                    currentTrack = track
                    saveCurrentPlaybackState()
                    listeners.forEach { it.onTrackChanged(track) }

                    val isPreview = track.audioUrl.isBlank() || EmberAudioEngine.isPreviewUrl(track.audioUrl)
                    if (isPreview) {
                        resolveAndPlayTrack(track, matchedIndex)
                    } else {
                        // Pre-resolve upcoming track in advance
                        preResolveNextTrack(matchedIndex + 1)
                        // Load high-resolution artwork bytes asynchronously without blocking audio
                        loadArtworkAsync(track)
                    }

                    // Autoplay recommendation check: automatically extend queue when near end
                    checkAndAppendQueueRecommendations(track)
                }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                listeners.forEach { it.onPlaybackStateChanged(player.isPlaying, playbackState) }
                saveCurrentPlaybackState()
                if (playbackState == Player.STATE_ENDED) {
                    // Track 100% completed
                    val ct = currentTrack
                    if (ct != null) {
                        val isLiked = dbManager.isTrackLiked(ct.id)
                        dbManager.recordEngagement(ct, 1.0f, isLiked = isLiked)
                    }
                    if (player.hasNextMediaItem()) {
                        playNext()
                    } else {
                        // Queue ended: dynamic continuous autoplay using smart recommendations
                        val track = currentTrack
                        if (track != null) {
                            serviceScope.launch(Dispatchers.IO) {
                                val recs = EmberRecommendationEngine.getQueueRecommendations(
                                    currentTrack = track,
                                    excludeIds = playlistQueue.map { it.id }.toSet(),
                                    count = 8,
                                    dbManager = dbManager
                                )
                                withContext(Dispatchers.Main) {
                                    if (recs.isNotEmpty()) {
                                        val firstRec = recs.first()
                                        playlistQueue.addAll(recs)
                                        recs.forEach { player.addMediaItem(buildMediaItem(it)) }
                                        listeners.forEach { it.onQueueChanged() }
                                        saveCurrentPlaybackState()
                                        playTrack(firstRec)
                                    } else if (playlistQueue.isNotEmpty()) {
                                        currentTrackIndex = 0
                                        player.seekTo(0, 0L)
                                        player.play()
                                    }
                                }
                            }
                        } else if (playlistQueue.isNotEmpty()) {
                            playNext()
                        }
                    }
                }
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                listeners.forEach { it.onPlaybackStateChanged(isPlaying, player.playbackState) }
                if (isPlaying) {
                    startPositionPersistence()
                } else {
                    stopPositionPersistence()
                    saveCurrentPlaybackState()
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                Log.e(TAG, "ExoPlayer error on track $currentTrackIndex: ${error.message}")
                // Auto-advance so user playback never hangs or gets permanently stuck
                serviceScope.launch {
                    delay(500L)
                    playNext()
                }
            }
        })

        // Clicking the notification launches MainActivity
        val sessionActivityPendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        mediaSession = MediaSession.Builder(this, player)
            .setSessionActivity(sessionActivityPendingIntent)
            .setCallback(EmberMediaSessionCallback())
            .build()

        // Configure system media notification with EMBER small icon and hidden lockscreen visibility
        try {
            val defaultProvider = DefaultMediaNotificationProvider.Builder(this)
                .setChannelId(CHANNEL_ID)
                .build().apply {
                    setSmallIcon(R.drawable.ic_disc)
                }

            val lockscreenCleanProvider = object : androidx.media3.session.MediaNotification.Provider {
                override fun createNotification(
                    mediaSession: MediaSession,
                    customLayout: com.google.common.collect.ImmutableList<androidx.media3.session.CommandButton>,
                    actionFactory: androidx.media3.session.MediaNotification.ActionFactory,
                    onNotificationChangedCallback: androidx.media3.session.MediaNotification.Provider.Callback
                ): androidx.media3.session.MediaNotification {
                    val mediaNotif = defaultProvider.createNotification(
                        mediaSession,
                        customLayout,
                        actionFactory,
                        onNotificationChangedCallback
                    )
                    mediaNotif.notification.visibility = Notification.VISIBILITY_SECRET
                    return mediaNotif
                }

                override fun handleCustomCommand(
                    session: MediaSession,
                    action: String,
                    extras: android.os.Bundle
                ): Boolean {
                    return defaultProvider.handleCustomCommand(session, action, extras)
                }
            }

            setMediaNotificationProvider(lockscreenCleanProvider)
        } catch (e: Exception) {
            // Default provider fallback
        }

        // Restore last playback state if user closed and reopened the app
        restoreSavedPlaybackState()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(NotificationManager::class.java)
            try {
                notificationManager?.deleteNotificationChannel("ember_playback_channel")
                notificationManager?.deleteNotificationChannel("ember_playback_channel_v2")
                notificationManager?.deleteNotificationChannel("ember_playback_channel_v3")
            } catch (e: Exception) {
                // ignore
            }

            val channel = NotificationChannel(
                CHANNEL_ID,
                "EMBER Music Playback",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Active music playback"
                setShowBadge(false)
                lockscreenVisibility = Notification.VISIBILITY_SECRET
            }
            notificationManager?.createNotificationChannel(channel)
        }
    }

    private fun acquireWakeLock() {
        try {
            if (wakeLock == null) {
                val powerManager = getSystemService(Context.POWER_SERVICE) as? PowerManager
                wakeLock = powerManager?.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "ember:playback_wakelock")
            }
            wakeLock?.acquire(15000L) // 15-second safety timeout
        } catch (e: Exception) {
            // ignore
        }
    }

    private fun releaseWakeLock() {
        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
            }
        } catch (e: Exception) {
            // ignore
        }
    }

    /**
     * Saves current playback state (track, seek position, full queue, track index)
     */
    fun saveCurrentPlaybackState() {
        try {
            val track = currentTrack ?: return
            val pos = player.currentPosition
            val db = EmberDatabaseManager.getInstance(this)
            db.saveLastPlaybackState(track, pos, playlistQueue, currentTrackIndex)
        } catch (e: Exception) {
            Log.e(TAG, "Error saving playback state: ${e.message}")
        }
    }

    private fun startPositionPersistence() {
        positionPersistJob?.cancel()
        positionPersistJob = serviceScope.launch {
            while (true) {
                delay(3000L)
                try {
                    if (player.isPlaying && currentTrack != null) {
                        val pos = player.currentPosition
                        if (pos > 0) {
                            EmberDatabaseManager.getInstance(this@EmberPlaybackService).updateLastPlaybackPosition(pos)
                        }
                    }
                } catch (e: Exception) {
                    // ignore
                }
            }
        }
    }

    private fun stopPositionPersistence() {
        positionPersistJob?.cancel()
        positionPersistJob = null
    }

    /**
     * Restores previously played track, queue, and seek position paused on startup
     */
    private fun restoreSavedPlaybackState() {
        try {
            val db = EmberDatabaseManager.getInstance(this)
            val saved = db.getLastPlaybackState() ?: return
            if (saved.queue.isEmpty()) return

            playlistQueue.clear()
            playlistQueue.addAll(saved.queue)
            currentTrackIndex = saved.queueIndex.coerceIn(0, playlistQueue.size - 1)
            currentTrack = saved.track

            val mediaItems = playlistQueue.map { buildMediaItem(it) }
            val safePos = if (saved.positionMs > 0) saved.positionMs else 0L
            player.setMediaItems(mediaItems, currentTrackIndex, safePos)
            player.prepare()

            listeners.forEach { it.onTrackChanged(saved.track) }
            listeners.forEach { it.onQueueChanged() }
            listeners.forEach { it.onPlaybackStateChanged(false, Player.STATE_READY) }

            loadArtworkAsync(saved.track)
            preResolveNextTrack(currentTrackIndex + 1)

            // Proactively resolve URL if empty or preview so play resumes instantly
            val isPreview = saved.track.audioUrl.isBlank() || EmberAudioEngine.isPreviewUrl(saved.track.audioUrl)
            if (isPreview) {
                serviceScope.launch(Dispatchers.IO) {
                    val resolved = EmberAudioEngine.resolveAudioUrl(saved.track)
                    if (resolved.isNotBlank()) {
                        saved.track.audioUrl = resolved
                        withContext(Dispatchers.Main) {
                            if (currentTrackIndex in 0 until player.mediaItemCount) {
                                player.replaceMediaItem(currentTrackIndex, buildMediaItem(saved.track))
                            }
                        }
                    }
                }
            }
            Log.d(TAG, "Restored saved playback state: ${saved.track.title} at ${saved.positionMs}ms")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to restore playback state: ${e.message}")
        }
    }

    /**
     * Automatically queries and appends recommended tracks when user approaches the end of queue
     */
    private fun checkAndAppendQueueRecommendations(track: Track) {
        if (isAutoRecommending) return
        val remaining = playlistQueue.size - (currentTrackIndex + 1)
        if (remaining <= 2) {
            isAutoRecommending = true
            serviceScope.launch(Dispatchers.IO) {
                try {
                    val existingIds = playlistQueue.map { it.id }.toSet()
                    val recommendations = EmberRecommendationEngine.getQueueRecommendations(
                        currentTrack = track,
                        excludeIds = existingIds,
                        count = 8,
                        dbManager = dbManager
                    )
                    if (recommendations.isNotEmpty()) {
                        withContext(Dispatchers.Main) {
                            val newTracks = recommendations.filter { rec -> playlistQueue.none { it.id == rec.id } }
                            if (newTracks.isNotEmpty()) {
                                playlistQueue.addAll(newTracks)
                                newTracks.forEach { newTrack ->
                                    player.addMediaItem(buildMediaItem(newTrack))
                                }
                                listeners.forEach { it.onQueueChanged() }
                                saveCurrentPlaybackState()
                                Log.i(TAG, "Queue Autoplay: Added ${newTracks.size} recommendations for ${track.title}")
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Queue recommendation error: ${e.message}")
                } finally {
                    isAutoRecommending = false
                }
            }
        }
    }

    private inner class EmberMediaSessionCallback : MediaSession.Callback {
        override fun onConnect(
            session: MediaSession,
            controller: MediaSession.ControllerInfo
        ): MediaSession.ConnectionResult {
            val connectionResult = super.onConnect(session, controller)
            val availableSessionCommands = connectionResult.availableSessionCommands.buildUpon()
            val availablePlayerCommands = connectionResult.availablePlayerCommands.buildUpon()
                .add(Player.COMMAND_PLAY_PAUSE)
                .add(Player.COMMAND_PREPARE)
                .add(Player.COMMAND_STOP)
                .add(Player.COMMAND_SEEK_TO_DEFAULT_POSITION)
                .add(Player.COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM)
                .add(Player.COMMAND_SEEK_TO_NEXT)
                .add(Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM)
                .add(Player.COMMAND_SEEK_TO_PREVIOUS)
                .add(Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM)
                .build()

            return MediaSession.ConnectionResult.AcceptedResultBuilder(session)
                .setAvailablePlayerCommands(availablePlayerCommands)
                .setAvailableSessionCommands(availableSessionCommands.build())
                .build()
        }

        override fun onPlayerCommandRequest(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
            playerCommand: Int
        ): Int {
            when (playerCommand) {
                Player.COMMAND_SEEK_TO_NEXT, Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM -> {
                    playNext()
                    return SessionResult.RESULT_SUCCESS
                }
                Player.COMMAND_SEEK_TO_PREVIOUS, Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM -> {
                    playPrevious()
                    return SessionResult.RESULT_SUCCESS
                }
            }
            return super.onPlayerCommandRequest(session, controller, playerCommand)
        }
    }

    /**
     * Primary entry point: Sets full queue and starts playing at startIndex
     */
    fun setQueueAndPlay(tracks: List<Track>, startIndex: Int) {
        if (tracks.isEmpty()) return
        playlistQueue.clear()
        playlistQueue.addAll(tracks)
        currentTrackIndex = startIndex.coerceIn(0, tracks.size - 1)
        val targetTrack = playlistQueue[currentTrackIndex]
        currentTrack = targetTrack
        listeners.forEach { it.onTrackChanged(targetTrack) }
        listeners.forEach { it.onQueueChanged() }
        saveCurrentPlaybackState()

        resolveAudioJob?.cancel()
        val isPreview = targetTrack.audioUrl.isBlank() || EmberAudioEngine.isPreviewUrl(targetTrack.audioUrl)
        if (isPreview) {
            resolveAudioJob = serviceScope.launch {
                acquireWakeLock()
                try {
                    val resolved = EmberAudioEngine.resolveAudioUrl(targetTrack)
                    if (resolved.isNotBlank()) {
                        targetTrack.audioUrl = resolved
                    }
                } finally {
                    releaseWakeLock()
                }
                loadExoPlayerPlaylist(currentTrackIndex)
            }
        } else {
            loadExoPlayerPlaylist(currentTrackIndex)
        }
    }

    /**
     * Prepares and loads the ExoPlayer playlist with all queued media items
     */
    private fun loadExoPlayerPlaylist(startIndex: Int) {
        val mediaItems = playlistQueue.map { track ->
            buildMediaItem(track)
        }

        player.setMediaItems(mediaItems, startIndex, 0L)
        player.prepare()
        player.play()

        val ct = currentTrack
                    if (ct != null) { loadArtworkAsync(ct) }
        preResolveNextTrack(startIndex + 1)
        saveCurrentPlaybackState()
    }

    private fun buildMediaItem(track: Track): MediaItem {
        val metadataBuilder = MediaMetadata.Builder()
            .setTitle(track.title)
            .setArtist(track.artist)
            .setAlbumTitle(track.album)

        if (track.thumbnail.isNotEmpty()) {
            metadataBuilder.setArtworkUri(Uri.parse(track.thumbnail))
        }

        val uri = if (track.audioUrl.isNotBlank()) Uri.parse(track.audioUrl) else Uri.EMPTY

        return MediaItem.Builder()
            .setMediaId(track.id)
            .setUri(uri)
            .setMediaMetadata(metadataBuilder.build())
            .build()
    }

    /**
     * Non-blocking artwork loader for lock screen and notification media style
     */
    private fun loadArtworkAsync(track: Track) {
        if (track.thumbnail.isBlank()) return
        serviceScope.launch(Dispatchers.IO) {
            try {
                val loader = ImageLoader(this@EmberPlaybackService)
                val request = ImageRequest.Builder(this@EmberPlaybackService)
                    .data(track.thumbnail)
                    .allowHardware(false)
                    .build()
                val result = (loader.execute(request) as? SuccessResult)?.drawable
                val bitmap = (result as? BitmapDrawable)?.bitmap
                if (bitmap != null && currentTrack?.id == track.id) {
                    val stream = ByteArrayOutputStream()
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 85, stream)
                    val bytes = stream.toByteArray()
                    withContext(Dispatchers.Main) {
                        if (currentTrack?.id == track.id && currentTrackIndex in 0 until player.mediaItemCount) {
                            val curItem = player.currentMediaItem
                            if (curItem != null) {
                                val updatedMetadata = curItem.mediaMetadata.buildUpon()
                                    .setArtworkData(bytes, MediaMetadata.PICTURE_TYPE_FRONT_COVER)
                                    .build()
                                val updatedItem = curItem.buildUpon().setMediaMetadata(updatedMetadata).build()
                                player.replaceMediaItem(currentTrackIndex, updatedItem)
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                // Non-blocking fallback
            }
        }
    }

    /**
     * Pre-resolves audioUrl of the upcoming track so next track transition has zero delay
     */
    private fun preResolveNextTrack(nextIndex: Int) {
        if (nextIndex !in playlistQueue.indices) return
        val nextTrack = playlistQueue[nextIndex]
        val isNextPreview = nextTrack.audioUrl.isBlank() || EmberAudioEngine.isPreviewUrl(nextTrack.audioUrl)
        if (isNextPreview) {
            serviceScope.launch(Dispatchers.IO) {
                try {
                    val resolved = EmberAudioEngine.resolveAudioUrl(nextTrack)
                    if (resolved.isNotBlank()) {
                        nextTrack.audioUrl = resolved
                        withContext(Dispatchers.Main) {
                            if (nextIndex in 0 until player.mediaItemCount) {
                                player.replaceMediaItem(nextIndex, buildMediaItem(nextTrack))
                            }
                        }
                    }
                } catch (e: Exception) {
                    // ignore
                }
            }
        }
    }

    private fun resolveAndPlayTrack(track: Track, index: Int) {
        resolveAudioJob?.cancel()
        resolveAudioJob = serviceScope.launch {
            acquireWakeLock()
            try {
                val resolved = EmberAudioEngine.resolveAudioUrl(track)
                if (resolved.isNotBlank()) {
                    track.audioUrl = resolved
                    withContext(Dispatchers.Main) {
                        if (index in 0 until player.mediaItemCount) {
                            player.replaceMediaItem(index, buildMediaItem(track))
                            if (index == player.currentMediaItemIndex) {
                                player.prepare()
                                player.play()
                            }
                        }
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        playNext()
                    }
                }
            } finally {
                releaseWakeLock()
            }
        }
    }

    fun playTrack(track: Track) {
        val existingIndex = playlistQueue.indexOfFirst { it.id == track.id }
        if (existingIndex >= 0) {
            currentTrackIndex = existingIndex
            currentTrack = playlistQueue[existingIndex]
            if (existingIndex < player.mediaItemCount) {
                if (existingIndex == player.currentMediaItemIndex && player.playbackState != Player.STATE_IDLE) {
                    player.play()
                } else {
                    player.seekTo(existingIndex, 0L)
                    player.play()
                }
            } else {
                setQueueAndPlay(playlistQueue, existingIndex)
            }
        } else {
            setQueueAndPlay(listOf(track), 0)
        }
        saveCurrentPlaybackState()
    }

    fun playNext() {
        if (playlistQueue.isEmpty()) return
        if (player.hasNextMediaItem()) {
            player.seekToNextMediaItem()
        } else if (playlistQueue.size > 1) {
            player.seekTo(0, 0L)
        }
        player.play()
        saveCurrentPlaybackState()
    }

    fun playPrevious() {
        if (playlistQueue.isEmpty()) return
        if (player.currentPosition > 3000L) {
            player.seekTo(0L)
        } else if (player.hasPreviousMediaItem()) {
            player.seekToPreviousMediaItem()
        } else {
            player.seekTo(playlistQueue.size - 1, 0L)
        }
        player.play()
        saveCurrentPlaybackState()
    }

    fun addToQueue(track: Track) {
        if (playlistQueue.none { it.id == track.id }) {
            playlistQueue.add(track)
            player.addMediaItem(buildMediaItem(track))
            listeners.forEach { it.onQueueChanged() }
            saveCurrentPlaybackState()
        }
    }

    fun addAllToQueue(tracks: List<Track>) {
        if (tracks.isEmpty()) return
        val newTracks = tracks.filter { track -> playlistQueue.none { it.id == track.id } }
        if (newTracks.isEmpty()) return
        playlistQueue.addAll(newTracks)
        newTracks.forEach { player.addMediaItem(buildMediaItem(it)) }
        listeners.forEach { it.onQueueChanged() }
        saveCurrentPlaybackState()
    }

    fun playNextInQueue(track: Track) {
        val insertIndex = (currentTrackIndex + 1).coerceAtMost(playlistQueue.size)
        playlistQueue.add(insertIndex, track)
        player.addMediaItem(insertIndex, buildMediaItem(track))
        listeners.forEach { it.onQueueChanged() }
        saveCurrentPlaybackState()
    }

    fun removeFromQueue(position: Int) {
        if (position !in playlistQueue.indices) return
        if (position == currentTrackIndex) return
        playlistQueue.removeAt(position)
        player.removeMediaItem(position)
        if (position < currentTrackIndex) {
            currentTrackIndex--
        }
        listeners.forEach { it.onQueueChanged() }
        saveCurrentPlaybackState()
    }

    fun clearQueueExceptCurrent() {
        val cur = currentTrack ?: return
        val pos = player.currentPosition
        playlistQueue.clear()
        playlistQueue.add(cur)
        currentTrackIndex = 0
        player.clearMediaItems()
        player.setMediaItem(buildMediaItem(cur), pos)
        listeners.forEach { it.onQueueChanged() }
        saveCurrentPlaybackState()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        saveCurrentPlaybackState()
        val p = mediaSession?.player
        // Keep service alive if music is playing or buffering
        if (p == null || (!p.playWhenReady && p.playbackState != Player.STATE_BUFFERING)) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        saveCurrentPlaybackState()
        stopPositionPersistence()
        instance = null
        releaseWakeLock()
        serviceScope.cancel()
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        super.onDestroy()
    }
}
