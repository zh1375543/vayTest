package com.vaycore.finance.ui.activities.mine

import com.vaycore.finance.R
import com.vaycore.finance.base.BaseActivity
import com.vaycore.finance.databinding.FeedActivityBinding
import com.vaycore.finance.util.showSoftInput
import com.vaycore.finance.util.showToastMessage
import com.vaycore.finance.ui.extension.singleClick
import com.vaycore.finance.util.viewBinding

class FeedActivity : BaseActivity<FeedActivityBinding>() {

    override val binding by viewBinding(FeedActivityBinding::inflate)
    override fun initView() = with(binding) {
        etContent.requestFocus()
        showSoftInput(etContent)
        tvSubmit.singleClick {
            if (etContent.text.isNullOrBlank()) {
                getString(R.string.enter_feedback).showToastMessage()
                return@singleClick
            }
            getString(R.string.feedback_success).showToastMessage()
            finish()
        }
    }
}
