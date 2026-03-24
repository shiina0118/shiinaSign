package com.shiinasign.server

import com.google.gson.Gson
import com.shiinasign.xposed.DeviceInfo
import com.shiinasign.xposed.EcdhCapture
import com.shiinasign.xposed.QimeiHook
import com.shiinasign.xposed.QimeiResult
import de.robv.android.xposed.XposedBridge
import fi.iki.elonen.NanoHTTPD

/**
 * NanoHTTPD-based HTTP server providing active getSign endpoint.
 * Must be called from QQ's MSF process after ClassLoader injection.
 */
class SignServer(port: Int) : NanoHTTPD(port) {

    private val gson = Gson()

    override fun serve(session: IHTTPSession): Response {
        return when (session.uri) {
            "/sign" -> handleSign(session)
            "/qimei" -> handleQimei(session)
            "/ecdh" -> handleEcdh()
            "/ping" -> newFixedLengthResponse(Response.Status.OK, "application/json", """{"status":"ok"}""")
            else -> newFixedLengthResponse(Response.Status.NOT_FOUND, "application/json", """{"error":"not found"}""")
        }
    }

    private fun handleSign(session: IHTTPSession): Response {
        session.parseBody(mapOf())
        val params = session.parameters
        val cmd = params["cmd"]
        val bufferHex = params["buffer"]
        val seqStr = params["seq"]
        val uin = params["uin"]

        if (cmd.isNullOrEmpty() || bufferHex.isNullOrEmpty() || seqStr.isNullOrEmpty() || uin.isNullOrEmpty()) {
            return newFixedLengthResponse(
                Response.Status.BAD_REQUEST, "application/json",
                """{"error":"missing params: cmd, buffer, seq, uin"}"""
            )
        }

        val seq = seqStr.toIntOrNull()
        if (seq == null) {
            return newFixedLengthResponse(
                Response.Status.BAD_REQUEST, "application/json",
                """{"error":"seq must be integer"}"""
            )
        }

        val buffer = hex2ByteArray(bufferHex)
        if (buffer == null) {
            return newFixedLengthResponse(
                Response.Status.BAD_REQUEST, "application/json",
                """{"error":"invalid hex buffer"}"""
            )
        }

        return try {
            val result = doSign(cmd, buffer, seq, uin)
            val response = SignResponse(
                ret = 0,
                msg = "success",
                data = SignData(
                    token = result.token,
                    extra = result.extra,
                    sign = result.sign
                )
            )
            newFixedLengthResponse(Response.Status.OK, "application/json", gson.toJson(response))
        } catch (e: Exception) {
            XposedBridge.log("[shiinaSign] sign error: ${e.message}")
            newFixedLengthResponse(
                Response.Status.INTERNAL_ERROR, "application/json",
                """{"ret":-1,"error":"${e.message?.replace("\"", "\\\"") ?: "unknown"}"}"""
            )
        }
    }

    private fun handleQimei(session: IHTTPSession): Response {
        session.parseBody(mapOf())

        val params = session.parameters

        // Check if custom device info is provided
        val hasDeviceInfo = params.keys.any { it in listOf(
            "imei", "android_id", "mac", "model", "brand",
            "manufacturer", "fingerprint", "boot_id", "proc_version",
            "sdk_version", "display", "device", "board"
        )}

        return try {
            val result: QimeiResult = if (hasDeviceInfo) {
                // Generate with custom device info
                val deviceInfo = DeviceInfo(
                    imei = params["imei"] ?: "",
                    androidId = params["android_id"] ?: "",
                    mac = params["mac"] ?: "",
                    model = params["model"] ?: "",
                    brand = params["brand"] ?: "",
                    manufacturer = params["manufacturer"] ?: "",
                    fingerprint = params["fingerprint"] ?: "",
                    bootId = params["boot_id"] ?: "",
                    procVersion = params["proc_version"] ?: "",
                    sdkVersion = params["sdk_version"] ?: "",
                    display = params["display"] ?: "",
                    device = params["device"] ?: "",
                    board = params["board"] ?: ""
                )
                QimeiHook.generateQimei(deviceInfo)
            } else {
                // Get current QIMEI without custom info
                QimeiHook.getCurrentQimei()
            }

            val response = mapOf(
                "ret" to 0,
                "qimei16" to (result.qimei16 ?: ""),
                "qimei36" to (result.qimei36 ?: "")
            )
            newFixedLengthResponse(Response.Status.OK, "application/json", gson.toJson(response))
        } catch (e: Exception) {
            XposedBridge.log("[shiinaSign] qimei error: ${e.message}")
            newFixedLengthResponse(
                Response.Status.INTERNAL_ERROR, "application/json",
                """{"ret":-1,"error":"${e.message?.replace("\"", "\\\"") ?: "unknown"}"}"""
            )
        }
    }

