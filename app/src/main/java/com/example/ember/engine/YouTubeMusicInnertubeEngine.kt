package com.example.ember.engine

import android.util.Log
import com.example.ember.model.AlbumItem
import com.example.ember.model.ArtistItem
import com.example.ember.model.PlaylistItem
import com.example.ember.model.TopMatchItem
import com.example.ember.model.Track
import com.example.ember.model.UnifiedSearchResults
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/**
 * Zero-Quota YouTube Music (Innertube API) Engine for EMBER Music.
 * 
 * Direct client-to-API communication using YouTube Music's internal web client:
 * - Client Name: WEB_REMIX (67)
 * - Version: 1.20240101.01.00
 * - Zero Google Cloud API keys, 0 quota exhaustion, 0 billing cost.
 * - Supports live autocomplete, catalog search, native Automix AI recommendations,
 *   album/playlist browsing, and external metadata resolution.
 */
object YouTubeMusicInnertubeEngine {

    private const val TAG = "InnertubeEngine"
    private const val BASE_URL = "https://music.youtube.com/youtubei/v1"

    // Filter Tokens for Catalog Search
    const val FILTER_SONGS = "EgWKAQIIAWoSEAQQBRADEAkQChAVEA4QEBAR"
    const val FILTER_ARTISTS = "EgWKAQIgAWoSEAQQBRADEAkQChAVEA4QEBAR"
    const val FILTER_ALBUMS = "EgWKAQIYAWoSEAQQBRADEAkQChAVEA4QEBAR"
    const val FILTER_PLAYLISTS = "EgeKAQQoAEABahIQBBAFEAMQCRAKEBUQDhAQEBE%3D"

    private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

    private val client = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    // 1-Hour In-Memory Cache
    private const val CACHE_TTL_MS = 60 * 60 * 1000L
    private val memoryCache = ConcurrentHashMap<String, Pair<Long, Any>>()

    private fun getCached(key: String): Any? {
        val entry = memoryCache[key] ?: return null
        if (System.currentTimeMillis() - entry.first < CACHE_TTL_MS) {
            return entry.second
        }
        memoryCache.remove(key)
        return null
    }

    private fun putCache(key: String, value: Any) {
        memoryCache[key] = Pair(System.currentTimeMillis(), value)
    }

    fun clearCache() {
        memoryCache.clear()
    }

    /**
     * Builds client context JSON required by all Innertube endpoints
     */
    private fun buildClientContext(): JSONObject {
        val client = JSONObject().apply {
            put("clientName", "WEB_REMIX")
            put("clientVersion", "1.20240101.01.00")
            put("hl", "en")
            put("gl", "IN")
        }
        return JSONObject().apply {
            put("client", client)
        }
    }

    private fun createBaseRequestBuilder(endpoint: String, jsonBody: JSONObject): Request.Builder {
        val body = jsonBody.toString().toRequestBody(JSON_MEDIA_TYPE)
        return Request.Builder()
            .url("$BASE_URL/$endpoint?alt=json")
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
            .header("Content-Type", "application/json")
            .header("X-YouTube-Client-Name", "67")
            .header("X-YouTube-Client-Version", "1.20240101.01.00")
            .header("Referer", "https://music.youtube.com/")
            .header("Origin", "https://music.youtube.com")
            .post(body)
    }

    // =========================================================================
    // 1. Live Autocomplete Search Suggestions
    // =========================================================================

    suspend fun getSearchSuggestions(query: String): List<String> = withContext(Dispatchers.IO) {
        val cleanQuery = query.trim()
        if (cleanQuery.isBlank()) return@withContext emptyList()

        val cacheKey = "sugg:$cleanQuery"
        getCached(cacheKey)?.let {
            @Suppress("UNCHECKED_CAST")
            return@withContext it as List<String>
        }

        try {
            val json = JSONObject().apply {
                put("context", buildClientContext())
                put("input", cleanQuery)
            }
            val req = createBaseRequestBuilder("music/get_search_suggestions", json).build()
            val res = client.newCall(req).execute()
            val body = res.body?.string() ?: return@withContext emptyList()

            val root = JSONObject(body)
            val contents = root.optJSONArray("contents") ?: return@withContext emptyList()
            if (contents.length() == 0) return@withContext emptyList()

            val section = contents.getJSONObject(0).optJSONObject("searchSuggestionsSectionRenderer")
            val items = section?.optJSONArray("contents") ?: return@withContext emptyList()

            val suggestions = mutableListOf<String>()
            for (i in 0 until items.length()) {
                val item = items.getJSONObject(i)
                val renderer = item.optJSONObject("searchSuggestionRenderer") ?: continue
                val runs = renderer.optJSONObject("suggestion")?.optJSONArray("runs") ?: continue
                val sb = StringBuilder()
                for (r in 0 until runs.length()) {
                    sb.append(runs.getJSONObject(r).optString("text"))
                }
                val text = sb.toString().trim()
                if (text.isNotBlank() && !suggestions.contains(text)) {
                    suggestions.add(text)
                }
            }

            putCache(cacheKey, suggestions)
            return@withContext suggestions
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch search suggestions: ${e.message}")
            return@withContext emptyList()
        }
    }

