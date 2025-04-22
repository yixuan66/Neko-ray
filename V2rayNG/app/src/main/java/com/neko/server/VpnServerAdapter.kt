package com.neko.server

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.neko.v2ray.databinding.ItemServerCardBinding
import com.neko.v2ray.R

class VpnServerAdapter(
    private val servers: List<VpnServer>,
    private val onServerClick: (VpnServer) -> Unit // Lambda for click handling
) : RecyclerView.Adapter<VpnServerAdapter.ViewHolder>() {

    // ViewHolder that holds the reference to the card item view
    inner class ViewHolder(val binding: ItemServerCardBinding) : RecyclerView.ViewHolder(binding.root) {
        init {
            // Set up the click listener for each item
            binding.root.setOnClickListener {
                val server = servers[adapterPosition]
                onServerClick(server) // Trigger the click handler
            }
        }
    }

    // Inflate the layout and create ViewHolder
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemServerCardBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    // Return the total number of items
    override fun getItemCount(): Int = servers.size

    // Bind data to the views in each item
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val server = servers[position]
        with(holder.binding) {
            // Bind the data from the server to the views
            tvName.text = server.name
            tvStatus.text = server.status
            tvPing.text = if (server.ping >= 0) "${server.ping} ms" else "N/A"

            // Set background color based on server status
            val context = root.context
            val cardView = root as MaterialCardView

            val bgColor = when (server.status) {
                "Online" -> R.color.server_online_bg
                "Offline" -> R.color.server_offline_bg
                else -> R.color.server_card_bg
            }

            // Set the card's background color dynamically based on server status
            cardView.setCardBackgroundColor(ContextCompat.getColor(context, bgColor))
        }
    }
}
