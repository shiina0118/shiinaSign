package moe.ore.txhook.custom

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import android.widget.TextView
import androidx.appcompat.widget.SwitchCompat
import moe.ore.txhook.R

class SettingView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {
    // Public properties that match the original SettingView
    val mRightIcon_switch: SwitchCompat
    var mChecked: Boolean = false
    private val leftTextView: TextView

    init {
        val view = LayoutInflater.from(context).inflate(R.layout.custom_setting_view, this, true)
        mRightIcon_switch = view.findViewById(R.id.setting_switch)
        leftTextView = view.findViewById(R.id.setting_left_text)

        // Parse custom attributes
        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.SettingView)
        try {
            val leftText = typedArray.getString(R.styleable.SettingView_leftText)
            leftTextView.text = leftText

            val rightStyle = typedArray.getString(R.styleable.SettingView_rightStyle)
            if (rightStyle == "iconSwitch") {
                mRightIcon_switch.visibility = VISIBLE
            } else {
                mRightIcon_switch.visibility = GONE
            }
        } finally {
            typedArray.recycle()
        }
    }

    fun setLeftText(text: String) {
        leftTextView.text = text
    }
}