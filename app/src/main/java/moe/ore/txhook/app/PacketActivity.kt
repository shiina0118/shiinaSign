package moe.ore.txhook.app

import android.os.Bundle
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.google.android.material.tabs.TabLayoutMediator
import kotlinx.serialization.ExperimentalSerializationApi
import moe.ore.android.EasyActivity
import moe.ore.android.util.StatusBarUtil
import com.shiinasign.R
import moe.ore.txhook.app.fragment.MainFragment
import moe.ore.txhook.app.fragment.PacketHexFragment
import moe.ore.txhook.app.fragment.PacketInfoFragment
import moe.ore.txhook.app.fragment.ParserFragment
import com.shiinasign.databinding.ActivityPacketBinding


class PacketActivity: EasyActivity() {
    companion object {
        private val titles = arrayOf("详细", "分析", "HEX")
    }

    private val mFragmentList: ArrayList<Fragment> = ArrayList()
    private lateinit var binding: ActivityPacketBinding

    @OptIn(ExperimentalSerializationApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        StatusBarUtil.setStatusBarColor(this, ResourcesCompat.getColor(resources, R.color.white, null))
        StatusBarUtil.setAndroidNativeLightStatusBar(this, true)

        binding = ActivityPacketBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val packet = intent.getParcelableExtra<MainFragment.Packet>("data")
            ?: error("packet activity packet field is must not null")

        mFragmentList.add(PacketInfoFragment().apply { this.packet = packet })
        mFragmentList.add(ParserFragment().also { it.packet = packet })
        mFragmentList.add(PacketHexFragment().also { it.initBuffer(packet.buffer) })

        val viewPager = binding.viewPager
        val adapter: FragmentStateAdapter = object : FragmentStateAdapter(supportFragmentManager, lifecycle) {
            override fun getItemCount(): Int = mFragmentList.size

            override fun createFragment(position: Int): Fragment = mFragmentList[position]
        }
        viewPager.adapter = adapter
        val tabLayout = binding.tabs

        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = titles[position]
        }.attach()

        binding.back.setOnClickListener { finish() }

        binding.more.setOnClickListener {
            /*
            val mTopRightMenu = TopRightMenu(this)
            val menuItems: MutableList<MenuItem> = ArrayList()
            menuItems.add(MenuItem(R.drawable.icon_catch, "分享出去"))
            menuItems.add(MenuItem(R.drawable.ic_baseline_save_24, "保存文件"))
            // menuItems.add(MenuItem(R.drawable.met_ic_clear, "扫一�?))

            mTopRightMenu
                .setHeight(800) //默认高度480
                .setWidth(400) //默认宽度wrap_content
                .showIcon(true) //显示菜单图标，默认为true
                .dimBackground(true) //背景变暗，默认为true
                .needAnimationStyle(true) //显示动画，默认为true
                .setAnimationStyle(R.style.TRM_ANIM_STYLE)
                .addMenuList(menuItems)
                .setOnMenuItemClickListener { position ->
                    Toast.toast(this, "点击菜单:$position")
                }
                .showAsDropDown(it, -50, 0) //带偏移量*/
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        super.onBackPressed()
        finish()
    }
}
