package com.neko.iplocation

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import android.widget.Toast
import com.neko.v2ray.R

class KeyValueAdapter(private val context: Context, private val items: List<KeyValueItem>) : BaseAdapter() {

    override fun getCount(): Int = items.size
    override fun getItem(position: Int): Any = items[position]
    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val item = items[position]
        val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.item_key_value, parent, false)

        val keyText = view.findViewById<TextView>(R.id.keyText)
        val valueText = view.findViewById<TextView>(R.id.valueText)

        keyText.text = item.key
        valueText.text = item.value

        view.setOnClickListener {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("copied", "${item.key}: ${item.value}")
            clipboard.setPrimaryClip(clip)
            Toast.makeText(context, "Copied: ${item.key}: ${item.value}", Toast.LENGTH_SHORT).show()
        }

        return view
    }
}

