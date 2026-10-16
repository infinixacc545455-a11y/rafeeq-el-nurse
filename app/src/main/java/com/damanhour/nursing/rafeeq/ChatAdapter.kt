package com.damanhour.nursing.rafeeq

import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ChatAdapter(private val items: List<Msg>) : RecyclerView.Adapter<ChatAdapter.VH>() {

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val row: LinearLayout = v.findViewById(R.id.row)
        val bubble: TextView = v.findViewById(R.id.bubble)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_message, parent, false)
        return VH(v)
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val m = items[position]
        val ctx = holder.itemView.context
        val d = ctx.resources.displayMetrics.density
        val big = 20f * d
        val small = 5f * d

        holder.bubble.text = m.text
        val bg = GradientDrawable()
        if (m.user) {
            // اليمين (RTL): الزاوية السفلية اليمنى صغيرة
            bg.cornerRadii = floatArrayOf(big, big, big, big, small, small, big, big)
            bg.setColor(ctx.getColor(R.color.user_bubble))
            holder.bubble.setTextColor(Color.WHITE)
            holder.bubble.setTypeface(null, Typeface.NORMAL)
            holder.row.gravity = Gravity.START
        } else {
            // الشمال: الزاوية السفلية اليسرى صغيرة
            bg.cornerRadii = floatArrayOf(big, big, big, big, big, big, small, small)
            bg.setColor(ctx.getColor(R.color.bot_bubble))
            bg.setStroke((1 * d).toInt(), Color.parseColor("#DCE8E5"))
            if (m.loading) {
                holder.bubble.setTextColor(Color.parseColor("#7F9A95"))
                holder.bubble.setTypeface(null, Typeface.ITALIC)
            } else {
                holder.bubble.setTextColor(Color.parseColor("#1B2B29"))
                holder.bubble.setTypeface(null, Typeface.NORMAL)
            }
            holder.row.gravity = Gravity.END
        }
        holder.bubble.background = bg
    }
}
