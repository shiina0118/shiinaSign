package moe.ore.xposed.hook.base

import android.app.ActivityManager
import android.content.Context
import android.os.Process
import kotlin.Exception

object ProcUtil {
    const val MAIN = 1
    const val MSF = 1 shl 1
    const val OTHER = 1 shl 31

    private val mPid: Int by lazy { Process.myPid() }

    val procName: String by lazy {
        val am = hostApp.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        runCatching {
            am.runningAppProcesses.firstOrNull { it.pid == mPid }?.processName
        }.getOrNull() ?: "unknown"
    }

    val procType: Int by lazy {
        val parts = procName.split(":")
        if (parts.size == 1) return@lazy MAIN
        return@lazy when (parts.last()) {
            "MSF" -> MSF
            else -> OTHER
        }
    }

    val isMsf: Boolean by lazy { (procType and MSF) != 0 }
}
