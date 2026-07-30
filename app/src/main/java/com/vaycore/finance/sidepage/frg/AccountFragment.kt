package com.vaycore.finance.sidepage.frg

import android.content.Intent
import com.vaycore.finance.R
import com.vaycore.finance.base.BaseFragment
import com.vaycore.finance.data.loginInfo
import com.vaycore.finance.databinding.SidepageMineFragmentBinding
import com.vaycore.finance.ui.extension.singleClick
import com.vaycore.finance.sidepage.act.AccountSettingsActivity
import com.vaycore.finance.sidepage.act.HelpCenterActivity
import com.vaycore.finance.util.ExternalActionLauncher
import com.vaycore.finance.util.showToastMessage
import com.vaycore.finance.util.start
import com.vaycore.finance.util.viewBinding

/** Account page for the side-page experience. */
class AccountFragment : BaseFragment<SidepageMineFragmentBinding>(
    R.layout.sidepage_mine_fragment
) {
    override val binding by viewBinding(SidepageMineFragmentBinding::bind)

    override fun initView() = with(binding) {
        tvRate.singleClick {
            val opened = context?.let {
                ExternalActionLauncher.openRatingPage(it)
            } ?: false
            if (!opened) {
                getString(R.string.unable_open_google).showToastMessage()
            }
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
