package com.vaycore.finance.loan

import android.content.Context
import android.view.View
import androidx.activity.viewModels
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.vaycore.finance.app.App
import com.vaycore.finance.R
import com.vaycore.finance.identity.SessionViewModel
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
import com.vaycore.finance.util.LoanEventUtil
import com.vaycore.finance.util.deviceRiskPermissions
import com.vaycore.finance.util.requestRuntimePermissions
import com.vaycore.finance.util.setSystemBar
import com.vaycore.finance.util.showToastMessage
import com.vaycore.finance.util.start
import com.vaycore.finance.util.viewBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class ContractSignActivity : BaseActivity<ActivityContractSignBinding>() {

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
            context.start<ContractSignActivity> {
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

    private val loanEvent by lazy { LoanEventUtil.Companion.instance }

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
        setSystemBar(darkMode = true)
        vm.recordEvent(TrackBean(p = PageSign, act = ACT_in))
        if (isShowBackHome) {
            loanEvent.initEventFileUniqueSuffix((loginInfo?.id ?: 111).toString())
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
        titleBar.setNavigationAction { handleBack() }
        tvBack.singleClick {
            MainActivity.Companion.launch(this@ContractSignActivity)
            handleBack()
        }
        onBackAction(vm) {
            handleBack()
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
            vm.recordEvent(TrackBean(p = PageSign, act = ACT_clickSubmit))
            if (isShowBackHome) {
                loanEvent.logClickApplyLoan()
                requestRuntimePermissions(deviceRiskPermissions) {
                    App.Companion.appViewModel.postRiskInfo(PageSign) { isSuccess ->
                        if (isSuccess) {
                            loanEvent.logClickSubmitLoan()
                            sign()
                        }
                    }
                }
            } else {
                sign()
            }
        }
    }

    private fun handleBack() {
        if (isShowBackHome) {
            signBackHome = true
            MainActivity.Companion.launch(this)
        }
        finish()
    }

    private fun sign() {
        lifecycleScope.launch {
            val file =
                File(App.Companion.appContext.cacheDir, "sign_${System.currentTimeMillis()}.png")
            if (withContext(Dispatchers.IO) {
                    binding.signView.saveToFile(file)
                }) {
                finish()
                LoanApplyResultActivity.Companion.launch(
                    this@ContractSignActivity,
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
            loanEvent.initBaseServerTime(System.currentTimeMillis())
            loanEvent.logViewEnterLoan()
        }
    }

    override fun onStop() {
        super.onStop()
        if (isShowBackHome) {
            loanEvent.logViewQuitLoan()
            loanEvent.writeLog2File()
        }
    }
}