    // =========================================================================
    // 2. Multi-Category Catalog Search
    // =========================================================================

    suspend fun searchUnified(query: String): UnifiedSearchResults = withContext(Dispatchers.IO) {
        val cleanQuery = query.trim()
        if (cleanQuery.isBlank()) {
            return@withContext UnifiedSearchResults(query, null, emptyList(), emptyList(), emptyList(), emptyList())
        }

        val cacheKey = "search_unified:$cleanQuery"
        getCached(cacheKey)?.let {
            return@withContext it as UnifiedSearchResults
        }

        try {
            val json = JSONObject().apply {
                put("context", buildClientContext())
                put("query", cleanQuery)
            }
            val req = createBaseRequestBuilder("search", json).build()
            val res = client.newCall(req).execute()
            val body = res.body?.string() ?: return@withContext UnifiedSearchResults(query, null, emptyList(), emptyList(), emptyList(), emptyList())

            val root = JSONObject(body)
            val tabs = root.optJSONObject("contents")
                ?.optJSONObject("tabbedSearchResultsRenderer")
                ?.optJSONArray("tabs") ?: return@withContext UnifiedSearchResults(query, null, emptyList(), emptyList(), emptyList(), emptyList())

            if (tabs.length() == 0) return@withContext UnifiedSearchResults(query, null, emptyList(), emptyList(), emptyList(), emptyList())

            val sectionList = tabs.getJSONObject(0)
                .optJSONObject("tabRenderer")
                ?.optJSONObject("content")
                ?.optJSONObject("sectionListRenderer")
                ?.optJSONArray("contents") ?: JSONArray()

            var topMatchItem: TopMatchItem? = null
            val songs = mutableListOf<Track>()
            val artists = mutableListOf<ArtistItem>()
            val albums = mutableListOf<AlbumItem>()
            val playlists = mutableListOf<PlaylistItem>()

            for (i in 0 until sectionList.length()) {
                val secObj = sectionList.getJSONObject(i)

                // Top Match: musicCardShelfRenderer
                val cardShelf = secObj.optJSONObject("musicCardShelfRenderer")
                if (cardShelf != null && topMatchItem == null) {
                    val cardTitle = extractRunsText(cardShelf.optJSONObject("title")?.optJSONArray("runs"))
                    val cardSubtitle = extractRunsText(cardShelf.optJSONObject("subtitle")?.optJSONArray("runs"))
                    val cardThumb = extractHighResThumbnail(cardShelf.optJSONObject("thumbnail")?.optJSONObject("musicThumbnailRenderer")?.optJSONObject("thumbnail"))
                    val cardVideoId = extractVideoIdFromCard(cardShelf)

                    if (cardVideoId.isNotBlank()) {
                        val topTrack = Track(
                            id = cardVideoId,
                            title = cardTitle,
                            artist = cardSubtitle.split("•").firstOrNull()?.trim() ?: cardSubtitle,
                            album = "YouTube Music",
                            thumbnail = cardThumb,
                            youtubeVideoId = cardVideoId,
                            source = "ytmusic"
                        )
                        topMatchItem = TopMatchItem.SongMatch(topTrack)
                    } else if (cardSubtitle.contains("Artist", ignoreCase = true)) {
                        val topArtist = ArtistItem(
                            id = "yt_art_${cleanQuery.hashCode()}",
                            name = cardTitle,
                            image = cardThumb,
                            role = "Artist",
                            followerCount = cardSubtitle
                        )
                        topMatchItem = TopMatchItem.ArtistMatch(topArtist)
                    }
                }

                // Shelves: musicShelfRenderer
                val shelf = secObj.optJSONObject("musicShelfRenderer") ?: continue
                val shelfTitle = extractRunsText(shelf.optJSONObject("title")?.optJSONArray("runs")).lowercase()
                val shelfContents = shelf.optJSONArray("contents") ?: continue

                for (c in 0 until shelfContents.length()) {
                    val itemWrapper = shelfContents.getJSONObject(c)
                    val item = itemWrapper.optJSONObject("musicResponsiveListItemRenderer") ?: continue

                    when {
                        shelfTitle.contains("song") || shelfTitle.contains("track") -> {
                            parseTrackItem(item)?.let { if (songs.none { s -> s.id == it.id }) songs.add(it) }
                        }
                        shelfTitle.contains("artist") -> {
                            parseArtistItem(item)?.let { if (artists.none { a -> a.name == it.name }) artists.add(it) }
                        }
                        shelfTitle.contains("album") -> {
                            parseAlbumItem(item)?.let { if (albums.none { al -> al.id == it.id }) albums.add(it) }
                        }
                        shelfTitle.contains("playlist") -> {
                            parsePlaylistItem(item)?.let { if (playlists.none { pl -> pl.id == it.id }) playlists.add(it) }
                        }
                        else -> {
                            // General item fallback: try parsing as track if it has a videoId
                            parseTrackItem(item)?.let { if (songs.none { s -> s.id == it.id }) songs.add(it) }
                        }
                    }
                }
            }

            // Fallback Top Result if not explicitly present
            if (topMatchItem == null && songs.isNotEmpty()) {
                topMatchItem = TopMatchItem.SongMatch(songs[0])
            }

            val result = UnifiedSearchResults(
                query = cleanQuery,
                topResult = topMatchItem,
                songs = songs,
                artists = artists,
                albums = albums,
                playlists = playlists
            )
            putCache(cacheKey, result)
            return@withContext result
        } catch (e: Exception) {
            Log.e(TAG, "Unified search error: ${e.message}", e)
            return@withContext UnifiedSearchResults(query, null, emptyList(), emptyList(), emptyList(), emptyList())
        }
    }

