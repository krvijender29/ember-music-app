package com.example.ember.engine



import android.text.Html

import android.util.Base64

import android.util.Log

import com.example.ember.model.AlbumItem

import com.example.ember.model.ArtistItem

import com.example.ember.model.PlaylistItem

import com.example.ember.model.TopMatchItem

import com.example.ember.model.Track

import com.example.ember.model.UnifiedSearchResults

import kotlinx.coroutines.Dispatchers

import kotlinx.coroutines.async

import kotlinx.coroutines.coroutineScope

import kotlinx.coroutines.withContext

import okhttp3.OkHttpClient

import okhttp3.Request

import org.json.JSONArray

import org.json.JSONObject

import java.net.URLEncoder

import java.util.UUID

import java.util.concurrent.ConcurrentHashMap

import java.util.concurrent.TimeUnit

import java.util.regex.Pattern

import javax.crypto.Cipher

import javax.crypto.spec.SecretKeySpec



object EmberAudioEngine {



    private const val TAG = "EmberAudioEngine"

    private const val SAAVN_BASE = "https://www.jiosaavn.com/api.php"

    private const val DES_KEY = "38346591"



    private val searchCache = ConcurrentHashMap<String, Pair<Long, Any>>()

    private const val CACHE_TTL_MS = 12 * 60 * 60 * 1000L // 12 hours



    

    fun isPreviewUrl(url: String?): Boolean {

        if (url.isNullOrBlank()) return true

        val lower = url.lowercase()

        return lower.contains("itunes.apple.com") ||

                lower.contains("audio-ssl.itunes.apple.com") ||

                lower.contains("mzstatic.com") ||

                lower.contains("preview") ||

                lower.contains("spotify.com")

    }



    fun clearCache() {

        searchCache.clear()

    }



    private val client = OkHttpClient.Builder()

        .connectTimeout(12, TimeUnit.SECONDS)

        .readTimeout(15, TimeUnit.SECONDS)

        .build()



    /**

     * Native DES ECB Decryptor

     * Decrypts JioSaavn encrypted_media_url into direct 320 kbps Akamai CDN stream URL

     */

    fun decryptMediaUrl(encMediaUrl: String): String {

        if (encMediaUrl.isBlank()) return ""

        return try {

            val keySpec = SecretKeySpec(DES_KEY.toByteArray(Charsets.UTF_8), "DES")

            val cipher = Cipher.getInstance("DES/ECB/PKCS5Padding")

            cipher.init(Cipher.DECRYPT_MODE, keySpec)

            val decoded = Base64.decode(encMediaUrl.trim(), Base64.DEFAULT)

            val decryptedBytes = cipher.doFinal(decoded)

            val raw = String(decryptedBytes, Charsets.UTF_8)

            raw.replace("http:", "https:")

                .replace("_96.mp4", "_320.mp4")

                .replace("_160.mp4", "_320.mp4")

        } catch (e: Exception) {

            Log.e(TAG, "Error decrypting stream URL: ${e.message}")

            ""

        }

    }



    /**

     * Deduplicate tracks using normalized title.toLowerCase() + '|' + artist.toLowerCase() and unique id

     */

        /**
     * Smart deduplication: Normalizes title to collapse variants (e.g. "Song (Official)", "Song - EP"),
     * caps identical cover artwork to avoid repetitive visual thumbnails, and purges bitrate artifacts.
     */
    fun sanitizeMetadata(text: String): String {
        if (text.isBlank()) return ""
        return text
            .replace(Regex("""(?i)\s*[\u2022\u00b7\u2219\-|•·]\s*320\s*k(bps)?"""), "")
            .replace(Regex("""(?i)\s*320\s*k(bps)?"""), "")
            .replace(Regex("""(?i)\b320\s*kbps\b"""), "")
            .replace(Regex("""(?i)\b320k\b"""), "")
            .trim()
            .trimEnd(' ', '•', '·', '-', '|', '–', '—')
    }

    fun deduplicateTracks(tracks: List<Track>): List<Track> {
        val seenNormalizedTitles = mutableSetOf<String>()
        val seenIds = mutableSetOf<String>()
        val seenThumbnails = mutableMapOf<String, Int>()
        val result = mutableListOf<Track>()

        for (t in tracks) {
            val cleanTitle = t.title
                .replace("""(?i)\s*[•·|]?\s*320\s*k(bps)?""".toRegex(), "")
                .replace("""(?i)\([feat|ft]\..*?\)""".toRegex(), "")
                .replace("""(?i)\[[feat|ft]\..*?\]""".toRegex(), "")
                .replace("""(?i)\(official.*?\)""".toRegex(), "")
                .replace("""(?i)\[official.*?\]""".toRegex(), "")
                .replace("""(?i)\(from ".*?"\)""".toRegex(), "")
                .replace("""(?i)\[from ".*?"\]""".toRegex(), "")
                .replace("""(?i)- (single|ep|remix|version)""".toRegex(), "")
                .replace("""\s+""".toRegex(), " ")
                .trim()
                .lowercase()

            val thumbKey = t.thumbnail.trim()
            val thumbCount = if (thumbKey.isNotEmpty()) seenThumbnails.getOrDefault(thumbKey, 0) else 0

            // Skip if normalized title or track ID already present, or if same album art already has 2 tracks
            if (!seenNormalizedTitles.contains(cleanTitle) && !seenIds.contains(t.id) && thumbCount < 2) {
                seenNormalizedTitles.add(cleanTitle)
                seenIds.add(t.id)
                if (thumbKey.isNotEmpty()) {
                    seenThumbnails[thumbKey] = thumbCount + 1
                }
                val cleanedTrack = t.copy(
                    title = sanitizeMetadata(t.title),
                    artist = sanitizeMetadata(t.artist)
                )
                result.add(cleanedTrack)
            }
        }
        return result
    }



