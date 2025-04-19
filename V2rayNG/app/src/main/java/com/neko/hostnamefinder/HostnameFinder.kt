package com.neko.hostnamefinder

import android.content.ClipData
import android.content.ClipboardManager
import android.os.Bundle
import android.util.Base64
import android.view.inputmethod.EditorInfo
import android.widget.*
import androidx.lifecycle.lifecycleScope
import com.google.android.material.appbar.CollapsingToolbarLayout
import com.google.android.material.appbar.MaterialToolbar
import com.neko.v2ray.R
import com.neko.v2ray.ui.BaseActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.InetAddress
import java.net.URL

class HostnameFinder : BaseActivity() {

    private lateinit var edtHost: EditText
    private lateinit var btnSearch: Button
    private lateinit var txtResults: TextView
    private lateinit var txtResultTitle: TextView
    private lateinit var btnCopy: ImageView
    private lateinit var spinnerSource: Spinner

    private val sources = listOf("Local DNS", "crt.sh", "VirusTotal", "YouGetSignal")
    private val virusTotalApiKey = "NjhlMDUzYjc5NmI3OWE1YzBiNDczYWJhZDFjNTVkYWY2ZWRlNGU4M2VjMWNkZmMwNTUxYTVhODRkOGEyZjIzMw=="

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.uwu_hostname_finder)

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        val toolbarLayout = findViewById<CollapsingToolbarLayout>(R.id.collapsing_toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        edtHost = findViewById(R.id.edtHost)
        btnSearch = findViewById(R.id.btnSearch)
        txtResults = findViewById(R.id.txtResults)
        txtResultTitle = findViewById(R.id.txtResultTitle)
        btnCopy = findViewById(R.id.btnCopy)
        spinnerSource = findViewById(R.id.spinnerSource)

        spinnerSource.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, sources)

        btnSearch.setOnClickListener { performSearch() }

        edtHost.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                performSearch()
                true
            } else false
        }

        btnCopy.setOnClickListener {
            val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("hostnames", txtResults.text)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(this, "Copied to clipboard!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun performSearch() {
        val host = edtHost.text.toString().trim()
        if (host.isEmpty()) {
            edtHost.error = "Please input a host"
            return
        }

        txtResults.text = "Loading..."
        txtResultTitle.text = "results : Please wait..."

        when (spinnerSource.selectedItem.toString()) {
            "Local DNS" -> searchWithDns(host)
            "crt.sh" -> fetchFromCrtSh(host)
            "VirusTotal" -> fetchFromVirusTotal(host)
            "YouGetSignal" -> fetchFromYouGetSignal(host)
        }
    }

    private fun searchWithDns(domain: String) {
        lifecycleScope.launch {
            try {
                val addresses = withContext(Dispatchers.IO) {
                    InetAddress.getAllByName(domain).map { it.hostAddress }.distinct()
                }
                txtResults.text = addresses.joinToString("\n")
                txtResultTitle.text = "results : Found ${addresses.size} IP(s)"
            } catch (e: Exception) {
                txtResults.text = "Error: ${e.localizedMessage}"
                txtResultTitle.text = "results : Error"
            }
        }
    }

    private fun fetchFromCrtSh(domain: String) {
        lifecycleScope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    val url = "https://crt.sh/?q=%25.$domain&output=json"
                    val conn = URL(url).openConnection() as HttpURLConnection
                    conn.inputStream.bufferedReader().use { it.readText() }
                }
                val entries = Regex("\"common_name\":\"(.*?)\"").findAll(result).map { it.groupValues[1] }.toSet()
                txtResults.text = entries.sorted().joinToString("\n")
                txtResultTitle.text = "results : Found ${entries.size} hostnames"
            } catch (e: Exception) {
                txtResults.text = "Error from crt.sh: ${e.localizedMessage}"
                txtResultTitle.text = "results : Error"
            }
        }
    }

    private fun fetchFromVirusTotal(domain: String) {
        lifecycleScope.launch {
            try {
                val apiKey = String(Base64.decode(virusTotalApiKey, Base64.DEFAULT))

                val result = withContext(Dispatchers.IO) {
                    val url = "https://www.virustotal.com/api/v3/domains/$domain/subdomains?limit=40"
                    val conn = URL(url).openConnection() as HttpURLConnection
                    conn.setRequestProperty("x-apikey", apiKey)
                    conn.inputStream.bufferedReader().use { it.readText() }
                }

                val data = JSONObject(result).getJSONArray("data")
                val subdomains = (0 until data.length()).map { i ->
                    data.getJSONObject(i).getString("id")
                }

                txtResults.text = subdomains.sorted().joinToString("\n")
                txtResultTitle.text = "results : Found ${subdomains.size} subdomains"
            } catch (e: Exception) {
                txtResults.text = "Error from VirusTotal: ${e.localizedMessage}"
                txtResultTitle.text = "results : Error"
            }
        }
    }

    private fun fetchFromYouGetSignal(domain: String) {
        lifecycleScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    val url = URL("https://domains.yougetsignal.com/domains.php")
                    val conn = url.openConnection() as HttpURLConnection
                    conn.requestMethod = "POST"
                    conn.doOutput = true
                    conn.outputStream.bufferedWriter().use { it.write("remoteAddress=$domain") }
                    conn.inputStream.bufferedReader().use { it.readText() }
                }

                val json = JSONObject(response)
                val pairs = json.getJSONArray("domainArray")
                val domains = (0 until pairs.length()).map { i ->
                    pairs.getJSONArray(i).getString(0)
                }

                txtResults.text = domains.sorted().joinToString("\n")
                txtResultTitle.text = "results : Found ${domains.size} domains"
            } catch (e: Exception) {
                txtResults.text = "Error from YouGetSignal: ${e.localizedMessage}"
                txtResultTitle.text = "results : Error"
            }
        }
    }
}
