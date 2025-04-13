package com.neko.v2ray

import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner

class AppLifecycleObserver(private val context: Context) : DefaultLifecycleObserver {

    private val handler = Handler(Looper.getMainLooper())
    private var isInBackground = false

    private val resetLaunchFlagRunnable = Runnable {
        // Reset hanya jika masih di background setelah delay
        if (isInBackground) {
            val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
            prefs.edit().putBoolean("is_first_launch", true).apply()
        }
    }

    override fun onStop(owner: LifecycleOwner) {
        // App pindah ke background
        isInBackground = true
        handler.postDelayed(resetLaunchFlagRunnable, 10000) // Delay 10 detik
    }

    override fun onStart(owner: LifecycleOwner) {
        // App kembali ke foreground
        isInBackground = false
        handler.removeCallbacks(resetLaunchFlagRunnable)
    }
}
