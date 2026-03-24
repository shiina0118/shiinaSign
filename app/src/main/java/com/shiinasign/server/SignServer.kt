package com.shiinasign.server

import com.google.gson.Gson
import com.tencent.mobileqq.fe.FEKit
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
            "/ping" -> newFixedLengthResponse(Response.Status.OK, "application/json", """{"status":"ok"}""")
            else -> newFixedLengthResponse(Response.Status.NOT_FOUND, "application/json", """{"error":"not found"}""")
        }
    }

    private fun handleSign(session: IHTTPSession): Response {
        val params = session.parms
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
                    token = result.token?.toHexString() ?: "",
                    extra = result.extra?.toHexString() ?: "",
                    sign = result.sign?.toHexString() ?: ""
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

    companion object {
        /**
         * Actively call FEKit.getSign via reflection.
         * This works because the module ClassLoader has been injected into QQ's ClassLoader chain,
         * so FEKit.getInstance() resolves to QQ's real implementation.
         */
        fun doSign(cmd: String, buffer: ByteArray, seq: Int, uin: String): FEKitResult {
            val feKit = FEKit.getInstance()
                ?: throw IllegalStateException("FEKit.getInstance() returned null - is QQ running?")

            val context = try {
                val appClass = Class.forName("com.tencent.common.app.BaseApplicationImpl")
                val field = appClass.declaredFields.first { it.type == appClass }
                field.isAccessible = true
                field.get(null) as android.content.Context
            } catch (e: Exception) {
                throw IllegalStateException("Cannot get QQ Context: ${e.message}")
            }

            // Get GUID via oicq.wlogin_sdk.tools.util.get_last_guid
            val guid = try {
                val utilClass = Class.forName("oicq.wlogin_sdk.tools.util")
                val method = utilClass.getDeclaredMethod("get_last_guid", android.content.Context::class.java)
                method.isAccessible = true
                val guidBytes = method.invoke(null, context) as ByteArray
                guidBytes.toHexString()
            } catch (e: Exception) {
                ""
            }

            // QUA and QIMEI - best effort, not critical
            val qua = ""
            val qimei = ""

            // Init FEKit and call getSign
            feKit.init(context, uin, guid, "", qimei, qua)
            val signResult = feKit.getSign(cmd, buffer, seq, uin)

            return FEKitResult(
                token = signResult.token,
                extra = signResult.extra,
                sign = signResult.sign
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
    val token: ByteArray?,
    val extra: ByteArray?,
    val sign: ByteArray?
)
