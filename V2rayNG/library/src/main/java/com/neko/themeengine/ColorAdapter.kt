package com.neko.themeengine

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.neko.R
import com.neko.databinding.ItemColorBinding

/**
 * Adapter for displaying a list of colors and allowing the user to select one.
 * The selected item shows a checkmark icon.
 */
class ColorAdapter(private val colorArray: List<Int>) :
    RecyclerView.Adapter<ColorAdapter.ViewHolder>() {

    // Index of the currently selected color
    var checkedPosition = -1
        private set

    /**
     * Updates the selected position and refreshes the previously selected and newly selected items.
     *
     * @param value The selected theme enum, whose ordinal corresponds to a color position.
     */
    fun setCheckedPosition(value: Theme) {
        val lastCheckedPosition: Int = checkedPosition
        checkedPosition = value.ordinal
        notifyItemChanged(lastCheckedPosition)
        notifyItemChanged(checkedPosition)
    }

    // Enables stable IDs for better performance with RecyclerView
    init {
        setHasStableIds(true)
    }

    override fun getItemId(position: Int): Long {
        return colorArray[position].toLong()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        // Inflate the layout for each color item
        return ViewHolder(
            ItemColorBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        with(holder.binding) {
            // Set the background color for the item
            colorView.setBackgroundColorRes(colorArray[position])
            // Show a check icon if the item is selected
            colorView.setImageResource(
                if (checkedPosition == position) R.drawable.ic_round_check else 0
            )
        }
    }

    override fun getItemCount() = colorArray.size

    /**
     * ViewHolder class for binding color views.
     */
    inner class ViewHolder(val binding: ItemColorBinding) :
        RecyclerView.ViewHolder(binding.root) {

        init {
            binding.colorView.setOnClickListener {
                val lastCheckedPosition: Int = checkedPosition
                // Update the checked position to the clicked item
                checkedPosition = adapterPosition
                // Show check icon on newly selected item
                binding.colorView.setImageResource(R.drawable.ic_round_check)
                // Refresh the previously selected item to remove the check icon
                notifyItemChanged(lastCheckedPosition)
            }
        }
    }
}
