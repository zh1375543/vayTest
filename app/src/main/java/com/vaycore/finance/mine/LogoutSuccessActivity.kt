package com.vaycore.finance.mine

import androidx.lifecycle.lifecycleScope
import com.vaycore.finance.R
import com.vaycore.finance.base.BaseActivity
import com.vaycore.finance.databinding.ActivityLogoutCompleteBinding
import com.vaycore.finance.app.MainActivity
import com.vaycore.finance.sidepage.PortalActivity
import com.vaycore.finance.sidepage.act.AccountSettingsActivity
import com.vaycore.finance.util.AppStackUtil
import com.vaycore.finance.util.countdownTimer
import com.vaycore.finance.util.context.resolveColorCompat
import com.vaycore.finance.ui.extension.setClickableTextWithScale
import com.vaycore.finance.ui.extension.singleClick
import com.vaycore.finance.util.viewBinding

class LogoutSuccessActivity :
    BaseActivity<ActivityLogoutCompleteBinding>() {

    override val binding by viewBinding(ActivityLogoutCompleteBinding::inflate)
    private val returnToPortal by lazy {
        intent.getBooleanExtra(EXTRA_RETURN_TO_PORTAL, false)
    }

    override fun initView() = with(binding) {
        applyTopInset(root)
        tvOK.singleClick {
            handleBackPressed()
        }
        registerTrackedBackHandler(null) {
            handleBackPressed()
        }
        lifecycleScope.countdownTimer(10, next = { seconds ->
            binding.tvTips.setClickableTextWithScale(
                String.format(getString(R.string.back_to_home_tips), seconds.toString()),
                seconds.toString(),
                resolveColorCompat(R.color.action_withdraw)
            )
        }, end = {
            handleBackPressed()
        })
        tvTips.setClickableTextWithScale(
            String.format(getString(R.string.back_to_home_tips), "10"),
            "10",
            resolveColorCompat(R.color.action_withdraw)
        )
    }

    private fun handleBackPressed() {
        logOut(true)
    }

    companion object {
        const val EXTRA_RETURN_TO_PORTAL = "return_to_portal"
    }
}
