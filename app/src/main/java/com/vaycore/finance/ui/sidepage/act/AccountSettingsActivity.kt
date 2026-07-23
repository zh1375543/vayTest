package com.vaycore.finance.ui.sidepage.act

import com.vaycore.finance.base.BaseActivity
import com.vaycore.finance.databinding.SidepageAccountSettingsActivityBinding
import com.vaycore.finance.ui.activities.LoginActivity
import com.vaycore.finance.ui.extension.singleClick
import com.vaycore.finance.ui.showConfirmDialog
import com.vaycore.finance.util.SPUtil
import com.vaycore.finance.util.start
import com.vaycore.finance.util.viewBinding

/** Settings entry point dedicated to the side-page experience. */
class AccountSettingsActivity : BaseActivity<SidepageAccountSettingsActivityBinding>() {

    override val binding by viewBinding(SidepageAccountSettingsActivityBinding::inflate)

    override fun initView() = with(binding) {
        applyTopInset(root)
        titleBar.setNavigationAction(::finish)

        tvDeleteAccount.singleClick {
            start<AccountDeletionActivity>()
        }
        tvLogOut.singleClick {
            showConfirmDialog {
                SPUtil.getInstance().clear()
                LoginActivity.launchForPortal(this@AccountSettingsActivity)
                finish()
            }
        }
    }
}
