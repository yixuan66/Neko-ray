package com.neko.hosttoip

import android.os.Bundle
import android.widget.*
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.appbar.CollapsingToolbarLayout
import com.neko.v2ray.R
import com.neko.v2ray.ui.BaseActivity
import com.neko.v2ray.util.SoftInputAssist
import kotlinx.coroutines.*
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.InetAddress
import java.net.URL

class HostToIPActivity : BaseActivity() {

    private lateinit var hostInput: EditText
    private lateinit var resolveButton: Button
    private lateinit var resultText: TextView
    private lateinit var softInputAssist: SoftInputAssist
    private val coroutineScope = CoroutineScope(Dispatchers.Main)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_host_to_ip)

        val toolbar: MaterialToolbar = findViewById(R.id.toolbar)
        val toolbarLayout: CollapsingToolbarLayout = findViewById(R.id.collapsing_toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        softInputAssist = SoftInputAssist(this)

        hostInput = findViewById(R.id.host_input)
        resolveButton = findViewById(R.id.resolve_button)
        resultText = findViewById(R.id.result_text)

        resolveButton.setOnClickListener {
            val host = hostInput.text.toString().trim()
            if (host.isNotEmpty()) {
                resolveHostInfo(host)
            } else {
                Toast.makeText(this, "Please enter a hostname", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun resolveHostInfo(host: String) {
        coroutineScope.launch {
            resultText.text = "Resolving..."
            val result = withContext(Dispatchers.IO) {
                try {
                    val ipAddress = InetAddress.getByName(host).hostAddress
                    val info = getIpDetails(ipAddress)
                    """
                        Hostname: $host
                        IP Address: $ipAddress
                        Country: ${info["country"]}
                        City: ${info["city"]}
                        ISP Provider: ${info["isp"]}
                        ISP Provider2: ${info["org"]}
                        ISP Provider3: ${info["as"]}
                    """.trimIndent()
                } catch (e: Exception) {
                    "Error: ${e.message}"
                }
            }
            resultText.text = result
        }
    }

    private fun getIpDetails(ip: String): Map<String, String> {
        val url = URL("http://ip-api.com/json/$ip")
        val connection = url.openConnection() as HttpURLConnection
        connection.connectTimeout = 5000
        connection.readTimeout = 5000
        connection.requestMethod = "GET"
        connection.connect()

        val response = connection.inputStream.bufferedReader().readText()
        val json = JSONObject(response)

        return if (json.getString("status") == "success") {
            mapOf(
                "country" to json.optString("country", "Unknown"),
                "city" to json.optString("city", "Unknown"),
                "isp" to json.optString("isp", "Unknown"),
                "org" to json.optString("org", "Unknown"),
                "as" to json.optString("as", "Unknown")
            )
        } else {
            mapOf(
                "country" to "Unknown",
                "city" to "Unknown",
                "isp" to "Unknown",
                "org" to "Unknown",
                "as" to "Unknown"
            )
        }
    }
    
    override fun onResume() {
        softInputAssist.onResume()
        super.onResume()
    }

    override fun onPause() {
        softInputAssist.onPause()
        super.onPause()
    }

    override fun onDestroy() {
        softInputAssist.onDestroy()
        coroutineScope.cancel()
        super.onDestroy()
    }
}
