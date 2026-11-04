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
import com.example.ember.engine.YouTubeMusicInnertubeEngine
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

        val httpDataSourceFactory = androidx.media3.datasource.DefaultHttpDataSource.Factory()
            .setUserAgent("com.google.android.youtube/20.10.38 (Linux; U; Android 14) gzip")
            .setAllowCrossProtocolRedirects(true)
            .setConnectTimeoutMs(15000)
            .setReadTimeoutMs(25000)

        val mediaSourceFactory = androidx.media3.exoplayer.source.DefaultMediaSourceFactory(this)
            .setDataSourceFactory(httpDataSourceFactory)

        player = ExoPlayer.Builder(this)
            .setMediaSourceFactory(mediaSourceFactory)
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
                        playTrackInternal(track)
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
                    val ct = currentTrack
                    if (ct != null) {
                        val isLiked = dbManager.isTrackLiked(ct.id)
                        dbManager.recordEngagement(ct, 1.0f, isLiked = isLiked)
                    }
                    // Seamlessly auto-advance to next track in YouTube Music mix
                    playNext()
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
                val cur = currentTrack
                serviceScope.launch {
                    delay(500L)
                    val seedVid = cur?.getResolvedVideoId()
                    if (!seedVid.isNullOrBlank()) {
                        val freshUrl = YouTubeMusicInnertubeEngine.resolveAudioStream(seedVid)
                        if (freshUrl.isNotBlank() && freshUrl != cur?.audioUrl) {
                            cur?.audioUrl = freshUrl
                            startPlaybackWithItem(cur!!)
                            return@launch
                        }
                    }
                    // Try alternative video resolution (e.g. audio / topic version matching website player.js handleYtError)
                    if (cur != null) {
                        try {
                            val altVid = YouTubeMusicInnertubeEngine.resolveTrackToVideoId("${cur.title} audio", cur.artist)
                            if (!altVid.isNullOrBlank() && altVid != seedVid) {
                                cur.youtubeVideoId = altVid
                                val altUrl = YouTubeMusicInnertubeEngine.resolveAudioStream(altVid)
                                if (altUrl.isNotBlank()) {
                                    cur.audioUrl = altUrl
                                    Log.i(TAG, "Switched to alternative YouTube stream: ${cur.title} ($altVid)")
                                    startPlaybackWithItem(cur)
                                    return@launch
                                }
                            }
                        } catch (e: Exception) {
                            Log.w(TAG, "Alternative stream lookup failed: ${e.message}")
                        }
                    }
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
            if (EmberAudioEngine.isExpiredUrl(saved.track.audioUrl)) {
                saved.track.audioUrl = ""
            }

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
     * Utilizes YouTube Music Automix AI Engine (/next) with seamless fallback
     */
    private fun checkAndAppendQueueRecommendations(track: Track) {
        if (isAutoRecommending) return
        val remaining = playlistQueue.size - (currentTrackIndex + 1)
        if (remaining <= 2) {
            isAutoRecommending = true
            serviceScope.launch(Dispatchers.IO) {
                try {
                    val existingIds = playlistQueue.map { it.id }.toSet()
                    val seedVideoId = track.getResolvedVideoId()
                        ?: YouTubeMusicInnertubeEngine.resolveTrackToVideoId(track.title, track.artist)

                    val ytRecs = if (!seedVideoId.isNullOrBlank()) {
                        YouTubeMusicInnertubeEngine.getAutomixRecommendations(seedVideoId, limit = 15)
                            .filter { it.id !in existingIds }
                    } else emptyList()

                    val recommendations = if (ytRecs.isNotEmpty()) {
                        Log.i(TAG, "Queue Autoplay: Fetched ${ytRecs.size} YouTube Music Automix recommendations for ${track.title}")
                        ytRecs
                    } else {
                        EmberRecommendationEngine.getQueueRecommendations(
                            currentTrack = track,
                            excludeIds = existingIds,
                            count = 10,
                            dbManager = dbManager
                        )
                    }
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

        playTrackInternal(targetTrack)

        // Concurrently fetch YouTube Music /next Automix recommendations for Up Next queue
        fetchYouTubeNextRecommendations(targetTrack)
    }

    private var automixFetchJob: Job? = null

    /**
     * Concurrently calls YouTube Music native /next Automix endpoint (RDAMVM<seedVideoId>)
     * and automatically populates the 'Up Next' queue with 25 acoustically matched tracks.
     */
    private fun fetchYouTubeNextRecommendations(track: Track) {
        automixFetchJob?.cancel()
        automixFetchJob = serviceScope.launch(Dispatchers.IO) {
            try {
                val seedVideoId = track.getResolvedVideoId()
                    ?: YouTubeMusicInnertubeEngine.resolveTrackToVideoId(track.title, track.artist)

                if (!seedVideoId.isNullOrBlank()) {
                    val recs = YouTubeMusicInnertubeEngine.getAutomixRecommendations(seedVideoId, limit = 25)
                    if (recs.isNotEmpty()) {
                        withContext(Dispatchers.Main) {
                            if (currentTrack?.id == track.id) {
                                val existingIds = playlistQueue.map { it.id }.toSet()
                                val newTracks = recs.filter { it.id !in existingIds }
                                if (newTracks.isNotEmpty()) {
                                    playlistQueue.addAll(newTracks)
                                    listeners.forEach { it.onQueueChanged() }
                                    saveCurrentPlaybackState()
                                    preResolveNextTrack(currentTrackIndex + 1)
                                    Log.i(TAG, "YouTube Music /next: Added ${newTracks.size} Automix tracks to Up Next queue for ${track.title}")
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Error fetching YouTube /next recommendations: ${e.message}")
            }
        }
    }

    private fun playTrackInternal(track: Track) {
        resolveAudioJob?.cancel()
        val isPreview = track.audioUrl.isBlank() || EmberAudioEngine.isPreviewUrl(track.audioUrl)
        if (isPreview) {
            resolveAudioJob = serviceScope.launch {
                acquireWakeLock()
                try {
                    val resolved = EmberAudioEngine.resolveAudioUrl(track)
                    if (resolved.isNotBlank()) {
                        track.audioUrl = resolved
                    }
                } finally {
                    releaseWakeLock()
                }
                withContext(Dispatchers.Main) {
                    if (currentTrack?.id == track.id) {
                        startPlaybackWithItem(track)
                    }
                }
            }
        } else {
            startPlaybackWithItem(track)
        }
    }

    private fun startPlaybackWithItem(track: Track) {
        if (track.audioUrl.isBlank()) {
            Log.e(TAG, "Cannot start playback: audioUrl is empty for ${track.title}")
            return
        }
        val mediaItem = buildMediaItem(track)
        player.setMediaItem(mediaItem)
        player.prepare()
        player.play()

        loadArtworkAsync(track)
        preResolveNextTrack(currentTrackIndex + 1)
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
                        Log.i(TAG, "Pre-resolved upcoming YouTube Music track: ${nextTrack.title}")
                    }
                } catch (e: Exception) {
                    // ignore
                }
            }
        }
    }

    fun playTrackAtIndex(index: Int) {
        if (index !in playlistQueue.indices) return
        currentTrackIndex = index
        val targetTrack = playlistQueue[currentTrackIndex]
        currentTrack = targetTrack
        listeners.forEach { it.onTrackChanged(targetTrack) }
        saveCurrentPlaybackState()

        playTrackInternal(targetTrack)

        // As user advances through queue, continuously append Automix recommendations
        checkAndAppendQueueRecommendations(targetTrack)
    }

    fun playTrack(track: Track) {
        val existingIndex = playlistQueue.indexOfFirst { it.id == track.id }
        if (existingIndex >= 0) {
            playTrackAtIndex(existingIndex)
        } else {
            setQueueAndPlay(listOf(track), 0)
        }
        saveCurrentPlaybackState()
    }

    fun playNext() {
        if (playlistQueue.isEmpty()) return
        val nextIndex = (currentTrackIndex + 1) % playlistQueue.size
        playTrackAtIndex(nextIndex)
    }

    fun playPrevious() {
        if (playlistQueue.isEmpty()) return
        if (player.currentPosition > 3000L) {
            player.seekTo(0L)
            player.play()
            return
        }
        val prevIndex = if (currentTrackIndex - 1 < 0) playlistQueue.size - 1 else currentTrackIndex - 1
        playTrackAtIndex(prevIndex)
    }

    fun addToQueue(track: Track) {
        if (playlistQueue.none { it.id == track.id }) {
            playlistQueue.add(track)
            listeners.forEach { it.onQueueChanged() }
            saveCurrentPlaybackState()
            if (currentTrackIndex == -1 || playlistQueue.size == 1) {
                playTrackAtIndex(0)
            } else if (playlistQueue.size == currentTrackIndex + 2) {
                preResolveNextTrack(currentTrackIndex + 1)
            }
        }
    }

    fun addAllToQueue(tracks: List<Track>) {
        if (tracks.isEmpty()) return
        val newTracks = tracks.filter { track -> playlistQueue.none { it.id == track.id } }
        if (newTracks.isEmpty()) return
        playlistQueue.addAll(newTracks)
        listeners.forEach { it.onQueueChanged() }
        saveCurrentPlaybackState()
        if (currentTrackIndex == -1) {
            playTrackAtIndex(0)
        } else {
            preResolveNextTrack(currentTrackIndex + 1)
        }
    }

    fun playNextInQueue(track: Track) {
        val insertIndex = (currentTrackIndex + 1).coerceAtMost(playlistQueue.size)
        playlistQueue.add(insertIndex, track)
        listeners.forEach { it.onQueueChanged() }
        saveCurrentPlaybackState()
        preResolveNextTrack(insertIndex)
    }

    fun removeFromQueue(position: Int) {
        if (position !in playlistQueue.indices) return
        if (position == currentTrackIndex) return
        playlistQueue.removeAt(position)
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
