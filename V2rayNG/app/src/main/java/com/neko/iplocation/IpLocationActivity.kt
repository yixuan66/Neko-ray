package com.neko.iplocation

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.os.Bundle
import android.text.format.Formatter
import android.widget.ListView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.appbar.CollapsingToolbarLayout
import com.neko.v2ray.AppConfig
import com.neko.v2ray.R
import com.neko.v2ray.ui.BaseActivity
import kotlinx.coroutines.*
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.NetworkInterface
import java.util.*

class IpLocationActivity : BaseActivity() {

    private lateinit var listView: ListView
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private val client = OkHttpClient()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ip_location)

        val toolbar: MaterialToolbar = findViewById(R.id.toolbar)
        val toolbarLayout: CollapsingToolbarLayout = findViewById(R.id.collapsing_toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        listView = findViewById(R.id.listView)
        swipeRefresh = findViewById(R.id.swipeRefresh)

        swipeRefresh.setOnRefreshListener {
            fetchAndShowData()
        }

        fetchAndShowData()
    }

    private fun fetchAndShowData() {
        swipeRefresh.isRefreshing = true

        CoroutineScope(Dispatchers.IO).launch {
            val items = mutableListOf<KeyValueItem>()

            try {
                val request = Request.Builder().url(AppConfig.IP_API_URL).build()
                val response = client.newCall(request).execute()
                val json = JSONObject(response.body?.string() ?: "{}")

                val keys = json.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    val value = json.optString(key)
                    items.add(KeyValueItem(key, value))
                }
            } catch (e: Exception) {
                items.add(KeyValueItem("Error", e.message ?: "Unknown error"))
            }

            items.add(KeyValueItem("local_ip", getLocalIpAddress()))
            getWifiSSID()?.let { items.add(KeyValueItem("wifi_ssid", it)) }
            items.add(KeyValueItem("connection_type", getConnectionType()))
            items.add(KeyValueItem("vpn_active", if (isUsingVPN()) "Yes" else "No"))

            withContext(Dispatchers.Main) {
                listView.adapter = KeyValueAdapter(this@IpLocationActivity, items)
                swipeRefresh.isRefreshing = false
            }
        }
    }

    private fun getLocalIpAddress(): String {
        return try {
            val wm = applicationContext.getSystemService(WIFI_SERVICE) as WifiManager
            Formatter.formatIpAddress(wm.connectionInfo.ipAddress)
        } catch (e: Exception) {
            getIpFromInterfaces()
        }
    }

    private fun getIpFromInterfaces(): String {
        return try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            for (intf in Collections.list(interfaces)) {
                for (addr in Collections.list(intf.inetAddresses)) {
                    if (!addr.isLoopbackAddress && addr.hostAddress.indexOf(':') < 0) {
                        return addr.hostAddress ?: "Unavailable"
                    }
                }
            }
            "Unavailable"
        } catch (e: Exception) {
            "Unavailable"
        }
    }

    private fun getWifiSSID(): String? {
        return try {
            val wifiManager = applicationContext.getSystemService(WIFI_SERVICE) as WifiManager
            val info = wifiManager.connectionInfo
            val ssid = info.ssid
            if (ssid != null && ssid != "<unknown ssid>") ssid.replace("\"", "") else null
        } catch (e: Exception) {
            null
        }
    }

    private fun getConnectionType(): String {
        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return "No Connection"
        val capabilities = cm.getNetworkCapabilities(network) ?: return "Unknown"

        return when {
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "Wi-Fi"
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "Mobile"
            else -> "Other"
        }
    }

    private fun isUsingVPN(): Boolean {
        return try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            for (intf in Collections.list(interfaces)) {
                if (intf.isUp && intf.name.equals("tun0", ignoreCase = true)) {
                    return true
                }
            }
            false
        } catch (e: Exception) {
            false
        }
    }
}
