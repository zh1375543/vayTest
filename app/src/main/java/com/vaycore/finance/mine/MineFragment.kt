package com.vaycore.finance.mine

import androidx.fragment.app.viewModels
import com.vaycore.finance.R
import com.vaycore.finance.base.BaseFragment
import com.vaycore.finance.data.ACT_inMy
import com.vaycore.finance.data.PageMine
import com.vaycore.finance.data.PRIVACY_POLICY
import com.vaycore.finance.data.bean.TrackBean
import com.vaycore.finance.data.loginInfo
import com.vaycore.finance.databinding.MineFragmentBinding
import com.vaycore.finance.loan.viewmodel.LoanDashboardViewModel
import com.vaycore.finance.order.BorrowingHistoryActivity
import com.vaycore.finance.payback.BulkRepaymentActivity
import com.vaycore.finance.payback.createPaybackDialog
import com.vaycore.finance.browser.WebViewActivity
import com.vaycore.finance.ui.extension.singleClick
import com.vaycore.finance.util.start
import com.vaycore.finance.util.viewBinding
import com.vaycore.finance.wallet.PayoutAccountListActivity

class MineFragment : BaseFragment<MineFragmentBinding>(
    R.layout.mine_fragment
) {
    override val binding by viewBinding(MineFragmentBinding::bind)

    private val vm by viewModels<LoanDashboardViewModel>()

    private val paybackDialog by lazy {
        requireContext().createPaybackDialog()
    }

    override fun initView() = with(binding) {

        tvContactUs.singleClick {
            context?.start<ContactsActivity>()
        }
        tvAboutUs.singleClick {
            context?.start<AboutActivity>()
        }
        tvSettings.singleClick {
            context?.start<SetActivity>()
        }
        tvPolicy.singleClick {
            WebViewActivity.Companion.launch(
                it.context,
                getString(R.string.privacy_policy),
                PRIVACY_POLICY
            )
        }
        tvAccount.singleClick {
            it.context.start<PayoutAccountListActivity>()
        }
        tvOrder.singleClick {
            context?.start<BorrowingHistoryActivity>()
        }
        tvPayBack.singleClick {
            vm.getAuthData(true)
        }
        tvCert.singleClick {
            it.context.start<VerificationCenterActivity>()
        }
    }

    override fun onResume() {
        super.onResume()
        binding.tvPhone.text = loginInfo?.phone
//        binding.tvPhone.setClickableTextWithScale(
//            String.format(getString(R.string.welcome) + "\n" + loginInfo?.phone),
//            loginInfo?.phone.orEmpty(),
//            binding.root.context.resolveColorCompat(R.color.C_492E0D)
//        )
        vm.submitTrackingEvent(
            TrackBean(
                p = PageMine,
                act = ACT_inMy,
                result = System.currentTimeMillis().toString()
            )
        )
    }

    override fun initObserve() = with(vm) {
        authResult.observe(this@MineFragment) {
            if (it?.showMultipleRepaySign == 1) {
                context?.start<BulkRepaymentActivity>()
            } else {
                paybackDialog.show()
            }
        }
    }
}
