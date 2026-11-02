package com.example.ember.model

data class ArtistItem(
    val id: String,
    val name: String,
    val image: String,
    val role: String = "Artist",
    val followerCount: String = ""
)

data class AlbumItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val image: String,
    val year: String = ""
)

data class PlaylistItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val image: String,
    val trackCount: String = ""
)

sealed class TopMatchItem {
    data class SongMatch(val track: Track) : TopMatchItem()
    data class ArtistMatch(val artist: ArtistItem) : TopMatchItem()
}

data class UnifiedSearchResults(
    val query: String,
    val topResult: TopMatchItem?,
    val songs: List<Track>,
    val artists: List<ArtistItem>,
    val albums: List<AlbumItem>,
    val playlists: List<PlaylistItem>
)
