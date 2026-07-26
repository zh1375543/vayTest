package com.vaycore.finance.message

import com.vaycore.finance.base.BaseActivity
import com.vaycore.finance.databinding.ActivityNoticeDetailBinding
import com.vaycore.finance.model.message.MessageRecord
import com.vaycore.finance.util.viewBinding

class NoticeDetailActivity :
    BaseActivity<ActivityNoticeDetailBinding>() {

    override val binding by viewBinding(ActivityNoticeDetailBinding::inflate)
    private val messageRecord by lazy { intent.getParcelableExtra<MessageRecord>("msg") }

    override fun initView() = with(binding) {
        message = messageRecord
    }
}
