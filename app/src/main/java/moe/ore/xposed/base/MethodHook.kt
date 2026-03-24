package moe.ore.xposed.base

/**
 * 标记一个方法为 Hook 方法
 *
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY_GETTER,
    AnnotationTarget.PROPERTY_SETTER
)
annotation class MethodHook()
