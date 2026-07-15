package com.vaycore.finance.ui.fragments

import androidx.fragment.app.viewModels
import com.vaycore.finance.R
import com.vaycore.finance.ui.activities.mine.AboutActivity
import com.vaycore.finance.ui.activities.BankCardListActivity
import com.vaycore.finance.ui.activities.mine.ContactsActivity
import com.vaycore.finance.ui.activities.LoanOrderListActivity
import com.vaycore.finance.ui.activities.mine.SetActivity
import com.vaycore.finance.ui.activities.WebViewActivity
import com.vaycore.finance.base.BaseFragment
import com.vaycore.finance.data.local.PRIVACY_POLICY
import com.vaycore.finance.data.local.bean.TrackBean
import com.vaycore.finance.ui.viewmodels.DashboardViewModel
import com.vaycore.finance.databinding.MineFragmentBinding
import com.vaycore.finance.data.ACT_inMy
import com.vaycore.finance.data.PageMine
import com.vaycore.finance.data.local.loginInfo
import com.vaycore.finance.ui.extension.singleClick
import com.vaycore.finance.ui.activities.mine.AuthCenterActivity
import com.vaycore.finance.ui.activities.RepaymentBatchActivity
import com.vaycore.finance.ui.createPaybackDialog
import com.vaycore.finance.util.start
import com.vaycore.finance.util.viewBinding

class MineFragment : BaseFragment<MineFragmentBinding>(
    R.layout.mine_fragment
) {
    override val binding by viewBinding(MineFragmentBinding::bind)

    private val vm by viewModels<DashboardViewModel>()

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
            WebViewActivity.launch(
                it.context,
                getString(R.string.privacy_policy),
                PRIVACY_POLICY
            )
        }
        tvAccount.singleClick {
            it.context.start<BankCardListActivity>()
        }
        tvOrder.singleClick {
            context?.start<LoanOrderListActivity>()
        }
        tvPayBack.singleClick {
            vm.getAuthData(true)
        }
        tvCert.singleClick {
            it.context.start<AuthCenterActivity>()
        }
    }

    override fun onResume() {
        super.onResume()
        binding.tvPhone.text = loginInfo?.phone
//        binding.tvPhone.setClickableTextWithScale(
//            String.format(getString(R.string.welcome) + "\n" + loginInfo?.phone),
//            loginInfo?.phone.orEmpty(),
//            binding.root.context.getColor2(R.color.C_492E0D)
//        )
        vm.recordEvent(
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
                context?.start<RepaymentBatchActivity>()
            } else {
                paybackDialog.show()
            }
        }
    }
}
