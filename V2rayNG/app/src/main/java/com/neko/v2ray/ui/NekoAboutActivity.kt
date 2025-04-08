package com.neko.v2ray.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.CollapsingToolbarLayout
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.mikepenz.aboutlibraries.LibsBuilder
import com.neko.appupdater.AppUpdater
import com.neko.changelog.ChangelogAdapter
import com.neko.changelog.ChangelogEntry
import com.neko.nointernet.callbacks.ConnectionCallback
import com.neko.nointernet.dialogs.signal.NoInternetDialogSignal
import com.neko.v2ray.AppConfig
import com.neko.v2ray.BuildConfig
import com.neko.v2ray.R
import com.neko.v2ray.extension.toast
import com.neko.v2ray.util.Utils

class NekoAboutActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.uwu_about_activity)

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        val toolbarLayout = findViewById<CollapsingToolbarLayout>(R.id.collapsing_toolbar)

        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        supportFragmentManager.beginTransaction()
            .replace(R.id.content_wrapper, NekoAboutFragment())
            .commit()
    }

    fun uwuUpdater(view: View) {
        startNoInternetDialog()

        AppUpdater(this).apply {
            configUrl = AppConfig.UWU_UPDATE_URL
            showIfUpToDate = true
            onUpdateAvailable = {
                // Optional: aksi tambahan jika update tersedia
            }
            onUpdateNotAvailable = {
                // Optional: aksi jika tidak ada update
            }
            checkForUpdate()
        }
    }

    fun uwuRepository(view: View) {
        Utils.openUri(this, AppConfig.APP_URL)
    }

    fun promotion(view: View) {
        Utils.openUri(this, AppConfig.APP_PROMOTION_URL)
    }

    fun license(view: View) {
        LibsBuilder()
            .withSortEnabled(true)
            .withLicenseShown(true)
            .withLicenseDialog(true)
            .withVersionShown(true)
            .withAboutIconShown(true)
            .withAboutMinimalDesign(false)
            .withAboutVersionShown(true)
            .withAboutVersionShownName(true)
            .withAboutVersionShownCode(true)
            .withSearchEnabled(true)
            .withActivityTitle(getString(R.string.uwu_license_title))
            .start(this)
    }

    fun privacypolicy(view: View) {
        Utils.openUri(this, AppConfig.APP_PRIVACY_POLICY)
    }

    fun uwumodder(view: View) {
        Utils.openUri(this, AppConfig.TG_CHANNEL_URL)
    }

    fun uwuCredits(view: View) {
        startActivity(Intent(this, CreditsActivity::class.java))
    }

    fun changelog(view: View) {
        showChangelogBottomSheet()
    }

    private fun startNoInternetDialog() {
        NoInternetDialogSignal.Builder(this, lifecycle).apply {
            dialogProperties.apply {
                connectionCallback = object : ConnectionCallback {
                    override fun hasActiveConnection(hasActiveConnection: Boolean) {
                        // Optionally handle connection changes
                    }
                }
                cancelable = true
                showInternetOnButtons = true
                showAirplaneModeOffButtons = true
            }
        }.build()
    }

    private fun showChangelogBottomSheet() {
        val bottomSheetDialog = BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.changelog_bottom_sheet, null)
        bottomSheetDialog.setContentView(view)

        val recyclerView = view.findViewById<RecyclerView>(R.id.changelogRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = ChangelogAdapter(loadChangelogFromAssets())

        bottomSheetDialog.show()
    }

    private fun loadChangelogFromAssets(): List<ChangelogEntry> {
        val json = assets.open("changelog.json").bufferedReader().use { it.readText() }
        val type = object : TypeToken<List<ChangelogEntry>>() {}.type
        return Gson().fromJson(json, type)
    }
}
