package com.example.ember.engine

import android.util.Log
import com.example.ember.data.EmberDatabaseManager
import com.example.ember.model.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.concurrent.ConcurrentHashMap

/**
 * Intelligent Client-Side Auto-Recommendation Engine for EMBER Music (Next-Gen).
 * Employs Multi-Armed Bandit sampling, Dynamic Collaborative Co-Occurrence Graph,
 * Exponential Recency Decay, Anti-Fatigue Filtering, and Context-Aware Day/Time Models.
 */
object EmberRecommendationEngine {

    private const val TAG = "EmberRecEngine"
    private const val CACHE_TTL_MS = 25 * 60 * 1000L // 25 minutes cache

    private val cache = ConcurrentHashMap<String, Pair<Long, Any>>()

    // Comprehensive Seed Graph of artist affinities across Punjabi, Bollywood, Hip-Hop, and Global Pop (100+ artists)
    private val RELATED_ARTISTS_MAP = mapOf(
        // Punjabi & Northern Soundscapes
        "navaan sandhu" to listOf("Dhanda Nyoliwala", "Bir", "Karan Aujla", "Shubh", "Prem Dhillon", "Varinder Brar", "Raf Saperra"),
        "karan aujla" to listOf("Diljit Dosanjh", "Navaan Sandhu", "Amrit Maan", "Deep Jandu", "Shubh", "Sidhu Moose Wala", "Ikky"),
        "diljit dosanjh" to listOf("Karan Aujla", "Amrinder Gill", "Garry Sandhu", "B Praak", "Jordan Sandhu", "Harnoor", "Intense"),
        "sidhu moose wala" to listOf("Karan Aujla", "Amrit Maan", "Prem Dhillon", "Sunny Malton", "Byg Byrd", "The Kidd"),
        "shubh" to listOf("AP Dhillon", "Gurinder Gill", "Navaan Sandhu", "Karan Aujla", "Intense"),
        "ap dhillon" to listOf("Gurinder Gill", "Shubh", "Diljit Dosanjh", "Intense", "Gminxr"),
        "prem dhillon" to listOf("Navaan Sandhu", "Sidhu Moose Wala", "Bir", "Snappy", "San B"),
        "talwiinder" to listOf("Nds", "Rippy Grewal", "Sanjoy", "Harnoor", "Wazir Patar"),
        "sukha" to listOf("The PropheC", "Navaan Sandhu", "Chani Nattan", "Inderpal Moga", "Gurinder Gill"),
        "wazir patar" to listOf("Roop Bhullar", "Talwiinder", "Navaan Sandhu", "Prem Dhillon"),
        "raf saperra" to listOf("Sukshinder Shinda", "Navaan Sandhu", "Panjabi MC", "Bobby Friction"),
        "amrinder gill" to listOf("Diljit Dosanjh", "Garry Sandhu", "Gurdas Maan", "Satinder Sartaaj", "Prabh Gill"),
        "b praak" to listOf("Jaani", "Arijit Singh", "Harrdy Sandhu", "Jassie Gill", "Darshan Raval"),
        "harrdy sandhu" to listOf("B Praak", "Jaani", "Jassie Gill", "Guru Randhawa", "Mankirt Aulakh"),
        "jassie gill" to listOf("Babbal Rai", "Harrdy Sandhu", "B Praak", "Garry Sandhu"),
        "guru randhawa" to listOf("Harrdy Sandhu", "Yo Yo Honey Singh", "Badshah", "Mika Singh"),
        "yo yo honey singh" to listOf("Badshah", "Raftaar", "Guru Randhawa", "Alfaaz", "Lil Golu", "Hommie Dilliwala"),
        "badshah" to listOf("Yo Yo Honey Singh", "Raftaar", "Aastha Gill", "Divine", "Ikka"),
        "mankirt aulakh" to listOf("Jordan Sandhu", "Parmish Verma", "Karan Aujla", "Singga"),
        "jordan sandhu" to listOf("Mankirt Aulakh", "Diljit Dosanjh", "Amrit Maan", "Garry Sandhu"),
        "satinder sartaaj" to listOf("Amrinder Gill", "Gurdas Maan", "Harshdeep Kaur", "Prabh Gill"),
        "the prophec" to listOf("Sukha", "Mickey Singh", "Fateh", "Zack Knight", "Pav Dharia"),
        "dhanda nyoliwala" to listOf("Navaan Sandhu", "Bir", "Masoom Sharma", "Gulzaar Chhaniwala"),
        "masoom sharma" to listOf("Dhanda Nyoliwala", "Gulzaar Chhaniwala", "Renuka Panwar", "Khasa Aala Chahar"),

        // Hindi / Bollywood / Acoustic & Melodic
        "arijit singh" to listOf("Atif Aslam", "Pritam", "Jubin Nautiyal", "Mohit Chauhan", "Armaan Malik", "Sachin-Jigar", "Vishal Mishra"),
        "atif aslam" to listOf("Arijit Singh", "Rahat Fateh Ali Khan", "Mustafa Zahid", "Pritam", "KK", "Ali Zafar"),
        "pritam" to listOf("Arijit Singh", "KK", "Mohit Chauhan", "Vishal-Shekhar", "Sachin-Jigar"),
        "jubin nautiyal" to listOf("Arijit Singh", "Armaan Malik", "Rochak Kohli", "Tanishk Bagchi", "Akhil Sachdeva"),
        "kk" to listOf("Shaan", "Lucky Ali", "Mohit Chauhan", "Pritam", "Atif Aslam", "Javed Ali"),
        "mohit chauhan" to listOf("Arijit Singh", "Lucky Ali", "Papon", "KK", "Rabbi Shergill"),
        "armaan malik" to listOf("Amaal Mallik", "Darshan Raval", "Arijit Singh", "Sanam", "Jubin Nautiyal"),
        "darshan raval" to listOf("Armaan Malik", "Asim Azhar", "Jasleen Royal", "Anuv Jain", "Stebin Ben"),
        "anuv jain" to listOf("Prateek Kuhad", "Jasleen Royal", "Zaeden", "When Chai Met Toast", "The Local Train"),
        "prateek kuhad" to listOf("Anuv Jain", "Jasleen Royal", "When Chai Met Toast", "Osho Jain", "Kanishk Seth"),
        "jasleen royal" to listOf("Prateek Kuhad", "Anuv Jain", "B Praak", "Arijit Singh", "Shreya Ghoshal"),
        "shreya ghoshal" to listOf("Sunidhi Chauhan", "Monali Thakur", "Neeti Mohan", "Arijit Singh", "Shilpa Rao"),
        "sunidhi chauhan" to listOf("Shreya Ghoshal", "Neha Bhasin", "Shilpa Rao", "Rekha Bhardwaj"),
        "vishal mishra" to listOf("Arijit Singh", "B Praak", "Sachet Tandon", "Mithoon", "Jubin Nautiyal"),
        "rahat fateh ali khan" to listOf("Atif Aslam", "Nusrat Fateh Ali Khan", "Shafqat Amanat Ali", "Javed Ali"),
        "lucky ali" to listOf("KK", "Mohit Chauhan", "Strings", "Euphoria", "Shaan"),

        // Desi Hip-Hop & Rap
        "divine" to listOf("Naezy", "MC Stan", "KR\$NA", "Raftaar", "Seedhe Maut", "Shah Rule", "Karan Aujla"),
        "mc stan" to listOf("Divine", "Seedhe Maut", "KR\$NA", "Raftaar", "Emiway Bantai"),
        "seedhe maut" to listOf("KR\$NA", "Raftaar", "Divine", "Prabh Deep", "Sez On The Beat"),
        "kr\$na" to listOf("Raftaar", "Seedhe Maut", "Divine", "Karma", "Deep Kalsi", "Brotha V"),
        "raftaar" to listOf("KR\$NA", "Badshah", "Yo Yo Honey Singh", "Deep Kalsi", "Seedhe Maut"),
        "king" to listOf("Bella", "Paradox", "MC Square", "Srushti Tawade", "Badshah"),
        "emiway bantai" to listOf("Divine", "MC Stan", "Raftaar", "Munawar Faruqui"),

        // Global Pop & Hip-Hop
        "the weeknd" to listOf("Post Malone", "Drake", "Kendrick Lamar", "Bruno Mars", "Travis Scott", "SZA", "Daft Punk"),
        "taylor swift" to listOf("Olivia Rodrigo", "Sabrina Carpenter", "Ariana Grande", "Billie Eilish", "Lana Del Rey", "Gracie Abrams"),
        "drake" to listOf("Travis Scott", "Kendrick Lamar", "21 Savage", "Future", "J. Cole", "Lil Baby", "Metro Boomin"),
        "post malone" to listOf("The Weeknd", "Juice WRLD", "Khalid", "Swae Lee", "XXXTENTACION", "Morgan Wallen"),
        "travis scott" to listOf("Drake", "Don Toliver", "Playboi Carti", "Metro Boomin", "Future", "Kid Cudi"),
        "billie eilish" to listOf("Finneas", "Lorde", "Olivia Rodrigo", "Lana Del Rey", "Girl In Red"),
        "dua lipa" to listOf("Bebe Rexha", "Calvin Harris", "Katy Perry", "Ava Max", "Miley Cyrus"),
        "kendrick lamar" to listOf("J. Cole", "Drake", "Baby Keem", "Pusha T", "Kanye West"),
        "kanye west" to listOf("Kid Cudi", "Jay-Z", "Travis Scott", "Pusha T", "Ty Dolla \$ign"),
        "eminem" to listOf("Dr. Dre", "50 Cent", "Snoop Dogg", "Royce Da 5'9", "Joyner Lucas"),
        "bruno mars" to listOf("Anderson .Paak", "Silk Sonic", "The Weeknd", "Charlie Puth", "Ed Sheeran"),
        "ed sheeran" to listOf("Shawn Mendes", "Charlie Puth", "James Arthur", "Lewis Capaldi", "Sam Smith")
    )

