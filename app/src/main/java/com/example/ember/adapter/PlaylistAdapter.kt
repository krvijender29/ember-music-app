package com.example.ember.adapter

import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.ember.R
import com.example.ember.model.Playlist

class PlaylistAdapter(
    private val onPlaylistClick: (playlist: Playlist, position: Int) -> Unit,
    private val onOptionsClick: (playlist: Playlist, position: Int) -> Unit
) : RecyclerView.Adapter<PlaylistAdapter.PlaylistViewHolder>() {

    private val playlists = mutableListOf<Playlist>()

    fun setPlaylists(newPlaylists: List<Playlist>) {
        playlists.clear()
        playlists.addAll(newPlaylists)
        notifyDataSetChanged()
    }

    fun getPlaylists(): List<Playlist> = playlists

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlaylistViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_playlist, parent, false)
        return PlaylistViewHolder(view)
    }

    override fun onBindViewHolder(holder: PlaylistViewHolder, position: Int) {
        val playlist = playlists[position]
        holder.bind(playlist, position)
    }

    override fun getItemCount(): Int = playlists.size

    inner class PlaylistViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivCover: ImageView = itemView.findViewById(R.id.ivPlaylistCover)
        private val ivFallback: ImageView = itemView.findViewById(R.id.ivPlaylistIconFallback)
        private val tvName: TextView = itemView.findViewById(R.id.tvPlaylistName)
        private val tvCount: TextView = itemView.findViewById(R.id.tvPlaylistTrackCount)
        private val ivOptions: ImageView = itemView.findViewById(R.id.ivPlaylistOptions)

        fun bind(playlist: Playlist, position: Int) {
            tvName.text = playlist.name
            val count = playlist.tracks.size
            tvCount.text = "$count tracks • Custom Playlist"

            val firstArtwork = playlist.tracks.firstOrNull { it.thumbnail.isNotEmpty() }?.thumbnail
            if (!firstArtwork.isNullOrEmpty()) {
                ivFallback.visibility = View.GONE
                ivCover.visibility = View.VISIBLE
                ivCover.load(firstArtwork) {
                    crossfade(true)
                    placeholder(R.drawable.rounded_card_bg)
                    error(R.drawable.rounded_card_bg)
                }
            } else {
                ivCover.visibility = View.GONE
                ivFallback.visibility = View.VISIBLE
            }

            itemView.setOnClickListener {
                it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                onPlaylistClick(playlist, position)
            }

            ivOptions.setOnClickListener {
                it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                onOptionsClick(playlist, position)
            }
        }
    }
}
