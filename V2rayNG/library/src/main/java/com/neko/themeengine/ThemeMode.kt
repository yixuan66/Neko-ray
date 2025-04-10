package com.neko.themeengine

/**
 * Object representing different theme modes that can be applied to the application.
 */
object ThemeMode {

    /** 
     * Automatically switch between light and dark mode based on system settings or time of day. 
     */
    const val AUTO = 0

    /**
     * Force the application to always use light mode.
     */
    const val LIGHT = 1

    /**
     * Force the application to always use dark mode.
     */
    const val DARK = 2
}
