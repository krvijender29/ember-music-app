package com.example.ember.adapter

import android.graphics.Color
import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.ember.R

data class LyricLine(val timeMs: Long, val text: String)

class LyricsAdapter(
    private val onLineClick: (LyricLine) -> Unit
) : RecyclerView.Adapter<LyricsAdapter.LyricViewHolder>() {

    private val lines = mutableListOf<LyricLine>()
    private var activeLineIndex: Int = -1

    fun setLines(newLines: List<LyricLine>) {
        lines.clear()
        lines.addAll(newLines)
        activeLineIndex = -1
        notifyDataSetChanged()
    }

    fun updateActiveTimestamp(currentMs: Long): Int {
        if (lines.isEmpty()) return -1

        var matchedIndex = -1
        for (i in lines.indices) {
            if (lines[i].timeMs <= currentMs) {
                matchedIndex = i
            } else {
                break
            }
        }

        if (matchedIndex != activeLineIndex) {
            val old = activeLineIndex
            activeLineIndex = matchedIndex
            if (old >= 0 && old < lines.size) notifyItemChanged(old)
            if (activeLineIndex >= 0 && activeLineIndex < lines.size) notifyItemChanged(activeLineIndex)
            return activeLineIndex
        }
        return -1
    }

    fun forceUpdateTimestamp(currentMs: Long): Int {
        if (lines.isEmpty()) return -1
        var matchedIndex = -1
        for (i in lines.indices) {
            if (lines[i].timeMs <= currentMs) {
                matchedIndex = i
            } else {
                break
            }
        }
        val old = activeLineIndex
        activeLineIndex = matchedIndex
        if (old >= 0 && old < lines.size) notifyItemChanged(old)
        if (activeLineIndex >= 0 && activeLineIndex < lines.size) notifyItemChanged(activeLineIndex)
        return activeLineIndex
    }

    fun getActiveIndex(): Int = activeLineIndex

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LyricViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_lyric_line, parent, false)
        return LyricViewHolder(view)
    }

    override fun onBindViewHolder(holder: LyricViewHolder, position: Int) {
        holder.bind(lines[position], position == activeLineIndex)
    }

    override fun getItemCount(): Int = lines.size

    inner class LyricViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvLine: TextView = itemView.findViewById(R.id.tvLyricLine)

        fun bind(line: LyricLine, isActive: Boolean) {
            tvLine.text = line.text
            if (isActive) {
                tvLine.setTextColor(Color.WHITE)
                tvLine.textSize = 17.5f
                tvLine.typeface = Typeface.DEFAULT_BOLD
                tvLine.alpha = 1.0f
            } else {
                tvLine.setTextColor(Color.parseColor("#7A7785"))
                tvLine.textSize = 15.5f
                tvLine.typeface = Typeface.DEFAULT
                tvLine.alpha = 0.5f
            }

            itemView.setOnClickListener {
                onLineClick(line)
            }
        }
    }
}
