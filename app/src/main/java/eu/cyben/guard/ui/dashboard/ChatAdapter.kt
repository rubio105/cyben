package eu.cyben.guard.ui.dashboard

import android.view.Gravity
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import eu.cyben.guard.R
import eu.cyben.guard.data.models.ChatMessage

class ChatAdapter(private val messages: List<ChatMessage>) : RecyclerView.Adapter<ChatAdapter.VH>() {
    inner class VH(val tv: TextView, val container: LinearLayout) : RecyclerView.ViewHolder(container)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val container = LinearLayout(parent.context).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { setMargins(16, 8, 16, 8) }
        }
        val tv = TextView(parent.context).apply {
            setPadding(32, 24, 32, 24)
            textSize = 15f
            maxWidth = (parent.width * 0.80).toInt()
        }
        container.addView(tv)
        return VH(tv, container)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val msg = messages[position]
        holder.tv.text = msg.text
        if (msg.isUser) {
            holder.tv.setBackgroundResource(R.drawable.bg_bubble_user)
            holder.tv.setTextColor(ContextCompat.getColor(holder.tv.context, android.R.color.white))
            holder.container.gravity = Gravity.END
        } else {
            holder.tv.setBackgroundResource(R.drawable.bg_bubble_ai)
            holder.tv.setTextColor(ContextCompat.getColor(holder.tv.context, R.color.text_primary))
            holder.container.gravity = Gravity.START
        }
    }

    override fun getItemCount() = messages.size
}
