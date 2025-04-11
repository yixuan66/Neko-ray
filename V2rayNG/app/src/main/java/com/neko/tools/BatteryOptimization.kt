package com.neko.tools

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.neko.v2ray.R

object BatteryOptimization {

    private const val PREFS_NAME = "battery_optimization_prefs"
    private const val KEY_DIALOG_SHOWN = "battery_dialog_shown"

    fun showIfNeeded(activity: AppCompatActivity) {
        val prefs = activity.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val powerManager = activity.getSystemService(Context.POWER_SERVICE) as PowerManager
            val packageName = activity.packageName
            val isIgnoring = powerManager.isIgnoringBatteryOptimizations(packageName)

            // Reset jika izin belum diberikan
            if (!isIgnoring) {
                prefs.edit().putBoolean(KEY_DIALOG_SHOWN, false).apply()
            }
        }

        val alreadyShown = prefs.getBoolean(KEY_DIALOG_SHOWN, false)
        if (!alreadyShown) {
            MaterialAlertDialogBuilder(activity)
                .setTitle(activity.getString(R.string.battery_optimization_title))
                .setMessage(
                    activity.getString(
                        R.string.battery_optimization_message,
                        activity.getString(R.string.app_name)
                    )
                )
                .setPositiveButton(activity.getString(R.string.battery_optimization_allow)) { _, _ ->
                    prefs.edit().putBoolean(KEY_DIALOG_SHOWN, true).apply()
                    requestIgnoreBatteryOptimization(activity)
                }
                .setNegativeButton(activity.getString(R.string.battery_optimization_cancel)) { _, _ ->
                    prefs.edit().putBoolean(KEY_DIALOG_SHOWN, true).apply()
                }
                .setOnCancelListener {
                    prefs.edit().putBoolean(KEY_DIALOG_SHOWN, true).apply()
                }
                .show()
        }
    }

    private fun requestIgnoreBatteryOptimization(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = Uri.parse("package:${context.packageName}")
            }
            context.startActivity(intent)
        }
    }
}