    /**

     * Live search query across 80+ million tracks with optional filter

     */

    suspend fun searchTracks(query: String, count: Int = 30, filter: String = "All"): List<Track> {

        return searchSongs(query, count)

    }



    /**

     * Unified "All" Results Tab (Apple Music Inspired)

     * Parallel workers fetch songs, artists, albums, playlists concurrently.

     */

    suspend fun searchUnified(query: String): UnifiedSearchResults = withContext(Dispatchers.IO) {

        val cleanQ = query.trim()

        if (cleanQ.isBlank()) {

            return@withContext UnifiedSearchResults(cleanQ, null, emptyList(), emptyList(), emptyList(), emptyList())

        }



        val cacheKey = "${cleanQ.lowercase()}::all::24"

        val cached = searchCache[cacheKey]

        val now = System.currentTimeMillis()

        if (cached != null && (now - cached.first) < CACHE_TTL_MS) {

            val res = cached.second as? UnifiedSearchResults

            if (res != null) return@withContext res

        }



        coroutineScope {

            val songsDeferred = async { searchSongs(cleanQ, 20) }

            val artistsDeferred = async { searchArtists(cleanQ, 12) }

            val albumsDeferred = async { searchAlbums(cleanQ, 20) }

            val playlistsDeferred = async { searchPlaylists(cleanQ, 20) }



            val rawSongs = songsDeferred.await()

            val artists = artistsDeferred.await()

            val albums = albumsDeferred.await()

            val playlists = playlistsDeferred.await()



            val songs = deduplicateTracks(rawSongs)



            // Determine Top Result (Apple Music inspired)

            val qLower = cleanQ.lowercase()

            val topResult: TopMatchItem? = if (artists.isNotEmpty() &&

                (artists[0].name.lowercase().contains(qLower) || qLower.contains(artists[0].name.lowercase()))

            ) {

                TopMatchItem.ArtistMatch(artists[0])

            } else if (songs.isNotEmpty()) {

                TopMatchItem.SongMatch(songs[0])

            } else null



            val result = UnifiedSearchResults(

                query = cleanQ,

                topResult = topResult,

                songs = songs,

                artists = artists,

                albums = albums,

                playlists = playlists

            )



            searchCache[cacheKey] = Pair(now, result)

            result

        }

    }



    /**

     * Multi-category: Songs search with direct 320kbps streams + iTunes fallback

     */

        suspend fun searchSaavnTracks(query: String, count: Int = 30): List<Track> = withContext(Dispatchers.IO) {

        val cleanQ = query.trim()

        if (cleanQ.isBlank()) return@withContext emptyList()

        try {

            val encodedQuery = URLEncoder.encode(cleanQ, "UTF-8")

            val url = "$SAAVN_BASE?__call=search.getResults&q=$encodedQuery&n=$count&p=1&_format=json&_marker=0&api_version=4&ctx=web6dot0"

            val req = Request.Builder().url(url).header("User-Agent", "Mozilla/5.0").build()

            val res = client.newCall(req).execute()

            val body = res.body?.string() ?: ""

            parseTracksFromResponse(body)

        } catch (e: Exception) {

            emptyList()

        }

    }



    suspend fun searchSongs(query: String, count: Int = 30): List<Track> = withContext(Dispatchers.IO) {

        val cleanQ = query.trim()

        if (cleanQ.isBlank()) return@withContext emptyList()



        val cacheKey = "${cleanQ.lowercase()}::songs::$count"

        val cached = searchCache[cacheKey]

        val now = System.currentTimeMillis()

        if (cached != null && (now - cached.first) < CACHE_TTL_MS) {

            val res = cached.second as? List<Track>

            if (res != null) return@withContext res

        }



        // 1. Fetch from JioSaavn (100% full length 320kbps Akamai CDN streams)

        val saavnTracks = searchSaavnTracks(cleanQ, count)

        val finalTracks = if (saavnTracks.size >= count.coerceAtMost(8)) {

            saavnTracks

        } else {

            // Supplement with iTunes only if JioSaavn returned few results,

            // but leave audioUrl empty so resolveAudioUrl dynamically resolves the full song!

            val itunesTracks = searchItunes(cleanQ, limit = 10)

            val merged = mutableListOf<Track>()

            merged.addAll(saavnTracks)

            merged.addAll(itunesTracks)

            merged

        }



        val deduped = deduplicateTracks(finalTracks)

        searchCache[cacheKey] = Pair(now, deduped)

        deduped

    }



