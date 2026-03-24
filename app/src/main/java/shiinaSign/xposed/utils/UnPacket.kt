package moe.ore.xposed.utils

import java.nio.ByteBuffer
import java.nio.ByteOrder

class UnPacket {
    // 使用 ThreadLocal 为每个线程提供独立的 ByteBuffer 实例
    private val threadLocalBuffer = ThreadLocal<ByteBuffer>()

    // 确保 buffer 被初始化
    private fun ensureBufferInitialized() {
        val buffer = getBuffer()
        if (buffer == null) {
            throw IllegalStateException("Buffer not initialized. Call wrapBytesAddr first.")
        }
    }

    // 获取当前线程的 ByteBuffer 实例
    private fun getBuffer(): ByteBuffer? = threadLocalBuffer.get()

    // 初始化 ByteBuffer，线程本地存储
    fun wrapBytesAddr(input: Any): UnPacket {
        val byteArray = when (input) {
            is ByteArray -> input
            is String -> hexStringToByteArray(input)
            else -> throw IllegalArgumentException("Unsupported input type")
        }
        // 为当前线程设置独立的 ByteBuffer
        threadLocalBuffer.set(ByteBuffer.wrap(byteArray).order(ByteOrder.BIG_ENDIAN))
        return this
    }

    // 跳过指定字节长度
    fun skip(len: Int): UnPacket {
        ensureBufferInitialized()
        val buffer = getBuffer() ?: throw IllegalStateException("Buffer not initialized.")
        if (len < 0 || buffer.position() + len > buffer.limit())
            throw IllegalStateException("Not enough bytes to skip $len")
        buffer.position(buffer.position() + len)
        return this
    }

    // 获取当前 position
    fun getPosition(): Int {
        ensureBufferInitialized()
        return getBuffer()!!.position()
    }

    // 获取剩余字节数
    fun getRemaining(): Int {
        ensureBufferInitialized()
        return getBuffer()!!.remaining()
    }

    // 获取一个字节
    fun getByte(): Byte {
        ensureBufferInitialized()
        val buffer = getBuffer()!!
        if (buffer.remaining() < 1) throw IllegalStateException("Not enough bytes for byte")
        return buffer.get()
    }

    // 获取一个 short
    fun getShort(): Short {
        ensureBufferInitialized()
        val buffer = getBuffer()!!
        if (buffer.remaining() < 2) throw IllegalStateException("Not enough bytes for short")
        return buffer.short
    }

    // 获取一个 int
    fun getInt(): Int {
        ensureBufferInitialized()
        val buffer = getBuffer()!!
        if (buffer.remaining() < 4) throw IllegalStateException("Not enough bytes for int")
        return buffer.int
    }

    // 获取一个 long
    fun getLong(): Long {
        ensureBufferInitialized()
        val buffer = getBuffer()!!
        if (buffer.remaining() < 8) throw IllegalStateException("Not enough bytes for long")
        return buffer.long
    }

    // 获取一个 float
    fun getFloat(): Float {
        ensureBufferInitialized()
        val buffer = getBuffer()!!
        if (buffer.remaining() < 4) throw IllegalStateException("Not enough bytes for float")
        return buffer.float
    }

    // 获取一个 double
    fun getDouble(): Double {
        ensureBufferInitialized()
        val buffer = getBuffer()!!
        if (buffer.remaining() < 8) throw IllegalStateException("Not enough bytes for double")
        return buffer.double
    }

    // 获取指定长度的字节数组
    fun getBytes(length: Int): ByteArray {
        ensureBufferInitialized()
        val buffer = getBuffer()!!
        if (buffer.remaining() < length)
            throw IllegalStateException("Not enough bytes to get $length bytes")
        val bytes = ByteArray(length)
        buffer.get(bytes)
        return bytes
    }

    // 获取剩余字节
    fun remainingBytes(): ByteArray {
        ensureBufferInitialized()
        val buffer = getBuffer()!!
        val remainingBytes = ByteArray(buffer.remaining())
        buffer.get(remainingBytes)
        return remainingBytes
    }

    // 查看下一个字节而不改变 position
    fun peekByte(): Byte {
        ensureBufferInitialized()
        val buffer = getBuffer()!!
        if (buffer.remaining() < 1) throw IllegalStateException("Not enough bytes to peek byte")
        val dup = buffer.duplicate()
        dup.position(buffer.position())
        return dup.get()
    }

