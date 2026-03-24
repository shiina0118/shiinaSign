package moe.ore.xposed.hook.base

import android.app.Application
import android.content.Context

lateinit var hostApp: Application
lateinit var hostClassLoader: ClassLoader
lateinit var hostPackageName: String
lateinit var hostProcessName: String

val hostContext: Context get() = hostApp

val hostVersionCode: Long by lazy {
    runCatching {
        val pi = hostApp.packageManager.getPackageInfo(hostPackageName, 0)
        if (pi != null) {
            if (android.os.Build.VERSION.SDK_INT >= 28) pi.longVersionCode else pi.versionCode.toLong()
        } else 0L
    }.getOrDefault(0L)
}
