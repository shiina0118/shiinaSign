package moe.ore.txhook.app.fragment

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Parcel
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.protobuf.ProtoBuf
import moe.ore.android.toast.Toast
import moe.ore.protocol.SSOLoginMerge
import moe.ore.script.Consist
import moe.ore.txhook.R
import moe.ore.txhook.app.CatchProvider
import moe.ore.txhook.app.Md5Activity
import moe.ore.txhook.app.PacketActivity
import moe.ore.txhook.app.TeaActivity
import moe.ore.txhook.app.TlvActivity
import moe.ore.txhook.app.fragment.MainFragment.Packet.CREATOR.MQQ
import moe.ore.txhook.app.fragment.MainFragment.Packet.CREATOR.QQLITE
import moe.ore.txhook.app.fragment.MainFragment.Packet.CREATOR.TIM
import moe.ore.txhook.databinding.FragmentMainBinding
import moe.ore.txhook.databinding.ListElemBinding
import moe.ore.txhook.forEachL
import moe.ore.txhook.helper.EMPTY_BYTE_ARRAY
import moe.ore.txhook.helper.FormatUtil
import moe.ore.txhook.helper.ZipUtil
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@ExperimentalSerializationApi
class MainFragment: Fragment() {
    companion object {
        @ExperimentalSerializationApi
        val catchList: ArrayList<Packet> by lazy { arrayListOf() }
        @ExperimentalSerializationApi
        val actionList: ArrayList<Action> by lazy { arrayListOf() }
    }

    lateinit var uiHandler: Handler
    lateinit var binding: FragmentMainBinding
    lateinit var catchingAdapter: ArrayAdapter<Packet>
    lateinit var actionAdapter: ArrayAdapter<Action>

    // 处于什么模式 0 抓sso 1 抓取行为
    var inMode: Int = 0

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = FragmentMainBinding.inflate(inflater)
        .also { binding = it }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.delete.setOnClickListener {
            (binding.catchContent.adapter as ArrayAdapter<*>).clear()

            Toast.toast(requireContext(), "清空成功")
        }

        notifyDataSetChanged()

        CatchProvider.catchHandler = object: CatchProvider.Companion.CatchHandler() {
            override fun handleMd5(source: Int, data: ByteArray, result: ByteArray) {
                add(Action(3).also {
                    it.buffer = data
                    it.result = result
                    it.source = source
                    it.from = true
                })
            }

            override fun handleTlvSet(tlv: Int, buf: ByteArray, source: Int) {
                add(Action(2).also {
                    it.buffer = buf
                    it.what = tlv
                    it.source = source
                    it.from = true
                })
            }

            override fun handleTlvGet(tlv: Int, buf: ByteArray, source: Int) {
                add(Action(2).also {
                    it.buffer = buf
                    it.what = tlv
                    it.source = source
                    it.from = false
                })
            }

            override fun handlePacket(time: Long, packet: Packet) {
                if (packet.cmd == "SSO.LoginMerge") {
                    var buf = packet.buffer.let { it.sliceArray(4 until it.size) }
                    if (buf.first() == 0x78.toByte()) {
                        buf = ZipUtil.unCompress(buf)
                    }
                    ProtoBuf.decodeFromByteArray<SSOLoginMerge.BusiBuffData>(buf).buffList?.forEach {
                        add(Packet(packet.from,it.cmd, it.seq, it.data, packet.time, packet.uin, packet.msgCookie, packet.type, packet.source,
                            merge = false))
                    }
                } else uiHandler.post {
                    add(packet)
                }
            }

            override fun handleTea(
                isEnc: Boolean,
                data: ByteArray,
                key: ByteArray,
                result: ByteArray,
                source: Int
            ) {
                add(Action(if (isEnc) 0 else 1).also {
                    it.buffer = data
                    it.result = result
                    it.from = !isEnc
                    it.source = source
                    it.key = key
                })
            }
        }

        this.uiHandler = Handler(Looper.getMainLooper())
        val listView = binding.catchContent

