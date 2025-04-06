package com.neko.tools

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.neko.v2ray.ui.BaseActivity

class NetworkSwitcher : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            val radioInfo = Intent("android.intent.action.MAIN").apply {
                setClassName("com.android.settings", "com.android.settings.RadioInfo")
            }
            startActivity(radioInfo)
            finish()
        } catch (e: Exception) {
            try {
                val radioInfo = Intent("android.intent.action.MAIN").apply {
                    setClassName("com.android.phone", "com.android.phone.settings.RadioInfo")
                }
                startActivity(radioInfo)
                finish()
            } catch (e2: Exception) {
                MaterialAlertDialogBuilder(this)
                    .setMessage("Sorry, this feature is not available for your device.")
                    .setPositiveButton("I UNDERSTAND") { dialog, _ ->
                        dialog.dismiss()
                        finish()
                    }
                    .show()
            }
        }
    }
}