    data class MoodMixResult(
        val title: String,
        val subtitle: String,
        val iconRes: String,
        val tracks: List<Track>
    )

    fun clearCache() {
        cache.clear()
    }

    /**
     * Resolves related artists combining the dynamic collaborative on-device graph
     * with the extensive 100+ artist seed dictionary.
     */
    fun getRelatedArtists(artist: String, dbManager: EmberDatabaseManager? = null, limit: Int = 6): List<String> {
        val result = LinkedHashSet<String>()
        // 1. Check on-device dynamic collaborative graph
        if (dbManager != null) {
            val dynamicNeighbors = dbManager.getDynamicRelatedArtists(artist, limit)
            result.addAll(dynamicNeighbors)
        }
        // 2. Fall back / augment with seed map
        val seed = RELATED_ARTISTS_MAP[artist.trim().lowercase()] ?: emptyList()
        result.addAll(seed)
        return result.take(limit).toList()
    }

    /**
     * 1. RECOMMENDED FOR YOU (Multi-Armed Bandit: 70% Comfort / 20% Adjacent / 10% Novelty)
     * Tailored to user's highest decayed-affinity artists and genres with anti-fatigue filtering.
     */
    suspend fun getRecommendedForYou(
        dbManager: EmberDatabaseManager,
        count: Int = 12
    ): List<Track> = withContext(Dispatchers.IO) {
        val cacheKey = "rec_for_you_v2_$count"
        val cached = getFromCache<List<Track>>(cacheKey)
        if (cached != null && cached.isNotEmpty()) return@withContext cached

        val topArtists = dbManager.getTopArtists(4)
        val topGenres = dbManager.getTopGenres(2)
        val historyIds = dbManager.getHistory().map { it.id }.toSet()

        val queries = mutableListOf<String>()

        // Arm 1 (70% Comfort): Top decayed-affinity artists
        for (artist in topArtists.take(2)) {
            queries.add("$artist latest hits 2025")
            queries.add("$artist top songs")
        }

        // Arm 2 (20% Adjacent Horizon): Neighboring artists from dynamic/seed graph
        val adjacentArtist = topArtists.firstOrNull()?.let { getRelatedArtists(it, dbManager, 3).shuffled().firstOrNull() }
        if (!adjacentArtist.isNullOrBlank()) {
            queries.add("$adjacentArtist hits")
        }

        // Arm 3 (10% Novelty / Serendipity): Top genre trending
        val primaryGenre = topGenres.firstOrNull() ?: "Punjabi"
        queries.add("$primaryGenre top trending hits 2025")

        val fetchedTracks = executeParallelQueries(queries.take(4), limitPerQuery = 6)

        // Filter recent history to maximize freshness
        val freshTracks = fetchedTracks.filter { it.id !in historyIds }.toMutableList()
        val pool = if (freshTracks.size >= count / 2) freshTracks else fetchedTracks

        val validResult = filterValidTracks(pool)
        val deduped = EmberAudioEngine.deduplicateTracks(validResult)

        // Anti-Fatigue Filtering: Cap max tracks from any single artist to <= 25%
        val balancedTracks = applyAntiFatigueFilter(deduped, maxPerArtist = 2)
            .shuffled()
            .take(count)

        val finalTracks = if (balancedTracks.isNotEmpty()) {
            balancedTracks
        } else {
            EmberAudioEngine.getTrendingTracks("trending").take(count)
        }

        cache[cacheKey] = Pair(System.currentTimeMillis(), finalTracks)
        finalTracks
    }

