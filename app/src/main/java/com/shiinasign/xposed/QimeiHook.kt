package com.shiinasign.xposed

import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import moe.ore.xposed.hook.base.hostClassLoader

/**
 * Hook QIMEI generation to allow injecting custom device info.
 * When custom device info is set via setCustomDeviceInfo(), the next
 * call to U.r() will use it instead of the real device info.
 */
internal object QimeiHook {

    // Holds custom device info to inject on next U.r() call
    @Volatile
    var pendingDeviceInfo: Array<String>? = null

    // Signal to force refresh
    @Volatile
    var forceRefresh = false

    // Generated result after U.r() completes
    @Volatile
    var lastQimei16: String? = null
    @Volatile
    var lastQimei36: String? = null
    @Volatile
    var generationDone = false

    operator fun invoke() {
        try {
            hookQimeiGeneration()
            XposedBridge.log("[shiinaSign] QimeiHook initialized")
        } catch (e: Exception) {
            XposedBridge.log("[shiinaSign] QimeiHook failed: ${e.message}")
        }
    }

    private fun hookQimeiGeneration() {
        val classLoader = hostClassLoader ?: return
        val uClass = classLoader.loadClass("com.tencent.qimei.uin.U")

        // Hook U.r() to inject custom device info
        val rMethod = uClass.getDeclaredMethod(
            "r",
            Boolean::class.java, Int::class.java, Int::class.java,
            String::class.java, Int::class.java,
            Array<String>::class.java, String::class.java
        )

        XposedBridge.hookMethod(rMethod, object : XC_MethodHook() {
            override fun beforeHookedMethod(param: MethodHookParam) {
                val customInfo = pendingDeviceInfo
                if (customInfo != null) {
                    // Inject custom device info
                    param.args[0] = true // forceRefresh
                    param.args[5] = customInfo // deviceInfo array
                    XposedBridge.log("[shiinaSign] Injected custom device info into U.r()")
                }
            }

            override fun afterHookedMethod(param: MethodHookParam) {
                try {
                    val oMethod = uClass.getDeclaredMethod("o")
                    val pMethod = uClass.getDeclaredMethod("p")
                    lastQimei16 = oMethod.invoke(null) as? String
                    lastQimei36 = pMethod.invoke(null) as? String
                    generationDone = true
                    XposedBridge.log("[shiinaSign] QIMEI generated - qimei16: $lastQimei16, qimei36: $lastQimei36")
                } catch (e: Exception) {
                    XposedBridge.log("[shiinaSign] Failed to read QIMEI after generation: ${e.message}")
                    generationDone = true
                }
                // Clear pending info
                pendingDeviceInfo = null
            }
        })

        XposedBridge.log("[shiinaSign] Hooked U.r() for QIMEI generation")
    }

    /**
     * Generate QIMEI with custom device info.
     * This sets the device info and triggers U.r() via reflection.
     * The hook will intercept the call and inject our custom data.
     */
    fun generateQimei(deviceInfo: DeviceInfo): QimeiResult {
        val classLoader = hostClassLoader
            ?: throw IllegalStateException("QQ ClassLoader not available")

        // Reset state
        generationDone = false
        lastQimei16 = null
        lastQimei36 = null

        // Build device info JSON
        val deviceInfoJson = buildDeviceInfoJson(deviceInfo)

        // Set pending info (will be picked up by the hook)
        pendingDeviceInfo = arrayOf(deviceInfoJson)

        // Get Context
        val appClass = classLoader.loadClass("com.tencent.common.app.BaseApplicationImpl")
        val field = appClass.declaredFields.first { it.type == appClass }
        field.isAccessible = true
        val context = field.get(null) as android.content.Context

        // Call U.n(context) to init if needed, then U.r() to generate
        val uClass = classLoader.loadClass("com.tencent.qimei.uin.U")
        try {
            val nMethod = uClass.getDeclaredMethod("n", android.content.Context::class.java)
            nMethod.invoke(null, context)
        } catch (_: Exception) {}

        // Trigger generation
        val rMethod = uClass.getDeclaredMethod(
            "r",
            Boolean::class.java, Int::class.java, Int::class.java,
            String::class.java, Int::class.java,
            Array<String>::class.java, String::class.java
        )
        rMethod.invoke(null, true, 0, 0, "", 0, arrayOf(deviceInfoJson), "5370613241")

        // Wait for hook to complete (the afterHookedMethod runs synchronously)
        // Actually XposedBridge.hookMethod's afterHookedMethod runs in the same call,
        // so by the time invoke() returns, the hook should have already fired.
        // But let's add a small wait loop just in case.
        var waited = 0
        while (!generationDone && waited < 50) {
            Thread.sleep(100)
            waited++
        }

        if (!generationDone) {
            throw IllegalStateException("QIMEI generation timed out")
        }

        return QimeiResult(
            qimei16 = lastQimei16 ?: "",
            qimei36 = lastQimei36 ?: ""
        )
    }

