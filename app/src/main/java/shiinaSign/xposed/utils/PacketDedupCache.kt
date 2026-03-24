package moe.ore.xposed.utils

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

object PacketDedupCache {
    private val recentKeys = ConcurrentHashMap<String, Long>()
    private const val EXPIRE_MS = 2000L

    // 启动后台线程每分钟清理一次过期数据
    private val cleanupExecutor = Executors.newSingleThreadScheduledExecutor().apply {
        scheduleWithFixedDelay({
            cleanUp()
        }, 1, 1, TimeUnit.MINUTES)
    }

    fun shouldProcess(seq: Int, cmd: String, data: ByteArray): Boolean {
        val now = System.currentTimeMillis()

        val dataHash = data.contentHashCode()
        val key = "$seq-${cmd.hashCode()}-$dataHash"

        val lastTime = recentKeys[key]
        return if (lastTime == null || now - lastTime > EXPIRE_MS) {
            recentKeys[key] = now
            true
        } else {
            false
        }
    }

    private fun cleanUp() {
        val now = System.currentTimeMillis()
        recentKeys.entries.removeIf { now - it.value > EXPIRE_MS }
    }
}
