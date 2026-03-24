package moe.ore.xposed.hook

import de.robv.android.xposed.XposedBridge
import moe.ore.xposed.hook.base.hostPackageName
import moe.ore.xposed.hook.base.hostVersionCode
import moe.ore.xposed.utils.QQ_9_2_10_29175
import moe.ore.xposed.utils.XPClassloader.load
import moe.ore.xposed.utils.hookMethod

/**
 * Anti-detection from TXHook.
 * Forces QQ to use WTLogin (legacy) protocol instead of NT protocol,
 * ensuring QQSecuritySign/Dandelion/ByteData signing APIs remain active.
 */
internal object AntiDetection {

    operator fun invoke() {
        disableSwitch()
        isLoginByNTHook()
    }

    private fun disableSwitch() {
        val configClass = load("com.tencent.freesia.UnitedConfig")
        configClass?.let {
            it.hookMethod("isSwitchOn")?.after { param ->
                val tag = param.args[1] as? String ?: return@after
                when (tag) {
                    "msf_init_optimize", "msf_network_service_switch_new" -> {
                        if (isSupportedDisablingNewService()) {
                            param.result = false
                        }
                    }
                    "wt_login_upgrade" -> {
                        param.result = false
                    }
                    "nt_login_downgrade" -> {
                        param.result = true
                    }
                }
            }
        }
    }

    private fun isLoginByNTHook() {
        load("mqq.app.MobileQQ")?.hookMethod("isLoginByNT")?.after { param ->
            param.result = false
        }
    }

    private fun isSupportedDisablingNewService(): Boolean {
        return (hostPackageName == "com.tencent.mobileqq" && hostVersionCode <= QQ_9_2_10_29175) ||
                hostPackageName == "com.tencent.tim"
    }
}
