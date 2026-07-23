package com.vaycore.finance.ui.activities

import android.content.Context
import android.content.Intent
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.vaycore.finance.R
import com.vaycore.finance.App
import com.vaycore.finance.base.BaseActivity
import com.vaycore.finance.data.PageHome
import com.vaycore.finance.data.local.bean.TrackBean
import com.vaycore.finance.databinding.MainActivityBinding
import com.vaycore.finance.data.ACT_clickActivity
import com.vaycore.finance.data.ACT_clickMy
import com.vaycore.finance.data.ACT_clickOrder
import com.vaycore.finance.data.ACT_exit
import com.vaycore.finance.data.ACT_in
import com.vaycore.finance.data.PageExit
import com.vaycore.finance.data.local.authConfigList
import com.vaycore.finance.ui.viewmodels.DashboardViewModel
import com.vaycore.finance.ui.fragments.HomeFragment
import com.vaycore.finance.ui.fragments.MineFragment
import com.vaycore.finance.ui.fragments.OrderFragment
import com.vaycore.finance.util.ifLoginAction
import com.vaycore.finance.ui.extension.addStatusBarTopMargin
import com.vaycore.finance.util.showToastMessage
import com.vaycore.finance.ui.extension.singleClick
import com.vaycore.finance.ui.showContactUsDialog
import com.vaycore.finance.util.start

import com.vaycore.finance.util.viewBinding

class MainActivity : BaseActivity<MainActivityBinding>() {

    override val binding by viewBinding(MainActivityBinding::inflate)

    companion object {
        fun launch(
            context: Context,
            page: Int = 0,
            isFromAuth: Boolean = false,
            flags: Int = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        ) {
            context.start<MainActivity> {
                putExtra("page", page)
                putExtra("isFromAuth", isFromAuth)
                addFlags(flags)
            }
        }
    }

    private val vm by viewModels<DashboardViewModel>()

    private var currentPage: Int = 0

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        currentPage = 0
        selectPage(currentPage)
        val isAuthToFillBank = intent.getBooleanExtra("isFromAuth", false)
        if (isAuthToFillBank) {
            vm.getAuthData()
        }
        postDeviceInfo()
    }

    private var lastBackPressTime = 0L
    private val EXIT_INTERVAL = 2000

    override fun initView() = with(binding) {
        vm.recordEvent(
            TrackBean(
                p = PageHome, act = ACT_in
            )
        )
        currentPage = intent.getIntExtra("page", 0)
        ivMsg.addStatusBarTopMargin()
        selectPage(currentPage)
        vpMain.apply {
            offscreenPageLimit = 3
            isUserInputEnabled = false
            adapter = object : FragmentStateAdapter(this@MainActivity) {
                override fun getItemCount(): Int = 3

                override fun createFragment(position: Int): Fragment {
                    return when (position) {
                        0 -> HomeFragment()
                        1 -> OrderFragment()
                        else -> MineFragment()
                    }
                }
            }
            registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    super.onPageSelected(position)
                    binding.apply {
                        tvHome.setSelected(position == 0)
                        tvOrder.setSelected(position == 1)
                        tvMine.setSelected(position == 2)
                        topGroup.isVisible = position != 2
                    }
                }
            })
        }
        vHome.singleClick {
            selectPage(0)
        }
        vOrder.singleClick {
            ifLoginAction {
                selectPage(1)
            }
        }
        vMine.singleClick {
            ifLoginAction {
                selectPage(2)
            }
        }
      
        ivMsg.singleClick {
            ifLoginAction {
                start<NoticeListActivity>()
            }
        }
        onBackPressedDispatcher.addCallback(
            this@MainActivity, object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    val currentTime = System.currentTimeMillis()
                    if (currentTime - lastBackPressTime < EXIT_INTERVAL) {
                        isEnabled = false
                        vm.recordEvent(
                            TrackBean(
                                act = ACT_exit, result = PageHome, p = PageExit
                            )
                        )
                        onBackPressedDispatcher.onBackPressed() // normal back
                    } else {
                        lastBackPressTime = currentTime
                        getString(R.string.again_exit).showToastMessage()
                    }
                }
            })
        postDeviceInfo()
    }

    private fun postDeviceInfo() {
        App.appViewModel.postRiskInfo(PageHome) {}

    }

    fun selectPage(page: Int) {
        binding.apply {
            vpMain.setCurrentItem(page, false)
        }
        vm.recordEvent(
            TrackBean(
                p = PageHome, act = when (page) {
                    0 -> ACT_clickActivity
                    1 -> ACT_clickOrder
                    else -> ACT_clickMy
                }
            )
        )
    }

    override fun initObserve() =with(vm){
        super.initObserve()
        unAuthResult.observe(this@MainActivity) {
            it?.let { homeBean -> showContactUsDialog(homeBean) }
        }
    }

    override fun onResume() {
        super.onResume()
        vm.fetchAuthConfigList {
            authConfigList = it
        }
    }
}