    /**

     * Multi-category: Artists search

     */

    /**

     * Fetches official artist discography & top songs from Saavn artist page or fallback search

     */

    suspend fun getArtistTracks(artistId: String, artistName: String, count: Int = 40): List<Track> = withContext(Dispatchers.IO) {

        val cleanName = artistName.trim()

        if (cleanName.isBlank()) return@withContext emptyList()

        if (artistId.isNotBlank()) {

            try {

                val url = "$SAAVN_BASE?__call=artist.getArtistPageDetails&artistId=$artistId&n_song=$count&_format=json&_marker=0&api_version=4&ctx=web6dot0"

                val req = Request.Builder().url(url).header("User-Agent", "Mozilla/5.0").build()

                val res = client.newCall(req).execute()

                val body = res.body?.string() ?: ""

                val tracks = parseTracksFromResponse(body)

                if (tracks.isNotEmpty()) return@withContext deduplicateTracks(tracks)

            } catch (e: Exception) {

                // fallback to search

            }

        }

        val saavnSearch = searchSaavnTracks(cleanName, count)

        if (saavnSearch.isNotEmpty()) return@withContext deduplicateTracks(saavnSearch)

        searchSongs(cleanName, count)

    }



    suspend fun searchArtists(query: String, count: Int = 20): List<ArtistItem> = withContext(Dispatchers.IO) {

        val cleanQ = query.trim()

        if (cleanQ.isBlank()) return@withContext emptyList()



        val cacheKey = "${cleanQ.lowercase()}::artists::$count"

        val cached = searchCache[cacheKey]

        val now = System.currentTimeMillis()

        if (cached != null && (now - cached.first) < CACHE_TTL_MS) {

            val res = cached.second as? List<ArtistItem>

            if (res != null) return@withContext res

        }



        val encodedQuery = URLEncoder.encode(cleanQ, "UTF-8")

        val url = "$SAAVN_BASE?__call=search.getArtistResults&q=$encodedQuery&n=$count&p=1&_format=json&_marker=0&api_version=4&ctx=web6dot0"

        val list = mutableListOf<ArtistItem>()

        try {

            val req = Request.Builder().url(url).header("User-Agent", "Mozilla/5.0").build()

            val res = client.newCall(req).execute()

            val body = res.body?.string() ?: ""

            val json = JSONObject(body)

            val results = json.optJSONArray("results") ?: JSONArray()

            for (i in 0 until results.length()) {

                val it = results.getJSONObject(i)

                val id = it.optString("id", UUID.randomUUID().toString())

                val rawName = it.optString("name", it.optString("title", "Artist"))

                val name = Html.fromHtml(rawName, Html.FROM_HTML_MODE_LEGACY).toString().trim()

                var img = it.optString("image", "").replace("50x50", "500x500").replace("150x150", "500x500")

                if (img.isBlank()) img = "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=500"

                val role = it.optString("role", "Artist • EMBER")

                val follower = it.optString("follower_count", "")

                list.add(ArtistItem(id, name, img, role, follower))

            }

        } catch (e: Exception) {

            Log.e(TAG, "searchArtists error: ${e.message}")

        }

        searchCache[cacheKey] = Pair(now, list)

        list

    }



    /**

     * Multi-category: Albums search (JioSaavn + iTunes hybrid search)

     */

    suspend fun searchAlbums(query: String, count: Int = 40): List<AlbumItem> = withContext(Dispatchers.IO) {

        val cleanQ = query.trim()

        if (cleanQ.isBlank()) return@withContext emptyList()



        val cacheKey = "${cleanQ.lowercase()}::albums::$count"

        val cached = searchCache[cacheKey]

        val now = System.currentTimeMillis()

        if (cached != null && (now - cached.first) < CACHE_TTL_MS) {

            val res = cached.second as? List<AlbumItem>

            if (res != null) return@withContext res

        }



        coroutineScope {

            val saavnDeferred = async {

                val encodedQuery = URLEncoder.encode(cleanQ, "UTF-8")

                val url = "$SAAVN_BASE?__call=search.getAlbumResults&q=$encodedQuery&n=$count&p=1&_format=json&_marker=0&api_version=4&ctx=web6dot0"

                val list = mutableListOf<AlbumItem>()

                try {

                    val req = Request.Builder().url(url).header("User-Agent", "Mozilla/5.0").build()

                    val res = client.newCall(req).execute()

                    val body = res.body?.string() ?: ""

                    val json = JSONObject(body)

                    val results = json.optJSONArray("results") ?: JSONArray()

                    for (i in 0 until results.length()) {

                        val it = results.getJSONObject(i)

                        val id = it.optString("id", it.optString("albumid", UUID.randomUUID().toString()))

                        val rawTitle = it.optString("title", it.optString("name", "Album"))

                        val title = sanitizeMetadata(Html.fromHtml(rawTitle, Html.FROM_HTML_MODE_LEGACY).toString().trim())

                        var img = it.optString("image", "").replace("50x50", "500x500").replace("150x150", "500x500")

                        if (img.isBlank()) img = "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?w=500"

                        val rawSub = it.optString("subtitle", it.optString("artist", "Album"))

                        val subtitle = Html.fromHtml(rawSub, Html.FROM_HTML_MODE_LEGACY).toString().trim()

                        val year = it.optString("year", "")

                        list.add(AlbumItem(id, title, subtitle, img, year))

                    }

                } catch (e: Exception) {

                    Log.e(TAG, "searchAlbums error: ${e.message}")

                }

                list

            }



            val itunesDeferred = async {

                searchItunesAlbums(cleanQ, 25)

            }



            val saavnAlbums = saavnDeferred.await()

            val itunesAlbums = itunesDeferred.await()



            val combined = mutableListOf<AlbumItem>()

            combined.addAll(saavnAlbums)

            val seenTitles = saavnAlbums.map { it.title.lowercase().trim() }.toMutableSet()

            for (al in itunesAlbums) {

                val key = al.title.lowercase().trim()

                if (!seenTitles.contains(key)) {

                    seenTitles.add(key)

                    combined.add(al)

                }

            }



            searchCache[cacheKey] = Pair(now, combined)

            combined

        }

    }



