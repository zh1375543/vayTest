package com.vaycore.finance.ui.activities

import android.annotation.SuppressLint
import android.content.Intent
import android.location.LocationManager
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.widget.doAfterTextChanged
import androidx.core.widget.doOnTextChanged
import androidx.lifecycle.lifecycleScope
import com.hjq.permissions.XXPermissions
import com.hjq.permissions.permission.PermissionLists
import com.vaycore.finance.R
import com.vaycore.finance.App
import com.vaycore.finance.base.BaseActivity
import com.vaycore.finance.data.PageLogin
import com.vaycore.finance.data.local.AGREEMENT_REGISTER
import com.vaycore.finance.data.local.PRIVACY_POLICY
import com.vaycore.finance.data.local.bean.ClickablePart
import com.vaycore.finance.data.local.bean.TrackBean
import com.vaycore.finance.databinding.LoginActivityBinding
import com.vaycore.finance.data.ACT_InputPhoneNumberEnd
import com.vaycore.finance.data.ACT_InputPhonenumberStart
import com.vaycore.finance.data.ACT_clickLoginOTP
import com.vaycore.finance.data.ACT_clickOTPLogin
import com.vaycore.finance.data.ACT_clickVerifyCode
import com.vaycore.finance.data.ACT_exit
import com.vaycore.finance.data.ACT_in
import com.vaycore.finance.data.PageExit
import com.vaycore.finance.data.PageHome
import com.vaycore.finance.data.local.agreePhonePrivacy
import com.vaycore.finance.data.local.location
import com.vaycore.finance.ui.viewmodels.DashboardViewModel
import com.vaycore.finance.util.LOGIN_VIA_OTP
import com.vaycore.finance.util.countdownTimer
import com.vaycore.finance.util.context.getColor2
import com.vaycore.finance.util.isPhoneNumberValid
import com.vaycore.finance.ui.extension.setSpannableClickableTexts
import com.vaycore.finance.util.showToastMessage
import com.vaycore.finance.ui.extension.singleClick
import com.vaycore.finance.ui.navigation.MainNavigator
import com.vaycore.finance.ui.navigation.MainDestination
import com.vaycore.finance.ui.sidepage.act.FirstSavingsPlanActivity
import com.vaycore.finance.util.trackEvent
import com.vaycore.finance.ui.viewmodels.SessionViewModel
import com.vaycore.finance.ui.showConfirmDialog
import com.vaycore.finance.util.SmsAutoFillHelper
import com.vaycore.finance.util.SPUtil
import com.vaycore.finance.util.viewBinding
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

class LoginActivity : BaseActivity<LoginActivityBinding>() {

    override val binding by viewBinding(LoginActivityBinding::inflate)
     override val adjustForImeInsets = false

    private val vm by viewModels<SessionViewModel>()
    private val homeVm by viewModels<DashboardViewModel>()

    private val locationManager by lazy { getSystemService(LOCATION_SERVICE) as LocationManager }

    private var startInputTime: Long = 0L
    private var debounceJob: Job? = null
    private val debounceTime = 500L  // treat as input finished after 500ms idle

    private var canNavigateBack = false
    private var lastBackPressTime = 0L