    suspend fun searchSongs(query: String, limit: Int = 25): List<Track> = withContext(Dispatchers.IO) {
        val cleanQuery = query.trim()
        if (cleanQuery.isBlank()) return@withContext emptyList()

        val cacheKey = "search_songs:$cleanQuery:$limit"
        getCached(cacheKey)?.let {
            @Suppress("UNCHECKED_CAST")
            return@withContext it as List<Track>
        }

        try {
            val json = JSONObject().apply {
                put("context", buildClientContext())
                put("query", cleanQuery)
                put("params", FILTER_SONGS)
            }
            val req = createBaseRequestBuilder("search", json).build()
            val res = client.newCall(req).execute()
            val body = res.body?.string() ?: return@withContext emptyList()

            val tracks = parseItemsFromSearchResponse(JSONObject(body), limit)
            putCache(cacheKey, tracks)
            return@withContext tracks
        } catch (e: Exception) {
            Log.e(TAG, "Search songs error: ${e.message}")
            return@withContext emptyList()
        }
    }

    suspend fun searchArtists(query: String, limit: Int = 20): List<ArtistItem> = withContext(Dispatchers.IO) {
        val cleanQuery = query.trim()
        if (cleanQuery.isBlank()) return@withContext emptyList()

        try {
            val json = JSONObject().apply {
                put("context", buildClientContext())
                put("query", cleanQuery)
                put("params", FILTER_ARTISTS)
            }
            val req = createBaseRequestBuilder("search", json).build()
            val res = client.newCall(req).execute()
            val body = res.body?.string() ?: return@withContext emptyList()

            val items = parseSectionItems(JSONObject(body))
            val list = mutableListOf<ArtistItem>()
            for (i in 0 until items.length()) {
                if (list.size >= limit) break
                val obj = items.getJSONObject(i).optJSONObject("musicResponsiveListItemRenderer") ?: continue
                parseArtistItem(obj)?.let { list.add(it) }
            }
            return@withContext list
        } catch (e: Exception) {
            Log.e(TAG, "Search artists error: ${e.message}")
            return@withContext emptyList()
        }
    }