    /**

     * Search albums from iTunes Search API

     */

    suspend fun searchItunesAlbums(query: String, limit: Int = 25): List<AlbumItem> = withContext(Dispatchers.IO) {

        val albums = mutableListOf<AlbumItem>()

        try {

            val encoded = URLEncoder.encode(query.trim(), "UTF-8")

            val url = "https://itunes.apple.com/search?term=$encoded&media=music&entity=album&limit=$limit"

            val req = Request.Builder().url(url).header("User-Agent", "Mozilla/5.0").build()

            val res = client.newCall(req).execute()

            val body = res.body?.string() ?: ""

            val json = JSONObject(body)

            val results = json.optJSONArray("results") ?: JSONArray()

            for (i in 0 until results.length()) {

                val it = results.getJSONObject(i)

                val id = "itunes_al_" + it.optString("collectionId", UUID.randomUUID().toString())

                val rawTitle = it.optString("collectionName", "Album")

                val title = Html.fromHtml(rawTitle, Html.FROM_HTML_MODE_LEGACY).toString().trim()

                val rawSub = it.optString("artistName", "Artist")

                val subtitle = Html.fromHtml(rawSub, Html.FROM_HTML_MODE_LEGACY).toString().trim()

                var img = it.optString("artworkUrl100", "").replace("100x100bb.jpg", "600x600bb.jpg").replace("100x100", "600x600")

                if (img.isBlank()) img = "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?w=500"

                val releaseDate = it.optString("releaseDate", "")

                val year = if (releaseDate.length >= 4) releaseDate.substring(0, 4) else ""

                albums.add(AlbumItem(id, title, subtitle, img, year))

            }

        } catch (e: Exception) {

            Log.w(TAG, "iTunes albums error: ${e.message}")

        }

        albums

    }



    /**

     * Multi-category: Playlists search (Expanded JioSaavn search)

     */

    suspend fun searchPlaylists(query: String, count: Int = 40): List<PlaylistItem> = withContext(Dispatchers.IO) {

        val cleanQ = query.trim()

        if (cleanQ.isBlank()) return@withContext emptyList()



        val cacheKey = "${cleanQ.lowercase()}::playlists::$count"

        val cached = searchCache[cacheKey]

        val now = System.currentTimeMillis()

        if (cached != null && (now - cached.first) < CACHE_TTL_MS) {

            val res = cached.second as? List<PlaylistItem>

            if (res != null) return@withContext res

        }



        val encodedQuery = URLEncoder.encode(cleanQ, "UTF-8")

        val url = "$SAAVN_BASE?__call=search.getPlaylistResults&q=$encodedQuery&n=$count&p=1&_format=json&_marker=0&api_version=4&ctx=web6dot0"

        val list = mutableListOf<PlaylistItem>()

        try {

            val req = Request.Builder().url(url).header("User-Agent", "Mozilla/5.0").build()

            val res = client.newCall(req).execute()

            val body = res.body?.string() ?: ""

            val json = JSONObject(body)

            val results = json.optJSONArray("results") ?: JSONArray()

            val seenTitles = mutableSetOf<String>()

            for (i in 0 until results.length()) {

                val it = results.getJSONObject(i)

                val id = it.optString("id", it.optString("listid", UUID.randomUUID().toString()))

                val rawTitle = it.optString("title", it.optString("listname", it.optString("name", "Playlist")))

                val title = Html.fromHtml(rawTitle, Html.FROM_HTML_MODE_LEGACY).toString().trim()

                val normTitle = title.lowercase().trim()

                if (seenTitles.contains(normTitle)) continue

                seenTitles.add(normTitle)



                var img = it.optString("image", "").replace("50x50", "500x500").replace("150x150", "500x500")

                if (img.isBlank()) img = "https://images.unsplash.com/photo-1470225620780-dba8ba36b745?w=500"

                val countStr = it.optString("count", "")

                val subtitle = if (countStr.isNotEmpty()) "$countStr Songs" else it.optString("subtitle", "Playlist")

                list.add(PlaylistItem(id, title, subtitle, img, countStr))

            }

        } catch (e: Exception) {

            Log.e(TAG, "searchPlaylists error: ${e.message}")

        }

        searchCache[cacheKey] = Pair(now, list)

        list

    }



