package com.vaycore.finance.payback

import androidx.lifecycle.lifecycleScope
import com.vaycore.finance.R
import com.vaycore.finance.base.BaseActivity
import com.vaycore.finance.databinding.RepaymentSuccessActivityBinding
import com.vaycore.finance.order.BorrowingDetailActivity
import com.vaycore.finance.order.BorrowingHistoryActivity
import com.vaycore.finance.app.MainActivity
import com.vaycore.finance.util.AppStackUtil
import com.vaycore.finance.util.countdownTimer
import com.vaycore.finance.util.context.resolveColorCompat
import com.vaycore.finance.ui.extension.setClickableTextWithScale
import com.vaycore.finance.ui.extension.singleClick
import com.vaycore.finance.util.viewBinding

class RepaymentConfirmationActivity :
    BaseActivity<RepaymentSuccessActivityBinding>() {

    override val binding by viewBinding(RepaymentSuccessActivityBinding::inflate)
    override fun initView() = with(binding) {
        applyTopInset(root)
        registerTrackedBackHandler(null) {
            returnToBorrowingOverview()
        }

        binding.tvTips.setClickableTextWithScale(
            String.format(getString(R.string.back_to_home_tips), "10"),
            "10",
            resolveColorCompat(R.color.color_7087F8)
        )
        lifecycleScope.countdownTimer(
            10,
            next = { seconds ->
                binding.tvTips.setClickableTextWithScale(
                    String.format(getString(R.string.back_to_home_tips), seconds.toString()),
                    seconds.toString(),
                    resolveColorCompat(R.color.color_7087F8)
                )
            },
            end = {
                returnToBorrowingOverview()
            }
        )
        tvOK.singleClick {
            returnToBorrowingOverview()
        }
    }

    private fun returnToBorrowingOverview() {
        AppStackUtil.finishActivity(BorrowingHistoryActivity::class.java)
        AppStackUtil.finishActivity(BorrowingDetailActivity::class.java)
        MainActivity.launch(this)
        finish()
    }
}