    suspend fun searchAlbums(query: String, limit: Int = 20): List<AlbumItem> = withContext(Dispatchers.IO) {
        val cleanQuery = query.trim()
        if (cleanQuery.isBlank()) return@withContext emptyList()

        try {
            val json = JSONObject().apply {
                put("context", buildClientContext())
                put("query", cleanQuery)
                put("params", FILTER_ALBUMS)
            }
            val req = createBaseRequestBuilder("search", json).build()
            val res = client.newCall(req).execute()
            val body = res.body?.string() ?: return@withContext emptyList()

            val items = parseSectionItems(JSONObject(body))
            val list = mutableListOf<AlbumItem>()
            for (i in 0 until items.length()) {
                if (list.size >= limit) break
                val obj = items.getJSONObject(i).optJSONObject("musicResponsiveListItemRenderer") ?: continue
                parseAlbumItem(obj)?.let { list.add(it) }
            }
            return@withContext list
        } catch (e: Exception) {
            Log.e(TAG, "Search albums error: ${e.message}")
            return@withContext emptyList()
        }
    }

    suspend fun searchPlaylists(query: String, limit: Int = 20): List<PlaylistItem> = withContext(Dispatchers.IO) {
        val cleanQuery = query.trim()
        if (cleanQuery.isBlank()) return@withContext emptyList()

        try {
            val json = JSONObject().apply {
                put("context", buildClientContext())
                put("query", cleanQuery)
                put("params", FILTER_PLAYLISTS)
            }
            val req = createBaseRequestBuilder("search", json).build()
            val res = client.newCall(req).execute()
            val body = res.body?.string() ?: return@withContext emptyList()

            val items = parseSectionItems(JSONObject(body))
            val list = mutableListOf<PlaylistItem>()
            for (i in 0 until items.length()) {
                if (list.size >= limit) break
                val obj = items.getJSONObject(i).optJSONObject("musicResponsiveListItemRenderer") ?: continue
                parsePlaylistItem(obj)?.let { list.add(it) }
            }
            return@withContext list
        } catch (e: Exception) {
            Log.e(TAG, "Search playlists error: ${e.message}")
            return@withContext emptyList()
        }
    }

    // =========================================================================
    // 3. AI Recommendations / Automix Radio ("Up Next")
    // =========================================================================

    /**
     * Calls YouTube Music's native /next Automix endpoint with RDAMVM<videoId>.
     * Returns 25-50 acoustically and algorithmically matched songs.
     */
    suspend fun getAutomixRecommendations(seedVideoId: String, limit: Int = 25): List<Track> = withContext(Dispatchers.IO) {
        val cleanId = seedVideoId.trim()
        if (cleanId.length != 11) {
            Log.w(TAG, "Invalid seed videoId for Automix: $cleanId")
            return@withContext emptyList()
        }

        val cacheKey = "recs:$cleanId:$limit"
        getCached(cacheKey)?.let {
            @Suppress("UNCHECKED_CAST")
            return@withContext it as List<Track>
        }

        try {
            val json = JSONObject().apply {
                put("context", buildClientContext())
                put("videoId", cleanId)
                put("playlistId", "RDAMVM$cleanId")
                put("isAudioOnly", true)
            }
            val req = createBaseRequestBuilder("next", json).build()
            val res = client.newCall(req).execute()
            val body = res.body?.string() ?: return@withContext emptyList()

            val root = JSONObject(body)
            val tabs = root.optJSONObject("contents")
                ?.optJSONObject("singleColumnMusicWatchNextResultsRenderer")
                ?.optJSONObject("tabbedRenderer")
                ?.optJSONObject("watchNextTabbedResultsRenderer")
                ?.optJSONArray("tabs") ?: return@withContext emptyList()

            if (tabs.length() == 0) return@withContext emptyList()

            val playlistPanel = tabs.getJSONObject(0)
                .optJSONObject("tabRenderer")
                ?.optJSONObject("content")
                ?.optJSONObject("musicQueueRenderer")
                ?.optJSONObject("content")
                ?.optJSONObject("playlistPanelRenderer")
                ?.optJSONArray("contents") ?: return@withContext emptyList()

            val recommendations = mutableListOf<Track>()
            for (i in 0 until playlistPanel.length()) {
                if (recommendations.size >= limit) break
                val item = playlistPanel.getJSONObject(i).optJSONObject("playlistPanelVideoRenderer") ?: continue
                val videoId = item.optString("videoId")
                if (videoId.isBlank() || videoId == cleanId) continue

                val title = extractRunsText(item.optJSONObject("title")?.optJSONArray("runs"))
                val byline = extractRunsText(item.optJSONObject("longBylineText")?.optJSONArray("runs"))
                val durationStr = extractRunsText(item.optJSONObject("lengthText")?.optJSONArray("runs")).ifBlank { "--:--" }
                val thumbnail = extractHighResThumbnail(item.optJSONObject("thumbnail"))

                val parts = byline.split("•")
                val artist = parts.firstOrNull()?.trim() ?: "Unknown Artist"
                val album = if (parts.size > 1) parts[1].trim() else "YouTube Music"

                val track = Track(
                    id = videoId,
                    title = title,
                    artist = artist,
                    album = album,
                    duration = durationStr,
                    durationSec = parseDurationToSeconds(durationStr),
                    thumbnail = thumbnail,
                    youtubeVideoId = videoId,
                    source = "ytmusic_recommendation"
                )

                if (recommendations.none { it.id == videoId }) {
                    recommendations.add(track)
                }
            }

            Log.i(TAG, "Fetched ${recommendations.size} native Automix recommendations for seed $cleanId")
            putCache(cacheKey, recommendations)
            return@withContext recommendations
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch Automix recommendations for $cleanId: ${e.message}", e)
            return@withContext emptyList()
        }
    }

