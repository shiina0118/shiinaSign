package com.tencent.mobileqq.fe

import android.content.Context
import com.tencent.mobileqq.sign.QQSecuritySign

/**
 * FEKit stub - compile-time only.
 * At runtime, QQ's own com.tencent.mobileqq.fe.FEKit is loaded via ClassLoader injection.
 */
class FEKit {
    fun init(context: Context, uin: String, guid: String, o3did: String, q36: String, qua: String) {}
    fun getSign(cmd: String, buffer: ByteArray, seq: Int, uin: String): QQSecuritySign.SignResult {
        return QQSecuritySign.SignResult()
    }
    companion object {
        @JvmStatic
        fun getInstance(): FEKit? = null
    }
}