    // 查看下一个 short
    fun peekShort(): Short {
        ensureBufferInitialized()
        val buffer = getBuffer()!!
        if (buffer.remaining() < 2) throw IllegalStateException("Not enough bytes to peek short")
        val dup = buffer.duplicate().order(ByteOrder.BIG_ENDIAN)
        dup.position(buffer.position())
        return dup.short
    }

    // 查看下一个 int
    fun peekInt(): Int {
        ensureBufferInitialized()
        val buffer = getBuffer()!!
        if (buffer.remaining() < 4) throw IllegalStateException("Not enough bytes to peek int")
        val dup = buffer.duplicate().order(ByteOrder.BIG_ENDIAN)
        dup.position(buffer.position())
        return dup.int
    }

    // 查看下一个 long
    fun peekLong(): Long {
        ensureBufferInitialized()
        val buffer = getBuffer()!!
        if (buffer.remaining() < 8) throw IllegalStateException("Not enough bytes to peek long")
        val dup = buffer.duplicate().order(ByteOrder.BIG_ENDIAN)
        dup.position(buffer.position())
        return dup.long
    }

    // 查看下一个 float
    fun peekFloat(): Float {
        ensureBufferInitialized()
        val buffer = getBuffer()!!
        if (buffer.remaining() < 4) throw IllegalStateException("Not enough bytes to peek float")
        val dup = buffer.duplicate().order(ByteOrder.BIG_ENDIAN)
        dup.position(buffer.position())
        return dup.float
    }

    // 查看下一个 double
    fun peekDouble(): Double {
        ensureBufferInitialized()
        val buffer = getBuffer()!!
        if (buffer.remaining() < 8) throw IllegalStateException("Not enough bytes to peek double")
        val dup = buffer.duplicate().order(ByteOrder.BIG_ENDIAN)
        dup.position(buffer.position())
        return dup.double
    }

    // 查看指定长度的字节数组
    fun peekBytes(length: Int): ByteArray {
        ensureBufferInitialized()
        val buffer = getBuffer()!!
        if (buffer.remaining() < length)
            throw IllegalStateException("Not enough bytes to peek $length bytes")
        val dup = buffer.duplicate().order(ByteOrder.BIG_ENDIAN)
        dup.position(buffer.position())
        val bytes = ByteArray(length)
        dup.get(bytes)
        return bytes
    }

    // 将十六进制字符串转换为字节数组
    private fun hexStringToByteArray(hex: String): ByteArray {
        val cleanHex = hex.replace(" ", "").replace("\n", "")
        require(cleanHex.length % 2 == 0) { "Invalid hex string" }
        require(cleanHex.matches("[0-9a-fA-F]+".toRegex())) { "Invalid hex characters" }
        return ByteArray(cleanHex.length / 2) {
            cleanHex.substring(it * 2, it * 2 + 2).toInt(16).toByte()
        }
    }
}

fun splitPackets(data: ByteArray): List<ByteArray> {
    val packets = mutableListOf<ByteArray>()
    var offset = 0

    while (offset + 4 <= data.size) {
        val length = ByteBuffer.wrap(data, offset, 4).order(ByteOrder.BIG_ENDIAN).int
        if (length < 4 || offset + length > data.size) break

        val packet = data.copyOfRange(offset, offset + length)
        packets.add(packet)
        offset += length
    }

    return packets
}

fun String.toUtf8ByteArray(): ByteArray = this.toByteArray(Charsets.UTF_8)

fun String.toUnHexString(): String = this
    .toUtf8ByteArray()
    .joinToString("") { "%02X".format(it) }

fun ByteArray.indexOf(subArray: ByteArray): Int {
    if (subArray.isEmpty() || this.size < subArray.size) return -1

    outer@ for (i in 0..(this.size - subArray.size)) {
        for (j in subArray.indices) {
            if (this[i + j] != subArray[j]) continue@outer
        }
        return i // 匹配成功
    }
    return -1 // 没找到
}

fun ByteArray.indexesOf(subArray: ByteArray): List<Int> {
    val result = mutableListOf<Int>()
    if (subArray.isEmpty() || this.size < subArray.size) return result

    outer@ for (i in 0..(this.size - subArray.size)) {
        for (j in subArray.indices) {
            if (this[i + j] != subArray[j]) continue@outer
        }
        result.add(i)
    }
    return result
}
