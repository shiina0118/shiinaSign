package shiinaSign.xposed

import android.content.res.XModuleResources
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.IXposedHookZygoteInit
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam
import io.github.kyuubiran.ezxhelper.android.logging.Logger
import io.github.kyuubiran.ezxhelper.xposed.EzXposed
import shiinaSign.xposed.base.LoadApp
import shiinaSign.xposed.hook.base.modulePath
import shiinaSign.xposed.hook.base.moduleRes
import shiinaSign.xposed.hook.enums.QQTypeEnum

const val TAG = "shiinaSign"

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
