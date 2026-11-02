package com.example.ember.adapter

import com.example.ember.util.ThumbnailLoadingDrawable

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.ember.R
import com.example.ember.model.Track

class TrackAdapter(
    private val showRank: Boolean = false,
    private val onTrackClick: (track: Track, position: Int) -> Unit,
    private val onLikeClick: ((track: Track, position: Int) -> Unit)? = null,
    private val onOptionsClick: ((track: Track, position: Int) -> Unit)? = null,
    private val onRemoveClick: ((track: Track, position: Int) -> Unit)? = null,
    private val onAddClick: ((track: Track, position: Int) -> Unit)? = null
) : RecyclerView.Adapter<TrackAdapter.TrackViewHolder>() {

    private val tracks = mutableListOf<Track>()
    private var playingTrackId: String? = null
    private var isCurrentlyPlaying: Boolean = false

    fun setTracks(newTracks: List<Track>) {
        tracks.clear()
        tracks.addAll(newTracks)
        notifyDataSetChanged()
    }

    fun getTracks(): List<Track> = tracks

    fun updatePlaybackState(trackId: String?, isPlaying: Boolean) {
        val oldId = playingTrackId
        playingTrackId = trackId
        isCurrentlyPlaying = isPlaying

        // Rebind affected items
        val oldIndex = tracks.indexOfFirst { it.id == oldId }
        val newIndex = tracks.indexOfFirst { it.id == trackId }
        if (oldIndex >= 0) notifyItemChanged(oldIndex)
        if (newIndex >= 0 && newIndex != oldIndex) notifyItemChanged(newIndex)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TrackViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_track, parent, false)
        return TrackViewHolder(view)
    }

    override fun onBindViewHolder(holder: TrackViewHolder, position: Int) {
        val track = tracks[position]
        holder.bind(track, position)
    }

    override fun getItemCount(): Int = tracks.size

    inner class TrackViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val layoutRankOrEq: FrameLayout = itemView.findViewById(R.id.layoutRankOrEq)
        private val tvRank: TextView = itemView.findViewById(R.id.tvTrackRank)
        private val layoutEqBars: LinearLayout = itemView.findViewById(R.id.layoutEqBars)
        private val eqBar1: View = itemView.findViewById(R.id.eqBar1)
        private val eqBar2: View = itemView.findViewById(R.id.eqBar2)
        private val eqBar3: View = itemView.findViewById(R.id.eqBar3)

        private val ivThumb: ImageView = itemView.findViewById(R.id.ivItemThumb)
        private val tvTitle: TextView = itemView.findViewById(R.id.tvItemTitle)
        private val tvSubtitle: TextView = itemView.findViewById(R.id.tvItemSubtitle)
        private val ivLike: ImageView = itemView.findViewById(R.id.ivItemLike)
        private val ivOptions: ImageView = itemView.findViewById(R.id.ivItemOptions)
        private val ivAdd: ImageView = itemView.findViewById(R.id.ivItemAdd)
        private val ivRemove: ImageView = itemView.findViewById(R.id.ivItemRemove)

        private var anim1: ObjectAnimator? = null
        private var anim2: ObjectAnimator? = null
        private var anim3: ObjectAnimator? = null

        fun bind(track: Track, position: Int) {
            val isThisTrackActive = (track.id == playingTrackId)

            tvTitle.text = com.example.ember.engine.EmberAudioEngine.sanitizeMetadata(track.title)
            tvSubtitle.text = "${com.example.ember.engine.EmberAudioEngine.sanitizeMetadata(track.artist)} • ${track.duration}"

            if (isThisTrackActive) {
                tvTitle.setTextColor(ContextCompat.getColor(itemView.context, R.color.accent_primary))
            } else {
                tvTitle.setTextColor(ContextCompat.getColor(itemView.context, R.color.text_primary))
            }

            // Rank & Equalizer display
            if (showRank || isThisTrackActive) {
                layoutRankOrEq.visibility = View.VISIBLE
                if (isThisTrackActive && isCurrentlyPlaying) {
                    tvRank.visibility = View.GONE
                    layoutEqBars.visibility = View.VISIBLE
                    startEqAnimation()
                } else {
                    stopEqAnimation()
                    layoutEqBars.visibility = View.GONE
                    tvRank.visibility = View.VISIBLE
                    tvRank.text = "#${position + 1}"
                    if (position < 3) {
                        tvRank.setTextColor(ContextCompat.getColor(itemView.context, R.color.accent_primary))
                    } else {
                        tvRank.setTextColor(ContextCompat.getColor(itemView.context, R.color.text_secondary))
                    }
                }
            } else {
                stopEqAnimation()
                layoutRankOrEq.visibility = View.GONE
            }

            // Thumbnail
            if (track.thumbnail.isNotEmpty()) {
                ivThumb.load(track.thumbnail) {
                    crossfade(true)
                    placeholder(ThumbnailLoadingDrawable(2f, 12f))
                    error(R.drawable.rounded_card_bg)
                }
            } else {
                ivThumb.setImageResource(R.drawable.rounded_card_bg)
            }

            // Delete / Cross button (Queue deletion)
            if (onRemoveClick != null) {
                ivRemove.visibility = View.VISIBLE
                ivLike.visibility = View.GONE
                ivOptions.visibility = View.GONE
                ivAdd.visibility = View.GONE
                ivRemove.setOnClickListener {
                    it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    onRemoveClick.invoke(track, position)
                }
            } else if (onAddClick != null) {
                // Quick Add button (Queue recommendation)
                ivAdd.visibility = View.VISIBLE
                ivRemove.visibility = View.GONE
                ivLike.visibility = View.GONE
                ivOptions.visibility = View.GONE
                ivAdd.setOnClickListener {
                    it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    onAddClick.invoke(track, position)
                }
            } else {
                ivRemove.visibility = View.GONE
                ivAdd.visibility = View.GONE
                ivLike.visibility = View.VISIBLE
                ivOptions.visibility = View.VISIBLE

                // Like heart icon
                if (track.isLiked) {
                    ivLike.setImageResource(R.drawable.ic_heart_filled)
                    ivLike.imageTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#FF453A"))
                } else {
                    ivLike.setImageResource(R.drawable.ic_heart)
                    ivLike.imageTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#8E8E93"))
                }

                ivLike.setOnClickListener {
                    it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    onLikeClick?.invoke(track, position)
                    notifyItemChanged(position)
                }

                ivOptions.setOnClickListener {
                    it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    onOptionsClick?.invoke(track, position)
                }
            }

            itemView.setOnClickListener {
                it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                onTrackClick(track, position)
            }
        }

        private fun startEqAnimation() {
            if (anim1 == null) {
                anim1 = ObjectAnimator.ofFloat(eqBar1, "scaleY", 0.3f, 1f).apply {
                    duration = 320
                    repeatCount = ValueAnimator.INFINITE
                    repeatMode = ValueAnimator.REVERSE
                }
                anim2 = ObjectAnimator.ofFloat(eqBar2, "scaleY", 1f, 0.4f).apply {
                    duration = 270
                    repeatCount = ValueAnimator.INFINITE
                    repeatMode = ValueAnimator.REVERSE
                }
                anim3 = ObjectAnimator.ofFloat(eqBar3, "scaleY", 0.5f, 1f).apply {
                    duration = 380
                    repeatCount = ValueAnimator.INFINITE
                    repeatMode = ValueAnimator.REVERSE
                }
            }
            if (anim1?.isStarted != true) {
                anim1?.start()
                anim2?.start()
                anim3?.start()
            }
        }

        private fun stopEqAnimation() {
            anim1?.cancel()
            anim2?.cancel()
            anim3?.cancel()
        }
    }
}
