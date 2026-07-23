package com.vaycore.finance.ui.sidepage.act

import androidx.activity.viewModels
import com.vaycore.finance.BuildConfig
import com.vaycore.finance.R
import com.vaycore.finance.base.BaseActivity
import com.vaycore.finance.databinding.SidepageHelpCenterActivityBinding
import com.vaycore.finance.ui.extension.singleClick
import com.vaycore.finance.ui.showContactUsDialog
import com.vaycore.finance.ui.viewmodels.DashboardViewModel
import com.vaycore.finance.util.viewBinding

/** Support landing page for the side-page experience. */
class HelpCenterActivity : BaseActivity<SidepageHelpCenterActivityBinding>() {

    override val binding by viewBinding(SidepageHelpCenterActivityBinding::inflate)
    private val vm by viewModels<DashboardViewModel>()

    override fun initView() = with(binding) {
        applyTopInset(root)
        titleBar.setNavigationAction(::finish)
        tvAppName.text = getString(R.string.app_name)
        tvVersion.text = getString(R.string.version_value, BuildConfig.VERSION_NAME)

        tvContactUs.singleClick {
            vm.getUnAuthData(true)
        }
        tvFaq.singleClick {

        }

    }

    override fun initObserve() = with(vm) {
        super.initObserve()
        unAuthResult.observe(this@HelpCenterActivity) {
            it?.let(::showContactUsDialog)
        }
    }
}
