package com.example.ember.adapter

import com.example.ember.util.ThumbnailLoadingDrawable

import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.animation.ValueAnimator
import android.content.res.ColorStateList
import android.graphics.Color
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.ember.R
import com.example.ember.model.Track
import com.example.ember.ui.EmberTheme

class PlaylistDetailTrackAdapter(
    private var allTracks: List<Track> = emptyList(),
    var isCustomPlaylist: Boolean = false,
    var currentPlayingTrackId: String? = null,
    var isPlaying: Boolean = false,
    private val onTrackClick: (track: Track, index: Int, allFilteredTracks: List<Track>) -> Unit,
    private val onLikeClick: (track: Track, isLiked: Boolean) -> Unit,
    private val onOptionsClick: (track: Track, view: View) -> Unit,
    private val onRemoveClick: ((track: Track) -> Unit)? = null
) : RecyclerView.Adapter<PlaylistDetailTrackAdapter.TrackViewHolder>() {

    private var filteredTracks: MutableList<Track> = allTracks.toMutableList()
    private var activeAnimators = mutableListOf<ValueAnimator>()

    fun setTracks(newTracks: List<Track>) {
        allTracks = newTracks
        filteredTracks = newTracks.toMutableList()
        notifyDataSetChanged()
    }

    fun filter(query: String) {
        val q = query.trim().lowercase()
        filteredTracks = if (q.isEmpty()) {
            allTracks.toMutableList()
        } else {
            allTracks.filter {
                it.title.lowercase().contains(q) || it.artist.lowercase().contains(q)
            }.toMutableList()
        }
        notifyDataSetChanged()
    }

    fun getFilteredTracks(): List<Track> = filteredTracks

    fun updatePlaybackState(trackId: String?, playing: Boolean) {
        currentPlayingTrackId = trackId
        isPlaying = playing
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TrackViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_playlist_track_row, parent, false)
        return TrackViewHolder(view)
    }

    override fun getItemCount(): Int = filteredTracks.size

    override fun onBindViewHolder(holder: TrackViewHolder, position: Int) {
        val track = filteredTracks[position]
        holder.bind(track, position)
    }

    inner class TrackViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvTrackNumber: TextView = itemView.findViewById(R.id.tvTrackNumber)
        private val layoutTrackEq: LinearLayout = itemView.findViewById(R.id.layoutTrackEq)
        private val eqBar1: View = itemView.findViewById(R.id.eqBar1)
        private val eqBar2: View = itemView.findViewById(R.id.eqBar2)
        private val eqBar3: View = itemView.findViewById(R.id.eqBar3)
        private val ivTrackThumb: ImageView = itemView.findViewById(R.id.ivTrackThumb)
        private val tvTrackTitle: TextView = itemView.findViewById(R.id.tvTrackTitle)
        private val tvTrackArtist: TextView = itemView.findViewById(R.id.tvTrackArtist)
        private val tvTrackDuration: TextView = itemView.findViewById(R.id.tvTrackDuration)
        private val ivTrackLike: ImageView = itemView.findViewById(R.id.ivTrackLike)
        private val btnTrackRemove: ImageView = itemView.findViewById(R.id.btnTrackRemove)
        private val btnTrackOptions: ImageView = itemView.findViewById(R.id.btnTrackOptions)

        fun bind(track: Track, position: Int) {
            val isActive = track.id == currentPlayingTrackId
            val isActivelyPlaying = isActive && isPlaying

            // 1. Equalizer vs Track Number
            if (isActivelyPlaying) {
                tvTrackNumber.visibility = View.GONE
                layoutTrackEq.visibility = View.VISIBLE
                startEqualizerAnim()
            } else {
                layoutTrackEq.visibility = View.GONE
                tvTrackNumber.visibility = View.VISIBLE
                tvTrackNumber.text = (position + 1).toString().padStart(2, '0')
                tvTrackNumber.setTextColor(if (isActive) EmberTheme.Colors.AccentPrimary else EmberTheme.Colors.TextMuted)
            }

            // 2. Title & Artist
            tvTrackTitle.text = com.example.ember.engine.EmberAudioEngine.sanitizeMetadata(track.title)
            tvTrackArtist.text = com.example.ember.engine.EmberAudioEngine.sanitizeMetadata(track.artist)

            if (isActive) {
                tvTrackTitle.setTextColor(EmberTheme.Colors.AccentPrimary)
            } else {
                tvTrackTitle.setTextColor(EmberTheme.Colors.TextPrimary)
            }

            // 3. Duration
            tvTrackDuration.text = track.duration

            // 4. Thumbnail
            ivTrackThumb.load(track.thumbnail) {
                crossfade(true)
                placeholder(ThumbnailLoadingDrawable(2f, 12f))
                error(R.drawable.rounded_card_bg)
            }

            // 5. Like Heart
            if (track.isLiked) {
                ivTrackLike.setImageResource(R.drawable.ic_heart_filled)
                ivTrackLike.imageTintList = ColorStateList.valueOf(EmberTheme.Colors.Destructive)
            } else {
                ivTrackLike.setImageResource(R.drawable.ic_heart)
                ivTrackLike.imageTintList = ColorStateList.valueOf(EmberTheme.Colors.TextMuted)
            }

            ivTrackLike.setOnClickListener {
                it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                track.isLiked = !track.isLiked

                // Scale bounce
                ivTrackLike.scaleX = 0.8f
                ivTrackLike.scaleY = 0.8f
                ivTrackLike.animate()
                    .scaleX(1.3f)
                    .scaleY(1.3f)
                    .setDuration(120L)
                    .setInterpolator(OvershootInterpolator(1.4f))
                    .withEndAction {
                        ivTrackLike.animate().scaleX(1.0f).scaleY(1.0f).setDuration(90L).start()
                    }
                    .start()

                if (track.isLiked) {
                    ivTrackLike.setImageResource(R.drawable.ic_heart_filled)
                    ivTrackLike.imageTintList = ColorStateList.valueOf(Color.parseColor("#FF3B30"))
                } else {
                    ivTrackLike.setImageResource(R.drawable.ic_heart)
                    ivTrackLike.imageTintList = ColorStateList.valueOf(Color.parseColor("#71717A"))
                }
                onLikeClick(track, track.isLiked)
            }

            // 6. Custom playlist remove button
            if (isCustomPlaylist) {
                btnTrackRemove.visibility = View.VISIBLE
                btnTrackRemove.setOnClickListener {
                    it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    onRemoveClick?.invoke(track)
                }
            } else {
                btnTrackRemove.visibility = View.GONE
            }

            // 7. Options 3-dots
            btnTrackOptions.setOnClickListener {
                it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                onOptionsClick(track, it)
            }

            // 8. Row click to play
            itemView.setOnClickListener {
                it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                onTrackClick(track, position, filteredTracks)
            }
        }

        private fun startEqualizerAnim() {
            val anim1 = ObjectAnimator.ofFloat(eqBar1, "scaleY", 0.3f, 1.0f, 0.4f).apply {
                duration = 320L
                repeatMode = ValueAnimator.REVERSE
                repeatCount = ValueAnimator.INFINITE
                interpolator = AccelerateDecelerateInterpolator()
            }
            val anim2 = ObjectAnimator.ofFloat(eqBar2, "scaleY", 0.4f, 1.0f, 0.2f).apply {
                duration = 420L
                repeatMode = ValueAnimator.REVERSE
                repeatCount = ValueAnimator.INFINITE
                interpolator = AccelerateDecelerateInterpolator()
            }
            val anim3 = ObjectAnimator.ofFloat(eqBar3, "scaleY", 0.2f, 1.0f, 0.5f).apply {
                duration = 360L
                repeatMode = ValueAnimator.REVERSE
                repeatCount = ValueAnimator.INFINITE
                interpolator = AccelerateDecelerateInterpolator()
            }
            anim1.start()
            anim2.start()
            anim3.start()
        }
    }
}
