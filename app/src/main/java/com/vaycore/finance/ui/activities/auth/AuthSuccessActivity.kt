package com.vaycore.finance.ui.activities.auth

import androidx.lifecycle.lifecycleScope
import com.vaycore.finance.R
import com.vaycore.finance.base.BaseActivity
import com.vaycore.finance.databinding.AuthSuccessActivityBinding
import com.vaycore.finance.ui.activities.MainActivity
import com.vaycore.finance.util.countdownTimer
import com.vaycore.finance.util.context.getColor2
import com.vaycore.finance.ui.extension.setClickableTextWithScale
import com.vaycore.finance.ui.extension.singleClick
import com.vaycore.finance.util.viewBinding

class AuthSuccessActivity :
    BaseActivity<AuthSuccessActivityBinding>() {

    override val binding by viewBinding(AuthSuccessActivityBinding::inflate)
    override fun initView() = with(binding) {
        onBackAction(null) {
            finish()
            MainActivity.launch(this@AuthSuccessActivity, isFromAuth = true)
        }
        binding.tvTips.setClickableTextWithScale(
            String.format(getString(R.string.back_to_home_tips), "10"),
            "10",
            getColor2(R.color.C_F62909)
        )
        lifecycleScope.countdownTimer(10, next = { seconds ->
            binding.tvTips.setClickableTextWithScale(
                String.format(getString(R.string.back_to_home_tips), seconds.toString()),
                seconds.toString(),
                getColor2(R.color.C_F62909)
            )
        }, end = {
            BtnOK.performClick()
        })
        BtnOK.singleClick {
            finish()
            MainActivity.launch(this@AuthSuccessActivity, isFromAuth = true)
        }
    }
}
