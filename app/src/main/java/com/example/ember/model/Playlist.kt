package com.example.ember.model

data class Playlist(
    val id: String,
    var name: String,
    var description: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val tracks: MutableList<Track> = mutableListOf()
) {
    val coverUrl: String
        get() = tracks.firstOrNull { it.thumbnail.isNotBlank() }?.thumbnail ?: ""

    val trackCount: Int
        get() = tracks.size
}
