package com.example.ember.ui

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.util.TypedValue
import android.widget.TextView

/**
 * ==============================================================================
 * EMBER Music Design Tokens, Typography Hierarchy, and Color System
 * Strictly mirrors the official EMBER Web Architecture & Mobile Design System
 * ==============================================================================
 */
object EmberTheme {

    // ==========================================
    // 1. COLOR PALETTE
    // ==========================================
    object Colors {
        // Canvas & Surfaces
        val BackgroundDefault: Int = Color.parseColor("#090909")      // Obsidian Noir
        val ElevatedSurface: Int = Color.parseColor("#181818")        // Sheets, Menus
        val CardsRowItems: Int = Color.parseColor("#141414")          // Cards & Row Items
        val FrostedGlassSurface: Int = Color.argb((0.06 * 255).toInt(), 255, 255, 255) // rgba(255, 255, 255, 0.06)
        val StickyHeaderMiniPlayer: Int = Color.argb((0.88 * 255).toInt(), 12, 12, 18) // rgba(12, 12, 18, 0.88)

        const val FrostedGlassBlurRadius: Float = 24f // px blur
        const val StickyHeaderBlurRadius: Float = 30f // px blur

        // Typography Tiers
        val TextPrimary: Int = Color.parseColor("#FFFFFF")            // Titles, Headers
        val TextSecondary: Int = Color.parseColor("#A1A1AA")          // Artist, Descriptions
        val TextMuted: Int = Color.parseColor("#71717A")              // Durations, Index Numbers, Inactive Icons
        val TextPlaceholder: Int = Color.parseColor("#52525B")        // Input placeholder

        // Brand Signature Accents
        val AccentPrimary: Int = Color.parseColor("#FF5A36")          // Fiery Coral Amber
        val AccentGradientStart: Int = Color.parseColor("#FF5A36")    // linear-gradient 0%
        val AccentGradientEnd: Int = Color.parseColor("#FF8A45")      // linear-gradient 100%
        val LikedSongsAccent: Int = Color.parseColor("#7928CA")        // Electric Purple
        val AccentSoft: Int = Color.argb((0.14 * 255).toInt(), 255, 90, 54) // rgba(255, 90, 54, 0.14)
        val Destructive: Int = Color.parseColor("#FF453A")            // Delete / Remove red

        // Functional Accents
        val SuccessGreen: Int = Color.parseColor("#34C759")

        // Borders
        val BorderSubtle: Int = Color.argb((0.08 * 255).toInt(), 255, 255, 255) // rgba(255, 255, 255, 0.08)
        val BorderCard: Int = Color.argb((0.10 * 255).toInt(), 255, 255, 255)   // rgba(255, 255, 255, 0.10)
        val BorderAccentActive: Int = Color.argb((0.35 * 255).toInt(), 255, 90, 54) // rgba(255, 90, 54, 0.35)
    }

    // ==========================================
    // 2. CORNER RADII (dp)
    // ==========================================
    object Radii {
        const val BigAlbumCover: Float = 20f       // 20dp squircle
        const val CardCollectionTile: Float = 16f  // 16dp
        const val SongThumbnail: Float = 10f       // 10dp
        const val FilterPill: Float = 999f         // 999dp (Fully rounded capsule)
        const val BottomSheetTop: Float = 24f      // 24dp
        const val ElevatedMenu: Float = 16f        // 16dp
    }

    // ==========================================
    // 3. TYPOGRAPHY SCALE & HIERARCHY
    // ==========================================
    data class TextStyle(
        val sizeSp: Float,
        val weight: Int,            // 400 (Normal), 500 (Medium), 600 (SemiBold), 700 (Bold), 800 (ExtraBold), 900 (Black)
        val color: Int,
        val letterSpacingPx: Float = 0f,
        val isMono: Boolean = false,
        val isUppercase: Boolean = false
    )

    object Typography {
        // 1. Splash Brand: 44sp, Weight 900 (Black), LetterSpacing -1.2px, Color #FFFFFF
        val SplashBrand = TextStyle(
            sizeSp = 44f,
            weight = 900,
            letterSpacingPx = -1.2f,
            color = Colors.TextPrimary
        )

        // 2. Screen Title (H1): 24sp, Weight 800 (Bold), LetterSpacing -0.5px, Color #FFFFFF
        val ScreenTitleH1 = TextStyle(
            sizeSp = 24f,
            weight = 800,
            letterSpacingPx = -0.5f,
            color = Colors.TextPrimary
        )

        // 3. Playlist Hero Title: 22sp, Weight 800 (Bold), Color #FFFFFF
        val PlaylistHeroTitle = TextStyle(
            sizeSp = 22f,
            weight = 800,
            letterSpacingPx = 0f,
            color = Colors.TextPrimary
        )

        // 4. Section Header (H2/H3): 18sp, Weight 700 (Bold), Color #FFFFFF
        val SectionHeader = TextStyle(
            sizeSp = 18f,
            weight = 700,
            letterSpacingPx = 0f,
            color = Colors.TextPrimary
        )

        // 5. Song Title: 15sp, Weight 600, Color #FFFFFF
        val SongTitle = TextStyle(
            sizeSp = 15f,
            weight = 600,
            letterSpacingPx = 0f,
            color = Colors.TextPrimary
        )

