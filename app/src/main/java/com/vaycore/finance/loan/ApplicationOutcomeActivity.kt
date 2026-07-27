package com.vaycore.finance.loan

import android.annotation.SuppressLint
import android.content.Context
import android.location.LocationManager
import android.os.Handler
import android.os.Looper
import android.os.Message
import androidx.activity.viewModels
import androidx.core.view.isVisible
import com.vaycore.finance.app.App
import com.vaycore.finance.BuildConfig
import com.vaycore.finance.R
import com.vaycore.finance.base.BaseActivity
import com.vaycore.finance.data.PageHome
import com.vaycore.finance.data.PageProductDetail
import com.vaycore.finance.data.APPCODE
import com.vaycore.finance.data.location
import com.vaycore.finance.data.loginInfo
import com.vaycore.finance.data.signBackHome
import com.vaycore.finance.databinding.ActivityLoanApplyResultBinding
import com.vaycore.finance.model.loan.LoanDashboardResponse
import com.vaycore.finance.model.loan.ProductBean
import com.vaycore.finance.app.MainActivity
import com.vaycore.finance.home.HomeProductAdapter
import com.vaycore.finance.loan.adapter.LoanResultAdapter
import com.vaycore.finance.loan.viewmodel.LoanApplyViewModel
import com.vaycore.finance.loan.viewmodel.LoanProductViewModel
import com.vaycore.finance.ui.extension.singleClick
import com.vaycore.finance.util.AppStackUtil
import com.vaycore.finance.util.LOAN_GET_NOW_CLICK
import com.vaycore.finance.util.LoanEventUtil
import com.vaycore.finance.util.LogUtil
import com.vaycore.finance.util.PermissionCoordinator
import com.vaycore.finance.util.PermissionScenario
import com.vaycore.finance.util.generateRequestBody
import com.vaycore.finance.util.getLocalIpAddress
import com.vaycore.finance.util.isPositive
import com.vaycore.finance.util.parseJson
import com.vaycore.finance.util.runtime.DeviceHelper
import com.vaycore.finance.util.start
import com.vaycore.finance.util.trackEvent
import com.vaycore.finance.util.viewBinding
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import java.io.File