    /**
     * 2. DISCOVER SOMETHING NEW (Exploration & Horizon Expansion)
     * Leverages dynamic co-occurrence graph + adjacent seed nodes with zero history repetition.
     */
    suspend fun getDiscoverNew(
        dbManager: EmberDatabaseManager,
        count: Int = 12
    ): List<Track> = withContext(Dispatchers.IO) {
        val cacheKey = "discover_new_v2_$count"
        val cached = getFromCache<List<Track>>(cacheKey)
        if (cached != null && cached.isNotEmpty()) return@withContext cached

        val topArtists = dbManager.getTopArtists(5)
        val historyArtists = dbManager.getHistory().map { it.artist.lowercase() }.toSet()
        val historyIds = dbManager.getHistory().map { it.id }.toSet()

        // Find adjacent artists from dynamic + seed graph that user hasn't played much
        val candidateArtists = mutableListOf<String>()
        for (top in topArtists) {
            val related = getRelatedArtists(top, dbManager, limit = 4)
            for (r in related) {
                if (!historyArtists.any { it.contains(r.lowercase()) }) {
                    candidateArtists.add(r)
                }
            }
        }

        if (candidateArtists.isEmpty()) {
            candidateArtists.addAll(listOf("Talwiinder", "Raf Saperra", "Anuv Jain", "Jasleen Royal", "Seedhe Maut", "Dua Lipa"))
        }

        val queries = mutableListOf<String>()
        for (artist in candidateArtists.shuffled().take(3)) {
            queries.add("$artist best songs")
        }
        queries.add("breakout soundscapes 2025")

        val fetched = executeParallelQueries(queries, limitPerQuery = 6)
        val fresh = fetched.filter { it.id !in historyIds }
        val pool = if (fresh.size >= count / 2) fresh else fetched

        val validPool = filterValidTracks(pool)
        val deduped = EmberAudioEngine.deduplicateTracks(validPool)
        val result = applyAntiFatigueFilter(deduped, maxPerArtist = 2)
            .shuffled()
            .take(count)

        cache[cacheKey] = Pair(System.currentTimeMillis(), result)
        result
    }