        this.actionAdapter = object: ArrayAdapter<Action>(requireContext(), R.layout.list_elem, actionList) {
            @SuppressLint("SetTextI18n")
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val binding = if (convertView != null) {
                    ListElemBinding.bind(convertView)
                } else ListElemBinding.inflate(LayoutInflater.from(requireContext()), parent, false)

                getItem(position)?.let {

                    binding.icon.setImageResource(when (it.source) {
                        QQLITE, MQQ -> R.drawable.icon_mobileqq
                        TIM -> R.drawable.icon_tim
                        else -> R.drawable.icon_mobileqq
                    })

                    binding.cmd.text = when(it.type) {
                        0 -> "Tea加密"
                        1 -> "Tea解密"
                        2 -> {
                            "T${Integer.toHexString(it.what)}${
                                if(it.from) "解密" else "加密"
                            }"
                        }
                        3 -> "Md5"
                        else -> "未知行为"
                    }
                    binding.seq.visibility = GONE
                    binding.size.text = FormatUtil.formatFileSize(it.buffer.size.toLong())

                    binding.time.text = dateToString(Date(it.time), "HH:mm:ss")
                    binding.uin.text = "unknown"

                    binding.mode.setImageResource(
                        if (it.from)
                            R.drawable.ic_baseline_call_received_24
                        else
                            R.drawable.ic_baseline_call_made_24
                    )
                }
                return binding.root
            }

            override fun clear() {
                if (actionList.isNotEmpty()) {
                    // 玩点小动画
                    val hideAnimation: Animation = AlphaAnimation(1f, 0f)
                    hideAnimation.duration = 200
                    listView.startAnimation(hideAnimation)

                    super.clear()
                }

                this@MainFragment.notifyDataSetChanged()
            }
        }
        this.catchingAdapter = object: ArrayAdapter<Packet>(requireContext(), R.layout.list_elem, catchList) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val binding = if (convertView != null) {
                    ListElemBinding.bind(convertView)
                } else ListElemBinding.inflate(LayoutInflater.from(requireContext()), parent, false)

                getItem(position)?.let {

                    binding.icon.setImageResource(when (it.source) {
                        QQLITE, MQQ -> R.drawable.icon_mobileqq
                        TIM -> R.drawable.icon_tim
                        else -> R.drawable.icon_mobileqq
                    })

                    binding.cmd.text = it.cmd.let { if (it.length <= 25) it else it.substring(0, 25) + "..." }
                    binding.seq.text = it.seq.toString()
                    binding.size.text = FormatUtil.formatFileSize(it.buffer.size.toLong())

                    binding.time.text = dateToString(Date(it.time), "HH:mm:ss")
                    binding.uin.text = it.uin.toString()

                    binding.mode.setImageResource(
                        if (it.from)
                            R.drawable.ic_baseline_call_received_24
                        else
                            R.drawable.ic_baseline_call_made_24
                    )

                    if (it.merge) {
                        binding.tipsView.visibility = VISIBLE
                    }
                }
                return binding.root
            }

            override fun clear() {
                if (catchList.isNotEmpty()) {
                    // 玩点小动画
                    val hideAnimation: Animation = AlphaAnimation(1f, 0f)
                    hideAnimation.duration = 200
                    listView.startAnimation(hideAnimation)

                    super.clear()
                }

                this@MainFragment.notifyDataSetChanged()
            }
        }
            .also { listView.adapter = it } // 设置主adapter

        val spinner = binding.spinner
        spinner.onItemSelectedListener = object: AdapterView.OnItemSelectedListener {
            override fun onItemSelected(_1: AdapterView<*>?, _2: View?, position: Int, _3: Long) {
                uiHandler.post {
                    if (inMode != position) {
                        notifyDataSetChangedNoUI()
                        this@MainFragment.inMode = position
                        listView.adapter = when(position) {
                            0 -> catchingAdapter
                            1 -> actionAdapter
                            else -> error("unknown adapter")
                        }
                        notifyDataSetChanged(true)
                    }
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
        spinner.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, arrayOf(
            "sso", "action"
        )).also {
            it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }

        listView.setOnItemClickListener { parent, _, position, _ ->
            when (inMode) {
                0 -> {
                    val intent = Intent(context, PacketActivity::class.java)
                    intent.putExtra("data", parent.getItemAtPosition(position) as Parcelable)
                    startActivity(intent)
                }
                1 -> {
                    val action = parent.getItemAtPosition(position) as Action
                    val intent = when (action.type) {
                        2 -> Intent(context, TlvActivity::class.java)
                        3 -> Intent(context, Md5Activity::class.java)
                        else -> Intent(context, TeaActivity::class.java)
                    }
                    intent.putExtra("data", action)
                    startActivity(intent)
                }
            }
        }
    }

    private fun removeByName(name: String) {
        if (inMode == 0) {
            catchList.forEachL { index, packet ->
                if (packet.cmd == name) catchList.removeAt(index)
            }
            notifyDataSetChanged()
        }
    }

    private fun remove(position: Int) {
        if (inMode == 0)
            catchList.removeAt(position)
        else if (inMode == 1)
            actionList.removeAt(position)
        notifyDataSetChanged()
    }

    private fun add(packet: Packet) {
        uiHandler.post {
            kotlin.runCatching {
                if (Consist.isCatch) {
                    catchList.add(0, packet)
                    notifyDataSetChanged()
                }
            }.onFailure {
                it.printStackTrace()
            }
        }
    }

    private fun add(action: Action) {
        uiHandler.post {
            kotlin.runCatching {
                if (Consist.isCatch) {
                    actionList.add(0, action)
                    notifyDataSetChanged()
                }
            }.onFailure {
                it.printStackTrace()
            }
        }
    }

    private fun notifyDataSetChangedNoUI() {
        if (this::catchingAdapter.isInitialized)
            catchingAdapter.notifyDataSetChanged()
        if (this::actionAdapter.isInitialized)
            actionAdapter.notifyDataSetChanged()
    }

    private fun notifyDataSetChanged(ani: Boolean = true) {
        notifyDataSetChangedNoUI()

        if ( (catchList.isNotEmpty()) || actionList.isNotEmpty()) {
            binding.catchView.visibility = VISIBLE
        } else {
            if (binding.catchView.visibility == VISIBLE && ani) {
                val showAnimation: Animation = AlphaAnimation(0f, 1f)
                showAnimation.duration = 150
                binding.root.startAnimation(showAnimation)
            }
            binding.catchView.visibility = GONE
        }
    }

    private fun dateToString(data: Date, formatType: String): String {
        return SimpleDateFormat(formatType, Locale.ROOT).format(data)
    }

    // 0 tea enc
    // 1 tea dec
    data class Action(
        var type: Int = 0
    ): Parcelable {
        var what: Int = 0
        var from: Boolean = false
        val time: Long = System.currentTimeMillis()
        var buffer: ByteArray = EMPTY_BYTE_ARRAY
        var result: ByteArray = EMPTY_BYTE_ARRAY
        var source: Int = 0
        var key: ByteArray = EMPTY_BYTE_ARRAY

        constructor(parcel: Parcel) : this(parcel.readInt()) {
            what = parcel.readInt()
            from = parcel.readByte() != 0.toByte()
            buffer = parcel.createByteArray()!!
            result = parcel.createByteArray()!!
            source = parcel.readInt()
            key = parcel.createByteArray()!!
        }

        override fun writeToParcel(parcel: Parcel, flags: Int) {
            parcel.writeInt(type)
            parcel.writeInt(what)
            parcel.writeByte(if (from) 1 else 0)
            parcel.writeByteArray(buffer)
            parcel.writeByteArray(result)
            parcel.writeInt(source)
            parcel.writeByteArray(key)
        }

        override fun describeContents(): Int {
            return 0
        }

        companion object CREATOR : Parcelable.Creator<Action> {
            override fun createFromParcel(parcel: Parcel): Action {
                return Action(parcel)
            }

            override fun newArray(size: Int): Array<Action?> {
                return arrayOfNulls(size)
            }
        }
    }

    data class Packet(
        var from: Boolean = false,
        var cmd: String = "wtlogin.login",
        var seq: Int = 0,
        var buffer: ByteArray = EMPTY_BYTE_ARRAY,
        var time: Long = 0L,
        var uin: Long = 0L,

        var msgCookie: ByteArray = EMPTY_BYTE_ARRAY,
        var type: String = "",

        var source: Int = 0,
        var merge: Boolean = false,
        var hash: Int = 0,
    ): Parcelable {
        constructor(parcel: Parcel) : this(
            parcel.readByte() != 0.toByte(),
            parcel.readString()!!,
            parcel.readInt(),
            parcel.createByteArray()!!,
            parcel.readLong(),
            parcel.readLong(),
            parcel.createByteArray()!!,
            parcel.readString()!!,
            parcel.readInt(),
            parcel.readByte() != 0.toByte(),
            parcel.readInt()
        )

        override fun writeToParcel(parcel: Parcel, flags: Int) {
            parcel.writeByte(if (from) 1 else 0)
            parcel.writeString(cmd)
            parcel.writeInt(seq)
            parcel.writeByteArray(buffer)
            parcel.writeLong(time)
            parcel.writeLong(uin)
            parcel.writeByteArray(msgCookie)
            parcel.writeString(type)
            parcel.writeInt(source)
            parcel.writeByte(if(merge) 1 else 0)
            parcel.writeInt(hash)
        }

        override fun describeContents(): Int {
            return 0
        }

        companion object CREATOR : Parcelable.Creator<Packet> {
            const val MQQ = 0
            const val TIM = 1
            const val QQLITE = 2
            const val QIDIAN = 3

            override fun createFromParcel(parcel: Parcel): Packet {
                return Packet(parcel)
            }

            override fun newArray(size: Int): Array<Packet?> {
                return arrayOfNulls(size)
            }

            fun create(buffer: ByteArray): Packet {
                return Packet(buffer = buffer)
            }
        }
    }
}
