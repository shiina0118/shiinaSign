package moe.ore.xposed.base

import io.github.kyuubiran.ezxhelper.android.logging.Logger
import moe.ore.xposed.hook.base.ProcUtil

abstract class BaseHook : IBaseHook {

    override fun init() {
        val init by lazy {
            if (!this.isCompatible || !this.enabled) {
                Logger.w("跳过Hook: [${this.description}]")
                return@lazy
            }
            Logger.i("加载Hook: [${this.description}]")
            Logger.i("当前进程名: [${ProcUtil.procName}]")
            invokeHookMethod(this)
        }
        return init
    }

    private fun invokeHookMethod(impl: BaseHook) {
        val methods = impl.javaClass.declaredMethods
        for (method in methods) {
            if (!method.isAnnotationPresent(MethodHook::class.java)) continue

            try {
                method.isAccessible = true
                method.invoke(impl)
            } catch (e: Exception) {
                Logger.e("调用方法出现错误", e)
            }
        }
    }
}