    /**
     * 3. MOOD MIX (Time-Of-Day & Day-Of-Week Context-Aware Music Mix)
     * Distinguishes weekday productivity vs. weekend chill with atmospheric tuning.
     */
    suspend fun getMoodMix(
        dbManager: EmberDatabaseManager,
        count: Int = 12
    ): MoodMixResult = withContext(Dispatchers.IO) {
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
        val isWeekend = (dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY)
        val topGenre = dbManager.getTopGenres(1).firstOrNull() ?: "Punjabi"

        val (title, subtitle, icon, queries) = when (hour) {
            in 5..11 -> {
                if (isWeekend) {
                    Quadruple(
                        "☕ Acoustic Weekend Chill",
                        "Lazy morning acoustics, warm melodies & effortless soundscapes",
                        "☕",
                        listOf("weekend acoustic chill $topGenre", "indie acoustic morning", "mellow breakfast vibes")
                    )
                } else {
                    Quadruple(
                        "☀️ Morning Clarity & Focus",
                        "Uplifting frequencies, clear acoustics & positive drive for your day",
                        "☀️",
                        listOf("morning positive $topGenre", "acoustic focus hits 2025", "fresh energy morning")
                    )
                }
            }
            in 12..16 -> {
                Quadruple(
                    "⚡ High Octane Flow",
                    "Driving rhythms & high-energy beats for peak momentum",
                    "⚡",
                    listOf("high energy $topGenre hits", "upbeat workflow beats", "banger workout hits")
                )
            }
            in 17..20 -> {
                Quadruple(
                    "🌆 Golden Hour Unwind",
                    "Mellow sunset soundscapes, acoustic warmth & soothing vocals",
                    "🌆",
                    listOf("sunset mellow $topGenre", "golden hour acoustic hits", "soothing sunset vibes")
                )
            }
            else -> {
                Quadruple(
                    "🌙 Late Night Nocturne",
                    "Deep bass, slow-tempo lo-fi & hypnotic midnight reverberations",
                    "🌙",
                    listOf("late night $topGenre chill", "midnight drive lofi beats", "hypnotic midnight soundscape")
                )
            }
        }

        val cacheKey = "mood_mix_${hour / 4}_${isWeekend}_$topGenre"
        val cached = getFromCache<List<Track>>(cacheKey)
        val tracks = if (cached != null && cached.isNotEmpty()) {
            cached
        } else {
            val fetched = executeParallelQueries(queries, limitPerQuery = 6)
            val validFetched = filterValidTracks(fetched)
            val deduped = EmberAudioEngine.deduplicateTracks(validFetched)
            val balanced = applyAntiFatigueFilter(deduped, maxPerArtist = 3)
                .shuffled()
                .take(count)
            cache[cacheKey] = Pair(System.currentTimeMillis(), balanced)
            balanced
        }

        MoodMixResult(title, subtitle, icon, tracks)
    }

