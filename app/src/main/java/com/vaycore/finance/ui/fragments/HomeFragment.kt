package com.vaycore.finance.ui.fragments

import android.app.Dialog
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.vaycore.finance.R
import com.vaycore.finance.ui.activities.BankCardListActivity
import com.vaycore.finance.ui.activities.MainActivity
import com.vaycore.finance.ui.activities.LoanProductActivity
import com.vaycore.finance.ui.activities.ContractSignActivity
import com.vaycore.finance.ui.activities.LoanProductMultiActivity
import com.vaycore.finance.ui.activities.auth.routeToNextAuthStep
import com.vaycore.finance.ui.adapters.HomeProductAdapter
import com.vaycore.finance.base.BaseFragment
import com.vaycore.finance.data.PageHome
import com.vaycore.finance.data.local.bean.GuestHomeResponse
import com.vaycore.finance.data.local.bean.LoanDashboardResponse
import com.vaycore.finance.data.local.bean.ProductBean
import com.vaycore.finance.data.local.bean.TrackBean
import com.vaycore.finance.data.local.bean.UserAuthStatusResponse
import com.vaycore.finance.data.local.isLogin
import com.vaycore.finance.databinding.HomeFragmentBinding
import com.vaycore.finance.data.ACT_approvalDenied
import com.vaycore.finance.data.ACT_approvalInProgress
import com.vaycore.finance.data.ACT_clickClose
import com.vaycore.finance.data.ACT_clickImmediate
import com.vaycore.finance.data.ACT_userAppBankMyCard
import com.vaycore.finance.data.PageHomePre
import com.vaycore.finance.data.PageHomeRefuse
import com.vaycore.finance.data.local.signBackHome
import com.vaycore.finance.util.LOAN_GET_NOW_CLICK
import com.vaycore.finance.ui.extension.animateAmount
import com.vaycore.finance.util.countdownTimer
import com.vaycore.finance.util.formatDays
import com.vaycore.finance.util.context.getColor2
import com.vaycore.finance.util.context.openExternalBrowser
import com.vaycore.finance.util.context.openPlayStore
import com.vaycore.finance.util.ifLoginAction
import com.vaycore.finance.util.isPositive
import com.vaycore.finance.ui.extension.resetScale
import com.vaycore.finance.ui.extension.setClickableTextWithScale
import com.vaycore.finance.ui.extension.singleClick
import com.vaycore.finance.util.toJsonString
import com.vaycore.finance.util.trackEvent
import com.vaycore.finance.ui.showCreditUnderReviewDialog
import com.vaycore.finance.ui.createNewProductDialog
import com.vaycore.finance.ui.showPreCreditExpiredDialog
import com.vaycore.finance.ui.viewmodels.DashboardViewModel
import com.vaycore.finance.ui.viewmodels.DashboardViewModel.Companion.isFirstEnter
import com.vaycore.finance.ui.viewmodels.LoanProductViewModel
import com.vaycore.finance.ui.showAppRatingDialog
import com.vaycore.finance.util.showToastMessage
import com.vaycore.finance.util.start
import com.vaycore.finance.ui.extension.stopScaleAnimation
import kotlinx.coroutines.Job
import kotlin.toString
import com.vaycore.finance.ui.createAvailableCreditDialog
import com.vaycore.finance.util.formatAmountWithPrefix
import com.vaycore.finance.util.viewBinding

class HomeFragment : BaseFragment<HomeFragmentBinding>(R.layout.home_fragment) {

    override val binding by viewBinding(HomeFragmentBinding::bind)

    private val vm by viewModels<DashboardViewModel>()
    private val productVm by viewModels<LoanProductViewModel>()

