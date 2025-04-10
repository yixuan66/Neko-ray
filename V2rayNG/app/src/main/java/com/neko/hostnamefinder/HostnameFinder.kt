package com.neko.hostnamefinder

import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.text.Html
import android.widget.*
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.appbar.CollapsingToolbarLayout
import com.google.android.material.appbar.MaterialToolbar
import com.neko.v2ray.R
import com.neko.v2ray.ui.BaseActivity
import org.json.JSONObject
import java.io.*
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.*

class HostnameFinder : BaseActivity() {

    companion object {
        private const val PERMISSION_REQUEST_CODE = 1
        private const val REQUEST_MANAGE_EXTERNAL_STORAGE = 3123
    }

    private lateinit var copyButton: ImageView
    private lateinit var ipInput: EditText
    private lateinit var resultOutput: EditText
    private lateinit var resultCount: TextView
    private lateinit var scanButton: Button
    private lateinit var saveLocal: CompoundButton
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.uwu_hostname_finder)

        initializeUI()
    }

    // Sets up the toolbar and initializes view references
    private fun initializeUI() {
        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        val toolbarLayout = findViewById<CollapsingToolbarLayout>(R.id.collapsing_toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        ipInput = findViewById(R.id.ip_input)
        resultOutput = findViewById(R.id.result_output)
        resultCount = findViewById(R.id.result_count)
        scanButton = findViewById(R.id.scan_button)
        copyButton = findViewById(R.id.copy_button)
        saveLocal = findViewById(R.id.save_local)

        scanButton.setOnClickListener { scan() }
        copyButton.setOnClickListener { copyText(resultOutput.text.toString()) }
        saveLocal.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked && !requestStoragePermission()) {
                saveLocal.isChecked = false
            }
        }
    }

    // Starts the scan process in a background thread
    private fun scan() {
        val ip = ipInput.text.toString().trim()
        if (ip.isEmpty()) {
            showToast("Please enter the IP or Hostname first")
            return
        }

        resultOutput.setText("")
        resultCount.text = "Searching..."
        showToast("Searching...")

        Thread { fetchHostnames(ip) }.start()
    }

    // Performs the POST request to fetch hostnames
    private fun fetchHostnames(ip: String) {
        try {
            val url = URL("https://domains.yougetsignal.com/domains.php")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.setRequestProperty("X-Requested-With", "XMLHttpRequest")
            connection.doOutput = true

            val output = "remoteAddress=" + URLEncoder.encode(ip, "UTF-8")
            connection.outputStream.write(output.toByteArray(Charsets.UTF_8))

            val response = BufferedReader(InputStreamReader(connection.inputStream)).use { it.readLine() }
            handleResponse(response)
        } catch (e: Exception) {
            showError(e.message.orEmpty())
        }
    }

    // Handles the response from the API
    private fun handleResponse(response: String) {
        handler.post {
            try {
                val jsonObject = JSONObject(response)
                val status = jsonObject.getString("status")

                if (status == "Fail") {
                    val message = jsonObject.getString("message")
                    resultOutput.setText(message)
                    resultCount.text = Html.fromHtml("<span style='color:#ff0000'>$message</span>")
                    return@post
                }

                val domainArray = jsonObject.getJSONArray("domainArray")
                val resultBuilder = StringBuilder()

                for (i in 0 until domainArray.length()) {
                    resultBuilder.append(
                        domainArray.getString(i)
                            .replace("[\"", "")
                            .replace("\",\"\"]", "")
                    ).append("\n")
                }

                val resultText = resultBuilder.toString()
                resultOutput.setText(resultText)
                resultCount.text = "Results Found: ${jsonObject.getString("domainCount")}"

                if (saveLocal.isChecked) {
                    saveToFile(resultText)
                }
            } catch (e: Exception) {
                showError(e.message.orEmpty())
            }
        }
    }

    // Copies the given text to the clipboard
    private fun copyText(text: String) {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        clipboard?.text = text
        showToast("Results have been copied")
    }

    // Requests storage permissions (for API < 30 or manage-all for API >= 30)
    private fun requestStoragePermission(): Boolean {
        return if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE) == 0) {
                true
            } else {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(android.Manifest.permission.WRITE_EXTERNAL_STORAGE),
                    PERMISSION_REQUEST_CODE
                )
                false
            }
        } else {
            if (Environment.isExternalStorageManager()) {
                true
            } else {
                val intent = Intent(android.provider.Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                    data = Uri.fromParts("package", packageName, null)
                }
                startActivityForResult(intent, REQUEST_MANAGE_EXTERNAL_STORAGE)
                false
            }
        }
    }

    // Saves result to external storage
    private fun saveToFile(content: String) {
        val directory = File(Environment.getExternalStorageDirectory(), "Hostname Finder")
        if (!directory.exists()) directory.mkdirs()

        val file = File(directory, String.format("%05d.txt", Random().nextInt(100000)))
        try {
            FileWriter(file, true).use { it.write(content) }
            showToast("Saved in: ${file.path}")
        } catch (e: IOException) {
            showError(e.message.orEmpty())
        }
    }

    // Shows a toast from the UI thread
    private fun showToast(message: String) {
        handler.post {
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        }
    }

    // Displays error messages in the result output
    private fun showError(error: String) {
        handler.post {
            resultOutput.setText(error)
            resultCount.text = ""
        }
    }
}
