@file:OptIn(ExperimentalSerializationApi::class)

package moe.ore.txhook.app

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.view.KeyEvent
import androidx.core.app.WindowsCompat
import androidx.core.app.WindowsCompat.transWindowsHide
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.tabs.TabLayoutMediator
import kotlinx.serialization.ExperimentalSerializationApi
import moe.ore.android.AndroKtx
import moe.ore.android.EasyActivity
import moe.ore.android.tab.TabFlashyAnimator
import moe.ore.android.toast.Toast.toast
import moe.ore.android.util.StatusBarUtil
import moe.ore.script.Consist
import moe.ore.txhook.EntryActivity
import moe.ore.txhook.R
import moe.ore.txhook.app.fragment.MainFragment
import moe.ore.txhook.app.fragment.SettingFragment
import moe.ore.txhook.databinding.ActivityMainBinding

class MainActivity: EasyActivity() {
    lateinit var binding: ActivityMainBinding

    private val mFragmentList: ArrayList<Fragment> = ArrayList()
    internal lateinit var tabFlashyAnimator: TabFlashyAnimator
    private val titles = arrayOf("主页", "设置")

    private var isExit = 0
    private val exitHandler: Handler by lazy {
        object : Handler(mainLooper) {
            override fun handleMessage(msg: Message) {
                super.handleMessage(msg)
                isExit--
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        if (!AndroKtx.isInit) {
            val intent = Intent(this, EntryActivity::class.java)
            startActivity(intent)
            finish()
        }

        super.onCreate(savedInstanceState)

        StatusBarUtil.transparentStatusBar(this)
        StatusBarUtil.setAndroidNativeLightStatusBar(this, true)

        transWindowsHide { WindowsCompat.WINDOWS_FLAG }
        setContentView(binding.root)

        val selectedColor = Color.rgb(105,105,105)

        val toolbar = binding.toolbar
        toolbar.setTitleTextColor(selectedColor)
        toolbar.setSubtitleTextColor(selectedColor)
        toolbar.subtitle = getString(R.string.imqq)
        setSupportActionBar(toolbar)

        mFragmentList.add(MainFragment())
        mFragmentList.add(SettingFragment())

        val viewPager = binding.viewPager
        val adapter: FragmentStateAdapter = object : FragmentStateAdapter(supportFragmentManager, lifecycle) {
            override fun getItemCount(): Int = mFragmentList.size

            override fun createFragment(position: Int): Fragment = mFragmentList[position]
        }
        viewPager.adapter = adapter
        val tabLayout = binding.tabLayout

        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = Consist.titles[position]
        }.attach()

        tabFlashyAnimator = TabFlashyAnimator(tabLayout, selectedColor)
        tabFlashyAnimator.addTabItem(titles[0], R.drawable.ic_events)
        tabFlashyAnimator.addTabItem(titles[1], R.drawable.ic_setting)
        tabFlashyAnimator.highLightTab(0)

        val fab = binding.fab
        fab.imageTintList = ColorStateList.valueOf(ResourcesCompat.getColor(resources, R.color.white, null))
        fab.setOnClickListener {
            val button = it as FloatingActionButton
            if (Consist.isCatch) {
                Consist.isCatch = false
                button.setImageDrawable(ResourcesCompat.getDrawable(resources, R.drawable.icon_nocatch, null))
                button.backgroundTintList = ColorStateList.valueOf(ResourcesCompat.getColor(resources, R.color.accent, null))
            } else {
                Consist.isCatch = true
                button.setImageDrawable(ResourcesCompat.getDrawable(resources, R.drawable.icon_catch, null))
                button.backgroundTintList = ColorStateList.valueOf(ResourcesCompat.getColor(resources, R.color.blue500, null))
            }
        }

        viewPager.registerOnPageChangeCallback(tabFlashyAnimator)
        viewPager.registerOnPageChangeCallback(object: ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                if (position == 0) {
                    fab.show()
                } else {
                    fab.hide()
                }
                if (position == 1)
                    mFragmentList[1].onHiddenChanged(true)
            }
        })
    }

    override fun onStart() {
        super.onStart()

        // authCheck.invoke(this)
        transWindowsHide { WindowsCompat.WINDOWS_FLAG + 1 }
    }

    override fun onStop() {
        super.onStop()
        tabFlashyAnimator.onStop()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            isExit++
            exit()
            return false
        }
        return super.onKeyDown(keyCode, event)
    }


    private fun exit() {
        if (isExit < 2) {
            toast(msg = "再按一次退出应用")
            exitHandler.sendEmptyMessageDelayed(0, 2000)
        } else {
            finish()
            super.onBackPressed()
        }
    }
}