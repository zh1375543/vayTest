package com.vaycore.finance.ui.sidepage.frg

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.vaycore.finance.R
import com.vaycore.finance.base.BaseFragment
import com.vaycore.finance.databinding.SidepageOrderFragmentBinding
import com.vaycore.finance.util.viewBinding

/** Hosts the plan status tabs and their corresponding plan lists. */
class RecordsFragment : BaseFragment<SidepageOrderFragmentBinding>(
    R.layout.sidepage_order_fragment
) {
    override val binding by viewBinding(SidepageOrderFragmentBinding::bind)

    override fun initView() = with(binding) {
        vpPlans.apply {
            isUserInputEnabled = false
            offscreenPageLimit = PLAN_STATUS_COUNT - 1
            adapter = object : FragmentStateAdapter(this@RecordsFragment) {
                override fun getItemCount(): Int = PLAN_STATUS_COUNT

                override fun createFragment(position: Int): Fragment = when (position) {
                    ACTIVE_PAGE -> ActivePlansFragment()
                    COMPLETED_PAGE -> CompletedPlansFragment()
                    else -> CancelledPlansFragment()
                }
            }
            registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    updateSelectedTab(position)
                }
            })
        }

        tvActive.setOnClickListener { selectPage(ACTIVE_PAGE) }
        tvCompleted.setOnClickListener { selectPage(COMPLETED_PAGE) }
        tvCancelled.setOnClickListener { selectPage(CANCELLED_PAGE) }

        updateSelectedTab(ACTIVE_PAGE)
    }

    override fun initObserve() = Unit

    private fun selectPage(page: Int) {
        binding.vpPlans.setCurrentItem(page, true)
    }

    private fun updateSelectedTab(page: Int) = with(binding) {
        tvActive.isSelected = page == ACTIVE_PAGE
        tvCompleted.isSelected = page == COMPLETED_PAGE
        tvCancelled.isSelected = page == CANCELLED_PAGE
    }

    private companion object {
        const val ACTIVE_PAGE = 0
        const val COMPLETED_PAGE = 1
        const val CANCELLED_PAGE = 2
        const val PLAN_STATUS_COUNT = 3
    }
}
