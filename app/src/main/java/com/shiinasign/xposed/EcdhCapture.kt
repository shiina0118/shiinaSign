package com.shiinasign.xposed

import de.robv.android.xposed.XposedBridge

/**
 * Stores captured ECDH parameters from native hook.
 * Written by JNI code when ECDH_compute_key is intercepted.
 */
object EcdhCapture {

    @Volatile
    var sharedSecret: String = ""

    @Volatile
    var sharedSecretLength: Int = 0

    @Volatile
    var serverPublicKey: String = ""

    @Volatile
    var lastCaptureTime: Long = 0

    @Volatile
    var captureCount: Int = 0

    @Volatile
    var initialized: Boolean = false

    val summary: String
        get() = buildString {
            appendLine("=== ECDH Capture (${if (initialized) "Active" else "Inactive"}) ===")
            appendLine("Capture count: $captureCount")
            appendLine("Last capture: ${if (lastCaptureTime > 0) java.text.SimpleDateFormat("HH:mm:ss.SSS", java.util.Locale.US).format(java.util.Date(lastCaptureTime)) else "N/A"}")
            appendLine()
            appendLine("[Shared Secret]")
            if (sharedSecret.isNotEmpty()) {
                appendLine("Length: $sharedSecretLength bytes")
                appendLine("Hex: $sharedSecret")
            } else {
                appendLine("(not yet captured)")
            }
            appendLine()
            appendLine("[Server Public Key]")
            if (serverPublicKey.isNotEmpty()) {
                appendLine("Hex: $serverPublicKey")
            } else {
                appendLine("(not yet captured)")
            }
        }

    /**
     * Called from JNI to store captured data.
     */
    @JvmStatic
    fun onEcdhComputed(
        sharedSecretHex: String,
        secretLen: Int,
        serverPubKeyHex: String
    ) {
        sharedSecret = sharedSecretHex
        sharedSecretLength = secretLen
        serverPublicKey = serverPubKeyHex
        lastCaptureTime = System.currentTimeMillis()
        captureCount++
        XposedBridge.log("[shiinaSign] ECDH captured: secret_len=$secretLen, count=$captureCount")
    }

    fun reset() {
        sharedSecret = ""
        sharedSecretLength = 0
        serverPublicKey = ""
        lastCaptureTime = 0
        captureCount = 0
    }
}