    private val firstSavingsPlanLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) {
        MainNavigator.launch(this@LoginActivity, clearTask = true)
    }

    private val smsHelper by lazy {
        SmsAutoFillHelper { code ->
            binding.etOtp.setText(code)
            binding.etOtp.setSelection(code.length)
        }
    }

    @SuppressLint("MissingPermission")
    override fun initView() = with(binding) {
        titleBar.showNavigation(false)
        titleBar.setNavigationAction { handleLoginBack() }
        tvOtpLogin.text=getString(
            R.string.welcome_to_app,
            getString(R.string.app_name)
        )
        smsHelper.register(this@LoginActivity)
        vm.recordEvent(
            TrackBean(
                p = PageLogin,
                act = ACT_in
            )
        )
        onBackPressedDispatcher.addCallback(
            this@LoginActivity,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() = handleLoginBack()
            },
        )
        tvOtpLogin.singleClick {
            vm.recordEvent(
                TrackBean(
                    p = PageLogin,
                    act = ACT_clickOTPLogin,
                )
            )
        }
        etPhone.doOnTextChanged { _, _, _, _ ->
            val now = System.currentTimeMillis()

            // 1. first input → record start time
            if (startInputTime == 0L) {
                startInputTime = now
                vm.recordEvent(
                    TrackBean(
                        p = PageLogin,
                        act = ACT_InputPhonenumberStart,
                        result = startInputTime.toString()
                    )
                )
            }

            // 2. typing → reset end timer
            debounceJob?.cancel()
            debounceJob = lifecycleScope.launch {
                delay(debounceTime.milliseconds)

                // 3. user stopped typing → record end time

                vm.recordEvent(
                    TrackBean(
                        p = PageLogin,
                        act = ACT_InputPhoneNumberEnd,
                        result = System.currentTimeMillis().toString()
                    )
                )
            }
        }
        etOtp.doAfterTextChanged {
            updateLoginButtonState()
        }
        etPhone.doAfterTextChanged {
            when {
                it.isNullOrBlank() -> etPhone.clearValidationState()
                it.toString().isPhoneNumberValid() -> etPhone.showValidationSuccess()
                else -> etPhone.showValidationError()
            }
            updateLoginButtonState()
        }
        tvAccept.setSpannableClickableTexts(
            String.format(
                getString(R.string.accept_policy),
                getString(R.string.privacy_agreement),
                getString(R.string.privacy_blue)
            ),
            arrayListOf(
                ClickablePart(
                    getString(R.string.privacy_agreement),
                    getColor2(R.color.color_7087F8),
                    onClick = {
                        WebViewActivity.launch(
                            this@LoginActivity,
                            getString(R.string.privacy_agreement),
                            AGREEMENT_REGISTER
                        )
                    }),
                ClickablePart(
                    getString(R.string.privacy_blue),
                    getColor2(R.color.color_7087F8),
                    onClick = {
                        WebViewActivity.launch(
                            this@LoginActivity,
                            getString(R.string.privacy_blue),
                            PRIVACY_POLICY
                        )
                    }),
            )
        )
        tvGetOtp.singleClick {
            if (!agreePhonePrivacy) {
                showConfirmDialog(
                    getString(R.string.phone_collect_title),
                    String.format(
                        getString(R.string.phone_collect_desc),
                        getString(R.string.app_name)
                    ),
                    getString(R.string.reject),
                    getString(R.string.agree)
                ) {
                    sendCode()
                }
                return@singleClick
            }
            sendCode()
        }
        tvLogin.singleClick {
            if (!etPhone.text.toString().isPhoneNumberValid()) {
                etPhone.showValidationError()
                getString(R.string.please_check_phone).showToastMessage()
                return@singleClick
            }
            if (etOtp.text.isNullOrBlank()) {
                getString(R.string.please_check_otp).showToastMessage()
                return@singleClick
            }
            trackEvent(LOGIN_VIA_OTP)
            vm.recordEvent(
                TrackBean(
                    p = PageLogin,
                    act = ACT_clickLoginOTP,
                )
            )
            vm.login(
                binding.etPhone.text.toString(),
                binding.etOtp.text.toString(),
                null
            )
        }
        if (location.first == 0.0
            && XXPermissions.isGrantedPermissions(
                this@LoginActivity,
                listOf(PermissionLists.getAccessCoarseLocationPermission())
            )
        ) {
            locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)?.let {
                location = it.longitude to it.latitude
            }
        }
        homeVm.getUnAuthData()
    }

    override fun initObserve() = with(vm) {
        super.initObserve()
        otpResult.observe(this@LoginActivity) {
            smsHelper.startListening()

            // Lock the resend action as soon as OTP delivery succeeds.
            binding.tvGetOtp.text = String.format("%ss", 59)
            binding.tvGetOtp.isEnabled = false
            lifecycleScope.countdownTimer(
                58, {}, end = {
                    binding.tvGetOtp.text = getString(R.string.retry)
                    binding.tvGetOtp.isEnabled = true
                }
            ) {
                binding.tvGetOtp.text = String.format("%ss", it.toString())
            }
        }
        loginResult.observe(this@LoginActivity) {
            it?.let {
                App.appViewModel.postRiskInfo(PageLogin) {}
                vm.postDeviceInfo()
                launchPostLoginDestination()
            }
        }
        homeVm.unAuthResult.observe(this@LoginActivity) {
            canNavigateBack = it?.showBackButton?.trim() == "1"
            binding.titleBar.showNavigation(canNavigateBack)
        }
    }

    private fun handleLoginBack() {
        if (canNavigateBack) {
            MainActivity.launch(this)
            finish()
            return
        }

        val currentTime = System.currentTimeMillis()
        if (currentTime - lastBackPressTime < EXIT_INTERVAL) {
            vm.recordEvent(
                TrackBean(
                    act = ACT_exit,
                    result = PageHome,
                    p = PageExit,
                )
            )
            finishAffinity()
        } else {
            lastBackPressTime = currentTime
            getString(R.string.again_exit).showToastMessage()
        }
    }

    private fun launchPostLoginDestination() {
        val isFirstLogin = !hasHandledFirstLogin()
        if (isFirstLogin) {
            SPUtil.newInstance().save(FirstSavingsPlanActivity.KEY_FIRST_LOGIN_HANDLED, true)
        }

        if (!isFirstLogin || MainNavigator.resolveDestination() != MainDestination.PORTAL) {
            MainNavigator.launch(this, clearTask = true)
            return
        }

        firstSavingsPlanLauncher.launch(Intent(this, FirstSavingsPlanActivity::class.java))
    }

    private fun hasHandledFirstLogin(): Boolean = SPUtil.newInstance().get(
        FirstSavingsPlanActivity.KEY_FIRST_LOGIN_HANDLED,
        false,
    )

    private fun sendCode() {
        if (!binding.etPhone.text.toString().isPhoneNumberValid()) {
            binding.etPhone.showValidationError()
            getString(R.string.please_check_phone).showToastMessage()
            return
        }
        vm.recordEvent(
            TrackBean(
                p = PageLogin,
                act = ACT_clickVerifyCode
            )
        )
        vm.sendOTP(binding.etPhone.text.toString())
    }

    private fun updateLoginButtonState() = with(binding) {
        tvLogin.isEnabled = etPhone.text.toString().isPhoneNumberValid() &&
            !etOtp.text.isNullOrBlank()
    }

    override fun onDestroy() {
        super.onDestroy()
        smsHelper.unregister()
    }

    private companion object {
        const val EXIT_INTERVAL = 2_000L
    }
}
