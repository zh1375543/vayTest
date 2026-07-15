package com.vaycore.finance.ui.activities

import androidx.lifecycle.lifecycleScope
import com.vaycore.finance.R
import com.vaycore.finance.base.BaseActivity
import com.vaycore.finance.databinding.RepaymentSuccessActivityBinding
import com.vaycore.finance.util.AppStackUtil
import com.vaycore.finance.util.countdownTimer
import com.vaycore.finance.util.context.getColor2
import com.vaycore.finance.ui.extension.setClickableTextWithScale
import com.vaycore.finance.ui.extension.singleClick
import com.vaycore.finance.util.viewBinding

class RepaymentSuccessActivity :
    BaseActivity<RepaymentSuccessActivityBinding>() {

    override val binding by viewBinding(RepaymentSuccessActivityBinding::inflate)
    override fun initView() = with(binding) {
        onBackAction(null) {
            handleBack()
        }

        binding.tvTips.setClickableTextWithScale(
            String.format(getString(R.string.back_to_home_tips), "10"),
            "10",
            getColor2(R.color.C_FA560D)
        )
        lifecycleScope.countdownTimer(
            10,
            next = { seconds ->
                binding.tvTips.setClickableTextWithScale(
                    String.format(getString(R.string.back_to_home_tips), seconds.toString()),
                    seconds.toString(),
                    getColor2(R.color.C_FA560D)
                )
            },
            end = {
                handleBack()
            }
        )
        tvOK.singleClick {
            handleBack()
        }
    }

    private fun handleBack() {
        AppStackUtil.finishActivity(LoanOrderListActivity::class.java)
        AppStackUtil.finishActivity(LoanOrderDetailActivity::class.java)
        MainActivity.launch(this)
        finish()
    }
}