    /**

     * Debounced Live Autocomplete suggestions (JioSaavn + Google Suggest fallback)

     */

    suspend fun getSuggestions(query: String): List<String> = withContext(Dispatchers.IO) {

        val cleanQ = query.trim()

        if (cleanQ.isBlank()) return@withContext emptyList()



        val suggestions = mutableListOf<String>()

        val seen = mutableSetOf<String>()



        try {

            val encoded = URLEncoder.encode(cleanQ, "UTF-8")

            val url = "$SAAVN_BASE?__call=autocomplete.get&query=$encoded&_format=json&_marker=0&api_version=4&ctx=web6dot0"

            val req = Request.Builder().url(url).header("User-Agent", "Mozilla/5.0").build()

            val res = client.newCall(req).execute()

            val body = res.body?.string() ?: ""

            if (body.isNotEmpty()) {

                val root = JSONObject(body)

                val topData = root.optJSONObject("topquery")?.optJSONArray("data")

                val songsData = root.optJSONObject("songs")?.optJSONArray("data")

                val artistsData = root.optJSONObject("artists")?.optJSONArray("data")

                val albumsData = root.optJSONObject("albums")?.optJSONArray("data")



                fun addData(arr: JSONArray?) {

                    if (arr == null) return

                    for (i in 0 until arr.length()) {

                        val it = arr.optJSONObject(i) ?: continue

                        val t = Html.fromHtml(it.optString("title", it.optString("name", "")), Html.FROM_HTML_MODE_LEGACY).toString().trim()

                        if (t.isNotEmpty() && !seen.contains(t.lowercase())) {

                            seen.add(t.lowercase())

                            suggestions.add(t)

                        }

                    }

                }



                addData(topData)

                addData(songsData)

                addData(artistsData)

                addData(albumsData)

            }

        } catch (e: Exception) {

            Log.w(TAG, "JioSaavn suggest failed: ${e.message}")

        }



        if (suggestions.isEmpty()) {

            try {

                val gUrl = "https://suggestqueries.google.com/complete/search?client=firefox&ds=yt&q=${URLEncoder.encode(cleanQ, "UTF-8")}"

                val req = Request.Builder().url(gUrl).header("User-Agent", "Mozilla/5.0").build()

                val res = client.newCall(req).execute()

                val body = res.body?.string() ?: ""

                val arr = JSONArray(body)

                if (arr.length() > 1) {

                    val list = arr.optJSONArray(1)

                    if (list != null) {

                        for (i in 0 until list.length()) {

                            val s = list.optString(i, "").trim()

                            if (s.isNotEmpty() && !seen.contains(s.lowercase())) {

                                seen.add(s.lowercase())

                                suggestions.add(s)

                            }

                        }

                    }

                }

            } catch (e: Exception) {

                Log.w(TAG, "Google suggest failed: ${e.message}")

            }

        }



        suggestions.take(8)

    }



    /**

     * iTunes Search API helper

     */

    suspend fun searchItunes(query: String, limit: Int = 15): List<Track> = withContext(Dispatchers.IO) {

        val tracks = mutableListOf<Track>()

        try {

            val encoded = URLEncoder.encode(query.trim(), "UTF-8")

            val url = "https://itunes.apple.com/search?term=$encoded&media=music&entity=song&limit=$limit"

            val req = Request.Builder().url(url).header("User-Agent", "Mozilla/5.0").build()

            val res = client.newCall(req).execute()

            val body = res.body?.string() ?: ""

            val json = JSONObject(body)

            val results = json.optJSONArray("results") ?: JSONArray()

            for (i in 0 until results.length()) {

                val it = results.getJSONObject(i)

                val id = "itunes_" + it.optString("trackId", UUID.randomUUID().toString())

                val title = it.optString("trackName", "Track")

                val artist = it.optString("artistName", "Artist")

                val album = it.optString("collectionName", "Album")

                var img = it.optString("artworkUrl100", "")

                if (img.isNotEmpty()) {

                    img = img.replace("100x100bb.jpg", "600x600bb.jpg").replace("100x100", "600x600")

                }

                val preview = it.optString("previewUrl", "")

                val durationMs = it.optLong("trackTimeMillis", 0L)

                val durationSec = (durationMs / 1000).toInt()

                val mins = durationSec / 60

                val secs = durationSec % 60

                val duration = String.format("%d:%02d", mins, secs)



                tracks.add(

                    Track(

                        id = id,

                        title = title,

                        artist = artist,

                        album = album,

                        duration = duration,

                        durationSec = durationSec,

                        thumbnail = img,

                        audioUrl = "", // Never use 30s preview; dynamically resolved to full 320kbps track

                        isLiked = false,

                        isYoutubeFallback = false

                    )

                )

            }

        } catch (e: Exception) {

            Log.w(TAG, "iTunes search error: ${e.message}")

        }

        tracks

    }



    /**

     * Fetch curated trending and mood tracks

     */

