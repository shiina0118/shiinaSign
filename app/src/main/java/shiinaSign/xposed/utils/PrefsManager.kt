package moe.ore.xposed.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

object PrefsManager {
    private const val PREFS_NAME = "TXHookPrefs"

    const val KEY_AGREED_TO_TERMS = "agreed_to_terms"
    const val KEY_PUSH_API = "push_api"

    private lateinit var prefs: SharedPreferences

    /**
     * 初始化SharedPreferences
     */
    fun initialize(context: Context) {
        if (!::prefs.isInitialized) {
            prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        }
    }

    /**
     * 检查是否已初始化
     */
    fun isInitialized(): Boolean {
        return ::prefs.isInitialized
    }

    /**
     * 获取字符串值
     */
    fun getString(key: String, defaultValue: String = ""): String {
        return prefs.getString(key, defaultValue) ?: defaultValue
    }

    /**
     * 设置字符串值
     */
    fun setString(key: String, value: String) {
        prefs.edit { putString(key, value) }
    }

    /**
     * 获取布尔值
     */
    fun getBoolean(key: String, defaultValue: Boolean = false): Boolean {
        return prefs.getBoolean(key, defaultValue)
    }

    /**
     * 设置布尔值
     */
    fun setBoolean(key: String, value: Boolean) {
        prefs.edit { putBoolean(key, value) }
    }

    /**
     * 获取整数值
     */
    fun getInt(key: String, defaultValue: Int = 0): Int {
        return prefs.getInt(key, defaultValue)
    }

    /**
     * 设置整数值
     */
    fun setInt(key: String, value: Int) {
        prefs.edit { putInt(key, value) }
    }

    /**
     * 获取长整数值
     */
    fun getLong(key: String, defaultValue: Long = 0L): Long {
        return prefs.getLong(key, defaultValue)
    }

    /**
     * 设置长整数值
     */
    fun setLong(key: String, value: Long) {
        prefs.edit { putLong(key, value) }
    }

    /**
     * 获取浮点数值
     */
    fun getFloat(key: String, defaultValue: Float = 0f): Float {
        return prefs.getFloat(key, defaultValue)
    }

    /**
     * 设置浮点数值
     */
    fun setFloat(key: String, value: Float) {
        prefs.edit { putFloat(key, value) }
    }

    /**
     * 检查是否包含指定键
     */
    fun contains(key: String): Boolean {
        return prefs.contains(key)
    }

    /**
     * 移除指定键的值
     */
    fun remove(key: String) {
        prefs.edit { remove(key) }
    }

    /**
     * 清除所有数据
     */
    fun clear() {
        prefs.edit { clear() }
    }
}