    private fun handleEcdh(): Response {
        val capture = EcdhCapture
        val response = mapOf(
            "ret" to 0,
            "initialized" to capture.initialized,
            "capture_count" to capture.captureCount,
            "last_capture_time" to capture.lastCaptureTime,
            "shared_secret" to capture.sharedSecret,
            "shared_secret_length" to capture.sharedSecretLength,
            "server_public_key" to capture.serverPublicKey
        )
        return newFixedLengthResponse(Response.Status.OK, "application/json", gson.toJson(response))
    }

    companion object {
        /**
         * Call FEKit.getSign via pure reflection.
         * Avoids classloader conflict with stub classes.
         */
        fun doSign(cmd: String, buffer: ByteArray, seq: Int, uin: String): FEKitResult {
            val classLoader = Thread.currentThread().contextClassLoader
                ?: throw IllegalStateException("contextClassLoader is null")

            // FEKit.getInstance()
            val feKitClass = classLoader.loadClass("com.tencent.mobileqq.fe.FEKit")
            val getInstance = feKitClass.getDeclaredMethod("getInstance")
            val feKit = getInstance.invoke(null)
                ?: throw IllegalStateException("FEKit.getInstance() returned null")

            // Get Context
            val context = try {
                val appClass = classLoader.loadClass("com.tencent.common.app.BaseApplicationImpl")
                val field = appClass.declaredFields.first { it.type == appClass }
                field.isAccessible = true
                field.get(null) as android.content.Context
            } catch (e: Exception) {
                throw IllegalStateException("Cannot get QQ Context: ${e.message}")
            }

            // Get GUID
            val guid = try {
                val utilClass = classLoader.loadClass("oicq.wlogin_sdk.tools.util")
                val method = utilClass.getDeclaredMethod("get_last_guid", android.content.Context::class.java)
                method.isAccessible = true
                val guidBytes = method.invoke(null, context) as ByteArray
                guidBytes.toHexString()
            } catch (e: Exception) {
                ""
            }

            // Init FEKit
            val initMethod = feKitClass.getDeclaredMethod(
                "init",
                android.content.Context::class.java,
                String::class.java, String::class.java, String::class.java,
                String::class.java, String::class.java
            )
            initMethod.invoke(feKit, context, uin, guid, "", "", "")

            // getSign
            val getSignMethod = feKitClass.getDeclaredMethod(
                "getSign",
                String::class.java, ByteArray::class.java,
                Int::class.javaPrimitiveType, String::class.java
            )
            val signResult = getSignMethod.invoke(feKit, cmd, buffer, seq, uin)

            // Extract fields from SignResult
            val resultClass = signResult.javaClass
            val tokenField = resultClass.getDeclaredField("token")
            val extraField = resultClass.getDeclaredField("extra")
            val signField = resultClass.getDeclaredField("sign")
            tokenField.isAccessible = true
            extraField.isAccessible = true
            signField.isAccessible = true

            return FEKitResult(
                token = (tokenField.get(signResult) as? ByteArray)?.toHexString() ?: "",
                extra = (extraField.get(signResult) as? ByteArray)?.toHexString() ?: "",
                sign = (signField.get(signResult) as? ByteArray)?.toHexString() ?: ""
            )
        }

        fun ByteArray.toHexString(): String {
            return joinToString("") { "%02x".format(it) }
        }

        fun hex2ByteArray(hex: String): ByteArray? {
            return try {
                if (hex.length % 2 != 0) return null
                ByteArray(hex.length / 2) {
                    hex.substring(it * 2, it * 2 + 2).toInt(16).toByte()
                }
            } catch (e: Exception) {
                null
            }
        }
    }
}

data class SignResponse(
    val ret: Int,
    val msg: String,
    val data: SignData
)

data class SignData(
    val token: String,
    val extra: String,
    val sign: String
)

data class FEKitResult(
    val token: String,
    val extra: String,
    val sign: String
)
