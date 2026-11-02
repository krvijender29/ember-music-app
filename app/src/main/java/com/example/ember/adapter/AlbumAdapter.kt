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
import com.example.ember.model.AlbumItem
import com.example.ember.ui.EmberTheme

class AlbumAdapter(
    private val isHorizontal: Boolean = true,
    private val onAlbumClick: (AlbumItem) -> Unit,
    private val onPlayClick: ((AlbumItem) -> Unit)? = null
) : RecyclerView.Adapter<AlbumAdapter.AlbumViewHolder>() {

    private val albums = mutableListOf<AlbumItem>()

    fun setAlbums(newAlbums: List<AlbumItem>) {
        albums.clear()
        albums.addAll(newAlbums)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AlbumViewHolder {
        val layoutRes = if (isHorizontal) R.layout.item_album_horizontal else R.layout.item_album_card
        val view = LayoutInflater.from(parent.context).inflate(layoutRes, parent, false)
        return AlbumViewHolder(view)
    }

    override fun onBindViewHolder(holder: AlbumViewHolder, position: Int) {
        holder.bind(albums[position])
    }

    override fun getItemCount(): Int = albums.size

    inner class AlbumViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivCover: ImageView = itemView.findViewById(R.id.ivAlbumCover)
        private val tvTitle: TextView = itemView.findViewById(R.id.tvAlbumTitle)
        private val tvSubtitle: TextView = itemView.findViewById(R.id.tvAlbumSubtitle)
        private val btnPlay: View? = itemView.findViewById(R.id.btnAlbumPlay)

        fun bind(album: AlbumItem) {
            tvTitle.text = album.title
            tvSubtitle.text = if (album.year.isNotBlank()) "${album.year} • ${album.subtitle}" else album.subtitle
            ivCover.load(album.image) {
                crossfade(true)
                transformations(RoundedCornersTransformation(EmberTheme.Radii.SongThumbnail))
                placeholder(ThumbnailLoadingDrawable(2.5f, 16f))
                error(R.drawable.rounded_card_bg)
            }
            itemView.setOnClickListener { onAlbumClick(album) }
            btnPlay?.setOnClickListener {
                if (onPlayClick != null) onPlayClick.invoke(album) else onAlbumClick(album)
            }
        }
    }
}
