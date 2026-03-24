package moe.ore.xposed.hook.base

import android.app.Application
import android.content.res.XModuleResources
import moe.ore.xposed.HookEntry
import moe.ore.xposed.utils.getAndroidId
import moe.ore.xposed.utils.getAppName
import moe.ore.xposed.utils.getAppVersionCode
import moe.ore.xposed.utils.getAppVersionName

lateinit var hostApp: Application
lateinit var hostClassLoader: ClassLoader
lateinit var hostPackageName: String
lateinit var hostProcessName: String

lateinit var modulePath: String
lateinit var moduleRes: XModuleResources

val hostContext get() = hostApp
val hostAppName by lazy { getAppName(hostContext, hostPackageName) }
val hostVersionCode by lazy { getAppVersionCode(hostContext, hostPackageName) }
val hostVersionName by lazy { getAppVersionName(hostContext, hostPackageName) }
val hostAndroidId by lazy { getAndroidId(hostContext) }

val moduleClassLoader: ClassLoader = HookEntry::class.java.classLoader!!
var moduleLoadInit = false

val hostInit get() = ::hostApp.isInitialized
