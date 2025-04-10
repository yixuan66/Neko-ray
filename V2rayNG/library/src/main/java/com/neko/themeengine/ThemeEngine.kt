package com.neko.themeengine

import android.app.Activity
import android.app.Application
import android.app.Application.ActivityLifecycleCallbacks
import android.content.Context
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.edit
import com.neko.R

/**
 * ThemeEngine is a utility class to manage light/dark mode, dynamic theming,
 * and static theme selection across the app.
 *
 * @constructor Initializes preferences and applies default values on first launch.
 */
class ThemeEngine(context: Context) {
    private val prefs = context.getSharedPreferences("theme_engine_prefs", Context.MODE_PRIVATE)

    private var isFirstStart
        get() = prefs.getBoolean(FIRST_START, true)
        set(value) = prefs.edit { putBoolean(FIRST_START, value) }

    init {
        if (isFirstStart) {
            setDefaultValues(context)
            isFirstStart = false
        }
    }

    /**
     * Returns the current theme mode.
     * Possible values are [ThemeMode.LIGHT], [ThemeMode.DARK], or [ThemeMode.AUTO].
     *
     * Setting this will persist the value and apply the selected mode using AppCompatDelegate.
     *
     * @throws IllegalArgumentException if value is outside the valid range.
     */
    var themeMode: Int
        get() = prefs.getInt(THEME_MODE, ThemeMode.AUTO)
        set(value) {
            require(value in 0..2) {
                "Incompatible value! Set this property with help of ThemeMode object."
            }
            prefs.edit { putInt(THEME_MODE, value) }
            AppCompatDelegate.setDefaultNightMode(
                when (value) {
                    ThemeMode.LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
                    ThemeMode.DARK -> AppCompatDelegate.MODE_NIGHT_YES
                    else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                }
            )
        }

    private val nightMode
        get() = when (themeMode) {
            ThemeMode.LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
            ThemeMode.DARK -> AppCompatDelegate.MODE_NIGHT_YES
            else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        }

    /**
     * Indicates whether dynamic theming is enabled.
     *
     * Dynamic theming is available only on Android 12 (API 31) and above.
     * After changing this value, you should call `Activity.recreate()` to apply the change.
     */
    var isDynamicTheme
        get() = prefs.getBoolean(DYNAMIC_THEME, Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
        set(value) = prefs.edit { putBoolean(DYNAMIC_THEME, value) }

    /**
     * Returns the current app theme resource ID.
     *
     * Uses a dynamic theme if [isDynamicTheme] is enabled and the device supports it,
     * otherwise returns the static theme.
     */
    fun getTheme(): Int {
        return if (hasS() && isDynamicTheme) R.style.Theme_ThemeEngine_Dynamic else staticTheme.themeId
    }

    /**
     * Returns or sets the current static theme.
     *
     * This is used when dynamic theming is not supported or disabled.
     */
    var staticTheme: Theme
        get() = Theme.values()[prefs.getInt(APP_THEME, 1)]
        set(value) = prefs.edit { putInt(APP_THEME, value.ordinal) }

    /**
     * Resets the selected static theme to its default.
     */
    fun resetTheme() {
        prefs.edit { remove(APP_THEME) }
    }

    /**
     * Reads default values from XML resources and stores them in preferences.
     * Called only on the first launch of the app.
     */
    private fun setDefaultValues(context: Context) {
        themeMode = context.getIntSafe(R.integer.theme_mode, ThemeMode.AUTO)
        prefs.edit { putInt(APP_THEME, context.getIntSafe(R.integer.static_theme, 1)) }
        isDynamicTheme = context.getBooleanSafe(R.bool.dynamic_theme, hasS()) && hasS()
    }

    companion object {
        private var INSTANCE: ThemeEngine? = null

        /**
         * Returns a singleton instance of ThemeEngine.
         */
        @JvmStatic
        fun getInstance(context: Context): ThemeEngine {
            val currentInstance = INSTANCE

            if (currentInstance != null) {
                return currentInstance
            }

            synchronized(this) {
                val newInstance = ThemeEngine(context)
                INSTANCE = newInstance
                return newInstance
            }
        }

        /**
         * Applies the selected theme and night mode to all activities in the application
         * by registering a lifecycle callback.
         *
         * @param application The Application to apply themes to.
         */
        @JvmStatic
        fun applyToActivities(application: Application) {
            application.registerActivityLifecycleCallbacks(ThemeEngineActivityCallback())
        }

        /**
         * Applies the selected theme and night mode to a specific activity.
         *
         * @param activity The Activity to apply the theme to.
         */
        @JvmStatic
        fun applyToActivity(activity: Activity) {
            with(getInstance(activity)) {
                activity.theme.applyStyle(getTheme(), true)
                AppCompatDelegate.setDefaultNightMode(nightMode)
            }
        }

        private const val THEME_MODE = "theme_mode"
        private const val DYNAMIC_THEME = "dynamic_theme"
        private const val APP_THEME = "app_theme"
        private const val FIRST_START = "first_start"
    }
}

/**
 * Internal class used to hook into activity lifecycle and apply the theme during creation.
 */
private class ThemeEngineActivityCallback : ActivityLifecycleCallbacks {
    override fun onActivityPreCreated(
        activity: Activity,
        savedInstanceState: Bundle?
    ) {
        ThemeEngine.applyToActivity(activity)
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
    override fun onActivityStarted(activity: Activity) {}
    override fun onActivityResumed(activity: Activity) {}
    override fun onActivityPaused(activity: Activity) {}
    override fun onActivityStopped(activity: Activity) {}
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
    override fun onActivityDestroyed(activity: Activity) {}
}
