package shiinaSign.xposed.base

import android.app.Application
import android.content.Context
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XC_MethodReplacement
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage
import io.github.kyuubiran.ezxhelper.android.logging.Logger
import shiinaSign.xposed.hook.AntiDetection
import shiinaSign.xposed.hook.MainHook
import shiinaSign.xposed.hook.base.ProcUtil
import shiinaSign.xposed.hook.base.hostAndroidId
import shiinaSign.xposed.hook.base.hostApp
import shiinaSign.xposed.hook.base.hostAppName
import shiinaSign.xposed.hook.base.hostClassLoader
import shiinaSign.xposed.hook.base.hostInit
import shiinaSign.xposed.hook.base.hostPackageName
import shiinaSign.xposed.hook.base.hostProcessName
import shiinaSign.xposed.hook.base.hostVersionCode
import shiinaSign.xposed.hook.base.hostVersionName
import shiinaSign.xposed.hook.base.moduleClassLoader
import shiinaSign.xposed.hook.enums.QQTypeEnum
import shiinaSign.xposed.hooks.ListenTXHookUpdate
import shiinaSign.xposed.utils.FuzzySearchClass
import shiinaSign.xposed.utils.XPClassloader
import shiinaSign.xposed.utils.afterHook
import java.lang.reflect.Field
import java.lang.reflect.Modifier

object LoadApp {

    fun init(lpparam: XC_LoadPackage.LoadPackageParam) {
        when (QQTypeEnum.valueOfPackage(lpparam.packageName)) {
            QQTypeEnum.TXHook -> hookActivation(lpparam)
            else -> hookEntry(lpparam)
        }
    }

    private fun hookActivation(lpparam: XC_LoadPackage.LoadPackageParam) {
        XposedHelpers.findAndHookMethod(
            "shiinaSign.xposed.common.ModeleStatus",
            lpparam.classLoader,
            "isModuleActivated",
            XC_MethodReplacement.returnConstant(true)
        )
    }

    private fun hookEntry(lpparam: XC_LoadPackage.LoadPackageParam) {
        hostPackageName = lpparam.packageName
        hostProcessName = lpparam.processName
        hostClassLoader = lpparam.classLoader

        val startup = afterHook(50) { param ->
            try {
                val loader = param.thisObject.javaClass.classLoader!!
                XPClassloader.ctxClassLoader = loader
                // ... 其余实现 ...
            } catch (e: Exception) {
                Logger.e("LoadApp hookEntry error", e)
            }
        }
    }
}