    private val homeAdapter by lazy {
        HomeProductAdapter().apply {
            setOnChildClickListener { view, _, position ->
                when (view.id) {
                    R.id.tvApply -> {
                        trackEvent(LOAN_GET_NOW_CLICK)
                        val item = items[position]
                        if (!item.canApply) return@setOnChildClickListener
                        if (item.creditStatus == 2) {
                            context.showPreCreditExpiredDialog(item.enableLoanStr ?: "")
                            return@setOnChildClickListener
                        }
                        if (item.creditStatus == 0) {
                            context.showCreditUnderReviewDialog()
                            return@setOnChildClickListener
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
                }
            }
        }
    }

    private var newProductDialog: Dialog? = null
    private var creditDialog: Dialog? = null
    private var hasShownCreditDialog = false

    override fun initView() = with(binding) {
        contentLayout.apply {
            rvProduct.adapter = homeAdapter
            tvModifyCard.singleClick {
                vm.recordEvent(
                    TrackBean(
                        p = PageHome,
                        act = ACT_userAppBankMyCard
                    )
                )
                it.context.start<BankCardListActivity>()
            }
            tvRefresh.singleClick {
                refreshData()
            }
            tvBorrowNow.singleClick {
                it.context.ifLoginAction {
                    isGoAuth = true
                    vm.getUserAuthStatus()
                }

            }
            tvLoan.singleClick {
                it.context.ifLoginAction {
                    context?.start<LoanProductMultiActivity>()
                }
            }
            ivCloseBank.singleClick {
                bankErrorLayout.isVisible = false
            }
        }
        loadingLayout.setOnRetryClickListener {
            refreshData()
        }
        swipeRefreshLayout.setOnRefreshListener {
            refreshData()
        }
    }

    private var isGoAuth = false
    override fun onResume() {
        super.onResume()
        // ViewPager keeps HomeFragment alive, so allow the credit dialog once per Home entry.
        hasShownCreditDialog = false
        refreshData()
    }

    fun refreshData() = with(binding) {
        loadingLayout.showLoading()
        contentLayout.apply {
            topLayout.isVisible = true
            contentLayout.isVisible = true
        }
        calmLayout.calmLayout.isVisible = false
        if (isLogin) {
            isGoAuth = false
            vm.getUserAuthStatus()
        } else {
            vm.getUnAuthData()
        }
        vm.getBannerList()
    }

    private var timeJob: Job? = null
    private var loanDateStr: String? = null

    override fun initObserve() {
        vm.authFailedResult.observe(this@HomeFragment) {
            binding.swipeRefreshLayout.isRefreshing = false
            binding.loadingLayout.showError()
        }
        vm.userAuthStatusResult.observe(this@HomeFragment) {
            handleUserAuthStatus(it)
        }
        vm.unAuthResult.observe(this@HomeFragment) {
            it?.let { handleUnAuthData(it) }
        }
        vm.authResult.observe(this@HomeFragment) {
            it?.let { handleAuthData(it) }
        }
        vm.bannerResult.observe(this@HomeFragment) {
            binding.contentLayout.bannerView.setData(it ?: emptyList())
            binding.contentLayout.bannerView.isVisible = !it.isNullOrEmpty()
        }
        productVm.detailResult.observe(this@HomeFragment) {
            handleProductDetail(it)
        }
    }

    private fun handleUserAuthStatus(data: UserAuthStatusResponse?) {
        if (isGoAuth) {
            data?.routeToNextAuthStep(binding.root.context, false)
            return
        }
        isGoAuth = false
        vm.fetchAuthConfigList { configList ->
            if (data?.isPass(configList) == true) {
                vm.getAuthData()
            } else {
                vm.getUnAuthData()
            }
        }
    }

    private fun handleUnAuthData(data: GuestHomeResponse) = with(binding) {
        loadingLayout.showContent()
        swipeRefreshLayout.isRefreshing = false
        contentLayout.apply {
            tvAmount.animateAmount(data.maxAmount, prefix = data.currencySymbol ?: "")
            tvLoanAmount.text = getString(R.string.l_amount)
            tvPercent.text = data.annualizedInterestRate
            tvPeriod.text = root.context.formatDays(data.loanTerm)
            tvRateLabel.text = data.recommendText
            tvRateLabel.isVisible = !data.recommendText.isNullOrEmpty()
            authLayout.isVisible = false
            unAuthLayout.isVisible = true
            questionLayout.isVisible = true
            productLayout.isVisible = false
            emptyProduct.isVisible = false
            bankErrorLayout.isVisible = false
            calmLayout.calmLayout.isVisible = false
            preLayout.isVisible = false
            refuseLayout.isVisible = false
            tvQuick.isVisible = true
            marqueeView.setTexts(isWhiteColor = false)
        }
    }

    private fun handleAuthData(data: LoanDashboardResponse) = with(binding) {
        loadingLayout.showContent()
        swipeRefreshLayout.isRefreshing = false
        contentLayout.apply {
            tvAmount2.animateAmount(
                data.userCreditAmount,
                prefix = data.userCreditCurrencySymbol ?: ""
            )
            tvMaxAmount.text = data.totalCreditAmount.formatAmountWithPrefix(data.userCreditCurrencySymbol)
            tvUsedAmount.text = data.usedAmount.formatAmountWithPrefix(data.userCreditCurrencySymbol)
            tvLoanRateLabel.text = data.recommendText
            tvLoanRateLabel.isVisible = !data.recommendText.isNullOrEmpty()
            unAuthLayout.isVisible = false
            authLayout.isVisible = true
            tvQuick.isVisible = true
            questionLayout.isVisible = false
            productLayout.isVisible = true
            topLayout.isVisible = data.userCreditStatus == 1
            marqueeView.setTexts()
            contentLayout.isVisible = true
            loanDateStr = data.enableLoanStr
            tvLoan.isEnabled = data.togetherLoanSign == 1 && data.userCreditAmount.isPositive()
            if (tvLoan.isEnabled) tvLoan.resetScale() else tvLoan.stopScaleAnimation()

            if (data.repayProducts?.any { it.isPendingRepayment() || it.isRepaymentProcessing() } == true) {
                activity?.showAppRatingDialog { content ->
                    vm.submitFeed(content) {
                        getString(R.string.feedback_success).showToastMessage()
                    }
                }
            }
            val navigateToOrder = !data.repayProducts.isNullOrEmpty() && isFirstEnter
            if (navigateToOrder) {
                isFirstEnter = false
                (activity as MainActivity?)?.selectPage(1)
            }

            bankErrorLayout.isVisible = data.bankErrorFlag == true
            val isCert = data.userCreditStatus == 0 || data.userCreditStatus == 2
            val productList = ArrayList<ProductBean>()
            data.showProducts?.let { list ->
                productList.addAll(list.onEach {
                    it.canApply = true
                })
            }
            data.canNotApplyProducts?.let { list ->
                productList.addAll(list.onEach {
                    it.canApply = false
                })
            }

            timeJob?.cancel()
            if (data.userCreditStatus == 0) {
                startCountdown(60)
            }

            homeAdapter.submitItems(productList.onEach { p ->
                if (isCert) {
                    p.canApply = false
                } else if (p.canApply) {
                    p.canApply = p.isNormalProduct() || p.isAddInfoProduct()
                }
            })

            productLayout.isVisible = productList.isNotEmpty()
            emptyProduct.isVisible = productList.isEmpty()
            preLayout.isVisible = data.userCreditStatus == 0
            refuseLayout.isVisible = data.userCreditStatus == 2

            if (data.userCreditStatus == 0) {
                vm.recordEvent(
                    TrackBean(
                        p = PageHomePre,
                        act = ACT_approvalInProgress,
                        result = System.currentTimeMillis().toString()
                    )
                )
            } else if (data.userCreditStatus == 2) {
                vm.recordEvent(
                    TrackBean(
                        p = PageHomeRefuse,
                        act = ACT_approvalDenied,
                        result = System.currentTimeMillis().toString()
                    )
                )
            }

            tvPreTips.text =
                String.format(getString(R.string.home_pre_tips), data.enableLoanStr ?: "-")
            val calmTips =
                String.format(getString(R.string.home_calm_tips3), data.enableLoanStr ?: "-")
            binding.calmLayout.tvCalmTips3.setClickableTextWithScale(
                calmTips,
                data.enableLoanStr ?: "-",
                binding.root.context.getColor2(R.color.C_374151)
            )
            if (!navigateToOrder) {
                val newProducts = data.showProducts.orEmpty().filter { it.newSign == 1 }
                if (newProducts.isNotEmpty()) {
                    showNewProductDialogIfNeeded(newProducts)
                } else {
                    showCreditDialogIfNeeded(data, productList) {}
                }
            }
            if (isCert) {
                authLayout.isVisible = false
                unAuthLayout.isVisible = false
                return@apply
            }

            calmLayout.calmLayout.isVisible = data.calmFlag == true
            if (data.calmFlag) {
                contentLayout.isVisible = false
                return@apply
            }

            if (productList.isEmpty()) {
                preLayout.isVisible = false
                refuseLayout.isVisible = false
                productLayout.isVisible = false
                bannerView.isVisible = false
                binding.calmLayout.calmLayout.isVisible = false
                topLayout.isVisible = false
            }

        }
    }

    private fun showCreditDialogIfNeeded(
        data: LoanDashboardResponse,
        productList: List<ProductBean>,
        onDismiss: () -> Unit,
    ): Boolean {
        with(binding.contentLayout) {
            if (hasShownCreditDialog) return false
            if (data.userCreditStatus != 1) return false
            if (!data.userCreditAmount.isPositive()) return false
            if (productList.isEmpty()) return false
            if (creditDialog?.isShowing == true || newProductDialog?.isShowing == true) return false

            val amount =data.userCreditAmount
                .formatAmountWithPrefix(data.userCreditCurrencySymbol ?: data.currencySymbol)
            hasShownCreditDialog = true
            var isNavigatingToLoan = false
            creditDialog = root.context.createAvailableCreditDialog(amount) {
                if (!tvLoan.isEnabled) return@createAvailableCreditDialog
                isNavigatingToLoan = true
                root.context.start<LoanProductMultiActivity>()
            }
            creditDialog?.setOnDismissListener {
                creditDialog = null
                if (!isNavigatingToLoan && isAdded && isResumed) {
                    onDismiss()
                }
            }
            creditDialog?.show()
            return true
        }
    }

    private fun showNewProductDialogIfNeeded(newProducts: List<ProductBean>) {
        if (newProducts.isEmpty()) return
        if (creditDialog?.isShowing == true || newProductDialog?.isShowing == true) return

        newProductDialog = context?.createNewProductDialog(newProducts, closeAction = {
            vm.recordEvent(TrackBean(p = PageHome, act = ACT_clickClose))
        }) {
            if (!binding.contentLayout.tvLoan.isEnabled) return@createNewProductDialog
            vm.recordEvent(TrackBean(p = PageHome, act = ACT_clickImmediate))
            context?.start<LoanProductMultiActivity>()
        }
        newProductDialog?.show()
    }

    @Suppress("SameParameterValue")
    private fun startCountdown(t: Long = 60L) = with(binding.contentLayout) {
        timeJob = lifecycleScope.countdownTimer(
            t, {
                tvPreTimes.isVisible = true
                val fullText = String.format(getString(R.string.home_refuse_times), t)
                tvPreTimes.setClickableTextWithScale(
                    fullText,
                    t.toString(),
                    root.context.getColor2(R.color.color_7087F8)
                )
            }, end = {
                tvPreTimes.isVisible = false
            }
        ) {
            val fullText = String.format(getString(R.string.home_refuse_times), it)
            tvPreTimes.setClickableTextWithScale(
                fullText,
                it.toString(),
                root.context.getColor2(R.color.color_7087F8)
            )
        }
    }

    private fun handleProductDetail(data: ProductBean?) {
        if (data == null) return
        if (signBackHome) {
            val map: MutableMap<Long?, Int?> = HashMap()
            data.productInstallmentPlanDTOList?.let { list ->
                val index = list.indexOfFirst { it.isDefault == 1 }.coerceAtLeast(0)
                if (index < list.size) {
                    map[list[index].productId] = list[index].planNums
                }
            }
            val termMap: MutableMap<Long?, Long?> = HashMap()
            data.loanTermConfigDTOList?.let { list ->
                val index = list.indexOfFirst { it.defaultSign == 1 }.coerceAtLeast(0)
                if (index < list.size) {
                    termMap[data.id] = list[index].id
                }
            }
            ContractSignActivity.launch(
                binding.root.context,
                data.bankInfoId,
                null,
                data.id.toString(),
                data.bankInfoId,
                data.maxLoanAmount?.toString(),
                if (map.isEmpty()) null else map.toJsonString(),
                termMap.toJsonString(),
                true
            )
        } else {
            context?.start<LoanProductActivity> {
                putExtra("product", data)
            }
        }
    }
}
