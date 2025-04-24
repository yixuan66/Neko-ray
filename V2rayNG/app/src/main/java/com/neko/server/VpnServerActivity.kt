package com.neko.server

import android.content.Intent
import android.os.*
import android.text.method.ScrollingMovementMethod
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.CollapsingToolbarLayout
import com.google.android.material.appbar.MaterialToolbar
import com.neko.v2ray.R
import com.neko.v2ray.BuildConfig
import com.neko.v2ray.ui.BaseActivity
import java.io.File
import java.io.FileWriter
import java.net.InetSocketAddress
import java.net.Socket
import kotlin.concurrent.thread

data class VpnServer(val name: String, var status: String = "Checking...", var ping: Int = -1)

class VpnServerActivity : BaseActivity() {

    private val servers = mutableListOf<VpnServer>()
    private lateinit var adapter: VpnServerAdapter
    private lateinit var pingManager: PingHistoryManager
    private var autoRefreshEnabled = true
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_vpn_server)

        val toolbar: MaterialToolbar = findViewById(R.id.toolbar)
        val toolbarLayout: CollapsingToolbarLayout = findViewById(R.id.collapsing_toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val etServerInput: EditText = findViewById(R.id.etServerInput)
        val recyclerView: RecyclerView = findViewById(R.id.recyclerView)

        pingManager = PingHistoryManager(this)

        adapter = VpnServerAdapter(servers) { server ->
            PingChartBottomSheet.newInstance(server.name).show(supportFragmentManager, "chart")
        }
        recyclerView.layoutManager = GridLayoutManager(this, 2)
        recyclerView.adapter = adapter

        val savedInput = getSharedPreferences("vpn_prefs", MODE_PRIVATE)
            .getString("server_input", "") ?: ""

        etServerInput.apply {
            isVerticalScrollBarEnabled = true
            movementMethod = ScrollingMovementMethod.getInstance()
            isScrollbarFadingEnabled = false
            maxLines = 5
            setText(savedInput)

            setOnTouchListener { v, event ->
                v.parent.requestDisallowInterceptTouchEvent(true)
                false
            }
        }

        if (savedInput.isNotBlank()) loadServers(savedInput)

        findViewById<Button>(R.id.btnLoadServers).setOnClickListener {
            val inputText = etServerInput.text.toString()
            if (inputText.isNotBlank()) {
                getSharedPreferences("vpn_prefs", MODE_PRIVATE).edit()
                    .putString("server_input", inputText).apply()
                loadServers(inputText)
            } else {
                Toast.makeText(this, "Please enter at least one server", Toast.LENGTH_SHORT).show()
            }
        }

        findViewById<Button>(R.id.btnSelectBest).setOnClickListener {
            val best = servers.filter { it.ping > 0 }.minByOrNull { it.ping }
            val message = best?.let { "Best Server: ${it.name}" } ?: "No online server found"
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        }

        findViewById<Button>(R.id.btnExport).setOnClickListener {
            val file = File(getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "vpn_history.csv")
            FileWriter(file).use { it.write(pingManager.exportToCsv()) }

            val uri = FileProvider.getUriForFile(
                this,
                BuildConfig.APPLICATION_ID + ".provider",
                file
            )

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            startActivity(Intent.createChooser(intent, "Export History"))
        }

        findViewById<ToggleButton>(R.id.btnToggleRefresh).apply {
            isChecked = true
            setOnCheckedChangeListener { _, isChecked ->
                autoRefreshEnabled = isChecked
                if (isChecked) startAutoRefresh() else stopAutoRefresh()
            }
        }

        startAutoRefresh()
    }

    private fun loadServers(inputText: String) {
        servers.clear()
        inputText.lines().map { it.trim() }.filter { it.isNotEmpty() }.forEach {
            val server = VpnServer(it)
            servers.add(server)
            pingServer(server)
        }
        adapter.notifyDataSetChanged()
    }

    private fun pingServer(server: VpnServer) {
        thread {
            val start = System.currentTimeMillis()
            val reachable = try {
                Socket().use {
                    it.connect(InetSocketAddress(server.name, 80), 1000)
                }
                true
            } catch (_: Exception) {
                false
            }
            val end = System.currentTimeMillis()

            runOnUiThread {
                server.status = if (reachable) "Online" else "Offline"
                server.ping = if (reachable) (end - start).toInt() else -1
                if (server.ping >= 0) pingManager.savePing(server.name, server.ping)
                adapter.notifyDataSetChanged()
            }
        }
    }

    private val refreshRunnable = object : Runnable {
        override fun run() {
            if (autoRefreshEnabled) {
                servers.forEach { pingServer(it) }
                handler.postDelayed(this, 60000)
            }
        }
    }

    private fun startAutoRefresh() {
        handler.post(refreshRunnable)
    }

    private fun stopAutoRefresh() {
        handler.removeCallbacks(refreshRunnable)
    }

    override fun onDestroy() {
        super.onDestroy()
        stopAutoRefresh()
    }
}
