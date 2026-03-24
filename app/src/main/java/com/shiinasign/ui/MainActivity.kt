package com.shiinasign.ui

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.shiinasign.R

class MainActivity : AppCompatActivity() {

    private lateinit var prefs: SharedPreferences
    private var serverRunning = false
    private val handler = Handler(Looper.getMainLooper())
    private var autoRefresh = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        prefs = getSharedPreferences("shiinasign", Context.MODE_PRIVATE)

        val etPort = findViewById<EditText>(R.id.et_port)
        val btnToggle = findViewById<MaterialButton>(R.id.btn_toggle)
        val tvStatus = findViewById<TextView>(R.id.tv_status)
        val etEcdh = findViewById<EditText>(R.id.et_ecdh)
        val btnRefreshEcdh = findViewById<MaterialButton>(R.id.btn_refresh_ecdh)

        etPort.setText(prefs.getInt("port", 7878).toString())

        btnToggle.setOnClickListener {
            if (serverRunning) {
                serverRunning = false
                autoRefresh = false
                btnToggle.text = "启动 HTTP Server"
                tvStatus.text = "状态: 已停止"
            } else {
                val port = etPort.text.toString().toIntOrNull() ?: 7878
                prefs.edit().putInt("port", port).apply()
                serverRunning = true
                autoRefresh = true
                btnToggle.text = "停止 HTTP Server"
                tvStatus.text = "状态: HTTP Server 将在 QQ 启动后运行\n端口: $port"
            }
        }

        btnRefreshEcdh.setOnClickListener {
            refreshEcdhDisplay(etEcdh)
        }

        // Auto-refresh ECDH display every 2 seconds when server is running
        val autoRefreshRunnable = object : Runnable {
            override fun run() {
                if (autoRefresh) {
                    refreshEcdhDisplay(etEcdh)
                    handler.postDelayed(this, 2000)
                }
            }
        }
        handler.postDelayed(autoRefreshRunnable, 2000)
    }

    private fun refreshEcdhDisplay(etEcdh: EditText) {
        try {
            // Try to fetch from HTTP endpoint
            val port = prefs.getInt("port", 7878)
            Thread {
                try {
                    val url = java.net.URL("http://127.0.0.1:$port/ecdh")
                    val conn = url.openConnection() as java.net.HttpURLConnection
                    conn.connectTimeout = 1000
                    conn.readTimeout = 1000
                    val code = conn.responseCode
                    if (code == 200) {
                        val response = conn.inputStream.bufferedReader().readText()
                        // Parse JSON and format
                        val json = org.json.JSONObject(response)
                        val sb = StringBuilder()
                        sb.appendLine("=== ECDH Capture ===")
                        sb.appendLine("Status: ${if (json.optBoolean("initialized")) "Active" else "Inactive"}")
                        sb.appendLine("Capture count: ${json.optInt("capture_count")}")
                        val lastTime = json.optLong("last_capture_time")
                        if (lastTime > 0) {
                            sb.appendLine("Last capture: ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", java.util.Locale.US).format(java.util.Date(lastTime))}")
                        } else {
                            sb.appendLine("Last capture: N/A")
                        }
                        sb.appendLine()
                        sb.appendLine("[Shared Secret]")
                        val secret = json.optString("shared_secret", "")
                        val secretLen = json.optInt("shared_secret_length")
                        if (secret.isNotEmpty()) {
                            sb.appendLine("Length: $secretLen bytes")
                            sb.appendLine("Hex: $secret")
                        } else {
                            sb.appendLine("(not yet captured)")
                        }
                        sb.appendLine()
                        sb.appendLine("[Server Public Key]")
                        val pubKey = json.optString("server_public_key", "")
                        if (pubKey.isNotEmpty()) {
                            sb.appendLine("Hex: $pubKey")
                        } else {
                            sb.appendLine("(not yet captured)")
                        }
                        handler.post { etEcdh.setText(sb.toString()) }
                    } else {
                        handler.post { etEcdh.setText("HTTP $code - Server not responding") }
                    }
                    conn.disconnect()
                } catch (e: Exception) {
                    handler.post { etEcdh.setText("Cannot connect to server.\nMake sure QQ is running with the module active.") }
                }
            }.start()
        } catch (_: Exception) {}
    }

    override fun onDestroy() {
        super.onDestroy()
        autoRefresh = false
        handler.removeCallbacksAndMessages(null)
    }
}
