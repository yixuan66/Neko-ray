package com.neko.server

import android.content.Intent
import android.os.Bundle
import android.os.Environment
import android.text.method.ScrollingMovementMethod
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.CollapsingToolbarLayout
import com.google.android.material.appbar.MaterialToolbar
import com.neko.v2ray.R
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_vpn_server)

        val toolbar: MaterialToolbar = findViewById(R.id.toolbar)
        val toolbarLayout: CollapsingToolbarLayout = findViewById(R.id.collapsing_toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val etServerInput: EditText = findViewById(R.id.etServerInput)
        val recyclerView: RecyclerView = findViewById(R.id.recyclerView)

        adapter = VpnServerAdapter(servers)
        recyclerView.layoutManager = GridLayoutManager(this, 2)
        recyclerView.adapter = adapter

        val savedInput = getSharedPreferences("vpn_prefs", MODE_PRIVATE)
            .getString("server_input", "") ?: ""

        etServerInput.apply {
            isVerticalScrollBarEnabled = true
            movementMethod = ScrollingMovementMethod.getInstance()  // Enable text scrolling
            maxLines = 5
            isScrollbarFadingEnabled = false
            setOnTouchListener { v, _ ->
                v.parent.requestDisallowInterceptTouchEvent(true)
                false
            }
            setText(savedInput)
        }

        if (savedInput.isNotBlank()) {
            servers.clear()
            savedInput.lines().map { it.trim() }.filter { it.isNotEmpty() }.forEach {
                val server = VpnServer(it)
                servers.add(server)
                pingServer(server)
            }
            adapter.notifyDataSetChanged()
        }

        val btnLoad: Button = findViewById(R.id.btnLoadServers)
        val btnBest: Button = findViewById(R.id.btnSelectBest)
        val btnExport: Button = findViewById(R.id.btnExport)

        btnLoad.setOnClickListener {
            val inputText = etServerInput.text.toString()
            if (inputText.isNotBlank()) {
                getSharedPreferences("vpn_prefs", MODE_PRIVATE).edit()
                    .putString("server_input", inputText).apply()

                servers.clear()
                inputText.lines().map { it.trim() }.filter { it.isNotEmpty() }.forEach {
                    val server = VpnServer(it)
                    servers.add(server)
                    pingServer(server)
                }
                adapter.notifyDataSetChanged()
            } else {
                Toast.makeText(this, "Please enter at least one server", Toast.LENGTH_SHORT).show()
            }
        }

        btnBest.setOnClickListener {
            val best = servers.filter { it.ping > 0 }.minByOrNull { it.ping }
            val message = best?.let { "Best Server: ${it.name}" } ?: "No online server found"
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        }

        btnExport.setOnClickListener {
            exportToCsv()
        }
    }

    private fun pingServer(server: VpnServer) {
        thread {
            val start = System.currentTimeMillis()
            val reachable = try {
                Socket().use {
                    it.connect(InetSocketAddress(server.name, 80), 1000)
                }
                true
            } catch (e: Exception) {
                false
            }
            val end = System.currentTimeMillis()

            runOnUiThread {
                server.status = if (reachable) "Online" else "Offline"
                server.ping = if (reachable) (end - start).toInt() else -1
                adapter.notifyDataSetChanged()
            }
        }
    }

    private fun exportToCsv() {
        val file = File(getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "vpn_servers.csv")
        FileWriter(file).use { writer ->
            writer.append("Server,Status,Ping\n")
            servers.forEach {
                writer.append("${it.name},${it.status},${it.ping}\n")
            }
        }

        val uri = FileProvider.getUriForFile(
            this,
            "com.neko.v2ray.monitor",
            file
        )

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        startActivity(Intent.createChooser(intent, "Export CSV"))
    }
}
