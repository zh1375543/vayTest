package com.vaycore.finance.ui.activities.mine

import androidx.activity.viewModels
import com.vaycore.finance.base.BaseActivity
import com.vaycore.finance.databinding.LogoutActivityBinding
import com.vaycore.finance.data.local.loginInfo
import com.vaycore.finance.ui.viewmodels.SessionViewModel
import com.vaycore.finance.ui.extension.singleClick
import com.vaycore.finance.util.start
import com.vaycore.finance.util.viewBinding

class LogoutActivity : BaseActivity<LogoutActivityBinding>() {

    override val binding by viewBinding(LogoutActivityBinding::inflate)
    private val vm by viewModels<SessionViewModel>()

    private val selectTextList by lazy {
        arrayListOf(
            binding.tvReason1,
            binding.tvReason2,
            binding.tvReason3,
            binding.tvReason4,
        )
    }

    override fun initView() = with(binding) {
        tvAccount.text = loginInfo?.phone
        btnSubmit.isEnabled = false
        selectTextList.forEach {
            it.setOnClickListener { it1 ->
                it1.isSelected = !it.isSelected
                btnSubmit.isEnabled = selectTextList.any { it1 -> it1.isSelected }
            }
        }
        btnSubmit.singleClick {
            vm.logout()
        }
    }

    override fun initObserve() =with(vm){
        super.initObserve()
        logoutResult.observe(this@LogoutActivity) {
            start<LogoutSuccessActivity>()
            finish()
        }
    }
}
