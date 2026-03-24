package moe.ore.android

import android.annotation.SuppressLint
import android.content.Context
import kotlinx.serialization.ExperimentalSerializationApi

@SuppressLint("StaticFieldLeak")
object AndroKtx {
    var isInit: Boolean = false
    lateinit var context: Context

    @OptIn(ExperimentalSerializationApi::class)
    fun init(context: Context) {
        this.context = context
    }
}
