package com.neko.server

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.neko.v2ray.databinding.ItemServerCardBinding
import com.neko.v2ray.R

class VpnServerAdapter(private val servers: List<VpnServer>) :
    RecyclerView.Adapter<VpnServerAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: ItemServerCardBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemServerCardBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun getItemCount(): Int = servers.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val server = servers[position]
        with(holder.binding) {
            tvName.text = server.name
            tvStatus.text = server.status
            tvPing.text = if (server.ping >= 0) "${server.ping} ms" else "N/A"

            val context = root.context
            val cardView = root as MaterialCardView

            val bgColor = when (server.status) {
                "Online" -> R.color.server_online_bg
                "Offline" -> R.color.server_offline_bg
                else -> R.color.server_card_bg
            }

            cardView.setCardBackgroundColor(ContextCompat.getColor(context, bgColor))
        }
    }
}
