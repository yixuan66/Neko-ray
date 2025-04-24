package com.neko.appupdater

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.util.Log
import android.view.LayoutInflater
import android.widget.ProgressBar
import android.widget.TextView // <-- ADDED
import androidx.appcompat.app.AlertDialog
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.FileProvider
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.neko.v2ray.R
import kotlinx.coroutines.*
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

class AppUpdater(private val context: Context) {

    private val TAG = "AppUpdater"
    private val CHANNEL_ID = "update_channel"
    private val NOTIFICATION_ID = 2025

    var configUrl: String = "https://raw.githubusercontent.com/MRT-project/Neko-ray/main/release.json"
    var showIfUpToDate: Boolean = false
    var onUpdateAvailable: (() -> Unit)? = null
    var onUpdateNotAvailable: (() -> Unit)? = null

    private val prefs = context.getSharedPreferences("app_updater_prefs", Context.MODE_PRIVATE)
    private val PREF_SKIP_UPDATE_VERSION = "skip_update_version"
    private val PREF_SKIP_UPDATE_TIMESTAMP = "skip_update_timestamp"
    private val ONE_DAY_MILLIS = 24 * 60 * 60 * 1000L

    fun checkForUpdate(useNotificationIfAvailable: Boolean = false) {
        if (configUrl.isEmpty()) {
            Log.e(TAG, "Config URL is not set")
            return
        }

        CoroutineScope(Dispatchers.Main).launch {
            val result = fetchUpdate(configUrl)

            if (result == null) {
                Log.e(TAG, "Failed to check for update")
                return@launch
            }

            if (result.updateAvailable) {
                val skippedVersion = prefs.getString(PREF_SKIP_UPDATE_VERSION, null)
                val skippedTimestamp = prefs.getLong(PREF_SKIP_UPDATE_TIMESTAMP, 0L)
                val currentTime = System.currentTimeMillis()

                if (skippedVersion == result.latestVersion && currentTime - skippedTimestamp < ONE_DAY_MILLIS) {
                    Log.i(TAG, "Update skipped recently for ${result.latestVersion}")
                    return@launch
                }

                onUpdateAvailable?.invoke()

                if (useNotificationIfAvailable) {
                    showNotificationForUpdate(result)
                } else {
                    showUpdateDialog(result)
                }
            } else {
                if (showIfUpToDate) showNoUpdateDialog()
                onUpdateNotAvailable?.invoke()
            }
        }
    }

    private suspend fun fetchUpdate(urlString: String): UpdateResult? = withContext(Dispatchers.IO) {
        try {
            val conn = URL(urlString).openConnection() as HttpURLConnection
            conn.connectTimeout = 5000
            conn.readTimeout = 5000
            conn.requestMethod = "GET"

            if (conn.responseCode == HttpURLConnection.HTTP_OK) {
                val stream = conn.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(stream)

                val latestVersion = json.getString("latestVersion")
                val latestVersionCode = json.optInt("latestVersionCode", -1)
                val abi = Build.SUPPORTED_ABIS.firstOrNull() ?: "armeabi-v7a"
                val updateUrl = json.optJSONObject("apkUrls")?.optString(abi)
                    ?: json.optString("updateUrl")
                val releaseNotesArray = json.optJSONArray("releaseNotes")

                val releaseNotes = buildString {
                    if (releaseNotesArray != null) {
                        for (i in 0 until releaseNotesArray.length()) {
                            append("• ${releaseNotesArray.getString(i)}\n")
                        }
                    }
                }

                val currentVersionCode = context.packageManager.getPackageInfo(context.packageName, 0).versionCode
                val updateAvailable = latestVersionCode > currentVersionCode

                UpdateResult(updateAvailable, latestVersion, updateUrl, releaseNotes)
            } else null
        } catch (e: Exception) {
            Log.e(TAG, "fetchUpdate: error", e)
            null
        }
    }

    private fun showUpdateDialog(result: UpdateResult) {
        val appName = context.getString(R.string.app_name)
        val message = context.getString(
            R.string.appupdater_update_available_description_dialog_before_release_notes,
            result.latestVersion ?: "", appName
        ) + if (!result.releaseNotes.isNullOrBlank()) "\n\n${result.releaseNotes}" else ""

        MaterialAlertDialogBuilder(context)
            .setTitle(R.string.appupdater_update_available)
            .setMessage(message)
            .setCancelable(false)
            .setPositiveButton(R.string.appupdater_btn_update) { _, _ ->
                result.updateUrl?.let { downloadAndInstallApk(it) }
            }
            .setNegativeButton(R.string.appupdater_btn_dismiss, null)
            .setNeutralButton(R.string.appupdater_btn_disable) { _, _ ->
                result.latestVersion?.let {
                    prefs.edit()
                        .putString(PREF_SKIP_UPDATE_VERSION, it)
                        .putLong(PREF_SKIP_UPDATE_TIMESTAMP, System.currentTimeMillis())
                        .apply()
                }
            }
            .show()
    }

