package com.vaycore.finance.ui.sidepage.frg

import android.content.Intent
import com.vaycore.finance.R
import com.vaycore.finance.base.BaseFragment
import com.vaycore.finance.data.local.loginInfo
import com.vaycore.finance.databinding.SidepageMineFragmentBinding
import com.vaycore.finance.ui.extension.singleClick
import com.vaycore.finance.ui.showAppRatingDialog
import com.vaycore.finance.ui.sidepage.act.AccountSettingsActivity
import com.vaycore.finance.ui.sidepage.act.HelpCenterActivity
import com.vaycore.finance.util.start
import com.vaycore.finance.util.viewBinding

/** Account page for the side-page experience. */
class AccountFragment : BaseFragment<SidepageMineFragmentBinding>(
    R.layout.sidepage_mine_fragment
) {
    override val binding by viewBinding(SidepageMineFragmentBinding::bind)

    override fun initView() = with(binding) {
        tvRate.singleClick {
            activity?.showAppRatingDialog { }
        }
        tvShareApp.singleClick {
            shareApp()
        }
        tvSettings.singleClick {
            context?.start<AccountSettingsActivity>()
        }
        tvHelpCenter.singleClick {
            context?.start<HelpCenterActivity>()
        }
    }

    override fun onResume() {
        super.onResume()
        binding.tvPhone.text = loginInfo?.phone
    }

    private fun shareApp() {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(
                Intent.EXTRA_TEXT,
                "https://play.google.com/store/apps/details?id=${requireContext().packageName}"
            )
        }
        startActivity(Intent.createChooser(shareIntent, getString(R.string.share_app)))
    }

    override fun initObserve() = Unit
}
