package moe.ore.txhook.app.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.SwitchCompat
import androidx.fragment.app.Fragment
import moe.ore.android.dialog.Dialog
import moe.ore.android.toast.Toast
import moe.ore.android.util.FuckSettingItem
import moe.ore.txhook.databinding.FragmentSettingBinding
import moe.ore.xposed.utils.PrefsManager
import moe.ore.xposed.utils.PrefsManager.KEY_PUSH_API

class SettingFragment: Fragment() {
    private lateinit var binding: FragmentSettingBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = FragmentSettingBinding.inflate(inflater, container, false).also {
        this.binding = it
    }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val addressText = binding.address
        addressText.text = PrefsManager.getString(KEY_PUSH_API).ifBlank { "未配置地址" }

        FuckSettingItem.setSwitchListener(binding.pushApi.also {
            if (PrefsManager.getString(KEY_PUSH_API).isNotEmpty())
                FuckSettingItem.turnSettingSwitch(it, true)
        }) {
            if ((it as SwitchCompat).isChecked) {
                Dialog.EditTextAlertBuilder(requireContext())
                    .setTitle("输入目标地址")
                    .setTextListener { text ->
                        val finalText = text?.takeIf { it.isNotBlank() } ?: "192.168.31.63:6779"
                        PrefsManager.setString(KEY_PUSH_API, finalText.toString())
                        addressText.text = finalText
                        Toast.toast(requireContext(), "Push服务配置成功")
                        FuckSettingItem.turnSettingSwitch(binding.pushApi, true)
                    }
                    .setFloatingText("请输入你自己的Domain：")
                    .setHint("192.168.31.63:6779")
                    .setPositiveButton("确定") { dialog, _ ->
                        dialog.dismiss()
                    }.setOnCancelListener {
                        FuckSettingItem.turnSettingSwitch(binding.pushApi, false)
                    }
                    .show()
            } else {
                addressText.text = "未配置服务"
                PrefsManager.setString(KEY_PUSH_API, "")
                Toast.toast(requireContext(), "Push服务已关闭")
            }
        }
    }
}
