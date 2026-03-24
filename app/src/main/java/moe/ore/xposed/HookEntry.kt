package moe.ore.xposed

import android.content.res.XModuleResources
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.IXposedHookZygoteInit
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam
import io.github.kyuubiran.ezxhelper.android.logging.Logger
import io.github.kyuubiran.ezxhelper.xposed.EzXposed
import moe.ore.xposed.base.LoadApp
import moe.ore.xposed.hook.base.modulePath
import moe.ore.xposed.hook.base.moduleRes
import moe.ore.xposed.hook.enums.QQTypeEnum

const val TAG = "TXHook"

class HookEntry : IXposedHookLoadPackage, IXposedHookZygoteInit {

    private lateinit var mLoadPackageParam: LoadPackageParam

    override fun handleLoadPackage(lpparam: LoadPackageParam) {
        mLoadPackageParam = lpparam
        val packageName = lpparam.packageName

        when {
            QQTypeEnum.contain(packageName) -> {
                EzXposed.initHandleLoadPackage(mLoadPackageParam)
                Logger.tag = TAG

                LoadApp.init(mLoadPackageParam)
            }
        }
    }

    override fun initZygote(startupParam: IXposedHookZygoteInit.StartupParam) {
        modulePath = startupParam.modulePath
        moduleRes = XModuleResources.createInstance(modulePath, null)
        EzXposed.initZygote(startupParam)
    }
}
