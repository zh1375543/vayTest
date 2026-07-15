package com.vaycore.finance.ui.activities.auth

import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import com.vaycore.finance.R
import com.vaycore.finance.base.BaseActivity
import com.vaycore.finance.databinding.CarrierOtpActivityBinding
import com.vaycore.finance.data.ACT_clickGet
import com.vaycore.finance.data.ACT_in
import com.vaycore.finance.data.ACT_nextStep
import com.vaycore.finance.data.PageVerifyCode
import com.vaycore.finance.data.local.authConfigList
import com.vaycore.finance.data.local.bean.TrackBean
import com.vaycore.finance.data.local.loginInfo
import com.vaycore.finance.ui.viewmodels.CarrierVerifyViewModel
import com.vaycore.finance.ui.viewmodels.DashboardViewModel
import com.vaycore.finance.util.AppStackUtil
import com.vaycore.finance.util.countdownTimer
import com.vaycore.finance.util.showToastMessage
import com.vaycore.finance.ui.extension.singleClick
import com.vaycore.finance.util.start
import com.vaycore.finance.util.viewBinding

class CarrierOtpActivity :
    BaseActivity<CarrierOtpActivityBinding>() {

    override val binding by viewBinding(CarrierOtpActivityBinding::inflate)
    override val adjustForImeInsets = false
    private val vm by viewModels<CarrierVerifyViewModel>()
    private val homeVm by viewModels<DashboardViewModel>()

    private val company by lazy { intent.getStringExtra("type") ?: "" }

    override fun initView() = with(binding) {
        vm.recordEvent(
            TrackBean(
                p = PageVerifyCode,
                act = ACT_in
            )
        )
        titleBar.setAction(
            "${authConfigList.indexOf("TELECOM") + 1}/${authConfigList.size}"
        )
        val phone = loginInfo?.phone ?: ""
        tvPhone.text = "+$phone"
        tvGetOtp.singleClick {
            vm.recordEvent(
                TrackBean(
                    p = PageVerifyCode,
                    act = ACT_clickGet
                )
            )
            vm.getOtp(phone, company)
        }
        btNext.singleClick {
            if (etOtp.text.isNullOrBlank()) {
                getString(R.string.please_enter_otp).showToastMessage()
                return@singleClick
            }
            vm.recordEvent(
                TrackBean(
                    p = PageVerifyCode,
                    act = ACT_nextStep
                )
            )
            if (company == "vietnamobile") {
                vm.submitOtp(
                    phone,
                    company,
                    etOtp.text.toString()
                )
            } else {
                vm.submitWithGetOtp(
                    phone,
                    company,
                    etOtp.text.toString()
                )
            }
        }
    }

    override fun initObserve() =with(vm){
        super.initObserve()
        getOtpResult.observe(this@CarrierOtpActivity) {
            lifecycleScope.countdownTimer(
                59, {}, end = {
                    binding.tvGetOtp.text = getString(R.string.retry)
                    binding.tvGetOtp.isEnabled = true
                }
            ) {
                binding.tvGetOtp.text = "$it" + "s"
                binding.tvGetOtp.isEnabled = false
            }
        }
        submitOtpResult.observe(this@CarrierOtpActivity) {
            homeVm.getUserAuthStatus()
        }
        homeVm.userAuthStatusResult.observe(this@CarrierOtpActivity) {
            finish()
            AppStackUtil.finishActivity(CarrierAuthActivity::class.java)
            it?.routeToNextAuthStep(this@CarrierOtpActivity)
        }
        submitWithGetOtpResult.observe(this@CarrierOtpActivity) {
            start<CarrierConfirmOtpActivity> {
                putExtra("type", company)
            }
        }
    }
}
