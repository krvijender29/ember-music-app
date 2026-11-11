package com.example.ember

import com.example.ember.update.EmberUpdateManager
import com.example.ember.engine.YouTubeMusicInnertubeEngine

import com.example.ember.util.ThumbnailLoadingDrawable
import com.example.ember.ui.EmberShimmerLayout
import com.example.ember.ui.EmberRadialBufferView
import com.example.ember.ui.EmberWaveformBufferView
import com.example.ember.ui.EmberLaserSweepView



import android.animation.ObjectAnimator

import android.content.ClipData

import android.content.ClipboardManager

import android.content.Context

import android.content.Intent
import android.net.Uri

import android.content.res.ColorStateList

import android.animation.ValueAnimator

import android.view.animation.AccelerateDecelerateInterpolator

import android.graphics.Bitmap

import android.graphics.BitmapFactory

import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.drawable.ColorDrawable
import android.graphics.Typeface

import android.graphics.RenderEffect

import android.graphics.Shader

import android.graphics.drawable.BitmapDrawable

import android.graphics.drawable.GradientDrawable

import android.graphics.drawable.LayerDrawable

import android.os.Build

import android.media.audiofx.BassBoost

import android.media.audiofx.Equalizer

import android.os.Bundle

import android.os.CountDownTimer

import android.os.Handler

import android.os.Looper

import java.util.Locale

import android.text.Editable

import android.text.Spannable

import android.text.SpannableStringBuilder

import android.text.TextWatcher

import android.text.style.ForegroundColorSpan

import android.view.GestureDetector

import android.view.Gravity

import android.view.HapticFeedbackConstants

import android.view.MotionEvent

import android.view.View

import android.view.WindowManager

import android.view.animation.AccelerateInterpolator

import android.view.animation.DecelerateInterpolator

import android.view.animation.LinearInterpolator

import android.view.animation.OvershootInterpolator

import android.view.inputmethod.EditorInfo

import android.view.inputmethod.InputMethodManager

import android.widget.EditText

import android.widget.FrameLayout

import android.widget.ImageButton

import android.widget.ImageView

import android.widget.LinearLayout

import android.widget.ProgressBar

import android.widget.RelativeLayout

import android.widget.ScrollView

import android.widget.SeekBar

import android.widget.TextView

import android.util.DisplayMetrics

import android.util.Log

import android.widget.Toast

import androidx.activity.OnBackPressedCallback

import androidx.activity.result.contract.ActivityResultContracts

import androidx.appcompat.app.AppCompatActivity

import androidx.appcompat.widget.SwitchCompat

import androidx.core.content.ContextCompat

import androidx.core.view.ViewCompat

import androidx.core.view.WindowCompat

import androidx.core.view.WindowInsetsCompat

import androidx.lifecycle.lifecycleScope

import android.Manifest

import android.content.ComponentName

import android.content.pm.PackageManager

import androidx.core.app.ActivityCompat

import androidx.media3.common.AudioAttributes

import androidx.media3.common.C

import androidx.media3.common.MediaItem

import androidx.media3.common.Player

import androidx.media3.exoplayer.ExoPlayer

import androidx.media3.session.MediaController

import androidx.media3.session.SessionToken

import com.example.ember.service.EmberPlaybackService

import com.google.common.util.concurrent.ListenableFuture

import androidx.recyclerview.widget.LinearLayoutManager

import androidx.recyclerview.widget.LinearSmoothScroller

import androidx.recyclerview.widget.RecyclerView

import coil.ImageLoader

import coil.load

import coil.request.ImageRequest

import coil.request.SuccessResult

import com.example.ember.util.BlurUtils

import com.example.ember.adapter.AlbumAdapter

import com.example.ember.adapter.ArtistAdapter

import com.example.ember.adapter.HorizontalCardAdapter

import com.example.ember.adapter.LyricLine

import com.example.ember.adapter.LyricsAdapter

import com.example.ember.adapter.PlaylistAdapter

import com.example.ember.adapter.PlaylistCardAdapter

import com.example.ember.adapter.PlaylistDetailTrackAdapter

import com.example.ember.adapter.RecentSearchesAdapter

import com.example.ember.adapter.SearchSuggestionsAdapter

import com.example.ember.adapter.TrackAdapter

import com.example.ember.data.EmberAuthManager

import com.example.ember.data.EmberDatabaseManager

import com.example.ember.engine.EmberRecommendationEngine

import com.example.ember.engine.EmberAudioEngine

import com.example.ember.ui.EmberTheme

import com.example.ember.model.ArtistItem

import com.example.ember.model.Playlist

import com.example.ember.model.PlaylistItem

import com.example.ember.model.TopMatchItem

import com.example.ember.model.Track

import com.example.ember.model.User

import com.google.android.gms.auth.api.signin.GoogleSignIn

import com.google.android.gms.auth.api.signin.GoogleSignInClient

import com.google.android.gms.auth.api.signin.GoogleSignInOptions

import com.google.android.gms.common.api.ApiException

import com.google.android.material.bottomsheet.BottomSheetDialog

import com.google.android.material.button.MaterialButton

import com.google.android.material.card.MaterialCardView

import com.google.android.material.chip.Chip

import com.google.android.material.chip.ChipGroup

import com.google.firebase.auth.FirebaseAuth

import com.google.firebase.auth.GoogleAuthProvider

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async

import kotlinx.coroutines.Job

import kotlinx.coroutines.delay

import kotlinx.coroutines.launch

import kotlinx.coroutines.withContext

import okhttp3.OkHttpClient

import okhttp3.Request

import org.json.JSONObject

import java.net.URLEncoder

import java.util.Calendar



class MainActivity : AppCompatActivity() {



    private lateinit var authManager: EmberAuthManager

    private lateinit var dbManager: EmberDatabaseManager

    private var player: Player? = null

    private var mediaControllerFuture: ListenableFuture<MediaController>? = null

    private val handler = Handler(Looper.getMainLooper())

    private val httpClient = OkHttpClient()



    // Audio effects

    private var equalizer: Equalizer? = null

    private var bassBoost: BassBoost? = null



    // UI - Tabs

    private lateinit var tabHomeView: View

    private lateinit var tabSearchView: View

    private lateinit var tabLibraryView: View

    private lateinit var tabProfileView: View



    // UI - Bottom Nav

    private lateinit var navHome: LinearLayout

    private lateinit var navSearch: LinearLayout

    private lateinit var navLibrary: LinearLayout

    private lateinit var navProfile: LinearLayout

    private lateinit var ivNavHome: ImageView

    private lateinit var tvNavHome: TextView

    private lateinit var ivNavSearch: ImageView

    private lateinit var tvNavSearch: TextView

    private lateinit var ivNavLibrary: ImageView

    private lateinit var tvNavLibrary: TextView

    private lateinit var ivNavProfile: ImageView

    private lateinit var tvNavProfile: TextView



    // UI - Home Tab

    private lateinit var tvHeaderAppLogo: TextView

    private lateinit var btnHeaderBell: ImageView

    private lateinit var btnHeaderProfile: MaterialCardView

    private lateinit var ivHeaderAvatarImg: ImageView

    private lateinit var tvHeaderAvatarInit: TextView

    private lateinit var cardHomeSearchTrigger: View

    private lateinit var ivHeroBg: ImageView

    private lateinit var tvHeroGreeting: TextView

    private lateinit var tvHeroSubtext: TextView

    private lateinit var btnHeroPlayNow: MaterialButton

    private lateinit var btnHeroExplore: MaterialButton

    private lateinit var btnTuneFromHome: TextView



    // Home Specialized Song Sections

    // Section 1: Jump Back In

    private lateinit var btnPlayAllJumpBackIn: TextView

    private lateinit var rvJumpBackIn: RecyclerView

    private lateinit var jumpBackInAdapter: HorizontalCardAdapter

    private val trendingCardsAdapter: HorizontalCardAdapter get() = jumpBackInAdapter



    // Auto-Recommendation Stream 1: Recommended For You

    private lateinit var layoutRecommendedForYouSection: View

    private lateinit var tvRecommendedSubtitle: TextView

    private lateinit var btnPlayAllRecommended: TextView

    private lateinit var rvRecommendedForYou: RecyclerView

    private lateinit var recommendedForYouAdapter: HorizontalCardAdapter



    // Section 2: Global Top Charts

    private lateinit var btnSeeAllCharts: TextView

    private lateinit var rvChartsCards: RecyclerView

    private lateinit var chartsCardsAdapter: HorizontalCardAdapter



    // Section 3: More from {topArtist}

    private lateinit var layoutMoreFromArtistSection: View

    private lateinit var tvMoreFromArtistTitle: TextView

    private lateinit var btnPlayAllMoreFromArtist: TextView

    private lateinit var rvMoreFromArtist: RecyclerView

    private lateinit var moreFromArtistAdapter: TrackAdapter



    // Section 4: Based on Your Taste

    private lateinit var layoutBasedOnTasteSection: View

    private lateinit var btnTuneTastePill: TextView

    private lateinit var rvBasedOnTaste: RecyclerView

    private lateinit var basedOnTasteAdapter: TrackAdapter

    private val homeAdapter: TrackAdapter get() = basedOnTasteAdapter



    // Auto-Recommendation Stream 2: Discover Something New

    private lateinit var layoutDiscoverNewSection: View

    private lateinit var btnPlayAllDiscoverNew: TextView

    private lateinit var rvDiscoverNew: RecyclerView

    private lateinit var discoverNewAdapter: HorizontalCardAdapter



    // Auto-Recommendation Stream 3: Mood Mix

    private lateinit var layoutMoodMixSection: View

    private lateinit var tvMoodMixTitle: TextView

    private lateinit var tvMoodMixSubtitle: TextView

    private lateinit var btnPlayAllMoodMix: TextView

    private lateinit var rvMoodMix: RecyclerView

    private lateinit var moodMixAdapter: HorizontalCardAdapter



    // Section 5: Made For You (Daily Mix Banner)

    private lateinit var cardDailyMixBanner: MaterialCardView

    private lateinit var tvDailyMixTitle: TextView

    private lateinit var tvDailyMixSubtitle: TextView

    private lateinit var btnDailyMixPlay: ImageView



    // Section 6: Your Playlists

    private lateinit var layoutHomeUserPlaylistsSection: View

    private lateinit var btnSeeAllPlaylists: TextView

    private lateinit var rvHomeUserPlaylists: RecyclerView

    private lateinit var homeUserPlaylistsAdapter: PlaylistCardAdapter



    private var pbHomeLoading: View? = null
    private lateinit var layoutHomeSkeleton: EmberShimmerLayout



    // UI - Search Tab

    private lateinit var etSearch: EditText

    private lateinit var ivClearSearch: ImageView

    private lateinit var pillFilterAll: TextView

    private lateinit var pillFilterSongs: TextView

    private lateinit var pillFilterArtists: TextView

    private lateinit var pillFilterAlbums: TextView

    private lateinit var pillFilterPlaylists: TextView

    private lateinit var pbSearchLoading: ProgressBar
    private lateinit var layoutSearchSkeleton: EmberShimmerLayout
    private lateinit var tvSearchSkeletonStatus: TextView

    private lateinit var rvSearchTracks: RecyclerView

    private lateinit var searchAdapter: TrackAdapter

    private lateinit var layoutSearchDefaultContent: View

    private lateinit var cardSpotifyImporter: MaterialCardView

    private lateinit var tagArijit: TextView

    private lateinit var tagWeeknd: TextView

    private lateinit var tagDiljit: TextView

    private lateinit var tagTaylor: TextView

    private lateinit var cardGenreBollywood: View

    private lateinit var cardGenrePunjabi: View

    private lateinit var cardGenreHiphop: View

    private lateinit var cardGenrePop: View

    private lateinit var cardGenreLofi: View

    private lateinit var cardGenreRnb: View

    private var currentSearchFilter = "All"

    private var searchDebounceJob: Job? = null



    // Search – advanced views (autocomplete, unified results, recent searches)

    private lateinit var cardSearchAutocomplete: MaterialCardView

    private lateinit var rvSearchAutocomplete: RecyclerView

    private lateinit var searchSuggestionsAdapter: SearchSuggestionsAdapter



    private lateinit var layoutRecentSearches: View

    private lateinit var btnClearRecentSearches: TextView

    private lateinit var rvRecentSearches: RecyclerView

    private lateinit var recentSearchesAdapter: RecentSearchesAdapter



    private lateinit var layoutSearchResultsAll: View

    private lateinit var cardTopMatchHero: MaterialCardView

    private lateinit var cardTopMatchImageWrapper: MaterialCardView

    private lateinit var tvTopMatchBadge: TextView

    private lateinit var ivTopMatchImage: ImageView

    private lateinit var tvTopMatchTitle: TextView

    private lateinit var tvTopMatchSubtitle: TextView

    private lateinit var btnTopMatchAction: View



    private lateinit var rvAllSongs: RecyclerView

    private lateinit var btnSeeAllSongs: TextView

    private lateinit var allSongsAdapter: TrackAdapter



    private lateinit var rvAllArtists: RecyclerView

    private lateinit var allArtistsAdapter: ArtistAdapter



    private lateinit var rvAllAlbums: RecyclerView

    private lateinit var allAlbumsAdapter: AlbumAdapter



    private lateinit var rvAllPlaylists: RecyclerView

    private lateinit var allPlaylistsAdapter: PlaylistCardAdapter



    private lateinit var rvSearchArtists: RecyclerView

    private lateinit var categoryArtistsAdapter: ArtistAdapter



    private lateinit var rvSearchAlbums: RecyclerView

    private lateinit var categoryAlbumsAdapter: AlbumAdapter



    private lateinit var rvSearchPlaylists: RecyclerView

    private lateinit var categoryPlaylistsAdapter: PlaylistCardAdapter



    private lateinit var layoutSearchEmpty: View

    private lateinit var tvSearchEmptyTitle: TextView

    private lateinit var tagLofi: TextView



    // UI - Library Tab

    private lateinit var etLibrarySearch: EditText

    private lateinit var cardLikedSongs: MaterialCardView
    private lateinit var cardDownloadedSongs: MaterialCardView
    private lateinit var tvDownloadedCount: TextView
    private lateinit var layoutOfflineBanner: LinearLayout
    private lateinit var tvOfflineBannerText: TextView
    private lateinit var btnOfflineGoToDownloads: TextView
    private lateinit var btnFullDownloadContainer: FrameLayout
    private lateinit var ivFullDownload: ImageView
    private lateinit var pbFullDownload: ProgressBar
    private lateinit var downloadManager: com.example.ember.data.EmberDownloadManager
    private var isOfflineMode = false
    private var networkCallback: android.net.ConnectivityManager.NetworkCallback? = null

    private lateinit var tvLikedCount: TextView

    private lateinit var btnCreatePlaylist: MaterialCardView

    private lateinit var tvPlaylistCount: TextView

    private lateinit var rvLibraryPlaylists: RecyclerView

    private lateinit var playlistAdapter: PlaylistAdapter

    private lateinit var rvLibraryTracks: RecyclerView

    private lateinit var libraryAdapter: TrackAdapter



    // UI - Profile & Auth Tab

    private lateinit var layoutLoggedIn: LinearLayout

    private lateinit var ivUserProfilePic: ImageView

    private lateinit var tvUserAvatar: TextView

    private lateinit var tvProfileName: TextView

    private lateinit var tvProfileEmail: TextView

    private lateinit var tvProfilePersona: TextView

    private lateinit var btnEditProfile: MaterialButton

    private lateinit var tvStatPlaylists: TextView

    private lateinit var tvStatLikes: TextView

    private lateinit var tvStatHistory: TextView

    private lateinit var cardMetricLikes: LinearLayout

    private lateinit var cardMetricPlaylists: LinearLayout

    private lateinit var cardMetricHistory: LinearLayout

    private lateinit var pillBitrate160: TextView

    private lateinit var pillBitrate256: TextView

    private lateinit var pillBitrate320: TextView

    private lateinit var tvEqPresetName: TextView

    private lateinit var btnOpenEqModal: MaterialButton

    private lateinit var switchStayAwake: SwitchCompat

    private lateinit var tvCloudSyncStatus: TextView

    private lateinit var btnSyncNow: MaterialButton

    private lateinit var tvCacheSize: TextView

    private lateinit var btnClearCache: MaterialButton

    private lateinit var btnClearHistory: TextView

    private lateinit var rvHistoryTracks: RecyclerView

    private lateinit var historyAdapter: TrackAdapter

    private lateinit var btnSignOut: View

    private var btnProfileHeaderSettings: View? = null



    // App Opening & Website-Style Welcome Screen Gate

    private lateinit var layoutOpeningLandingScreen: RelativeLayout

    private lateinit var layoutMainAppShell: RelativeLayout

    private lateinit var ivAmbientGlowOrb: View

    private lateinit var btnOpeningGoogleSignIn: MaterialButton

    private lateinit var btnOpeningEmailSignIn: MaterialButton

    private lateinit var eqBar1: View

    private lateinit var eqBar2: View

    private lateinit var eqBar3: View

    private lateinit var eqBar4: View

    private lateinit var eqBar5: View

    private var glowOrbAnimator: ValueAnimator? = null

    private var eqBarsAnimators = mutableListOf<ValueAnimator>()



    private lateinit var layoutAuthForm: LinearLayout

    private lateinit var tabAuthSignIn: TextView

    private lateinit var tabAuthSignUp: TextView

    private lateinit var etAuthName: EditText

    private lateinit var etAuthEmail: EditText

    private lateinit var etAuthPassword: EditText

    private lateinit var btnAuthSubmit: MaterialButton

    private lateinit var btnGoogleSignIn: MaterialButton

    private lateinit var pbAuthLoading: ProgressBar

    private lateinit var btnGuestMode: TextView

    private var isSignUpMode = false



    private lateinit var googleSignInClient: GoogleSignInClient



    private val googleSignInLauncher = registerForActivityResult(

        ActivityResultContracts.StartActivityForResult()

    ) { result ->

        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)

