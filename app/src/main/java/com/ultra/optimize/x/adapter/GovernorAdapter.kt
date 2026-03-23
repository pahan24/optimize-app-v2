package com.ultra.optimize.x.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.ultra.optimize.x.R

class GovernorAdapter(
    private val governors: List<String>,
    private val currentGovernor: String,
    private val onGovernorSelected: (String) -> Unit
) : RecyclerView.Adapter<GovernorAdapter.ViewHolder>() {

    private var selectedPosition = governors.indexOf(currentGovernor)

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvName: TextView = view.findViewById(R.id.tv_governor_name)
        val indicator: View = view.findViewById(R.id.view_indicator)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_governor, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val governor = governors[position]
        holder.tvName.text = governor
        
        if (position == selectedPosition) {
            holder.indicator.visibility = View.VISIBLE
            holder.tvName.setTextColor(holder.itemView.context.getColor(R.color.neon_blue))
        } else {
            holder.indicator.visibility = View.INVISIBLE
            holder.tvName.setTextColor(holder.itemView.context.getColor(R.color.text_primary))
        }

        holder.itemView.setOnClickListener {
            val oldPosition = selectedPosition
            selectedPosition = holder.adapterPosition
            notifyItemChanged(oldPosition)
            notifyItemChanged(selectedPosition)
            onGovernorSelected(governor)
        }
    }

    override fun getItemCount() = governors.size
}
