package moe.ore.xposed.utils

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

object ValueStore {

    // 存储四个类型的 map，每个 map 的值是 最后访问时间
    private val intMap = ConcurrentHashMap<Int, Long>()
    private val longMap = ConcurrentHashMap<Long, Long>()
    private val stringMap = ConcurrentHashMap<String, Long>()
    private val booleanMap = ConcurrentHashMap<Boolean, Long>()

    // TTL：10 分钟
    private const val TTL_MILLIS = 10 * 60 * 1000L

    // 启动后台线程每分钟清理一次过期数据
    private val cleanupExecutor = Executors.newSingleThreadScheduledExecutor().apply {
        scheduleWithFixedDelay({
            cleanUp()
        }, 1, 1, TimeUnit.MINUTES)
    }

    /**
     * 存储一个或多个值，返回 true 表示所有值都是新值（即之前未存储过）
     */
    fun store(vararg values: Any): Boolean {
        val now = System.currentTimeMillis()
        var allNew = true

        for (value in values) {
            val added = when (value) {
                is Int -> intMap.putIfAbsent(value, now) == null
                is Long -> longMap.putIfAbsent(value, now) == null
                is String -> stringMap.putIfAbsent(value, now) == null
                is Boolean -> booleanMap.putIfAbsent(value, now) == null
                else -> throw IllegalArgumentException("Unsupported type: ${value::class}")
            }
            if (!added) {
                // 已存在但仍然更新时间戳
                updateTimestamp(value, now)
                allNew = false
            }
        }
        return allNew
    }

    /**
     * 检查值是否已存在（同时更新访问时间）
     */
    fun exists(vararg values: Any): Boolean {
        val now = System.currentTimeMillis()
        for (value in values) {
            val map = getMap(value)
            if (map != null && map.containsKey(value)) {
                updateTimestamp(value, now)
            } else {
                return false
            }
        }
        return true
    }

    /**
     * 清理所有类型中过期的数据
     */
    private fun cleanUp() {
        val now = System.currentTimeMillis()
        cleanMap(intMap, now)
        cleanMap(longMap, now)
        cleanMap(stringMap, now)
        cleanMap(booleanMap, now)
    }

    private fun <T> cleanMap(map: ConcurrentHashMap<T, Long>, now: Long) {
        val iterator = map.entries.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            if (now - entry.value > TTL_MILLIS) {
                iterator.remove()
            }
        }
    }

    private fun updateTimestamp(value: Any, time: Long) {
        when (value) {
            is Int -> intMap[value] = time
            is Long -> longMap[value] = time
            is String -> stringMap[value] = time
            is Boolean -> booleanMap[value] = time
        }
    }

    private fun getMap(value: Any): ConcurrentHashMap<Any, Long>? {
        return when (value) {
            is Int -> intMap as ConcurrentHashMap<Any, Long>
            is Long -> longMap as ConcurrentHashMap<Any, Long>
            is String -> stringMap as ConcurrentHashMap<Any, Long>
            is Boolean -> booleanMap as ConcurrentHashMap<Any, Long>
            else -> null
        }
    }
}