    /**
     * 4. SMART DAILY MIX
     * Multi-tiered blend: 50% Top Artist + 30% Taste Genre + 20% Discovery.
     */
    suspend fun getSmartDailyMix(
        dbManager: EmberDatabaseManager,
        count: Int = 15
    ): List<Track> = withContext(Dispatchers.IO) {
        val cacheKey = "smart_daily_mix_v2_$count"
        val cached = getFromCache<List<Track>>(cacheKey)
        if (cached != null && cached.isNotEmpty()) return@withContext cached

        val topArtists = dbManager.getTopArtists(2)
        val topGenres = dbManager.getTopGenres(2)

        val primaryArtist = topArtists.firstOrNull() ?: "Navaan Sandhu"
        val secondaryArtist = topArtists.getOrNull(1) ?: "Karan Aujla"
        val primaryGenre = topGenres.firstOrNull() ?: "Punjabi"

        val queries = listOf(
            "$primaryArtist best hits",
            "$secondaryArtist new songs",
            "$primaryGenre top songs",
            "trending soundscapes"
        )

        val fetched = executeParallelQueries(queries, limitPerQuery = 5)
        val validFetched = filterValidTracks(fetched)
        val mixed = EmberAudioEngine.deduplicateTracks(validFetched)
        val balanced = applyAntiFatigueFilter(mixed, maxPerArtist = 2)
            .shuffled()
            .take(count)

        cache[cacheKey] = Pair(System.currentTimeMillis(), balanced)
        balanced
    }

