package com.vaycore.finance.ui.activities.auth

import androidx.activity.viewModels
import com.vaycore.finance.R
import com.vaycore.finance.base.BaseActivity
import com.vaycore.finance.databinding.CarrierConfirmOtpActivityBinding
import com.vaycore.finance.data.ACT_clickNextStep
import com.vaycore.finance.data.ACT_in
import com.vaycore.finance.data.PageVerifyCode2
import com.vaycore.finance.data.local.authConfigList
import com.vaycore.finance.data.local.bean.TrackBean
import com.vaycore.finance.data.local.loginInfo
import com.vaycore.finance.ui.viewmodels.CarrierVerifyViewModel
import com.vaycore.finance.ui.viewmodels.DashboardViewModel
import com.vaycore.finance.util.AppStackUtil
import com.vaycore.finance.util.showToastMessage
import com.vaycore.finance.ui.extension.singleClick
import com.vaycore.finance.util.viewBinding

class CarrierConfirmOtpActivity :
    BaseActivity<CarrierConfirmOtpActivityBinding>() {

    override val binding by viewBinding(CarrierConfirmOtpActivityBinding::inflate)
    override val adjustForImeInsets = false
    private val vm by viewModels<CarrierVerifyViewModel>()
    private val homeVm by viewModels<DashboardViewModel>()

    private val company by lazy { intent.getStringExtra("type") ?: "" }

    override fun initView() = with(binding) {
        vm.recordEvent(
            TrackBean(
                p = PageVerifyCode2,
                act = ACT_in
            )
        )
        titleBar.setAction(
            "${authConfigList.indexOf("TELECOM") + 1}/${authConfigList.size}"
        )
        val phone = loginInfo?.phone ?: ""
        tvPhone.text = "+$phone"
        btNext.singleClick {
            if (etOtp.text.isNullOrBlank()) {
                getString(R.string.please_enter_otp).showToastMessage()
                return@singleClick
            }
            vm.recordEvent(
                TrackBean(
                    p = PageVerifyCode2,
                    act = ACT_clickNextStep
                )
            )
            vm.submitOtp(
                phone,
                company,
                etOtp.text.toString()
            )
        }
    }

    override fun initObserve() =with(vm){
        super.initObserve()
        submitOtpResult.observe(this@CarrierConfirmOtpActivity) {
            homeVm.getUserAuthStatus()
        }
        homeVm.userAuthStatusResult.observe(this@CarrierConfirmOtpActivity) {
            finish()
            AppStackUtil.finishActivity(CarrierAuthActivity::class.java)
            AppStackUtil.finishActivity(CarrierOtpActivity::class.java)
            it?.routeToNextAuthStep(this@CarrierConfirmOtpActivity)
        }
    }
}
