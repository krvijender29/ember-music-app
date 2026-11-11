package com.example.ember.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.ember.model.Playlist
import com.example.ember.model.Track
import com.example.ember.model.User
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.UUID
import java.util.concurrent.TimeUnit

class EmberDatabaseManager(private val context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("ember_db_prefs", Context.MODE_PRIVATE)
    private val authManager = EmberAuthManager.getInstance(context)

    companion object {
        private const val TAG = "EmberDatabaseManager"
        private const val SUPABASE_URL = "https://ombptejuakpgaapeclyj.supabase.co"
        private const val SUPABASE_ANON_KEY = "sb_publishable_RJOx9D6cYvi_75XCoWY8Dw_VDUHR8eF"

        @Volatile
        private var INSTANCE: EmberDatabaseManager? = null

        fun getInstance(context: Context): EmberDatabaseManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: EmberDatabaseManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    // ------------------------------------------------------------------------
    // Liked Songs (Favorites)
    // ------------------------------------------------------------------------

    fun isTrackLiked(trackId: String): Boolean {
        val liked = getLikedSongs()
        return liked.any { it.id == trackId }
    }

    fun toggleLike(track: Track): Boolean {
        val currentLiked = getLikedSongs().toMutableList()
        val index = currentLiked.indexOfFirst { it.id == track.id }
        val isNowLiked: Boolean

        if (index >= 0) {
            currentLiked.removeAt(index)
            isNowLiked = false
            track.isLiked = false
            syncDeleteLikedSong(track.id)
        } else {
            track.isLiked = true
            currentLiked.add(0, track)
            isNowLiked = true
            syncInsertLikedSong(track)
            recordEngagement(track, 1.0f, isLiked = true)
        }

        saveLikedSongs(currentLiked)
        return isNowLiked
    }

    fun getLikedSongs(): List<Track> {
        val jsonStr = prefs.getString("liked_songs_cache", "[]") ?: "[]"
        return parseTracks(jsonStr)
    }

    fun saveLikedSongs(tracks: List<Track>) {
        val jsonArr = serializeTracks(tracks)
        prefs.edit().putString("liked_songs_cache", jsonArr.toString()).apply()
    }

    // ------------------------------------------------------------------------
    // Downloaded Songs (Offline Playback)
    // ------------------------------------------------------------------------

    fun getDownloadedTracks(): List<Track> {
        val jsonStr = prefs.getString("downloaded_tracks_cache", "[]") ?: "[]"
        val tracks = parseTracks(jsonStr)
        return tracks.filter { t ->
            val p = t.localAudioPath
            !p.isNullOrBlank() && java.io.File(p).exists()
        }
    }

    fun isTrackDownloaded(trackId: String): Boolean {
        return getDownloadedTracks().any { it.id == trackId }
    }

    fun getDownloadedTrack(trackId: String): Track? {
        return getDownloadedTracks().firstOrNull { it.id == trackId }
    }

    fun saveDownloadedTrack(track: Track) {
        val current = getDownloadedTracks().toMutableList()
        current.removeAll { it.id == track.id }
        track.isDownloaded = true
        current.add(0, track)
        val jsonArr = serializeTracks(current)
        prefs.edit().putString("downloaded_tracks_cache", jsonArr.toString()).apply()
    }

    fun removeDownloadedTrack(trackId: String) {
        val current = getDownloadedTracks().toMutableList()
        val found = current.find { it.id == trackId }
        if (found != null) {
            found.localAudioPath?.let { p ->
                val f = java.io.File(p)
                if (f.exists()) f.delete()
            }
            found.localThumbnailPath?.let { p ->
                val f = java.io.File(p)
                if (f.exists()) f.delete()
            }
            current.remove(found)
            val jsonArr = serializeTracks(current)
            prefs.edit().putString("downloaded_tracks_cache", jsonArr.toString()).apply()
        }
    }

    fun getDownloadedCount(): Int = getDownloadedTracks().size

    fun getTotalDownloadedBytes(): Long {
        var total = 0L
        for (t in getDownloadedTracks()) {
            val p = t.localAudioPath
            if (!p.isNullOrBlank()) {
                val f = java.io.File(p)
                if (f.exists()) total += f.length()
            }
        }
        return total
    }

    fun getLocalAvatarFile(): java.io.File {
        val dir = context.filesDir.resolve("user_profile")
        if (!dir.exists()) dir.mkdirs()
        return java.io.File(dir, "avatar.jpg")
    }

    fun hasLocalAvatar(): Boolean {
        return getLocalAvatarFile().exists() && getLocalAvatarFile().length() > 0
    }

    // ------------------------------------------------------------------------
    // Listening History / Recently Played
    // ------------------------------------------------------------------------

    fun recordHistory(track: Track) {
        val history = getHistory().toMutableList()
        history.removeAll { it.id == track.id }
        history.add(0, track)
        if (history.size > 50) {
            history.removeAt(history.size - 1)
        }

        val jsonArr = serializeTracks(history)
        prefs.edit().putString("history_cache", jsonArr.toString()).apply()

        // Track play counts for auto-recommendations
        incrementArtistPlayCount(track.artist)
        val inferredGenre = inferGenreFromTrack(track)
        incrementGenrePlayCount(inferredGenre)

        // Sync with Supabase in background
        syncInsertHistory(track)
    }

    private fun inferGenreFromTrack(track: Track): String {
        val text = "${track.title} ${track.artist} ${track.album}".lowercase()
        return when {
            text.contains("punjabi") || text.contains("sandhu") || text.contains("aujla") ||
            text.contains("dosanjh") || text.contains("sidhu") || text.contains("shubh") ||
            text.contains("dhillon") || text.contains("karan") -> "Punjabi"
            text.contains("arijit") || text.contains("bollywood") || text.contains("pritam") ||
            text.contains("shreya") || text.contains("atif") || text.contains("kumar") ||
            text.contains("jubin") || text.contains("romantic") -> "Bollywood"
            text.contains("lofi") || text.contains("lo-fi") || text.contains("chill") ||
            text.contains("sleep") || text.contains("ambient") -> "Lo-Fi Chill"
            text.contains("hip-hop") || text.contains("hip hop") || text.contains("rap") ||
            text.contains("drake") || text.contains("eminem") || text.contains("divine") -> "Hip-Hop"
            text.contains("weeknd") || text.contains("taylor") || text.contains("pop") ||
            text.contains("sheeran") || text.contains("dua lipa") || text.contains("billie") -> "Pop"
            text.contains("rock") || text.contains("guitar") -> "Rock"
            text.contains("edm") || text.contains("electronic") || text.contains("synth") -> "Electronic"
            else -> {
                val (genres, _) = getUserPreferences()
                genres.firstOrNull() ?: "Punjabi"
            }
        }
    }

    // ------------------------------------------------------------------------
    // Advanced Next-Gen Recommendation Telemetry & Affinity Engine
    // ------------------------------------------------------------------------

    private val DECAY_LAMBDA = 0.693147 / (14.0 * 24.0 * 60.0 * 60.0 * 1000.0) // 14-day half-life

    /**
     * Records fine-grained user engagement with a track.
     * Evaluates skip ratio, completion, likes, and consecutive repeats to adjust weights.
     */
    fun recordEngagement(
        track: Track,
        listenRatio: Float,
        isLiked: Boolean = false,
        isRepeated: Boolean = false
    ) {
        val baseScore = when {
            listenRatio < 0.15f -> -2.0f // Skip penalty
            listenRatio < 0.50f -> 0.5f  // Passive listen
            listenRatio < 0.85f -> 1.5f  // Substantial listen
            else -> 3.0f                 // Full completion
        }
        val bonus = (if (isLiked) 5.0f else 0.0f) + (if (isRepeated) 3.0f else 0.0f)
        val scoreDelta = baseScore + bonus

        val artists = track.artist.split(Regex("[,&/]|\\bft\\.?\\b|\\bfeat\\.?\\b", RegexOption.IGNORE_CASE))
            .map { it.trim() }
            .filter { it.isNotBlank() && it.length > 1 }

        val now = System.currentTimeMillis()
        val affinityJsonStr = prefs.getString("artist_affinity_scores", "{}") ?: "{}"
        try {
            val json = JSONObject(affinityJsonStr)
            for (a in artists) {
                val currentObj = json.optJSONObject(a)
                val currentScore = currentObj?.optDouble("score", 0.0) ?: (getArtistPlayCounts()[a]?.toDouble() ?: 0.0)
                val lastUpdated = currentObj?.optLong("updated", now) ?: now
                val ageMs = (now - lastUpdated).coerceAtLeast(0L)
                val decayedCurrent = currentScore * Math.exp(-DECAY_LAMBDA * ageMs)
                val newScore = (decayedCurrent + scoreDelta).coerceAtLeast(0.0)

                val newObj = JSONObject().apply {
                    put("score", newScore)
                    put("updated", now)
                    if (scoreDelta > 0) put("completions", (currentObj?.optInt("completions", 0) ?: 0) + 1)
                    if (scoreDelta < 0) put("skips", (currentObj?.optInt("skips", 0) ?: 0) + 1)
                }
                json.put(a, newObj)
            }
            prefs.edit().putString("artist_affinity_scores", json.toString()).apply()
        } catch (e: Exception) {
            Log.w(TAG, "recordEngagement artist error: ${e.message}")
        }

        // Record genre affinity
        val genre = inferGenreFromTrack(track)
        val genreJsonStr = prefs.getString("genre_affinity_scores", "{}") ?: "{}"
        try {
            val json = JSONObject(genreJsonStr)
            val currentObj = json.optJSONObject(genre)
            val currentScore = currentObj?.optDouble("score", 0.0) ?: (getGenrePlayCounts()[genre]?.toDouble() ?: 0.0)
            val lastUpdated = currentObj?.optLong("updated", now) ?: now
            val ageMs = (now - lastUpdated).coerceAtLeast(0L)
            val decayedCurrent = currentScore * Math.exp(-DECAY_LAMBDA * ageMs)
            val newScore = (decayedCurrent + scoreDelta).coerceAtLeast(0.0)

            val newObj = JSONObject().apply {
                put("score", newScore)
                put("updated", now)
            }
            json.put(genre, newObj)
            prefs.edit().putString("genre_affinity_scores", json.toString()).apply()
        } catch (e: Exception) {
            Log.w(TAG, "recordEngagement genre error: ${e.message}")
        }
    }

    /**
     * Returns artists ranked by decayed affinity score (most recent & loved artists first).
     */
    fun getDecayedArtistScores(): Map<String, Float> {
        val map = mutableMapOf<String, Float>()
        val affinityJsonStr = prefs.getString("artist_affinity_scores", "{}") ?: "{}"
        val now = System.currentTimeMillis()
        try {
            val json = JSONObject(affinityJsonStr)
            val keys = json.keys()
            while (keys.hasNext()) {
                val k = keys.next()
                val obj = json.getJSONObject(k)
                val score = obj.optDouble("score", 0.0)
                val updated = obj.optLong("updated", now)
                val ageMs = (now - updated).coerceAtLeast(0L)
                val decayed = (score * Math.exp(-DECAY_LAMBDA * ageMs)).toFloat()
                if (decayed > 0.05f) {
                    map[k] = decayed
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "getDecayedArtistScores error: ${e.message}")
        }
        return map
    }

    /**
     * Returns genres ranked by decayed affinity score.
     */
    fun getDecayedGenreScores(): Map<String, Float> {
        val map = mutableMapOf<String, Float>()
        val genreJsonStr = prefs.getString("genre_affinity_scores", "{}") ?: "{}"
        val now = System.currentTimeMillis()
        try {
            val json = JSONObject(genreJsonStr)
            val keys = json.keys()
            while (keys.hasNext()) {
                val k = keys.next()
                val obj = json.getJSONObject(k)
                val score = obj.optDouble("score", 0.0)
                val updated = obj.optLong("updated", now)
                val ageMs = (now - updated).coerceAtLeast(0L)
                val decayed = (score * Math.exp(-DECAY_LAMBDA * ageMs)).toFloat()
                if (decayed > 0.05f) {
                    map[k] = decayed
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "getDecayedGenreScores error: ${e.message}")
        }
        return map
    }

    /**
     * Records a co-occurrence edge between two artists played consecutively without skip.
     */
    fun recordArtistTransition(rawA: String, rawB: String, weight: Float = 1.0f) {
        val a = rawA.split(Regex("[,&/]|\\bft\\.?\\b|\\bfeat\\.?\\b", RegexOption.IGNORE_CASE)).firstOrNull()?.trim() ?: rawA.trim()
        val b = rawB.split(Regex("[,&/]|\\bft\\.?\\b|\\bfeat\\.?\\b", RegexOption.IGNORE_CASE)).firstOrNull()?.trim() ?: rawB.trim()
        if (a.isBlank() || b.isBlank() || a.equals(b, ignoreCase = true)) return

        val graphStr = prefs.getString("artist_cooccurrence_graph", "{}") ?: "{}"
        try {
            val json = JSONObject(graphStr)
            val nodeA = json.optJSONObject(a.lowercase()) ?: JSONObject()
            val currentW = nodeA.optDouble(b, 0.0)
            nodeA.put(b, currentW + weight)
            json.put(a.lowercase(), nodeA)

            // Symmetric link
            val nodeB = json.optJSONObject(b.lowercase()) ?: JSONObject()
            val currentBW = nodeB.optDouble(a, 0.0)
            nodeB.put(a, currentBW + weight)
            json.put(b.lowercase(), nodeB)

            prefs.edit().putString("artist_cooccurrence_graph", json.toString()).apply()
        } catch (e: Exception) {
            Log.w(TAG, "recordArtistTransition error: ${e.message}")
        }
    }

    /**
     * Retrieves dynamic neighboring artists discovered from user playback & playlists.
     */
    fun getDynamicRelatedArtists(rawArtist: String, limit: Int = 6): List<String> {
        val a = rawArtist.split(Regex("[,&/]|\\bft\\.?\\b|\\bfeat\\.?\\b", RegexOption.IGNORE_CASE)).firstOrNull()?.trim() ?: rawArtist.trim()
        val graphStr = prefs.getString("artist_cooccurrence_graph", "{}") ?: "{}"
        try {
            val json = JSONObject(graphStr)
            val node = json.optJSONObject(a.lowercase()) ?: return emptyList()
            val list = mutableListOf<Pair<String, Double>>()
            val keys = node.keys()
            while (keys.hasNext()) {
                val k = keys.next()
                list.add(Pair(k, node.getDouble(k)))
            }
            return list.sortedByDescending { it.second }.take(limit).map { it.first }
        } catch (e: Exception) {
            Log.w(TAG, "getDynamicRelatedArtists error: ${e.message}")
            return emptyList()
        }
    }

    fun incrementArtistPlayCount(rawArtist: String) {
        if (rawArtist.isBlank()) return
        val artists = rawArtist.split(Regex("[,&/]|\\bft\\.?\\b|\\bfeat\\.?\\b", RegexOption.IGNORE_CASE))
            .map { it.trim() }
            .filter { it.isNotBlank() && it.length > 1 }

        val jsonStr = prefs.getString("artist_play_counts", "{}") ?: "{}"
        try {
            val json = JSONObject(jsonStr)
            for (a in artists) {
                val current = json.optInt(a, 0)
                json.put(a, current + 1)
            }
            prefs.edit().putString("artist_play_counts", json.toString()).apply()
        } catch (e: Exception) {
            Log.w(TAG, "incrementArtistPlayCount error: ${e.message}")
        }
    }

    fun incrementGenrePlayCount(genre: String) {
        if (genre.isBlank()) return
        val jsonStr = prefs.getString("genre_play_counts", "{}") ?: "{}"
        try {
            val json = JSONObject(genreJsonStr(genre))
            val current = json.optInt(genre, 0)
            json.put(genre, current + 1)
            prefs.edit().putString("genre_play_counts", json.toString()).apply()
        } catch (e: Exception) {
            Log.w(TAG, "incrementGenrePlayCount error: ${e.message}")
        }
    }

    private fun genreJsonStr(genre: String): String = prefs.getString("genre_play_counts", "{}") ?: "{}"

    fun getArtistPlayCounts(): Map<String, Int> {
        val jsonStr = prefs.getString("artist_play_counts", "{}") ?: "{}"
        val map = mutableMapOf<String, Int>()
        try {
            val json = JSONObject(jsonStr)
            val keys = json.keys()
            while (keys.hasNext()) {
                val k = keys.next()
                map[k] = json.getInt(k)
            }
        } catch (e: Exception) {
            Log.w(TAG, "getArtistPlayCounts error: ${e.message}")
        }
        return map
    }

    fun getGenrePlayCounts(): Map<String, Int> {
        val jsonStr = prefs.getString("genre_play_counts", "{}") ?: "{}"
        val map = mutableMapOf<String, Int>()
        try {
            val json = JSONObject(jsonStr)
            val keys = json.keys()
            while (keys.hasNext()) {
                val k = keys.next()
                map[k] = json.getInt(k)
            }
        } catch (e: Exception) {
            Log.w(TAG, "getGenrePlayCounts error: ${e.message}")
        }
        return map
    }

    fun getTopArtists(limit: Int = 6): List<String> {
        // 1. Decayed affinity scores (exponential half-life prioritization)
        val decayed = getDecayedArtistScores().toList().sortedByDescending { it.second }.map { it.first }
        // 2. Play counts fallback
        val counts = getArtistPlayCounts().toList().sortedByDescending { it.second }.map { it.first }
        // 3. Liked song artists
        val likedArtists = getLikedSongs().flatMap { track ->
            track.artist.split(Regex("[,&/]|\\bft\\.?\\b|\\bfeat\\.?\\b", RegexOption.IGNORE_CASE)).map { it.trim() }
        }.filter { it.isNotBlank() }
        // 4. Recent history artists
        val historyArtists = getHistory().flatMap { track ->
            track.artist.split(Regex("[,&/]|\\bft\\.?\\b|\\bfeat\\.?\\b", RegexOption.IGNORE_CASE)).map { it.trim() }
        }.filter { it.isNotBlank() }
        val (_, prefArtists) = getUserPreferences()

        val ordered = LinkedHashSet<String>()
        ordered.addAll(decayed)
        ordered.addAll(counts)
        ordered.addAll(likedArtists)
        ordered.addAll(historyArtists)
        ordered.addAll(prefArtists)
        if (ordered.isEmpty()) {
            ordered.addAll(listOf("Navaan Sandhu", "Karan Aujla", "Arijit Singh", "Diljit Dosanjh", "The Weeknd"))
        }
        return ordered.take(limit).toList()
    }

    fun getTopGenres(limit: Int = 4): List<String> {
        // 1. Decayed genre affinity
        val decayed = getDecayedGenreScores().toList().sortedByDescending { it.second }.map { it.first }
        // 2. Play counts fallback
        val counts = getGenrePlayCounts().toList().sortedByDescending { it.second }.map { it.first }
        val (prefGenres, _) = getUserPreferences()
        val ordered = LinkedHashSet<String>()
        ordered.addAll(decayed)
        ordered.addAll(counts)
        ordered.addAll(prefGenres)
        if (ordered.isEmpty()) {
            ordered.addAll(listOf("Punjabi", "Bollywood", "Pop", "Hip-Hop"))
        }
        return ordered.take(limit).toList()
    }

    fun getHistory(): List<Track> {
        val jsonStr = prefs.getString("history_cache", "[]") ?: "[]"
        return parseTracks(jsonStr)
    }

    fun clearHistory() {
        prefs.edit().putString("history_cache", "[]").apply()
        val user = authManager.currentUser
        if (user != null && !user.isGuest) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val request = Request.Builder()
                        .url("$SUPABASE_URL/rest/v1/listening_history?user_id=eq.${user.id}")
                        .header("apikey", SUPABASE_ANON_KEY)
                        .header("Authorization", "Bearer $SUPABASE_ANON_KEY")
                        .delete()
                        .build()
                    val res = client.newCall(request).execute()
                    res.close()
                } catch (e: Exception) {
                    Log.w(TAG, "clearHistory error: ${e.message}")
                }
            }
        }
    }

    // ------------------------------------------------------------------------
    // User Playlists
    // ------------------------------------------------------------------------

    fun getPlaylists(): List<Playlist> {
        val jsonStr = prefs.getString("playlists_cache", "[]") ?: "[]"
        return parsePlaylists(jsonStr)
    }

    fun savePlaylists(playlists: List<Playlist>) {
        val jsonArr = serializePlaylists(playlists)
        prefs.edit().putString("playlists_cache", jsonArr.toString()).apply()
    }

    fun createPlaylist(name: String, description: String = ""): Playlist {
        val pl = Playlist(
            id = UUID.randomUUID().toString(), // PostgreSQL playlists.id is UUID
            name = name,
            description = description,
            createdAt = System.currentTimeMillis(),
            tracks = mutableListOf()
        )
        val list = getPlaylists().toMutableList()
        list.add(0, pl)
        savePlaylists(list)
        syncInsertPlaylist(pl)
        return pl
    }

    fun deletePlaylist(playlistId: String) {
        val list = getPlaylists().toMutableList()
        list.removeAll { it.id == playlistId }
        savePlaylists(list)
        syncDeletePlaylist(playlistId)
    }

    fun renamePlaylist(playlistId: String, newName: String) {
        val list = getPlaylists().toMutableList()
        val index = list.indexOfFirst { it.id == playlistId }
        if (index >= 0) {
            list[index].name = newName
            savePlaylists(list)
            syncUpdatePlaylist(list[index])
        }
    }

    fun addTrackToPlaylist(playlistId: String, track: Track): Boolean {
        val list = getPlaylists().toMutableList()
        val pl = list.firstOrNull { it.id == playlistId } ?: return false
        if (pl.tracks.none { it.id == track.id }) {
            pl.tracks.add(track)
            savePlaylists(list)
            syncInsertPlaylistSong(playlistId, track, pl.tracks.size)
            return true
        }
        return false
    }

    fun removeTrackFromPlaylist(playlistId: String, trackId: String): Boolean {
        val list = getPlaylists().toMutableList()
        val pl = list.firstOrNull { it.id == playlistId } ?: return false
        val removed = pl.tracks.removeAll { it.id == trackId }
        if (removed) {
            savePlaylists(list)
            syncDeletePlaylistSong(playlistId, trackId)
        }
        return removed
    }

    // ------------------------------------------------------------------------
    // User Stats & Preferences ("Tune Your Soundscape")
    // ------------------------------------------------------------------------

    fun getStats(): Triple<Int, Int, Int> {
        val playlistsCount = getPlaylists().size
        val likedCount = getLikedSongs().size
        val historyCount = getHistory().size
        return Triple(playlistsCount, likedCount, historyCount)
    }

    fun getUserPreferences(): Pair<List<String>, List<String>> {
        val rawGenres = prefs.getString("user_pref_genres", "Punjabi,Bollywood,Pop,Hip-Hop") ?: "Punjabi,Bollywood,Pop,Hip-Hop"
        val rawArtists = prefs.getString("user_pref_artists", "Arijit Singh,The Weeknd,Diljit Dosanjh,Taylor Swift") ?: "Arijit Singh,The Weeknd,Diljit Dosanjh,Taylor Swift"
        val genres = rawGenres.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        val artists = rawArtists.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        return Pair(genres, artists)
    }

    fun saveUserPreferences(genres: List<String>, artists: List<String>) {
        prefs.edit()
            .putString("user_pref_genres", genres.joinToString(","))
            .putString("user_pref_artists", artists.joinToString(","))
            .apply()

        val user = authManager.currentUser
        if (user != null && !user.isGuest) {
            CoroutineScope(Dispatchers.IO).launch {
                syncPreferences(user.id, genres, artists)
            }
        }
    }

    // ------------------------------------------------------------------------
    // Cloud Synchronization with Supabase
    // ------------------------------------------------------------------------

    suspend fun syncFromSupabase(): Boolean = withContext(Dispatchers.IO) {
        val user = authManager.currentUser
        if (user == null || user.isGuest) {
            Log.d(TAG, "Skipping Supabase sync: user is null or guest.")
            return@withContext false
        }

        Log.i(TAG, "Starting comprehensive Supabase sync for user ${user.id} (${user.email})...")
        ensureProfile(user)

        var hasAnySyncSuccess = false

        // 1. Fetch & Verify Profile Data
        try {
            val profUrl = "$SUPABASE_URL/rest/v1/profiles?id=eq.${user.id}&select=id,email,display_name,avatar_url"
            val profRequest = Request.Builder()
                .url(profUrl)
                .header("apikey", SUPABASE_ANON_KEY)
                .header("Authorization", "Bearer $SUPABASE_ANON_KEY")
                .get()
                .build()

            val profRes = client.newCall(profRequest).execute()
            val profBody = profRes.body?.string() ?: "[]"
            if (profRes.isSuccessful) {
                val profArr = JSONArray(profBody)
                if (profArr.length() > 0) {
                    val pObj = profArr.getJSONObject(0)
                    val dispName = pObj.optString("display_name", user.displayName)
                    val avatar = pObj.optString("avatar_url", null)
                    if (dispName.isNotEmpty() && dispName != user.displayName) {
                        authManager.persistUser(user.copy(
                            displayName = dispName,
                            avatarUrl = if (avatar.isNullOrEmpty() || avatar == "null") user.avatarUrl as String? else avatar
                        ))
                    }
                    hasAnySyncSuccess = true
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error syncing profile: ${e.message}")
        }

        // 2. Fetch User Preferences ("Tune Your Soundscape")
        try {
            val prefUrl = "$SUPABASE_URL/rest/v1/user_preferences?user_id=eq.${user.id}&select=favorite_genres,favorite_artists"
            val prefRequest = Request.Builder()
                .url(prefUrl)
                .header("apikey", SUPABASE_ANON_KEY)
                .header("Authorization", "Bearer $SUPABASE_ANON_KEY")
                .get()
                .build()

            val prefRes = client.newCall(prefRequest).execute()
            val prefBody = prefRes.body?.string() ?: "[]"
            if (prefRes.isSuccessful) {
                val prefArr = JSONArray(prefBody)
                if (prefArr.length() > 0) {
                    val prefObj = prefArr.getJSONObject(0)
                    val genresJson = prefObj.optJSONArray("favorite_genres")
                    val artistsJson = prefObj.optJSONArray("favorite_artists")

                    val genresList = mutableListOf<String>()
                    if (genresJson != null) {
                        for (i in 0 until genresJson.length()) {
                            genresList.add(genresJson.getString(i))
                        }
                    }

                    val artistsList = mutableListOf<String>()
                    if (artistsJson != null) {
                        for (i in 0 until artistsJson.length()) {
                            val item = artistsJson.optJSONObject(i)
                            if (item != null && item.has("name")) {
                                artistsList.add(item.getString("name"))
                            } else {
                                val s = artistsJson.optString(i)
                                if (s.isNotEmpty()) artistsList.add(s)
                            }
                        }
                    }

                    if (genresList.isNotEmpty() || artistsList.isNotEmpty()) {
                        prefs.edit()
                            .putString("user_pref_genres", genresList.joinToString(","))
                            .putString("user_pref_artists", artistsList.joinToString(","))
                            .apply()
                        Log.i(TAG, "Synced preferences from Supabase: genres=$genresList, artists=$artistsList")
                        hasAnySyncSuccess = true
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error syncing user preferences: ${e.message}")
        }

        // 3. Fetch User Playlists & Songs
        try {
            val playlistsUrl = "$SUPABASE_URL/rest/v1/playlists?user_id=eq.${user.id}&select=id,name,description,created_at,playlist_songs(song_id,title,artist,artwork_url,position)&order=created_at.desc"
            val plRequest = Request.Builder()
                .url(playlistsUrl)
                .header("apikey", SUPABASE_ANON_KEY)
                .header("Authorization", "Bearer $SUPABASE_ANON_KEY")
                .get()
                .build()

            val plResponse = client.newCall(plRequest).execute()
            val plBody = plResponse.body?.string() ?: "[]"
            Log.d(TAG, "Supabase playlists status: ${plResponse.code}, body length: ${plBody.length}")

            if (plResponse.isSuccessful) {
                val parsedPlaylists = parsePlaylistsFromSupabase(plBody)
                if (parsedPlaylists.isNotEmpty()) {
                    savePlaylists(parsedPlaylists)
                    Log.i(TAG, "Successfully synced ${parsedPlaylists.size} playlists from Supabase (e.g. ${parsedPlaylists.first().name})!")
                    hasAnySyncSuccess = true
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching playlists from Supabase: ${e.message}", e)
        }

        // 4. Fetch Listening History / Recently Played
        try {
            val histUrl = "$SUPABASE_URL/rest/v1/listening_history?user_id=eq.${user.id}&order=played_at.desc&limit=50"
            val histRequest = Request.Builder()
                .url(histUrl)
                .header("apikey", SUPABASE_ANON_KEY)
                .header("Authorization", "Bearer $SUPABASE_ANON_KEY")
                .get()
                .build()

            val histRes = client.newCall(histRequest).execute()
            val histBody = histRes.body?.string() ?: "[]"
            if (histRes.isSuccessful) {
                val histArr = JSONArray(histBody)
                val syncedHistory = mutableListOf<Track>()
                for (i in 0 until histArr.length()) {
                    val row = histArr.getJSONObject(i)
                    val sId = row.optString("song_id", "")
                    val title = row.optString("title", "")
                    val artist = row.optString("artist", "")
                    val artwork = row.optString("artwork_url", "")
                    val genre = row.optString("genre", "Soundscape")
                    if (sId.isNotEmpty() && title.isNotEmpty()) {
                        if (syncedHistory.none { it.id == sId || (it.title.equals(title, ignoreCase = true) && it.artist.equals(artist, ignoreCase = true)) }) {
                            syncedHistory.add(
                                Track(
                                    id = sId,
                                    title = title,
                                    artist = artist,
                                    album = genre,
                                    thumbnail = artwork,
                                    audioUrl = "",
                                    duration = "--:--",
                                    durationSec = 0
                                )
                            )
                        }
                    }
                }
                if (syncedHistory.isNotEmpty()) {
                    prefs.edit().putString("history_cache", serializeTracks(syncedHistory).toString()).apply()
                    Log.i(TAG, "Successfully synced ${syncedHistory.size} history tracks from Supabase!")
                    hasAnySyncSuccess = true
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error syncing listening history: ${e.message}")
        }

        // 5. Fetch Liked Songs from Supabase
        try {
            val likedUrl = "$SUPABASE_URL/rest/v1/liked_songs?user_id=eq.${user.id}&order=liked_at.desc"
            val request = Request.Builder()
                .url(likedUrl)
                .header("apikey", SUPABASE_ANON_KEY)
                .header("Authorization", "Bearer $SUPABASE_ANON_KEY")
                .get()
                .build()

            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: "[]"
            Log.d(TAG, "Supabase liked_songs status: ${response.code}, body length: ${body.length}")

            if (response.isSuccessful) {
                val jsonArr = JSONArray(body)
                val songs = mutableListOf<Track>()
                for (i in 0 until jsonArr.length()) {
                    val row = jsonArr.getJSONObject(i)
                    val songId = row.optString("song_id", "")
                    val title = row.optString("title", "Unknown Title")
                    val artist = row.optString("artist", "Unknown Artist")
                    val artwork = row.optString("artwork_url", "")

                    if (songId.isNotEmpty()) {
                        songs.add(
                            Track(
                                id = songId,
                                title = title,
                                artist = artist,
                                album = row.optString("album", "EMBER Soundscape"),
                                thumbnail = artwork,
                                audioUrl = "",
                                duration = "--:--",
                                durationSec = 0,
                                isLiked = true
                            )
                        )
                    }
                }

                if (songs.isNotEmpty()) {
                    saveLikedSongs(songs)
                    Log.i(TAG, "Successfully synced ${songs.size} liked songs from Supabase!")
                    hasAnySyncSuccess = true
                } else {
                    // If cloud liked songs is empty, check if we have local liked songs to upload
                    val localLiked = getLikedSongs()
                    if (localLiked.isNotEmpty()) {
                        Log.i(TAG, "Uploading ${localLiked.size} local liked songs to Supabase...")
                        for (track in localLiked) {
                            syncInsertLikedSong(track)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching liked_songs from Supabase: ${e.message}", e)
        }

        return@withContext hasAnySyncSuccess
    }

    private fun ensureProfile(user: User) {
        if (user.isGuest) return
        try {
            val payload = JSONObject().apply {
                put("id", user.id)
                put("email", user.email)
                put("display_name", user.displayName)
                if (!user.avatarUrl.isNullOrEmpty() && user.avatarUrl != "null") {
                    put("avatar_url", user.avatarUrl)
                }
            }

            val mediaType = "application/json".toMediaType()
            val request = Request.Builder()
                .url("$SUPABASE_URL/rest/v1/profiles")
                .header("apikey", SUPABASE_ANON_KEY)
                .header("Authorization", "Bearer $SUPABASE_ANON_KEY")
                .header("Prefer", "resolution=merge-duplicates")
                .post(payload.toString().toRequestBody(mediaType))
                .build()

            val res = client.newCall(request).execute()
            res.close()
        } catch (e: Exception) {
            Log.w(TAG, "ensureProfile warning: ${e.message}")
        }
    }

    private fun syncPreferences(userId: String, genres: List<String>, artists: List<String>) {
        try {
            val genreArr = JSONArray(genres)
            val artistArr = JSONArray()
            for (a in artists) {
                artistArr.put(JSONObject().apply { put("name", a) })
            }

            val payload = JSONObject().apply {
                put("user_id", userId)
                put("favorite_genres", genreArr)
                put("favorite_artists", artistArr)
                put("updated_at", SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
                    timeZone = TimeZone.getTimeZone("UTC")
                }.format(Date()))
            }

            val mediaType = "application/json".toMediaType()
            val request = Request.Builder()
                .url("$SUPABASE_URL/rest/v1/user_preferences")
                .header("apikey", SUPABASE_ANON_KEY)
                .header("Authorization", "Bearer $SUPABASE_ANON_KEY")
                .header("Prefer", "resolution=merge-duplicates")
                .post(payload.toString().toRequestBody(mediaType))
                .build()

            val res = client.newCall(request).execute()
            res.close()
        } catch (e: Exception) {
            Log.w(TAG, "syncPreferences error: ${e.message}")
        }
    }

    private fun syncInsertLikedSong(track: Track) {
        val user = authManager.currentUser ?: return
        if (user.isGuest) return

        CoroutineScope(Dispatchers.IO).launch {
            try {
                ensureProfile(user)
                val isoDate = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
                    timeZone = TimeZone.getTimeZone("UTC")
                }.format(Date())

                val payload = JSONObject().apply {
                    put("user_id", user.id)
                    put("song_id", track.id)
                    put("title", track.title)
                    put("artist", track.artist)
                    put("artwork_url", track.thumbnail)
                    put("liked_at", isoDate)
                }

                val mediaType = "application/json".toMediaType()
                val request = Request.Builder()
                    .url("$SUPABASE_URL/rest/v1/liked_songs")
                    .header("apikey", SUPABASE_ANON_KEY)
                    .header("Authorization", "Bearer $SUPABASE_ANON_KEY")
                    .header("Prefer", "resolution=merge-duplicates")
                    .post(payload.toString().toRequestBody(mediaType))
                    .build()

                val res = client.newCall(request).execute()
                res.close()
            } catch (e: Exception) {
                Log.w(TAG, "Cloud sync like error: ${e.message}")
            }
        }
    }

    private fun syncDeleteLikedSong(songId: String) {
        val user = authManager.currentUser ?: return
        if (user.isGuest) return

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val request = Request.Builder()
                    .url("$SUPABASE_URL/rest/v1/liked_songs?user_id=eq.${user.id}&song_id=eq.$songId")
                    .header("apikey", SUPABASE_ANON_KEY)
                    .header("Authorization", "Bearer $SUPABASE_ANON_KEY")
                    .delete()
                    .build()

                val res = client.newCall(request).execute()
                res.close()
            } catch (e: Exception) {
                Log.w(TAG, "Cloud sync delete like error: ${e.message}")
            }
        }
    }

    private fun syncInsertHistory(track: Track) {
        val user = authManager.currentUser ?: return
        if (user.isGuest) return

        CoroutineScope(Dispatchers.IO).launch {
            try {
                ensureProfile(user)
                val isoDate = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
                    timeZone = TimeZone.getTimeZone("UTC")
                }.format(Date())

                val payload = JSONObject().apply {
                    put("user_id", user.id)
                    put("song_id", track.id)
                    put("title", track.title)
                    put("artist", track.artist)
                    put("artwork_url", track.thumbnail)
                    put("genre", if (track.album.isNotEmpty()) track.album else "Soundscape")
                    put("played_at", isoDate)
                    put("duration_played", 5)
                }

                val mediaType = "application/json".toMediaType()
                val request = Request.Builder()
                    .url("$SUPABASE_URL/rest/v1/listening_history")
                    .header("apikey", SUPABASE_ANON_KEY)
                    .header("Authorization", "Bearer $SUPABASE_ANON_KEY")
                    .post(payload.toString().toRequestBody(mediaType))
                    .build()

                val res = client.newCall(request).execute()
                res.close()
            } catch (e: Exception) {
                Log.w(TAG, "Cloud sync history error: ${e.message}")
            }
        }
    }

    private fun syncInsertPlaylist(playlist: Playlist) {
        val user = authManager.currentUser ?: return
        if (user.isGuest) return

        CoroutineScope(Dispatchers.IO).launch {
            try {
                ensureProfile(user)
                val payload = JSONObject().apply {
                    put("id", playlist.id)
                    put("user_id", user.id)
                    put("name", playlist.name)
                    put("description", playlist.description)
                }

                val mediaType = "application/json".toMediaType()
                val request = Request.Builder()
                    .url("$SUPABASE_URL/rest/v1/playlists")
                    .header("apikey", SUPABASE_ANON_KEY)
                    .header("Authorization", "Bearer $SUPABASE_ANON_KEY")
                    .header("Prefer", "resolution=merge-duplicates")
                    .post(payload.toString().toRequestBody(mediaType))
                    .build()

                val res = client.newCall(request).execute()
                res.close()
            } catch (e: Exception) {
                Log.w(TAG, "Cloud sync insert playlist error: ${e.message}")
            }
        }
    }

    private fun syncUpdatePlaylist(playlist: Playlist) {
        val user = authManager.currentUser ?: return
        if (user.isGuest) return

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val payload = JSONObject().apply {
                    put("name", playlist.name)
                    put("description", playlist.description)
                    put("updated_at", SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
                        timeZone = TimeZone.getTimeZone("UTC")
                    }.format(Date()))
                }

                val mediaType = "application/json".toMediaType()
                val request = Request.Builder()
                    .url("$SUPABASE_URL/rest/v1/playlists?id=eq.${playlist.id}&user_id=eq.${user.id}")
                    .header("apikey", SUPABASE_ANON_KEY)
                    .header("Authorization", "Bearer $SUPABASE_ANON_KEY")
                    .patch(payload.toString().toRequestBody(mediaType))
                    .build()

                val res = client.newCall(request).execute()
                res.close()
            } catch (e: Exception) {
                Log.w(TAG, "Cloud sync update playlist error: ${e.message}")
            }
        }
    }

    private fun syncDeletePlaylist(playlistId: String) {
        val user = authManager.currentUser ?: return
        if (user.isGuest) return

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Delete child playlist_songs first
                val delSongsReq = Request.Builder()
                    .url("$SUPABASE_URL/rest/v1/playlist_songs?playlist_id=eq.$playlistId")
                    .header("apikey", SUPABASE_ANON_KEY)
                    .header("Authorization", "Bearer $SUPABASE_ANON_KEY")
                    .delete()
                    .build()
                client.newCall(delSongsReq).execute().close()

                val request = Request.Builder()
                    .url("$SUPABASE_URL/rest/v1/playlists?id=eq.$playlistId&user_id=eq.${user.id}")
                    .header("apikey", SUPABASE_ANON_KEY)
                    .header("Authorization", "Bearer $SUPABASE_ANON_KEY")
                    .delete()
                    .build()

                val res = client.newCall(request).execute()
                res.close()
            } catch (e: Exception) {
                Log.w(TAG, "Cloud sync delete playlist error: ${e.message}")
            }
        }
    }

    private fun syncInsertPlaylistSong(playlistId: String, track: Track, position: Int) {
        val user = authManager.currentUser ?: return
        if (user.isGuest) return

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val payload = JSONObject().apply {
                    put("playlist_id", playlistId)
                    put("song_id", track.id)
                    put("title", track.title)
                    put("artist", track.artist)
                    put("artwork_url", track.thumbnail)
                    put("position", position)
                }

                val mediaType = "application/json".toMediaType()
                val request = Request.Builder()
                    .url("$SUPABASE_URL/rest/v1/playlist_songs")
                    .header("apikey", SUPABASE_ANON_KEY)
                    .header("Authorization", "Bearer $SUPABASE_ANON_KEY")
                    .header("Prefer", "resolution=merge-duplicates")
                    .post(payload.toString().toRequestBody(mediaType))
                    .build()

                val res = client.newCall(request).execute()
                res.close()
            } catch (e: Exception) {
                Log.w(TAG, "Cloud sync insert playlist song error: ${e.message}")
            }
        }
    }

    private fun syncDeletePlaylistSong(playlistId: String, songId: String) {
        val user = authManager.currentUser ?: return
        if (user.isGuest) return

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val request = Request.Builder()
                    .url("$SUPABASE_URL/rest/v1/playlist_songs?playlist_id=eq.$playlistId&song_id=eq.$songId")
                    .header("apikey", SUPABASE_ANON_KEY)
                    .header("Authorization", "Bearer $SUPABASE_ANON_KEY")
                    .delete()
                    .build()

                val res = client.newCall(request).execute()
                res.close()
            } catch (e: Exception) {
                Log.w(TAG, "Cloud sync delete playlist song error: ${e.message}")
            }
        }
    }

    // ------------------------------------------------------------------------
    // JSON Serialization Helpers
    // ------------------------------------------------------------------------

    private fun parseTracks(jsonStr: String): List<Track> {
        val list = mutableListOf<Track>()
        try {
            val jsonArr = JSONArray(jsonStr)
            for (i in 0 until jsonArr.length()) {
                val obj = jsonArr.getJSONObject(i)
                list.add(
                    Track(
                        id = obj.getString("id"),
                        title = obj.getString("title"),
                        artist = obj.getString("artist"),
                        album = obj.optString("album", "EMBER Soundscape"),
                        thumbnail = obj.optString("thumbnail", ""),
                        audioUrl = obj.optString("audioUrl", ""),
                        duration = obj.optString("duration", "--:--"),
                        durationSec = obj.optInt("durationSec", 0),
                        isLiked = obj.optBoolean("isLiked", true),
                        isYoutubeFallback = obj.optBoolean("isYoutubeFallback", false),
                        youtubeVideoId = obj.optString("youtubeVideoId", "").ifEmpty { null },
                        source = obj.optString("source", "ytmusic"),
                        localAudioPath = obj.optString("localAudioPath", "").ifEmpty { null },
                        localThumbnailPath = obj.optString("localThumbnailPath", "").ifEmpty { null },
                        isDownloaded = obj.optBoolean("isDownloaded", false),
                        downloadTimeMs = obj.optLong("downloadTimeMs", 0L),
                        fileSize = obj.optLong("fileSize", 0L)
                    )
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse tracks JSON: ${e.message}")
        }
        return list
    }

    private fun serializeTracks(tracks: List<Track>): JSONArray {
        val jsonArr = JSONArray()
        for (track in tracks) {
            val obj = JSONObject().apply {
                put("id", track.id)
                put("title", track.title)
                put("artist", track.artist)
                put("album", track.album)
                put("thumbnail", track.thumbnail)
                put("audioUrl", track.audioUrl)
                put("duration", track.duration)
                put("durationSec", track.durationSec)
                put("isLiked", track.isLiked)
                put("isYoutubeFallback", track.isYoutubeFallback)
                put("youtubeVideoId", track.youtubeVideoId ?: "")
                put("source", track.source)
                put("localAudioPath", track.localAudioPath ?: "")
                put("localThumbnailPath", track.localThumbnailPath ?: "")
                put("isDownloaded", track.isDownloaded)
                put("downloadTimeMs", track.downloadTimeMs)
                put("fileSize", track.fileSize)
            }
            jsonArr.put(obj)
        }
        return jsonArr
    }

    private fun parsePlaylists(jsonStr: String): List<Playlist> {
        val list = mutableListOf<Playlist>()
        try {
            val jsonArr = JSONArray(jsonStr)
            for (i in 0 until jsonArr.length()) {
                val obj = jsonArr.getJSONObject(i)
                val id = obj.optString("id", UUID.randomUUID().toString())
                val name = obj.optString("name", "Custom Playlist")
                val desc = obj.optString("description", "")
                val createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                val tracksArr = obj.optJSONArray("tracks") ?: JSONArray()
                val tracks = parseTracks(tracksArr.toString()).toMutableList()

                list.add(
                    Playlist(
                        id = id,
                        name = name,
                        description = desc,
                        createdAt = createdAt,
                        tracks = tracks
                    )
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse playlists JSON: ${e.message}")
        }
        return list
    }

    private fun parsePlaylistsFromSupabase(jsonStr: String): List<Playlist> {
        val list = mutableListOf<Playlist>()
        try {
            val jsonArr = JSONArray(jsonStr)
            for (i in 0 until jsonArr.length()) {
                val obj = jsonArr.getJSONObject(i)
                val id = obj.optString("id", UUID.randomUUID().toString())
                val name = obj.optString("name", "Playlist")
                val desc = obj.optString("description", "")

                val songsArr = obj.optJSONArray("playlist_songs") ?: JSONArray()
                val tracks = mutableListOf<Track>()
                for (j in 0 until songsArr.length()) {
                    val sObj = songsArr.getJSONObject(j)
                    val sId = sObj.optString("song_id", "")
                    if (sId.isNotEmpty()) {
                        tracks.add(
                            Track(
                                id = sId,
                                title = sObj.optString("title", "Track"),
                                artist = sObj.optString("artist", "Artist"),
                                album = "EMBER Playlist",
                                thumbnail = sObj.optString("artwork_url", ""),
                                audioUrl = "",
                                duration = "--:--",
                                durationSec = 0
                            )
                        )
                    }
                }

                list.add(
                    Playlist(
                        id = id,
                        name = name,
                        description = desc,
                        createdAt = System.currentTimeMillis(),
                        tracks = tracks
                    )
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse supabase playlists JSON: ${e.message}")
        }
        return list
    }

    private fun serializePlaylists(playlists: List<Playlist>): JSONArray {
        val jsonArr = JSONArray()
        for (pl in playlists) {
            val obj = JSONObject().apply {
                put("id", pl.id)
                put("name", pl.name)
                put("description", pl.description)
                put("createdAt", pl.createdAt)
                put("tracks", serializeTracks(pl.tracks))
            }
            jsonArr.put(obj)
        }
        return jsonArr
    }


    // ------------------------------------------------------------------------
    // Last Playback State Persistence (App Close & Resume)
    // ------------------------------------------------------------------------

    data class LastPlaybackState(
        val track: Track,
        val positionMs: Long,
        val queue: List<Track>,
        val queueIndex: Int
    )

    fun saveLastPlaybackState(track: Track, positionMs: Long, queue: List<Track>, queueIndex: Int) {
        try {
            val trackObj = JSONObject().apply {
                put("id", track.id)
                put("title", track.title)
                put("artist", track.artist)
                put("album", track.album)
                put("thumbnail", track.thumbnail)
                put("audioUrl", track.audioUrl)
                put("duration", track.duration)
                put("durationSec", track.durationSec)
                put("isLiked", track.isLiked)
                put("isYoutubeFallback", track.isYoutubeFallback)
            }
            val queueArr = serializeTracks(queue)
            prefs.edit()
                .putString("last_played_track", trackObj.toString())
                .putLong("last_played_position", positionMs)
                .putString("last_played_queue", queueArr.toString())
                .putInt("last_played_index", queueIndex)
                .apply()
        } catch (e: Exception) {
            Log.e(TAG, "Error saving playback state: ${e.message}")
        }
    }

    fun updateLastPlaybackPosition(positionMs: Long) {
        if (positionMs > 0) {
            prefs.edit().putLong("last_played_position", positionMs).apply()
        }
    }

    fun getLastPlaybackState(): LastPlaybackState? {
        try {
            val trackStr = prefs.getString("last_played_track", null) ?: return null
            val obj = JSONObject(trackStr)
            val track = Track(
                id = obj.getString("id"),
                title = obj.getString("title"),
                artist = obj.getString("artist"),
                album = obj.optString("album", "EMBER Soundscape"),
                thumbnail = obj.getString("thumbnail"),
                audioUrl = obj.optString("audioUrl", ""),
                duration = obj.optString("duration", "--:--"),
                durationSec = obj.optInt("durationSec", 0),
                isLiked = obj.optBoolean("isLiked", false),
                isYoutubeFallback = obj.optBoolean("isYoutubeFallback", false)
            )
            val pos = prefs.getLong("last_played_position", 0L)
            val queueStr = prefs.getString("last_played_queue", "[]") ?: "[]"
            val queue = parseTracks(queueStr)
            val idx = prefs.getInt("last_played_index", 0)
            return LastPlaybackState(track, pos, if (queue.isNotEmpty()) queue else listOf(track), idx)
        } catch (e: Exception) {
            Log.e(TAG, "Error loading playback state: ${e.message}")
            return null
        }
    }

    // ------------------------------------------------------------------------
    // Recent Searches
    // ------------------------------------------------------------------------

    fun getRecentSearches(): List<String> {
        val jsonStr = prefs.getString("recent_searches_list", "[]") ?: "[]"
        val list = mutableListOf<String>()
        try {
            val jsonArr = JSONArray(jsonStr)
            for (i in 0 until jsonArr.length()) {
                val q = jsonArr.optString(i, "").trim()
                if (q.isNotEmpty()) list.add(q)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse recent searches: ${e.message}")
        }
        return list
    }

    fun addRecentSearch(query: String) {
        val clean = query.trim()
        if (clean.isEmpty()) return
        val list = getRecentSearches().toMutableList()
        list.removeAll { it.equals(clean, ignoreCase = true) }
        list.add(0, clean)
        val trimmed = if (list.size > 10) list.subList(0, 10) else list
        val arr = JSONArray(trimmed)
        prefs.edit().putString("recent_searches_list", arr.toString()).apply()
    }

    fun removeRecentSearch(query: String) {
        val clean = query.trim()
        val list = getRecentSearches().toMutableList()
        list.removeAll { it.equals(clean, ignoreCase = true) }
        val arr = JSONArray(list)
        prefs.edit().putString("recent_searches_list", arr.toString()).apply()
    }

    fun clearRecentSearches() {
        prefs.edit().remove("recent_searches_list").apply()
    }
}
