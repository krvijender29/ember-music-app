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
        if (!youtubeVideoId.isNullOrBlank() && youtubeVideoId!!.length == 11) {
            return youtubeVideoId
        }
        if (id.length == 11 && !id.contains(" ") && !id.contains("_")) {
            return id
        }
        return null
    }
}
