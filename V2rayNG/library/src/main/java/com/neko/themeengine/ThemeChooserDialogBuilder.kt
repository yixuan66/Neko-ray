package com.neko.themeengine

import android.content.Context
import android.content.DialogInterface.BUTTON_NEUTRAL
import android.content.DialogInterface.BUTTON_POSITIVE
import android.view.LayoutInflater
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.neko.databinding.RecyclerviewBinding

/**
 * Builder class to create a Theme Chooser Dialog.
 * This dialog displays a grid of selectable theme color options and allows the user
 * to confirm or cancel their selection via positive/neutral/negative buttons.
 *
 * Usage Example:
 * ThemeChooserDialogBuilder(context)
 *     .setTitle(R.string.select_theme)
 *     .setPositiveButton("Apply") { pos, theme -> applyTheme(theme) }
 *     .setNegativeButton("Cancel")
 *     .create()
 *     .show()
 *
 * @param context Context used to inflate layouts and build the dialog
 * @return ThemeChooserDialogBuilder
 * @author Prathamesh M
 */
class ThemeChooserDialogBuilder(private val context: Context) {

    private lateinit var builder: MaterialAlertDialogBuilder
    private lateinit var colorAdapter: ColorAdapter
    private val themes = Theme.values() // Get all available theme values

    init {
        createDialog()
    }

    /**
     * Internal method to create and initialize the dialog layout, adapter and builder.
     * It inflates a RecyclerView from the layout and sets up the grid of color options.
     */
    private fun createDialog() {
        val binding = RecyclerviewBinding.inflate(LayoutInflater.from(context))

        val themeEngine = ThemeEngine.getInstance(context)
        val colorArray = themes.map { it.primaryColor }

        // Initialize and configure the color adapter
        colorAdapter = ColorAdapter(colorArray)
        colorAdapter.setCheckedPosition(themeEngine.staticTheme)

        // Setup the recycler view to show theme colors in a grid layout
        binding.recyclerView.apply {
            layoutManager = GridLayoutManager(context, 4)
            adapter = colorAdapter
        }

        // Initialize the Material dialog builder with the customized layout
        builder = MaterialAlertDialogBuilder(context)
            .setView(binding.root)
    }

    /**
     * Set the title of the dialog using a string resource.
     */
    fun setTitle(@StringRes res: Int): ThemeChooserDialogBuilder {
        builder.setTitle(res)
        return this
    }

    /**
     * Set the icon of the dialog using a drawable resource.
     */
    fun setIcon(@DrawableRes iconId: Int): ThemeChooserDialogBuilder {
        builder.setIcon(iconId)
        return this
    }

    /**
     * Set a positive button with custom text and an OnClickListener.
     * When clicked, passes the selected theme's position and value to the listener.
     */
    fun setPositiveButton(text: String, listener: OnClickListener): ThemeChooserDialogBuilder {
        builder.setPositiveButton(text) { _, which ->
            if (which == BUTTON_POSITIVE) {
                val checkedPosition = colorAdapter.checkedPosition
                listener.onClick(checkedPosition, themes[checkedPosition])
            }
        }
        return this
    }

    /**
     * Set a positive button using a string resource.
     */
    fun setPositiveButton(
        @StringRes res: Int,
        listener: OnClickListener
    ): ThemeChooserDialogBuilder {
        setPositiveButton(context.getString(res), listener)
        return this
    }

    /**
     * Set a negative button with custom text.
     * This version does not provide a click listener (default dismiss behavior).
     */
    fun setNegativeButton(text: String): ThemeChooserDialogBuilder {
        builder.setNegativeButton(text, null)
        return this
    }

    /**
     * Set a negative button using a string resource.
     */
    fun setNegativeButton(@StringRes res: Int): ThemeChooserDialogBuilder {
        builder.setNegativeButton(res, null)
        return this
    }

    /**
     * Set a neutral button with custom text and an OnClickListener.
     * When clicked, passes the selected theme's position and value to the listener.
     */
    fun setNeutralButton(text: String, listener: OnClickListener): ThemeChooserDialogBuilder {
        builder.setNeutralButton(text) { _, which ->
            if (which == BUTTON_NEUTRAL) {
                val checkedPosition = colorAdapter.checkedPosition
                val theme = themes[checkedPosition]
                listener.onClick(checkedPosition, theme)
            }
        }
        return this
    }

    /**
     * Set a neutral button using a string resource.
     */
    fun setNeutralButton(
        @StringRes res: Int,
        listener: OnClickListener
    ): ThemeChooserDialogBuilder {
        setNeutralButton(context.getString(res), listener)
        return this
    }

    /**
     * Builds and returns the AlertDialog instance configured with the current settings.
     */
    fun create(): AlertDialog {
        return builder.create()
    }

    /**
     * Functional interface definition for click listener callbacks.
     * Used with positive and neutral buttons to pass selected theme info.
     */
    fun interface OnClickListener {
        fun onClick(position: Int, theme: Theme)
    }
}
