package com.vaycore.finance.ui.activities.mine

import com.vaycore.finance.BuildConfig
import com.vaycore.finance.base.BaseActivity
import com.vaycore.finance.databinding.SetActivityBinding
import com.vaycore.finance.ui.showConfirmDialog
import com.vaycore.finance.ui.extension.singleClick
import com.vaycore.finance.util.start
import com.vaycore.finance.util.viewBinding

class SetActivity : BaseActivity<SetActivityBinding>() {

    override val binding by viewBinding(SetActivityBinding::inflate)
    override fun initView() = with(binding) {
        tvVersion.text = BuildConfig.VERSION_NAME
        tvCloseAccount.singleClick {
            start<LogoutActivity>()
        }
        tvFeedback.singleClick {
            start<FeedActivity>()
        }
        tvLogout.singleClick {
            showConfirmDialog {
                logOut()
            }
        }
    }
}