    suspend fun getTrendingTracks(category: String = "Trending"): List<Track> = withContext(Dispatchers.IO) {

        val query = when (category.lowercase()) {

            "punjabi" -> "punjabi top 50 hits 2024"

            "bollywood" -> "bollywood romantic hits 2024"

            "pop", "english" -> "global top english hits"

            "lo-fi", "chill", "lo-fi chill" -> "lofi chill beats"

            "hip-hop", "hip hop" -> "hip hop top hits"

            "r&b", "r&b / soul", "soul" -> "r&b soul smooth hits"

            else -> "trending hits"

        }

        searchTracks(query, 25)

    }



    /**

     * Dual-Engine Audio Resolver:

     * 1. Direct 320kbps Akamai CDN stream from JioSaavn.

     * 2. Automated YouTube Audio Fallback if CDN stream is missing or fails.

     */

        suspend fun resolveAudioUrl(track: Track): String = withContext(Dispatchers.IO) {

        if (track.audioUrl.isNotBlank() && !isPreviewUrl(track.audioUrl)) {

            return@withContext track.audioUrl

        }



        // Primary Engine: JioSaavn direct 320kbps CDN search

        try {

            val cleanTitle = track.title.replace(Regex("\\(.*\\)|\\[.*\\]"), "").trim()

            val cleanArtist = track.artist.split(",", "&", "feat.", "ft.").firstOrNull()?.trim() ?: track.artist.trim()



            val queries = listOf(

                "$cleanTitle $cleanArtist",

                "${track.title} ${track.artist}",

                cleanTitle

            )



            for (q in queries) {

                if (q.isBlank()) continue

                val search = searchSaavnTracks(q, count = 6)

                val matched = search.firstOrNull { it.audioUrl.isNotBlank() && !isPreviewUrl(it.audioUrl) }

                if (matched != null) {

                    track.audioUrl = matched.audioUrl

                    if (track.duration == "--:--" || track.duration.isBlank() || isPreviewUrl(track.audioUrl)) {

                        track.duration = matched.duration

                        track.durationSec = matched.durationSec

                    }

                    if (track.thumbnail.isBlank()) {

                        track.thumbnail = matched.thumbnail

                    }

                    track.isYoutubeFallback = false

                    Log.i(TAG, "Resolved full 320kbps CDN stream for ${track.title}")

                    return@withContext matched.audioUrl

                }

            }

        } catch (e: Exception) {

            Log.w(TAG, "CDN resolution error for ${track.title}: ${e.message}")

        }



        // Secondary Engine: YouTube Audio Fallback for full track

        Log.i(TAG, "Attempting YouTube fallback engine for ${track.title}...")

        val fallbackUrl = resolveYoutubeAudioFallback("${track.title} ${track.artist}")

        if (fallbackUrl.isNotBlank()) {

            track.audioUrl = fallbackUrl

            track.isYoutubeFallback = true

            Log.i(TAG, "Successfully resolved full YouTube audio fallback for ${track.title}")

            return@withContext fallbackUrl

        }



        ""

    }



    suspend fun resolveYoutubeAudioFallback(query: String): String = withContext(Dispatchers.IO) {

        val encoded = URLEncoder.encode(query.trim(), "UTF-8")

        val instances = listOf(

            "https://api.piped.private.coffee",

            "https://pipedapi.kavin.rocks",

            "https://pipedapi.adminforge.de",

            "https://invidious.nerdvpn.de/api/v1"

        )



        for (base in instances) {

            try {

                if (base.contains("invidious")) {

                    val invUrl = "$base/search?q=$encoded&type=video"

                    val req = Request.Builder().url(invUrl).header("User-Agent", "EMBER-Audio/1.0").build()

                    val res = client.newCall(req).execute()

                    val body = res.body?.string() ?: continue

                    val arr = JSONArray(body)

                    if (arr.length() > 0) {

                        val videoId = arr.getJSONObject(0).optString("videoId")

                        if (videoId.isNotBlank()) {

                            // Fetch video stream info

                            val vidReq = Request.Builder().url("$base/videos/$videoId").header("User-Agent", "EMBER-Audio/1.0").build()

                            val vidRes = client.newCall(vidReq).execute()

                            val vidBody = vidRes.body?.string() ?: continue

                            val vidJson = JSONObject(vidBody)

                            val formatStreams = vidJson.optJSONArray("adaptiveFormats")

                            if (formatStreams != null) {

                                var bestAudio = ""

                                var maxBitrate = 0

                                for (k in 0 until formatStreams.length()) {

                                    val f = formatStreams.getJSONObject(k)

                                    val type = f.optString("type", "")

                                    if (type.contains("audio")) {

                                        val br = f.optInt("bitrate", 0)

                                        if (br >= maxBitrate) {

                                            maxBitrate = br

                                            bestAudio = f.optString("url", "")

                                        }

                                    }

                                }

                                if (bestAudio.isNotBlank()) return@withContext bestAudio

                            }

                        }

                    }

                } else {

                    val searchUrl = "$base/search?q=$encoded&filter=music_songs"

                    val req = Request.Builder().url(searchUrl).header("User-Agent", "EMBER-Audio/1.0").build()

                    val res = client.newCall(req).execute()

                    val body = res.body?.string() ?: continue

                    val json = JSONObject(body)

                    val items = json.optJSONArray("items") ?: continue

                    if (items.length() > 0) {

                        val first = items.getJSONObject(0)

                        val urlPath = first.optString("url", "")

                        val videoId = if (urlPath.contains("v=")) urlPath.substringAfter("v=") else urlPath.substringAfter("/watch?v=")

                        if (videoId.isNotBlank()) {

                            val streamReq = Request.Builder().url("$base/streams/$videoId").header("User-Agent", "EMBER-Audio/1.0").build()

                            val streamRes = client.newCall(streamReq).execute()

                            val streamBody = streamRes.body?.string() ?: continue

                            val streamJson = JSONObject(streamBody)

                            val audioStreams = streamJson.optJSONArray("audioStreams")

                            if (audioStreams != null && audioStreams.length() > 0) {

                                var bestAudio = ""

                                var maxBitrate = 0

                                for (k in 0 until audioStreams.length()) {

                                    val a = audioStreams.getJSONObject(k)

                                    val br = a.optInt("bitrate", 0)

                                    if (br >= maxBitrate) {

                                        maxBitrate = br

                                        bestAudio = a.optString("url", "")

                                    }

                                }

                                if (bestAudio.isNotBlank()) return@withContext bestAudio

                            }

                        }

                    }

                }

            } catch (e: Exception) {

                Log.w(TAG, "Instance $base attempt failed: ${e.message}")

            }

        }

        ""

    }



