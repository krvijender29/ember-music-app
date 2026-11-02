package com.example.ember.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.ember.R
import com.example.ember.model.Track

class HorizontalCardAdapter(
    private val showRank: Boolean = false,
    private val onCardClick: (track: Track, position: Int) -> Unit
) : RecyclerView.Adapter<HorizontalCardAdapter.CardViewHolder>() {

    private val tracks = mutableListOf<Track>()

    fun setTracks(newTracks: List<Track>) {
        tracks.clear()
        tracks.addAll(newTracks)
        notifyDataSetChanged()
    }

    fun getTracks(): List<Track> = tracks

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CardViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_horizontal_card, parent, false)
        return CardViewHolder(view)
    }

    override fun onBindViewHolder(holder: CardViewHolder, position: Int) {
        val track = tracks[position]
        holder.bind(track, position)
    }

    override fun getItemCount(): Int = tracks.size

    inner class CardViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivThumb: ImageView = itemView.findViewById(R.id.ivCardThumb)
        private val tvTitle: TextView = itemView.findViewById(R.id.tvCardTitle)
        private val tvArtist: TextView = itemView.findViewById(R.id.tvCardArtist)
        private val tvRank: TextView = itemView.findViewById(R.id.tvCardRank)
        private val btnPlay: ImageView = itemView.findViewById(R.id.btnCardPlay)

        fun bind(track: Track, position: Int) {
            tvTitle.text = track.title
            tvArtist.text = track.artist

            if (showRank) {
                tvRank.visibility = View.VISIBLE
                tvRank.text = "#${position + 1}"
            } else {
                tvRank.visibility = View.GONE
            }

            if (track.thumbnail.isNotEmpty()) {
                ivThumb.load(track.thumbnail) {
                    crossfade(true)
                    placeholder(R.drawable.ember_banner)
                    error(R.drawable.ember_banner)
                }
            } else {
                ivThumb.setImageResource(R.drawable.ember_banner)
            }

            itemView.setOnClickListener {
                onCardClick(track, position)
            }

            btnPlay.setOnClickListener {
                onCardClick(track, position)
            }
        }
    }
}
