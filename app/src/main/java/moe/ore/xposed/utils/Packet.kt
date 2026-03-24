package moe.ore.xposed.utils

import java.nio.ByteBuffer
import java.nio.ByteOrder

class Packet {
    // 每个线程独立的 ByteBuffer 实例
    private val threadLocalBuffer = ThreadLocal<ByteBuffer>()

    // 获取当前线程的 ByteBuffer 实例
    private fun getBuffer(): ByteBuffer? = threadLocalBuffer.get()

    // 初始化一个 ByteBuffer（可指定容量和字节序）
    fun initBuffer(capacity: Int = 1024, order: ByteOrder = ByteOrder.BIG_ENDIAN): Packet {
        threadLocalBuffer.set(ByteBuffer.allocate(capacity).order(order))
        return this
    }

    // 确保已初始化
    private fun ensureBufferInitialized() {
        val buffer = getBuffer()
        if (buffer == null) {
            throw IllegalStateException("Buffer not initialized. Call initBuffer first.")
        }
    }

    // 设置 ByteOrder（可选）
    fun setByteOrder(order: ByteOrder): Packet {
        ensureBufferInitialized()
        getBuffer()!!.order(order)
        return this
    }

    // 写入一个 byte
    fun putByte(value: Byte): Packet {
        ensureBufferInitialized()
        getBuffer()!!.put(value)
        return this
    }

    // 写入一个 short
    fun putShort(value: Short): Packet {
        ensureBufferInitialized()
        getBuffer()!!.putShort(value)
        return this
    }

    // 写入一个 int
    fun putInt(value: Int): Packet {
        ensureBufferInitialized()
        getBuffer()!!.putInt(value)
        return this
    }

    // 写入一个 long
    fun putLong(value: Long): Packet {
        ensureBufferInitialized()
        getBuffer()!!.putLong(value)
        return this
    }

    // 写入一个 float
    fun putFloat(value: Float): Packet {
        ensureBufferInitialized()
        getBuffer()!!.putFloat(value)
        return this
    }

    // 写入一个 double
    fun putDouble(value: Double): Packet {
        ensureBufferInitialized()
        getBuffer()!!.putDouble(value)
        return this
    }

    // 写入字节数组
    fun putBytes(data: ByteArray): Packet {
        ensureBufferInitialized()
        getBuffer()!!.put(data)
        return this
    }

    // 写入十六进制字符串
    fun putHex(hex: String): Packet {
        val bytes = hexStringToByteArray(hex)
        return putBytes(bytes)
    }

    // 获取当前写入位置
    fun getPosition(): Int {
        ensureBufferInitialized()
        return getBuffer()!!.position()
    }

    // 获取已写入的 ByteArray
    fun toByteArray(): ByteArray {
        ensureBufferInitialized()
        val buffer = getBuffer()!!
        val size = buffer.position()
        val array = ByteArray(size)
        buffer.rewind()
        buffer.get(array, 0, size)
        return array
    }

    // 清空当前 buffer
    fun clear(): Packet {
        ensureBufferInitialized()
        getBuffer()!!.clear()
        return this
    }

    // 十六进制字符串转字节数组（与 UnPacket 同步逻辑）
    private fun hexStringToByteArray(hex: String): ByteArray {
        val cleanHex = hex.replace(" ", "").replace("\n", "")
        require(cleanHex.length % 2 == 0) { "Invalid hex string" }
        require(cleanHex.matches("[0-9a-fA-F]+".toRegex())) { "Invalid hex characters" }
        return ByteArray(cleanHex.length / 2) {
            cleanHex.substring(it * 2, it * 2 + 2).toInt(16).toByte()
        }
    }
}

fun getPatchBuffer(ssoseq: Int): ByteArray {
    val body = Packet().initBuffer(1024)
        .putInt(ssoseq)
        .putInt(0)
        .putInt(28)
        .putBytes("PhoneSigLcCheck succeed.".toUtf8ByteArray())
        .putInt(19)
        .putBytes("PhSigLcId.Check".toUtf8ByteArray())
        .putInt(8)
        .putHex("0B FE DF 42")
        .putHex("00 00 00 00 00 00 00 0A A8 01 00 C8 01 02")
        .putInt(4)
        .putHex("00 00 00 44 10 02 2C 3C 4C 56 01 61 66 01 62 7D")
        .putHex("00 00 2C 08 00 01 06 03 72 65 73 18 00 01 06 17")
        .putHex("4B 51 51 43 6F 6E 66 69 67 2E 53 69 67 6E 61 74")
        .putHex("75 72 65 52 65 73 70 1D 00 00 04 0A 10 01 0B 8C")
        .putHex("98 0C A8 0C")
        .toByteArray()
    val result = Packet().initBuffer(1024)
        .putInt(4 + body.size)
        .putBytes(body)
        .toByteArray()
    val enbody = Crypter().encrypt(result, ByteArray(16))

    val buffer = Packet().initBuffer(1024)
        .putInt(10)
        .putByte(0x02.toByte())
        .putByte(0x00.toByte())
        .putInt(5)
        .putByte(0x30.toByte())
        .putBytes(enbody)
        .toByteArray()
    val packet = Packet().initBuffer(1024)
        .putInt(4 + buffer.size)
        .putBytes(buffer)
        .toByteArray()

    return packet
}
