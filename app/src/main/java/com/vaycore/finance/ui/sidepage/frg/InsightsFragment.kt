package com.vaycore.finance.ui.sidepage.frg

import com.vaycore.finance.R
import com.vaycore.finance.base.BaseFragment
import com.vaycore.finance.databinding.SidepageStatsFragmentBinding
import com.vaycore.finance.util.viewBinding

/** Insights page without analytics hooks. */
class InsightsFragment : BaseFragment<SidepageStatsFragmentBinding>(
    R.layout.sidepage_stats_fragment
) {
    override val binding by viewBinding(SidepageStatsFragmentBinding::bind)

    override fun initView() = Unit

    override fun initObserve() = Unit
}
