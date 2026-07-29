package com.vaycore.finance.mine

import com.vaycore.finance.BuildConfig
import com.vaycore.finance.base.BaseActivity
import com.vaycore.finance.databinding.ActivitySettingsBinding
import com.vaycore.finance.ui.showConfirmDialog
import com.vaycore.finance.ui.extension.singleClick
import com.vaycore.finance.util.start
import com.vaycore.finance.util.viewBinding

class SetActivity : BaseActivity<ActivitySettingsBinding>() {

    override val binding by viewBinding(ActivitySettingsBinding::inflate)
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
