package com.vaycore.finance.ui.activities.mine

import androidx.lifecycle.lifecycleScope
import com.vaycore.finance.R
import com.vaycore.finance.base.BaseActivity
import com.vaycore.finance.databinding.LogoutSuccessActivityBinding
import com.vaycore.finance.ui.activities.MainActivity
import com.vaycore.finance.ui.sidepage.PortalActivity
import com.vaycore.finance.ui.sidepage.act.AccountSettingsActivity
import com.vaycore.finance.util.AppStackUtil
import com.vaycore.finance.util.countdownTimer
import com.vaycore.finance.util.context.getColor2
import com.vaycore.finance.ui.extension.setClickableTextWithScale
import com.vaycore.finance.ui.extension.singleClick
import com.vaycore.finance.util.viewBinding

class LogoutSuccessActivity :
    BaseActivity<LogoutSuccessActivityBinding>() {

    override val binding by viewBinding(LogoutSuccessActivityBinding::inflate)
    private val returnToPortal by lazy {
        intent.getBooleanExtra(EXTRA_RETURN_TO_PORTAL, false)
    }

    override fun initView() = with(binding) {
        applyTopInset(root)
        tvOK.singleClick {
            handleBackPressed()
        }
        onBackAction(null) {
            handleBackPressed()
        }
        lifecycleScope.countdownTimer(10, next = { seconds ->
            binding.tvTips.setClickableTextWithScale(
                String.format(getString(R.string.back_to_home_tips), seconds.toString()),
                seconds.toString(),
                getColor2(R.color.C_FA560D)
            )
        }, end = {
            handleBackPressed()
        })
        tvTips.setClickableTextWithScale(
            String.format(getString(R.string.back_to_home_tips), "10"),
            "10",
            getColor2(R.color.C_FA560D)
        )
    }

    private fun handleBackPressed() {
        logOut(false)
        if (returnToPortal) {
            AppStackUtil.finishActivity(AccountSettingsActivity::class.java)
            PortalActivity.launch(this)
        } else {
            AppStackUtil.finishActivity(SetActivity::class.java)
            MainActivity.launch(this)
        }
    }

    companion object {
        const val EXTRA_RETURN_TO_PORTAL = "return_to_portal"
    }
}
