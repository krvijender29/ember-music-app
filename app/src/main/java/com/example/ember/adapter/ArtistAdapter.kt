package com.example.ember.adapter

import com.example.ember.util.ThumbnailLoadingDrawable

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import coil.load
import coil.transform.CircleCropTransformation
import com.example.ember.R
import com.example.ember.model.ArtistItem

class ArtistAdapter(
    private val isHorizontal: Boolean = true,
    private val onArtistClick: (ArtistItem) -> Unit
) : RecyclerView.Adapter<ArtistAdapter.ArtistViewHolder>() {

    private val artists = mutableListOf<ArtistItem>()

    fun setArtists(newArtists: List<ArtistItem>) {
        artists.clear()
        artists.addAll(newArtists)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ArtistViewHolder {
        val layoutRes = if (isHorizontal) R.layout.item_artist_horizontal else R.layout.item_artist_grid
        val view = LayoutInflater.from(parent.context).inflate(layoutRes, parent, false)
        return ArtistViewHolder(view)
    }

    override fun onBindViewHolder(holder: ArtistViewHolder, position: Int) {
        holder.bind(artists[position])
    }

    override fun getItemCount(): Int = artists.size

    inner class ArtistViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivAvatar: ImageView = itemView.findViewById(R.id.ivArtistAvatar)
        private val tvName: TextView = itemView.findViewById(R.id.tvArtistName)
        private val tvSubtitle: TextView? = itemView.findViewById(R.id.tvArtistSubtitle)

        fun bind(artist: ArtistItem) {
            tvName.text = artist.name
            tvSubtitle?.text = if (artist.followerCount.isNotBlank()) "${artist.followerCount} Fans" else artist.role
            ivAvatar.load(artist.image) {
                crossfade(true)
                transformations(CircleCropTransformation())
                placeholder(ThumbnailLoadingDrawable(2.5f, 16f))
                error(R.drawable.rounded_card_bg)
            }
            itemView.setOnClickListener { onArtistClick(artist) }
        }
    }
}
