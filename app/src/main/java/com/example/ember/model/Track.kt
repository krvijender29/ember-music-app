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
    var isYoutubeFallback: Boolean = false
)