    private fun showNotificationForUpdate(result: UpdateResult) {
        createNotificationChannel()

        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(result.updateUrl))
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val contentText = context.getString(
            R.string.appupdater_update_available_description_dialog_before_release_notes,
            result.latestVersion ?: "", context.getString(R.string.app_name)
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.uwu_icon_update)
            .setContentTitle(context.getString(R.string.appupdater_update_available))
            .setContentText(contentText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(contentText))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, builder.build())
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Update Notification"
            val descriptionText = "App update notifications"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }

            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun downloadAndInstallApk(url: String) {
        CoroutineScope(Dispatchers.Main).launch {
            val dialogView = LayoutInflater.from(context)
                .inflate(R.layout.uwu_dialog_progress_update, null)
            val progressBar = dialogView.findViewById<ProgressBar>(R.id.progressBar)
            val tvProgressPercent = dialogView.findViewById<TextView>(R.id.tvProgressPercent) // <-- NEW

            var isPaused = false
            var isCanceled = false
            val pauseLock = Object()

            lateinit var progressDialog: AlertDialog

            progressDialog = MaterialAlertDialogBuilder(context)
                .setTitle(R.string.appupdater_downloading)
                .setView(dialogView)
                .setCancelable(false)
                .setPositiveButton(R.string.appupdater_btn_pause, null)
                .setNegativeButton(R.string.appupdater_btn_cancel) { _, _ ->
                    isCanceled = true
                }
                .create()

            progressDialog.setOnShowListener {
                val pauseResumeBtn = progressDialog.getButton(AlertDialog.BUTTON_POSITIVE)
                pauseResumeBtn.setOnClickListener {
                    isPaused = !isPaused
                    pauseResumeBtn.text = if (isPaused) {
                        context.getString(R.string.appupdater_btn_resume)
                    } else {
                        synchronized(pauseLock) { pauseLock.notify() }
                        context.getString(R.string.appupdater_btn_pause)
                    }
                }
            }

            progressDialog.show()

            withContext(Dispatchers.IO) {
                try {
                    val fileName = "update.apk"
                    val file = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), fileName)
                    if (file.exists()) file.delete()

                    val connection = URL(url).openConnection() as HttpURLConnection
                    connection.connect()

                    val fileLength = connection.contentLength
                    val input = connection.inputStream
                    val output = FileOutputStream(file)

                    val buffer = ByteArray(4096)
                    var total: Long = 0
                    var count: Int

                    while (input.read(buffer).also { count = it } != -1) {
                        if (isCanceled) {
                            input.close()
                            output.close()
                            file.delete()
                            throw CancellationException("Download canceled")
                        }

                        synchronized(pauseLock) {
                            while (isPaused) {
                                pauseLock.wait()
                            }
                        }

                        total += count
                        output.write(buffer, 0, count)

                        val progress = if (fileLength > 0) (total * 100 / fileLength).toInt() else -1
                        withContext(Dispatchers.Main) {
                            progressBar.progress = progress
                            tvProgressPercent.text = "$progress%" // <-- NEW
                        }
                    }

                    input.close()
                    output.close()

                    withContext(Dispatchers.Main) {
                        progressDialog.dismiss()
                        installApk(file)
                    }

                } catch (e: Exception) {
                    Log.e(TAG, "Download failed", e)
                    withContext(Dispatchers.Main) {
                        progressDialog.dismiss()
                    }
                }
            }
        }
    }

    private fun installApk(file: File) {
        val uri: Uri = FileProvider.getUriForFile(context, "com.neko.v2ray.updater", file)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
        }
        context.startActivity(intent)

        CoroutineScope(Dispatchers.IO).launch {
            delay(30_000)
            if (file.exists()) file.delete()
        }
    }

    private fun showNoUpdateDialog() {
        val message = context.getString(R.string.appupdater_update_not_available_description, context.getString(R.string.app_name))
        MaterialAlertDialogBuilder(context)
            .setTitle(R.string.appupdater_update_not_available)
            .setMessage(message)
            .setPositiveButton("OK", null)
            .show()
    }

    private data class UpdateResult(
        val updateAvailable: Boolean,
        val latestVersion: String?,
        val updateUrl: String?,
        val releaseNotes: String?
    )
}
