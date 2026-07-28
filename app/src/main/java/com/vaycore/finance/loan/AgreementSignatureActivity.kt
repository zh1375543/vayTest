package com.vaycore.finance.loan

import android.content.Context
import android.view.View
import androidx.activity.viewModels
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.vaycore.finance.app.App
import com.vaycore.finance.R
import com.vaycore.finance.identity.viewmodel.SessionViewModel
import com.vaycore.finance.base.BaseActivity
import com.vaycore.finance.data.ACT_clickSubmit
import com.vaycore.finance.data.ACT_in
import com.vaycore.finance.data.PageSign
import com.vaycore.finance.data.bean.TrackBean
import com.vaycore.finance.data.loginInfo
import com.vaycore.finance.data.signBackHome
import com.vaycore.finance.databinding.ActivityContractSignBinding
import com.vaycore.finance.model.loan.ProductBean
import com.vaycore.finance.app.MainActivity
import com.vaycore.finance.ui.extension.singleClick
import com.vaycore.finance.ui.views.SignatureView
import com.vaycore.finance.util.loanevent.LoanEvent
import com.vaycore.finance.util.loanevent.LoanEventRecorder
import com.vaycore.finance.util.PermissionCoordinator
import com.vaycore.finance.util.PermissionScenario
import com.vaycore.finance.util.platform.configureSystemBars
import com.vaycore.finance.util.showToastMessage
import com.vaycore.finance.util.start
import com.vaycore.finance.util.viewBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class AgreementSignatureActivity : BaseActivity<ActivityContractSignBinding>() {

    override val binding by viewBinding(ActivityContractSignBinding::inflate)
    companion object {

        fun launch(
            context: Context,
            cardId: Long?,
            productList: ArrayList<ProductBean>?,
            productId: String?,
            bankId: Long?,
            amount: String?,
            productInstallmentMap: String?,
            termIdMap: String?,
            isBackHome: Boolean = false,
            payWay: String = "CARD",
        ) {
            context.start<AgreementSignatureActivity> {
                putExtra("isBackHome", isBackHome)
                putExtra("productList", productList)
                putExtra("bankId", bankId)
                putExtra("amount", amount)
                putExtra("productId", productId)
                putExtra("bankId", cardId)
                putExtra("productInstallmentMap", productInstallmentMap)
                putExtra("termIdMap", termIdMap)
                putExtra("payWay", payWay)
            }
        }
    }

    private val vm by viewModels<SessionViewModel>()

    private val isShowBackHome by lazy {
        intent.getBooleanExtra("isBackHome", false)
    }
    private val productList by lazy {
        intent.getParcelableArrayListExtra<ProductBean>("productList")
    }
    private val bankId by lazy { intent.getLongExtra("bankId", 0L) }
    private val amount by lazy { intent.getStringExtra("amount") }
    private val productId by lazy { intent.getStringExtra("productId") }
    private val productInstallmentMap by lazy { intent.getStringExtra("productInstallmentMap") }
    private val termIdMap by lazy { intent.getStringExtra("termIdMap") }
    private val payWay by lazy { intent.getStringExtra("payWay") ?: "CARD" }

    private var isSign = false

    override fun initView() {
        renderSignatureWorkspace()
        connectSigningCommands()
    }

    private fun renderSignatureWorkspace() = with(binding) {
        configureSystemBars(darkMode = true)
        vm.submitTrackingEvent(TrackBean(p = PageSign, act = ACT_in))
        if (isShowBackHome) {
            LoanEventRecorder.setEventFileSuffix((loginInfo?.id ?: 111).toString())
        }
        tvBack.visibility = if (isShowBackHome) View.VISIBLE else View.INVISIBLE
        tvSign.visibility = tvBack.visibility
        tvSign2.visibility = if (!isShowBackHome) View.VISIBLE else View.GONE
        signView.setOnSignatureListener(object : SignatureView.OnSignatureListener {
            override fun onStartSigning() {
                tvHint.isVisible = false
                isSign = true
            }

            override fun onCleared() {
                tvHint.isVisible = true
                isSign = false
            }

        })
    }

    private fun connectSigningCommands() = with(binding) {
        titleBar.setNavigationAction { exitSignatureFlow() }
        tvBack.singleClick {
            MainActivity.Companion.launch(this@AgreementSignatureActivity)
            exitSignatureFlow()
        }
        registerTrackedBackHandler(vm) {
            exitSignatureFlow()
        }
//            if (CacheManager.signFile.exists() && CacheManager.signFile.length() > 0) {
//                setResult(
//                    RESULT_OK, Intent()
//                        .putExtra("filePath", CacheManager.signFile.absolutePath)
//                )
//                finish()
//            }
        tvSign2.singleClick {
            tvSign.performClick()
        }
        tvSign.singleClick {
            if (!isSign) {
                getString(R.string.please_sign).showToastMessage()
                return@singleClick
            }
            vm.submitTrackingEvent(TrackBean(p = PageSign, act = ACT_clickSubmit))
            if (isShowBackHome) {
                LoanEventRecorder.record(LoanEvent.CLICK_APPLY_LOAN)
                PermissionCoordinator.request(this@AgreementSignatureActivity, PermissionScenario.DEVICE_RISK) {
                    App.Companion.appViewModel.postRiskInfo(PageSign) { isSuccess ->
                        if (isSuccess) {
                            LoanEventRecorder.record(LoanEvent.CLICK_SUBMIT_LOAN)
                            submitSignedAgreement()
                        }
                    }
                }
            } else {
                submitSignedAgreement()
            }
        }
    }

    private fun exitSignatureFlow() {
        if (isShowBackHome) {
            signBackHome = true
            MainActivity.Companion.launch(this)
        }
        finish()
    }

    private fun submitSignedAgreement() {
        lifecycleScope.launch {
            val file =
                File(App.Companion.appContext.cacheDir, "sign_${System.currentTimeMillis()}.png")
            if (withContext(Dispatchers.IO) {
                    binding.signView.saveToFile(file)
                }) {
                finish()
                ApplicationOutcomeActivity.Companion.launch(
                    this@AgreementSignatureActivity,
                    productList,
                    productId,
                    bankId,
                    file.absolutePath,
                    amount,
                    productInstallmentMap,
                    termIdMap,
                    payWay
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (isShowBackHome) {
            LoanEventRecorder.initializeBaseServerTime(System.currentTimeMillis())
            LoanEventRecorder.record(LoanEvent.VIEW_ENTER_LOAN)
        }
    }

    override fun onStop() {
        super.onStop()
        if (isShowBackHome) {
            LoanEventRecorder.record(LoanEvent.VIEW_QUIT_LOAN)
            LoanEventRecorder.flush()
        }
    }
}
