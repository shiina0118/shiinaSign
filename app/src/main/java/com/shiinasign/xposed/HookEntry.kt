package com.shiinasign.xposed

import android.content.Context
import com.shiinasign.server.SignServer
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.callbacks.XC_LoadPackage
import moe.ore.xposed.hook.AntiDetection
import moe.ore.xposed.hook.base.hostClassLoader
import moe.ore.xposed.hook.base.hostPackageName
import moe.ore.xposed.hook.base.hostProcessName
import moe.ore.xposed.utils.FuzzySearchClass
import java.lang.reflect.Modifier

class HookEntry : IXposedHookLoadPackage {

    companion object {
        @Volatile
        private var initialized = false
    }

    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        when (lpparam.packageName) {
            "com.tencent.mobileqq", "com.tencent.tim" -> entryQQ(lpparam)
        }
    }

    private fun entryQQ(lpparam: XC_LoadPackage.LoadPackageParam) {
        hostClassLoader = lpparam.classLoader
        hostPackageName = lpparam.packageName
        hostProcessName = lpparam.processName

        val startup = object : XC_MethodHook() {
            override fun afterHookedMethod(param: MethodHookParam) {
                if (initialized) return
                try {
                    val loader = param.thisObject.javaClass.classLoader!!
                    val clz = loader.loadClass("com.tencent.common.app.BaseApplicationImpl")
                    val field = clz.declaredFields.first { it.type == clz }
                    field.isAccessible = true
                    val ctx = field.get(null) as? Context

                    if (ctx != null) {
                        onAppStartup(ctx)
                    }
                } catch (e: Throwable) {
                    XposedBridge.log(e)
                }
            }
        }

        // Hook LoadDex - standard QQ
        runCatching {
            val loadDex = lpparam.classLoader.loadClass("com.tencent.mobileqq.startup.step.LoadDex")
            loadDex.declaredMethods
                .filter { it.returnType == Boolean::class.javaPrimitiveType && it.parameterTypes.isEmpty() }
                .forEach { XposedBridge.hookMethod(it, startup) }
        }.onFailure {
            // For NT QQ - fuzzy search
            hookNTQQEntry(lpparam.classLoader, startup)
        }
    }

    private fun hookNTQQEntry(classLoader: ClassLoader, startup: XC_MethodHook) {
        val fieldList = mutableListOf<java.lang.reflect.Field>()
        FuzzySearchClass.findAllClassByField(classLoader, "com.tencent.mobileqq.startup.task.config") { _, field ->
            (field.type == HashMap::class.java || field.type == Map::class.java) && Modifier.isStatic(field.modifiers)
        }.forEach {
            it.declaredFields.forEach { field ->
                if ((field.type == HashMap::class.java || field.type == Map::class.java)
                    && Modifier.isStatic(field.modifiers)) {
                    fieldList.add(field)
                }
            }
        }
        fieldList.forEach { field ->
            if (!field.isAccessible) field.isAccessible = true
            @Suppress("UNCHECKED_CAST")
            (field.get(null) as? Map<String, Class<*>>)?.forEach { (key, clazz) ->
                if (key.contains("LoadDex", ignoreCase = true)) {
                    clazz.declaredMethods.forEach {
                        if (it.parameterTypes.size == 1 && it.parameterTypes[0] == Context::class.java) {
                            XposedBridge.hookMethod(it, startup)
                        }
                    }
                }
            }
        }
    }

    private fun onAppStartup(ctx: Context) {
        if (initialized) return
        initialized = true

        XposedBridge.log("[shiinaSign] App startup detected: ${ctx.javaClass.name}, process: $hostProcessName")

        AntiDetection()

        XposedBridge.log("[shiinaSign] Starting sign server...")
        startSignServer()
    }

    private fun startSignServer() {
        try {
            val port = 7878
            val server = SignServer(port)
            server.start(5000, false)
            XposedBridge.log("[shiinaSign] HTTP Server started on port $port")
        } catch (e: Exception) {
            XposedBridge.log("[shiinaSign] Failed to start HTTP server: ${e.message}")
        }
    }
}
