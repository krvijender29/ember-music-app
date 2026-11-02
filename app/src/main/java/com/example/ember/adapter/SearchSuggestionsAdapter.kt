package com.example.ember.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.ember.R

class SearchSuggestionsAdapter(
    private val onSuggestionClick: (String) -> Unit
) : RecyclerView.Adapter<SearchSuggestionsAdapter.SuggestionViewHolder>() {

    private val suggestions = mutableListOf<String>()

    fun setSuggestions(newList: List<String>) {
        suggestions.clear()
        suggestions.addAll(newList)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SuggestionViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_search_suggestion, parent, false)
        return SuggestionViewHolder(view)
    }

    override fun onBindViewHolder(holder: SuggestionViewHolder, position: Int) {
        val isLast = position == suggestions.size - 1
        holder.bind(suggestions[position], isLast)
    }

    override fun getItemCount(): Int = suggestions.size

    inner class SuggestionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvQuery: TextView = itemView.findViewById(R.id.tvSuggestionText)
        private val btnInsert: View = itemView.findViewById(R.id.btnSuggestionInsert)
        private val layoutRow: View = itemView.findViewById(R.id.layoutSuggestionRow)
        private val viewDivider: View = itemView.findViewById(R.id.viewSuggestionDivider)

        fun bind(text: String, isLast: Boolean) {
            tvQuery.text = text
            viewDivider.visibility = if (isLast) View.GONE else View.VISIBLE
            layoutRow.setOnClickListener {
                it.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP)
                onSuggestionClick(text)
            }
            btnInsert.setOnClickListener {
                it.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP)
                onSuggestionClick(text)
            }
        }
    }
}
