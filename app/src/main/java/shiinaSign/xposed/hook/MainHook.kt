package shiinaSign.xposed.hook

import android.content.ContentValues
import android.content.Context
import androidx.core.net.toUri
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.annotations.SerializedName
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import shiinaSign.xposed.hook.base.hostClassLoader
import shiinaSign.xposed.hook.base.hostPackageName
import shiinaSign.xposed.hook.base.hostVersionCode
import shiinaSign.xposed.hook.enums.QQTypeEnum
import shiinaSign.xposed.utils.FuzzySearchClass
import shiinaSign.xposed.utils.GlobalData
import shiinaSign.xposed.utils.HookUtil
import shiinaSign.xposed.utils.HttpUtil
import shiinaSign.xposed.utils.PacketDedupCache
import shiinaSign.xposed.utils.QQ_9_2_10_29175
import shiinaSign.xposed.utils.XPClassloader
import shiinaSign.xposed.utils.getPatchBuffer
import shiinaSign.xposed.utils.hookMethod
import java.lang.ref.WeakReference
import java.lang.reflect.Field
import java.lang.reflect.Method
import java.nio.ByteBuffer

object MainHook {
    private const val DEFAULT_URI = "content://shiinaSign.CatchProvider"
    private const val MODE_BDH_SESSION = "bdh.session"
    private const val MODE_BDH_SESSION_KEY = "bdh.sessionkey"
    private const val MODE_MD5 = "md5"
    private const val MODE_TLV_GET_BUF = "tlv.get_buf"
    private const val MODE_TLV_SET_BUF = "tlv.set_buf"
    private const val MODE_TEA = "tea"
    private const val MODE_RECE_DATA = "receData"
    private const val MODE_SEND = "send"
    private const val TYPE_FLY = "fly"
    private const val TYPE_GETSIGN = "getsign"
    private const val TYPE_GET_FE_KIT_ATTACH = "getFeKitAttach"
    private const val TYPE_NATIVE_SET_ACCOUNT_KEY = "nativeSetAccountKey"
    private const val TYPE_ECDH_DATA = "ecdhData"

    private val defaultUri = DEFAULT_URI.toUri()
    private var isInit: Boolean = false
    private var source = 0
    private val global = GlobalData()
    private val EcdhCrypt = XPClassloader.load("oicq.wlogin_sdk.tools.EcdhCrypt")!!
    private val CodecWarpper = XPClassloader.load("com.tencent.qphone.base.util.CodecWarpper")!!
    private val cryptor = XPClassloader.load("oicq.wlogin_sdk.tools.cryptor")!!
    private val tlv_t = XPClassloader.load("oicq.wlogin_sdk.tlv_type.tlv_t")!!
    private val MD5 = XPClassloader.load("oicq.wlogin_sdk.tools.MD5")!!
    private val HighwaySessionData = XPClassloader.load("com.tencent.mobileqq.highway.openup.SessionInfo")!!
    private val MSFKernel = XPClassloader.load("com.tencent.mobileqq.msfcore.MSFKernel")
    // ... 其余实现 ...
}
