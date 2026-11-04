package com.example.ember.model

data class Track(
    val id: String,
    val title: String,
    val artist: String,
    val album: String = "EMBER Soundscape",
    var duration: String = "--:--",
    var durationSec: Int = 0,
    var thumbnail: String = "",
    var audioUrl: String = "",
    var isLiked: Boolean = false,
    var isYoutubeFallback: Boolean = false,
    var youtubeVideoId: String? = null,
    var source: String = "ytmusic"
) {
    /**
     * Returns the 11-char YouTube video ID if available,
     * or checks if the primary id itself is a valid 11-char YouTube ID.
     */
    fun getResolvedVideoId(): String? {
        val ytRegex = Regex("^[a-zA-Z0-9_-]{11}$")
        if (!youtubeVideoId.isNullOrBlank() && ytRegex.matches(youtubeVideoId!!)) {
            return youtubeVideoId
        }
        if (ytRegex.matches(id)) {
            return id
        }
        return null
    }
}
