package com.vaycore.finance.ui.activities

import android.annotation.SuppressLint
import android.content.Context
import android.location.LocationManager
import android.os.Handler
import android.os.Looper
import android.os.Message
import androidx.activity.viewModels
import androidx.core.view.isVisible
import com.vaycore.finance.BuildConfig
import com.vaycore.finance.R
import com.vaycore.finance.App
import com.vaycore.finance.data.PageHome
import com.vaycore.finance.ui.adapters.LoanResultAdapter
import com.vaycore.finance.base.BaseActivity
import com.vaycore.finance.data.local.APPCODE
import com.vaycore.finance.data.local.bean.LoanDashboardResponse
import com.vaycore.finance.data.local.bean.ProductBean
import com.vaycore.finance.databinding.ActivityLoanApplyResultBinding
import com.vaycore.finance.data.PageProductDetail
import com.vaycore.finance.data.local.location
import com.vaycore.finance.data.local.loginInfo
import com.vaycore.finance.data.local.signBackHome
import com.vaycore.finance.ui.adapters.HomeProductAdapter
import com.vaycore.finance.ui.showCreditUnderReviewDialog
import com.vaycore.finance.ui.showPreCreditExpiredDialog
import com.vaycore.finance.ui.viewmodels.DashboardViewModel
import com.vaycore.finance.util.AppStackUtil
import com.vaycore.finance.util.runtime.DeviceHelper
import com.vaycore.finance.ui.viewmodels.LoanProductViewModel
import com.vaycore.finance.util.LoanEventUtil
import com.vaycore.finance.util.LogUtil
import com.vaycore.finance.util.LOAN_GET_NOW_CLICK
import com.vaycore.finance.util.generateRequestBody
import com.vaycore.finance.util.getLocalIpAddress
import com.vaycore.finance.util.isPositive
import com.vaycore.finance.util.context.openExternalBrowser
import com.vaycore.finance.util.context.openPlayStore
import com.vaycore.finance.util.requestRuntimePermissions
import com.vaycore.finance.ui.extension.singleClick
import com.vaycore.finance.util.parseJson
import com.vaycore.finance.ui.viewmodels.LoanApplyViewModel
import com.vaycore.finance.util.deviceRiskPermissions
import com.vaycore.finance.util.start
import com.vaycore.finance.util.trackEvent
import com.vaycore.finance.util.viewBinding
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.MultipartBody.Part
import okhttp3.RequestBody
import java.io.File
import kotlin.toString