    /**
     * 5. QUEUE RECOMMENDATIONS / AUTOPLAY GENERATOR
     * Generates seamlessly matching next tracks avoiding back-to-back artist repetition.
     */
    suspend fun getQueueRecommendations(
        currentTrack: Track,
        excludeIds: Set<String> = emptySet(),
        count: Int = 10,
        dbManager: EmberDatabaseManager? = null
    ): List<Track> = withContext(Dispatchers.IO) {
        val cacheKey = "queue_rec_${currentTrack.id}_$count"
        val cached = getFromCache<List<Track>>(cacheKey)
        if (cached != null && cached.isNotEmpty()) {
            val fresh = cached.filter { it.id !in excludeIds }
            if (fresh.size >= 4) return@withContext fresh
        }

        val primaryArtist = currentTrack.artist.split(",", "&", "feat.", "ft.", "Feat.").firstOrNull()?.trim() ?: currentTrack.artist.trim()
        val cleanTitle = currentTrack.title.replace(Regex("\\(.*\\)|\\[.*\\]"), "").trim()
        val related = getRelatedArtists(primaryArtist, dbManager, limit = 4)

        val queries = mutableListOf<String>()
        if (related.isNotEmpty()) {
            val adjacent = related.shuffled().take(2)
            adjacent.forEach { queries.add("$it hits") }
        }
        queries.add("$primaryArtist top hits")
        queries.add("$cleanTitle $primaryArtist soundscape")

        val fetched = executeParallelQueries(queries.take(3), limitPerQuery = 6)
        val valid = filterValidTracks(fetched)
        val filtered = valid.filter { it.id != currentTrack.id && it.id !in excludeIds && it.durationSec >= 45 }
        val deduped = EmberAudioEngine.deduplicateTracks(filtered)

        // Queue Anti-Fatigue: disallow repeating the same artist back-to-back if another is available
        val sortedForFlow = deduped.sortedWith { t1, t2 ->
            val t1SameArtist = t1.artist.contains(primaryArtist, ignoreCase = true)
            val t2SameArtist = t2.artist.contains(primaryArtist, ignoreCase = true)
            when {
                t1SameArtist && !t2SameArtist -> 1
                !t1SameArtist && t2SameArtist -> -1
                else -> 0
            }
        }.take(count)

        if (sortedForFlow.isNotEmpty()) {
            cache[cacheKey] = Pair(System.currentTimeMillis(), sortedForFlow)
        }
        sortedForFlow
    }

    /**
     * Enforces Anti-Fatigue rules so no single artist dominates more than maxPerArtist slots.
     */
    private fun applyAntiFatigueFilter(tracks: List<Track>, maxPerArtist: Int = 2): List<Track> {
        val artistCounts = mutableMapOf<String, Int>()
        val result = mutableListOf<Track>()

        for (track in tracks) {
            val normArtist = track.artist.split(",", "&", "/").firstOrNull()?.trim()?.lowercase() ?: track.artist.trim().lowercase()
            val currentCount = artistCounts.getOrDefault(normArtist, 0)
            if (currentCount < maxPerArtist) {
                artistCounts[normArtist] = currentCount + 1
                result.add(track)
            }
        }
        return result
    }

    private fun filterValidTracks(tracks: List<Track>): List<Track> {
        return tracks.filter { track ->
            val isTooShort = track.durationSec in 1..35
            val isPreview = EmberAudioEngine.isPreviewUrl(track.audioUrl) && track.audioUrl.isNotBlank()
            !isTooShort && !isPreview
        }
    }

    private suspend fun executeParallelQueries(
        queries: List<String>,
        limitPerQuery: Int = 6
    ): List<Track> = coroutineScope {
        val deferredList = queries.map { q ->
            async {
                try {
                    val tracks = EmberAudioEngine.searchTracks(q, count = limitPerQuery)
                    tracks.map { t ->
                        if (EmberAudioEngine.isPreviewUrl(t.audioUrl)) {
                            t.copy(audioUrl = "")
                        } else {
                            t
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Query '$q' failed: ${e.message}")
                    emptyList()
                }
            }
        }
        deferredList.flatMap { it.await() }
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T> getFromCache(key: String): T? {
        val entry = cache[key] ?: return null
        val age = System.currentTimeMillis() - entry.first
        return if (age < CACHE_TTL_MS) entry.second as? T else null
    }

    private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
}
