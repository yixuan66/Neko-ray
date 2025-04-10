package com.neko.themeengine

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Build
import androidx.annotation.BoolRes
import androidx.annotation.ChecksSdkIntAtLeast
import androidx.annotation.IntegerRes

/**
 * Extension property to check if the current system theme is in dark mode.
 *
 * @return true if the device is in dark (night) mode, false otherwise.
 */
val Context.isDarkMode: Boolean
    get() = (resources.configuration.uiMode and
            Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES

/**
 * Utility function to check if the device is running Android 12 (API 31) or above.
 *
 * @return true if the SDK version is >= 31, false otherwise.
 */
@ChecksSdkIntAtLeast(api = 31)
fun hasS(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

/**
 * Safely retrieves a boolean resource.
 *
 * @param res The resource ID of the boolean.
 * @param defaultValue The default value to return if the resource is not found.
 * @return The boolean value of the resource, or the default value if not found.
 */
fun Context.getBooleanSafe(@BoolRes res: Int, defaultValue: Boolean): Boolean {
    return try {
        resources.getBoolean(res)
    } catch (e: Resources.NotFoundException) {
        defaultValue
    }
}

/**
 * Safely retrieves an integer resource.
 *
 * @param res The resource ID of the integer.
 * @param defaultValue The default value to return if the resource is not found.
 * @return The integer value of the resource, or the default value if not found.
 */
fun Context.getIntSafe(@IntegerRes res: Int, defaultValue: Int): Int {
    return try {
        resources.getInteger(res)
    } catch (e: Resources.NotFoundException) {
        defaultValue
    }
}