class LoanApplyResultActivity :
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
        ) {
            context.start<LoanApplyResultActivity> {
                putExtra("productList", productList)
                putExtra("bankId", bankId)
                putExtra("signPath", signPath)
                putExtra("amount", amount)
                putExtra("productId", productId)
                putExtra("productInstallmentMap", productInstallmentMap)
                putExtra("termIdMap", termIdMap)
            }
        }
    }

    private val vm by viewModels<LoanApplyViewModel>()
    private val dashboardVm by viewModels<DashboardViewModel>()
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
        onBackAction(vm) {
            handleBack()
        }
        titleBar.setNavigationAction { handleBack() }
        tvWithdrawal.singleClick {
            start<LoanProductMultiActivity>()
        }
        rvProduct.adapter = resultAdapter
        rvCashableProduct.adapter = homeAdapter
        loadingLayout.showLoading()
        initRisk()
        if (location.first == 0.0) {
            requestRuntimePermissions(deviceRiskPermissions) {
                val locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
                locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)?.let {
                    location = it.latitude to it.longitude
                }
            }
        }
    }

    private fun handleBack() {
        AppStackUtil.finishActivity(LoanProductMultiActivity::class.java)
        AppStackUtil.finishActivity(ContractSignActivity::class.java)
        AppStackUtil.finishActivity(LoanProductActivity::class.java)
        finish()
        MainActivity.launch(this)
    }

    private fun handleRecommendedProductClick(item: ProductBean) {
        trackEvent(LOAN_GET_NOW_CLICK)
        if (!item.canApply) return
        if (item.creditStatus == 2) {
            showPreCreditExpiredDialog(item.enableLoanStr ?: "")
            return
        }
        if (item.creditStatus == 0) {
            showCreditUnderReviewDialog()
            return
        }
        when (item.jumpType) {
            1 -> item.downloadUrl?.openExternalBrowser()
            2 -> item.downloadUrl?.openPlayStore()
            else -> {
                productVm.getProductDetail(
                    PageHome,
                    item.productId.toString(),
                    item.maxLoanAmount.toString(),
                    true
                ) {}
            }
        }
    }

    private fun refreshRecommendedProducts() {
        binding.apply {
            cashableProductLayout.isVisible = false
            tvWithdrawal.isVisible = false
        }
        homeAdapter.submitItems(emptyList())
        dashboardVm.getAuthData(isLoading = true)
    }

    private fun updateRecommendedProducts(data: LoanDashboardResponse?) {
        val isCert = data?.userCreditStatus == 0 || data?.userCreditStatus == 2
        val products = data?.showProducts.orEmpty().onEach { product ->
            product.canApply = if (isCert) {
                false
            } else {
                product.isNormalProduct() || product.isAddInfoProduct()
            }
        }
        homeAdapter.submitItems(products)
        val hasCashableProducts =
            data?.userCreditAmount.isPositive() &&
                products.isNotEmpty()
        binding.apply {
            cashableProductLayout.isVisible = hasCashableProducts
            tvWithdrawal.isVisible = hasCashableProducts
        }
    }

    private fun handleProductDetail(data: ProductBean?) {
        data ?: return
        start<LoanProductActivity> {
            putExtra("product", data)
        }
    }


    private fun startLoan(eventFile: File?) = with(binding) {
//        LogUtil.e("signature image provided: $signPath")
        val builder: MultipartBody.Builder = MultipartBody.Builder().setType(MultipartBody.FORM)
        if (signPath != null) {
//                val signPic = File(cacheDir, "test.jpeg")
            val signPic = File(signPath!!)
            if (signPic.exists()) {
                val imgFileRQ = RequestBody.create("image/*".toMediaTypeOrNull(), signPic)
                val imgPart = Part.createFormData("signPic", signPic.name, imgFileRQ)
                builder.addPart(imgPart)
//                    LogUtil.e("signature image provided")
            }
        }
        if (eventFile?.exists() == true) {
            val fileRQ = RequestBody.create("text/plain".toMediaTypeOrNull(), eventFile)
            val part = Part.createFormData("eventFile", eventFile.name, fileRQ)
            builder.addPart(part)
        }
        val parts: List<Part> = builder.build().parts

        val map = HashMap<String, String>()
        map["mobileType"] = "2"
        map["appCode"] = APPCODE
        map["version"] = BuildConfig.VERSION_NAME
        map["bankInfoId"] = bankId.toString()
        map["userId"] = loginInfo?.id.toString()
        map["payWay"] = "CARD"
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
        App.appViewModel.hasDeviceInfo(PageProductDetail) {
            if (it) {
                getEventFile { file ->
                    startLoan(file)
                }
                return@hasDeviceInfo
            }
            App.appViewModel.postRiskInfo(
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
            if (msg.what == LoanEventUtil.MSG_LOG_FILE_PREPARED) {
                action.invoke(msg.obj as File?)
            }
            true
        }
        LoanEventUtil.instance.preparedUploadLogFile(mEventLogHandler)
    }

    private fun loanSuccess() {
        binding.apply {
            loadingLayout.showContent()
            successLayout.isVisible = true
            failLayout.isVisible = false
        }
        signBackHome = false
    refreshRecommendedProducts()
    }

    private fun loanFailed() {
        binding.apply {
            loadingLayout.showContent()
            successLayout.isVisible = false
            failLayout.isVisible = true
        }
        signBackHome = false
       refreshRecommendedProducts()
    }

    override fun initObserve() {
        super.initObserve()
        vm.loanResult.observe(this@LoanApplyResultActivity) {
            binding.rvProduct.isVisible = false
            loanSuccess()
        }
        vm.loanFailResult.observe(this@LoanApplyResultActivity) {
            loanFailed()
        }
        vm.togetherLoanResult.observe(this@LoanApplyResultActivity) {
            resultAdapter.submitItems(it?.onEach { it1 ->
                it1.currency = productList?.get(0)?.currency
                it1.currencySymbol = productList?.get(0)?.currencySymbol
            })
            binding.rvProduct.isVisible = !it.isNullOrEmpty()
            loanSuccess()
        }
        dashboardVm.authResult.observe(this@LoanApplyResultActivity) {
            updateRecommendedProducts(it)
        }
        dashboardVm.authFailedResult.observe(this@LoanApplyResultActivity) {
            binding.apply {
                cashableProductLayout.isVisible = false
                tvWithdrawal.isVisible = false
            }
        }
        productVm.detailResult.observe(this@LoanApplyResultActivity) {
            handleProductDetail(it)
        }
    }
}
