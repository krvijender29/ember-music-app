package com.example.ember.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.ember.R

class RecentSearchesAdapter(
    private val onQueryClick: (String) -> Unit,
    private val onRemoveClick: (String) -> Unit
) : RecyclerView.Adapter<RecentSearchesAdapter.RecentViewHolder>() {

    private val queries = mutableListOf<String>()

    fun setQueries(newQueries: List<String>) {
        queries.clear()
        queries.addAll(newQueries)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecentViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_recent_search_chip, parent, false)
        return RecentViewHolder(view)
    }

    override fun onBindViewHolder(holder: RecentViewHolder, position: Int) {
        holder.bind(queries[position])
    }

    override fun getItemCount(): Int = queries.size

    inner class RecentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvQuery: TextView = itemView.findViewById(R.id.tvRecentQuery)
        private val btnRemove: ImageView = itemView.findViewById(R.id.btnRemoveRecent)

        fun bind(query: String) {
            tvQuery.text = query
            itemView.setOnClickListener { onQueryClick(query) }
            btnRemove.setOnClickListener { onRemoveClick(query) }
        }
    }
}