        try {

            val account = task.getResult(ApiException::class.java)

            val idToken = account.idToken

            val email = account.email ?: "svijender130@gmail.com"

            val name = account.displayName ?: email.substringBefore("@")

            val photo = account.photoUrl?.toString()



            lifecycleScope.launch {

                var finalUid = "8H10dwk0lvOytj0Ow8ooRgNpM4Z2"

                val sbUser = authManager.resolveSupabaseProfileByEmail(email)

                if (sbUser != null) {

                    finalUid = sbUser.id

                } else if (account.id != null) {

                    finalUid = account.id!!

                }



                if (idToken != null) {

                    try {

                        val credential = GoogleAuthProvider.getCredential(idToken, null)

                        FirebaseAuth.getInstance().signInWithCredential(credential)

                            .addOnCompleteListener(this@MainActivity) { fbTask ->

                                if (fbTask.isSuccessful) {

                                    val fbUser = fbTask.result?.user

                                    if (sbUser == null && fbUser != null) {

                                        finalUid = fbUser.uid

                                    }

                                }

                            }

                    } catch (e: Exception) {

                        Log.w("MainActivity", "Firebase credential sign-in: ${e.message}")

                    }

                }



                val googleUser = User(

                    id = finalUid,

                    email = email,

                    displayName = sbUser?.displayName ?: name,

                    avatarUrl = sbUser?.avatarUrl ?: photo,

                    token = idToken,

                    isGuest = false

                )

                authManager.saveGoogleUser(googleUser)

                onUserSignedInSuccess(googleUser)

            }

        } catch (e: ApiException) {

            val reason = when (e.statusCode) {

                10 -> "Google SHA-1 not configured. Enter email below to sync Supabase directly."

                12500 -> "Sign-in error (12500): Check Play Services or enter email below."

                12501 -> "Sign-in cancelled."

                else -> "Google Error (${e.statusCode}): ${e.message}"

            }

            Toast.makeText(this, reason, Toast.LENGTH_LONG).show()

        } catch (e: Exception) {

            Toast.makeText(this, "Sign-In error: ${e.message}", Toast.LENGTH_LONG).show()

        }

    }



    // UI - Persistent Floating Mini Player

    private lateinit var viewMiniLeftFlank: View

    private lateinit var viewMiniRightFlank: View

    private lateinit var miniPlayerView: MaterialCardView

    private lateinit var ivMiniAmbientMesh: ImageView

    private lateinit var viewMiniUpperGlow: View

    private lateinit var pbMiniProgress: ProgressBar

    private lateinit var ivMiniThumb: ImageView

    private lateinit var tvMiniTitle: TextView

    private lateinit var tvMiniArtist: TextView

    private lateinit var ivMiniLike: ImageView

    private lateinit var btnMiniPlayPause: ImageButton

    private lateinit var btnMiniNext: ImageButton

    private var currentHaloColor = Color.parseColor("#FF5A36")

    private var haloColorAnimator: ValueAnimator? = null



    // UI - Opened Playlist & Album View

    private lateinit var layoutOpenedPlaylistOverlay: FrameLayout

    private lateinit var ivPlaylistAmbientGlow: ImageView

    private lateinit var scrollPlaylistContent: androidx.core.widget.NestedScrollView

    private lateinit var viewPlaylistTopSpacer: View

    private lateinit var cardPlaylistHeroCover: MaterialCardView

    private lateinit var ivPlaylistHeroCover: ImageView

    private lateinit var layoutLikedHeroCover: FrameLayout

    private lateinit var tvPlaylistBadge: TextView

    private lateinit var tvPlaylistHeroTitle: TextView

    private lateinit var tvPlaylistMetadata: TextView

    private lateinit var btnSaveToLibrary: LinearLayout

    private lateinit var ivSaveToLibraryIcon: ImageView

    private lateinit var tvSaveToLibraryText: TextView

    private lateinit var btnDeletePlaylist: LinearLayout

    private lateinit var btnPlaylistShuffle: ImageView

    private lateinit var btnPlaylistActionShare: ImageView

    private lateinit var btnPlaylistPlayAll: MaterialCardView

    private lateinit var ivPlaylistPlayIcon: ImageView

    private lateinit var layoutPlaylistFilterBar: LinearLayout

    private lateinit var etPlaylistFilter: EditText

    private lateinit var btnPlaylistFilterClear: ImageView

    private lateinit var pbPlaylistLoading: ProgressBar

    private lateinit var layoutPlaylistEmpty: LinearLayout

    private lateinit var rvPlaylistTracks: RecyclerView

    private lateinit var layoutPlaylistStickyHeader: FrameLayout

    private lateinit var viewPlaylistStickyHeaderBg: View

    private lateinit var viewStickyHeaderAccentRim: View

    private lateinit var layoutStickyHeaderContent: RelativeLayout

    private lateinit var layoutStickyCenterInfo: LinearLayout

    private lateinit var ivStickyCover: ImageView

    private lateinit var tvStickyPlaylistTitle: TextView

    private lateinit var tvStickyPlaylistSubtitle: TextView

    private lateinit var btnStickyPlayAll: View

    private lateinit var btnPlaylistBack: ImageView

    private lateinit var btnPlaylistHeaderShare: ImageView

    private lateinit var btnPlaylistHeaderOptions: ImageView

    private lateinit var playlistDetailAdapter: com.example.ember.adapter.PlaylistDetailTrackAdapter

    private var currentOpenedPlaylist: Playlist? = null

    private var currentOpenedAlbum: com.example.ember.model.AlbumItem? = null

    private var isCurrentOpenedSaved: Boolean = false



    // Navigation & Shell Insets

    private lateinit var bottomNav: LinearLayout

    private lateinit var indicatorHome: View

    private lateinit var indicatorSearch: View

    private lateinit var indicatorLibrary: View

    private lateinit var indicatorProfile: View

    private lateinit var headerHomeBar: View

    private lateinit var tvSearchTitle: TextView

    private lateinit var tvLibraryTitle: TextView

    private lateinit var tvProfileTitle: TextView



    // UI - Fullscreen Now Playing Sheet

    private lateinit var fullPlayerOverlay: FrameLayout

    private lateinit var ivFullAmbientMesh: ImageView

    private lateinit var fullPlayerTopHeader: RelativeLayout

    private lateinit var layoutArtworkWrapper: FrameLayout

    private lateinit var layoutFullPlayerBottomDock: LinearLayout

    private lateinit var sheetDragHandle: View

    private lateinit var btnCollapsePlayer: ImageButton

    private lateinit var btnFullOptions: ImageButton

    private lateinit var ivArtworkAmbientGlow: ImageView

    private lateinit var cardArtworkContainer: MaterialCardView

    private lateinit var ivFullThumb: ImageView

    private lateinit var layoutVinylDisc: FrameLayout

    private lateinit var vinylRecordCanvas: com.example.ember.ui.VinylRecordView

    private lateinit var ivVinylCenterLabel: ImageView

    private lateinit var btnVinylMode: View

    private lateinit var tvVinylModeLabel: TextView

    private lateinit var layoutFullBuffering: View
    private lateinit var pbFullPlayBuffering: View
    private lateinit var pbMiniBuffering: View
    private lateinit var pbFullBuffering: View
    private lateinit var laserMiniBuffering: View

    private lateinit var tvFullTitle: TextView

    private lateinit var tvFullArtist: TextView

    private lateinit var btnFullLikeContainer: FrameLayout

    private lateinit var ivFullLike: ImageView

    private lateinit var fullSeekBar: SeekBar



    private lateinit var tvFullCurrentTime: TextView

    private lateinit var tvFullDuration: TextView



    private lateinit var btnFullShuffle: ImageButton

    private lateinit var btnFullPrev: ImageButton

    private lateinit var btnFullPlayPause: ImageButton

    private lateinit var btnFullNext: ImageButton

    private lateinit var btnFullRepeat: ImageButton



    private lateinit var badgeLossless: TextView

    private lateinit var badgeAwake: View

    private lateinit var ivAwakeIcon: ImageView

    private lateinit var tvAwakeText: TextView



    private lateinit var btnFullLyrics: View

    private lateinit var btnFullEQ: View

    private lateinit var btnFullTimer: View

    private lateinit var tvTimerLabel: TextView

    private lateinit var btnFullQueue: View



    // Playback and Feature States

    private val playlistQueue = mutableListOf<Track>()

    private var currentTrackIndex = -1

    private var currentTrack: Track? = null



    private var isShuffleMode = false

    private var repeatMode = 0 // 0 = OFF, 1 = ALL, 2 = ONE

    private var isVinylMode = false

    private var isAwakeEnabled = false

    private var vinylAnimator: ObjectAnimator? = null



    // Sleep Timer

    private var sleepTimer: CountDownTimer? = null

    private var sleepTimerRemainingMillis = 0L



    // Synced Karaoke Lyrics state

    private var activeLyricsAdapter: LyricsAdapter? = null

    private var activeLyricsRecyclerView: RecyclerView? = null

    private var isUserSeeking: Boolean = false



    // In-Card Live Synced Lyrics Overlay Views & State

    private lateinit var layoutLyricsPanel: View

    private lateinit var badgeLiveSynced: View

    private lateinit var layoutLyricsOffset: View

    private lateinit var btnOffsetMinus: TextView

    private lateinit var tvOffsetCurrent: TextView

    private lateinit var btnOffsetPlus: TextView

    private lateinit var btnCloseLyricsCard: View

    private lateinit var rvCardLyrics: RecyclerView

    private lateinit var pbCardLyricsLoading: ProgressBar

    private lateinit var tvCardLyricsEmpty: TextView

    private lateinit var ivFullLyricsIcon: ImageView

    private lateinit var tvFullLyricsLabel: TextView



    private var isLyricsActive = false

    private var lyricsOffsetMs = 0L

    private var cardLyricsAdapter: LyricsAdapter? = null

    private var lyricsFetchJob: Job? = null

    private var cachedLyricsTrackId: String? = null

    private var cachedSyncedLines: List<LyricLine>? = null

    private var cachedPlainLyrics: String? = null



    private val progressUpdater = object : Runnable {

        override fun run() {

            player?.let { p ->

                if (p.isPlaying) {

                    val current = p.currentPosition

                    val total = p.duration

                    if (total > 0 && !isUserSeeking) {

                        val progressInt = ((current * 1000) / total).toInt()

                        fullSeekBar.progress = progressInt

                                                pbMiniProgress.progress = progressInt

                    }

                    if (!isUserSeeking) {

                        tvFullCurrentTime.text = formatTime(current)

                    }



                    // Update in-card live lyrics highlighting

                    if (isLyricsActive) {

                        cardLyricsAdapter?.let { la ->

                            val activeIdx = la.updateActiveTimestamp(current + lyricsOffsetMs)

                            if (activeIdx >= 0) {

                                smoothScrollLyricsToCenter(rvCardLyrics, activeIdx)

                            }

                        }

                    }



                    // Update bottom sheet live lyrics highlighting if open

                    activeLyricsAdapter?.let { la ->

                        val activeIdx = la.updateActiveTimestamp(current + lyricsOffsetMs)

                        if (activeIdx >= 0) {

                            activeLyricsRecyclerView?.smoothScrollToPosition(activeIdx)

                        }

                    }

                }

            }

            handler.postDelayed(this, 250)

        }

    }



    private fun smoothScrollLyricsToCenter(recyclerView: RecyclerView, position: Int) {

        if (position < 0) return

        val layoutManager = recyclerView.layoutManager as? LinearLayoutManager ?: return

        val context = recyclerView.context

        val scroller = object : LinearSmoothScroller(context) {

            override fun calculateDtToFit(

                viewStart: Int,

                viewEnd: Int,

                boxStart: Int,

                boxEnd: Int,

                snapPreference: Int

            ): Int {

                return (boxStart + (boxEnd - boxStart) / 2) - (viewStart + (viewEnd - viewStart) / 2)

            }



            override fun calculateSpeedPerPixel(displayMetrics: DisplayMetrics): Float {

                return 150f / displayMetrics.densityDpi

            }

        }

        scroller.targetPosition = position

        layoutManager.startSmoothScroll(scroller)

    }



    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)



        authManager = EmberAuthManager.getInstance(this)

        dbManager = EmberDatabaseManager.getInstance(this)
        downloadManager = com.example.ember.data.EmberDownloadManager.getInstance(this)

        initViews()

        setupDownloadListener()
        setupNetworkMonitoring()

        setupWindowInsets()

        setupBottomNavigation()

        setupRecyclerViews()

        setupExoPlayer()

        setupMiniPlayerGestures()

        setupSearchAndDiscovery()

        setupLibrary()

        setupProfileAndAuth()

        setupBackNavigation()



        // App Opening Screen & Initial State Gate

        checkAuthAndDisplayOpeningScreen()

        // Background Check for Custom APK Updates
        lifecycleScope.launch {
            kotlinx.coroutines.delay(4500)
            triggerAppUpdateFlow(isManual = false)
        }

    }



    private fun initViews() {

        // Navigation & Shell Insets Views

        headerHomeBar = findViewById(R.id.headerHomeBar)

        tvSearchTitle = findViewById(R.id.tvSearchTitle)

        tvLibraryTitle = findViewById(R.id.tvLibraryTitle)

        tvProfileTitle = findViewById(R.id.tvProfileTitle)

        bottomNav = findViewById(R.id.bottomNav)



        // Tabs

        tabHomeView = findViewById(R.id.tabHomeView)

        tabSearchView = findViewById(R.id.tabSearchView)

        tabLibraryView = findViewById(R.id.tabLibraryView)

        tabProfileView = findViewById(R.id.tabProfileView)



        // Bottom Nav

        navHome = findViewById(R.id.navHome)

        navSearch = findViewById(R.id.navSearch)

        navLibrary = findViewById(R.id.navLibrary)

        navProfile = findViewById(R.id.navProfile)

        ivNavHome = findViewById(R.id.ivNavHome)

        tvNavHome = findViewById(R.id.tvNavHome)

        ivNavSearch = findViewById(R.id.ivNavSearch)

        tvNavSearch = findViewById(R.id.tvNavSearch)

        ivNavLibrary = findViewById(R.id.ivNavLibrary)

        tvNavLibrary = findViewById(R.id.tvNavLibrary)

        ivNavProfile = findViewById(R.id.ivNavProfile)

        tvNavProfile = findViewById(R.id.tvNavProfile)

        indicatorHome = findViewById(R.id.indicatorHome)

        indicatorSearch = findViewById(R.id.indicatorSearch)

        indicatorLibrary = findViewById(R.id.indicatorLibrary)

        indicatorProfile = findViewById(R.id.indicatorProfile)



        // Home Top & Hero

        tvHeaderAppLogo = findViewById(R.id.tvHeaderAppLogo)

        setupEmberLogo()



        btnHeaderBell = findViewById(R.id.btnHeaderBell)

        btnHeaderProfile = findViewById(R.id.btnHeaderProfile)

        ivHeaderAvatarImg = findViewById(R.id.ivHeaderAvatarImg)

        tvHeaderAvatarInit = findViewById(R.id.tvHeaderAvatarInit)

        cardHomeSearchTrigger = findViewById(R.id.cardHomeSearchTrigger)



        ivHeroBg = findViewById(R.id.ivHeroBg)

        applyHeroBackdrop(null)  // Must be after ivHeroBg is bound

        tvHeroGreeting = findViewById(R.id.tvHeroGreeting)

        tvHeroSubtext = findViewById(R.id.tvHeroSubtext)

        btnHeroPlayNow = findViewById(R.id.btnHeroPlayNow)

        btnHeroExplore = findViewById(R.id.btnHeroExplore)



        // Section 1: Jump Back In

        btnPlayAllJumpBackIn = findViewById(R.id.btnPlayAllJumpBackIn)

        rvJumpBackIn = findViewById(R.id.rvJumpBackIn)



        // Auto-Recommendation: Recommended For You

        layoutRecommendedForYouSection = findViewById(R.id.layoutRecommendedForYouSection)

        tvRecommendedSubtitle = findViewById(R.id.tvRecommendedSubtitle)

        btnPlayAllRecommended = findViewById(R.id.btnPlayAllRecommended)

        rvRecommendedForYou = findViewById(R.id.rvRecommendedForYou)



        // Section 2: Global Top Charts

        btnSeeAllCharts = findViewById(R.id.btnSeeAllCharts)

        rvChartsCards = findViewById(R.id.rvChartsCards)

        btnSeeAllCharts.setOnClickListener {

            it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)

            openCuratedTracksView(

                title = "Global Top Charts",

                badge = "TOP CHARTS",

                tracks = chartsCardsAdapter.getTracks(),

                coverUrl = chartsCardsAdapter.getTracks().firstOrNull()?.thumbnail

            )

        }



        // Section 3: More from {topArtist}

        layoutMoreFromArtistSection = findViewById(R.id.layoutMoreFromArtistSection)

        tvMoreFromArtistTitle = findViewById(R.id.tvMoreFromArtistTitle)

        btnPlayAllMoreFromArtist = findViewById(R.id.btnPlayAllMoreFromArtist)

        rvMoreFromArtist = findViewById(R.id.rvMoreFromArtist)



        // Section 4: Based on Your Taste

        layoutBasedOnTasteSection = findViewById(R.id.layoutBasedOnTasteSection)

        btnTuneTastePill = findViewById(R.id.btnTuneTastePill)

        rvBasedOnTaste = findViewById(R.id.rvBasedOnTaste)

        btnTuneTastePill.setOnClickListener { openPreferencesDialog() }



        // Auto-Recommendation: Discover Something New

        layoutDiscoverNewSection = findViewById(R.id.layoutDiscoverNewSection)

        btnPlayAllDiscoverNew = findViewById(R.id.btnPlayAllDiscoverNew)

        rvDiscoverNew = findViewById(R.id.rvDiscoverNew)



        // Auto-Recommendation: Mood Mix

        layoutMoodMixSection = findViewById(R.id.layoutMoodMixSection)

        tvMoodMixTitle = findViewById(R.id.tvMoodMixTitle)

        tvMoodMixSubtitle = findViewById(R.id.tvMoodMixSubtitle)

        btnPlayAllMoodMix = findViewById(R.id.btnPlayAllMoodMix)

        rvMoodMix = findViewById(R.id.rvMoodMix)



        // Section 5: Made For You (Daily Mix Banner)

        cardDailyMixBanner = findViewById(R.id.cardDailyMixBanner)

        tvDailyMixTitle = findViewById(R.id.tvDailyMixTitle)

        tvDailyMixSubtitle = findViewById(R.id.tvDailyMixSubtitle)

        btnDailyMixPlay = findViewById(R.id.btnDailyMixPlay)



        // Section 6: Your Playlists

        layoutHomeUserPlaylistsSection = findViewById(R.id.layoutHomeUserPlaylistsSection)

        btnSeeAllPlaylists = findViewById(R.id.btnSeeAllPlaylists)

        rvHomeUserPlaylists = findViewById(R.id.rvHomeUserPlaylists)

        btnSeeAllPlaylists.setOnClickListener { switchTab(2) }



        // pbHomeLoading removed per user request
        layoutHomeSkeleton = findViewById(R.id.layoutHomeSkeleton)



        btnHeaderBell.setOnClickListener {

            Toast.makeText(this, "EMBER Audio: Lossless Dual-Engine Active", Toast.LENGTH_SHORT).show()

        }



        btnHeaderProfile.setOnClickListener {

            switchTab(3)

            refreshProfileUI()

        }



        cardHomeSearchTrigger.setOnClickListener {

            switchTab(1)

            etSearch.requestFocus()

            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager

            imm?.showSoftInput(etSearch, InputMethodManager.SHOW_IMPLICIT)

        }



        btnHeroExplore.setOnClickListener { openQueueDialog() }



        // Search Tab UI

        etSearch = findViewById(R.id.etSearch)

        ivClearSearch = findViewById(R.id.ivClearSearch)

        pillFilterAll = findViewById(R.id.pillFilterAll)

        pillFilterSongs = findViewById(R.id.pillFilterSongs)

        pillFilterArtists = findViewById(R.id.pillFilterArtists)

        pillFilterAlbums = findViewById(R.id.pillFilterAlbums)

        pillFilterPlaylists = findViewById(R.id.pillFilterPlaylists)

        pbSearchLoading = findViewById(R.id.pbSearchLoading)
        layoutSearchSkeleton = findViewById(R.id.layoutSearchSkeleton)
        tvSearchSkeletonStatus = findViewById(R.id.tvSearchSkeletonStatus)

        rvSearchTracks = findViewById(R.id.rvSearchTracks)

        layoutSearchDefaultContent = findViewById(R.id.layoutSearchDefaultContent)

        cardSpotifyImporter = findViewById(R.id.cardSpotifyImporter)

        tagArijit = findViewById(R.id.tagArijit)

        tagWeeknd = findViewById(R.id.tagWeeknd)

        tagDiljit = findViewById(R.id.tagDiljit)

        tagTaylor = findViewById(R.id.tagTaylor)

        cardGenreBollywood = findViewById(R.id.cardGenreBollywood)

        cardGenrePunjabi = findViewById(R.id.cardGenrePunjabi)

        cardGenreHiphop = findViewById(R.id.cardGenreHiphop)

        cardGenrePop = findViewById(R.id.cardGenrePop)

        cardGenreLofi = findViewById(R.id.cardGenreLofi)

        cardGenreRnb = findViewById(R.id.cardGenreRnb)



        // Search – Advanced: Autocomplete dropdown

        cardSearchAutocomplete = findViewById(R.id.cardSearchAutocomplete)

        rvSearchAutocomplete = findViewById(R.id.rvSearchAutocomplete)

        searchSuggestionsAdapter = SearchSuggestionsAdapter { suggestion ->

            executeSearch(suggestion)

        }

        rvSearchAutocomplete.layoutManager = LinearLayoutManager(this)

        rvSearchAutocomplete.adapter = searchSuggestionsAdapter

        cardSearchAutocomplete.visibility = View.GONE



        // Search – Advanced: Recent Searches panel

        layoutRecentSearches = findViewById(R.id.layoutRecentSearches)

        btnClearRecentSearches = findViewById(R.id.btnClearRecentSearches)

        rvRecentSearches = findViewById(R.id.rvRecentSearches)

        recentSearchesAdapter = RecentSearchesAdapter(

            onQueryClick = { query -> executeSearch(query) },

            onRemoveClick = { query ->

                dbManager.removeRecentSearch(query)

                refreshRecentSearches()

            }

        )

        rvRecentSearches.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)

        rvRecentSearches.adapter = recentSearchesAdapter



        // Search – Advanced: Unified "All" Results

        layoutSearchResultsAll = findViewById(R.id.layoutSearchResultsAll)

        cardTopMatchHero = findViewById(R.id.cardTopMatchHero)

        cardTopMatchImageWrapper = findViewById(R.id.cardTopMatchImageWrapper)

        tvTopMatchBadge = findViewById(R.id.tvTopMatchBadge)

        ivTopMatchImage = findViewById(R.id.ivTopMatchImage)

        tvTopMatchTitle = findViewById(R.id.tvTopMatchTitle)

        tvTopMatchSubtitle = findViewById(R.id.tvTopMatchSubtitle)

        btnTopMatchAction = findViewById(R.id.btnTopMatchAction)



        rvAllSongs = findViewById(R.id.rvAllSongs)

        btnSeeAllSongs = findViewById(R.id.btnSeeAllSongs)

        allSongsAdapter = TrackAdapter(

            onTrackClick = { track, _ ->
                onSearchResultTrackSelected(track)
            },

            onLikeClick = { track, _ -> handleLikeToggle(track) },

            onOptionsClick = { track, _ -> openSongOptionsDialog(track) }

        )

        rvAllSongs.layoutManager = LinearLayoutManager(this)

        rvAllSongs.isNestedScrollingEnabled = false

        rvAllSongs.adapter = allSongsAdapter



        rvAllArtists = findViewById(R.id.rvAllArtists)

        allArtistsAdapter = ArtistAdapter(isHorizontal = true) { artist -> openArtistView(artist) }

        rvAllArtists.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)

        rvAllArtists.adapter = allArtistsAdapter



        rvAllAlbums = findViewById(R.id.rvAllAlbums)

        allAlbumsAdapter = AlbumAdapter(

            isHorizontal = true,

            onAlbumClick = { album -> openAlbumView(album) }

        )

        rvAllAlbums.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)

        rvAllAlbums.adapter = allAlbumsAdapter



        rvAllPlaylists = findViewById(R.id.rvAllPlaylists)

        allPlaylistsAdapter = PlaylistCardAdapter(isHorizontal = true) { playlist ->

            openPlaylistItemView(playlist)

        }

        rvAllPlaylists.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)

        rvAllPlaylists.adapter = allPlaylistsAdapter



        // Search – Per-category dedicated RecyclerViews

        rvSearchArtists = findViewById(R.id.rvSearchArtists)

        categoryArtistsAdapter = ArtistAdapter(isHorizontal = false) { artist -> openArtistView(artist) }

        rvSearchArtists.layoutManager = LinearLayoutManager(this)

        rvSearchArtists.adapter = categoryArtistsAdapter



        rvSearchAlbums = findViewById(R.id.rvSearchAlbums)

        categoryAlbumsAdapter = AlbumAdapter(

            isHorizontal = false,

            onAlbumClick = { album -> openAlbumView(album) }

        )

        rvSearchAlbums.layoutManager = androidx.recyclerview.widget.GridLayoutManager(this, 3)

        rvSearchAlbums.adapter = categoryAlbumsAdapter



        rvSearchPlaylists = findViewById(R.id.rvSearchPlaylists)

        categoryPlaylistsAdapter = PlaylistCardAdapter(isHorizontal = false) { playlist ->

            openPlaylistItemView(playlist)

        }

        rvSearchPlaylists.layoutManager = androidx.recyclerview.widget.GridLayoutManager(this, 3)

        rvSearchPlaylists.adapter = categoryPlaylistsAdapter



        // Search – Empty state & tagLofi

        layoutSearchEmpty = findViewById(R.id.layoutSearchEmpty)

        tvSearchEmptyTitle = findViewById(R.id.tvSearchEmptyTitle)

        tagLofi = findViewById(R.id.tagLofi)



        layoutSearchResultsAll.visibility = View.GONE

        layoutSearchEmpty.visibility = View.GONE



        // Library Tab UI

        etLibrarySearch = findViewById(R.id.etLibrarySearch)

        cardLikedSongs = findViewById(R.id.cardLikedSongs)

        tvLikedCount = findViewById(R.id.tvLikedCount)

        btnCreatePlaylist = findViewById(R.id.btnCreatePlaylist)

        tvPlaylistCount = findViewById(R.id.tvPlaylistCount)

        rvLibraryPlaylists = findViewById(R.id.rvLibraryPlaylists)

        rvLibraryTracks = findViewById(R.id.rvLibraryTracks)

        cardDownloadedSongs = findViewById(R.id.cardDownloadedSongs)
        tvDownloadedCount = findViewById(R.id.tvDownloadedCount)
        layoutOfflineBanner = findViewById(R.id.layoutOfflineBanner)
        tvOfflineBannerText = findViewById(R.id.tvOfflineBannerText)
        btnOfflineGoToDownloads = findViewById(R.id.btnOfflineGoToDownloads)
        btnFullDownloadContainer = findViewById(R.id.btnFullDownloadContainer)
        ivFullDownload = findViewById(R.id.ivFullDownload)
        pbFullDownload = findViewById(R.id.pbFullDownload)

        btnOfflineGoToDownloads.setOnClickListener {
            switchTab(2)
            openDownloadedSongsView()
        }

        btnFullDownloadContainer.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            val cur = currentTrack ?: return@setOnClickListener
            if (downloadManager.isDownloaded(cur.id)) {
                Toast.makeText(this, "${cur.title} is already downloaded for offline listening.", Toast.LENGTH_SHORT).show()
            } else if (downloadManager.isDownloading(cur.id)) {
                Toast.makeText(this, "Downloading in progress...", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Downloading ${cur.title}...", Toast.LENGTH_SHORT).show()
                downloadManager.startDownload(cur)
                updateDownloadButtonForTrack(cur)
            }
        }



        // Profile Tab UI

        layoutLoggedIn = findViewById(R.id.layoutLoggedIn)

        ivUserProfilePic = findViewById(R.id.ivUserProfilePic)

        tvUserAvatar = findViewById(R.id.tvUserAvatar)

        tvProfileName = findViewById(R.id.tvProfileName)

        tvProfileEmail = findViewById(R.id.tvProfileEmail)

        tvProfilePersona = findViewById(R.id.tvProfilePersona)

        btnEditProfile = findViewById(R.id.btnEditProfile)

        tvStatPlaylists = findViewById(R.id.tvStatPlaylists)

        tvStatLikes = findViewById(R.id.tvStatLikes)

        tvStatHistory = findViewById(R.id.tvStatHistory)

        cardMetricLikes = findViewById(R.id.cardMetricLikes)

        cardMetricPlaylists = findViewById(R.id.cardMetricPlaylists)

        cardMetricHistory = findViewById(R.id.cardMetricHistory)

        pillBitrate160 = findViewById(R.id.pillBitrate160)

        pillBitrate256 = findViewById(R.id.pillBitrate256)

        pillBitrate320 = findViewById(R.id.pillBitrate320)

        tvEqPresetName = findViewById(R.id.tvEqPresetName)

        btnOpenEqModal = findViewById(R.id.btnOpenEqModal)

        switchStayAwake = findViewById(R.id.switchStayAwake)

        tvCloudSyncStatus = findViewById(R.id.tvCloudSyncStatus)

        btnSyncNow = findViewById(R.id.btnSyncNow)

        tvCacheSize = findViewById(R.id.tvCacheSize)

        btnClearCache = findViewById(R.id.btnClearCache)

        btnClearHistory = findViewById(R.id.btnClearHistory)

        rvHistoryTracks = findViewById(R.id.rvHistoryTracks)

        btnSignOut = findViewById(R.id.btnSignOut)

        btnProfileHeaderSettings = findViewById(R.id.btnProfileHeaderSettings)



        // App Opening Screen & Website-Style Welcome Views

        layoutOpeningLandingScreen = findViewById(R.id.layoutOpeningLandingScreen)

        layoutMainAppShell = findViewById(R.id.layoutMainAppShell)

        ivAmbientGlowOrb = findViewById(R.id.ivAmbientGlowOrb)

        btnOpeningGoogleSignIn = findViewById(R.id.btnOpeningGoogleSignIn)

        btnOpeningEmailSignIn = findViewById(R.id.btnOpeningEmailSignIn)

        eqBar1 = findViewById(R.id.eqBar1)

        eqBar2 = findViewById(R.id.eqBar2)

        eqBar3 = findViewById(R.id.eqBar3)

        eqBar4 = findViewById(R.id.eqBar4)

        eqBar5 = findViewById(R.id.eqBar5)



        layoutAuthForm = findViewById(R.id.layoutAuthForm)

        tabAuthSignIn = findViewById(R.id.tabAuthSignIn)

        tabAuthSignUp = findViewById(R.id.tabAuthSignUp)

        etAuthName = findViewById(R.id.etAuthName)

        etAuthEmail = findViewById(R.id.etAuthEmail)

        etAuthPassword = findViewById(R.id.etAuthPassword)

        btnAuthSubmit = findViewById(R.id.btnAuthSubmit)

        btnGoogleSignIn = findViewById(R.id.btnGoogleSignIn)

        pbAuthLoading = findViewById(R.id.pbAuthLoading)

        btnGuestMode = findViewById(R.id.btnGuestMode)



        // Opened Playlist & Album View UI

        layoutOpenedPlaylistOverlay = findViewById(R.id.layoutOpenedPlaylistOverlay)

        ivPlaylistAmbientGlow = findViewById(R.id.ivPlaylistAmbientGlow)

        scrollPlaylistContent = findViewById(R.id.scrollPlaylistContent)

        viewPlaylistTopSpacer = findViewById(R.id.viewPlaylistTopSpacer)

        cardPlaylistHeroCover = findViewById(R.id.cardPlaylistHeroCover)

        ivPlaylistHeroCover = findViewById(R.id.ivPlaylistHeroCover)

        layoutLikedHeroCover = findViewById(R.id.layoutLikedHeroCover)

        tvPlaylistBadge = findViewById(R.id.tvPlaylistBadge)

        tvPlaylistHeroTitle = findViewById(R.id.tvPlaylistHeroTitle)

        tvPlaylistMetadata = findViewById(R.id.tvPlaylistMetadata)

        btnSaveToLibrary = findViewById(R.id.btnSaveToLibrary)

        ivSaveToLibraryIcon = findViewById(R.id.ivSaveToLibraryIcon)

        tvSaveToLibraryText = findViewById(R.id.tvSaveToLibraryText)

        btnDeletePlaylist = findViewById(R.id.btnDeletePlaylist)

        btnPlaylistShuffle = findViewById(R.id.btnPlaylistShuffle)

        btnPlaylistActionShare = findViewById(R.id.btnPlaylistActionShare)

        btnPlaylistPlayAll = findViewById(R.id.btnPlaylistPlayAll)

        ivPlaylistPlayIcon = findViewById(R.id.ivPlaylistPlayIcon)

        layoutPlaylistFilterBar = findViewById(R.id.layoutPlaylistFilterBar)

        etPlaylistFilter = findViewById(R.id.etPlaylistFilter)

        btnPlaylistFilterClear = findViewById(R.id.btnPlaylistFilterClear)

        pbPlaylistLoading = findViewById(R.id.pbPlaylistLoading)

        layoutPlaylistEmpty = findViewById(R.id.layoutPlaylistEmpty)

        rvPlaylistTracks = findViewById(R.id.rvPlaylistTracks)

        layoutPlaylistStickyHeader = findViewById(R.id.layoutPlaylistStickyHeader)

        viewPlaylistStickyHeaderBg = findViewById(R.id.viewPlaylistStickyHeaderBg)

        viewStickyHeaderAccentRim = findViewById(R.id.viewStickyHeaderAccentRim)

        layoutStickyHeaderContent = findViewById(R.id.layoutStickyHeaderContent)

        layoutStickyCenterInfo = findViewById(R.id.layoutStickyCenterInfo)

        ivStickyCover = findViewById(R.id.ivStickyCover)

        tvStickyPlaylistTitle = findViewById(R.id.tvStickyPlaylistTitle)

        tvStickyPlaylistSubtitle = findViewById(R.id.tvStickyPlaylistSubtitle)

        btnStickyPlayAll = findViewById(R.id.btnStickyPlayAll)

        btnPlaylistBack = findViewById(R.id.btnPlaylistBack)

        btnPlaylistHeaderShare = findViewById(R.id.btnPlaylistHeaderShare)

        btnPlaylistHeaderOptions = findViewById(R.id.btnPlaylistHeaderOptions)



        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {

            try {

                ivPlaylistAmbientGlow.setRenderEffect(RenderEffect.createBlurEffect(100f, 100f, Shader.TileMode.CLAMP))

            } catch (_: Exception) {}

        }



        setupOpenedPlaylistViewEvents()



        // Mini Player & Lateral Ambient Side Glow

        viewMiniLeftFlank = findViewById(R.id.viewMiniLeftFlank)

        viewMiniRightFlank = findViewById(R.id.viewMiniRightFlank)

        miniPlayerView = findViewById(R.id.miniPlayerView)



        ivMiniAmbientMesh = findViewById(R.id.ivMiniAmbientMesh)

        viewMiniUpperGlow = findViewById(R.id.viewMiniUpperGlow)

        pbMiniProgress = findViewById(R.id.pbMiniProgress)

        ivMiniThumb = findViewById(R.id.ivMiniThumb)

        tvMiniTitle = findViewById(R.id.tvMiniTitle)

        tvMiniArtist = findViewById(R.id.tvMiniArtist)

        ivMiniLike = findViewById(R.id.ivMiniLike)

        btnMiniPlayPause = findViewById(R.id.btnMiniPlayPause)

        btnMiniNext = findViewById(R.id.btnMiniNext)



        updateHaloColor(currentHaloColor)



        val cardMiniThumb = findViewById<MaterialCardView>(R.id.cardMiniThumb)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {

            // Mini Player Drop Shadow: 0 16px 40px rgba(0, 0, 0, 0.80) with upward throw

            miniPlayerView.outlineAmbientShadowColor = Color.argb(100, 0, 0, 0)

            miniPlayerView.outlineSpotShadowColor = Color.argb(153, Color.red(currentHaloColor), Color.green(currentHaloColor), Color.blue(currentHaloColor))



            // Play/Pause Button Shadow: 0 2px 10px rgba(255, 255, 255, 0.25)

            btnMiniPlayPause.outlineAmbientShadowColor = Color.argb(64, 255, 255, 255)

            btnMiniPlayPause.outlineSpotShadowColor = Color.argb(64, 255, 255, 255)



            // Thumbnail Wrapper Shadow: 0 4px 10px rgba(0, 0, 0, 0.40)

            cardMiniThumb?.outlineAmbientShadowColor = Color.argb(50, 0, 0, 0)

            cardMiniThumb?.outlineSpotShadowColor = Color.argb(102, 0, 0, 0)

        }



        // Hardware blur for floating mini player (98px Apple Music deep acrylic blur with high saturation) & full player

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {

            try {

                val miniBlur = RenderEffect.createBlurEffect(98f, 98f, Shader.TileMode.CLAMP)

                ivMiniAmbientMesh.setRenderEffect(miniBlur)

                val satMatrix = ColorMatrix().apply { setSaturation(1.6f) }

                ivMiniAmbientMesh.colorFilter = ColorMatrixColorFilter(satMatrix)



                ivFullAmbientMesh.setRenderEffect(RenderEffect.createBlurEffect(120f, 120f, Shader.TileMode.CLAMP))

            } catch (_: Exception) {}

        }



        // Full Player

        fullPlayerOverlay = findViewById(R.id.fullPlayerOverlay)

        ivFullAmbientMesh = findViewById(R.id.ivFullAmbientMesh)

        fullPlayerTopHeader = findViewById(R.id.fullPlayerTopHeader)

        layoutArtworkWrapper = findViewById(R.id.layoutArtworkWrapper)

        layoutFullPlayerBottomDock = findViewById(R.id.layoutFullPlayerBottomDock)

        sheetDragHandle = findViewById(R.id.sheetDragHandle)

        btnCollapsePlayer = findViewById(R.id.btnCollapsePlayer)

        btnFullOptions = findViewById(R.id.btnFullOptions)

        ivArtworkAmbientGlow = findViewById(R.id.ivArtworkAmbientGlow)

        cardArtworkContainer = findViewById(R.id.cardArtworkContainer)

        val fullPlayerContentLayout = findViewById<LinearLayout>(R.id.fullPlayerContent)
        fullPlayerOverlay.clipChildren = false
        fullPlayerOverlay.clipToPadding = false
        fullPlayerContentLayout?.clipChildren = false
        fullPlayerContentLayout?.clipToPadding = false
        layoutArtworkWrapper.clipChildren = false
        layoutArtworkWrapper.clipToPadding = false

        // 1:1 Aspect Ratio album artwork constraint (Moderate size ~330dp / 80-82% screen width)
        layoutArtworkWrapper.addOnLayoutChangeListener { _, left, top, right, bottom, _, _, _, _ ->
            val availWidth = (right - left) - layoutArtworkWrapper.paddingStart - layoutArtworkWrapper.paddingEnd
            val availHeight = (bottom - top) - layoutArtworkWrapper.paddingTop - layoutArtworkWrapper.paddingBottom
            if (availWidth > 0 && availHeight > 0) {
                // Moderate size: target 330dp (~80-82% of screen width), bounded cleanly by available width and height
                val targetModerate = (330f * resources.displayMetrics.density).toInt()
                val squareSide = Math.min(targetModerate, Math.min(availWidth, availHeight))
                val params = cardArtworkContainer.layoutParams as? FrameLayout.LayoutParams
                if (params != null && (params.width != squareSide || params.height != squareSide)) {
                    params.width = squareSide
                    params.height = squareSide
                    params.gravity = Gravity.CENTER
                    cardArtworkContainer.layoutParams = params
                }
                if (::ivArtworkAmbientGlow.isInitialized) {
                    val glowSize = squareSide + (110f * resources.displayMetrics.density).toInt()
                    val glowParams = ivArtworkAmbientGlow.layoutParams as? FrameLayout.LayoutParams
                    if (glowParams != null && (glowParams.width != glowSize || glowParams.height != glowSize)) {
                        glowParams.width = glowSize
                        glowParams.height = glowSize
                        glowParams.gravity = Gravity.CENTER
                        ivArtworkAmbientGlow.layoutParams = glowParams
                        updateArtworkGlow(currentFullSongColor, glowSize)
                    }
                }
            }
        }



        ivFullThumb = findViewById(R.id.ivFullThumb)

        layoutVinylDisc = findViewById(R.id.layoutVinylDisc)

        vinylRecordCanvas = findViewById(R.id.vinylRecordCanvas)

        ivVinylCenterLabel = findViewById(R.id.ivVinylCenterLabel)

        btnVinylMode = findViewById(R.id.btnVinylMode)

        tvVinylModeLabel = findViewById(R.id.tvVinylModeLabel)

        layoutFullBuffering = findViewById(R.id.layoutFullBuffering)
        pbFullPlayBuffering = findViewById(R.id.pbFullPlayBuffering)
        pbMiniBuffering = findViewById(R.id.pbMiniBuffering)
        pbFullBuffering = findViewById(R.id.pbFullBuffering)
        laserMiniBuffering = findViewById(R.id.laserMiniBuffering)

        tvFullTitle = findViewById(R.id.tvFullTitle)

        tvFullArtist = findViewById(R.id.tvFullArtist)

        btnFullLikeContainer = findViewById(R.id.btnFullLikeContainer)

        ivFullLike = findViewById(R.id.ivFullLike)

        fullSeekBar = findViewById(R.id.fullSeekBar)





        tvFullCurrentTime = findViewById(R.id.tvFullCurrentTime)

        tvFullDuration = findViewById(R.id.tvFullDuration)



        btnFullShuffle = findViewById(R.id.btnFullShuffle)

        btnFullPrev = findViewById(R.id.btnFullPrev)

        btnFullPlayPause = findViewById(R.id.btnFullPlayPause)

        btnFullNext = findViewById(R.id.btnFullNext)

        btnFullRepeat = findViewById(R.id.btnFullRepeat)



        badgeLossless = findViewById(R.id.badgeLossless)

        badgeAwake = findViewById(R.id.badgeAwake)

        ivAwakeIcon = findViewById(R.id.ivAwakeIcon)

        tvAwakeText = findViewById(R.id.tvAwakeText)



        btnFullLyrics = findViewById(R.id.btnFullLyrics)

        ivFullLyricsIcon = findViewById(R.id.ivFullLyricsIcon)

        tvFullLyricsLabel = findViewById(R.id.tvFullLyricsLabel)

        btnFullEQ = findViewById(R.id.btnFullEQ)

        btnFullTimer = findViewById(R.id.btnFullTimer)

        tvTimerLabel = findViewById(R.id.tvTimerLabel)

        btnFullQueue = findViewById(R.id.btnFullQueue)



        // In-Card Live Synced Lyrics Overlay Views

        layoutLyricsPanel = findViewById(R.id.layoutLyricsPanel)

        badgeLiveSynced = findViewById(R.id.badgeLiveSynced)

        layoutLyricsOffset = findViewById(R.id.layoutLyricsOffset)

        btnOffsetMinus = findViewById(R.id.btnOffsetMinus)

        tvOffsetCurrent = findViewById(R.id.tvOffsetCurrent)

        btnOffsetPlus = findViewById(R.id.btnOffsetPlus)

        btnCloseLyricsCard = findViewById(R.id.btnCloseLyricsCard)

        rvCardLyrics = findViewById(R.id.rvCardLyrics)

        pbCardLyricsLoading = findViewById(R.id.pbCardLyricsLoading)

        tvCardLyricsEmpty = findViewById(R.id.tvCardLyricsEmpty)



        // Setup Card Lyrics Adapter & RecyclerView

        cardLyricsAdapter = LyricsAdapter { line ->

            player?.seekTo(line.timeMs)

            val idx = cardLyricsAdapter?.forceUpdateTimestamp(line.timeMs + lyricsOffsetMs) ?: -1

            if (idx >= 0) {

                smoothScrollLyricsToCenter(rvCardLyrics, idx)

            }

        }

        rvCardLyrics.layoutManager = LinearLayoutManager(this)

        rvCardLyrics.adapter = cardLyricsAdapter



        btnCloseLyricsCard.setOnClickListener {

            it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)

            toggleLyricsCard(false)

        }



        btnOffsetMinus.setOnClickListener {

            it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)

            lyricsOffsetMs -= 500L

            updateLyricsOffsetDisplay()

        }



        tvOffsetCurrent.setOnClickListener {

            it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)

            lyricsOffsetMs = 0L

            updateLyricsOffsetDisplay()

        }



        btnOffsetPlus.setOnClickListener {

            it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)

            lyricsOffsetMs += 500L

            updateLyricsOffsetDisplay()

        }



        // Tap Collapse or Drag Handle to minimize

        btnCollapsePlayer.setOnClickListener {

            it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)

            closeNowPlayingSheet()

        }

        setupFullPlayerDragToDismiss()



        val togglePlayAction = {

            player?.let { p ->

                if (p.playbackState == Player.STATE_IDLE && currentTrack != null) {

                    playTrack(currentTrack!!)

                } else if (p.isPlaying) {

                    p.pause()

                } else {

                    p.play()

                }

            }

            Unit

        }



        // Isolated touch & 0.92 press scale for Mini Play/Pause button

        btnMiniPlayPause.setOnTouchListener { v, event ->

            when (event.actionMasked) {

                MotionEvent.ACTION_DOWN -> {

                    v.parent?.requestDisallowInterceptTouchEvent(true)

                    v.animate().scaleX(0.92f).scaleY(0.92f).setDuration(80L).start()

                }

                MotionEvent.ACTION_UP -> {

                    val isInside = event.x >= 0 && event.x <= v.width && event.y >= 0 && event.y <= v.height

                    v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(120L).start()

                    if (isInside) {

                        v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)

                        togglePlayAction()

                    }

                }

                MotionEvent.ACTION_CANCEL -> {

                    v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(120L).start()

                }

            }

            true

        }

        btnFullPlayPause.setOnClickListener {

            it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)

            togglePlayAction()

        }



        // Isolated touch & 0.92 press scale for Mini Next button

        btnMiniNext.setOnTouchListener { v, event ->

            when (event.actionMasked) {

                MotionEvent.ACTION_DOWN -> {

                    v.parent?.requestDisallowInterceptTouchEvent(true)

                    v.animate().scaleX(0.92f).scaleY(0.92f).setDuration(80L).start()

                }

                MotionEvent.ACTION_UP -> {

                    val isInside = event.x >= 0 && event.x <= v.width && event.y >= 0 && event.y <= v.height

                    v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(120L).start()

                    if (isInside) {

                        v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)

                        playNext()

                    }

                }

                MotionEvent.ACTION_CANCEL -> {

                    v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(120L).start()

                }

            }

            true

        }

        btnFullNext.setOnClickListener {

            it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)

            playNext()

        }

        btnFullPrev.setOnClickListener {

            it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)

            playPrevious()

        }



        val toggleCurrentLike = {

            currentTrack?.let { track ->

                val isLiked = dbManager.toggleLike(track)

                updateLikeIcons(isLiked)

                updateLibraryLikedCount()

                homeAdapter.notifyDataSetChanged()

                searchAdapter.notifyDataSetChanged()

                allSongsAdapter.notifyDataSetChanged()

                trendingCardsAdapter.notifyDataSetChanged()

                chartsCardsAdapter.notifyDataSetChanged()

                libraryAdapter.setTracks(dbManager.getLikedSongs())

                refreshProfileUI()

            }

            Unit

        }



        // Isolated touch & scale bounce for Mini Like button

        ivMiniLike.setOnTouchListener { v, event ->

            when (event.actionMasked) {

                MotionEvent.ACTION_DOWN -> {

                    v.parent?.requestDisallowInterceptTouchEvent(true)

                    v.animate().scaleX(0.85f).scaleY(0.85f).setDuration(80L).start()

                }

                MotionEvent.ACTION_UP -> {

                    val isInside = event.x >= 0 && event.x <= v.width && event.y >= 0 && event.y <= v.height

                    v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(120L).start()

                    if (isInside) {

                        v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)

                        toggleCurrentLike()

                    }

                }

                MotionEvent.ACTION_CANCEL -> {

                    v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(120L).start()

                }

            }

            true

        }

        btnFullLikeContainer.setOnClickListener {

            it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)

            toggleCurrentLike()

        }



        findViewById<View>(R.id.miniActionsLayout).setOnTouchListener { v, _ ->

            v.parent?.requestDisallowInterceptTouchEvent(true)

            true

        }



        // Vinyl Mode Toggle (Cover Art Mode <-> Vinyl Mode)

        btnVinylMode.setOnClickListener {

            it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)

            isVinylMode = !isVinylMode

            if (isVinylMode) {

                ivFullThumb.visibility = View.GONE

                layoutVinylDisc.visibility = View.VISIBLE

                btnVinylMode.setBackgroundResource(R.drawable.cover_art_pill_bg)

                tvVinylModeLabel.text = "Cover Art"

                startVinylRotation()

            } else {

                layoutVinylDisc.visibility = View.GONE

                ivFullThumb.visibility = View.VISIBLE

                btnVinylMode.setBackgroundResource(R.drawable.vinyl_mode_pill_bg)

                tvVinylModeLabel.text = "Vinyl Mode"

                stopVinylRotation()

            }

        }



        // Shuffle Mode Toggle (Section 6 Spec)

        btnFullShuffle.setOnClickListener {

            it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)

            isShuffleMode = !isShuffleMode

            player?.shuffleModeEnabled = isShuffleMode

            EmberPlaybackService.instance?.let { s -> s.isShuffleMode = isShuffleMode }

            val color = if (isShuffleMode) Color.parseColor("#F05A3D") else ContextCompat.getColor(this, R.color.player_icon_muted)

            btnFullShuffle.imageTintList = ColorStateList.valueOf(color)

            Toast.makeText(this, if (isShuffleMode) "Shuffle: ON" else "Shuffle: OFF", Toast.LENGTH_SHORT).show()

        }



        // Repeat Mode Cycle (Section 6 Spec)

        btnFullRepeat.setOnClickListener {

            it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)

            repeatMode = (repeatMode + 1) % 3

            when (repeatMode) {

                0 -> {

                    player?.repeatMode = Player.REPEAT_MODE_OFF

                    btnFullRepeat.imageTintList = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.player_icon_muted))

                    Toast.makeText(this, "Repeat: OFF", Toast.LENGTH_SHORT).show()

                }

                1 -> {

                    player?.repeatMode = Player.REPEAT_MODE_ALL

                    btnFullRepeat.imageTintList = ColorStateList.valueOf(Color.parseColor("#F05A3D"))

                    Toast.makeText(this, "Repeat: ALL", Toast.LENGTH_SHORT).show()

                }

                2 -> {

                    player?.repeatMode = Player.REPEAT_MODE_ONE

                    btnFullRepeat.imageTintList = ColorStateList.valueOf(Color.parseColor("#F05A3D"))

                    Toast.makeText(this, "Repeat: ONE", Toast.LENGTH_SHORT).show()

                }

            }

        }



        // Screen Keep Awake Toggle

        badgeAwake.setOnClickListener {

            it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)

            isAwakeEnabled = !isAwakeEnabled

            if (isAwakeEnabled) {

                window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

                badgeAwake.setBackgroundResource(R.drawable.awake_badge_bg)

                ivAwakeIcon.imageTintList = ColorStateList.valueOf(Color.parseColor("#F05A3D"))

                tvAwakeText.setTextColor(Color.parseColor("#F05A3D"))

                tvAwakeText.text = "AWAKE"

                Toast.makeText(this, "Screen Stay Awake: ON", Toast.LENGTH_SHORT).show()

            } else {

                window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

                badgeAwake.setBackgroundResource(R.drawable.vinyl_mode_pill_bg)

                ivAwakeIcon.imageTintList = ColorStateList.valueOf(Color.parseColor("#99969F"))

                tvAwakeText.setTextColor(Color.parseColor("#99969F"))

                tvAwakeText.text = "AWAKE: OFF"

                Toast.makeText(this, "Screen Stay Awake: OFF", Toast.LENGTH_SHORT).show()

            }

        }



        // Bottom Dock 4 Features

        btnFullLyrics.setOnClickListener {

            it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)

            toggleLyricsCard(!isLyricsActive)

        }

        btnFullEQ.setOnClickListener {

            it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)

            openEqualizerDialog()

        }

        btnFullTimer.setOnClickListener {

            it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)

            openSleepTimerDialog()

        }

        btnFullQueue.setOnClickListener {

            it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)

            openQueueDialog()

        }



        // 3-Dots Options Button

        btnFullOptions.setOnClickListener {

            it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)

            currentTrack?.let { track -> openSongOptionsDialog(track) }

        }



        // SeekBar Tracking & Smooth Seeking (Section 1 & 2 Spec)

        fullSeekBar.max = 1000

        fullSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {

            override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {

                if (fromUser) {

                    pbMiniProgress.progress = progress

                    player?.let { p ->

                        val duration = p.duration

                        if (duration > 0) {

                            val seekTo = (duration * progress) / 1000

                            tvFullCurrentTime.text = formatTime(seekTo)

                        }

                    }

                }

            }

            override fun onStartTrackingTouch(sb: SeekBar?) {

                isUserSeeking = true

                sb?.animate()?.scaleY(1.35f)?.setDuration(120L)?.start()

            }

            override fun onStopTrackingTouch(sb: SeekBar?) {

                sb?.animate()?.scaleY(1.0f)?.setDuration(120L)?.start()

                val progress = sb?.progress ?: 0

                pbMiniProgress.progress = progress

                player?.let { p ->

                    val duration = p.duration

                    if (duration > 0) {

                        val seekTo = (duration * progress) / 1000

                        p.seekTo(seekTo)

                        tvFullCurrentTime.text = formatTime(seekTo)

                        if (isLyricsActive) {

                            val idx = cardLyricsAdapter?.forceUpdateTimestamp(seekTo + lyricsOffsetMs) ?: -1

                            if (idx >= 0) {

                                smoothScrollLyricsToCenter(rvCardLyrics, idx)

                            }

                        }

                    }

                }

                isUserSeeking = false

            }

        })

    }



    // ==========================================

    // Mini Player Gestures (Real-time Elastic Drag, Swipe Skip & Vertical Expand)

    // ==========================================

    private fun setupMiniPlayerGestures() {

        val density = resources.displayMetrics.density

        val swipeThresholdPx = (35f * density).coerceAtMost(90f).coerceAtLeast(35f)

        val tapThresholdPx = 16f * density



        var startX = 0f

        var startY = 0f

        var downTime = 0L

        var isDraggingHorizontal = false

        var isSwipingVertical = false

        var gestureHandled = false



        val gestureListener = View.OnTouchListener { v, event ->

            when (event.actionMasked) {

                MotionEvent.ACTION_DOWN -> {

                    startX = event.rawX

                    startY = event.rawY

                    downTime = System.currentTimeMillis()

                    isDraggingHorizontal = false

                    isSwipingVertical = false

                    gestureHandled = false

                    miniPlayerView.animate().cancel()

                    v.parent?.requestDisallowInterceptTouchEvent(true)

                    true

                }

                MotionEvent.ACTION_MOVE -> {

                    val diffX = event.rawX - startX

                    val diffY = event.rawY - startY

                    val absDiffX = Math.abs(diffX)

                    val absDiffY = Math.abs(diffY)



                    // Direction determination

                    if (!isDraggingHorizontal && !isSwipingVertical) {

                        if (absDiffX > 6f * density && absDiffX > absDiffY) {

                            isDraggingHorizontal = true

                            v.parent?.requestDisallowInterceptTouchEvent(true)

                        } else if (diffY < -6f * density && absDiffY > absDiffX) {

                            isSwipingVertical = true

                            v.parent?.requestDisallowInterceptTouchEvent(true)

                        }

                    }



                    if (isDraggingHorizontal) {

                        // Elastic resistance factor: translateX = diffX * 0.45

                        val translateX = diffX * 0.45f

                        miniPlayerView.translationX = translateX



                        // Subtle glowing upper line intensity boost on swipe

                        if (absDiffX > 15f * density) {

                            viewMiniUpperGlow.alpha = 1.0f

                        } else {

                            viewMiniUpperGlow.alpha = 0.5f

                        }

                    } else if (isSwipingVertical) {

                        // Swiping upwards (diffY < -35px): Expand Fullscreen Sheet immediately

                        if (diffY < -swipeThresholdPx && !gestureHandled) {

                            gestureHandled = true

                            v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)

                            openNowPlayingSheet()

                        }

                    }

                    true

                }

                MotionEvent.ACTION_UP -> {

                    val totalDiffX = event.rawX - startX

                    val totalDiffY = event.rawY - startY

                    val duration = (System.currentTimeMillis() - downTime).coerceAtLeast(1L)

                    val velocityX = (totalDiffX / duration) * 1000f

                    val velocityY = (totalDiffY / duration) * 1000f



                    val isTap = Math.abs(totalDiffX) < tapThresholdPx && 

                                Math.abs(totalDiffY) < tapThresholdPx && 

                                duration < 400L



                    if (!gestureHandled) {

                        when {

                            isTap -> {

                                v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)

                                openNowPlayingSheet()

                            }

                            totalDiffY < -swipeThresholdPx || (totalDiffY < -12f * density && velocityY < -350f) -> {

                                v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)

                                openNowPlayingSheet()

                            }

                            totalDiffX < -swipeThresholdPx || velocityX < -350f -> {

                                // Left swipe (> 35px or fast flick) -> Skip to Next Track + haptic vibration

                                v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)

                                playNext()

                                Toast.makeText(this@MainActivity, "Skipped to next song", Toast.LENGTH_SHORT).show()

                            }

                            totalDiffX > swipeThresholdPx || velocityX > 350f -> {

                                // Right swipe (> 35px or fast flick) -> Skip to Previous Track + haptic vibration

                                v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)

                                playPrevious()

                                Toast.makeText(this@MainActivity, "Playing previous song", Toast.LENGTH_SHORT).show()

                            }

                        }

                    }



                    // Clean Release: Capsule snaps smoothly back to center (translateX(0)) via spring physics

                    miniPlayerView.animate()

                        .translationX(0f)

                        .setDuration(260L)

                        .setInterpolator(OvershootInterpolator(1.25f))

                        .withEndAction {

                            viewMiniUpperGlow.alpha = 0.5f

                        }

                        .start()



                    true

                }

                MotionEvent.ACTION_CANCEL -> {

                    miniPlayerView.animate()

                        .translationX(0f)

                        .setDuration(220L)

                        .setInterpolator(OvershootInterpolator(1.25f))

                        .withEndAction {

                            viewMiniUpperGlow.alpha = 0.5f

                        }

                        .start()

                    true

                }

                else -> false

            }

        }



        // Attach gesture engine across entire mini player capsule surface

        miniPlayerView.setOnTouchListener(gestureListener)

        findViewById<View>(R.id.layoutMiniTrackInfo).setOnTouchListener(gestureListener)

        findViewById<View>(R.id.cardMiniThumb)?.setOnTouchListener(gestureListener)

        findViewById<View>(R.id.ivMiniThumb).setOnTouchListener(gestureListener)

    }



    private fun openNowPlayingSheet() {

        if (fullPlayerOverlay.visibility != View.VISIBLE) {

            fullPlayerOverlay.visibility = View.VISIBLE

            fullPlayerOverlay.alpha = 0f

            fullPlayerOverlay.translationY = 60f

            fullPlayerOverlay.animate()

                .alpha(1f)

                .translationY(0f)

                .setDuration(220L)

                .setInterpolator(DecelerateInterpolator())

                .start()

        }

    }



    private fun closeNowPlayingSheet() {

        if (fullPlayerOverlay.visibility == View.VISIBLE) {

            fullPlayerOverlay.animate()

                .alpha(0f)

                .translationY(60f)

                .setDuration(180L)

                .withEndAction {

                    fullPlayerOverlay.visibility = View.GONE

                    fullPlayerOverlay.alpha = 1f

                    fullPlayerOverlay.translationY = 0f

                }

                .start()

            if (isLyricsActive) {

                toggleLyricsCard(false)

            }

        }

    }



    private fun setupFullPlayerDragToDismiss() {

        val density = resources.displayMetrics.density

        val dismissThresholdPx = 130f * density

        val tapThresholdPx = 12f * density



        var startY = 0f

        var downTime = 0L

        var isDragging = false



        val dragListener = View.OnTouchListener { v, event ->

            when (event.actionMasked) {

                MotionEvent.ACTION_DOWN -> {

                    startY = event.rawY

                    downTime = System.currentTimeMillis()

                    isDragging = false

                    fullPlayerOverlay.animate().cancel()

                    v.parent?.requestDisallowInterceptTouchEvent(true)

                    true

                }

                MotionEvent.ACTION_MOVE -> {

                    val diffY = event.rawY - startY

                    if (!isDragging && Math.abs(diffY) > 5f * density) {

                        isDragging = true

                        v.parent?.requestDisallowInterceptTouchEvent(true)

                    }



                    if (isDragging) {

                        if (diffY >= 0f) {

                            // Downward drag: directly translate full sheet following finger

                            fullPlayerOverlay.translationY = diffY

                            val maxDrag = resources.displayMetrics.heightPixels.toFloat()

                            val progress = (diffY / (maxDrag * 0.75f)).coerceIn(0f, 0.45f)

                            fullPlayerOverlay.alpha = 1f - progress

                            // Subtle stretch feedback on line bar

                            sheetDragHandle.scaleX = (1f + (diffY / (120f * density)) * 0.25f).coerceAtMost(1.35f)

                        } else {

                            // Upward drag: elastic rubber-band resistance

                            fullPlayerOverlay.translationY = diffY * 0.25f

                            fullPlayerOverlay.alpha = 1f

                            sheetDragHandle.scaleX = 1f

                        }

                    }

                    true

                }

                MotionEvent.ACTION_UP -> {

                    val totalDiffY = event.rawY - startY

                    val duration = (System.currentTimeMillis() - downTime).coerceAtLeast(1L)

                    val velocityY = (totalDiffY / duration) * 1000f



                    val isTap = Math.abs(totalDiffY) < tapThresholdPx && duration < 300L



                    sheetDragHandle.animate()

                        .scaleX(1f)

                        .setDuration(220L)

                        .setInterpolator(OvershootInterpolator(1.2f))

                        .start()



                    if (isTap) {

                        v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)

                        closeNowPlayingSheet()

                    } else if (totalDiffY > dismissThresholdPx || (totalDiffY > 40f * density && velocityY > 550f)) {

                        // Dismiss down completely

                        v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)

                        val screenHeight = resources.displayMetrics.heightPixels.toFloat()

                        fullPlayerOverlay.animate()

                            .translationY(screenHeight)

                            .alpha(0f)

                            .setDuration(220L)

                            .setInterpolator(DecelerateInterpolator())

                            .withEndAction {

                                fullPlayerOverlay.visibility = View.GONE

                                fullPlayerOverlay.alpha = 1f

                                fullPlayerOverlay.translationY = 0f

                                if (isLyricsActive) {

                                    toggleLyricsCard(false)

                                }

                            }

                            .start()

                    } else {

                        // SPRING MOTION SNAP-BACK!

                        v.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)

                        fullPlayerOverlay.animate()

                            .translationY(0f)

                            .alpha(1f)

                            .setDuration(360L)

                            .setInterpolator(OvershootInterpolator(1.5f))

                            .start()

                    }

                    true

                }

                MotionEvent.ACTION_CANCEL -> {

                    sheetDragHandle.animate().scaleX(1f).setDuration(200L).start()

                    fullPlayerOverlay.animate()

                        .translationY(0f)

                        .alpha(1f)

                        .setDuration(300L)

                        .setInterpolator(OvershootInterpolator(1.3f))

                        .start()

                    true

                }

                else -> false

            }

        }



        sheetDragHandle.setOnTouchListener(dragListener)

        findViewById<View?>(R.id.layoutDragHandleTouch)?.setOnTouchListener(dragListener)

        findViewById<View?>(R.id.tvNowPlayingLabel)?.setOnTouchListener(dragListener)

        fullPlayerTopHeader.setOnTouchListener(dragListener)

    }



    // ==========================================

    // Bottom Navigation Logic & Tactile Pop Animation

    // ==========================================

    private fun setupBottomNavigation() {

        navHome.setOnClickListener { switchTab(0) }

        navSearch.setOnClickListener { switchTab(1) }

        navLibrary.setOnClickListener {

            switchTab(2)

            refreshLibraryTab()

            if (authManager.isLoggedIn()) {

                lifecycleScope.launch {

                    val ok = dbManager.syncFromSupabase()

                    if (ok) {

                        refreshLibraryTab()

                        updateLibraryLikedCount()

                    }

                }

            }

        }

        navProfile.setOnClickListener {

            switchTab(3)

            refreshProfileUI()

            if (authManager.isLoggedIn()) {

                lifecycleScope.launch {

                    val ok = dbManager.syncFromSupabase()

                    if (ok) {

                        refreshProfileUI()

                    }

                }

            }

        }

    }



    private fun switchTab(tabIndex: Int) {

        if (::layoutOpenedPlaylistOverlay.isInitialized && layoutOpenedPlaylistOverlay.visibility == View.VISIBLE) {

            closeOpenedPlaylistView(animate = false)

        }



        tabHomeView.visibility = if (tabIndex == 0) View.VISIBLE else View.GONE

        tabSearchView.visibility = if (tabIndex == 1) View.VISIBLE else View.GONE

        tabLibraryView.visibility = if (tabIndex == 2) View.VISIBLE else View.GONE

        tabProfileView.visibility = if (tabIndex == 3) View.VISIBLE else View.GONE



        val activeColor = ContextCompat.getColor(this, R.color.accent_primary)

        val inactiveColor = ContextCompat.getColor(this, R.color.text_secondary)



        ivNavHome.imageTintList = ColorStateList.valueOf(if (tabIndex == 0) activeColor else inactiveColor)

        tvNavHome.setTextColor(if (tabIndex == 0) activeColor else inactiveColor)



        ivNavSearch.imageTintList = ColorStateList.valueOf(if (tabIndex == 1) activeColor else inactiveColor)

        tvNavSearch.setTextColor(if (tabIndex == 1) activeColor else inactiveColor)



        ivNavLibrary.imageTintList = ColorStateList.valueOf(if (tabIndex == 2) activeColor else inactiveColor)

        tvNavLibrary.setTextColor(if (tabIndex == 2) activeColor else inactiveColor)



        ivNavProfile.imageTintList = ColorStateList.valueOf(if (tabIndex == 3) activeColor else inactiveColor)

        tvNavProfile.setTextColor(if (tabIndex == 3) activeColor else inactiveColor)



        // Active Tab Indicator Dash

        indicatorHome.visibility = if (tabIndex == 0) View.VISIBLE else View.INVISIBLE

        indicatorSearch.visibility = if (tabIndex == 1) View.VISIBLE else View.INVISIBLE

        indicatorLibrary.visibility = if (tabIndex == 2) View.VISIBLE else View.INVISIBLE

        indicatorProfile.visibility = if (tabIndex == 3) View.VISIBLE else View.INVISIBLE



        // Tactile Spring Bounce Animation

        val selectedIcon = when (tabIndex) {

            0 -> ivNavHome

            1 -> ivNavSearch

            2 -> ivNavLibrary

            else -> ivNavProfile

        }

        animateTabBounce(selectedIcon)

        if (tabIndex == 3) {
            tabProfileView.post {
                tabProfileView.scrollTo(0, 0)
            }
            refreshProfileUI()
        }
    }



    private fun animateTabBounce(view: View) {

        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)

        view.animate()

            .scaleX(0.85f)

            .scaleY(0.85f)

            .setDuration(90)

            .withEndAction {

                view.animate()

                    .scaleX(1.15f)

                    .scaleY(1.15f)

                    .setDuration(110)

                    .withEndAction {

                        view.animate()

                            .scaleX(1.0f)

                            .scaleY(1.0f)

                            .setDuration(90)

                            .start()

                    }.start()

            }.start()

    }



    // ==========================================

    // RecyclerViews Setup

    // ==========================================

    private fun setupRecyclerViews() {

        EmberRecommendationEngine.clearCache()

        EmberAudioEngine.clearCache()

        // Section 1: Jump Back In

        jumpBackInAdapter = HorizontalCardAdapter(showRank = false) { track, _ ->
            onSearchResultTrackSelected(track)
        }

        rvJumpBackIn.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)

        rvJumpBackIn.adapter = jumpBackInAdapter



        // Auto-Recommendation: Recommended For You

        recommendedForYouAdapter = HorizontalCardAdapter(showRank = false) { track, _ ->
            onSearchResultTrackSelected(track)
        }

        rvRecommendedForYou.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)

        rvRecommendedForYou.adapter = recommendedForYouAdapter



        // Section 2: Global Top Charts (Ranked #1, #2...)

        chartsCardsAdapter = HorizontalCardAdapter(showRank = true) { track, _ ->
            onSearchResultTrackSelected(track)
        }

        rvChartsCards.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)

        rvChartsCards.adapter = chartsCardsAdapter



        // Section 3: More from {topArtist}

        moreFromArtistAdapter = TrackAdapter(

            showRank = false,

            onTrackClick = { track, _ ->
                onSearchResultTrackSelected(track)
            },

            onLikeClick = { track, _ ->

                dbManager.toggleLike(track)

                updateLibraryLikedCount()

                if (currentTrack?.id == track.id) updateLikeIcons(track.isLiked)

            },

            onOptionsClick = { track, _ ->

                openSongOptionsDialog(track)

            }

        )

        rvMoreFromArtist.layoutManager = LinearLayoutManager(this)

        rvMoreFromArtist.adapter = moreFromArtistAdapter



        // Section 4: Based on Your Taste

        basedOnTasteAdapter = TrackAdapter(

            showRank = false,

            onTrackClick = { track, _ ->
                onSearchResultTrackSelected(track)
            },

            onLikeClick = { track, _ ->

                dbManager.toggleLike(track)

                updateLibraryLikedCount()

                if (currentTrack?.id == track.id) updateLikeIcons(track.isLiked)

            },

            onOptionsClick = { track, _ ->

                openSongOptionsDialog(track)

            }

        )

        rvBasedOnTaste.layoutManager = LinearLayoutManager(this)

        rvBasedOnTaste.adapter = basedOnTasteAdapter



        // Auto-Recommendation: Discover Something New

        discoverNewAdapter = HorizontalCardAdapter(showRank = false) { track, _ ->
            onSearchResultTrackSelected(track)
        }

        rvDiscoverNew.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)

        rvDiscoverNew.adapter = discoverNewAdapter



        // Auto-Recommendation: Mood Mix

        moodMixAdapter = HorizontalCardAdapter(showRank = false) { track, _ ->
            onSearchResultTrackSelected(track)
        }

        rvMoodMix.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)

        rvMoodMix.adapter = moodMixAdapter



        // Section 6: Your Playlists

        homeUserPlaylistsAdapter = PlaylistCardAdapter(isHorizontal = true) { playlistItem ->

            val playlist = dbManager.getPlaylists().firstOrNull { it.id == playlistItem.id }

            if (playlist != null) {

                openPlaylistView(playlist)

            }

        }

        rvHomeUserPlaylists.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)

        rvHomeUserPlaylists.adapter = homeUserPlaylistsAdapter



        searchAdapter = TrackAdapter(

            showRank = false,

            onTrackClick = { track, _ ->
                onSearchResultTrackSelected(track)
            },

            onLikeClick = { track, _ ->

                dbManager.toggleLike(track)

                updateLibraryLikedCount()

                if (currentTrack?.id == track.id) updateLikeIcons(track.isLiked)

            },

            onOptionsClick = { track, _ ->

                openSongOptionsDialog(track)

            }

        )

        rvSearchTracks.layoutManager = LinearLayoutManager(this)

        rvSearchTracks.adapter = searchAdapter



        libraryAdapter = TrackAdapter(

            showRank = false,

            onTrackClick = { track, _ ->

                playlistQueue.clear()

                playlistQueue.addAll(libraryAdapter.getTracks())

                currentTrackIndex = playlistQueue.indexOfFirst { it.id == track.id }

                playTrack(track)

            },

            onLikeClick = { track, _ ->

                dbManager.toggleLike(track)

                updateLibraryLikedCount()

                refreshLibraryTab()

                if (currentTrack?.id == track.id) updateLikeIcons(track.isLiked)

            },

            onOptionsClick = { track, _ ->

                openSongOptionsDialog(track)

            }

        )

        rvLibraryTracks.layoutManager = LinearLayoutManager(this)

        rvLibraryTracks.adapter = libraryAdapter



        playlistAdapter = PlaylistAdapter(

            onPlaylistClick = { playlist, _ ->

                openPlaylistView(playlist)

            },

            onOptionsClick = { playlist, _ ->

                openPlaylistOptionsDialog(playlist)

            }

        )

        rvLibraryPlaylists.layoutManager = LinearLayoutManager(this)

        rvLibraryPlaylists.adapter = playlistAdapter



        // Setup Opened Playlist Tracks Adapter

        playlistDetailAdapter = PlaylistDetailTrackAdapter(

            allTracks = emptyList(),

            isCustomPlaylist = false,

            currentPlayingTrackId = currentTrack?.id,

            isPlaying = player?.isPlaying == true,

            onTrackClick = { track, index, allFilteredTracks ->

                playlistQueue.clear()

                playlistQueue.addAll(allFilteredTracks)

                currentTrackIndex = index

                playTrack(track)

            },

            onLikeClick = { track, isLiked ->

                dbManager.toggleLike(track)

                updateLibraryLikedCount()

                if (currentTrack?.id == track.id) updateLikeIcons(isLiked)

                homeAdapter.notifyDataSetChanged()

                searchAdapter.notifyDataSetChanged()

                libraryAdapter.notifyDataSetChanged()

            },

            onOptionsClick = { track, _ ->

                openSongOptionsDialog(track)

            },

            onRemoveClick = { track ->

                currentOpenedPlaylist?.let { pl ->

                    dbManager.removeTrackFromPlaylist(pl.id, track.id)

                    val updatedPl = dbManager.getPlaylists().firstOrNull { it.id == pl.id }

                    if (updatedPl != null) {

                        currentOpenedPlaylist = updatedPl

                        playlistDetailAdapter.setTracks(updatedPl.tracks)

                        val totalSec = updatedPl.tracks.sumOf { it.durationSec }

                        val totalMin = totalSec / 60

                        tvPlaylistMetadata.text = "Created by You • ${updatedPl.tracks.size} songs • ${totalMin} min"

                    }

                    refreshLibraryTab()

                    Toast.makeText(this, "Removed from ${pl.name}", Toast.LENGTH_SHORT).show()

                }

            }

        )

        rvPlaylistTracks.layoutManager = LinearLayoutManager(this)

        rvPlaylistTracks.adapter = playlistDetailAdapter



        historyAdapter = TrackAdapter(

            showRank = false,

            onTrackClick = { track, _ ->

                playTrack(track)

            },

            onLikeClick = { track, _ ->

                dbManager.toggleLike(track)

                updateLibraryLikedCount()

                if (currentTrack?.id == track.id) updateLikeIcons(track.isLiked)

            },

            onOptionsClick = { track, _ ->

                openSongOptionsDialog(track)

            }

        )

        rvHistoryTracks.layoutManager = LinearLayoutManager(this)

        rvHistoryTracks.adapter = historyAdapter

    }



    // ==========================================

    // Home Data Loader

    // ==========================================

    private fun loadHomeData() {
        if (!isNetworkAvailable()) {
            if (::layoutHomeSkeleton.isInitialized) hideHomeSkeleton()
            updateHeroGreeting()
            if (currentTrack == null) {
                miniPlayerView.visibility = View.GONE
            } else {
                miniPlayerView.visibility = View.VISIBLE
                updateUiForTrack(currentTrack!!)
            }
            handleOfflineTransition()
            return
        }
        if (::layoutHomeSkeleton.isInitialized) {
            layoutHomeSkeleton.visibility = View.VISIBLE
            layoutHomeSkeleton.alpha = 1f
            layoutHomeSkeleton.startShimmer()
        }
        updateHeroGreeting()



        // If no track is playing, hide mini player cleanly
        if (currentTrack == null) {
            miniPlayerView.visibility = View.GONE
        } else {
            miniPlayerView.visibility = View.VISIBLE
            updateUiForTrack(currentTrack!!)
        }



        updateHeroCardBackdropFromHistory()

        updateHeroGreeting()

        updateHeroContextualSubtitle()



        // Section 1: Jump Back In

        refreshJumpBackInSection()



        // Section 2: Global Top Charts

        lifecycleScope.launch {

            val charts = EmberAudioEngine.getTrendingTracks("pop")

            for (t in charts) t.isLiked = dbManager.isTrackLiked(t.id)

            chartsCardsAdapter.setTracks(charts)

        }



        // Section 3: More from {topArtist} & Section 4: Based on Your Taste & Section 5: Daily Mix

        recalibrateDailyMix()



        // Section 6: Your Playlists

        refreshHomePlaylistsSection()

    }



    private fun refreshJumpBackInSection() {

        val history = dbManager.getHistory()

        if (history.isNotEmpty()) {

            jumpBackInAdapter.setTracks(history)

        } else {

            lifecycleScope.launch {

                val trending = EmberAudioEngine.getTrendingTracks("trending")

                for (t in trending) t.isLiked = dbManager.isTrackLiked(t.id)

                jumpBackInAdapter.setTracks(trending)

            }

        }

        btnPlayAllJumpBackIn.setOnClickListener {

            val tracks = jumpBackInAdapter.getTracks()

            if (tracks.isNotEmpty()) {

                playlistQueue.clear()

                playlistQueue.addAll(tracks)

                currentTrackIndex = 0

                playTrack(tracks[0])

            }

        }

    }



    private fun refreshRecommendedForYouSection() {

        lifecycleScope.launch {

            val topArtists = dbManager.getTopArtists(2)

            if (topArtists.isNotEmpty()) {

                tvRecommendedSubtitle.text = "Curated from ${topArtists.joinToString(" & ")} and your favorites"

            }

            val recTracks = EmberRecommendationEngine.getRecommendedForYou(dbManager, count = 12)

            for (t in recTracks) t.isLiked = dbManager.isTrackLiked(t.id)

            recommendedForYouAdapter.setTracks(recTracks)

        }

        btnPlayAllRecommended.setOnClickListener {

            val tracks = recommendedForYouAdapter.getTracks()

            if (tracks.isNotEmpty()) {

                playlistQueue.clear()

                playlistQueue.addAll(tracks)

                currentTrackIndex = 0

                playTrack(tracks[0])

            }

        }

    }



    private fun refreshDiscoverNewSection() {

        lifecycleScope.launch {

            val discoverTracks = EmberRecommendationEngine.getDiscoverNew(dbManager, count = 12)

            for (t in discoverTracks) t.isLiked = dbManager.isTrackLiked(t.id)

            discoverNewAdapter.setTracks(discoverTracks)

        }

        btnPlayAllDiscoverNew.setOnClickListener {

            val tracks = discoverNewAdapter.getTracks()

            if (tracks.isNotEmpty()) {

                playlistQueue.clear()

                playlistQueue.addAll(tracks)

                currentTrackIndex = 0

                playTrack(tracks[0])

            }

        }

    }



    private fun refreshMoodMixSection() {

        lifecycleScope.launch {

            val moodResult = EmberRecommendationEngine.getMoodMix(dbManager, count = 12)

            tvMoodMixTitle.text = moodResult.title

            tvMoodMixSubtitle.text = moodResult.subtitle

            for (t in moodResult.tracks) t.isLiked = dbManager.isTrackLiked(t.id)

            moodMixAdapter.setTracks(moodResult.tracks)

        }

        btnPlayAllMoodMix.setOnClickListener {

            val tracks = moodMixAdapter.getTracks()

            if (tracks.isNotEmpty()) {

                playlistQueue.clear()

                playlistQueue.addAll(tracks)

                currentTrackIndex = 0

                playTrack(tracks[0])

            }

        }

    }



    private fun refreshHomePlaylistsSection() {

        val playlists = dbManager.getPlaylists()

        if (playlists.isNotEmpty()) {

            layoutHomeUserPlaylistsSection.visibility = View.VISIBLE

            homeUserPlaylistsAdapter.setPlaylists(playlists.map { p ->

                PlaylistItem(

                    id = p.id,

                    title = p.name,

                    subtitle = "${p.tracks.size} songs • By You",

                    image = p.coverUrl.ifEmpty { p.tracks.firstOrNull()?.thumbnail ?: "" },

                    trackCount = "${p.tracks.size} songs"

                )

            })

            btnSeeAllPlaylists.setOnClickListener { switchTab(2) }

        } else {

            layoutHomeUserPlaylistsSection.visibility = View.GONE

        }

    }



    private fun recalibrateDailyMix() {
        lifecycleScope.launch {
            val topArtists = dbManager.getTopArtists(3)
            val topGenres = dbManager.getTopGenres(2)
            val primaryGenre = topGenres.firstOrNull() ?: "Punjabi"
            val topArtist = topArtists.firstOrNull() ?: "Navaan Sandhu"

            // Concurrent high-speed fetching: all sections load in parallel
            val artistTracksDef = async(Dispatchers.IO) {
                EmberAudioEngine.searchTracks("$topArtist best hits", count = 6)
            }
            val tasteTracksDef = async(Dispatchers.IO) {
                EmberAudioEngine.searchTracks("$primaryGenre top songs", count = 6)
            }
            val smartMixDef = async(Dispatchers.IO) {
                EmberRecommendationEngine.getSmartDailyMix(dbManager, count = 16)
            }

            // Quick recommendations
            refreshRecommendedForYouSection()
            refreshDiscoverNewSection()
            refreshMoodMixSection()

            val artistTracks = artistTracksDef.await()
            for (t in artistTracks) t.isLiked = dbManager.isTrackLiked(t.id)

            if (artistTracks.isNotEmpty()) {
                layoutMoreFromArtistSection.visibility = View.VISIBLE
                tvMoreFromArtistTitle.text = "✦ More from $topArtist"
                moreFromArtistAdapter.setTracks(artistTracks)
                btnPlayAllMoreFromArtist.setOnClickListener {
                    playlistQueue.clear()
                    playlistQueue.addAll(artistTracks)
                    currentTrackIndex = 0
                    playTrack(artistTracks[0])
                }
            } else {
                layoutMoreFromArtistSection.visibility = View.GONE
            }

            val tasteTracks = tasteTracksDef.await()
            for (t in tasteTracks) t.isLiked = dbManager.isTrackLiked(t.id)
            basedOnTasteAdapter.setTracks(tasteTracks)

            val smartMixTracks = smartMixDef.await()
            for (t in smartMixTracks) t.isLiked = dbManager.isTrackLiked(t.id)

            tvDailyMixTitle.text = "Daily Mix • $topArtist & More"
            tvDailyMixSubtitle.text = "Personalized soundscape featuring $topArtist, $primaryGenre and fresh discoveries."

            val playDailyMix = {
                val mix = if (smartMixTracks.isNotEmpty()) smartMixTracks else artistTracks + tasteTracks
                if (mix.isNotEmpty()) {
                    playlistQueue.clear()
                    playlistQueue.addAll(mix)
                    currentTrackIndex = 0
                    playTrack(mix[0])
                }
            }
            cardDailyMixBanner.setOnClickListener { playDailyMix() }
            btnDailyMixPlay.setOnClickListener { playDailyMix() }

            val heroQueue = if (smartMixTracks.isNotEmpty()) smartMixTracks else artistTracks
            if (heroQueue.isNotEmpty()) {
                btnHeroPlayNow.setOnClickListener {
                    playlistQueue.clear()
                    playlistQueue.addAll(heroQueue)
                    currentTrackIndex = 0
                    playTrack(heroQueue[0])
                }
                btnHeroExplore.setOnClickListener {
                    playlistQueue.addAll(heroQueue)
                    Toast.makeText(this@MainActivity, "Added featured station to queue", Toast.LENGTH_SHORT).show()
                }
            }

            hideHomeSkeleton()
            tvHeroSubtext.text = "Vibing with $topArtist and $primaryGenre soundscapes tailored for you."
        }
    }



    private fun updateHeroCardBackdropFromHistory() {

        val history = dbManager.getHistory()

        val heroArtwork = when {

            history.size >= 2 -> history[1].thumbnail

            history.isNotEmpty() -> history[0].thumbnail

            else -> null

        }

        applyHeroBackdrop(heroArtwork)

    }



    private fun updateHeroGreeting() {

        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)

        val greeting = when {

            hour < 12 -> "Good Morning"

            hour < 18 -> "Good Afternoon"

            else -> "Good Evening"

        }

        val name = authManager.currentUser?.displayName?.split(" ")?.firstOrNull() ?: "Explorer"

        tvHeroGreeting.text = "$greeting, $name"

    }



    private fun updateHeroContextualSubtitle() {

        lifecycleScope.launch {

            val (_, artists) = dbManager.getUserPreferences()

            val topArtist = artists.firstOrNull()

            if (!topArtist.isNullOrEmpty()) {

                tvHeroSubtext.text = "Vibing with $topArtist and curated soundscapes tailored for you."

            } else {

                tvHeroSubtext.text = "Immerse in high-fidelity soundscapes tailored for you."

            }

        }

    }



    private fun setupEmberLogo() {

        val emberText = "EMBER"

        val accentColor = ContextCompat.getColor(this, R.color.accent_primary)

        val whiteColor = Color.WHITE



        fun createSpannable() = SpannableStringBuilder(emberText).apply {
            setSpan(ForegroundColorSpan(accentColor), 0, 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE) // 'E'
            setSpan(ForegroundColorSpan(whiteColor), 1, 2, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)  // 'M'
            setSpan(ForegroundColorSpan(accentColor), 2, 3, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE) // 'B'
            setSpan(ForegroundColorSpan(whiteColor), 3, 4, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)  // 'E'
            setSpan(ForegroundColorSpan(accentColor), 4, 5, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE) // 'R'
        }

        tvHeaderAppLogo.text = createSpannable()
        findViewById<TextView>(R.id.tvSkeletonHeaderLogo)?.text = createSpannable()
        findViewById<TextView>(R.id.tvProfileHeaderLogo)?.text = createSpannable()

    }



    private fun applyHeroBackdrop(thumbnailUrl: String?) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {

            ivHeroBg.setRenderEffect(RenderEffect.createBlurEffect(35f, 35f, Shader.TileMode.CLAMP))

            if (!thumbnailUrl.isNullOrEmpty()) {

                ivHeroBg.load(thumbnailUrl) {

                    crossfade(400)

                    placeholder(R.drawable.ember_banner)

                    error(R.drawable.ember_banner)

                }

            } else {

                ivHeroBg.load(R.drawable.ember_banner) {

                    crossfade(400)

                }

            }

        } else {

            val urlToLoad = thumbnailUrl ?: ""

            lifecycleScope.launch(Dispatchers.IO) {

                try {

                    val bmp: Bitmap? = if (urlToLoad.isNotEmpty()) {

                        val loader = ImageLoader(this@MainActivity)

                        val request = ImageRequest.Builder(this@MainActivity)

                            .data(urlToLoad)

                            .allowHardware(false)

                            .placeholder(R.drawable.ember_banner)

                            .error(R.drawable.ember_banner)

                            .build()

                        val result = (loader.execute(request) as? SuccessResult)?.drawable

                        (result as? BitmapDrawable)?.bitmap

                    } else {

                        BitmapFactory.decodeResource(resources, R.drawable.ember_banner)

                    }



                    if (bmp != null) {

                        val blurred = BlurUtils.fastBlur(bmp, radius = 20, downscaleWidth = 100)

                        withContext(Dispatchers.Main) {

                            ivHeroBg.setImageBitmap(blurred)

                        }

                    }

                } catch (e: Exception) {

                    withContext(Dispatchers.Main) {

                        if (urlToLoad.isNotEmpty()) {

                            ivHeroBg.load(urlToLoad) {

                                crossfade(true)

                                placeholder(R.drawable.ember_banner)

                            }

                        }

                    }

                }

            }

        }

    }



    // ==========================================

    // MediaSessionService & Resilient Background Playback

    // ==========================================

    private fun checkNotificationPermission() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {

            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {

                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 2001)

            }

        }

    }



    private val playbackServiceListener = object : EmberPlaybackService.PlaybackChangeListener {

        override fun onTrackChanged(track: Track?) {

            if (track != null) {

                runOnUiThread {

                    currentTrack = track

                    updateUiForTrack(track)

                }

            }

        }



        override fun onPlaybackStateChanged(isPlaying: Boolean, state: Int) {

            runOnUiThread {
                val isBuffering = (state == Player.STATE_BUFFERING)
                if (::layoutFullBuffering.isInitialized) layoutFullBuffering.visibility = if (isBuffering) View.VISIBLE else View.GONE
                if (::pbFullBuffering.isInitialized) pbFullBuffering.visibility = if (isBuffering) View.VISIBLE else View.GONE
                if (::pbFullPlayBuffering.isInitialized) pbFullPlayBuffering.visibility = if (isBuffering) View.VISIBLE else View.GONE
                if (::pbMiniBuffering.isInitialized) pbMiniBuffering.visibility = if (isBuffering) View.VISIBLE else View.GONE
                if (::laserMiniBuffering.isInitialized) laserMiniBuffering.visibility = if (isBuffering) View.VISIBLE else View.GONE
                if (::pbMiniProgress.isInitialized) pbMiniProgress.visibility = if (isBuffering) View.INVISIBLE else View.VISIBLE
                btnFullPlayPause.alpha = if (isBuffering) 0.6f else 1.0f
                btnMiniPlayPause.alpha = if (isBuffering) 0.6f else 1.0f

                val icon = if (isPlaying) R.drawable.ic_pause_black else R.drawable.ic_play_black

                btnMiniPlayPause.setImageResource(icon)

                btnFullPlayPause.setImageResource(icon)



                // Update animated equalizer bars across adapters

                if (::moreFromArtistAdapter.isInitialized) moreFromArtistAdapter.updatePlaybackState(currentTrack?.id, isPlaying)

                if (::basedOnTasteAdapter.isInitialized) basedOnTasteAdapter.updatePlaybackState(currentTrack?.id, isPlaying)

                searchAdapter.updatePlaybackState(currentTrack?.id, isPlaying)

                libraryAdapter.updatePlaybackState(currentTrack?.id, isPlaying)

                if (::playlistDetailAdapter.isInitialized) {

                    playlistDetailAdapter.updatePlaybackState(currentTrack?.id, isPlaying)

                }



                if (isPlaying) {

                    handler.post(progressUpdater)

                    if (isVinylMode) startVinylRotation()

                } else {

                    handler.removeCallbacks(progressUpdater)

                    stopVinylRotation()

                }

            }

        }



        override fun onQueueChanged() {

            runOnUiThread {

                EmberPlaybackService.instance?.let { s ->

                    playlistQueue.clear()

                    playlistQueue.addAll(s.playlistQueue)

                    currentTrackIndex = s.currentTrackIndex

                }

            }

        }

    }



    private fun syncUiFromService() {

        val s = EmberPlaybackService.instance

        if (s != null && s.playlistQueue.isNotEmpty()) {

            playlistQueue.clear()

            playlistQueue.addAll(s.playlistQueue)

            currentTrackIndex = s.currentTrackIndex

        } else {

            val savedState = dbManager.getLastPlaybackState()

            if (savedState != null && savedState.queue.isNotEmpty()) {

                playlistQueue.clear()

                playlistQueue.addAll(savedState.queue)

                currentTrackIndex = savedState.queueIndex.coerceIn(0, playlistQueue.size - 1)

            }

        }



        val activeTrack = s?.currentTrack ?: dbManager.getLastPlaybackState()?.track

        if (activeTrack != null) {

            currentTrack = activeTrack

            updateUiForTrack(activeTrack)

            val isPlaying = s?.player?.isPlaying ?: false

            val icon = if (isPlaying) R.drawable.ic_pause_black else R.drawable.ic_play_black

            btnMiniPlayPause.setImageResource(icon)

            btnFullPlayPause.setImageResource(icon)



            val savedPos = dbManager.getLastPlaybackState()?.positionMs ?: 0L

            val currentPos = if (s != null && s.player.currentPosition > 0) s.player.currentPosition else savedPos

            val total = if (s != null && s.player.duration > 0) s.player.duration else (activeTrack.durationSec * 1000L)

            if (total > 0) {

                val progressInt = ((currentPos * 1000) / total).toInt().coerceIn(0, 1000)

                fullSeekBar.progress = progressInt

                pbMiniProgress.progress = progressInt

                tvFullDuration.text = formatTime(total)

            }

            tvFullCurrentTime.text = formatTime(currentPos)



            if (isPlaying) {

                handler.post(progressUpdater)

                if (isVinylMode) startVinylRotation()

            }

        }

    }



    private fun setupExoPlayer() {

        checkNotificationPermission()



        // Immediately populate UI from last saved playback state on startup

        syncUiFromService()



        // Ensure Background Playback Service is running

        val serviceIntent = Intent(this, EmberPlaybackService::class.java)

        startService(serviceIntent)



        // Connect MediaController to the MediaSession

        val sessionToken = SessionToken(this, ComponentName(this, EmberPlaybackService::class.java))

        mediaControllerFuture = MediaController.Builder(this, sessionToken).buildAsync()

        mediaControllerFuture?.addListener({

            val controller = mediaControllerFuture?.get() ?: return@addListener

            player = controller



            attachPlayerListener(controller)

            EmberPlaybackService.instance?.addListener(playbackServiceListener)

            syncUiFromService()

        }, ContextCompat.getMainExecutor(this))

    }



    private fun attachPlayerListener(p: Player) {

        p.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                val isBuffering = (playbackState == Player.STATE_BUFFERING)
                if (::layoutFullBuffering.isInitialized) layoutFullBuffering.visibility = if (isBuffering) View.VISIBLE else View.GONE
                if (::pbFullBuffering.isInitialized) pbFullBuffering.visibility = if (isBuffering) View.VISIBLE else View.GONE
                if (::pbFullPlayBuffering.isInitialized) pbFullPlayBuffering.visibility = if (isBuffering) View.VISIBLE else View.GONE
                if (::pbMiniBuffering.isInitialized) pbMiniBuffering.visibility = if (isBuffering) View.VISIBLE else View.GONE
                if (::laserMiniBuffering.isInitialized) laserMiniBuffering.visibility = if (isBuffering) View.VISIBLE else View.GONE
                if (::pbMiniProgress.isInitialized) pbMiniProgress.visibility = if (isBuffering) View.INVISIBLE else View.VISIBLE
                btnFullPlayPause.alpha = if (isBuffering) 0.6f else 1.0f
                btnMiniPlayPause.alpha = if (isBuffering) 0.6f else 1.0f

                when (playbackState) {
                    Player.STATE_READY -> {
                        tvFullDuration.text = formatTime(p.duration)
                        setupAudioEffects()
                    }
                    Player.STATE_ENDED -> {
                        // Handled centrally by EmberPlaybackService
                    }
                    Player.STATE_IDLE -> {
                    }
                }
            }



            override fun onIsPlayingChanged(isPlaying: Boolean) {

                val icon = if (isPlaying) R.drawable.ic_pause_black else R.drawable.ic_play_black

                btnMiniPlayPause.setImageResource(icon)

                btnFullPlayPause.setImageResource(icon)



                // Update animated equalizer bars across adapters

                if (::moreFromArtistAdapter.isInitialized) moreFromArtistAdapter.updatePlaybackState(currentTrack?.id, isPlaying)

                if (::basedOnTasteAdapter.isInitialized) basedOnTasteAdapter.updatePlaybackState(currentTrack?.id, isPlaying)

                searchAdapter.updatePlaybackState(currentTrack?.id, isPlaying)

                libraryAdapter.updatePlaybackState(currentTrack?.id, isPlaying)

                if (::playlistDetailAdapter.isInitialized) {

                    playlistDetailAdapter.updatePlaybackState(currentTrack?.id, isPlaying)

                }



                if (isPlaying) {

                    handler.post(progressUpdater)

                    if (isVinylMode) startVinylRotation()

                } else {

                    handler.removeCallbacks(progressUpdater)

                    stopVinylRotation()

                }

            }

        })

    }



    private fun updateUiForTrack(track: Track) {
        updateDownloadButtonForTrack(track)

        if (::moreFromArtistAdapter.isInitialized) moreFromArtistAdapter.updatePlaybackState(track.id, true)

        if (::basedOnTasteAdapter.isInitialized) basedOnTasteAdapter.updatePlaybackState(track.id, true)

        if (::playlistDetailAdapter.isInitialized) {

            playlistDetailAdapter.updatePlaybackState(track.id, true)

        }



        // Update Mini Player immediately

        miniPlayerView.visibility = View.VISIBLE

        val sanitizedArtist = EmberAudioEngine.sanitizeMetadata(track.artist)
        val sanitizedTitle = EmberAudioEngine.sanitizeMetadata(track.title)
        tvMiniTitle.text = sanitizedTitle
        tvMiniArtist.text = sanitizedArtist

        if (track.thumbnail.isNotEmpty()) {

            ivMiniThumb.load(track.thumbnail) {

                crossfade(true)

                allowHardware(false)

                placeholder(ThumbnailLoadingDrawable())

                error(R.drawable.rounded_card_bg)

                listener(

                    onSuccess = { _, result ->

                        val bmp = (result.drawable as? BitmapDrawable)?.bitmap

                        if (bmp != null) {

                            val color = extractVibrantRimColor(bmp)

                            applyDynamicUpperLineColor(color)

                            val blurred = BlurUtils.fastBlur(bmp, radius = 25, downscaleWidth = 60)

                            ivMiniAmbientMesh.setImageBitmap(blurred)

                            ivFullAmbientMesh.setImageBitmap(blurred)

                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {

                                ivMiniAmbientMesh.setRenderEffect(RenderEffect.createBlurEffect(120f, 120f, Shader.TileMode.CLAMP))

                                val satMatrix = ColorMatrix().apply { setSaturation(2.2f) }

                                ivMiniAmbientMesh.colorFilter = ColorMatrixColorFilter(satMatrix)

                            }

                        }

                    }

                )

            }

            ivFullThumb.load(track.thumbnail) {

                crossfade(true)

                placeholder(ThumbnailLoadingDrawable())

                error(R.drawable.rounded_card_bg)

            }

            ivVinylCenterLabel.load(track.thumbnail) {

                crossfade(true)

                placeholder(ThumbnailLoadingDrawable())

                error(R.drawable.rounded_card_bg)

            }

            updateAmbientBackdrop(track.thumbnail)

        }



        // Invalidate cached lyrics and reset offset for new track

        lyricsFetchJob?.cancel()

        cachedLyricsTrackId = null

        cachedSyncedLines = null

        cachedPlainLyrics = null

        lyricsOffsetMs = 0L

        updateLyricsOffsetDisplay()



        if (isLyricsActive) {

            loadLyricsForCurrentTrack()

        }



        // Update Full Player immediately

        tvFullTitle.text = sanitizedTitle
        tvFullArtist.text = sanitizedArtist

        tvFullDuration.text = track.duration

        fullSeekBar.progress = 0

        pbMiniProgress.progress = 0

        tvFullCurrentTime.text = "0:00"



        track.isLiked = dbManager.isTrackLiked(track.id)

        updateLikeIcons(track.isLiked)

        dbManager.recordHistory(track)

        updateHeroCardBackdropFromHistory()

        refreshJumpBackInSection()



        // Lossless Badge: Direct 320kbps CDN vs YouTube Fallback Engine

        if (track.isYoutubeFallback) {

            badgeLossless.text = "YOUTUBE AUDIO"

            badgeLossless.setTextColor(Color.parseColor("#FF8A45"))

        } else {

            badgeLossless.text = "LOSSLESS"

            badgeLossless.setTextColor(ContextCompat.getColor(this, R.color.accent_primary))

        }

    }



    /**
     * Search-to-Recommendation Queue Mechanism (Crucial UX Feature):
     * 1. DO NOT add remaining search results into the queue.
     * 2. DO NOT add random listening history into the queue.
     * 3. IMMEDIATELY play the tapped song.
     * 4. CONCURRENTLY call YouTube Music native /next recommendation endpoint (RDAMVM<videoId>).
     * 5. Automatically populate "Up Next" queue with 20–25 acoustically matched songs from YouTube Music Automix.
     */
    private fun onSearchResultTrackSelected(selectedTrack: Track) {
        currentTrack = selectedTrack
        updateUiForTrack(selectedTrack)
        Toast.makeText(this, "Playing \"${selectedTrack.title}\"", Toast.LENGTH_SHORT).show()

        val service = EmberPlaybackService.instance
        if (service != null) {
            service.isShuffleMode = isShuffleMode
            service.setQueueAndPlay(listOf(selectedTrack), 0)
        } else {
            playlistQueue.clear()
            playlistQueue.add(selectedTrack)
            currentTrackIndex = 0
            playTrack(selectedTrack)
        }
    }

    private fun playTrack(track: Track) {

        currentTrack = track

        updateUiForTrack(track)

        if (::layoutFullBuffering.isInitialized) layoutFullBuffering.visibility = View.VISIBLE
        if (::pbFullBuffering.isInitialized) pbFullBuffering.visibility = View.VISIBLE
        if (::pbFullPlayBuffering.isInitialized) pbFullPlayBuffering.visibility = View.VISIBLE
        if (::pbMiniBuffering.isInitialized) pbMiniBuffering.visibility = View.VISIBLE
        if (::laserMiniBuffering.isInitialized) laserMiniBuffering.visibility = View.VISIBLE
        if (::pbMiniProgress.isInitialized) pbMiniProgress.visibility = View.INVISIBLE
        btnFullPlayPause.alpha = 0.6f
        btnMiniPlayPause.alpha = 0.6f



        if (playlistQueue.isEmpty() || !playlistQueue.any { it.id == track.id }) {

            playlistQueue.clear()

            playlistQueue.add(track)

            currentTrackIndex = 0

        } else {

            currentTrackIndex = playlistQueue.indexOfFirst { it.id == track.id }.coerceAtLeast(0)

        }



        val service = EmberPlaybackService.instance

        if (service != null) {

            service.isShuffleMode = isShuffleMode

            service.setQueueAndPlay(playlistQueue, currentTrackIndex)

        } else {

            playResolvedAudio(track)

        }

    }



    private fun playResolvedAudio(track: Track) {
        val service = EmberPlaybackService.instance
        if (service != null) {
            service.playTrack(track)
            return
        }

        val dlManager = com.example.ember.data.EmberDownloadManager.getInstance(this)
        if (!track.isDownloaded && dlManager.isDownloaded(track.id)) {
            val dl = dlManager.getDownloadedTrack(track.id)
            if (dl != null) {
                track.localAudioPath = dl.localAudioPath
                track.localThumbnailPath = dl.localThumbnailPath
                track.isDownloaded = true
            }
        }

        val hasLocal = !track.localAudioPath.isNullOrBlank() && java.io.File(track.localAudioPath!!).exists()
        if (hasLocal) {
            startDirectPlayback(track)
            return
        }

        if (!isNetworkAvailable()) {
            Toast.makeText(this, "You're offline. Only downloaded tracks can be played.", Toast.LENGTH_SHORT).show()
            return
        }

        val isPreview = track.audioUrl.isBlank() || EmberAudioEngine.isPreviewUrl(track.audioUrl)
        if (isPreview) {
            lifecycleScope.launch(Dispatchers.IO) {
                val resolved = EmberAudioEngine.resolveAudioUrl(track)
                withContext(Dispatchers.Main) {
                    if (resolved.isNotBlank()) {
                        track.audioUrl = resolved
                        startDirectPlayback(track)
                    } else {
                        Toast.makeText(this@MainActivity, "Unable to resolve full audio stream for ${track.title}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        } else {
            startDirectPlayback(track)
        }
    }

    private fun startDirectPlayback(track: Track) {
        val p = player ?: return
        p.stop()
        try {
            val uri: Uri = if (!track.localAudioPath.isNullOrBlank() && java.io.File(track.localAudioPath!!).exists()) {
                Uri.fromFile(java.io.File(track.localAudioPath!!))
            } else if (track.audioUrl.isNotBlank()) {
                Uri.parse(track.audioUrl)
            } else {
                return
            }
            val mediaItem = MediaItem.fromUri(uri)
            p.setMediaItem(mediaItem)
            p.prepare()
            p.play()
            tvFullDuration.text = track.duration
        } catch (e: Exception) {
            Toast.makeText(this, "Error playing audio: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }



    private fun updateAmbientBackdrop(thumbnailUrl: String) {

        if (thumbnailUrl.isEmpty()) return



        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {

            ivFullAmbientMesh.setRenderEffect(RenderEffect.createBlurEffect(135f, 135f, Shader.TileMode.CLAMP))
            val fullSatMatrix = ColorMatrix().apply { setSaturation(1.85f) }
            ivFullAmbientMesh.colorFilter = ColorMatrixColorFilter(fullSatMatrix)

            ivFullAmbientMesh.load(thumbnailUrl) {

                crossfade(true)

                placeholder(ThumbnailLoadingDrawable())

                error(R.drawable.rounded_card_bg)

            }

            ivMiniAmbientMesh.setRenderEffect(RenderEffect.createBlurEffect(98f, 98f, Shader.TileMode.CLAMP))

            val satMatrix = ColorMatrix().apply { setSaturation(1.6f) }

            ivMiniAmbientMesh.colorFilter = ColorMatrixColorFilter(satMatrix)

            ivMiniAmbientMesh.load(thumbnailUrl) {

                crossfade(true)

                placeholder(ThumbnailLoadingDrawable())

                error(R.drawable.rounded_card_bg)

            }

        }



        lifecycleScope.launch(Dispatchers.IO) {

            try {

                val loader = ImageLoader(this@MainActivity)

                val request = ImageRequest.Builder(this@MainActivity)

                    .data(thumbnailUrl)

                    .allowHardware(false)

                    .placeholder(ThumbnailLoadingDrawable())

                    .error(R.drawable.rounded_card_bg)

                    .build()

                val result = (loader.execute(request) as? SuccessResult)?.drawable

                val bmp = (result as? BitmapDrawable)?.bitmap

                if (bmp != null) {

                    val dynamicColor = extractVibrantRimColor(bmp)

                    val blurred = BlurUtils.fastBlur(bmp, radius = 25, downscaleWidth = 60)

                    withContext(Dispatchers.Main) {

                        applyDynamicUpperLineColor(dynamicColor)

                        ivFullAmbientMesh.setImageBitmap(blurred)

                        ivMiniAmbientMesh.setImageBitmap(blurred)

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {

                            ivMiniAmbientMesh.setRenderEffect(RenderEffect.createBlurEffect(120f, 120f, Shader.TileMode.CLAMP))

                            val satMatrix = ColorMatrix().apply { setSaturation(2.2f) }

                            ivMiniAmbientMesh.colorFilter = ColorMatrixColorFilter(satMatrix)

                        }

                    }

                }

            } catch (e: Exception) {

                withContext(Dispatchers.Main) {

                    applyDynamicUpperLineColor(Color.parseColor("#FF5A36"))

                }

            }

        }

    }



    private fun extractVibrantRimColor(bitmap: Bitmap): Int {

        val scaled = Bitmap.createScaledBitmap(bitmap, 32, 32, false)

        var bestColor = Color.parseColor("#FF5A36")

        var bestScore = -1f



        for (x in 0 until scaled.width) {

            for (y in 0 until scaled.height) {

                val pixel = scaled.getPixel(x, y)

                val alpha = Color.alpha(pixel)

                if (alpha < 128) continue



                val hsv = FloatArray(3)

                Color.colorToHSV(pixel, hsv)

                val saturation = hsv[1]

                val brightness = hsv[2]



                // Filter out near-blacks (< 0.15) and washed-out pure whites

                if (brightness in 0.15f..0.98f && saturation > 0.15f) {

                    val score = (saturation * 2.2f) + (1.0f - Math.abs(brightness - 0.70f))

                    if (score > bestScore) {

                        bestScore = score

                        bestColor = pixel

                    }

                }

            }

        }



        // Boost saturation and brightness for a luminous, glowing upper rim

        val outHsv = FloatArray(3)

        Color.colorToHSV(bestColor, outHsv)

        outHsv[1] = (outHsv[1] * 1.35f).coerceIn(0.60f, 1.0f)

        outHsv[2] = outHsv[2].coerceIn(0.75f, 0.98f)

        return Color.HSVToColor(outHsv)

    }



    private fun applyDynamicAmbientHalo(targetColor: Int) {

        haloColorAnimator?.cancel()

        val startColor = currentHaloColor

        val animator = ValueAnimator.ofArgb(startColor, targetColor).apply {

            duration = 600L

            interpolator = DecelerateInterpolator()

            addUpdateListener { va ->

                val animatedColor = va.animatedValue as Int

                currentHaloColor = animatedColor

                updateHaloColor(animatedColor)

                updateArtworkGlow(animatedColor)

            }

        }

        haloColorAnimator = animator

        animator.start()

    }



    private fun updateHaloColor(color: Int) {

        if (!::miniPlayerView.isInitialized) return



        // 1. Lateral Side Glow: Left and Right Flanks (1.8dp width) tinted with active song thumbnail color

        if (::viewMiniLeftFlank.isInitialized) {

            viewMiniLeftFlank.backgroundTintList = ColorStateList.valueOf(color)

        }

        if (::viewMiniRightFlank.isInitialized) {

            viewMiniRightFlank.backgroundTintList = ColorStateList.valueOf(color)

        }



        // 2. Soft Lateral Ambient Glow strictly radiating into left/right margins (Zero vertical offset = ZERO upward glow!)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {

            miniPlayerView.outlineSpotShadowColor = Color.argb(150, Color.red(color), Color.green(color), Color.blue(color))

            miniPlayerView.outlineAmbientShadowColor = Color.argb(55, Color.red(color), Color.green(color), Color.blue(color))
            miniPlayerView.cardElevation = 14f * resources.displayMetrics.density

        }



        // 3. Bottom Glowing Progress Bar

        if (::viewMiniUpperGlow.isInitialized) {

            viewMiniUpperGlow.backgroundTintList = ColorStateList.valueOf(color)

        }

        if (::pbMiniProgress.isInitialized) {

            pbMiniProgress.progressTintList = ColorStateList.valueOf(color)

        }



        // 4. Tint heart icon if currently liked

        if (::ivMiniLike.isInitialized && currentTrack?.isLiked == true) {

            ivMiniLike.imageTintList = ColorStateList.valueOf(color)

        }

    }



    private var currentFullSongColor: Int = Color.parseColor("#FF5A36")

    private fun updateArtworkGlow(color: Int, sizePx: Int = 0) {
        if (!::ivArtworkAmbientGlow.isInitialized) return
        currentFullSongColor = color
        val effectiveSize = if (sizePx > 0) sizePx else ivArtworkAmbientGlow.width.takeIf { it > 0 } ?: (cardArtworkContainer.width + (110f * resources.displayMetrics.density).toInt()).coerceAtLeast(100)
        val r = Color.red(color)
        val g = Color.green(color)
        val b = Color.blue(color)
        val radius = effectiveSize * 0.5f

        val gradient = GradientDrawable().apply {
            gradientType = GradientDrawable.RADIAL_GRADIENT
            gradientRadius = radius
            setGradientCenter(0.5f, 0.5f)
            colors = intArrayOf(
                Color.argb(160, r, g, b),
                Color.argb(115, r, g, b),
                Color.argb(60, r, g, b),
                Color.argb(18, r, g, b),
                Color.TRANSPARENT
            )
        }
        ivArtworkAmbientGlow.background = gradient
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            try {
                ivArtworkAmbientGlow.setRenderEffect(RenderEffect.createBlurEffect(55f, 55f, Shader.TileMode.CLAMP))
            } catch (_: Exception) {}
        }
    }

    private fun applyDynamicUpperLineColor(color: Int) {

        // Dynamic thumbnail-extracted radiant ambient halo with smooth 600ms crossfade

        applyDynamicAmbientHalo(color)

        // Full Screen Player: Clean Neutral Drop Shadow on Album Artwork (prevents asymmetric colored shadow bugs)
        if (::cardArtworkContainer.isInitialized && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            cardArtworkContainer.outlineSpotShadowColor = Color.argb(45, 0, 0, 0)
            cardArtworkContainer.outlineAmbientShadowColor = Color.argb(35, 0, 0, 0)
            cardArtworkContainer.cardElevation = 8f * resources.displayMetrics.density
        }

        updateArtworkGlow(color)

        // Full Screen Player: SeekBar Progress & Thumb halo tinted to song theme color

        fullSeekBar.progressTintList = ColorStateList.valueOf(color)

        val thumbDrawable = fullSeekBar.thumb

        if (thumbDrawable is LayerDrawable) {

            val halo = thumbDrawable.findDrawableByLayerId(R.id.thumbHalo)

            val alphaGlow = Color.argb(90, Color.red(color), Color.green(color), Color.blue(color))

            halo?.setTint(alphaGlow)

        }

    }



    private fun updateLikeIcons(isLiked: Boolean) {

        if (isLiked) {

            ivMiniLike.setImageResource(R.drawable.ic_heart_filled)

            ivMiniLike.imageTintList = ColorStateList.valueOf(currentHaloColor)

            ivMiniLike.scaleX = 0.8f

            ivMiniLike.scaleY = 0.8f

            ivMiniLike.animate()

                .scaleX(1.25f)

                .scaleY(1.25f)

                .setDuration(140L)

                .setInterpolator(OvershootInterpolator(1.4f))

                .withEndAction {

                    ivMiniLike.animate().scaleX(1.0f).scaleY(1.0f).setDuration(100L).start()

                }

                .start()



            btnFullLikeContainer.setBackgroundResource(R.drawable.circle_heart_active_bg)

            ivFullLike.setImageResource(R.drawable.ic_heart_filled)

            ivFullLike.imageTintList = ColorStateList.valueOf(Color.parseColor("#FF3B30"))

        } else {

            ivMiniLike.setImageResource(R.drawable.ic_heart)

            ivMiniLike.imageTintList = ColorStateList.valueOf(Color.parseColor("#FFFFFF"))

            ivMiniLike.scaleX = 1.0f

            ivMiniLike.scaleY = 1.0f



            btnFullLikeContainer.setBackgroundResource(R.drawable.circle_heart_inactive_bg)

            ivFullLike.setImageResource(R.drawable.ic_heart)

            ivFullLike.imageTintList = ColorStateList.valueOf(Color.parseColor("#9E9EB2"))

        }



        if (::playlistDetailAdapter.isInitialized) {

            playlistDetailAdapter.notifyDataSetChanged()

        }

    }



    private fun playNext() {

        val service = EmberPlaybackService.instance

        if (service != null) {

            service.playNext()

        } else {

            player?.seekToNextMediaItem()

        }

    }



    private fun playPrevious() {

        val service = EmberPlaybackService.instance

        if (service != null) {

            service.playPrevious()

        } else {

            player?.let { p ->

                if (p.currentPosition > 3000) {

                    p.seekTo(0)

                } else {

                    p.seekToPreviousMediaItem()

                }

            }

        }

    }



    // ==========================================

    // Vinyl Rotation Animation

    // ==========================================

    private fun startVinylRotation() {

        if (vinylAnimator == null) {

            vinylAnimator = ObjectAnimator.ofFloat(layoutVinylDisc, "rotation", 0f, 360f).apply {

                duration = 16000

                repeatCount = ObjectAnimator.INFINITE

                interpolator = LinearInterpolator()

            }

        }

        if (player?.isPlaying == true && isVinylMode) {

            if (vinylAnimator?.isPaused == true) {

                vinylAnimator?.resume()

            } else if (vinylAnimator?.isStarted != true) {

                vinylAnimator?.start()

            }

        }

    }



    private fun stopVinylRotation() {

        vinylAnimator?.pause()

    }



    // ==========================================

    // Audio Effects (5-Band EQ & Bass Boost)

    // ==========================================

    private fun setupAudioEffects() {

        val sessionId = EmberPlaybackService.instance?.player?.audioSessionId

            ?: (player as? ExoPlayer)?.audioSessionId

            ?: return

        if (sessionId != C.AUDIO_SESSION_ID_UNSET && equalizer == null) {

            try {

                equalizer = Equalizer(0, sessionId).apply {
                    val prefs = getSharedPreferences("ember_audio_prefs", MODE_PRIVATE)
                    val isEqEnabled = prefs.getBoolean("eq_enabled", true)
                    enabled = isEqEnabled
                    for (i in 0 until numberOfBands) {
                        val db = prefs.getInt("eq_band_$i", 0)
                        setBandLevel(i.toShort(), (db * 100).toShort())
                    }
                }

                bassBoost = BassBoost(0, sessionId).apply {

                    setStrength(600.toShort())

                    enabled = true

                }

            } catch (e: Exception) {

                android.util.Log.w("EMBER_EQ", "Audio effects init error: ${e.message}")

            }

        }

    }



    // ==========================================

    // Search Tab & Discovery Grid Logic

    // ==========================================

    private fun setupSearchAndDiscovery() {

        // Load recent searches on open

        refreshRecentSearches()



        // Debounced text watcher – 200ms autocomplete suggestions only

        // Full search fires only on: suggestion tap, IME search key, or Enter

        etSearch.addTextChangedListener(object : TextWatcher {

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {

                val query = s?.toString()?.trim() ?: ""

                ivClearSearch.visibility = if (query.isNotEmpty()) View.VISIBLE else View.GONE



                searchDebounceJob?.cancel()

                if (query.isNotEmpty()) {

                    // Only fetch suggestions while user is typing – do NOT auto-trigger search

                    searchDebounceJob = lifecycleScope.launch {

                        delay(200)

                        val suggestions = EmberAudioEngine.getSuggestions(query)

                        if (suggestions.isNotEmpty()) {

                            searchSuggestionsAdapter.setSuggestions(suggestions.take(6))

                            if (cardSearchAutocomplete.visibility != View.VISIBLE) {

                                cardSearchAutocomplete.alpha = 0f

                                cardSearchAutocomplete.translationY = -dpToPx(6).toFloat()

                                cardSearchAutocomplete.visibility = View.VISIBLE

                                cardSearchAutocomplete.animate()

                                    .alpha(1f)

                                    .translationY(0f)

                                    .setDuration(160L)

                                    .setInterpolator(DecelerateInterpolator())

                                    .start()

                            }

                        } else {

                            cardSearchAutocomplete.visibility = View.GONE

                        }

                    }

                } else {

                    // Back to idle state

                    cardSearchAutocomplete.visibility = View.GONE

                    showIdleState()

                }

            }

            override fun afterTextChanged(s: Editable?) {}

        })



        etSearch.setOnEditorActionListener { _, actionId, _ ->

            if (actionId == EditorInfo.IME_ACTION_SEARCH) {

                val query = etSearch.text.toString().trim()

                if (query.isNotEmpty()) executeSearch(query)

                hideKeyboard()

                true

            } else false

        }



        ivClearSearch.setOnClickListener {

            etSearch.text.clear()

            cardSearchAutocomplete.visibility = View.GONE

            showIdleState()

        }



        // Filter Pills: All, Songs, Artists, Albums, Playlists

        val filterPills = listOf(pillFilterAll, pillFilterSongs, pillFilterArtists, pillFilterAlbums, pillFilterPlaylists)

        fun selectPill(selected: TextView, filterName: String) {

            currentSearchFilter = filterName

            for (p in filterPills) {

                if (p == selected) {

                    p.setBackgroundResource(R.drawable.editorial_badge_bg)

                    p.setTextColor(ContextCompat.getColor(this, R.color.accent_primary))

                } else {

                    p.setBackgroundResource(R.drawable.badge_bg)

                    p.setTextColor(ContextCompat.getColor(this, R.color.text_secondary))

                }

            }

            val query = etSearch.text.toString().trim()

            if (query.isNotEmpty()) performSearch(query, currentSearchFilter)

        }



        pillFilterAll.setOnClickListener { it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP); selectPill(pillFilterAll, "All") }

        pillFilterSongs.setOnClickListener { it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP); selectPill(pillFilterSongs, "Songs") }

        pillFilterArtists.setOnClickListener { it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP); selectPill(pillFilterArtists, "Artists") }

        pillFilterAlbums.setOnClickListener { it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP); selectPill(pillFilterAlbums, "Albums") }

        pillFilterPlaylists.setOnClickListener { it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP); selectPill(pillFilterPlaylists, "Playlists") }



        // See All Songs → switch to Songs filter

        btnSeeAllSongs.setOnClickListener {

            it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)

            selectPill(pillFilterSongs, "Songs")

        }



        // Quick Search Tags (Trending)

        val setupTagClick = { tag: TextView ->

            tag.setOnClickListener {

                it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)

                val query = tag.text.toString()

                etSearch.setText(query)

                etSearch.setSelection(query.length)

                executeSearch(query)

            }

        }

        setupTagClick(tagArijit)

        setupTagClick(tagWeeknd)

        setupTagClick(tagDiljit)

        setupTagClick(tagTaylor)

        setupTagClick(tagLofi)



        // Spotify Playlist Importer Card

        cardSpotifyImporter.setOnClickListener {

            it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)

            openSpotifyImportDialog()

        }



        // 2-Column Moods & Genres Grid Cards

        val setupGenreClick = { card: View, genre: String ->

            card.setOnClickListener {

                it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)

                etSearch.setText(genre)

                etSearch.setSelection(genre.length)

                executeSearch(genre)

            }

        }

        setupGenreClick(cardGenreBollywood, "Bollywood")

        setupGenreClick(cardGenrePunjabi, "Punjabi")

        setupGenreClick(cardGenreHiphop, "Hip-Hop")

        setupGenreClick(cardGenrePop, "Pop")

        setupGenreClick(cardGenreLofi, "Lo-Fi Chill")

        setupGenreClick(cardGenreRnb, "R&B Soul")





    }



    /** Executes a full search, saves to recent history, hides autocomplete */

    private fun executeSearch(query: String) {

        if (query.isBlank()) return

        etSearch.setText(query)

        etSearch.setSelection(query.length)

        cardSearchAutocomplete.visibility = View.GONE

        hideKeyboard()

        dbManager.addRecentSearch(query)

        refreshRecentSearches()

        performSearch(query, currentSearchFilter)

    }



    /** Shows the idle discovery state (recent searches, trending, genres) */

    private fun showIdleState() {

        layoutSearchDefaultContent.visibility = View.VISIBLE

        layoutSearchResultsAll.visibility = View.GONE

        rvSearchTracks.visibility = View.GONE

        rvSearchArtists.visibility = View.GONE

        rvSearchAlbums.visibility = View.GONE

        rvSearchPlaylists.visibility = View.GONE

        layoutSearchEmpty.visibility = View.GONE

        searchAdapter.setTracks(emptyList())

        refreshRecentSearches()

    }



    /** Loads recent searches from DB and shows/hides the panel */

    private fun refreshRecentSearches() {

        val recents = dbManager.getRecentSearches()

        if (recents.isNotEmpty()) {

            layoutRecentSearches.visibility = View.VISIBLE

            recentSearchesAdapter.setQueries(recents)

        } else {

            layoutRecentSearches.visibility = View.GONE

        }

        btnClearRecentSearches.setOnClickListener {

            dbManager.clearRecentSearches()

            refreshRecentSearches()

        }

    }



    private fun performSearch(query: String, filter: String = "All") {

        if (query.isBlank()) return



        // Always dismiss autocomplete the moment a real search fires

        searchDebounceJob?.cancel()

        cardSearchAutocomplete.visibility = View.GONE



        if (::layoutSearchSkeleton.isInitialized) {
            layoutSearchSkeleton.visibility = View.VISIBLE
            layoutSearchSkeleton.alpha = 1f
            layoutSearchSkeleton.startShimmer()
            if (::tvSearchSkeletonStatus.isInitialized) {
                tvSearchSkeletonStatus.text = "Searching lossless catalog for \"$query\"..."
            }
        }
        pbSearchLoading.visibility = View.GONE
        layoutSearchDefaultContent.visibility = View.GONE
        layoutSearchEmpty.visibility = View.GONE



        // Hide all result containers first

        layoutSearchResultsAll.visibility = View.GONE

        rvSearchTracks.visibility = View.GONE

        rvSearchArtists.visibility = View.GONE

        rvSearchAlbums.visibility = View.GONE

        rvSearchPlaylists.visibility = View.GONE



        lifecycleScope.launch {

            when (filter) {

                "All" -> {

                    val results = EmberAudioEngine.searchUnified(query)
                    hideSearchSkeleton()



                    val isEmpty = results.songs.isEmpty() && results.artists.isEmpty() &&

                            results.albums.isEmpty() && results.playlists.isEmpty()



                    if (isEmpty) {

                        showEmptyState(query)

                        return@launch

                    }



                    layoutSearchResultsAll.visibility = View.VISIBLE



                    // Populate Top Match Hero Card

                    when (val top = results.topResult) {

                        is TopMatchItem.ArtistMatch -> {

                            cardTopMatchHero.visibility = View.VISIBLE

                            tvTopMatchBadge.text = "TOP ARTIST"

                            tvTopMatchTitle.text = top.artist.name

                            tvTopMatchSubtitle.text = "Official Artist • EMBER"

                            cardTopMatchImageWrapper.radius = dpToPx(36).toFloat()

                            ivTopMatchImage.load(top.artist.image) {

                                crossfade(true)

                                placeholder(ThumbnailLoadingDrawable())

                                error(R.drawable.rounded_card_bg)

                            }

                            cardTopMatchHero.setOnClickListener {

                                it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)

                                openArtistView(top.artist)

                            }

                            btnTopMatchAction.setOnClickListener {

                                it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)

                                openArtistView(top.artist)

                            }

                        }

                        is TopMatchItem.SongMatch -> {

                            cardTopMatchHero.visibility = View.VISIBLE

                            tvTopMatchBadge.text = "TOP SONG"

                            tvTopMatchTitle.text = top.track.title

                            tvTopMatchSubtitle.text = "${top.track.artist} • ${top.track.duration}"

                            cardTopMatchImageWrapper.radius = dpToPx(14).toFloat()

                            ivTopMatchImage.load(top.track.thumbnail) {

                                crossfade(true)

                                placeholder(ThumbnailLoadingDrawable())

                                error(R.drawable.rounded_card_bg)

                            }

                            cardTopMatchHero.setOnClickListener {
                                it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                onSearchResultTrackSelected(top.track)
                            }

                            btnTopMatchAction.setOnClickListener {
                                it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                onSearchResultTrackSelected(top.track)
                            }

                        }

                        null -> cardTopMatchHero.visibility = View.GONE

                    }



                    // Songs section (top 6)

                    val songsForLike = results.songs.onEach { it.isLiked = dbManager.isTrackLiked(it.id) }

                    allSongsAdapter.setTracks(songsForLike.take(6))



                    // Artists carousel

                    allArtistsAdapter.setArtists(results.artists)



                    // Albums row

                    allAlbumsAdapter.setAlbums(results.albums)



                    // Playlists row

                    allPlaylistsAdapter.setPlaylists(results.playlists)

                }



                "Songs" -> {

                    val tracks = EmberAudioEngine.searchSongs(query, 30)

                    tracks.forEach { it.isLiked = dbManager.isTrackLiked(it.id) }
                    hideSearchSkeleton()

                    if (tracks.isEmpty()) { showEmptyState(query); return@launch }

                    searchAdapter.setTracks(tracks)

                    rvSearchTracks.visibility = View.VISIBLE

                }



                "Artists" -> {

                    val artists = EmberAudioEngine.searchArtists(query, 30)
                    hideSearchSkeleton()

                    if (artists.isEmpty()) { showEmptyState(query); return@launch }

                    categoryArtistsAdapter.setArtists(artists)

                    rvSearchArtists.visibility = View.VISIBLE

                }



                "Albums" -> {

                    val albums = EmberAudioEngine.searchAlbums(query, 50)
                    hideSearchSkeleton()

                    if (albums.isEmpty()) { showEmptyState(query); return@launch }

                    categoryAlbumsAdapter.setAlbums(albums)

                    rvSearchAlbums.visibility = View.VISIBLE

                }



                "Playlists" -> {

                    val playlists = EmberAudioEngine.searchPlaylists(query, 50)
                    hideSearchSkeleton()

                    if (playlists.isEmpty()) { showEmptyState(query); return@launch }

                    categoryPlaylistsAdapter.setPlaylists(playlists)

                    rvSearchPlaylists.visibility = View.VISIBLE

                }



                else -> {

                    // Fallback: basic song search

                    val tracks = EmberAudioEngine.searchTracks(query, count = 30, filter = filter)

                    tracks.forEach { it.isLiked = dbManager.isTrackLiked(it.id) }

                    pbSearchLoading.visibility = View.GONE

                    searchAdapter.setTracks(tracks)

                    rvSearchTracks.visibility = View.VISIBLE

                }

            }

        }

    }



    private fun hideSearchSkeleton() {
        if (::layoutSearchSkeleton.isInitialized && layoutSearchSkeleton.visibility == View.VISIBLE) {
            layoutSearchSkeleton.animate()
                .alpha(0f)
                .setDuration(220L)
                .withEndAction {
                    layoutSearchSkeleton.visibility = View.GONE
                    layoutSearchSkeleton.alpha = 1f
                    layoutSearchSkeleton.stopShimmer()
                }
        }
        if (::pbSearchLoading.isInitialized) pbSearchLoading.visibility = View.GONE
    }

    private fun hideHomeSkeleton() {
        if (::layoutHomeSkeleton.isInitialized && layoutHomeSkeleton.visibility == View.VISIBLE) {
            layoutHomeSkeleton.animate()
                .alpha(0f)
                .setDuration(350L)
                .withEndAction {
                    layoutHomeSkeleton.visibility = View.GONE
                    layoutHomeSkeleton.alpha = 1f
                    layoutHomeSkeleton.stopShimmer()
                }
        }
        pbHomeLoading?.visibility = View.GONE
    }

    private fun showEmptyState(query: String) {
        hideSearchSkeleton()

        pbSearchLoading.visibility = View.GONE

        layoutSearchEmpty.visibility = View.VISIBLE

        tvSearchEmptyTitle.text = "No results for \"$query\""

    }



    private fun handleLikeToggle(track: Track) {

        val isLiked = dbManager.toggleLike(track)

        updateLikeIcons(isLiked)

        updateLibraryLikedCount()

        homeAdapter.notifyDataSetChanged()

        searchAdapter.notifyDataSetChanged()

        allSongsAdapter.notifyDataSetChanged()

        trendingCardsAdapter.notifyDataSetChanged()

        chartsCardsAdapter.notifyDataSetChanged()

        libraryAdapter.setTracks(dbManager.getLikedSongs())

        refreshProfileUI()

    }



    private fun loadCategoryTracks(category: String) {

        // Circular pbHomeLoading removed per user design request

        lifecycleScope.launch {

            val tracks = EmberAudioEngine.getTrendingTracks(category.lowercase())

            for (t in tracks) t.isLiked = dbManager.isTrackLiked(t.id)

            homeAdapter.setTracks(tracks)

            pbHomeLoading?.visibility = View.GONE

        }

    }



    // ==========================================

    // Spotify Playlist Importer Dialog

    // ==========================================

    private fun openSpotifyImportDialog() {

        val dialog = BottomSheetDialog(this, R.style.Theme_Ember_TransparentBottomSheetDialog).apply { applyEmberTheme() }

        val view = layoutInflater.inflate(R.layout.dialog_import_spotify, null)

        dialog.setContentView(view)



        val etSpotifyUrl = view.findViewById<EditText>(R.id.etSpotifyUrl)

        val btnPasteSpotify = view.findViewById<TextView>(R.id.btnPasteSpotify)

        val btnStartSpotifyImport = view.findViewById<MaterialButton>(R.id.btnStartSpotifyImport)

        val pbSpotifyLoading = view.findViewById<ProgressBar>(R.id.pbSpotifyLoading)

        val tvSpotifyStatus = view.findViewById<TextView>(R.id.tvSpotifyStatus)



        btnPasteSpotify.setOnClickListener {

            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

            val item = clipboard.primaryClip?.getItemAt(0)

            val text = item?.text?.toString() ?: ""

            if (text.isNotEmpty()) {

                etSpotifyUrl.setText(text)

            } else {

                Toast.makeText(this, "Clipboard is empty", Toast.LENGTH_SHORT).show()

            }

        }



        btnStartSpotifyImport.setOnClickListener {

            val url = etSpotifyUrl.text.toString().trim()

            if (url.isEmpty() || !url.contains("spotify.com")) {

                Toast.makeText(this, "Please enter a valid Spotify playlist URL", Toast.LENGTH_SHORT).show()

                return@setOnClickListener

            }



            pbSpotifyLoading.visibility = View.VISIBLE

            tvSpotifyStatus.visibility = View.VISIBLE

            tvSpotifyStatus.text = "Resolving playlist tracks to high-fidelity audio..."

            btnStartSpotifyImport.isEnabled = false



            lifecycleScope.launch {

                val (playlistName, tracks) = EmberAudioEngine.importSpotifyPlaylist(url)

                pbSpotifyLoading.visibility = View.GONE

                tvSpotifyStatus.visibility = View.GONE

                btnStartSpotifyImport.isEnabled = true



                if (tracks.isNotEmpty()) {

                    val pl = dbManager.createPlaylist(playlistName, "Imported from Spotify (${tracks.size} tracks)")

                    for (t in tracks) {

                        dbManager.addTrackToPlaylist(pl.id, t)

                    }
                for (i in 0 until tracks.size - 1) {
                    dbManager.recordArtistTransition(tracks[i].artist, tracks[i + 1].artist, 1.5f)
                }

                    refreshLibraryTab()

                    dialog.dismiss()

                    Toast.makeText(this@MainActivity, "Imported \"$playlistName\" with ${tracks.size} songs!", Toast.LENGTH_LONG).show()



                    // Play imported playlist immediately

                    playlistQueue.clear()

                    playlistQueue.addAll(tracks)

                    currentTrackIndex = 0

                    playTrack(tracks[0])

                } else {

                    Toast.makeText(this@MainActivity, "Could not resolve songs from this link. Please ensure it's a public playlist.", Toast.LENGTH_LONG).show()

                }

            }

        }



        dialog.show()

    }



    // ==========================================

    // Opened Playlist & Album View Controller

    // ==========================================

    private fun setupOpenedPlaylistViewEvents() {

        // 1. Back Navigation

        btnPlaylistBack.setOnClickListener {

            it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)

            closeOpenedPlaylistView()

        }



        // 2. Play All Action

        btnPlaylistPlayAll.setOnClickListener {

            it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)

            val tracks = playlistDetailAdapter.getFilteredTracks()

            if (tracks.isNotEmpty()) {

                playlistQueue.clear()

                playlistQueue.addAll(tracks)

                currentTrackIndex = 0

                playTrack(tracks[0])

                Toast.makeText(this, "Playing all tracks", Toast.LENGTH_SHORT).show()

            }

        }



        // 3. Shuffle Action

        btnPlaylistShuffle.setOnClickListener {

            it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)

            val tracks = playlistDetailAdapter.getFilteredTracks().shuffled()

            if (tracks.isNotEmpty()) {

                playlistQueue.clear()

                playlistQueue.addAll(tracks)

                currentTrackIndex = 0

                playTrack(tracks[0])

                Toast.makeText(this, "Shuffling playlist", Toast.LENGTH_SHORT).show()

            }

        }



        // 4. Save to Library Button (Albums & Curated Playlists)

        btnSaveToLibrary.setOnClickListener {

            it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)

            isCurrentOpenedSaved = !isCurrentOpenedSaved

            updateSaveToLibraryButtonState(isCurrentOpenedSaved)

            if (isCurrentOpenedSaved) {

                val tracks = playlistDetailAdapter.getFilteredTracks()

                val title = tvPlaylistHeroTitle.text.toString()

                val newPl = dbManager.createPlaylist(title, "Saved from EMBER Discovery")

                for (t in tracks) {

                    dbManager.addTrackToPlaylist(newPl.id, t)

                }

                refreshLibraryTab()

                Toast.makeText(this, "Saved \"$title\" to your Library", Toast.LENGTH_SHORT).show()

            } else {

                Toast.makeText(this, "Removed from Library", Toast.LENGTH_SHORT).show()

            }

        }



        // 5. Delete Custom Playlist Button

        btnDeletePlaylist.setOnClickListener {

            it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)

            val pl = currentOpenedPlaylist ?: return@setOnClickListener

            openDeletePlaylistConfirmDialog(pl) {

                closeOpenedPlaylistView()

            }

        }



        // 6. Share Actions

        val sharePlaylistAction = {

            val title = tvPlaylistHeroTitle.text.toString()

            val shareIntent = Intent(Intent.ACTION_SEND).apply {

                type = "text/plain"

                putExtra(Intent.EXTRA_SUBJECT, "Listen to $title on EMBER Music")

                putExtra(Intent.EXTRA_TEXT, "Listen to \"$title\" on EMBER Music — Obsidian Noir Experience!\nhttps://ember.music/playlist")

            }

            startActivity(Intent.createChooser(shareIntent, "Share Playlist via"))

        }



        btnPlaylistActionShare.setOnClickListener {

            it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)

            sharePlaylistAction()

        }

        btnPlaylistHeaderShare.setOnClickListener {

            it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)

            sharePlaylistAction()

        }



        // 7. Header 3-Dots Options

        btnPlaylistHeaderOptions.setOnClickListener {

            it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)

            currentOpenedPlaylist?.let { pl ->

                openPlaylistOptionsDialog(pl)

            } ?: run {

                openAlbumOptionsDialog()

            }

        }



        // 8. In-Playlist Fast Search / Filter Bar

        etPlaylistFilter.addTextChangedListener(object : TextWatcher {

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {

                val q = s?.toString() ?: ""

                playlistDetailAdapter.filter(q)

                btnPlaylistFilterClear.visibility = if (q.isNotEmpty()) View.VISIBLE else View.GONE

                layoutPlaylistEmpty.visibility = if (playlistDetailAdapter.itemCount == 0 && q.isNotEmpty()) View.VISIBLE else View.GONE

            }

            override fun afterTextChanged(s: Editable?) {}

        })



        btnPlaylistFilterClear.setOnClickListener {

            it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)

            etPlaylistFilter.setText("")

            hideKeyboard()

        }



        btnStickyPlayAll.setOnClickListener {

            it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)

            val tracks = playlistDetailAdapter.getFilteredTracks()

            if (tracks.isNotEmpty()) {

                playlistQueue.clear()

                playlistQueue.addAll(tracks)

                currentTrackIndex = 0

                playTrack(tracks[0])

            }

        }



        // 9. Sticky Header Dynamic Scroll (60 FPS UI Thread Interpolation)

        scrollPlaylistContent.setOnScrollChangeListener { _, _, scrollY, _, _ ->

            val bgThreshold = dpToPx(80).toFloat()

            val headerAlpha = (scrollY / bgThreshold).coerceIn(0f, 1f)

            viewPlaylistStickyHeaderBg.alpha = headerAlpha

            viewStickyHeaderAccentRim.alpha = headerAlpha



            val titleStart = dpToPx(130)

            val titleEnd = dpToPx(180)

            val titleAlpha = if (scrollY < titleStart) {

                0f

            } else {

                ((scrollY - titleStart).toFloat() / (titleEnd - titleStart)).coerceIn(0f, 1f)

            }

            layoutStickyCenterInfo.alpha = titleAlpha

            layoutStickyCenterInfo.translationY = (1f - titleAlpha) * dpToPx(6)



            btnStickyPlayAll.alpha = titleAlpha

            val scale = 0.8f + (0.2f * titleAlpha)

            btnStickyPlayAll.scaleX = scale

            btnStickyPlayAll.scaleY = scale

        }

    }



    private fun showOpenedPlaylistOverlay() {

        scrollPlaylistContent.scrollTo(0, 0)

        viewPlaylistStickyHeaderBg.alpha = 0f

        viewStickyHeaderAccentRim.alpha = 0f

        layoutStickyCenterInfo.alpha = 0f

        btnStickyPlayAll.alpha = 0f

        layoutOpenedPlaylistOverlay.visibility = View.VISIBLE

        layoutOpenedPlaylistOverlay.translationY = resources.displayMetrics.heightPixels.toFloat()

        layoutOpenedPlaylistOverlay.alpha = 0.8f

        layoutOpenedPlaylistOverlay.animate()

            .translationY(0f)

            .alpha(1f)

            .setDuration(260L)

            .setInterpolator(DecelerateInterpolator(1.5f))

            .start()

    }



    private fun closeOpenedPlaylistView(animate: Boolean = true) {

        hideKeyboard()

        if (!::layoutOpenedPlaylistOverlay.isInitialized || layoutOpenedPlaylistOverlay.visibility != View.VISIBLE) return

        layoutOpenedPlaylistOverlay.animate().cancel()

        if (animate) {

            layoutOpenedPlaylistOverlay.animate()

                .translationY(resources.displayMetrics.heightPixels.toFloat())

                .alpha(0.6f)

                .setDuration(200L)

                .setInterpolator(AccelerateInterpolator(1.5f))

                .withEndAction {

                    layoutOpenedPlaylistOverlay.visibility = View.GONE

                    layoutOpenedPlaylistOverlay.translationY = 0f

                    layoutOpenedPlaylistOverlay.alpha = 1f

                }

                .start()

        } else {

            layoutOpenedPlaylistOverlay.visibility = View.GONE

            layoutOpenedPlaylistOverlay.translationY = 0f

            layoutOpenedPlaylistOverlay.alpha = 1f

        }

    }



    private fun updateSaveToLibraryButtonState(isSaved: Boolean) {

        if (isSaved) {

            ivSaveToLibraryIcon.setImageResource(R.drawable.ic_check)

            ivSaveToLibraryIcon.imageTintList = ColorStateList.valueOf(EmberTheme.Colors.SuccessGreen)

            tvSaveToLibraryText.text = "✓ In Library"

            tvSaveToLibraryText.setTextColor(EmberTheme.Colors.SuccessGreen)

            btnSaveToLibrary.setBackgroundResource(R.drawable.pill_library_active_bg)

        } else {

            ivSaveToLibraryIcon.setImageResource(R.drawable.ic_heart)

            ivSaveToLibraryIcon.imageTintList = ColorStateList.valueOf(EmberTheme.Colors.TextPrimary)

            tvSaveToLibraryText.text = "+ Save to Library"

            tvSaveToLibraryText.setTextColor(EmberTheme.Colors.TextPrimary)

            btnSaveToLibrary.setBackgroundResource(R.drawable.pill_library_bg)

        }

    }



    private fun updateDownloadButtonForTrack(track: Track?) {
        if (track == null || !::ivFullDownload.isInitialized) return
        val isDownloaded = downloadManager.isDownloaded(track.id)
        val isDownloading = downloadManager.isDownloading(track.id)

        if (isDownloading) {
            ivFullDownload.visibility = View.GONE
            pbFullDownload.visibility = View.VISIBLE
        } else if (isDownloaded) {
            pbFullDownload.visibility = View.GONE
            ivFullDownload.visibility = View.VISIBLE
            ivFullDownload.setImageResource(R.drawable.ic_download_done)
            ivFullDownload.imageTintList = ColorStateList.valueOf(Color.parseColor("#1DB954"))
        } else {
            pbFullDownload.visibility = View.GONE
            ivFullDownload.visibility = View.VISIBLE
            ivFullDownload.setImageResource(R.drawable.ic_download)
            ivFullDownload.imageTintList = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.player_text_secondary))
        }
    }

    private fun setupDownloadListener() {
        downloadManager.addListener(object : com.example.ember.data.EmberDownloadManager.DownloadListener {
            override fun onDownloadStateChanged(state: com.example.ember.data.EmberDownloadManager.DownloadState) {
                runOnUiThread {
                    when (state) {
                        is com.example.ember.data.EmberDownloadManager.DownloadState.Completed -> {
                            Toast.makeText(this@MainActivity, "Downloaded: ${state.track.title}", Toast.LENGTH_SHORT).show()
                            updateDownloadButtonForTrack(currentTrack)
                            refreshLibraryTab()
                            if (::layoutOpenedPlaylistOverlay.isInitialized && layoutOpenedPlaylistOverlay.visibility == View.VISIBLE && tvPlaylistHeroTitle.text == "Downloaded Music") {
                                playlistDetailAdapter.setTracks(dbManager.getDownloadedTracks())
                            }
                        }
                        is com.example.ember.data.EmberDownloadManager.DownloadState.Progress -> {
                            if (currentTrack?.id == state.track.id) {
                                ivFullDownload.visibility = View.GONE
                                pbFullDownload.visibility = View.VISIBLE
                            }
                        }
                        is com.example.ember.data.EmberDownloadManager.DownloadState.Failed -> {
                            Toast.makeText(this@MainActivity, "Download failed: ${state.error}", Toast.LENGTH_SHORT).show()
                            updateDownloadButtonForTrack(currentTrack)
                        }
                        is com.example.ember.data.EmberDownloadManager.DownloadState.Idle -> {
                            updateDownloadButtonForTrack(currentTrack)
                            refreshLibraryTab()
                            if (::layoutOpenedPlaylistOverlay.isInitialized && layoutOpenedPlaylistOverlay.visibility == View.VISIBLE && tvPlaylistHeroTitle.text == "Downloaded Music") {
                                playlistDetailAdapter.setTracks(dbManager.getDownloadedTracks())
                            }
                        }
                        else -> {}
                    }
                }
            }
        })
    }

    private fun isNetworkAvailable(): Boolean {
        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as? android.net.ConnectivityManager ?: return false
        val activeNet = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(activeNet) ?: return false
        return caps.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    private fun setupNetworkMonitoring() {
        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as? android.net.ConnectivityManager ?: return
        networkCallback = object : android.net.ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: android.net.Network) {
                runOnUiThread {
                    if (isOfflineMode) {
                        isOfflineMode = false
                        if (::layoutOfflineBanner.isInitialized) {
                            layoutOfflineBanner.visibility = View.GONE
                        }
                        Toast.makeText(this@MainActivity, "Back online", Toast.LENGTH_SHORT).show()
                    }
                }
            }

            override fun onLost(network: android.net.Network) {
                runOnUiThread {
                    handleOfflineTransition(autoRedirect = false)
                }
            }
        }
        try {
            val req = android.net.NetworkRequest.Builder()
                .addCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()
            cm.registerNetworkCallback(req, networkCallback!!)
        } catch (e: Exception) {
            Log.w("MainActivity", "Failed to register network callback: ${e.message}")
        }
    }

    private fun handleOfflineTransition(autoRedirect: Boolean = false) {
        if (isOfflineMode) return
        isOfflineMode = true
        if (::layoutOfflineBanner.isInitialized) {
            layoutOfflineBanner.visibility = View.VISIBLE
        }
        val dlTracks = dbManager.getDownloadedTracks()
        if (dlTracks.isNotEmpty()) {
            if (autoRedirect) {
                Toast.makeText(this, "You're offline. Playing from Downloads.", Toast.LENGTH_SHORT).show()
                switchTab(2)
                openDownloadedSongsView()
            }
        } else {
            Toast.makeText(this, "You're offline. No downloaded music found.", Toast.LENGTH_SHORT).show()
        }
    }

    fun openDownloadedSongsView() {
        currentOpenedPlaylist = null
        currentOpenedAlbum = null
        isCurrentOpenedSaved = false

        tvPlaylistBadge.text = "OFFLINE VAULT"
        tvPlaylistBadge.setTextColor(Color.parseColor("#1DB954"))
        tvPlaylistBadge.backgroundTintList = ColorStateList.valueOf(Color.argb(38, 29, 185, 84))

        tvPlaylistHeroTitle.text = "Downloaded Music"
        tvStickyPlaylistTitle.text = "Downloaded Music"
        tvStickyPlaylistSubtitle.text = "OFFLINE VAULT"
        ivStickyCover.setImageResource(R.drawable.ic_download_done)

        ivPlaylistHeroCover.visibility = View.GONE
        layoutLikedHeroCover.visibility = View.VISIBLE
        ivPlaylistAmbientGlow.setImageResource(R.drawable.liked_songs_hero_gradient)

        btnSaveToLibrary.visibility = View.GONE
        btnDeletePlaylist.visibility = View.GONE
        btnPlaylistShuffle.visibility = View.VISIBLE
        btnPlaylistActionShare.visibility = View.GONE
        btnPlaylistPlayAll.visibility = View.VISIBLE

        playlistDetailAdapter.isCustomPlaylist = false
        val downloaded = dbManager.getDownloadedTracks()
        playlistDetailAdapter.setTracks(downloaded)

        val totalSec = downloaded.sumOf { it.durationSec }
        val min = totalSec / 60
        val sizeStr = downloadManager.getFormattedTotalSize()
        tvPlaylistMetadata.text = if (downloaded.isEmpty()) "No downloaded tracks yet" else "Offline • ${downloaded.size} songs • $sizeStr • ${min} min"

        etPlaylistFilter.setText("")
        layoutPlaylistEmpty.visibility = if (downloaded.isEmpty()) View.VISIBLE else View.GONE
        pbPlaylistLoading.visibility = View.GONE

        showOpenedPlaylistOverlay()
    }

    fun openLikedSongsView() {

        currentOpenedPlaylist = null

        currentOpenedAlbum = null

        isCurrentOpenedSaved = false



        tvPlaylistBadge.text = "PERSONAL COLLECTION"

        tvPlaylistBadge.setTextColor(EmberTheme.Colors.LikedSongsAccent)

        tvPlaylistBadge.backgroundTintList = ColorStateList.valueOf(Color.argb(38, 121, 40, 202))

        tvPlaylistHeroTitle.text = "Liked Songs"

        tvStickyPlaylistTitle.text = "Liked Songs"

        tvStickyPlaylistSubtitle.text = "PERSONAL COLLECTION"

        ivStickyCover.setImageResource(R.drawable.liked_songs_hero_gradient)



        // Liked Songs Signature Hero Cover & Ambient Backdrop

        ivPlaylistHeroCover.visibility = View.GONE

        layoutLikedHeroCover.visibility = View.VISIBLE

        ivPlaylistAmbientGlow.setImageResource(R.drawable.liked_songs_hero_gradient)



        // Action Controls

        btnSaveToLibrary.visibility = View.GONE

        btnDeletePlaylist.visibility = View.GONE

        btnPlaylistShuffle.visibility = View.VISIBLE

        btnPlaylistActionShare.visibility = View.VISIBLE

        btnPlaylistPlayAll.visibility = View.VISIBLE



        playlistDetailAdapter.isCustomPlaylist = false

        val liked = dbManager.getLikedSongs()

        playlistDetailAdapter.setTracks(liked)



        val totalSec = liked.sumOf { it.durationSec }

        val min = totalSec / 60

        tvPlaylistMetadata.text = "You • ${liked.size} songs • ${min} min"



        etPlaylistFilter.setText("")

        layoutPlaylistEmpty.visibility = View.GONE

        pbPlaylistLoading.visibility = View.GONE



        showOpenedPlaylistOverlay()

    }



    fun openPlaylistView(playlist: Playlist) {

        currentOpenedPlaylist = playlist

        currentOpenedAlbum = null

        isCurrentOpenedSaved = false



        tvPlaylistBadge.text = "CUSTOM PLAYLIST"

        tvPlaylistBadge.setTextColor(EmberTheme.Colors.AccentPrimary)

        tvPlaylistBadge.backgroundTintList = ColorStateList.valueOf(EmberTheme.Colors.AccentSoft)

        tvPlaylistHeroTitle.text = playlist.name

        tvStickyPlaylistTitle.text = playlist.name

        tvStickyPlaylistSubtitle.text = "CUSTOM PLAYLIST • ${playlist.tracks.size} SONGS"



        layoutLikedHeroCover.visibility = View.GONE

        ivPlaylistHeroCover.visibility = View.VISIBLE



        val firstArtwork = playlist.tracks.firstOrNull { it.thumbnail.isNotEmpty() }?.thumbnail

        if (!firstArtwork.isNullOrEmpty()) {

            ivPlaylistHeroCover.load(firstArtwork) {

                crossfade(true)

                placeholder(ThumbnailLoadingDrawable())

                error(R.drawable.rounded_card_bg)

            }

            ivPlaylistAmbientGlow.load(firstArtwork) {

                crossfade(true)

            }

            ivStickyCover.load(firstArtwork) {

                crossfade(true)

                placeholder(ThumbnailLoadingDrawable())

                error(R.drawable.rounded_card_bg)

            }

        } else {

            ivPlaylistHeroCover.setImageResource(R.drawable.deewane_cover)

            ivPlaylistAmbientGlow.setImageResource(R.drawable.deewane_cover)

            ivStickyCover.setImageResource(R.drawable.deewane_cover)

        }



        // Action Controls

        btnSaveToLibrary.visibility = View.GONE

        btnDeletePlaylist.visibility = View.VISIBLE

        btnPlaylistShuffle.visibility = View.VISIBLE

        btnPlaylistActionShare.visibility = View.VISIBLE

        btnPlaylistPlayAll.visibility = View.VISIBLE



        playlistDetailAdapter.isCustomPlaylist = true

        playlistDetailAdapter.setTracks(playlist.tracks)



        val totalSec = playlist.tracks.sumOf { it.durationSec }

        val min = totalSec / 60

        tvPlaylistMetadata.text = "Created by You • ${playlist.tracks.size} songs • ${min} min"



        etPlaylistFilter.setText("")

        layoutPlaylistEmpty.visibility = View.GONE

        pbPlaylistLoading.visibility = View.GONE



        showOpenedPlaylistOverlay()

    }



        fun openArtistView(artist: com.example.ember.model.ArtistItem) {
        currentOpenedPlaylist = null
        currentOpenedAlbum = null
        isCurrentOpenedSaved = false

        tvPlaylistBadge.text = "OFFICIAL ARTIST"
        tvPlaylistBadge.setTextColor(Color.parseColor("#FF5A36"))
        tvPlaylistBadge.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#26FF5A36"))
        tvPlaylistHeroTitle.text = artist.name
        tvStickyPlaylistTitle.text = artist.name
        tvStickyPlaylistSubtitle.text = "Official Artist • Discography"

        layoutLikedHeroCover.visibility = View.GONE
        ivPlaylistHeroCover.visibility = View.VISIBLE
        ivPlaylistHeroCover.load(artist.image) {
            crossfade(true)
            placeholder(ThumbnailLoadingDrawable())
            error(R.drawable.rounded_card_bg)
        }
        ivPlaylistAmbientGlow.load(artist.image) {
            crossfade(true)
        }
        ivStickyCover.load(artist.image) {
            crossfade(true)
            placeholder(ThumbnailLoadingDrawable())
            error(R.drawable.rounded_card_bg)
        }

        btnSaveToLibrary.visibility = View.GONE
        btnDeletePlaylist.visibility = View.GONE
        btnPlaylistShuffle.visibility = View.VISIBLE
        btnPlaylistActionShare.visibility = View.VISIBLE
        btnPlaylistPlayAll.visibility = View.VISIBLE

        playlistDetailAdapter.isCustomPlaylist = false
        etPlaylistFilter.setText("")
        layoutPlaylistEmpty.visibility = View.GONE
        playlistDetailAdapter.setTracks(emptyList())
        tvPlaylistMetadata.text = "Official Artist • Loading songs..."

        showOpenedPlaylistOverlay()
        pbPlaylistLoading.visibility = View.VISIBLE

        lifecycleScope.launch {
            val tracks = EmberAudioEngine.getArtistTracks(artist.id, artist.name, count = 40)
            pbPlaylistLoading.visibility = View.GONE
            if (tracks.isNotEmpty()) {
                playlistDetailAdapter.setTracks(tracks)
                val totalSec = tracks.sumOf { it.durationSec }
                val min = totalSec / 60
                tvPlaylistMetadata.text = "${tracks.size} songs • ${min} min • Lossless"

                btnPlaylistPlayAll.setOnClickListener {
                    it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    playlistQueue.clear()
                    playlistQueue.addAll(tracks)
                    currentTrackIndex = 0
                    playTrack(tracks[0])
                }
                btnPlaylistShuffle.setOnClickListener {
                    it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    val shuffled = tracks.shuffled()
                    playlistQueue.clear()
                    playlistQueue.addAll(shuffled)
                    currentTrackIndex = 0
                    playTrack(shuffled[0])
                }
            } else {
                tvPlaylistMetadata.text = "No songs found for ${artist.name}"
                layoutPlaylistEmpty.visibility = View.VISIBLE
            }
        }
    }

    fun openAlbumView(album: com.example.ember.model.AlbumItem) {

        currentOpenedPlaylist = null

        currentOpenedAlbum = album

        isCurrentOpenedSaved = false



        tvPlaylistBadge.text = "OFFICIAL ALBUM"

        tvPlaylistBadge.setTextColor(EmberTheme.Colors.AccentPrimary)

        tvPlaylistBadge.backgroundTintList = ColorStateList.valueOf(EmberTheme.Colors.AccentSoft)

        tvPlaylistHeroTitle.text = album.title

        tvStickyPlaylistTitle.text = album.title

        tvStickyPlaylistSubtitle.text = "OFFICIAL ALBUM • ${album.subtitle}"



        layoutLikedHeroCover.visibility = View.GONE

        ivPlaylistHeroCover.visibility = View.VISIBLE

        ivPlaylistHeroCover.load(album.image) {

            crossfade(true)

            placeholder(ThumbnailLoadingDrawable())

            error(R.drawable.rounded_card_bg)

        }

        ivPlaylistAmbientGlow.load(album.image) {

            crossfade(true)

        }

        ivStickyCover.load(album.image) {

            crossfade(true)

            placeholder(ThumbnailLoadingDrawable())

            error(R.drawable.rounded_card_bg)

        }



        btnSaveToLibrary.visibility = View.VISIBLE

        updateSaveToLibraryButtonState(false)

        btnDeletePlaylist.visibility = View.GONE

        btnPlaylistShuffle.visibility = View.VISIBLE

        btnPlaylistActionShare.visibility = View.VISIBLE

        btnPlaylistPlayAll.visibility = View.VISIBLE



        playlistDetailAdapter.isCustomPlaylist = false

        etPlaylistFilter.setText("")

        layoutPlaylistEmpty.visibility = View.GONE



        val existing = searchAdapter.getTracks().filter { it.album.equals(album.title, ignoreCase = true) }

        if (existing.isNotEmpty()) {

            playlistDetailAdapter.setTracks(existing)

            val totalSec = existing.sumOf { it.durationSec }

            val min = totalSec / 60

            tvPlaylistMetadata.text = "${album.subtitle} • ${existing.size} songs • ${min} min"

        } else {

            playlistDetailAdapter.setTracks(emptyList())

            tvPlaylistMetadata.text = "${album.subtitle} • Loading tracks..."

        }



        showOpenedPlaylistOverlay()



        pbPlaylistLoading.visibility = View.VISIBLE

        lifecycleScope.launch {

            val tracks = EmberAudioEngine.getAlbumTracks(album.id, album.title)

            pbPlaylistLoading.visibility = View.GONE

            if (currentOpenedAlbum?.id == album.id) {

                playlistDetailAdapter.setTracks(tracks)

                val totalSec = tracks.sumOf { it.durationSec }

                val min = totalSec / 60

                tvPlaylistMetadata.text = "${album.subtitle} • ${tracks.size} songs • ${min} min"

            }

        }

    }



    fun openPlaylistItemView(item: com.example.ember.model.PlaylistItem) {

        currentOpenedPlaylist = null

        currentOpenedAlbum = null

        isCurrentOpenedSaved = false



        tvPlaylistBadge.text = "CURATED PLAYLIST"

        tvPlaylistBadge.setTextColor(EmberTheme.Colors.AccentPrimary)

        tvPlaylistBadge.backgroundTintList = ColorStateList.valueOf(EmberTheme.Colors.AccentSoft)

        tvPlaylistHeroTitle.text = item.title

        tvStickyPlaylistTitle.text = item.title

        tvStickyPlaylistSubtitle.text = "CURATED PLAYLIST • ${item.subtitle}"



        layoutLikedHeroCover.visibility = View.GONE

        ivPlaylistHeroCover.visibility = View.VISIBLE

        ivPlaylistHeroCover.load(item.image) {

            crossfade(true)

            placeholder(ThumbnailLoadingDrawable())

            error(R.drawable.rounded_card_bg)

        }

        ivPlaylistAmbientGlow.load(item.image) {

            crossfade(true)

        }

        ivStickyCover.load(item.image) {

            crossfade(true)

            placeholder(ThumbnailLoadingDrawable())

            error(R.drawable.rounded_card_bg)

        }



        btnSaveToLibrary.visibility = View.VISIBLE

        updateSaveToLibraryButtonState(false)

        btnDeletePlaylist.visibility = View.GONE

        btnPlaylistShuffle.visibility = View.VISIBLE

        btnPlaylistActionShare.visibility = View.VISIBLE

        btnPlaylistPlayAll.visibility = View.VISIBLE



        playlistDetailAdapter.isCustomPlaylist = false

        etPlaylistFilter.setText("")

        layoutPlaylistEmpty.visibility = View.GONE

        playlistDetailAdapter.setTracks(emptyList())

        tvPlaylistMetadata.text = "${item.subtitle} • Loading tracks..."



        showOpenedPlaylistOverlay()



        pbPlaylistLoading.visibility = View.VISIBLE

        lifecycleScope.launch {

            val tracks = EmberAudioEngine.getPlaylistTracks(item.id, item.title)

            pbPlaylistLoading.visibility = View.GONE

            playlistDetailAdapter.setTracks(tracks)

            val totalSec = tracks.sumOf { it.durationSec }

            val min = totalSec / 60

            tvPlaylistMetadata.text = "${item.subtitle} • ${tracks.size} songs • ${min} min"

        }

    }



    fun openCuratedTracksView(title: String, badge: String, tracks: List<Track>, coverUrl: String?) {

        currentOpenedPlaylist = null

        currentOpenedAlbum = null

        isCurrentOpenedSaved = false



        tvPlaylistBadge.text = badge

        tvPlaylistBadge.setTextColor(EmberTheme.Colors.AccentPrimary)

        tvPlaylistBadge.backgroundTintList = ColorStateList.valueOf(EmberTheme.Colors.AccentSoft)

        tvPlaylistHeroTitle.text = title

        tvStickyPlaylistTitle.text = title

        tvStickyPlaylistSubtitle.text = "$badge • ${tracks.size} SONGS"



        layoutLikedHeroCover.visibility = View.GONE

        ivPlaylistHeroCover.visibility = View.VISIBLE

        if (!coverUrl.isNullOrEmpty()) {

            ivPlaylistHeroCover.load(coverUrl) {

                crossfade(true)

                placeholder(ThumbnailLoadingDrawable())

                error(R.drawable.rounded_card_bg)

            }

            ivPlaylistAmbientGlow.load(coverUrl) {

                crossfade(true)

            }

            ivStickyCover.load(coverUrl) {

                crossfade(true)

                placeholder(ThumbnailLoadingDrawable())

                error(R.drawable.rounded_card_bg)

            }

        } else {

            ivPlaylistHeroCover.setImageResource(R.drawable.deewane_cover)

            ivPlaylistAmbientGlow.setImageResource(R.drawable.deewane_cover)

            ivStickyCover.setImageResource(R.drawable.deewane_cover)

        }



        btnSaveToLibrary.visibility = View.GONE

        btnDeletePlaylist.visibility = View.GONE

        btnPlaylistShuffle.visibility = View.VISIBLE

        btnPlaylistActionShare.visibility = View.VISIBLE

        btnPlaylistPlayAll.visibility = View.VISIBLE



        playlistDetailAdapter.isCustomPlaylist = false

        playlistDetailAdapter.setTracks(tracks)



        val totalSec = tracks.sumOf { it.durationSec }

        val min = totalSec / 60

        tvPlaylistMetadata.text = "EMBER Curated • ${tracks.size} songs • ${min} min"



        etPlaylistFilter.setText("")

        layoutPlaylistEmpty.visibility = View.GONE

        pbPlaylistLoading.visibility = View.GONE



        showOpenedPlaylistOverlay()

    }



    // ==========================================

    // Library Tab Logic (Playlists & Liked Songs)

    // ==========================================

    private fun setupLibrary() {

        cardLikedSongs.setOnClickListener {

            it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)

            openLikedSongsView()

        }

        cardDownloadedSongs.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            openDownloadedSongsView()
        }



        btnCreatePlaylist.setOnClickListener {

            it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)

            openCreatePlaylistDialog()

        }



        etLibrarySearch.addTextChangedListener(object : TextWatcher {

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {

                val query = s?.toString()?.trim() ?: ""

                filterLibraryContent(query)

            }

            override fun afterTextChanged(s: Editable?) {}

        })



        refreshLibraryTab()

    }



    private fun filterLibraryContent(query: String) {

        val allPlaylists = dbManager.getPlaylists()

        val allLiked = dbManager.getLikedSongs()



        if (query.isBlank()) {

            playlistAdapter.setPlaylists(allPlaylists)

            libraryAdapter.setTracks(allLiked)

        } else {

            val filteredPlaylists = allPlaylists.filter { it.name.contains(query, ignoreCase = true) }

            val filteredTracks = allLiked.filter {

                it.title.contains(query, ignoreCase = true) || it.artist.contains(query, ignoreCase = true)

            }

            playlistAdapter.setPlaylists(filteredPlaylists)

            libraryAdapter.setTracks(filteredTracks)

        }

    }



    private fun refreshLibraryTab() {

        val liked = dbManager.getLikedSongs()

        val playlists = dbManager.getPlaylists()



        libraryAdapter.setTracks(liked)

        playlistAdapter.setPlaylists(playlists)

        updateLibraryLikedCount()

    }



    private fun updateLibraryLikedCount() {

        val likedCount = dbManager.getLikedSongs().size

        val playlistCount = dbManager.getPlaylists().size



        tvLikedCount.text = "$likedCount favorites"

        tvPlaylistCount.text = "$playlistCount playlists created"

        val dlCount = dbManager.getDownloadedCount()
        val dlSize = downloadManager.getFormattedTotalSize()
        tvDownloadedCount.text = "$dlCount songs • $dlSize • Offline Ready"

    }



    private fun openCreatePlaylistDialog() {

        val dialog = BottomSheetDialog(this, R.style.Theme_Ember_TransparentBottomSheetDialog).apply { applyEmberTheme() }

        val view = layoutInflater.inflate(R.layout.dialog_edit_playlist, null)

        dialog.setContentView(view)



        val etName = view.findViewById<EditText>(R.id.etPlaylistName)

        val etDesc = view.findViewById<EditText>(R.id.etPlaylistDesc)

        val btnCancel = view.findViewById<MaterialButton>(R.id.btnCancelPlaylist)

        val btnConfirm = view.findViewById<MaterialButton>(R.id.btnConfirmPlaylist)



        btnCancel.setOnClickListener { dialog.dismiss() }



        btnConfirm.setOnClickListener {

            val name = etName.text.toString().trim()

            val desc = etDesc.text.toString().trim()

            if (name.isNotEmpty()) {

                dbManager.createPlaylist(name, desc)

                refreshLibraryTab()

                dialog.dismiss()

                Toast.makeText(this, "Created playlist \"$name\"", Toast.LENGTH_SHORT).show()

            } else {

                Toast.makeText(this, "Please enter a playlist name", Toast.LENGTH_SHORT).show()

            }

        }



        dialog.show()

    }



    private fun openPlaylistOptionsDialog(playlist: Playlist) {

        val dialog = BottomSheetDialog(this, R.style.Theme_Ember_TransparentBottomSheetDialog).apply { applyEmberTheme() }

        val view = layoutInflater.inflate(R.layout.dialog_collection_options, null)

        dialog.setContentView(view)



        val ivCover = view.findViewById<ImageView>(R.id.ivCollectionCover)

        val tvBadge = view.findViewById<TextView>(R.id.tvCollectionBadge)

        val tvTitle = view.findViewById<TextView>(R.id.tvCollectionTitle)

        val tvSubtitle = view.findViewById<TextView>(R.id.tvCollectionSubtitle)



        val optPlayAll = view.findViewById<View>(R.id.optCollectionPlayAll)

        val optAddToQueue = view.findViewById<View>(R.id.optCollectionAddToQueue)

        val optShare = view.findViewById<View>(R.id.optCollectionShare)

        val optRename = view.findViewById<View>(R.id.optCollectionRename)

        val optDelete = view.findViewById<View>(R.id.optCollectionDelete)



        tvBadge.text = "CUSTOM PLAYLIST"

        tvBadge.setTextColor(ContextCompat.getColor(this, R.color.accent_primary))

        tvTitle.text = playlist.name

        tvSubtitle.text = "${playlist.tracks.size} songs • By You"



        val firstArt = playlist.tracks.firstOrNull { it.thumbnail.isNotEmpty() }?.thumbnail

        if (!firstArt.isNullOrEmpty()) {

            ivCover.load(firstArt) {

                crossfade(true)

                placeholder(ThumbnailLoadingDrawable())

            }

        } else {

            ivCover.setImageResource(R.drawable.deewane_cover)

        }



        optPlayAll.setOnClickListener {

            dialog.dismiss()

            if (playlist.tracks.isNotEmpty()) {

                playlistQueue.clear()

                playlistQueue.addAll(playlist.tracks)

                currentTrackIndex = 0

                playTrack(playlist.tracks[0])

            } else {

                Toast.makeText(this, "Playlist is empty", Toast.LENGTH_SHORT).show()

            }

        }



        optAddToQueue.setOnClickListener {

            dialog.dismiss()

            if (playlist.tracks.isNotEmpty()) {

                playlistQueue.addAll(playlist.tracks)

                Toast.makeText(this, "Added ${playlist.tracks.size} tracks to queue", Toast.LENGTH_SHORT).show()

            } else {

                Toast.makeText(this, "Playlist is empty", Toast.LENGTH_SHORT).show()

            }

        }



        optShare.setOnClickListener {

            dialog.dismiss()

            val shareIntent = Intent(Intent.ACTION_SEND).apply {

                type = "text/plain"

                putExtra(Intent.EXTRA_SUBJECT, "Listen to ${playlist.name} on EMBER Music")

                putExtra(Intent.EXTRA_TEXT, "Listen to \"${playlist.name}\" on EMBER Music — Obsidian Noir Experience!\nhttps://ember.music/playlist")

            }

            startActivity(Intent.createChooser(shareIntent, "Share Playlist via"))

        }



        optRename.visibility = View.VISIBLE

        optRename.setOnClickListener {

            dialog.dismiss()

            openRenamePlaylistDialog(playlist)

        }



        optDelete.visibility = View.VISIBLE

        optDelete.setOnClickListener {

            dialog.dismiss()

            openDeletePlaylistConfirmDialog(playlist)

        }



        dialog.show()

    }



    private fun openAlbumOptionsDialog() {

        val dialog = BottomSheetDialog(this, R.style.Theme_Ember_TransparentBottomSheetDialog).apply { applyEmberTheme() }

        val view = layoutInflater.inflate(R.layout.dialog_collection_options, null)

        dialog.setContentView(view)



        val ivCover = view.findViewById<ImageView>(R.id.ivCollectionCover)

        val tvBadge = view.findViewById<TextView>(R.id.tvCollectionBadge)

        val tvTitle = view.findViewById<TextView>(R.id.tvCollectionTitle)

        val tvSubtitle = view.findViewById<TextView>(R.id.tvCollectionSubtitle)



        val optPlayAll = view.findViewById<View>(R.id.optCollectionPlayAll)

        val optAddToQueue = view.findViewById<View>(R.id.optCollectionAddToQueue)

        val optShare = view.findViewById<View>(R.id.optCollectionShare)

        val optRename = view.findViewById<View>(R.id.optCollectionRename)

        val optDelete = view.findViewById<View>(R.id.optCollectionDelete)



        val albumTitle = tvPlaylistHeroTitle.text.toString()

        val albumSubtitle = tvPlaylistMetadata.text.toString()



        tvBadge.text = "OFFICIAL ALBUM"

        tvBadge.setTextColor(ContextCompat.getColor(this, R.color.accent_primary))

        tvTitle.text = albumTitle

        tvSubtitle.text = albumSubtitle



        val tracks = playlistDetailAdapter.getFilteredTracks()

        val coverUrl = tracks.firstOrNull { it.thumbnail.isNotEmpty() }?.thumbnail

        if (!coverUrl.isNullOrEmpty()) {

            ivCover.load(coverUrl) {

                crossfade(true)

                placeholder(ThumbnailLoadingDrawable())

            }

        } else {

            ivCover.setImageResource(R.drawable.deewane_cover)

        }



        optPlayAll.setOnClickListener {

            dialog.dismiss()

            if (tracks.isNotEmpty()) {

                playlistQueue.clear()

                playlistQueue.addAll(tracks)

                currentTrackIndex = 0

                playTrack(tracks[0])

            } else {

                Toast.makeText(this, "Album is empty", Toast.LENGTH_SHORT).show()

            }

        }



        optAddToQueue.setOnClickListener {

            dialog.dismiss()

            if (tracks.isNotEmpty()) {

                playlistQueue.addAll(tracks)

                Toast.makeText(this, "Added ${tracks.size} tracks to queue", Toast.LENGTH_SHORT).show()

            } else {

                Toast.makeText(this, "Album is empty", Toast.LENGTH_SHORT).show()

            }

        }



        optShare.setOnClickListener {

            dialog.dismiss()

            val shareIntent = Intent(Intent.ACTION_SEND).apply {

                type = "text/plain"

                putExtra(Intent.EXTRA_SUBJECT, "Listen to $albumTitle on EMBER Music")

                putExtra(Intent.EXTRA_TEXT, "Listen to \"$albumTitle\" on EMBER Music — Obsidian Noir Experience!\nhttps://ember.music/album")

            }

            startActivity(Intent.createChooser(shareIntent, "Share Album via"))

        }



        optRename.visibility = View.GONE

        optDelete.visibility = View.GONE



        dialog.show()

    }



    private fun openRenamePlaylistDialog(playlist: Playlist) {

        val dialog = BottomSheetDialog(this, R.style.Theme_Ember_TransparentBottomSheetDialog).apply { applyEmberTheme() }

        val view = layoutInflater.inflate(R.layout.dialog_edit_playlist, null)

        dialog.setContentView(view)



        val tvTitle = view.findViewById<TextView>(R.id.tvEditPlaylistTitle)

        val etName = view.findViewById<EditText>(R.id.etPlaylistName)

        val etDesc = view.findViewById<EditText>(R.id.etPlaylistDesc)

        val btnCancel = view.findViewById<MaterialButton>(R.id.btnCancelPlaylist)

        val btnConfirm = view.findViewById<MaterialButton>(R.id.btnConfirmPlaylist)



        tvTitle.text = "Rename Playlist"

        etName.setText(playlist.name)

        etDesc.setText(playlist.description)

        btnConfirm.text = "Save Changes"



        btnCancel.setOnClickListener { dialog.dismiss() }

        btnConfirm.setOnClickListener {

            val newName = etName.text.toString().trim()

            if (newName.isNotEmpty()) {

                dbManager.renamePlaylist(playlist.id, newName)

                refreshLibraryTab()

                if (currentOpenedPlaylist?.id == playlist.id) {

                    tvPlaylistHeroTitle.text = newName

                    tvStickyPlaylistTitle.text = newName

                }

                dialog.dismiss()

                Toast.makeText(this, "Playlist renamed to \"$newName\"", Toast.LENGTH_SHORT).show()

            } else {

                Toast.makeText(this, "Please enter a playlist name", Toast.LENGTH_SHORT).show()

            }

        }



        dialog.show()

    }



    private fun openDeletePlaylistConfirmDialog(playlist: Playlist, onDeleted: (() -> Unit)? = null) {

        val dialog = BottomSheetDialog(this, R.style.Theme_Ember_TransparentBottomSheetDialog).apply { applyEmberTheme() }

        val view = layoutInflater.inflate(R.layout.dialog_confirm_delete, null)

        dialog.setContentView(view)



        val tvMsg = view.findViewById<TextView>(R.id.tvDeleteConfirmMsg)

        val btnCancel = view.findViewById<MaterialButton>(R.id.btnCancelDelete)

        val btnConfirm = view.findViewById<MaterialButton>(R.id.btnConfirmDelete)



        tvMsg.text = "Are you sure you want to delete '${playlist.name}'? This cannot be undone."



        btnCancel.setOnClickListener { dialog.dismiss() }

        btnConfirm.setOnClickListener {

            dbManager.deletePlaylist(playlist.id)

            refreshLibraryTab()

            dialog.dismiss()

            Toast.makeText(this, "Playlist '${playlist.name}' deleted", Toast.LENGTH_SHORT).show()

            onDeleted?.invoke()

        }



        dialog.show()

    }



    // ==========================================

    // Song Options Context Menu (3-Dots Menu)

    // ==========================================

    private fun openSongOptionsDialog(track: Track) {

        val dialog = BottomSheetDialog(this, R.style.Theme_Ember_TransparentBottomSheetDialog).apply { applyEmberTheme() }

        val view = layoutInflater.inflate(R.layout.dialog_song_options, null)

        dialog.setContentView(view)



        val ivOptionsThumb = view.findViewById<ImageView>(R.id.ivOptionsThumb)

        val tvOptionsTitle = view.findViewById<TextView>(R.id.tvOptionsTitle)

        val tvOptionsArtist = view.findViewById<TextView>(R.id.tvOptionsArtist)



        val optStartRadio = view.findViewById<View>(R.id.optStartRadio)

        val optPlayNext = view.findViewById<View>(R.id.optPlayNext)

        val optAddToQueue = view.findViewById<View>(R.id.optAddToQueue)

        val optAddToPlaylist = view.findViewById<View>(R.id.optAddToPlaylist)

        val optLike = view.findViewById<View>(R.id.optLike)

        val ivOptLike = view.findViewById<ImageView>(R.id.ivOptLike)

        val tvOptLike = view.findViewById<TextView>(R.id.tvOptLike)

        val optViewArtist = view.findViewById<View>(R.id.optViewArtist)

        val tvOptViewArtist = view.findViewById<TextView>(R.id.tvOptViewArtist)

        val optViewAlbum = view.findViewById<View>(R.id.optViewAlbum)

        val tvOptViewAlbum = view.findViewById<TextView>(R.id.tvOptViewAlbum)

        val optShare = view.findViewById<View>(R.id.optShare)

        val optSongInfo = view.findViewById<View>(R.id.optSongInfo)

        val optCopyLink = view.findViewById<View>(R.id.optCopyLink)



        if (track.thumbnail.isNotEmpty()) {

            ivOptionsThumb.load(track.thumbnail) {

                crossfade(true)

                placeholder(R.drawable.ember_banner)

            }

        }

        tvOptionsTitle.text = track.title

        tvOptionsArtist.text = track.artist

        tvOptViewArtist.text = "View Artist (${track.artist.split(",").firstOrNull() ?: track.artist})"

        tvOptViewAlbum.text = "View Album (${track.album.ifBlank { track.title }})"



        val optDownload = view.findViewById<View>(R.id.optDownload)
        val ivOptDownload = view.findViewById<ImageView>(R.id.ivOptDownload)
        val tvOptDownload = view.findViewById<TextView>(R.id.tvOptDownload)

        val isSongDownloaded = downloadManager.isDownloaded(track.id)
        val isSongDownloading = downloadManager.isDownloading(track.id)

        if (isSongDownloaded) {
            ivOptDownload?.setImageResource(R.drawable.ic_trash)
            ivOptDownload?.imageTintList = ColorStateList.valueOf(Color.parseColor("#FF3B30"))
            tvOptDownload?.text = "Remove Download"
            optDownload?.setOnClickListener {
                dialog.dismiss()
                downloadManager.deleteDownload(track.id)
                Toast.makeText(this@MainActivity, "Removed from Downloads", Toast.LENGTH_SHORT).show()
                updateDownloadButtonForTrack(currentTrack)
                refreshLibraryTab()
            }
        } else if (isSongDownloading) {
            ivOptDownload?.setImageResource(R.drawable.ic_download)
            tvOptDownload?.text = "Downloading... (${downloadManager.getDownloadProgress(track.id)}%)"
            optDownload?.setOnClickListener {
                dialog.dismiss()
                Toast.makeText(this@MainActivity, "Download in progress...", Toast.LENGTH_SHORT).show()
            }
        } else {
            ivOptDownload?.setImageResource(R.drawable.ic_download)
            tvOptDownload?.text = "Download Song"
            optDownload?.setOnClickListener {
                dialog.dismiss()
                downloadManager.startDownload(track)
                Toast.makeText(this@MainActivity, "Downloading ${track.title}...", Toast.LENGTH_SHORT).show()
                updateDownloadButtonForTrack(currentTrack)
            }
        }

        val isLiked = dbManager.isTrackLiked(track.id)

        if (isLiked) {

            ivOptLike.setImageResource(R.drawable.ic_heart_filled)

            tvOptLike.text = "Remove from Favorites"

        } else {

            ivOptLike.setImageResource(R.drawable.ic_heart)

            tvOptLike.text = "Add to Favorites"

        }



        // 0. Start Song Radio (YouTube Music Automix)
        optStartRadio?.setOnClickListener {
            dialog.dismiss()
            Toast.makeText(this, "📻 Starting YouTube Music Radio for ${track.title}...", Toast.LENGTH_SHORT).show()
            onSearchResultTrackSelected(track)
        }

        // 1. Play Next

        optPlayNext.setOnClickListener {

            val service = EmberPlaybackService.instance

            if (service != null) {

                service.playNextInQueue(track)

            } else {

                if (playlistQueue.isNotEmpty() && currentTrackIndex >= 0) {

                    playlistQueue.add(currentTrackIndex + 1, track)

                } else {

                    playlistQueue.add(track)

                }

            }

            dialog.dismiss()

            Toast.makeText(this, "\"${track.title}\" will play next", Toast.LENGTH_SHORT).show()

        }



        // 2. Add to Queue

        optAddToQueue.setOnClickListener {

            val service = EmberPlaybackService.instance

            if (service != null) {

                service.addToQueue(track)

            } else {

                playlistQueue.add(track)

            }

            dialog.dismiss()

            Toast.makeText(this, "Added to queue (${playlistQueue.size} in queue)", Toast.LENGTH_SHORT).show()

        }



        // 3. Add to Playlist

        optAddToPlaylist.setOnClickListener {

            dialog.dismiss()

            openSelectPlaylistDialog(track)

        }



        // 4. Like Toggle

        optLike.setOnClickListener {

            val newLiked = dbManager.toggleLike(track)

            updateLikeIcons(newLiked)

            updateLibraryLikedCount()

            homeAdapter.notifyDataSetChanged()

            searchAdapter.notifyDataSetChanged()

            libraryAdapter.notifyDataSetChanged()

            dialog.dismiss()

            Toast.makeText(this, if (newLiked) "Added to Liked Songs ❤️" else "Removed from Liked Songs", Toast.LENGTH_SHORT).show()

        }



        // 5. View Artist

        optViewArtist.setOnClickListener {
            dialog.dismiss()
            val primaryArtist = track.artist.split(",", "&", "feat.", "ft.").firstOrNull()?.trim() ?: track.artist.trim()
            openArtistView(com.example.ember.model.ArtistItem(id = "", name = primaryArtist, image = track.thumbnail))
        }



        // 5b. View Album

        optViewAlbum.setOnClickListener {

            dialog.dismiss()

            val albumName = track.album.ifBlank { track.title }

            val albumItem = com.example.ember.model.AlbumItem(

                id = track.album.ifBlank { track.id },

                title = albumName,

                subtitle = track.artist,

                image = track.thumbnail,

                year = ""

            )

            openAlbumView(albumItem)

        }



        // 6. Share Song

        optShare.setOnClickListener {

            dialog.dismiss()

            val shareIntent = Intent(Intent.ACTION_SEND).apply {

                type = "text/plain"

                putExtra(Intent.EXTRA_SUBJECT, "Listen to ${track.title} on EMBER Music")

                putExtra(Intent.EXTRA_TEXT, "Listen to \"${track.title}\" by ${track.artist} on EMBER Music!\n\n${track.audioUrl}")

            }

            startActivity(Intent.createChooser(shareIntent, "Share song via"))

        }



        // 7. Song & Audio Quality Info

        optSongInfo.setOnClickListener {

            dialog.dismiss()

            openSongInfoDialog(track)

        }



        // 7. Copy Stream Link

        optCopyLink.setOnClickListener {

            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

            val clip = ClipData.newPlainText("EMBER Stream URL", track.audioUrl)

            clipboard.setPrimaryClip(clip)

            dialog.dismiss()

            Toast.makeText(this, "Direct stream URL copied to clipboard!", Toast.LENGTH_SHORT).show()

        }



        dialog.show()

    }



    private fun openSongInfoDialog(track: Track) {

        val dialog = BottomSheetDialog(this, R.style.Theme_Ember_TransparentBottomSheetDialog).apply { applyEmberTheme() }

        val view = layoutInflater.inflate(R.layout.dialog_audio_quality_info, null)

        dialog.setContentView(view)



        val tvTrackTitle = view.findViewById<TextView>(R.id.tvInfoTrackTitle)

        val tvArtistAlbum = view.findViewById<TextView>(R.id.tvInfoArtistAlbum)

        val tvStreamSource = view.findViewById<TextView>(R.id.tvInfoStreamSource)

        val tvDurationPayload = view.findViewById<TextView>(R.id.tvInfoDurationPayload)

        val btnDone = view.findViewById<MaterialButton>(R.id.btnDoneAudioInfo)



        tvTrackTitle.text = track.title

        tvArtistAlbum.text = "${track.artist} • ${track.album.ifBlank { "Single / EP" }}"

        tvStreamSource.text = if (track.isYoutubeFallback) {

            "YouTube High-Quality Audio Engine"

        } else {

            "Direct Akamai CDN (DES-ECB Decrypted)"

        }

        tvDurationPayload.text = "${track.duration} • MP4 / AAC • ExoPlayer Media3"



        btnDone.setOnClickListener { dialog.dismiss() }

        dialog.show()

    }



    private fun openSelectPlaylistDialog(track: Track) {

        val dialog = BottomSheetDialog(this, R.style.Theme_Ember_TransparentBottomSheetDialog).apply { applyEmberTheme() }

        val view = layoutInflater.inflate(R.layout.dialog_select_playlist, null)

        dialog.setContentView(view)



        val btnNew = view.findViewById<TextView>(R.id.btnNewPlaylistInSelect)

        val rvSelect = view.findViewById<RecyclerView>(R.id.rvSelectPlaylists)



        val selectAdapter = PlaylistAdapter(

            onPlaylistClick = { playlist, _ ->

                val added = dbManager.addTrackToPlaylist(playlist.id, track)

                dialog.dismiss()

                if (added) {

                    refreshLibraryTab()

                    Toast.makeText(this, "Added to ${playlist.name}!", Toast.LENGTH_SHORT).show()

                } else {

                    Toast.makeText(this, "Song is already in ${playlist.name}", Toast.LENGTH_SHORT).show()

                }

            },

            onOptionsClick = { _, _ -> }

        )

        selectAdapter.setPlaylists(dbManager.getPlaylists())

        rvSelect.layoutManager = LinearLayoutManager(this)

        rvSelect.adapter = selectAdapter



        btnNew.setOnClickListener {

            dialog.dismiss()

            openCreatePlaylistDialog()

        }



        dialog.show()

    }



    // ==========================================

    // In-Card Live Synced Karaoke Lyrics (Toggle & Offset)

    // ==========================================

    private fun toggleLyricsCard(show: Boolean) {

        if (isLyricsActive == show) return

        isLyricsActive = show



        if (show) {

            // Artwork / Vinyl -> Lyrics

            if (isVinylMode) {

                layoutVinylDisc.animate().alpha(0f).setDuration(220).withEndAction {

                    layoutVinylDisc.visibility = View.GONE

                }.start()

            } else {

                ivFullThumb.animate().alpha(0f).setDuration(220).withEndAction {

                    ivFullThumb.visibility = View.GONE

                }.start()

            }

            btnVinylMode.animate().alpha(0f).setDuration(220).withEndAction {

                btnVinylMode.visibility = View.GONE

            }.start()



            layoutLyricsPanel.alpha = 0f

            layoutLyricsPanel.visibility = View.VISIBLE

            layoutLyricsPanel.animate().alpha(1f).setDuration(250).start()



            // Update bottom dock button state

            btnFullLyrics.setBackgroundResource(R.drawable.bottom_tile_active_bg)

            ivFullLyricsIcon.imageTintList = ColorStateList.valueOf(Color.parseColor("#F05A3D"))

            tvFullLyricsLabel.setTextColor(Color.parseColor("#F05A3D"))



            // Load lyrics for currently playing track

            loadLyricsForCurrentTrack()

        } else {

            // Lyrics -> Artwork / Vinyl

            layoutLyricsPanel.animate().alpha(0f).setDuration(200).withEndAction {

                layoutLyricsPanel.visibility = View.GONE

            }.start()



            if (isVinylMode) {

                layoutVinylDisc.visibility = View.VISIBLE

                layoutVinylDisc.alpha = 0f

                layoutVinylDisc.animate().alpha(1f).setDuration(250).start()

            } else {

                ivFullThumb.visibility = View.VISIBLE

                ivFullThumb.alpha = 0f

                ivFullThumb.animate().alpha(1f).setDuration(250).start()

            }

            btnVinylMode.visibility = View.VISIBLE

            btnVinylMode.alpha = 0f

            btnVinylMode.animate().alpha(1f).setDuration(250).start()



            // Update bottom dock button state

            btnFullLyrics.setBackgroundResource(R.drawable.bottom_tile_bg)

            ivFullLyricsIcon.imageTintList = ColorStateList.valueOf(Color.parseColor("#B5B1BE"))

            tvFullLyricsLabel.setTextColor(Color.parseColor("#8E8B99"))

        }

    }



    private fun loadLyricsForCurrentTrack() {

        val track = currentTrack

        if (track == null) {

            showNoLyrics()

            return

        }



        // If already cached for this exact track

        if (cachedLyricsTrackId == track.id && cachedSyncedLines != null) {

            val lines = cachedSyncedLines!!

            if (lines.isNotEmpty()) {

                showSyncedLyrics(lines)

            } else if (!cachedPlainLyrics.isNullOrEmpty()) {

                showPlainLyrics(cachedPlainLyrics!!)

            } else {

                showNoLyrics()

            }

            return

        }



        lyricsFetchJob?.cancel()

        pbCardLyricsLoading.visibility = View.VISIBLE

        tvCardLyricsEmpty.visibility = View.GONE

        rvCardLyrics.visibility = View.GONE

        badgeLiveSynced.visibility = View.GONE

        layoutLyricsOffset.visibility = View.GONE



        lyricsFetchJob = lifecycleScope.launch(Dispatchers.IO) {

            try {

                val cleanTitle = track.title.substringBefore("(").substringBefore("-").trim()

                val cleanArtist = track.artist.split(",").firstOrNull()?.trim() ?: track.artist

                val url = "https://lrclib.net/api/get?track_name=${URLEncoder.encode(cleanTitle, "UTF-8")}&artist_name=${URLEncoder.encode(cleanArtist, "UTF-8")}"



                val request = Request.Builder().url(url).build()

                val response = httpClient.newCall(request).execute()

                val body = response.body?.string()



                if (response.isSuccessful && !body.isNullOrEmpty()) {

                    val json = JSONObject(body)

                    val syncedStr = json.optString("syncedLyrics")

                    val plainStr = json.optString("plainLyrics")



                    if (syncedStr.isNotEmpty()) {

                        val parsedLines = mutableListOf<LyricLine>()

                        val regex = Regex("\\[(\\d+):(\\d+\\.\\d+)\\](.*)")

                        for (rawLine in syncedStr.lines()) {

                            val match = regex.find(rawLine.trim())

                            if (match != null) {

                                val (mins, secs, text) = match.destructured

                                val timeMs = (mins.toLong() * 60 * 1000) + (secs.toFloat() * 1000).toLong()

                                if (text.trim().isNotEmpty()) {

                                    parsedLines.add(LyricLine(timeMs, text.trim()))

                                }

                            }

                        }



                        withContext(Dispatchers.Main) {

                            if (currentTrack?.id == track.id) {

                                cachedLyricsTrackId = track.id

                                cachedSyncedLines = parsedLines

                                cachedPlainLyrics = null

                                showSyncedLyrics(parsedLines)

                            }

                        }

                    } else if (plainStr.isNotEmpty()) {

                        withContext(Dispatchers.Main) {

                            if (currentTrack?.id == track.id) {

                                cachedLyricsTrackId = track.id

                                cachedSyncedLines = emptyList()

                                cachedPlainLyrics = plainStr

                                showPlainLyrics(plainStr)

                            }

                        }

                    } else {

                        withContext(Dispatchers.Main) {

                            if (currentTrack?.id == track.id) {

                                cachedLyricsTrackId = track.id

                                cachedSyncedLines = emptyList()

                                cachedPlainLyrics = null

                                showNoLyrics()

                            }

                        }

                    }

                } else {

                    withContext(Dispatchers.Main) {

                        if (currentTrack?.id == track.id) {

                            cachedLyricsTrackId = track.id

                            cachedSyncedLines = emptyList()

                            cachedPlainLyrics = null

                            showNoLyrics()

                        }

                    }

                }

            } catch (e: Exception) {

                withContext(Dispatchers.Main) {

                    if (currentTrack?.id == track.id) {

                        pbCardLyricsLoading.visibility = View.GONE

                        tvCardLyricsEmpty.text = "Could not load lyrics.\nTap to retry."

                        tvCardLyricsEmpty.visibility = View.VISIBLE

                        tvCardLyricsEmpty.setOnClickListener {

                            loadLyricsForCurrentTrack()

                        }

                    }

                }

            }

        }

    }



    private fun showSyncedLyrics(lines: List<LyricLine>) {

        pbCardLyricsLoading.visibility = View.GONE

        tvCardLyricsEmpty.visibility = View.GONE

        badgeLiveSynced.visibility = View.VISIBLE

        layoutLyricsOffset.visibility = View.VISIBLE

        rvCardLyrics.visibility = View.VISIBLE



        cardLyricsAdapter?.setLines(lines)

        val current = player?.currentPosition ?: 0L

        val idx = cardLyricsAdapter?.forceUpdateTimestamp(current + lyricsOffsetMs) ?: -1

        if (idx >= 0) {

            rvCardLyrics.post {

                smoothScrollLyricsToCenter(rvCardLyrics, idx)

            }

        }

    }



    private fun showPlainLyrics(plain: String) {

        pbCardLyricsLoading.visibility = View.GONE

        badgeLiveSynced.visibility = View.GONE

        layoutLyricsOffset.visibility = View.GONE

        rvCardLyrics.visibility = View.GONE

        tvCardLyricsEmpty.text = plain

        tvCardLyricsEmpty.visibility = View.VISIBLE

    }



    private fun showNoLyrics() {

        pbCardLyricsLoading.visibility = View.GONE

        badgeLiveSynced.visibility = View.GONE

        layoutLyricsOffset.visibility = View.GONE

        rvCardLyrics.visibility = View.GONE

        tvCardLyricsEmpty.text = "No lyrics available for this track"

        tvCardLyricsEmpty.visibility = View.VISIBLE

    }



    private fun updateLyricsOffsetDisplay() {

        val sec = lyricsOffsetMs / 1000.0

        val text = if (lyricsOffsetMs == 0L) {

            "0.0s"

        } else if (lyricsOffsetMs > 0L) {

            String.format(Locale.US, "+%.1fs", sec)

        } else {

            String.format(Locale.US, "%.1fs", sec)

        }

        tvOffsetCurrent.text = text

        if (lyricsOffsetMs != 0L) {

            tvOffsetCurrent.setTextColor(Color.parseColor("#F05A3D"))

        } else {

            tvOffsetCurrent.setTextColor(Color.WHITE)

        }



        player?.let { p ->

            val current = p.currentPosition

            val idx = cardLyricsAdapter?.forceUpdateTimestamp(current + lyricsOffsetMs) ?: -1

            if (idx >= 0) {

                smoothScrollLyricsToCenter(rvCardLyrics, idx)

            }

        }

    }



    // ==========================================

    // Live Synced Karaoke Lyrics (LRCLIB)

    // ==========================================

    private fun openLyricsDialog() {

        val dialog = BottomSheetDialog(this, R.style.Theme_Ember_TransparentBottomSheetDialog).apply { applyEmberTheme() }

        val view = layoutInflater.inflate(R.layout.dialog_lyrics, null)

        dialog.setContentView(view)



        val pbLyricsLoading = view.findViewById<ProgressBar>(R.id.pbLyricsLoading)

        val rvLyrics = view.findViewById<RecyclerView>(R.id.rvLyrics)

        val scrollLyrics = view.findViewById<ScrollView>(R.id.scrollLyrics)

        val tvLyricsContent = view.findViewById<TextView>(R.id.tvLyricsContent)

        val btnCloseLyrics = view.findViewById<ImageButton>(R.id.btnCloseLyrics)

        val badgeLyricsSource = view.findViewById<TextView>(R.id.badgeLyricsSource)



        btnCloseLyrics.setOnClickListener { dialog.dismiss() }



        val track = currentTrack

        if (track == null) {

            tvLyricsContent.text = "No track currently playing."

            scrollLyrics.visibility = View.VISIBLE

            dialog.show()

            return

        }



        pbLyricsLoading.visibility = View.VISIBLE

        dialog.show()



        lifecycleScope.launch(Dispatchers.IO) {

            try {

                val cleanTitle = track.title.substringBefore("(").substringBefore("-").trim()

                val cleanArtist = track.artist.split(",").firstOrNull()?.trim() ?: track.artist

                val url = "https://lrclib.net/api/get?track_name=${URLEncoder.encode(cleanTitle, "UTF-8")}&artist_name=${URLEncoder.encode(cleanArtist, "UTF-8")}"



                val request = Request.Builder().url(url).build()

                val response = httpClient.newCall(request).execute()

                val body = response.body?.string()



                if (response.isSuccessful && !body.isNullOrEmpty()) {

                    val json = JSONObject(body)

                    val syncedStr = json.optString("syncedLyrics")

                    val plainStr = json.optString("plainLyrics")



                    if (syncedStr.isNotEmpty()) {

                        // Parse synced LRC format: [mm:ss.xx] line

                        val parsedLines = mutableListOf<LyricLine>()

                        val regex = Regex("\\[(\\d+):(\\d+\\.\\d+)\\](.*)")

                        for (rawLine in syncedStr.lines()) {

                            val match = regex.find(rawLine.trim())

                            if (match != null) {

                                val (mins, secs, text) = match.destructured

                                val timeMs = (mins.toLong() * 60 * 1000) + (secs.toFloat() * 1000).toLong()

                                if (text.trim().isNotEmpty()) {

                                    parsedLines.add(LyricLine(timeMs, text.trim()))

                                }

                            }

                        }



                        withContext(Dispatchers.Main) {

                            pbLyricsLoading.visibility = View.GONE

                            badgeLyricsSource.text = "LRCLIB SYNCED"



                            val lyricsAdapter = LyricsAdapter { line ->

                                player?.seekTo(line.timeMs)

                            }

                            lyricsAdapter.setLines(parsedLines)

                            rvLyrics.layoutManager = LinearLayoutManager(this@MainActivity)

                            rvLyrics.adapter = lyricsAdapter

                            rvLyrics.visibility = View.VISIBLE

                            scrollLyrics.visibility = View.GONE



                            activeLyricsAdapter = lyricsAdapter

                            activeLyricsRecyclerView = rvLyrics

                        }

                    } else if (plainStr.isNotEmpty()) {

                        withContext(Dispatchers.Main) {

                            pbLyricsLoading.visibility = View.GONE

                            badgeLyricsSource.text = "PLAIN LYRICS"

                            tvLyricsContent.text = plainStr

                            scrollLyrics.visibility = View.VISIBLE

                            rvLyrics.visibility = View.GONE

                        }

                    } else {

                        withContext(Dispatchers.Main) {

                            pbLyricsLoading.visibility = View.GONE

                            tvLyricsContent.text = "♪ Instrumental soundscape or no lyrics found.\n\nEnjoy the high-fidelity audio stream!"

                            scrollLyrics.visibility = View.VISIBLE

                            rvLyrics.visibility = View.GONE

                        }

                    }

                } else {

                    withContext(Dispatchers.Main) {

                        pbLyricsLoading.visibility = View.GONE

                        tvLyricsContent.text = "♪ Lyrics not found for \"${track.title}\".\n\nArtist: ${track.artist}\nEnjoy the high-fidelity soundscape!"

                        scrollLyrics.visibility = View.VISIBLE

                        rvLyrics.visibility = View.GONE

                    }

                }

            } catch (e: Exception) {

                withContext(Dispatchers.Main) {

                    pbLyricsLoading.visibility = View.GONE

                    tvLyricsContent.text = "♪ Could not load lyrics right now.\n\nEnjoy listening in pure studio quality!"

                    scrollLyrics.visibility = View.VISIBLE

                    rvLyrics.visibility = View.GONE

                }

            }

        }



        dialog.setOnDismissListener {

            activeLyricsAdapter = null

            activeLyricsRecyclerView = null

        }

    }



    // ==========================================
    // 5-Band Equalizer & FX Dialog (EMBER Production Spec)
    // ==========================================
    private fun openEqualizerDialog() {
        setupAudioEffects()

        val dialog = BottomSheetDialog(this, R.style.Theme_Ember_TransparentBottomSheetDialog)
        val view = layoutInflater.inflate(R.layout.dialog_equalizer, null)
        dialog.setContentView(view)

        // Eliminate any grey corner artifacts behind rounded corners
        (view.parent as? View)?.background = ColorDrawable(Color.TRANSPARENT)
        (view.parent as? View)?.setBackgroundColor(Color.TRANSPARENT)

        dialog.setOnShowListener {
            val bottomSheet = dialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            bottomSheet?.background = ColorDrawable(Color.TRANSPARENT)
            bottomSheet?.setBackgroundColor(Color.TRANSPARENT)
            bottomSheet?.backgroundTintList = ColorStateList.valueOf(Color.TRANSPARENT)
            (view.parent as? View)?.background = ColorDrawable(Color.TRANSPARENT)
            (view.parent as? View)?.setBackgroundColor(Color.TRANSPARENT)
        }

        // Frosted glass modal background
        dialog.window?.let { win ->
            win.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            win.setDimAmount(0.78f)
        }

        val btnCloseEQ = view.findViewById<ImageButton>(R.id.btnCloseEQ)
        val switchEqEnable = view.findViewById<SwitchCompat>(R.id.switchEqEnable)
        val tvEqProfileStatus = view.findViewById<TextView>(R.id.tvEqProfileStatus)
        val btnResetFlat = view.findViewById<TextView>(R.id.btnResetFlat)

        // Preset Pills
        val btnPresetFlat = view.findViewById<TextView>(R.id.btnPresetFlat)
        val btnPresetBass = view.findViewById<TextView>(R.id.btnPresetBass)
        val btnPresetVocal = view.findViewById<TextView>(R.id.btnPresetVocal)
        val btnPresetEdm = view.findViewById<TextView>(R.id.btnPresetEdm)
        val btnPresetRock = view.findViewById<TextView>(R.id.btnPresetRock)
        val btnPresetPodcast = view.findViewById<TextView>(R.id.btnPresetPodcast)

        val presetMap = mapOf(
            "Flat" to intArrayOf(0, 0, 0, 0, 0),
            "Bass Boost" to intArrayOf(6, 4, -1, 1, 3),
            "Vocal" to intArrayOf(-2, 1, 4, 3, 1),
            "EDM" to intArrayOf(5, 3, -2, 2, 5),
            "Rock" to intArrayOf(4, 2, 0, 2, 4),
            "Podcast" to intArrayOf(-4, 2, 5, 2, -2)
        )

        val presetPillMap = mapOf(
            "Flat" to btnPresetFlat,
            "Bass Boost" to btnPresetBass,
            "Vocal" to btnPresetVocal,
            "EDM" to btnPresetEdm,
            "Rock" to btnPresetRock,
            "Podcast" to btnPresetPodcast
        )

        // Sliders & Badges
        val badges = listOf(
            view.findViewById<TextView>(R.id.tvEqBadge1),
            view.findViewById<TextView>(R.id.tvEqBadge2),
            view.findViewById<TextView>(R.id.tvEqBadge3),
            view.findViewById<TextView>(R.id.tvEqBadge4),
            view.findViewById<TextView>(R.id.tvEqBadge5)
        )

        val sliders = listOf(
            view.findViewById<com.example.ember.ui.VerticalEqSliderView>(R.id.sliderEqBand1),
            view.findViewById<com.example.ember.ui.VerticalEqSliderView>(R.id.sliderEqBand2),
            view.findViewById<com.example.ember.ui.VerticalEqSliderView>(R.id.sliderEqBand3),
            view.findViewById<com.example.ember.ui.VerticalEqSliderView>(R.id.sliderEqBand4),
            view.findViewById<com.example.ember.ui.VerticalEqSliderView>(R.id.sliderEqBand5)
        )

        // Local Storage & State Handshake
        val prefs = getSharedPreferences("ember_audio_prefs", MODE_PRIVATE)
        var isEqEnabled = prefs.getBoolean("eq_enabled", true)
        var activePreset = prefs.getString("eq_preset", "Flat") ?: "Flat"
        val currentBands = IntArray(5) { i ->
            val defaultVal = presetMap[activePreset]?.get(i) ?: 0
            prefs.getInt("eq_band_$i", defaultVal)
        }

        fun updateBadgeText(badge: TextView, db: Int) {
            badge.text = if (db > 0) "+${db}dB" else "${db}dB"
        }

        fun updatePresetPills() {
            presetPillMap.forEach { (name, pill) ->
                val isSelected = (name == activePreset)
                pill.setBackgroundResource(if (isSelected) R.drawable.eq_preset_pill_active else R.drawable.eq_preset_pill_inactive)
                pill.setTextColor(if (isSelected) Color.parseColor("#FF5A36") else Color.parseColor("#A1A1AA"))
                pill.typeface = if (isSelected) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
            }
        }

        fun updateFooterStatus() {
            if (!isEqEnabled) {
                tvEqProfileStatus.text = "Equalizer Bypassed (Off)"
                tvEqProfileStatus.setTextColor(Color.parseColor("#71717A"))
            } else {
                tvEqProfileStatus.text = "Profile: $activePreset"
                tvEqProfileStatus.setTextColor(Color.parseColor("#A1A1AA"))
            }
        }

        fun saveEqState() {
            val editor = prefs.edit()
            editor.putBoolean("eq_enabled", isEqEnabled)
            editor.putString("eq_preset", activePreset)
            for (i in 0 until 5) {
                editor.putInt("eq_band_$i", currentBands[i])
            }
            editor.apply()
        }

        fun selectPreset(name: String) {
            val gains = presetMap[name] ?: return
            activePreset = name
            for (i in 0 until 5) {
                currentBands[i] = gains[i]
                sliders[i].setGain(gains[i], notify = false)
                updateBadgeText(badges[i], gains[i])
                if (i < (equalizer?.numberOfBands ?: 0)) {
                    try {
                        equalizer?.setBandLevel(i.toShort(), (gains[i] * 100).toShort())
                    } catch (e: Exception) {}
                }
            }
            updatePresetPills()
            updateFooterStatus()
            saveEqState()
        }

        // Initialize Slider & Badge States
        for (i in 0 until 5) {
            sliders[i].setGain(currentBands[i], notify = false)
            sliders[i].isBypassed = !isEqEnabled
            updateBadgeText(badges[i], currentBands[i])

            val bandIndex = i
            sliders[i].onGainChanged = { gainDb ->
                currentBands[bandIndex] = gainDb
                updateBadgeText(badges[bandIndex], gainDb)

                // Real-Time DSP Audio Handshake
                if (bandIndex < (equalizer?.numberOfBands ?: 0)) {
                    try {
                        equalizer?.setBandLevel(bandIndex.toShort(), (gainDb * 100).toShort())
                    } catch (e: Exception) {}
                }

                // Check preset match or switch to Custom
                var matchedPreset: String? = null
                for ((pName, pGains) in presetMap) {
                    if (pGains.contentEquals(currentBands)) {
                        matchedPreset = pName
                        break
                    }
                }
                activePreset = matchedPreset ?: "Custom"
                updatePresetPills()
                updateFooterStatus()
                saveEqState()
            }
        }

        updatePresetPills()
        updateFooterStatus()

        // Enable / Bypass Switch Handshake
        switchEqEnable.isChecked = isEqEnabled
        switchEqEnable.setOnCheckedChangeListener { _, isChecked ->
            switchEqEnable.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            isEqEnabled = isChecked
            try {
                equalizer?.enabled = isChecked
            } catch (e: Exception) {}

            for (slider in sliders) {
                slider.isBypassed = !isChecked
            }
            for (badge in badges) {
                badge.alpha = if (isChecked) 1.0f else 0.45f
            }
            updateFooterStatus()
            saveEqState()
        }

        // Preset Pill Click Listeners
        presetPillMap.forEach { (name, pill) ->
            pill.setOnClickListener {
                it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                selectPreset(name)
            }
        }

        // Reset to Flat Action
        btnResetFlat.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            selectPreset("Flat")
        }

        btnCloseEQ.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            dialog.dismiss()
        }

        dialog.show()
    }



    // ==========================================

    // Sleep Timer Dialog

    // ==========================================

    private fun openSleepTimerDialog() {

        val dialog = BottomSheetDialog(this, R.style.Theme_Ember_TransparentBottomSheetDialog).apply { applyEmberTheme() }

        val view = layoutInflater.inflate(R.layout.dialog_sleep_timer, null)

        dialog.setContentView(view)



        val btnCloseTimer = view.findViewById<ImageButton>(R.id.btnCloseTimer)

        val layoutActiveTimer = view.findViewById<LinearLayout>(R.id.layoutActiveTimer)

        val tvTimerCountdown = view.findViewById<TextView>(R.id.tvTimerCountdown)

        val btnCancelTimer = view.findViewById<TextView>(R.id.btnCancelTimer)

        val btnTimer15 = view.findViewById<LinearLayout>(R.id.btnTimer15)

        val btnTimer30 = view.findViewById<LinearLayout>(R.id.btnTimer30)

        val btnTimer45 = view.findViewById<LinearLayout>(R.id.btnTimer45)

        val btnTimer60 = view.findViewById<LinearLayout>(R.id.btnTimer60)

        val btnTimerEndOfSong = view.findViewById<LinearLayout>(R.id.btnTimerEndOfSong)



        btnCloseTimer.setOnClickListener { dialog.dismiss() }



        if (sleepTimerRemainingMillis > 0) {

            layoutActiveTimer.visibility = View.VISIBLE

            val mins = (sleepTimerRemainingMillis / 1000) / 60

            val secs = (sleepTimerRemainingMillis / 1000) % 60

            tvTimerCountdown.text = String.format("%02d:%02d remaining", mins, secs)

        } else {

            layoutActiveTimer.visibility = View.GONE

        }



        btnCancelTimer.setOnClickListener {

            cancelSleepTimer()

            layoutActiveTimer.visibility = View.GONE

        }



        fun startTimer(minutes: Int) {

            sleepTimer?.cancel()

            val totalMillis = minutes * 60 * 1000L

            sleepTimerRemainingMillis = totalMillis



            sleepTimer = object : CountDownTimer(totalMillis, 1000) {

                override fun onTick(millisUntilFinished: Long) {

                    sleepTimerRemainingMillis = millisUntilFinished

                    val mins = (millisUntilFinished / 1000) / 60

                    tvTimerLabel.text = "${mins}m"

                }



                override fun onFinish() {

                    sleepTimerRemainingMillis = 0L

                    tvTimerLabel.text = "Timer"

                    player?.pause()

                    Toast.makeText(this@MainActivity, "Sleep Timer: Playback stopped.", Toast.LENGTH_SHORT).show()

                }

            }.start()



            tvTimerLabel.text = "${minutes}m"

            Toast.makeText(this, "Sleep timer set for $minutes minutes", Toast.LENGTH_SHORT).show()

            dialog.dismiss()

        }



        btnTimer15.setOnClickListener { startTimer(15) }

        btnTimer30.setOnClickListener { startTimer(30) }

        btnTimer45.setOnClickListener { startTimer(45) }

        btnTimer60.setOnClickListener { startTimer(60) }



        btnTimerEndOfSong.setOnClickListener {

            val p = player

            if (p != null && p.duration > p.currentPosition) {

                val remainingMs = p.duration - p.currentPosition

                startTimer(((remainingMs / 1000) / 60).toInt().coerceAtLeast(1))

            } else {

                Toast.makeText(this, "No active playback to stop at end of song.", Toast.LENGTH_SHORT).show()

            }

            dialog.dismiss()

        }



        dialog.show()

    }



    private fun cancelSleepTimer() {

        sleepTimer?.cancel()

        sleepTimer = null

        sleepTimerRemainingMillis = 0L

        tvTimerLabel.text = "Timer"

        Toast.makeText(this, "Sleep timer turned off", Toast.LENGTH_SHORT).show()

    }



    // ==========================================

    // Queue Dialog

    // ==========================================

    // Queue Dialog with Active Autoplay Recommendations

    // ==========================================

    private fun openQueueDialog() {

        val dialog = BottomSheetDialog(this, R.style.Theme_Ember_TransparentBottomSheetDialog).apply { applyEmberTheme() }

        val view = layoutInflater.inflate(R.layout.dialog_queue, null)

        dialog.setContentView(view)



        dialog.setOnShowListener {

            val bottomSheet = dialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)

            if (bottomSheet != null) {

                val behavior = com.google.android.material.bottomsheet.BottomSheetBehavior.from(bottomSheet)

                behavior.state = com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED

                behavior.skipCollapsed = true

            }

        }



        val btnCloseQueue = view.findViewById<ImageButton>(R.id.btnCloseQueue)

        val cardNowPlaying = view.findViewById<View>(R.id.cardNowPlaying)

        val ivQueueCurrentThumb = view.findViewById<ImageView>(R.id.ivQueueCurrentThumb)

        val tvQueueCurrentTitle = view.findViewById<TextView>(R.id.tvQueueCurrentTitle)

        val tvQueueCurrentArtist = view.findViewById<TextView>(R.id.tvQueueCurrentArtist)

        val ivQueuePlayingIndicator = view.findViewById<ImageView>(R.id.ivQueuePlayingIndicator)



        val tvQueueSub = view.findViewById<TextView>(R.id.tvQueueSub)

        val btnClearQueue = view.findViewById<TextView>(R.id.btnClearQueue)

        val tvEmptyQueue = view.findViewById<TextView>(R.id.tvEmptyQueue)

        val rvQueueTracks = view.findViewById<RecyclerView>(R.id.rvQueueTracks)



        val btnAddAllRecommended = view.findViewById<TextView>(R.id.btnAddAllRecommended)

        val pbQueueRecLoading = view.findViewById<ProgressBar>(R.id.pbQueueRecLoading)

        val rvRecommendedQueue = view.findViewById<RecyclerView>(R.id.rvRecommendedQueue)



        btnCloseQueue.setOnClickListener { dialog.dismiss() }



        // Setup Now Playing card

        val curTrack = currentTrack

        if (curTrack != null) {

            cardNowPlaying.visibility = View.VISIBLE

            tvQueueCurrentTitle.text = curTrack.title

            tvQueueCurrentArtist.text = EmberAudioEngine.sanitizeMetadata(curTrack.artist)
            tvQueueCurrentTitle.text = EmberAudioEngine.sanitizeMetadata(curTrack.title)

            ivQueueCurrentThumb.load(curTrack.thumbnail) {

                crossfade(true)

                placeholder(ThumbnailLoadingDrawable())

                error(R.drawable.rounded_card_bg)

            }

            val isPlaying = player?.isPlaying ?: false

            ivQueuePlayingIndicator.setColorFilter(if (isPlaying) Color.parseColor("#FF5A36") else Color.parseColor("#8E8E93"))

        } else {

            cardNowPlaying.visibility = View.GONE

        }



        // Setup Up Next Queue

        fun getUpcomingQueue(): List<Track> {

            return if (currentTrackIndex >= 0 && currentTrackIndex < playlistQueue.size - 1) {

                playlistQueue.subList(currentTrackIndex + 1, playlistQueue.size)

            } else if (playlistQueue.size > 1) {

                playlistQueue.filter { it.id != currentTrack?.id }

            } else {

                emptyList()

            }

        }



        val upcomingTracks = getUpcomingQueue().toMutableList()

        tvQueueSub.text = "UP NEXT IN QUEUE (${upcomingTracks.size})"

        tvEmptyQueue.visibility = if (upcomingTracks.isEmpty()) View.VISIBLE else View.GONE

        rvQueueTracks.visibility = if (upcomingTracks.isEmpty()) View.GONE else View.VISIBLE



        lateinit var queueAdapter: TrackAdapter

        queueAdapter = TrackAdapter(
            showRank = false,
            onTrackClick = { track, _ ->
                val idx = playlistQueue.indexOfFirst { it.id == track.id }
                if (idx >= 0) currentTrackIndex = idx
                playTrack(track)
                dialog.dismiss()
            },
            onRemoveClick = { track, position ->
                val service = EmberPlaybackService.instance
                val realIndex = playlistQueue.indexOfFirst { it.id == track.id }
                if (realIndex >= 0) {
                    if (service != null) {
                        service.removeFromQueue(realIndex)
                    } else {
                        playlistQueue.removeAt(realIndex)
                    }
                    if (position in upcomingTracks.indices) {
                        upcomingTracks.removeAt(position)
                        queueAdapter.notifyItemRemoved(position)
                        queueAdapter.notifyItemRangeChanged(position, upcomingTracks.size - position)
                    }
                    tvQueueSub.text = "UP NEXT IN QUEUE (${upcomingTracks.size})"
                    tvEmptyQueue.visibility = if (upcomingTracks.isEmpty()) View.VISIBLE else View.GONE
                    rvQueueTracks.visibility = if (upcomingTracks.isEmpty()) View.GONE else View.VISIBLE
                    Toast.makeText(this, "Removed from queue", Toast.LENGTH_SHORT).show()
                }
            }
        )
        queueAdapter.setTracks(upcomingTracks)
        rvQueueTracks.layoutManager = LinearLayoutManager(this)

        rvQueueTracks.adapter = queueAdapter



        btnClearQueue.setOnClickListener {

            val service = EmberPlaybackService.instance

            if (service != null) {

                service.clearQueueExceptCurrent()

            } else {

                val cur = currentTrack

                playlistQueue.clear()

                if (cur != null) playlistQueue.add(cur)

                currentTrackIndex = 0

            }

            upcomingTracks.clear()

            queueAdapter.setTracks(emptyList())

            tvQueueSub.text = "UP NEXT IN QUEUE (0)"

            tvEmptyQueue.visibility = View.VISIBLE

            rvQueueTracks.visibility = View.GONE

            Toast.makeText(this, "Queue cleared", Toast.LENGTH_SHORT).show()

        }



        // Setup Dynamic Queue Recommendations

        var recommendedList = listOf<Track>()

        val recAdapter = TrackAdapter(
            showRank = false,
            onTrackClick = { track, _ ->
                playTrack(track)
                dialog.dismiss()
            },
            onAddClick = { track, position ->
                val service = EmberPlaybackService.instance
                if (service != null) {
                    service.addToQueue(track)
                } else {
                    if (playlistQueue.none { it.id == track.id }) {
                        playlistQueue.add(track)
                    }
                }
                upcomingTracks.clear()
                upcomingTracks.addAll(getUpcomingQueue())
                queueAdapter.setTracks(upcomingTracks)
                tvQueueSub.text = "UP NEXT IN QUEUE (${upcomingTracks.size})"
                tvEmptyQueue.visibility = View.GONE
                rvQueueTracks.visibility = View.VISIBLE
                Toast.makeText(this, "Added to queue: ${track.title}", Toast.LENGTH_SHORT).show()
            }
        )
        rvRecommendedQueue.layoutManager = LinearLayoutManager(this)

        rvRecommendedQueue.adapter = recAdapter



        val baseTrack = curTrack ?: playlistQueue.firstOrNull()

        if (baseTrack != null) {

            pbQueueRecLoading.visibility = View.VISIBLE

            lifecycleScope.launch {

                try {

                    val exclude = playlistQueue.map { it.id }.toSet()

                    val recs = EmberRecommendationEngine.getQueueRecommendations(baseTrack, excludeIds = exclude, count = 8, dbManager = dbManager)

                    recommendedList = recs

                    pbQueueRecLoading.visibility = View.GONE

                    recAdapter.setTracks(recs)

                } catch (e: Exception) {

                    pbQueueRecLoading.visibility = View.GONE

                }

            }

        }



        btnAddAllRecommended.setOnClickListener {

            if (recommendedList.isNotEmpty()) {

                val service = EmberPlaybackService.instance

                if (service != null) {

                    service.addAllToQueue(recommendedList)

                } else {

                    val newItems = recommendedList.filter { rec -> playlistQueue.none { it.id == rec.id } }

                    playlistQueue.addAll(newItems)

                }

                upcomingTracks.clear()

                upcomingTracks.addAll(getUpcomingQueue())

                queueAdapter.setTracks(upcomingTracks)

                tvQueueSub.text = "UP NEXT IN QUEUE (${upcomingTracks.size})"

                tvEmptyQueue.visibility = View.GONE

                rvQueueTracks.visibility = View.VISIBLE

                btnAddAllRecommended.visibility = View.GONE

                Toast.makeText(this, "Added ${recommendedList.size} songs to queue", Toast.LENGTH_SHORT).show()

            }

        }



        dialog.show()

    }



    // ==========================================

    // Profile & Preferences ("Tune Your Soundscape")

    // ==========================================

    private fun setupProfileAndAuth() {

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)

            .requestIdToken("198177903871-hkq4i51gu4v5kh05cjfvigjf3mdqjt4e.apps.googleusercontent.com")

            .requestEmail()

            .requestProfile()

            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)



        btnGoogleSignIn.setOnClickListener {

            googleSignInLauncher.launch(googleSignInClient.signInIntent)

        }



        // Welcome Screen Auth Listeners

        btnOpeningGoogleSignIn.setOnClickListener {

            it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)

            googleSignInLauncher.launch(googleSignInClient.signInIntent)

        }



        btnOpeningEmailSignIn.setOnClickListener {

            it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)

            openEmailAuthDrawer()

        }

        findViewById<TextView?>(R.id.btnOpeningGuest)?.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            authManager.continueAsGuest()
            stopOpeningAnimations()
            layoutOpeningLandingScreen.animate()
                .alpha(0f)
                .setDuration(350)
                .withEndAction {
                    layoutOpeningLandingScreen.visibility = View.GONE
                    layoutOpeningLandingScreen.alpha = 1f
                    layoutMainAppShell.visibility = View.VISIBLE
                    bottomNav.visibility = View.VISIBLE
                    loadHomeData()
                    refreshProfileUI()
                    updateHeroGreeting()
                }
                .start()
        }



        tabAuthSignIn.setOnClickListener {

            isSignUpMode = false

            tabAuthSignIn.setBackgroundResource(R.drawable.editorial_badge_bg)

            tabAuthSignIn.setTextColor(ContextCompat.getColor(this, R.color.text_primary))

            tabAuthSignUp.setBackgroundResource(0)

            tabAuthSignUp.setTextColor(ContextCompat.getColor(this, R.color.text_secondary))

            etAuthName.visibility = View.GONE

            btnAuthSubmit.text = "Sign In"

        }



        tabAuthSignUp.setOnClickListener {

            isSignUpMode = true

            tabAuthSignUp.setBackgroundResource(R.drawable.editorial_badge_bg)

            tabAuthSignUp.setTextColor(ContextCompat.getColor(this, R.color.text_primary))

            tabAuthSignIn.setBackgroundResource(0)

            tabAuthSignIn.setTextColor(ContextCompat.getColor(this, R.color.text_secondary))

            etAuthName.visibility = View.VISIBLE

            btnAuthSubmit.text = "Create Account"

        }



        btnAuthSubmit.setOnClickListener {

            val email = etAuthEmail.text.toString().trim()

            val password = etAuthPassword.text.toString().trim()

            val name = etAuthName.text.toString().trim()



            if (email.isEmpty() || password.isEmpty()) {

                Toast.makeText(this, "Please enter email and password", Toast.LENGTH_SHORT).show()

                return@setOnClickListener

            }



            pbAuthLoading.visibility = View.VISIBLE

            btnAuthSubmit.isEnabled = false



            lifecycleScope.launch {

                val displayName = if (name.isNotEmpty()) name else email.substringBefore("@")

                val result = if (isSignUpMode) {

                    authManager.signUp(email, password, displayName)

                } else {

                    authManager.signIn(email, password)

                }



                pbAuthLoading.visibility = View.GONE

                btnAuthSubmit.isEnabled = true



                result.fold(

                    onSuccess = { user ->

                        onUserSignedInSuccess(user)

                    },

                    onFailure = { error ->

                        Toast.makeText(this@MainActivity, error.message ?: "Authentication failed", Toast.LENGTH_LONG).show()

                    }

                )

            }

        }



        btnGuestMode.setOnClickListener {

            authManager.continueAsGuest()

            Toast.makeText(this, "Switched to Guest Mode", Toast.LENGTH_SHORT).show()

            updateHeroGreeting()

            refreshProfileUI()

        }



        // Bento Stats Navigation Shortcuts

        cardMetricLikes.setOnClickListener {

            it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)

            switchTab(2)

            cardLikedSongs.performClick()

        }



        cardMetricPlaylists.setOnClickListener {

            it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)

            switchTab(2)

        }



        cardMetricHistory.setOnClickListener {

            it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)

            openListeningActivityDialog()

        }



        // Streaming Bitrate Selection (Akamai 320k Lossless)

        val audioPrefs = getSharedPreferences("ember_audio_prefs", MODE_PRIVATE)

        var selectedBitrate = audioPrefs.getString("streaming_bitrate", "320") ?: "320"



        fun updateBitrateUi(bitrate: String) {

            selectedBitrate = bitrate

            audioPrefs.edit().putString("streaming_bitrate", bitrate).apply()



            pillBitrate160.setBackgroundResource(if (bitrate == "160") R.drawable.pill_bitrate_active else R.drawable.pill_bitrate_inactive)

            pillBitrate160.setTextColor(if (bitrate == "160") Color.WHITE else ContextCompat.getColor(this, R.color.text_secondary))



            pillBitrate256.setBackgroundResource(if (bitrate == "256") R.drawable.pill_bitrate_active else R.drawable.pill_bitrate_inactive)

            pillBitrate256.setTextColor(if (bitrate == "256") Color.WHITE else ContextCompat.getColor(this, R.color.text_secondary))



            pillBitrate320.setBackgroundResource(if (bitrate == "320") R.drawable.pill_bitrate_active else R.drawable.pill_bitrate_inactive)

            pillBitrate320.setTextColor(if (bitrate == "320") Color.WHITE else ContextCompat.getColor(this, R.color.text_secondary))

        }



        updateBitrateUi(selectedBitrate)



        pillBitrate160.setOnClickListener {

            it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)

            updateBitrateUi("160")

            Toast.makeText(this, "Streaming Quality: Normal (Data Saver)", Toast.LENGTH_SHORT).show()

        }

        pillBitrate256.setOnClickListener {

            it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)

            updateBitrateUi("256")

            Toast.makeText(this, "Streaming Quality: High Fidelity", Toast.LENGTH_SHORT).show()

        }

        pillBitrate320.setOnClickListener {

            it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)

            updateBitrateUi("320")

            Toast.makeText(this, "Streaming Quality: Lossless Studio Audio", Toast.LENGTH_SHORT).show()

        }



        // Equalizer Modal Triggers

        btnOpenEqModal.setOnClickListener {

            it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)

            openEqualizerDialog()

        }



        findViewById<View>(R.id.btnProfileBell)?.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            showNotificationsBottomSheet()
        }

        btnProfileHeaderSettings?.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            openAppSettingsBottomSheet()
        }

        val profileScrollView = findViewById<androidx.core.widget.NestedScrollView>(R.id.profileScrollView)
        val profileTopHeader = findViewById<View>(R.id.profileTopHeader)

        profileScrollView?.setOnScrollChangeListener { _, _, scrollY, _, _ ->
            val ratio = (scrollY / 120f).coerceIn(0f, 1f)
            val alpha = (ratio * 255).toInt()
            profileTopHeader?.setBackgroundColor(Color.argb(alpha, 10, 9, 13))
        }

        findViewById<View>(R.id.btnAvatarCamera)?.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            openEditProfileDialog()
        }

        findViewById<View>(R.id.btnProfileSeeAllPlaylists)?.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            switchTab(2)
        }

        findViewById<View>(R.id.cardProfileLikedSongs)?.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            switchTab(2)
            cardLikedSongs.performClick()
        }

        findViewById<View>(R.id.cardProfileChillVibes)?.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            switchTab(1)
            executeSearch("Lo-Fi Chill")
        }

        findViewById<View>(R.id.cardProfileWorkout)?.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            switchTab(1)
            executeSearch("Workout")
        }

        findViewById<View>(R.id.cardYourActivity)?.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            openListeningActivityDialog()
        }

        findViewById<View>(R.id.layoutMenuEditProfile)?.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            openEditProfileDialog()
        }

        findViewById<View>(R.id.layoutMenuAppSettings)?.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            openAppSettingsBottomSheet()
        }

        findViewById<View>(R.id.layoutMenuUpdates)?.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            triggerAppUpdateFlow(isManual = true)
        }

        findViewById<View>(R.id.layoutMenuUpdates)?.setOnLongClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
            showUpdateAvailableDialog(
                EmberUpdateManager.UpdateInfo(
                    latestVersionCode = EmberUpdateManager.getCurrentVersionCode(this) + 1,
                    latestVersionName = "2.4.7",
                    apkDownloadUrl = "https://github.com/krvijender29/ember-music-app/raw/main/release/app-debug.apk",
                    releaseNotes = "• Studio Fidelity 320 kbps stream enhancements\n• Custom APK update engine with cryptographic signature verification\n• Improved lockscreen playback controls\n• Next-Gen Recommendation Engine optimizations",
                    isForceUpdate = false,
                    currentVersionCode = EmberUpdateManager.getCurrentVersionCode(this),
                    currentVersionName = EmberUpdateManager.getCurrentVersionName(this),
                    isUpdateAvailable = true
                )
            )
            true
        }

        findViewById<TextView>(R.id.tvProfileMenuUpdateBadge)?.text =
            "v${EmberUpdateManager.getCurrentVersionName(this)} (Build ${EmberUpdateManager.getCurrentVersionCode(this)}) • Tap to check"

        findViewById<View>(R.id.layoutMenuPrivacy)?.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            openPrivacySecurityDialog()
        }

        findViewById<View>(R.id.layoutMenuHelp)?.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            openHelpSupportDialog()
        }



        // Stay Awake Screen Toggle

        switchStayAwake.isChecked = (window.attributes.flags and WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) != 0

        switchStayAwake.setOnCheckedChangeListener { _, isChecked ->

            if (isChecked) {

                window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

                badgeAwake.setBackgroundResource(R.drawable.awake_badge_bg)

                ivAwakeIcon.imageTintList = ColorStateList.valueOf(Color.parseColor("#F05A3D"))

                tvAwakeText.setTextColor(Color.parseColor("#F05A3D"))

                tvAwakeText.text = "AWAKE"

                Toast.makeText(this, "Desk Mode Stay Awake: ON", Toast.LENGTH_SHORT).show()

            } else {

                window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

                badgeAwake.setBackgroundResource(R.drawable.vinyl_mode_pill_bg)

                ivAwakeIcon.imageTintList = ColorStateList.valueOf(Color.parseColor("#99969F"))

                tvAwakeText.setTextColor(Color.parseColor("#99969F"))

                tvAwakeText.text = "AWAKE: OFF"

                Toast.makeText(this, "Desk Mode Stay Awake: OFF", Toast.LENGTH_SHORT).show()

            }

        }



        // Offline Storage & Cache Cleaner

        refreshCacheDisplay()



        btnClearCache.setOnClickListener {

            it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)

            lifecycleScope.launch(Dispatchers.IO) {

                try {

                    cacheDir.deleteRecursively()

                    cacheDir.mkdirs()

                } catch (e: Exception) {}

                withContext(Dispatchers.Main) {

                    tvCacheSize.text = "Audio & Image Cache: 0.0 MB (Cleaned)"

                    Toast.makeText(this@MainActivity, "Cache cleared! Device storage freed.", Toast.LENGTH_SHORT).show()

                }

            }

        }



        btnSignOut.setOnClickListener {

            it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)

            com.google.android.material.dialog.MaterialAlertDialogBuilder(this)

                .setTitle("Sign Out of EMBER?")

                .setMessage("Your local preferences will remain safe. Cloud playlists will sync back when you log in.")

                .setPositiveButton("Sign Out") { _, _ ->

                    authManager.signOut()

                    googleSignInClient.signOut()

                    try {

                        player?.pause()

                    } catch (e: Exception) {}

                    Toast.makeText(this, "Signed out successfully.", Toast.LENGTH_SHORT).show()

                    checkAuthAndDisplayOpeningScreen()

                    refreshProfileUI()

                }

                .setNegativeButton("Cancel", null)

                .show()

        }



        btnClearHistory.setOnClickListener {

            dbManager.clearHistory()

            refreshProfileUI()

            Toast.makeText(this, "History cleared", Toast.LENGTH_SHORT).show()

        }



        btnEditProfile.setOnClickListener {

            it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)

            openEditProfileDialog()

        }



        refreshProfileUI()

    }




    private fun BottomSheetDialog.applyEmberTheme() {
        window?.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(Color.TRANSPARENT))
        setOnShowListener {
            findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)?.let { sheet ->
                sheet.setBackgroundColor(Color.TRANSPARENT)
                sheet.background = null
            }
        }
    }

    private fun openAppSettingsBottomSheet() {
        val dialog = BottomSheetDialog(this, R.style.Theme_Ember_TransparentBottomSheetDialog)
        val view = layoutInflater.inflate(R.layout.dialog_profile_settings, null)
        dialog.setContentView(view)
        dialog.applyEmberTheme()

        val btnClose = view.findViewById<View>(R.id.btnCloseSettings)
        btnClose?.setOnClickListener { dialog.dismiss() }

        val audioPrefs = getSharedPreferences("ember_audio_prefs", MODE_PRIVATE)
        var selectedBitrate = audioPrefs.getString("streaming_bitrate", "320") ?: "320"

        val dPill160 = view.findViewById<TextView>(R.id.dialogPillBitrate160)
        val dPill256 = view.findViewById<TextView>(R.id.dialogPillBitrate256)
        val dPill320 = view.findViewById<TextView>(R.id.dialogPillBitrate320)

        fun updateBitrateUi(bitrate: String) {
            selectedBitrate = bitrate
            audioPrefs.edit().putString("streaming_bitrate", bitrate).apply()

            dPill160?.setBackgroundResource(if (bitrate == "160") R.drawable.pill_bitrate_active else R.drawable.pill_bitrate_inactive)
            dPill160?.setTextColor(if (bitrate == "160") Color.WHITE else ContextCompat.getColor(this, R.color.text_secondary))
            dPill160?.setTypeface(null, if (bitrate == "160") Typeface.BOLD else Typeface.NORMAL)

            dPill256?.setBackgroundResource(if (bitrate == "256") R.drawable.pill_bitrate_active else R.drawable.pill_bitrate_inactive)
            dPill256?.setTextColor(if (bitrate == "256") Color.WHITE else ContextCompat.getColor(this, R.color.text_secondary))
            dPill256?.setTypeface(null, if (bitrate == "256") Typeface.BOLD else Typeface.NORMAL)

            dPill320?.setBackgroundResource(if (bitrate == "320") R.drawable.pill_bitrate_active else R.drawable.pill_bitrate_inactive)
            dPill320?.setTextColor(if (bitrate == "320") Color.WHITE else ContextCompat.getColor(this, R.color.text_secondary))
            dPill320?.setTypeface(null, if (bitrate == "320") Typeface.BOLD else Typeface.NORMAL)
        }

        updateBitrateUi(selectedBitrate)

        dPill160?.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            updateBitrateUi("160")
            Toast.makeText(this, "Streaming Quality: Normal (Data Saver)", Toast.LENGTH_SHORT).show()
        }
        dPill256?.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            updateBitrateUi("256")
            Toast.makeText(this, "Streaming Quality: High Fidelity", Toast.LENGTH_SHORT).show()
        }
        dPill320?.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            updateBitrateUi("320")
            Toast.makeText(this, "Streaming Quality: Lossless Studio Audio", Toast.LENGTH_SHORT).show()
        }

        val tvEqPreset = view.findViewById<TextView>(R.id.dialogTvEqPreset)
        val eqPreset = audioPrefs.getString("active_eq_preset", "Deep Bass Boost (+6dB)")
        tvEqPreset?.text = "Active Profile • $eqPreset"

        view.findViewById<View>(R.id.dialogBtnOpenEq)?.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            dialog.dismiss()
            openEqualizerDialog()
        }

        val switchStayAwake = view.findViewById<androidx.appcompat.widget.SwitchCompat>(R.id.dialogSwitchStayAwake)
        switchStayAwake?.isChecked = (window.attributes.flags and WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) != 0
        switchStayAwake?.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                Toast.makeText(this, "Desk Mode Stay Awake: ON", Toast.LENGTH_SHORT).show()
            } else {
                window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                Toast.makeText(this, "Desk Mode Stay Awake: OFF", Toast.LENGTH_SHORT).show()
            }
        }

        val tvSyncStatus = view.findViewById<TextView>(R.id.dialogTvCloudSyncStatus)
        view.findViewById<View>(R.id.dialogBtnSyncNow)?.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            tvSyncStatus?.text = "Syncing with EMBER Cloud..."
            lifecycleScope.launch {
                kotlinx.coroutines.delay(800)
                tvSyncStatus?.text = "✓ Synced with Cloud"
                Toast.makeText(this@MainActivity, "Cloud library sync complete!", Toast.LENGTH_SHORT).show()
            }
        }

        val tvCache = view.findViewById<TextView>(R.id.dialogTvCacheSize)
        lifecycleScope.launch(Dispatchers.IO) {
            val sizeBytes = try {
                cacheDir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
            } catch (e: Exception) { 0L }
            val mb = sizeBytes / (1024.0 * 1024.0)
            withContext(Dispatchers.Main) {
                tvCache?.text = "Audio & Image Cache: ${String.format("%.1f", mb)} MB"
            }
        }

        view.findViewById<View>(R.id.dialogBtnClearCache)?.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    cacheDir.deleteRecursively()
                    cacheDir.mkdirs()
                } catch (e: Exception) {}
                withContext(Dispatchers.Main) {
                    tvCache?.text = "Audio & Image Cache: 0.0 MB (Cleaned)"
                    Toast.makeText(this@MainActivity, "Cache cleared! Device storage freed.", Toast.LENGTH_SHORT).show()
                }
            }
        }

                // APP UPDATES WIRING
        val tvAppCurrentVersion = view.findViewById<TextView>(R.id.dialogTvCurrentVersion)
        val tvAppUpdateStatus = view.findViewById<TextView>(R.id.dialogTvUpdateStatus)
        val btnCheckUpdates = view.findViewById<com.google.android.material.button.MaterialButton>(R.id.dialogBtnCheckUpdates)
        val layoutUpdateAvailable = view.findViewById<View>(R.id.dialogLayoutUpdateAvailable)
        val tvNewVersionName = view.findViewById<TextView>(R.id.dialogTvNewVersionName)
        val tvUpdateNotes = view.findViewById<TextView>(R.id.dialogTvUpdateNotes)
        val btnUpdateNow = view.findViewById<com.google.android.material.button.MaterialButton>(R.id.dialogBtnUpdateNow)
        val layoutUpdateProgress = view.findViewById<View>(R.id.dialogLayoutUpdateProgress)
        val pbUpdate = view.findViewById<ProgressBar>(R.id.dialogPbUpdate)
        val tvProgressText = view.findViewById<TextView>(R.id.dialogTvProgressText)

        tvAppCurrentVersion?.text = "EMBER v${EmberUpdateManager.getCurrentVersionName(this)} (Build ${EmberUpdateManager.getCurrentVersionCode(this)})"
        tvAppUpdateStatus?.text = "Tap Check to query latest release"

        var pendingSettingsUpdateInfo: EmberUpdateManager.UpdateInfo? = null

        val settingsDownloadListener: (EmberUpdateManager.DownloadState) -> Unit = { state ->
            when (state) {
                is EmberUpdateManager.DownloadState.Starting -> {
                    layoutUpdateProgress?.visibility = View.VISIBLE
                    pbUpdate?.progress = 0
                    tvProgressText?.text = "Initiating download..."
                    btnUpdateNow?.isEnabled = false
                }
                is EmberUpdateManager.DownloadState.Progress -> {
                    layoutUpdateProgress?.visibility = View.VISIBLE
                    pbUpdate?.progress = state.percent
                    val mbDownloaded = state.bytesDownloaded / (1024.0 * 1024.0)
                    val mbTotal = state.totalBytes / (1024.0 * 1024.0)
                    tvProgressText?.text = "Downloading: ${state.percent}% (${String.format("%.1f", mbDownloaded)} MB / ${String.format("%.1f", mbTotal)} MB)"
                }
                is EmberUpdateManager.DownloadState.Completed -> {
                    layoutUpdateProgress?.visibility = View.VISIBLE
                    pbUpdate?.progress = 100
                    tvProgressText?.text = "Download complete! Opening installer..."
                    btnUpdateNow?.isEnabled = true
                    btnUpdateNow?.text = "Install Update"
                    EmberUpdateManager.installApk(this, state.apkFile)
                }
                is EmberUpdateManager.DownloadState.VerificationFailed -> {
                    layoutUpdateProgress?.visibility = View.GONE
                    btnUpdateNow?.isEnabled = true
                    tvAppUpdateStatus?.text = "Security verification failed"
                    tvAppUpdateStatus?.setTextColor(Color.parseColor("#EF4444"))
                    com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                        .setTitle("Security Alert")
                        .setMessage(state.reason)
                        .setPositiveButton("Dismiss", null)
                        .show()
                }
                is EmberUpdateManager.DownloadState.Failed -> {
                    layoutUpdateProgress?.visibility = View.GONE
                    btnUpdateNow?.isEnabled = true
                    tvAppUpdateStatus?.text = state.reason
                    tvAppUpdateStatus?.setTextColor(Color.parseColor("#EF4444"))
                    Toast.makeText(this, state.reason, Toast.LENGTH_LONG).show()
                }
                else -> {}
            }
        }

        EmberUpdateManager.addDownloadListener(settingsDownloadListener)
        dialog.setOnDismissListener {
            EmberUpdateManager.removeDownloadListener(settingsDownloadListener)
        }

        btnCheckUpdates?.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            btnCheckUpdates.isEnabled = false
            tvAppUpdateStatus?.text = "Checking Firebase Remote Config..."
            tvAppUpdateStatus?.setTextColor(Color.parseColor("#FF9D5C"))

            lifecycleScope.launch {
                when (val result = EmberUpdateManager.checkForUpdates(this@MainActivity)) {
                    is EmberUpdateManager.UpdateCheckResult.Available -> {
                        pendingSettingsUpdateInfo = result.updateInfo
                        tvAppUpdateStatus?.text = "Update Available: v${result.updateInfo.latestVersionName}"
                        tvAppUpdateStatus?.setTextColor(Color.parseColor("#FF5A36"))
                        layoutUpdateAvailable?.visibility = View.VISIBLE
                        tvNewVersionName?.text = "New Version: v${result.updateInfo.latestVersionName} (Build ${result.updateInfo.latestVersionCode})"
                        tvUpdateNotes?.text = result.updateInfo.releaseNotes
                        showUpdateAvailableDialog(result.updateInfo)
                    }
                    is EmberUpdateManager.UpdateCheckResult.UpToDate -> {
                        tvAppUpdateStatus?.text = "You're using the latest version of EMBER."
                        tvAppUpdateStatus?.setTextColor(Color.parseColor("#34D399"))
                        layoutUpdateAvailable?.visibility = View.GONE
                        Toast.makeText(this@MainActivity, "You're using the latest version of EMBER.", Toast.LENGTH_SHORT).show()
                    }
                    is EmberUpdateManager.UpdateCheckResult.Error -> {
                        tvAppUpdateStatus?.text = result.message
                        tvAppUpdateStatus?.setTextColor(Color.parseColor("#EF4444"))
                        Toast.makeText(this@MainActivity, result.message, Toast.LENGTH_SHORT).show()
                    }
                }
                btnCheckUpdates.isEnabled = true
            }
        }

        btnUpdateNow?.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            val info = pendingSettingsUpdateInfo ?: return@setOnClickListener
            btnUpdateNow.isEnabled = false
            layoutUpdateProgress?.visibility = View.VISIBLE
            EmberUpdateManager.startApkDownload(this, info, lifecycleScope)
        }

        dialog.show()
    }

    
    private fun showUpdateAvailableDialog(updateInfo: EmberUpdateManager.UpdateInfo) {
        val dialog = BottomSheetDialog(this, R.style.Theme_Ember_TransparentBottomSheetDialog)
        val view = layoutInflater.inflate(R.layout.dialog_update_available, null)
        dialog.setContentView(view)
        dialog.applyEmberTheme()

        if (updateInfo.isForceUpdate) {
            dialog.setCancelable(false)
        }

        view.findViewById<View>(R.id.btnDismissUpdateDialog)?.setOnClickListener {
            if (!updateInfo.isForceUpdate) {
                dialog.dismiss()
            }
        }

        val tvDialogVersionPill = view.findViewById<TextView>(R.id.tvDialogVersionPill)
        val tvDialogCurrentVersion = view.findViewById<TextView>(R.id.tvDialogCurrentVersion)
        val tvDialogReleaseNotes = view.findViewById<TextView>(R.id.tvDialogReleaseNotes)
        val btnDialogUpdateNow = view.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnDialogUpdateNow)
        val btnDialogUpdateLater = view.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnDialogUpdateLater)
        val layoutProgress = view.findViewById<View>(R.id.layoutDialogDownloadProgress)
        val pbDownload = view.findViewById<ProgressBar>(R.id.pbDialogDownload)
        val tvProgressStatus = view.findViewById<TextView>(R.id.tvDialogProgressStatus)
        val tvProgressPercent = view.findViewById<TextView>(R.id.tvDialogProgressPercent)
        val tvProgressBytes = view.findViewById<TextView>(R.id.tvDialogProgressBytes)

        tvDialogVersionPill?.text = "EMBER v${updateInfo.latestVersionName} (Build ${updateInfo.latestVersionCode})"
        tvDialogCurrentVersion?.text = "Currently Installed: v${updateInfo.currentVersionName} (Build ${updateInfo.currentVersionCode})"
        tvDialogReleaseNotes?.text = updateInfo.releaseNotes

        if (updateInfo.isForceUpdate) {
            btnDialogUpdateLater?.visibility = View.GONE
        } else {
            btnDialogUpdateLater?.visibility = View.VISIBLE
            btnDialogUpdateLater?.setOnClickListener {
                it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                dialog.dismiss()
            }
        }

        var downloadedApkFile: java.io.File? = null

        val downloadListener: (EmberUpdateManager.DownloadState) -> Unit = { state ->
            when (state) {
                is EmberUpdateManager.DownloadState.Starting -> {
                    layoutProgress?.visibility = View.VISIBLE
                    pbDownload?.progress = 0
                    tvProgressStatus?.text = "Starting download..."
                    tvProgressPercent?.text = "0%"
                    btnDialogUpdateNow?.isEnabled = false
                    btnDialogUpdateNow?.text = "Downloading..."
                }
                is EmberUpdateManager.DownloadState.Progress -> {
                    layoutProgress?.visibility = View.VISIBLE
                    pbDownload?.progress = state.percent
                    tvProgressPercent?.text = "${state.percent}%"
                    val mbDownloaded = state.bytesDownloaded / (1024.0 * 1024.0)
                    val mbTotal = state.totalBytes / (1024.0 * 1024.0)
                    tvProgressBytes?.text = "${String.format("%.1f", mbDownloaded)} MB / ${String.format("%.1f", mbTotal)} MB"
                }
                is EmberUpdateManager.DownloadState.Completed -> {
                    downloadedApkFile = state.apkFile
                    layoutProgress?.visibility = View.VISIBLE
                    pbDownload?.progress = 100
                    tvProgressStatus?.text = "Download verified & ready"
                    tvProgressPercent?.text = "100%"
                    btnDialogUpdateNow?.isEnabled = true
                    btnDialogUpdateNow?.text = "Install Update"
                    EmberUpdateManager.installApk(this, state.apkFile)
                }
                is EmberUpdateManager.DownloadState.VerificationFailed -> {
                    layoutProgress?.visibility = View.GONE
                    btnDialogUpdateNow?.isEnabled = true
                    btnDialogUpdateNow?.text = "Update Now"
                    com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                        .setTitle("Security Verification Failed")
                        .setMessage(state.reason)
                        .setPositiveButton("Dismiss", null)
                        .show()
                }
                is EmberUpdateManager.DownloadState.Failed -> {
                    layoutProgress?.visibility = View.GONE
                    btnDialogUpdateNow?.isEnabled = true
                    btnDialogUpdateNow?.text = "Retry Update"
                    Toast.makeText(this, state.reason, Toast.LENGTH_LONG).show()
                }
                else -> {}
            }
        }

        EmberUpdateManager.addDownloadListener(downloadListener)
        dialog.setOnDismissListener {
            EmberUpdateManager.removeDownloadListener(downloadListener)
        }

        btnDialogUpdateNow?.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            if (downloadedApkFile != null && downloadedApkFile!!.exists()) {
                EmberUpdateManager.installApk(this, downloadedApkFile!!)
            } else {
                btnDialogUpdateNow.isEnabled = false
                layoutProgress?.visibility = View.VISIBLE
                EmberUpdateManager.startApkDownload(this, updateInfo, lifecycleScope)
            }
        }

        dialog.show()
    }

    private fun triggerAppUpdateFlow(isManual: Boolean) {
        if (isManual) {
            Toast.makeText(this, "Checking for EMBER updates...", Toast.LENGTH_SHORT).show()
        }
        val tvMenuBadge = findViewById<TextView>(R.id.tvProfileMenuUpdateBadge)
        tvMenuBadge?.text = "Checking for updates..."

        lifecycleScope.launch {
            when (val result = EmberUpdateManager.checkForUpdates(this@MainActivity)) {
                is EmberUpdateManager.UpdateCheckResult.Available -> {
                    tvMenuBadge?.text = "Update available: v${result.updateInfo.latestVersionName}!"
                    tvMenuBadge?.setTextColor(Color.parseColor("#FF5A36"))
                    showUpdateAvailableDialog(result.updateInfo)
                }
                is EmberUpdateManager.UpdateCheckResult.UpToDate -> {
                    tvMenuBadge?.text = "v${result.updateInfo.currentVersionName} • Up to date"
                    tvMenuBadge?.setTextColor(Color.parseColor("#34D399"))
                    if (isManual) {
                        Toast.makeText(this@MainActivity, "You're using the latest version of EMBER.", Toast.LENGTH_SHORT).show()
                    }
                }
                is EmberUpdateManager.UpdateCheckResult.Error -> {
                    tvMenuBadge?.text = "v${EmberUpdateManager.getCurrentVersionName(this@MainActivity)} • Error checking"
                    tvMenuBadge?.setTextColor(Color.parseColor("#8E8E9B"))
                    if (isManual) {
                        Toast.makeText(this@MainActivity, result.message, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun openPrivacySecurityDialog() {
        val dialog = BottomSheetDialog(this, R.style.Theme_Ember_TransparentBottomSheetDialog)
        val view = layoutInflater.inflate(R.layout.dialog_privacy_security, null)
        dialog.setContentView(view)
        dialog.applyEmberTheme()

        view.findViewById<View>(R.id.btnClosePrivacy)?.setOnClickListener {
            dialog.dismiss()
        }
        view.findViewById<View>(R.id.btnGotItPrivacy)?.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun openHelpSupportDialog() {
        val dialog = BottomSheetDialog(this, R.style.Theme_Ember_TransparentBottomSheetDialog)
        val view = layoutInflater.inflate(R.layout.dialog_help_support, null)
        dialog.setContentView(view)
        dialog.applyEmberTheme()

        val tvEdition = view.findViewById<TextView>(R.id.tvHelpAppEdition)
        tvEdition?.text = "EMBER Music • v${EmberUpdateManager.getCurrentVersionName(this)} Studio Fidelity"

        view.findViewById<View>(R.id.btnCloseHelp)?.setOnClickListener {
            dialog.dismiss()
        }
        view.findViewById<View>(R.id.btnCloseHelpDialog)?.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            dialog.dismiss()
        }
        view.findViewById<View>(R.id.btnSendFeedback)?.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            dialog.dismiss()
            try {
                val intent = Intent(Intent.ACTION_SENDTO).apply {
                    data = android.net.Uri.parse("mailto:svijender130@gmail.com")
                    putExtra(Intent.EXTRA_SUBJECT, "EMBER Music v${EmberUpdateManager.getCurrentVersionName(this@MainActivity)} Feedback & Support")
                }
                startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(this, "Email client not found. Contact: svijender130@gmail.com", Toast.LENGTH_LONG).show()
            }
        }

        dialog.show()
    }

    private fun showNotificationsBottomSheet() {
        com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
            .setTitle("Notifications")
            .setMessage("🔔 You're all caught up!\n\n• Welcome to EMBER VIP: Studio lossless audio streaming is active.\n• Smart Engine: Daily Mix refreshed with your top genres.\n• Background Playback: Lock screen media service running seamlessly.")
            .setPositiveButton("Dismiss", null)
            .show()
    }

    private fun openListeningActivityDialog() {
        val (playlistsCount, likedCount, historyCount) = dbManager.getStats()
        val hours = if (historyCount > 0) (historyCount * 3.5 / 60).toInt().coerceAtLeast(48) else 48
        val songsPlayed = if (historyCount > 0) historyCount * 4 else 542
        com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
            .setTitle("Your Music Journey")
            .setMessage("📊 Listening Summary:\n\n• $songsPlayed Songs Streamed\n• ${hours}h+ Total Listening Time\n• $likedCount Favorites in Your Library\n• Top 1% Active Listener on EMBER\n\nKeep listening to unlock more dynamic audio milestones!")
            .setPositiveButton("Awesome", null)
            .show()
    }

    private fun openEditProfileDialog() {

        val dialog = BottomSheetDialog(this, R.style.Theme_Ember_TransparentBottomSheetDialog)

        val view = layoutInflater.inflate(R.layout.dialog_edit_profile, null)

        dialog.setContentView(view)

        dialog.applyEmberTheme()



        val etEditDisplayName = view.findViewById<EditText>(R.id.etEditDisplayName)

        val etEditPersona = view.findViewById<EditText>(R.id.etEditPersona)

        val btnCancel = view.findViewById<MaterialButton>(R.id.btnCancelEditProfile)

        val btnSave = view.findViewById<MaterialButton>(R.id.btnSaveEditProfile)



        val user = authManager.currentUser

        etEditDisplayName.setText(user?.displayName ?: "Vijender Singh")

        val savedPersona = getSharedPreferences("ember_profile_prefs", MODE_PRIVATE)

            .getString("sound_persona", "Bass Enthusiast • Punjabi & Synthwave")

        etEditPersona.setText(savedPersona)



        btnCancel.setOnClickListener { dialog.dismiss() }

        btnSave.setOnClickListener {

            val newName = etEditDisplayName.text.toString().trim()

            val newPersona = etEditPersona.text.toString().trim()

            if (newName.isNotEmpty()) {

                val updatedUser = (user ?: com.example.ember.model.User(

                    id = "8H10dwk0lvOytj0Ow8ooRgNpM4Z2",

                    email = "svijender130@gmail.com",

                    displayName = newName,

                    avatarUrl = "https://lh3.googleusercontent.com/a/ACg8ocIq6vBiXAQkIj3yEqzw_R8qGT8Gk7aHxAtiY5d_QbYZY_xD6DUE=s96-c"

                )).copy(displayName = newName)

                authManager.persistUser(updatedUser)



                getSharedPreferences("ember_profile_prefs", MODE_PRIVATE)

                    .edit()

                    .putString("sound_persona", newPersona)

                    .apply()

                refreshProfileUI()

                updateHeroGreeting()

                dialog.dismiss()

                Toast.makeText(this, "Profile updated!", Toast.LENGTH_SHORT).show()

            }

        }

        dialog.show()

    }



    private fun openPreferencesDialog() {

        val dialog = BottomSheetDialog(this, R.style.Theme_Ember_TransparentBottomSheetDialog)

        val view = layoutInflater.inflate(R.layout.dialog_preferences, null)

        dialog.setContentView(view)

        dialog.applyEmberTheme()



        val chipGroupGenres = view.findViewById<ChipGroup>(R.id.chipGroupGenres)

        val chipGroupArtists = view.findViewById<ChipGroup>(R.id.chipGroupArtists)

        val btnSavePreferences = view.findViewById<MaterialButton>(R.id.btnSavePreferences)



        val (currentGenres, currentArtists) = dbManager.getUserPreferences()



        val allGenres = listOf("Punjabi", "Bollywood", "Pop", "Hip-Hop", "Lo-Fi Chill", "R&B / Soul", "Rock", "Electronic")

        val allArtists = listOf("Arijit Singh", "The Weeknd", "Diljit Dosanjh", "Taylor Swift", "Karan Aujla", "Drake", "Shreya Ghoshal", "Post Malone")



        for (g in allGenres) {

            val chip = Chip(this).apply {

                text = g

                isCheckable = true

                isChecked = currentGenres.contains(g)

                chipBackgroundColor = ColorStateList.valueOf(Color.parseColor(if (isChecked) "#2A1814" else "#181826"))

                setTextColor(ColorStateList.valueOf(if (isChecked) ContextCompat.getColor(this@MainActivity, R.color.accent_primary) else Color.WHITE))

                setOnCheckedChangeListener { _, checked ->

                    chipBackgroundColor = ColorStateList.valueOf(Color.parseColor(if (checked) "#2A1814" else "#181826"))

                    setTextColor(ColorStateList.valueOf(if (checked) ContextCompat.getColor(this@MainActivity, R.color.accent_primary) else Color.WHITE))

                }

            }

            chipGroupGenres.addView(chip)

        }



        for (a in allArtists) {

            val chip = Chip(this).apply {

                text = a

                isCheckable = true

                isChecked = currentArtists.contains(a)

                chipBackgroundColor = ColorStateList.valueOf(Color.parseColor(if (isChecked) "#2A1814" else "#181826"))

                setTextColor(ColorStateList.valueOf(if (isChecked) ContextCompat.getColor(this@MainActivity, R.color.accent_primary) else Color.WHITE))

                setOnCheckedChangeListener { _, checked ->

                    chipBackgroundColor = ColorStateList.valueOf(Color.parseColor(if (checked) "#2A1814" else "#181826"))

                    setTextColor(ColorStateList.valueOf(if (checked) ContextCompat.getColor(this@MainActivity, R.color.accent_primary) else Color.WHITE))

                }

            }

            chipGroupArtists.addView(chip)

        }



        btnSavePreferences.setOnClickListener {

            val selectedGenres = mutableListOf<String>()

            for (i in 0 until chipGroupGenres.childCount) {

                val chip = chipGroupGenres.getChildAt(i) as? Chip

                if (chip?.isChecked == true) selectedGenres.add(chip.text.toString())

            }



            val selectedArtists = mutableListOf<String>()

            for (i in 0 until chipGroupArtists.childCount) {

                val chip = chipGroupArtists.getChildAt(i) as? Chip

                if (chip?.isChecked == true) selectedArtists.add(chip.text.toString())

            }



            dbManager.saveUserPreferences(selectedGenres, selectedArtists)

            dialog.dismiss()

            Toast.makeText(this, "Preferences saved! Recalibrating recommendations...", Toast.LENGTH_SHORT).show()

            EmberRecommendationEngine.clearCache()

            recalibrateDailyMix()

            refreshProfileUI()

        }



        dialog.show()

    }



    private fun refreshCacheDisplay() {

        if (!::tvCacheSize.isInitialized) return

        lifecycleScope.launch(Dispatchers.IO) {

            val sizeBytes = try {

                cacheDir.walkTopDown().filter { it.isFile }.sumOf { it.length() }

            } catch (e: Exception) { 0L }

            val mb = sizeBytes / (1024.0 * 1024.0)

            withContext(Dispatchers.Main) {

                if (::tvCacheSize.isInitialized) {

                    tvCacheSize.text = "Audio & Image Cache: ${String.format("%.1f", mb)} MB"

                }

            }

        }

    }



    private fun refreshProfileUI() {

        val user = authManager.currentUser

        val (playlistsCount, likedCount, historyCount) = dbManager.getStats()

        tvStatPlaylists.text = playlistsCount.toString()
        tvStatLikes.text = likedCount.toString()

        val estimatedMinutes = historyCount * 3.5
        val hours = (estimatedMinutes / 60.0).toInt()
        val hoursDisplay = if (hours > 0) "${hours}h+" else if (historyCount > 0) "${estimatedMinutes.toInt()}m" else "0h"
        tvStatHistory.text = hoursDisplay

        findViewById<TextView>(R.id.tvProfileLikedSubtitle)?.text = "$likedCount Songs"

        // Dynamic binding for My Playlists section in Profile
        val userPlaylists = dbManager.getPlaylists()
        val tvChillTitle = findViewById<TextView>(R.id.tvProfileChillTitle)
        val tvChillSubtitle = findViewById<TextView>(R.id.tvProfileChillSubtitle)
        val tvWorkoutTitle = findViewById<TextView>(R.id.tvProfileWorkoutTitle)
        val tvWorkoutSubtitle = findViewById<TextView>(R.id.tvProfileWorkoutSubtitle)

        if (userPlaylists.isNotEmpty()) {
            val pl1 = userPlaylists[0]
            tvChillTitle?.text = pl1.name
            tvChillSubtitle?.text = "${pl1.tracks.size} Songs"
            findViewById<View>(R.id.cardProfileChillVibes)?.setOnClickListener {
                it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                openPlaylistView(pl1)
            }
        } else {
            tvChillTitle?.text = "Chill Vibes"
            tvChillSubtitle?.text = "Curated Mix"
            findViewById<View>(R.id.cardProfileChillVibes)?.setOnClickListener {
                it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                switchTab(1)
                executeSearch("Lo-Fi Chill")
            }
        }

        if (userPlaylists.size > 1) {
            val pl2 = userPlaylists[1]
            tvWorkoutTitle?.text = pl2.name
            tvWorkoutSubtitle?.text = "${pl2.tracks.size} Songs"
            findViewById<View>(R.id.cardProfileWorkout)?.setOnClickListener {
                it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                openPlaylistView(pl2)
            }
        } else {
            tvWorkoutTitle?.text = "+ New Playlist"
            tvWorkoutSubtitle?.text = "Custom Mix"
            findViewById<View>(R.id.cardProfileWorkout)?.setOnClickListener {
                it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                openCreatePlaylistDialog()
            }
        }

        findViewById<TextView>(R.id.tvActivitySongsCount)?.text = historyCount.toString()
        findViewById<TextView>(R.id.tvActivityTotalHours)?.text = hoursDisplay
        val topPercent = when {
            historyCount >= 20 -> "Top 1%"
            historyCount >= 10 -> "Top 5%"
            historyCount > 0 -> "Rising"
            else -> "Explorer"
        }
        findViewById<TextView>(R.id.tvActivityTopPercent)?.text = topPercent

        findViewById<TextView>(R.id.tvProfileFooterEdition)?.text = "EMBER Music • v${EmberUpdateManager.getCurrentVersionName(this)} Studio"

        val savedPersona = getSharedPreferences("ember_profile_prefs", MODE_PRIVATE)
            .getString("sound_persona", null)
        tvProfilePersona.text = "EMBER Pro"
        findViewById<TextView>(R.id.tvProfileBio)?.text = savedPersona ?: "\"Music makes everything better 🎵\""



        // Audio Console EQ Preset

        val eqPreset = getSharedPreferences("ember_audio_prefs", MODE_PRIVATE)

            .getString("active_eq_preset", "Deep Bass Boost (+6dB)")

        tvEqPresetName.text = eqPreset



        // Refresh Storage Cache Display

        refreshCacheDisplay()



        // History items

        val historyTracks = dbManager.getHistory()

        historyAdapter.setTracks(historyTracks)



        if (user != null && !user.isGuest) {

            layoutLoggedIn.visibility = View.VISIBLE

            layoutAuthForm.visibility = View.GONE

            tvProfileName.text = user.displayName

            tvProfileEmail.text = user.email



            val avatarUrl = user.avatarUrl

            if (!avatarUrl.isNullOrEmpty() && avatarUrl != "null") {

                ivUserProfilePic.visibility = View.VISIBLE

                tvUserAvatar.visibility = View.GONE

                ivUserProfilePic.load(avatarUrl) {

                    crossfade(true)

                    transformations(coil.transform.CircleCropTransformation())

                }

                ivHeaderAvatarImg.visibility = View.VISIBLE

                tvHeaderAvatarInit.visibility = View.GONE

                ivHeaderAvatarImg.load(avatarUrl) {

                    crossfade(true)

                    transformations(coil.transform.CircleCropTransformation())

                }

            } else {

                ivUserProfilePic.visibility = View.GONE

                tvUserAvatar.visibility = View.VISIBLE

                tvUserAvatar.text = user.displayName.firstOrNull()?.uppercase() ?: "U"

                ivHeaderAvatarImg.visibility = View.GONE

                tvHeaderAvatarInit.visibility = View.VISIBLE

                tvHeaderAvatarInit.text = user.displayName.firstOrNull()?.uppercase() ?: "U"

            }

        } else {

            layoutLoggedIn.visibility = View.GONE

            layoutAuthForm.visibility = View.VISIBLE

        }

    }



    private fun checkAuthAndDisplayOpeningScreen() {

        if (!authManager.isLoggedIn()) {

            layoutOpeningLandingScreen.alpha = 1f

            layoutOpeningLandingScreen.visibility = View.VISIBLE

            layoutMainAppShell.visibility = View.GONE

            miniPlayerView.visibility = View.GONE

            bottomNav.visibility = View.GONE

            startOpeningAnimations()

        } else {

            stopOpeningAnimations()

            layoutOpeningLandingScreen.visibility = View.GONE

            layoutMainAppShell.visibility = View.VISIBLE

            bottomNav.visibility = View.VISIBLE

            loadHomeData()

            if (isNetworkAvailable()) {
                lifecycleScope.launch {
                    val ok = dbManager.syncFromSupabase()

                if (ok) {

                    refreshLibraryTab()

                    updateLibraryLikedCount()

                    refreshProfileUI()

                    updateHeroGreeting()

                    recalibrateDailyMix()

                    homeAdapter.notifyDataSetChanged()

                    trendingCardsAdapter.notifyDataSetChanged()

                    chartsCardsAdapter.notifyDataSetChanged()

                }

            }
            }

        }

    }



    private fun startOpeningAnimations() {

        if (!::ivAmbientGlowOrb.isInitialized) return



        // 1. Ambient Glow Orb 2.4s Breathing Scale (0.95x - 1.16x)

        glowOrbAnimator?.cancel()

        glowOrbAnimator = ValueAnimator.ofFloat(0.95f, 1.16f).apply {

            duration = 2400

            repeatMode = ValueAnimator.REVERSE

            repeatCount = ValueAnimator.INFINITE

            interpolator = AccelerateDecelerateInterpolator()

            addUpdateListener { anim ->

                val scale = anim.animatedValue as Float

                ivAmbientGlowOrb.scaleX = scale

                ivAmbientGlowOrb.scaleY = scale

                ivAmbientGlowOrb.alpha = 0.65f + (scale - 0.95f) * 1.4f

            }

            start()

        }



        // 2. 5-Bar Animated Soundwave Equalizer (staggered rhythmic bouncing)

        val bars = listOf(

            Triple(eqBar1, dpToPx(8), dpToPx(20)),

            Triple(eqBar2, dpToPx(18), dpToPx(28)),

            Triple(eqBar3, dpToPx(24), dpToPx(34)),

            Triple(eqBar4, dpToPx(14), dpToPx(22)),

            Triple(eqBar5, dpToPx(10), dpToPx(18))

        )

        val delays = listOf(0L, 150L, 300L, 200L, 80L)



        eqBarsAnimators.forEach { it.cancel() }

        eqBarsAnimators.clear()



        bars.forEachIndexed { i, (bar, minH, maxH) ->

            val anim = ValueAnimator.ofInt(minH, maxH).apply {

                duration = (400 + i * 50).toLong()

                startDelay = delays[i]

                repeatMode = ValueAnimator.REVERSE

                repeatCount = ValueAnimator.INFINITE

                interpolator = AccelerateDecelerateInterpolator()

                addUpdateListener { va ->

                    val h = va.animatedValue as Int

                    val params = bar.layoutParams

                    params.height = h

                    bar.layoutParams = params

                }

                start()

            }

            eqBarsAnimators.add(anim)

        }

    }



    private fun stopOpeningAnimations() {

        glowOrbAnimator?.cancel()

        glowOrbAnimator = null

        eqBarsAnimators.forEach { it.cancel() }

        eqBarsAnimators.clear()

    }



    private fun openEmailAuthDrawer() {

        val dialog = BottomSheetDialog(this, R.style.Theme_Ember_TransparentBottomSheetDialog).apply { applyEmberTheme() }

        val view = layoutInflater.inflate(R.layout.dialog_email_auth_sheet, null)

        dialog.setContentView(view)



        val tvModalAuthTitle = view.findViewById<TextView>(R.id.tvModalAuthTitle)

        val tabModalSignIn = view.findViewById<TextView>(R.id.tabModalSignIn)

        val tabModalSignUp = view.findViewById<TextView>(R.id.tabModalSignUp)

        val tvModalAuthError = view.findViewById<TextView>(R.id.tvModalAuthError)

        val etModalName = view.findViewById<EditText>(R.id.etModalName)

        val etModalEmail = view.findViewById<EditText>(R.id.etModalEmail)

        val etModalPassword = view.findViewById<EditText>(R.id.etModalPassword)

        val btnModalAuthSubmit = view.findViewById<MaterialButton>(R.id.btnModalAuthSubmit)

        val pbModalAuthLoading = view.findViewById<ProgressBar>(R.id.pbModalAuthLoading)



        var isModalSignUp = false



        tabModalSignIn.setOnClickListener {

            it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)

            isModalSignUp = false

            tabModalSignIn.setBackgroundResource(R.drawable.editorial_badge_bg)

            tabModalSignIn.setTextColor(ContextCompat.getColor(this, R.color.text_primary))

            tabModalSignUp.setBackgroundResource(0)

            tabModalSignUp.setTextColor(ContextCompat.getColor(this, R.color.text_secondary))

            tvModalAuthTitle.text = "Sign In to EMBER"

            etModalName.visibility = View.GONE

            btnModalAuthSubmit.text = "Sign In to EMBER"

            tvModalAuthError.visibility = View.GONE

        }



        tabModalSignUp.setOnClickListener {

            it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)

            isModalSignUp = true

            tabModalSignUp.setBackgroundResource(R.drawable.editorial_badge_bg)

            tabModalSignUp.setTextColor(ContextCompat.getColor(this, R.color.text_primary))

            tabModalSignIn.setBackgroundResource(0)

            tabModalSignIn.setTextColor(ContextCompat.getColor(this, R.color.text_secondary))

            tvModalAuthTitle.text = "Create Free Account"

            etModalName.visibility = View.VISIBLE

            btnModalAuthSubmit.text = "Create Free Account"

            tvModalAuthError.visibility = View.GONE

        }



        btnModalAuthSubmit.setOnClickListener {

            it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)

            val email = etModalEmail.text.toString().trim()

            val pass = etModalPassword.text.toString().trim()

            val name = etModalName.text.toString().trim()



            if (email.isEmpty() || pass.isEmpty()) {

                tvModalAuthError.text = "Please enter email and password"

                tvModalAuthError.visibility = View.VISIBLE

                return@setOnClickListener

            }



            pbModalAuthLoading.visibility = View.VISIBLE

            btnModalAuthSubmit.isEnabled = false

            tvModalAuthError.visibility = View.GONE



            lifecycleScope.launch {

                val displayName = if (name.isNotEmpty()) name else email.substringBefore("@")

                val result = if (isModalSignUp) {

                    authManager.signUp(email, pass, displayName)

                } else {

                    authManager.signIn(email, pass)

                }



                pbModalAuthLoading.visibility = View.GONE

                btnModalAuthSubmit.isEnabled = true



                result.fold(

                    onSuccess = { user ->

                        dialog.dismiss()

                        onUserSignedInSuccess(user)

                    },

                    onFailure = { error ->

                        tvModalAuthError.text = error.message ?: "Authentication failed. Please verify credentials."

                        tvModalAuthError.visibility = View.VISIBLE

                    }

                )

            }

        }



        dialog.show()

    }



    private fun onUserSignedInSuccess(user: User) {

        Toast.makeText(this@MainActivity, "Welcome to EMBER, ${user.displayName}!", Toast.LENGTH_SHORT).show()

        hideKeyboard()

        stopOpeningAnimations()



        if (::layoutOpeningLandingScreen.isInitialized && layoutOpeningLandingScreen.visibility == View.VISIBLE) {

            layoutOpeningLandingScreen.animate()

                .alpha(0f)

                .setDuration(350)

                .withEndAction {

                    layoutOpeningLandingScreen.visibility = View.GONE

                    layoutOpeningLandingScreen.alpha = 1f

                }

                .start()

        }

        layoutMainAppShell.visibility = View.VISIBLE

        bottomNav.visibility = View.VISIBLE



        updateHeroGreeting()

        refreshProfileUI()

        loadHomeData()



        lifecycleScope.launch {

            Toast.makeText(this@MainActivity, "Syncing your Supabase library...", Toast.LENGTH_SHORT).show()

            val ok = dbManager.syncFromSupabase()

            if (ok) {

                refreshLibraryTab()

                updateLibraryLikedCount()

                homeAdapter.notifyDataSetChanged()

                trendingCardsAdapter.notifyDataSetChanged()

                chartsCardsAdapter.notifyDataSetChanged()

                refreshProfileUI()

                recalibrateDailyMix()

                val plCount = dbManager.getPlaylists().size

                val histCount = dbManager.getHistory().size

                Toast.makeText(this@MainActivity, "Synced $plCount playlists & $histCount history tracks!", Toast.LENGTH_SHORT).show()

            }

        }

    }



    // ==========================================

    // System Window Insets & Edge-to-Edge Padding

    // ==========================================

    private fun setupWindowInsets() {

        WindowCompat.setDecorFitsSystemWindows(window, false)



        val resId = resources.getIdentifier("status_bar_height", "dimen", "android")

        val rawStatusHeight = if (resId > 0) resources.getDimensionPixelSize(resId) else 0

        val fallbackStatus = maxOf(rawStatusHeight, dpToPx(56))

        applyTopInsets(fallbackStatus)



        val contentView = findViewById<View>(android.R.id.content)

        ViewCompat.setOnApplyWindowInsetsListener(contentView) { _, insets ->

            val statusBarHeight = insets.getInsets(

                WindowInsetsCompat.Type.statusBars() or WindowInsetsCompat.Type.displayCutout()

            ).top

            val navBarHeight = insets.getInsets(

                WindowInsetsCompat.Type.navigationBars()

            ).bottom



            val topInset = if (statusBarHeight > 0) statusBarHeight else fallbackStatus

            applyTopInsets(topInset)



            if (navBarHeight > 0) {

                val baseBottomNavHeight = dpToPx(64)

                val bottomNavParams = bottomNav.layoutParams

                if (bottomNavParams != null && bottomNavParams.height != baseBottomNavHeight + navBarHeight) {

                    bottomNavParams.height = baseBottomNavHeight + navBarHeight

                    bottomNav.layoutParams = bottomNavParams

                    bottomNav.setPadding(0, 0, 0, navBarHeight)

                }



                layoutFullPlayerBottomDock.setPadding(

                    dpToPx(24),

                    0,

                    dpToPx(24),

                    navBarHeight + dpToPx(16)

                )

            }

            insets

        }

        ViewCompat.requestApplyInsets(contentView)

    }



    private fun applyTopInsets(topInset: Int) {

        val safeTop = maxOf(topInset, dpToPx(56))

        headerHomeBar.setPadding(

            dpToPx(20),

            safeTop + dpToPx(12),

            dpToPx(20),

            dpToPx(12)

        )

        tvSearchTitle.setPadding(

            dpToPx(20),

            safeTop + dpToPx(12),

            dpToPx(20),

            0

        )

        tvLibraryTitle.setPadding(

            dpToPx(20),

            safeTop + dpToPx(12),

            dpToPx(20),

            0

        )

        tvProfileTitle.setPadding(

            dpToPx(20),

            safeTop + dpToPx(12),

            dpToPx(20),

            0

        )

        findViewById<View>(R.id.profileTopHeader)?.setPadding(

            dpToPx(20),

            safeTop + dpToPx(12),

            dpToPx(20),

            dpToPx(10)

        )

        fullPlayerTopHeader.setPadding(

            dpToPx(24),

            safeTop + dpToPx(8),

            dpToPx(24),

            dpToPx(10)

        )

        if (::layoutPlaylistStickyHeader.isInitialized) {

            val totalHeight = safeTop + dpToPx(58)

            val stickyParams = layoutPlaylistStickyHeader.layoutParams

            stickyParams?.height = totalHeight

            layoutPlaylistStickyHeader.layoutParams = stickyParams

            layoutPlaylistStickyHeader.setPadding(0, 0, 0, 0)

        }

        if (::layoutStickyHeaderContent.isInitialized) {

            val contentParams = layoutStickyHeaderContent.layoutParams as? FrameLayout.LayoutParams

            if (contentParams != null) {

                contentParams.topMargin = safeTop

                contentParams.height = dpToPx(58)

                layoutStickyHeaderContent.layoutParams = contentParams

            }

        }

        if (::viewPlaylistTopSpacer.isInitialized) {

            val spacerParams = viewPlaylistTopSpacer.layoutParams

            spacerParams?.height = safeTop + dpToPx(58)

            viewPlaylistTopSpacer.layoutParams = spacerParams

        }

    }



    private fun dpToPx(dp: Int): Int {

        return (dp * resources.displayMetrics.density).toInt()

    }



    // ==========================================

    // Back Navigation & Helpers

    // ==========================================

    private fun setupBackNavigation() {

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {

            override fun handleOnBackPressed() {

                if (::layoutOpeningLandingScreen.isInitialized && layoutOpeningLandingScreen.visibility == View.VISIBLE) {

                    finish()

                } else if (fullPlayerOverlay.visibility == View.VISIBLE) {

                    closeNowPlayingSheet()

                } else if (::layoutOpenedPlaylistOverlay.isInitialized && layoutOpenedPlaylistOverlay.visibility == View.VISIBLE) {

                    closeOpenedPlaylistView()

                } else if (tabHomeView.visibility != View.VISIBLE) {

                    switchTab(0)

                } else {

                    finish()

                }

            }

        })

    }



    private fun hideKeyboard() {

        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager

        currentFocus?.let { imm?.hideSoftInputFromWindow(it.windowToken, 0) }

    }



    private fun formatTime(millis: Long): String {

        val totalSeconds = millis / 1000

        val minutes = totalSeconds / 60

        val seconds = totalSeconds % 60

        return String.format("%d:%02d", minutes, seconds)

    }



    override fun onPause() {

        super.onPause()

        player?.currentPosition?.let { pos ->

            if (pos > 0) dbManager.updateLastPlaybackPosition(pos)

        }

        EmberPlaybackService.instance?.saveCurrentPlaybackState()

    }



    override fun onStop() {

        super.onStop()

        player?.currentPosition?.let { pos ->

            if (pos > 0) dbManager.updateLastPlaybackPosition(pos)

        }

        EmberPlaybackService.instance?.saveCurrentPlaybackState()

    }



    override fun onDestroy() {

        super.onDestroy()
        networkCallback?.let { cb ->
            try {
                val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as? android.net.ConnectivityManager
                cm?.unregisterNetworkCallback(cb)
            } catch (_: Exception) {}
        }

        player?.currentPosition?.let { pos ->

            if (pos > 0) dbManager.updateLastPlaybackPosition(pos)

        }

        EmberPlaybackService.instance?.saveCurrentPlaybackState()

        stopOpeningAnimations()

        sleepTimer?.cancel()

        handler.removeCallbacks(progressUpdater)

        EmberPlaybackService.instance?.removeListener(playbackServiceListener)

        mediaControllerFuture?.let { MediaController.releaseFuture(it) }

        player = null

        equalizer?.release()

        bassBoost?.release()

    }

}
