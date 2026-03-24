package moe.ore.xposed.hook.enums

import moe.ore.xposed.hook.config.PACKAGE_NAME_QIDIAN
import moe.ore.xposed.hook.config.PACKAGE_NAME_QQ
import moe.ore.xposed.hook.config.PACKAGE_NAME_TIM
import moe.ore.xposed.hook.config.PACKAGE_NAME_TXHOOK
import moe.ore.xposed.hook.config.PACKAGE_NAME_WATCH

enum class QQTypeEnum(
    val packageName: String,
    val appName: String
) {
    QQ(PACKAGE_NAME_QQ, "QQ"),
    TIM(PACKAGE_NAME_TIM, "TIM"),
    WATCH(PACKAGE_NAME_WATCH, "QQ手表"),
    QIDIAN(PACKAGE_NAME_QIDIAN, "QQ企点"),
    TXHook(PACKAGE_NAME_TXHOOK, "TXHook");

    companion object {
        fun valueOfPackage(packageName: String): QQTypeEnum {
            val qqTypeEnums = entries.toTypedArray()
            for (qqTypeEnum in qqTypeEnums) {
                if (qqTypeEnum.packageName == packageName) {
                    return qqTypeEnum
                }
            }
            throw UnSupportQQTypeException("不支持的包名")
        }

        fun contain(packageName: String): Boolean {
            val qqTypeEnums = entries.toTypedArray()
            for (qqTypeEnum in qqTypeEnums) {
                if (qqTypeEnum.packageName == packageName) {
                    return true
                }
            }
            return false
        }
    }
}