    private fun buildDeviceInfoJson(info: DeviceInfo): String {
        // Build the JSON that U.r() expects - only include non-empty fields
        val parts = mutableListOf<String>()
        if (info.imei.isNotEmpty()) parts.add("\"imei\":\"${info.imei}\"")
        if (info.androidId.isNotEmpty()) parts.add("\"android_id\":\"${info.androidId}\"")
        if (info.mac.isNotEmpty()) parts.add("\"mac\":\"${info.mac}\"")
        if (info.model.isNotEmpty()) parts.add("\"model\":\"${info.model}\"")
        if (info.brand.isNotEmpty()) parts.add("\"brand\":\"${info.brand}\"")
        if (info.manufacturer.isNotEmpty()) parts.add("\"manufacturer\":\"${info.manufacturer}\"")
        if (info.fingerprint.isNotEmpty()) parts.add("\"fingerprint\":\"${info.fingerprint}\"")
        if (info.bootId.isNotEmpty()) parts.add("\"boot_id\":\"${info.bootId}\"")
        if (info.procVersion.isNotEmpty()) parts.add("\"proc_version\":\"${info.procVersion}\"")
        if (info.sdkVersion.isNotEmpty()) parts.add("\"sdk_version\":\"${info.sdkVersion}\"")
        if (info.display.isNotEmpty()) parts.add("\"display\":\"${info.display}\"")
        if (info.device.isNotEmpty()) parts.add("\"device\":\"${info.device}\"")
        if (info.board.isNotEmpty()) parts.add("\"board\":\"${info.board}\"")
        return "{${parts.joinToString(",")}}"
    }

    /**
     * Get the current QIMEI from QQ process without custom device info.
     */
    fun getCurrentQimei(): QimeiResult {
        val classLoader = hostClassLoader
            ?: throw IllegalStateException("QQ ClassLoader not available")

        return try {
            val uClass = classLoader.loadClass("com.tencent.qimei.uin.U")
            val oMethod = uClass.getDeclaredMethod("o")
            val pMethod = uClass.getDeclaredMethod("p")
            val q16 = oMethod.invoke(null) as? String ?: ""
            val q36 = pMethod.invoke(null) as? String ?: ""
            QimeiResult(qimei16 = q16, qimei36 = q36)
        } catch (e: Exception) {
            // Try BeaconReport as fallback
            try {
                val beaconClass = classLoader.loadClass("com.tencent.beacon.BeaconReport")
                val getInstance = beaconClass.getDeclaredMethod("getInstance")
                val instance = getInstance.invoke(null)
                val getQimei = beaconClass.getDeclaredMethod("getQimei")
                val qimei = getQimei.invoke(instance) as? String ?: ""
                QimeiResult(qimei16 = qimei, qimei36 = "")
            } catch (e2: Exception) {
                throw IllegalStateException("Cannot get QIMEI: ${e2.message}")
            }
        }
    }
}

data class DeviceInfo(
    val imei: String = "",
    val androidId: String = "",
    val mac: String = "",
    val model: String = "",
    val brand: String = "",
    val manufacturer: String = "",
    val fingerprint: String = "",
    val bootId: String = "",
    val procVersion: String = "",
    val sdkVersion: String = "",
    val display: String = "",
    val device: String = "",
    val board: String = ""
)

data class QimeiResult(
    val qimei16: String,
    val qimei36: String
)
