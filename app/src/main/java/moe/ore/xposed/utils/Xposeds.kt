package moe.ore.xposed.utils

import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XC_MethodHook.MethodHookParam
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers

internal typealias MethodHooker = (MethodHookParam) -> Unit

internal class XCHook {
    var before = nullableOf<MethodHooker>()
    var after = nullableOf<MethodHooker>()

    fun after(after: MethodHooker): XCHook {
        this.after.set(after)
        return this
    }

    fun before(before: MethodHooker): XCHook {
        this.before.set(before)
        return this
    }
}

class Nullable<T : Any>(private var value: T?) {
    fun get(): T = value!!
    fun getOrNull(): T? = value
    fun isNull(): Boolean = value == null
    fun isNotNull(): Boolean = value != null
    fun set(value: T?) { this.value = value }
}

fun <T : Any> nullableOf(data: T? = null): Nullable<T> = Nullable(data)

fun Class<*>?.hookMethod(funName: String?): XposedMethodHook? {
    return try {
        val hook = XposedMethodHook()
        XposedBridge.hookAllMethods(this, funName, hook)
        hook
    } catch (e: Exception) {
        XposedBridge.log(e)
        null
    }
}

fun Class<*>?.hookMethod(funName: String?, vararg args: Class<*>): XposedMethodHook? {
    return try {
        val hook = XposedMethodHook()
        val anise = arrayOfNulls<Any>(args.size + 1)
        args.forEachIndexed { index, clazz -> anise[index] = clazz }
        anise[anise.size - 1] = hook
        XposedHelpers.findAndHookMethod(this, funName, *anise)
        hook
    } catch (e: Exception) {
        XposedBridge.log(e)
        null
    }
}

class XposedMethodHook : XC_MethodHook() {
    private lateinit var beforeFun: XposedMethodHookFunction
    private lateinit var afterFun: XposedMethodHookFunction

    fun before(function: XposedMethodHookFunction): XposedMethodHook {
        this.beforeFun = function
        return this
    }

    fun after(function: XposedMethodHookFunction): XposedMethodHook {
        this.afterFun = function
        return this
    }

    override fun beforeHookedMethod(param: MethodHookParam) {
        if (this::beforeFun.isInitialized) {
            try { beforeFun(param) } catch (_: Exception) {}
        }
    }

    override fun afterHookedMethod(param: MethodHookParam) {
        if (this::afterFun.isInitialized) {
            try { afterFun(param) } catch (_: Exception) {}
        }
    }
}

fun interface XposedMethodHookFunction {
    operator fun invoke(param: MethodHookParam)
}
