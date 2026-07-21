package com.vaycore.finance.ui.sidepage.frg

import com.vaycore.finance.R
import com.vaycore.finance.base.BaseFragment
import com.vaycore.finance.databinding.SidepageOrderFragmentBinding
import com.vaycore.finance.util.viewBinding

/** Records page without analytics hooks. */
class RecordsFragment : BaseFragment<SidepageOrderFragmentBinding>(
    R.layout.sidepage_order_fragment
) {
    override val binding by viewBinding(SidepageOrderFragmentBinding::bind)

    override fun initView() = Unit

    override fun initObserve() = Unit
}
