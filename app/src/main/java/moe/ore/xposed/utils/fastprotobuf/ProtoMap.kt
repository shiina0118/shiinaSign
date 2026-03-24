package moe.ore.xposed.utils.fastprotobuf

import com.google.protobuf.ByteString
import com.google.protobuf.CodedOutputStream
import com.google.protobuf.WireFormat
import kotlinx.serialization.json.JsonElement

class ProtoMap(
    val value: HashMap<Int, ProtoValue>
): ProtoValue {
    constructor(): this(hashMapOf())

    override fun has(vararg tags: Int): Boolean {
        var curMap: ProtoMap = this
        tags.forEachIndexed { index, tag ->
            if (tag !in curMap) {
                return false
            }
            if (index == tags.size - 1) {
                return true
            }
            curMap = curMap[tag].asMap
        }
        return true
    }

    override fun contains(tag: Int): Boolean {
        return value.containsKey(tag)
    }

    override fun set(tag: Int, v: ProtoValue) {
        if (!contains(tag)) {
            value[tag] = v
        } else {
            val oldValue = value[tag]!!
            if (oldValue is ProtoList) {
                oldValue.add(v)
            } else {
                value[tag] = ProtoList(arrayListOf(oldValue, v))
            }
        }
    }

    override fun set(tag: Int, v: Number) {
        if (!contains(tag)) {
            value[tag] = ProtoNumber(v)
        } else {
            val oldValue = value[tag]!!
            if (oldValue is ProtoList) {
                oldValue.add(v.proto)
            } else {
                value[tag] = ProtoList(arrayListOf(oldValue, v.proto))
            }
        }
    }

    override fun get(vararg tags: Int): ProtoValue {
        var curMap = value
        tags.forEachIndexed { index, tag ->
            if (index == tags.size - 1) {
                return curMap[tag] ?: error("Tag $tag not found")
            }
            curMap[tag]?.let { v ->
                if (v is ProtoMap) {
                    curMap = v.value
                } else {
                    return v
                }
            } ?: error("Tag $tag not found")
        }
        error("Instance is not ProtoMap")
    }

    override fun size(): Int {
        return value.size
    }

    operator fun set(vararg tags: Int, v: ProtoValue) {
        var curProtoMap: ProtoMap = this
        tags.forEachIndexed { index, tag ->
            if (index == tags.size - 1) {
                return@forEachIndexed
            }
            if (!curProtoMap.contains(tag)) {
                val tmp = ProtoMap(hashMapOf())
                curProtoMap[tag] = tmp
                curProtoMap = tmp
            } else {
                curProtoMap = curProtoMap[tag].asMap
            }
        }
        curProtoMap[tags.last()] = v
    }

    operator fun set(vararg tags: Int, struct: (ProtoMap) -> Unit) {
        val map = ProtoMap()
        struct.invoke(map)
        set(*tags, v = map)
    }