package com.shiinasign.ui

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.shiinasign.R

class MainActivity : AppCompatActivity() {

    private lateinit var prefs: SharedPreferences
    private var serverRunning = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        prefs = getSharedPreferences("shiinasign", Context.MODE_PRIVATE)

        val etPort = findViewById<EditText>(R.id.et_port)
        val btnToggle = findViewById<MaterialButton>(R.id.btn_toggle)
        val tvStatus = findViewById<TextView>(R.id.tv_status)

        etPort.setText(prefs.getInt("port", 7878).toString())

        btnToggle.setOnClickListener {
            if (serverRunning) {
                serverRunning = false
                btnToggle.text = "启动 HTTP Server"
                tvStatus.text = "状态: 已停止"
            } else {
                val port = etPort.text.toString().toIntOrNull() ?: 7878
                prefs.edit().putInt("port", port).apply()
                serverRunning = true
                btnToggle.text = "停止 HTTP Server"
                tvStatus.text = "状态: HTTP Server 将在 QQ 启动后运行\n端口: $port\n\nPOST http://localhost:$port/sign\n参数: cmd, buffer(hex), seq, uin"
            }
        }
    }
}
