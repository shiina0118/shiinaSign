package com.shiinasign.ui

import android.os.Bundle
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.shiinasign.R

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val etPort = findViewById<EditText>(R.id.et_port)
        val btnToggle = findViewById<MaterialButton>(R.id.btn_toggle)
        val tvStatus = findViewById<TextView>(R.id.tv_status)

        tvStatus.text = "状态: Xposed 模块\n请在 LSPosed 中激活后启动 QQ"

        btnToggle.setOnClickListener {
            tvStatus.text = "状态: HTTP Server 已在 QQ 进程中启动\n请通过 POST http://localhost:7878/sign 调用"
        }
    }
}