    // =========================================================================
    // 4. Browse Album or Playlist Items
    // =========================================================================

    suspend fun browse(browseId: String): List<Track> = withContext(Dispatchers.IO) {
        val cleanId = browseId.trim()
        if (cleanId.isBlank()) return@withContext emptyList()

        val cacheKey = "browse:$cleanId"
        getCached(cacheKey)?.let {
            @Suppress("UNCHECKED_CAST")
            return@withContext it as List<Track>
        }

        try {
            val json = JSONObject().apply {
                put("context", buildClientContext())
                put("browseId", cleanId)
            }
            val req = createBaseRequestBuilder("browse", json).build()
            val res = client.newCall(req).execute()
            val body = res.body?.string() ?: return@withContext emptyList()

            val root = JSONObject(body)
            val tracks = mutableListOf<Track>()

            // Find all musicResponsiveListItemRenderer in the response
            findTracksRecursive(root, tracks)

            putCache(cacheKey, tracks)
            return@withContext tracks
        } catch (e: Exception) {
            Log.e(TAG, "Browse error for $cleanId: ${e.message}")
            return@withContext emptyList()
        }
    }

    private fun findTracksRecursive(json: Any, outTracks: MutableList<Track>) {
        when (json) {
            is JSONObject -> {
                if (json.has("musicResponsiveListItemRenderer")) {
                    json.optJSONObject("musicResponsiveListItemRenderer")?.let {
                        parseTrackItem(it)?.let { track ->
                            if (outTracks.none { t -> t.id == track.id }) {
                                outTracks.add(track)
                            }
                        }
                    }
                }
                val keys = json.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    json.opt(key)?.let { findTracksRecursive(it, outTracks) }
                }
            }
            is JSONArray -> {
                for (i in 0 until json.length()) {
                    json.opt(i)?.let { findTracksRecursive(it, outTracks) }
                }
            }
        }
    }

    // =========================================================================
    // 5. External Song Resolver (Spotify / Apple Music Metadata -> YouTube Video ID)
    // =========================================================================

    /**
     * Resolves external track metadata (title + artist) to a verified 11-char YouTube video ID.
     */
    suspend fun resolveTrackToVideoId(title: String, artist: String): String? = withContext(Dispatchers.IO) {
        val cleanTitle = title.replace(Regex("\\(.*\\)|\\[.*\\]"), "").trim()
        val cleanArtist = artist.split(",", "&", "feat.", "ft.").firstOrNull()?.trim() ?: artist.trim()
        val query = "$cleanTitle $cleanArtist"

        val cacheKey = "resolve_vid:$query"
        getCached(cacheKey)?.let {
            return@withContext it as String
        }

        val songs = searchSongs(query, limit = 5)
        val matched = songs.firstOrNull()?.getResolvedVideoId()
        if (matched != null) {
            putCache(cacheKey, matched)
            return@withContext matched
        }

        // Secondary query: clean title only
        val fallbackSongs = searchSongs(cleanTitle, limit = 5)
        val fallbackMatched = fallbackSongs.firstOrNull()?.getResolvedVideoId()
        if (fallbackMatched != null) {
            putCache(cacheKey, fallbackMatched)
            return@withContext fallbackMatched
        }

        return@withContext null
    }

    // =========================================================================
    // 6. Audio Stream Extractor for YouTube Video IDs
    // =========================================================================

    /**
     * Resolves playable high-bitrate direct audio URL for a YouTube video ID.
     * Uses fast proxy instances with instant failover.
     */
    suspend fun resolveAudioStream(videoId: String): String = withContext(Dispatchers.IO) {
        val cleanVid = videoId.trim()
        if (cleanVid.isBlank()) return@withContext ""

        val cacheKey = "stream:$cleanVid"
        getCached(cacheKey)?.let {
            return@withContext it as String
        }

        // Direct Ultra-Fast YouTube Innertube Player Resolution (<300ms)
        try {
            val json = JSONObject().apply {
                val clientObj = JSONObject().apply {
                    put("clientName", "ANDROID")
                    put("clientVersion", "20.10.38")
                    put("androidSdkVersion", 34)
                    put("hl", "en")
                    put("gl", "US")
                }
                put("context", JSONObject().apply { put("client", clientObj) })
                put("videoId", cleanVid)
            }
            val body = json.toString().toRequestBody(JSON_MEDIA_TYPE)
            val req = Request.Builder()
                .url("https://www.youtube.com/youtubei/v1/player")
                .header("User-Agent", "com.google.android.youtube/20.10.38 (Linux; U; Android 14) gzip")
                .header("Content-Type", "application/json")
                .post(body)
                .build()

            val res = client.newCall(req).execute()
            val resBody = res.body?.string() ?: ""
            if (resBody.isNotBlank()) {
                val root = JSONObject(resBody)
                val streamingData = root.optJSONObject("streamingData")
                val adaptiveFormats = streamingData?.optJSONArray("adaptiveFormats")
                if (adaptiveFormats != null && adaptiveFormats.length() > 0) {
                    var bestUrl = ""
                    var maxBitrate = 0
                    for (i in 0 until adaptiveFormats.length()) {
                        val format = adaptiveFormats.getJSONObject(i)
                        val mimeType = format.optString("mimeType", "")
                        if (mimeType.contains("audio", ignoreCase = true)) {
                            val url = format.optString("url", "")
                            val bitrate = format.optInt("bitrate", 0)
                            if (url.isNotBlank() && bitrate >= maxBitrate) {
                                maxBitrate = bitrate
                                bestUrl = url
                            }
                        }
                    }
                    if (bestUrl.isNotBlank()) {
                        putCache(cacheKey, bestUrl)
                        Log.i(TAG, "Ultra-fast direct YouTube audio stream: $cleanVid (${maxBitrate / 1000} kbps)")
                        return@withContext bestUrl
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Direct YouTube stream resolution error for $cleanVid: ${e.message}")
        }

        return@withContext ""
    }

    // =========================================================================
    // Parsing Utilities
    // =========================================================================

    private fun parseItemsFromSearchResponse(root: JSONObject, limit: Int): List<Track> {
        val tracks = mutableListOf<Track>()
        val contents = parseSectionItems(root)
        for (i in 0 until contents.length()) {
            if (tracks.size >= limit) break
            val item = contents.getJSONObject(i).optJSONObject("musicResponsiveListItemRenderer") ?: continue
            parseTrackItem(item)?.let {
                if (tracks.none { t -> t.id == it.id }) tracks.add(it)
            }
        }
        return tracks
    }

    private fun parseSectionItems(root: JSONObject): JSONArray {
        val tabs = root.optJSONObject("contents")
            ?.optJSONObject("tabbedSearchResultsRenderer")
            ?.optJSONArray("tabs") ?: return JSONArray()

        if (tabs.length() == 0) return JSONArray()

        val sectionList = tabs.getJSONObject(0)
            .optJSONObject("tabRenderer")
            ?.optJSONObject("content")
            ?.optJSONObject("sectionListRenderer")
            ?.optJSONArray("contents") ?: return JSONArray()

        if (sectionList.length() == 0) return JSONArray()

        val shelf = sectionList.getJSONObject(0).optJSONObject("musicShelfRenderer") ?: return JSONArray()
        return shelf.optJSONArray("contents") ?: JSONArray()
    }

    private fun parseTrackItem(item: JSONObject): Track? {
        val videoId = extractVideoId(item)
        if (videoId.isBlank()) return null

        val flexColumns = item.optJSONArray("flexColumns") ?: return null
        if (flexColumns.length() == 0) return null

        // Title
        val col0 = flexColumns.getJSONObject(0).optJSONObject("musicResponsiveListItemFlexColumnRenderer")
        val title = extractRunsText(col0?.optJSONObject("text")?.optJSONArray("runs")).ifBlank { "Unknown Title" }

        // Artist & Album
        var artist = "Unknown Artist"
        var album = "YouTube Music"
        if (flexColumns.length() > 1) {
            val col1 = flexColumns.getJSONObject(1).optJSONObject("musicResponsiveListItemFlexColumnRenderer")
            val subtitle = extractRunsText(col1?.optJSONObject("text")?.optJSONArray("runs"))
            val parts = subtitle.split("•")
            if (parts.isNotEmpty()) artist = parts[0].trim()
            if (parts.size > 1) album = parts[1].trim()
        }

        // Duration from fixedColumns
        var duration = "--:--"
        val fixedColumns = item.optJSONArray("fixedColumns")
        if (fixedColumns != null && fixedColumns.length() > 0) {
            val fixCol = fixedColumns.getJSONObject(0).optJSONObject("musicResponsiveListItemFixedColumnRenderer")
            duration = extractRunsText(fixCol?.optJSONObject("text")?.optJSONArray("runs")).ifBlank { "--:--" }
        }

        // Thumbnail
        val thumbObj = item.optJSONObject("thumbnail")?.optJSONObject("musicThumbnailRenderer")?.optJSONObject("thumbnail")
        val thumbnail = extractHighResThumbnail(thumbObj)

        return Track(
            id = videoId,
            title = title,
            artist = artist,
            album = album,
            duration = duration,
            durationSec = parseDurationToSeconds(duration),
            thumbnail = thumbnail,
            youtubeVideoId = videoId,
            source = "ytmusic"
        )
    }

    private fun parseArtistItem(item: JSONObject): ArtistItem? {
        val flexColumns = item.optJSONArray("flexColumns") ?: return null
        if (flexColumns.length() == 0) return null

        val col0 = flexColumns.getJSONObject(0).optJSONObject("musicResponsiveListItemFlexColumnRenderer")
        val name = extractRunsText(col0?.optJSONObject("text")?.optJSONArray("runs"))
        if (name.isBlank()) return null

        val thumbObj = item.optJSONObject("thumbnail")?.optJSONObject("musicThumbnailRenderer")?.optJSONObject("thumbnail")
        val image = extractHighResThumbnail(thumbObj)

        var subtitle = "Artist"
        if (flexColumns.length() > 1) {
            val col1 = flexColumns.getJSONObject(1).optJSONObject("musicResponsiveListItemFlexColumnRenderer")
            subtitle = extractRunsText(col1?.optJSONObject("text")?.optJSONArray("runs"))
        }

        return ArtistItem(
            id = "yt_art_${name.hashCode()}",
            name = name,
            image = image,
            role = "Artist",
            followerCount = subtitle
        )
    }

    private fun parseAlbumItem(item: JSONObject): AlbumItem? {
        val flexColumns = item.optJSONArray("flexColumns") ?: return null
        if (flexColumns.length() == 0) return null

        val col0 = flexColumns.getJSONObject(0).optJSONObject("musicResponsiveListItemFlexColumnRenderer")
        val title = extractRunsText(col0?.optJSONObject("text")?.optJSONArray("runs"))
        if (title.isBlank()) return null

        val thumbObj = item.optJSONObject("thumbnail")?.optJSONObject("musicThumbnailRenderer")?.optJSONObject("thumbnail")
        val image = extractHighResThumbnail(thumbObj)

        var subtitle = "Album"
        if (flexColumns.length() > 1) {
            val col1 = flexColumns.getJSONObject(1).optJSONObject("musicResponsiveListItemFlexColumnRenderer")
            subtitle = extractRunsText(col1?.optJSONObject("text")?.optJSONArray("runs"))
        }

        val browseId = extractBrowseId(item) ?: "yt_alb_${title.hashCode()}"

        return AlbumItem(
            id = browseId,
            title = title,
            subtitle = subtitle,
            image = image,
            year = subtitle.split("•").lastOrNull()?.trim() ?: ""
        )
    }

    private fun parsePlaylistItem(item: JSONObject): PlaylistItem? {
        val flexColumns = item.optJSONArray("flexColumns") ?: return null
        if (flexColumns.length() == 0) return null

        val col0 = flexColumns.getJSONObject(0).optJSONObject("musicResponsiveListItemFlexColumnRenderer")
        val title = extractRunsText(col0?.optJSONObject("text")?.optJSONArray("runs"))
        if (title.isBlank()) return null

        val thumbObj = item.optJSONObject("thumbnail")?.optJSONObject("musicThumbnailRenderer")?.optJSONObject("thumbnail")
        val image = extractHighResThumbnail(thumbObj)

        var subtitle = "Playlist"
        if (flexColumns.length() > 1) {
            val col1 = flexColumns.getJSONObject(1).optJSONObject("musicResponsiveListItemFlexColumnRenderer")
            subtitle = extractRunsText(col1?.optJSONObject("text")?.optJSONArray("runs"))
        }

        val browseId = extractBrowseId(item) ?: "yt_pl_${title.hashCode()}"

        return PlaylistItem(
            id = browseId,
            title = title,
            subtitle = subtitle,
            image = image,
            trackCount = subtitle.split("•").lastOrNull()?.trim() ?: ""
        )
    }

    private fun extractVideoId(item: JSONObject): String {
        // 1. playlistItemData.videoId
        val vid1 = item.optJSONObject("playlistItemData")?.optString("videoId", "") ?: ""
        if (vid1.isNotBlank()) return vid1

        // 2. overlay.musicItemThumbnailOverlayRenderer.content.musicPlayButtonRenderer.playNavigationEndpoint.watchEndpoint.videoId
        val vid2 = item.optJSONObject("overlay")
            ?.optJSONObject("musicItemThumbnailOverlayRenderer")
            ?.optJSONObject("content")
            ?.optJSONObject("musicPlayButtonRenderer")
            ?.optJSONObject("playNavigationEndpoint")
            ?.optJSONObject("watchEndpoint")
            ?.optString("videoId", "") ?: ""
        if (vid2.isNotBlank()) return vid2

        // 3. navigationEndpoint.watchEndpoint.videoId
        val vid3 = item.optJSONObject("navigationEndpoint")
            ?.optJSONObject("watchEndpoint")
            ?.optString("videoId", "") ?: ""
        if (vid3.isNotBlank()) return vid3

        return ""
    }

    private fun extractVideoIdFromCard(card: JSONObject): String {
        val btn = card.optJSONObject("buttons")
        val playEndpoint = card.optJSONObject("thumbnail")
            ?.optJSONObject("musicThumbnailRenderer")
            ?.optJSONObject("thumbnail")

        val vid1 = card.optJSONObject("onTap")
            ?.optJSONObject("watchEndpoint")
            ?.optString("videoId", "") ?: ""
        if (vid1.isNotBlank()) return vid1

        return ""
    }

    private fun extractBrowseId(item: JSONObject): String? {
        val nav = item.optJSONObject("navigationEndpoint") ?: return null
        return nav.optJSONObject("browseEndpoint")?.optString("browseId")
    }

    private fun extractRunsText(runs: JSONArray?): String {
        if (runs == null || runs.length() == 0) return ""
        val sb = StringBuilder()
        for (i in 0 until runs.length()) {
            sb.append(runs.getJSONObject(i).optString("text"))
        }
        return sb.toString().trim()
    }

    /**
     * Replaces standard low-res dimensions (=w120-h120, =s120) with high-res (=w540-h540)
     */
    private fun extractHighResThumbnail(thumbObj: JSONObject?): String {
        if (thumbObj == null) return ""
        val thumbs = thumbObj.optJSONArray("thumbnails") ?: return ""
        if (thumbs.length() == 0) return ""
        val last = thumbs.getJSONObject(thumbs.length() - 1)
        val url = last.optString("url", "")
        if (url.isBlank()) return ""

        // Upgrade dimensions to 540x540
        return url.replace(Regex("=w\\d+-h\\d+"), "=w540-h540")
            .replace(Regex("=s\\d+"), "=s540")
    }

    private fun parseDurationToSeconds(duration: String): Int {
        val parts = duration.split(":")
        return when (parts.size) {
            2 -> (parts[0].toIntOrNull() ?: 0) * 60 + (parts[1].toIntOrNull() ?: 0)
            3 -> (parts[0].toIntOrNull() ?: 0) * 3600 + (parts[1].toIntOrNull() ?: 0) * 60 + (parts[2].toIntOrNull() ?: 0)
            else -> 0
        }
    }
}