class ApplicationOutcomeActivity :
    BaseActivity<ActivityLoanApplyResultBinding>() {

    override val binding by viewBinding(ActivityLoanApplyResultBinding::inflate)
    companion object {
        fun launch(
            context: Context,
            productList: ArrayList<ProductBean>?,
            productId: String?,
            bankId: Long?,
            signPath: String?,
            amount: String?,
            productInstallmentMap: String? = null,
            termIdMap: String? = null,
            payWay: String = "CARD",
        ) {
            context.start<ApplicationOutcomeActivity> {
                putExtra("productList", productList)
                putExtra("bankId", bankId)
                putExtra("signPath", signPath)
                putExtra("amount", amount)
                putExtra("productId", productId)
                putExtra("productInstallmentMap", productInstallmentMap)
                putExtra("termIdMap", termIdMap)
                putExtra("payWay", payWay)
            }
        }
    }

    private val vm by viewModels<LoanApplyViewModel>()
    private val productVm by viewModels<LoanProductViewModel>()

    private val productList by lazy {
        intent.getParcelableArrayListExtra<ProductBean>("productList")
    }
    private val termIdMap by lazy { intent.getStringExtra("termIdMap") }
    private val bankId by lazy { intent.getLongExtra("bankId", 0L) }
    private val signPath by lazy { intent.getStringExtra("signPath") }
    private val amount by lazy { intent.getStringExtra("amount") }
    private val productId by lazy { intent.getStringExtra("productId") }
    private val productInstallmentMap by lazy { intent.getStringExtra("productInstallmentMap") }
    private val payWay by lazy { intent.getStringExtra("payWay") ?: "CARD" }
    private val resultAdapter by lazy {
        LoanResultAdapter()
    }
    private val homeAdapter by lazy {
        HomeProductAdapter().apply {
            setOnChildClickListener { view, _, position ->
                if (view.id == R.id.tvApply) {
                    items.getOrNull(position)?.let { item ->
                        handleRecommendedProductClick(item)
                    }
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    override fun initView() = with(binding) {
        registerTrackedBackHandler(vm) {
            returnToDashboard()
        }
        titleBar.setNavigationAction { returnToDashboard() }
        tvWithdrawal.singleClick {
            start<CombinedLoanOfferActivity>()
        }
        rvProduct.adapter = resultAdapter
        rvCashableProduct.adapter = homeAdapter
        loadingLayout.showLoading()
        initRisk()
        if (location.first == 0.0) {
            PermissionCoordinator.request(this@ApplicationOutcomeActivity, PermissionScenario.DEVICE_RISK) {
                val locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
                locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)?.let {
                    location = it.longitude to it.latitude
                }
            }
        }
    }

    private fun returnToDashboard() {
        AppStackUtil.finishActivity(CombinedLoanOfferActivity::class.java)
        AppStackUtil.finishActivity(AgreementSignatureActivity::class.java)
        AppStackUtil.finishActivity(LoanOfferActivity::class.java)
        finish()
        MainActivity.Companion.launch(this)
    }

    private fun handleRecommendedProductClick(item: ProductBean) {
        trackEvent(LOAN_GET_NOW_CLICK)
        productVm.getProductDetail(
            PageHome,
            item.productId.toString(),
            item.maxLoanAmount.toString(),
            true,
        ) {}
    }

    private fun refreshRecommendedProducts() {
        binding.apply {
            cashableProductLayout.isVisible = false
            tvWithdrawal.isVisible = false
            updateResultsCardVisibility()
        }
        homeAdapter.submitItems(emptyList())
        vm.getTogetherLoan(showLoading = true) {
            collapseOfferRecommendations()
        }
    }

    private fun updateRecommendedProducts(data: LoanDashboardResponse?) {
        val products = data?.showProducts.orEmpty().onEach { product ->
            product.canApply = true
            product.isTogether = true
            if (product.currency == null) product.currency = data?.currency
            if (product.currencySymbol == null) product.currencySymbol = data?.currencySymbol
        }
        homeAdapter.submitItems(products)
        val hasCashableProducts =
            data?.canApplyAmount.isPositive() &&
                products.isNotEmpty()
        binding.apply {
            cashableProductLayout.isVisible = hasCashableProducts
            tvWithdrawal.isVisible = hasCashableProducts
            updateResultsCardVisibility()
        }
    }

    private fun collapseOfferRecommendations() = with(binding) {
        cashableProductLayout.isVisible = false
        tvWithdrawal.isVisible = false
        updateResultsCardVisibility()
    }

    private fun updateResultsCardVisibility() = with(binding) {
        resultsCard.isVisible = rvProduct.isVisible || cashableProductLayout.isVisible
    }

    private fun handleProductDetail(data: ProductBean?) {
        data ?: return
        AppStackUtil.finishActivity(LoanOfferActivity::class.java)
        start<LoanOfferActivity> {
            putExtra("product", data)
        }
    }


    private fun startLoan(eventFile: File?) = with(binding) {
//        LogUtil.e("signature image provided: $signPath")
        val builder: MultipartBody.Builder = MultipartBody.Builder().setType(MultipartBody.Companion.FORM)
        if (signPath != null) {
//                val signPic = File(cacheDir, "test.jpeg")
            val signPic = File(signPath!!)
            if (signPic.exists()) {
                val imgFileRQ = RequestBody.Companion.create("image/*".toMediaTypeOrNull(), signPic)
                val imgPart = MultipartBody.Part.Companion.createFormData("signPic", signPic.name, imgFileRQ)
                builder.addPart(imgPart)
//                    LogUtil.e("signature image provided")
            }
        }
        if (eventFile?.exists() == true) {
            val fileRQ = RequestBody.Companion.create("text/plain".toMediaTypeOrNull(), eventFile)
            val part = MultipartBody.Part.Companion.createFormData("eventFile", eventFile.name, fileRQ)
            builder.addPart(part)
        }
        val parts: List<MultipartBody.Part> = builder.build().parts

        val map = HashMap<String, String>()
        map["mobileType"] = "2"
        map["appCode"] = APPCODE
        map["version"] = BuildConfig.VERSION_NAME
        map["userId"] = loginInfo?.id.toString()
        map["payWay"] = payWay
        if (payWay == "CARD") {
            map["bankInfoId"] = bankId.toString()
        } else {
            map["userCashWalletId"] = bankId.toString()
        }
        map["ip"] = getLocalIpAddress() ?: ""
        map["imei"] = DeviceHelper.getDeviceId()
        map["coordinate"] =
            "${location.first},${location.second}"
        map["auditKey"] = "auditKey"
        if (productList != null) {
            if (productInstallmentMap != null) {
                map["productInstallmentMap"] = productInstallmentMap!!
            }
            if (termIdMap != null) {
                map["productLoanTermIdMap"] = termIdMap!!
            }
            LogUtil.e("productLoanTermIdMap:$termIdMap")
            map["productIds"] =
                productList!!.joinToString(",") { it1 -> it1.productId.toString() }
            val mBody = map.generateRequestBody()
            vm.togetherLoan(parts, mBody)
        } else {
            LogUtil.e("termId:$termIdMap")
            if (productInstallmentMap != null) {
                try {
                    val obj = productInstallmentMap.parseJson<Map<Long?, Double?>>()
                    val planNums = obj?.values?.firstOrNull()?.toInt()
                    LogUtil.e("planNums:$planNums")
                    if (planNums != null) {
                        map["planNums"] = planNums.toString()
                    }
                } catch (e: Exception) {
                    LogUtil.e("planNumsEx:${e.message}")
                }
            }
            if (termIdMap != null) {
                try {
                    val obj = termIdMap.parseJson<Map<Long?, Double?>>()
                    val termId = obj?.values?.firstOrNull()?.toLong()
                    LogUtil.e("termId:$termId")
                    if (termId != null) {
                        map["loanTermId"] = termId.toString()
                    }
                } catch (e: Exception) {
                    LogUtil.e("termEx:${e.message}")
                }
            }
            map["productId"] = productId.toString()
            map["amount"] = amount.toString()
            val mBody = map.generateRequestBody()
            vm.loan(parts, mBody)
        }
    }

    private fun initRisk() {
        App.Companion.appViewModel.hasDeviceInfo(PageProductDetail) {
            if (it) {
                getEventFile { file ->
                    startLoan(file)
                }
                return@hasDeviceInfo
            }
            App.Companion.appViewModel.postRiskInfo(
                PageProductDetail
            ) { isSuccess ->
                if (isSuccess) {
                    getEventFile { file ->
                        startLoan(file)
                    }
                } else {
                    loanFailed()
                }
            }
        }
    }

    private fun getEventFile(action: (File?) -> Unit) {
        val mEventLogHandler = Handler(Looper.getMainLooper()) { msg: Message ->
            if (msg.what == LoanEventUtil.Companion.MSG_LOG_FILE_PREPARED) {
                action.invoke(msg.obj as File?)
            }
            true
        }
        LoanEventUtil.Companion.instance.preparedUploadLogFile(mEventLogHandler)
    }

    private fun loanSuccess() {
        binding.apply {
            loadingLayout.showContent()
            successLayout.isVisible = true
            failLayout.isVisible = false
            ivSuccess.isVisible = true
            ivFail.isVisible = false
            updateResultsCardVisibility()
        }
        signBackHome = false
        refreshRecommendedProducts()
    }

    private fun loanFailed() {
        binding.apply {
            loadingLayout.showContent()
            successLayout.isVisible = false
            failLayout.isVisible = true
            ivSuccess.isVisible = false
            ivFail.isVisible = true
            updateResultsCardVisibility()
        }
        signBackHome = false
        refreshRecommendedProducts()
    }

    override fun initObserve() {
        super.initObserve()
        vm.loanResult.observe(this@ApplicationOutcomeActivity) {
            binding.rvProduct.isVisible = false
            loanSuccess()
        }
        vm.loanFailResult.observe(this@ApplicationOutcomeActivity) {
            loanFailed()
        }
        vm.togetherLoanResult.observe(this@ApplicationOutcomeActivity) {
            resultAdapter.submitItems(it?.onEach { it1 ->
                it1.currency = productList?.get(0)?.currency
                it1.currencySymbol = productList?.get(0)?.currencySymbol
            })
            binding.rvProduct.isVisible = !it.isNullOrEmpty()
            loanSuccess()
        }
        vm.togetherInfo.observe(this@ApplicationOutcomeActivity) {
            updateRecommendedProducts(it)
        }
        productVm.detailResult.observe(this@ApplicationOutcomeActivity) {
            handleProductDetail(it)
        }
    }
}
