package com.vaycore.finance.ui.sidepage.frg

import com.vaycore.finance.R
import com.vaycore.finance.base.BaseFragment
import com.vaycore.finance.databinding.SidepageMineFragmentBinding
import com.vaycore.finance.util.viewBinding

/** Account page without analytics hooks. */
class AccountFragment : BaseFragment<SidepageMineFragmentBinding>(
    R.layout.sidepage_mine_fragment
) {
    override val binding by viewBinding(SidepageMineFragmentBinding::bind)

    override fun initView() = Unit

    override fun initObserve() = Unit
}
