package com.vaycore.finance.sidepage.act

import com.vaycore.finance.base.BaseActivity
import com.vaycore.finance.databinding.SidepageFaqActivityBinding
import com.vaycore.finance.util.viewBinding

/** Displays the fixed frequently asked questions for savings plans. */
class FaqActivity : BaseActivity<SidepageFaqActivityBinding>() {

    override val binding by viewBinding(SidepageFaqActivityBinding::inflate)

    override fun initView() = with(binding) {
        applyTopInset(root)
        titleBar.setNavigationAction(::finish)
    }
}
