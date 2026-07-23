package com.vaycore.finance.ui.sidepage.act

import androidx.activity.viewModels
import com.vaycore.finance.R
import com.vaycore.finance.base.BaseActivity
import com.vaycore.finance.data.local.loginInfo
import com.vaycore.finance.databinding.SidepageAccountDeletionActivityBinding
import com.vaycore.finance.ui.activities.mine.LogoutSuccessActivity
import com.vaycore.finance.ui.extension.singleClick
import com.vaycore.finance.ui.viewmodels.SessionViewModel
import com.vaycore.finance.util.start
import com.vaycore.finance.util.viewBinding

/** Confirms permanent account deletion for the side-page experience. */
class AccountDeletionActivity : BaseActivity<SidepageAccountDeletionActivityBinding>() {

    override val binding by viewBinding(SidepageAccountDeletionActivityBinding::inflate)
    private val viewModel by viewModels<SessionViewModel>()

    override fun initView() = with(binding) {
        applyTopInset(root)
        titleBar.setNavigationAction(::finish)
        tvAppName.text = getString(R.string.app_name)
        tvAccountNumber.text = loginInfo?.phone.orEmpty().maskPhoneNumber()

        tvConfirm.singleClick {
            viewModel.logout()
        }
    }

    override fun initObserve() = with(viewModel) {
        logoutResult.observe(this@AccountDeletionActivity) {
            start<LogoutSuccessActivity> {
                putExtra(LogoutSuccessActivity.EXTRA_RETURN_TO_PORTAL, true)
            }
            finish()
        }
    }

    private fun String.maskPhoneNumber(): String = when {
        isBlank() -> "-"
        length <= 4 -> this
        length <= 7 -> "${first()}****${last()}"
        else -> "${take(4)}****${takeLast(3)}"
    }
}