    /**

     * Spotify Playlist Importer

     * Parses public open.spotify.com/playlist/... link, retrieves tracks and resolves to 320kbps CDN streams.

     */

    suspend fun importSpotifyPlaylist(spotifyUrl: String): Pair<String, List<Track>> = withContext(Dispatchers.IO) {

        var playlistName = "Imported Spotify Playlist"

        val resolvedTracks = mutableListOf<Track>()



        try {

            // 1. Fetch metadata using Spotify oEmbed

            val oembedUrl = "https://open.spotify.com/oembed?url=${URLEncoder.encode(spotifyUrl.trim(), "UTF-8")}"

            val oembedReq = Request.Builder().url(oembedUrl).build()

            val oembedRes = client.newCall(oembedReq).execute()

            if (oembedRes.isSuccessful) {

                val oembedBody = oembedRes.body?.string() ?: ""

                if (oembedBody.isNotEmpty()) {

                    val oJson = JSONObject(oembedBody)

                    val title = oJson.optString("title", "")

                    if (title.isNotEmpty()) playlistName = title

                }

            }



            // 2. Fetch Embed HTML to parse tracklist

            val matcher = Pattern.compile("playlist[/:]([a-zA-Z0-9]+)").matcher(spotifyUrl)

            val playlistId = if (matcher.find()) matcher.group(1) else ""



            val trackNames = mutableListOf<Pair<String, String>>() // (Title, Artist)



            if (!playlistId.isNullOrEmpty()) {

                val embedUrl = "https://open.spotify.com/embed/playlist/$playlistId"

                val embedReq = Request.Builder()

                    .url(embedUrl)

                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")

                    .build()

                val embedRes = client.newCall(embedReq).execute()

                val embedHtml = embedRes.body?.string() ?: ""



                // Extract NEXT_DATA JSON

                if (embedHtml.contains("<script id=\"__NEXT_DATA__\"")) {

                    val jsonStr = embedHtml.substringAfter("<script id=\"__NEXT_DATA__\" type=\"application/json\">")

                        .substringBefore("</script>")

                    val nextData = JSONObject(jsonStr)

                    val entity = nextData.optJSONObject("props")

                        ?.optJSONObject("pageProps")

                        ?.optJSONObject("state")

                        ?.optJSONObject("data")

                        ?.optJSONObject("entity")



                    if (entity != null) {

                        val name = entity.optString("name", "")

                        if (name.isNotEmpty()) playlistName = name



                        val trackList = entity.optJSONArray("trackList")

                        if (trackList != null) {

                            for (i in 0 until trackList.length()) {

                                val tObj = trackList.getJSONObject(i)

                                val title = tObj.optString("title", "")

                                val subtitle = tObj.optString("subtitle", "")

                                if (title.isNotEmpty()) {

                                    trackNames.add(Pair(title, subtitle))

                                }

                            }

                        }

                    }

                }

            }



            // If scraping couldn't find items (e.g. rate limit), provide top curated hits based on title

            if (trackNames.isEmpty()) {

                val queryTerm = if (playlistName.contains("Playlist")) "Top Hits 2024" else playlistName

                val searched = searchTracks(queryTerm, 15)

                resolvedTracks.addAll(searched)

            } else {

                // Resolve each track into direct 320kbps CDN stream

                for ((title, artist) in trackNames.take(20)) {

                    val searchResult = searchTracks("$title $artist", count = 3)

                    val matched = searchResult.firstOrNull { it.audioUrl.isNotBlank() }

                    if (matched != null) {

                        resolvedTracks.add(matched)

                    } else if (searchResult.isNotEmpty()) {

                        resolvedTracks.add(searchResult[0])

                    }

                }

            }

        } catch (e: Exception) {

            Log.e(TAG, "Spotify import error: ${e.message}")

        }



        Pair(playlistName, resolvedTracks)

    }



