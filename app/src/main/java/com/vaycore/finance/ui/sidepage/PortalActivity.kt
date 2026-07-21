package com.vaycore.finance.ui.sidepage

import android.content.Context
import android.content.Intent
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.core.view.isVisible
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.vaycore.finance.R
import com.vaycore.finance.base.BaseActivity
import com.vaycore.finance.databinding.SidepageMainActivityBinding
import com.vaycore.finance.ui.sidepage.frg.AccountFragment
import com.vaycore.finance.ui.sidepage.frg.InsightsFragment
import com.vaycore.finance.ui.sidepage.frg.OverviewFragment
import com.vaycore.finance.ui.sidepage.frg.RecordsFragment
import com.vaycore.finance.util.viewBinding
import com.vaycore.finance.util.showToastMessage

class PortalActivity : BaseActivity<SidepageMainActivityBinding>() {

    override val binding by viewBinding(SidepageMainActivityBinding::inflate)
    private var lastBackPressTime = 0L

    override fun initView() {
        applyTopInset(binding.root)
        setupPages(intent.getIntExtra(EXTRA_PAGE, OVERVIEW_PAGE))
        setupExitOnBackPress()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        selectPage(intent.getIntExtra(EXTRA_PAGE, OVERVIEW_PAGE))
    }

    private fun setupPages(initialPage: Int) = with(binding) {
        vpMain.apply {
            isUserInputEnabled = false
            offscreenPageLimit = PAGE_COUNT - 1
            adapter = object : FragmentStateAdapter(this@PortalActivity) {
                override fun getItemCount(): Int = PAGE_COUNT

                override fun createFragment(position: Int): Fragment = when (position) {
                    OVERVIEW_PAGE -> OverviewFragment()
                    RECORDS_PAGE -> RecordsFragment()
                    INSIGHTS_PAGE -> InsightsFragment()
                    else -> AccountFragment()
                }
            }
            registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    updatePageUi(position)
                }
            })
        }

        vHome.setOnClickListener { selectPage(OVERVIEW_PAGE) }
        vOrder.setOnClickListener { selectPage(RECORDS_PAGE) }
        vStats.setOnClickListener { selectPage(INSIGHTS_PAGE) }
        vMine.setOnClickListener { selectPage(ACCOUNT_PAGE) }

        selectPage(initialPage)
    }

    fun selectPage(page: Int) {
        val targetPage = page.coerceIn(OVERVIEW_PAGE, ACCOUNT_PAGE)
        updatePageUi(targetPage)
        binding.vpMain.setCurrentItem(targetPage, false)
    }

    private fun updatePageUi(page: Int) = with(binding) {
        tvHome.isSelected = page == OVERVIEW_PAGE
        tvOrder.isSelected = page == RECORDS_PAGE
        tvStats.isSelected = page == INSIGHTS_PAGE
        tvMine.isSelected = page == ACCOUNT_PAGE

        tvAppName.isVisible = page != ACCOUNT_PAGE
        if (page != ACCOUNT_PAGE) {
            tvAppName.setText(
                when (page) {
                    OVERVIEW_PAGE -> R.string.portal_overview_title
                    RECORDS_PAGE -> R.string.portal_records_title
                    else -> R.string.portal_insights_title
                }
            )
        }
    }

    private fun setupExitOnBackPress() {
        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    val currentTime = System.currentTimeMillis()
                    if (currentTime - lastBackPressTime < EXIT_INTERVAL) {
                        isEnabled = false
                        onBackPressedDispatcher.onBackPressed()
                    } else {
                        lastBackPressTime = currentTime
                        getString(R.string.again_exit).showToastMessage()
                    }
                }
            },
        )
    }

    companion object {
        private const val EXTRA_PAGE = "page"
        private const val OVERVIEW_PAGE = 0
        private const val RECORDS_PAGE = 1
        private const val INSIGHTS_PAGE = 2
        private const val ACCOUNT_PAGE = 3
        private const val PAGE_COUNT = 4
        private const val EXIT_INTERVAL = 2_000L

        fun launch(context: Context, page: Int = OVERVIEW_PAGE) {
            context.startActivity(
                Intent(context, PortalActivity::class.java)
                    .putExtra(EXTRA_PAGE, page)
            )
        }
    }
}