        // 5b. Song Title When Playing: Weight 700, Color #FF5A36
        val SongTitlePlaying = TextStyle(
            sizeSp = 15f,
            weight = 700,
            letterSpacingPx = 0f,
            color = Colors.AccentPrimary
        )

        // 6. Search Input: 14sp, Weight 500, Color #FFFFFF
        val SearchInput = TextStyle(
            sizeSp = 14f,
            weight = 500,
            letterSpacingPx = 0f,
            color = Colors.TextPrimary
        )

        // 7. Artist Name: 13sp, Weight 500, Color #A1A1AA
        val ArtistName = TextStyle(
            sizeSp = 13f,
            weight = 500,
            letterSpacingPx = 0f,
            color = Colors.TextSecondary
        )

        // 8. Filter Pill / Button: 12.5sp, Weight 600, Color #FFFFFF
        val FilterPill = TextStyle(
            sizeSp = 12.5f,
            weight = 600,
            letterSpacingPx = 0f,
            color = Colors.TextPrimary
        )

        // 9. Timestamp Duration: 12sp, Mono, Weight 500, Color #71717A
        val TimestampDuration = TextStyle(
            sizeSp = 12f,
            weight = 500,
            letterSpacingPx = 0f,
            isMono = true,
            color = Colors.TextMuted
        )

        // 10. Track Index: 11.5sp, Mono, Weight 600, Color #71717A
        val TrackIndex = TextStyle(
            sizeSp = 11.5f,
            weight = 600,
            letterSpacingPx = 0f,
            isMono = true,
            color = Colors.TextMuted
        )

        // 11. Micro Badges ("ALBUM", "COLLECTION"): 10sp, Mono, Weight 800, Color #FF5A36, LetterSpacing 1.1px (Uppercase)
        val MicroBadge = TextStyle(
            sizeSp = 10f,
            weight = 800,
            letterSpacingPx = 1.1f,
            isMono = true,
            color = Colors.AccentPrimary,
            isUppercase = true
        )
    }

    // ==========================================
    // 4. THEME HELPERS & FACTORIES
    // ==========================================

    /**
     * Applies an official EMBER TextStyle token directly to an Android TextView.
     */
    fun applyTextStyle(textView: TextView, style: TextStyle) {
        textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, style.sizeSp)
        textView.setTextColor(style.color)

        if (style.isMono) {
            val styleFlag = if (style.weight >= 700) Typeface.BOLD else Typeface.NORMAL
            textView.setTypeface(Typeface.MONOSPACE, styleFlag)
        } else {
            val family = when {
                style.weight >= 900 -> "sans-serif-black"
                style.weight >= 700 -> "sans-serif-bold"
                style.weight >= 600 -> "sans-serif-medium"
                else -> "sans-serif"
            }
            val styleFlag = if (style.weight >= 700) Typeface.BOLD else Typeface.NORMAL
            textView.typeface = Typeface.create(family, styleFlag)
        }

        // Letter spacing in Android is in em units (pixels / (fontSize * density))
        if (style.letterSpacingPx != 0f) {
            val emSpacing = style.letterSpacingPx / style.sizeSp
            textView.letterSpacing = emSpacing
        } else {
            textView.letterSpacing = 0f
        }

        if (style.isUppercase) {
            textView.isAllCaps = true
        }
    }

    /**
     * Generates the signature EMBER 135deg Accent Linear Gradient.
     * linear-gradient(135deg, #FF5A36 0%, #FF8A45 100%)
     */
    fun createAccentGradientDrawable(cornerRadiusDp: Float = 0f, context: Context? = null): GradientDrawable {
        return GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            intArrayOf(Colors.AccentGradientStart, Colors.AccentGradientEnd)
        ).apply {
            if (cornerRadiusDp > 0 && context != null) {
                cornerRadius = dpToPx(context, cornerRadiusDp)
            }
        }
    }

    /**
     * Creates an elevated or card background drawable with exact border and fill tokens.
     */
    fun createCardDrawable(
        context: Context,
        isElevated: Boolean = false,
        cornerRadiusDp: Float = Radii.CardCollectionTile,
        hasBorder: Boolean = true
    ): GradientDrawable {
        val fill = if (isElevated) Colors.ElevatedSurface else Colors.CardsRowItems
        val stroke = if (hasBorder) Colors.BorderCard else Color.TRANSPARENT
        val strokeWidthPx = dpToPx(context, 1f).toInt()

        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            setColor(fill)
            cornerRadius = dpToPx(context, cornerRadiusDp)
            if (hasBorder) {
                setStroke(strokeWidthPx, stroke)
            }
        }
    }

    /**
     * Creates a fully-rounded capsule filter pill background.
     */
    fun createCapsulePillDrawable(
        context: Context,
        isActive: Boolean = false
    ): GradientDrawable {
        val fill = if (isActive) Colors.AccentSoft else Colors.CardsRowItems
        val stroke = if (isActive) Colors.BorderAccentActive else Colors.BorderSubtle
        val strokeWidthPx = dpToPx(context, 1f).toInt()

        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            setColor(fill)
            cornerRadius = dpToPx(context, Radii.FilterPill)
            setStroke(strokeWidthPx, stroke)
        }
    }

    /**
     * Converts DP to physical PX based on device density.
     */
    fun dpToPx(context: Context, dp: Float): Float {
        return dp * context.resources.displayMetrics.density
    }
}
