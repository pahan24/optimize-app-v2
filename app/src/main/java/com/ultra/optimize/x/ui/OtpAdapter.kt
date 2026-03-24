package com.ultra.optimize.x.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.ultra.optimize.x.R

data class Otp(val code: String, val isUsed: Boolean)

class OtpAdapter(
    private var otps: List<Otp>,
    private val onDelete: (Otp) -> Unit
) : RecyclerView.Adapter<OtpAdapter.OtpViewHolder>() {

    class OtpViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvCode: TextView = view.findViewById(R.id.tv_otp_code_item)
        val tvStatus: TextView = view.findViewById(R.id.tv_otp_status_item)
        val btnDelete: ImageButton = view.findViewById(R.id.btn_delete_otp)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OtpViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_otp, parent, false)
        return OtpViewHolder(view)
    }

    override fun onBindViewHolder(holder: OtpViewHolder, position: Int) {
        val otp = otps[position]
        holder.tvCode.text = otp.code
        holder.tvStatus.text = if (otp.isUsed) "USED" else "ACTIVE"
        holder.tvStatus.setTextColor(
            if (otp.isUsed) holder.itemView.resources.getColor(R.color.accent_red)
            else holder.itemView.resources.getColor(R.color.accent_green)
        )
        holder.btnDelete.setOnClickListener { onDelete(otp) }
    }

    override fun getItemCount() = otps.size

    fun updateData(newOtps: List<Otp>) {
        otps = newOtps
        notifyDataSetChanged()
    }
}