    suspend fun getAlbumTracks(albumId: String, albumTitle: String): List<Track> = withContext(Dispatchers.IO) {

        val cleanTitle = albumTitle.trim()

        try {

            if (albumId.isNotBlank()) {

                val url = "$SAAVN_BASE?__call=content.getAlbumDetails&albumid=$albumId&_format=json&_marker=0&api_version=4&ctx=web6dot0"

                val req = Request.Builder().url(url).header("User-Agent", "Mozilla/5.0").build()

                val res = client.newCall(req).execute()

                val body = res.body?.string() ?: ""

                val tracks = parseTracksFromResponse(body)

                if (tracks.isNotEmpty()) return@withContext deduplicateTracks(tracks)

            }

        } catch (e: Exception) {

            Log.w(TAG, "getAlbumTracks error: ${e.message}")

        }

        searchSongs(cleanTitle, 30)

    }



    suspend fun getPlaylistTracks(playlistId: String, playlistTitle: String): List<Track> = withContext(Dispatchers.IO) {

        val cleanTitle = playlistTitle.trim()

        try {

            if (playlistId.isNotBlank()) {

                val url = "$SAAVN_BASE?__call=playlist.getDetails&listid=$playlistId&_format=json&_marker=0&api_version=4&ctx=web6dot0"

                val req = Request.Builder().url(url).header("User-Agent", "Mozilla/5.0").build()

                val res = client.newCall(req).execute()

                val body = res.body?.string() ?: ""

                val tracks = parseTracksFromResponse(body)

                if (tracks.isNotEmpty()) return@withContext deduplicateTracks(tracks)

            }

        } catch (e: Exception) {

            Log.w(TAG, "getPlaylistTracks error: ${e.message}")

        }

        searchSongs(cleanTitle, 30)

    }



    private fun parseTracksFromResponse(jsonStr: String): List<Track> {

        val list = mutableListOf<Track>()

        try {

            val root = JSONObject(jsonStr)

            val results: JSONArray = root.optJSONArray("songs")

                ?: root.optJSONArray("list")

                ?: root.optJSONArray("results")

                ?: root.optJSONObject("topSongs")?.optJSONArray("songs")

                ?: return list



            for (i in 0 until results.length()) {

                val item = results.optJSONObject(i) ?: continue



                // Extract encrypted URL from root or more_info

                var enc = item.optString("encrypted_media_url", "")

                val moreInfo = item.optJSONObject("more_info")

                if (enc.isEmpty() && moreInfo != null) {

                    enc = moreInfo.optString("encrypted_media_url", "")

                }



                val audioUrl = decryptMediaUrl(enc)

                if (audioUrl.isEmpty()) continue



                val id = item.optString("id", System.currentTimeMillis().toString() + i)

                val rawTitle = item.optString("song", item.optString("title", "Unknown Track"))

                val title = Html.fromHtml(rawTitle, Html.FROM_HTML_MODE_LEGACY).toString().trim()



                // Primary artist

                var artist = item.optString("primary_artists", item.optString("singers", ""))

                if (artist.isEmpty() && moreInfo != null) {

                    val artistMap = moreInfo.optJSONObject("artistMap")

                    val primaryArr = artistMap?.optJSONArray("primary_artists")

                    if (primaryArr != null && primaryArr.length() > 0) {

                        val names = mutableListOf<String>()

                        for (a in 0 until primaryArr.length()) {

                            val aObj = primaryArr.optJSONObject(a)

                            val name = aObj?.optString("name", "") ?: ""

                            if (name.isNotEmpty()) names.add(name)

                        }

                        artist = names.joinToString(", ")

                    }

                }

                if (artist.isEmpty()) {

                    artist = item.optString("subtitle", "Ember Artist")

                }

                artist = sanitizeMetadata(Html.fromHtml(artist, Html.FROM_HTML_MODE_LEGACY).toString().trim())



                val rawAlbum = item.optString("album", moreInfo?.optString("album", "") ?: "") ?: ""

                val album = Html.fromHtml(rawAlbum, Html.FROM_HTML_MODE_LEGACY).toString().trim()



                var image = item.optString("image", "")

                if (image.isNotEmpty()) {

                    image = image.replace("150x150", "500x500").replace("50x50", "500x500")

                }



                val rawDuration = item.optString("duration", moreInfo?.optString("duration", "210") ?: "210") ?: "210"

                val durationSec = rawDuration.toIntOrNull() ?: 210

                val minutes = durationSec / 60

                val seconds = durationSec % 60

                val durationStr = String.format("%02d:%02d", minutes, seconds)



                list.add(

                    Track(

                        id = id,

                        title = title,

                        artist = artist,

                        album = album,

                        duration = durationStr,

                        durationSec = durationSec,

                        thumbnail = image,

                        audioUrl = audioUrl,

                        isYoutubeFallback = false

                    )

                )

            }

        } catch (e: Exception) {

            Log.e(TAG, "Error parsing track json: ${e.message}")

        }

        return list

    }

}

