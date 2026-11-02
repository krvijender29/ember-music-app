package com.example.ember.adapter

import com.example.ember.util.ThumbnailLoadingDrawable

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import coil.load
import coil.transform.RoundedCornersTransformation
import com.example.ember.R
import com.example.ember.model.PlaylistItem
import com.example.ember.ui.EmberTheme

class PlaylistCardAdapter(
    private val isHorizontal: Boolean = true,
    private val onPlaylistClick: (PlaylistItem) -> Unit
) : RecyclerView.Adapter<PlaylistCardAdapter.PlaylistViewHolder>() {

    private val playlists = mutableListOf<PlaylistItem>()

    fun setPlaylists(newList: List<PlaylistItem>) {
        playlists.clear()
        playlists.addAll(newList)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlaylistViewHolder {
        val layoutRes = if (isHorizontal) R.layout.item_playlist_horizontal else R.layout.item_playlist_grid_card
        val view = LayoutInflater.from(parent.context).inflate(layoutRes, parent, false)
        return PlaylistViewHolder(view)
    }

    override fun onBindViewHolder(holder: PlaylistViewHolder, position: Int) {
        holder.bind(playlists[position])
    }

    override fun getItemCount(): Int = playlists.size

    inner class PlaylistViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivCover: ImageView = itemView.findViewById(R.id.ivPlaylistCover)
        private val tvTitle: TextView = itemView.findViewById(R.id.tvPlaylistTitle)
        private val tvSubtitle: TextView = itemView.findViewById(R.id.tvPlaylistSubtitle)

        fun bind(playlist: PlaylistItem) {
            tvTitle.text = playlist.title
            tvSubtitle.text = playlist.subtitle
            ivCover.load(playlist.image) {
                crossfade(true)
                transformations(RoundedCornersTransformation(EmberTheme.Radii.SongThumbnail))
                placeholder(ThumbnailLoadingDrawable(2.5f, 16f))
                error(R.drawable.rounded_card_bg)
            }
            itemView.setOnClickListener { onPlaylistClick(playlist) }
        }
    }
}
