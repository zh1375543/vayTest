package com.vaycore.finance.home

import android.app.Dialog
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.vaycore.finance.R
import com.vaycore.finance.identity.routeToNextAuthStep
import com.vaycore.finance.identity.viewmodel.AuthStatusViewModel
import com.vaycore.finance.base.BaseFragment
import com.vaycore.finance.data.ACT_clickClose
import com.vaycore.finance.data.ACT_clickImmediate
import com.vaycore.finance.data.ACT_userAppBankMyCard
import com.vaycore.finance.data.PageHome
import com.vaycore.finance.data.bean.Event
import com.vaycore.finance.data.bean.TrackBean
import com.vaycore.finance.data.isLogin
import com.vaycore.finance.data.signBackHome
import com.vaycore.finance.databinding.FragmentLoanHomeBinding
import com.vaycore.finance.loan.AgreementSignatureActivity
import com.vaycore.finance.loan.LoanOfferActivity
import com.vaycore.finance.loan.CombinedLoanOfferActivity
import com.vaycore.finance.loan.viewmodel.LoanDashboardViewModel
import com.vaycore.finance.loan.viewmodel.LoanProductViewModel
import com.vaycore.finance.model.identity.UserAuthStatusResponse
import com.vaycore.finance.model.home.GuestHomeResponse
import com.vaycore.finance.model.loan.ProductBean
import com.vaycore.finance.app.MainActivity
import com.vaycore.finance.feedback.FeedbackViewModel
import com.vaycore.finance.home.state.HomeEffect
import com.vaycore.finance.home.state.HomeProductUi
import com.vaycore.finance.home.state.MemberHomeUiState
import com.vaycore.finance.home.viewmodel.HomeViewModel
import com.vaycore.finance.ui.createAvailableCreditDialog
import com.vaycore.finance.ui.createNewProductDialog
import com.vaycore.finance.ui.extension.animateAmount
import com.vaycore.finance.ui.extension.resetScale
import com.vaycore.finance.ui.extension.setClickableTextWithScale
import com.vaycore.finance.ui.extension.singleClick
import com.vaycore.finance.ui.extension.stopScaleAnimation
import com.vaycore.finance.ui.showAppRatingDialog
import com.vaycore.finance.ui.showCreditUnderReviewDialog
import com.vaycore.finance.ui.showPreCreditExpiredDialog
import com.vaycore.finance.util.LOAN_GET_NOW_CLICK
import com.vaycore.finance.util.context.resolveColorCompat
import com.vaycore.finance.util.ExternalActionLauncher
import com.vaycore.finance.util.countdownTimer
import com.vaycore.finance.util.formatAmountWithPrefix
import com.vaycore.finance.util.platform.formatLoanTerm
import com.vaycore.finance.util.platform.requireLogin
import com.vaycore.finance.util.showToastMessage
import com.vaycore.finance.util.start
import com.vaycore.finance.util.toJsonString
import com.vaycore.finance.util.trackEvent
import com.vaycore.finance.util.viewBinding
import com.vaycore.finance.wallet.PayoutAccountListActivity
import kotlinx.coroutines.Job

class HomeFragment : BaseFragment<FragmentLoanHomeBinding>(R.layout.fragment_loan_home) {

    override val binding by viewBinding(FragmentLoanHomeBinding::bind)

    private val vm by viewModels<HomeViewModel>()
    private val loanDashboardVm by viewModels<LoanDashboardViewModel>()
    private val authStatusVm by viewModels<AuthStatusViewModel>()
    private val feedbackVm by viewModels<FeedbackViewModel>()
    private val productVm by viewModels<LoanProductViewModel>()

    private val homeAdapter by lazy {
        LoanCatalogAdapter().apply {
            setOnChildClickListener { view, _, position ->
                when (view.id) {
                    R.id.tvApply -> {
                        trackEvent(LOAN_GET_NOW_CLICK)
                        val item = items[position]
                        if (!item.canApply) return@setOnChildClickListener
                        val product = item.product
                        if (product.creditStatus == 2) {
                            context.showPreCreditExpiredDialog(product.enableLoanStr ?: "")
                            return@setOnChildClickListener
                        }
                        if (product.creditStatus == 0) {
                            context.showCreditUnderReviewDialog()
                            return@setOnChildClickListener
                        }
                        when (product.jumpType) {
                            1 -> product.downloadUrl?.let {
                                ExternalActionLauncher.openBrowser(requireContext(), it)
                            }
                            2 -> ExternalActionLauncher.openStoreListing(
                                requireContext(),
                                product.downloadUrl,
                            )
                            else -> {
                                productVm.getProductDetail(
                                    PageHome,
                                    product.productId.toString(),
                                    product.maxLoanAmount.toString(),
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

    override fun initView() {
        setupProductList()
        setupBusinessActions()
        setupRefreshTriggers()
    }

    private fun setupProductList() {
        binding.contentLayout.rvProduct.adapter = homeAdapter
    }

    private fun setupBusinessActions() = with(binding.contentLayout) {
        tvModifyCard.singleClick {
            vm.submitTrackingEvent(
                TrackBean(
                    p = PageHome,
                    act = ACT_userAppBankMyCard
                )
            )
            it.context.start<PayoutAccountListActivity>()
        }
        tvBorrowNow.singleClick {
            it.context.requireLogin {
                isGoAuth = true
                authStatusVm.getUserAuthStatus()
            }
        }
        tvLoan.singleClick {
            it.context.requireLogin {
                context?.start<CombinedLoanOfferActivity>()
            }
        }
        ivCloseBank.singleClick {
            bankErrorLayout.isVisible = false
        }
    }

    private fun setupRefreshTriggers() = with(binding) {
        contentLayout.tvRefresh.singleClick {
            refreshData()
        }
        loadingLayout.setOnRetryClickListener {
            refreshData()
        }
        swipeRefreshLayout.setOnRefreshListener {
            refreshData()
        }
    }

    private var isGoAuth = false
    private var hasRenderedLoadError = false

    override fun onResume() {
        super.onResume()
        // ViewPager keeps HomeFragment alive, so allow the credit dialog once per Home entry.
        hasShownCreditDialog = false
        refreshData()
    }

    fun refreshData() = with(binding) {
        hasRenderedLoadError = false
        loadingLayout.showLoading()
        contentLayout.apply {
            topLayout.isVisible = true
            contentLayout.isVisible = true
        }
        calmLayout.calmLayout.isVisible = false
        if (isLogin) {
            isGoAuth = false
            authStatusVm.getUserAuthStatus()
        } else {
            vm.getUnAuthData()
        }
        vm.getBannerList()
    }

    private var timeJob: Job? = null
    private var loanDateStr: String? = null

    override fun initObserve() {
        vm.loadFailedResult.observe(this@HomeFragment) {
            renderLoadError()
        }
        loanDashboardVm.loadFailedResult.observe(this@HomeFragment) {
            renderLoadError()
        }
        authStatusVm.loadFailedResult.observe(this@HomeFragment) {
            renderLoadError()
        }
        authStatusVm.userAuthStatusResult.observe(this@HomeFragment) {
            handleUserAuthStatus(it)
        }
        vm.guestResult.observe(this@HomeFragment) {
            it?.let(::handleUnAuthData)
        }
        loanDashboardVm.memberHomeState.observe(this@HomeFragment, ::renderMemberHome)
        loanDashboardVm.homeEffect.observe(this@HomeFragment, ::handleHomeEffectEvent)
        vm.bannerResult.observe(this@HomeFragment) {
            binding.contentLayout.bannerView.setData(it ?: emptyList())
            binding.contentLayout.bannerView.isVisible = !it.isNullOrEmpty()
        }
        productVm.detailResult.observe(this@HomeFragment) {
            handleProductDetail(it)
        }
    }

    private fun renderLoadError() {
        if (hasRenderedLoadError) return

        hasRenderedLoadError = true
        binding.swipeRefreshLayout.isRefreshing = false
        binding.loadingLayout.showError()
    }

    private fun handleUserAuthStatus(data: UserAuthStatusResponse?) {
        if (isGoAuth) {
            data?.routeToNextAuthStep(binding.root.context, false)
            return
        }
        isGoAuth = false
        authStatusVm.fetchAuthConfigList { configList ->
            if (data?.isPass(configList) == true) {
                loanDashboardVm.getMemberHomeData()
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
            tvPeriod.text = root.context.formatLoanTerm(data.loanTerm)
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

    private fun renderMemberHome(state: MemberHomeUiState) = with(binding) {
        loadingLayout.showContent()
        swipeRefreshLayout.isRefreshing = false
        contentLayout.apply {
            tvAmount2.animateAmount(
                state.availableAmount,
                prefix = state.creditCurrencySymbol ?: ""
            )
            tvMaxAmount.text = state.totalAmount.formatAmountWithPrefix(state.creditCurrencySymbol)
            tvUsedAmount.text = state.usedAmount.formatAmountWithPrefix(state.creditCurrencySymbol)
            tvLoanRateLabel.text = state.recommendText
            tvLoanRateLabel.isVisible = !state.recommendText.isNullOrEmpty()
            unAuthLayout.isVisible = false
            authLayout.isVisible = state.showAuthenticatedLayout
            tvQuick.isVisible = true
            questionLayout.isVisible = false
            productLayout.isVisible = state.showProductList
            topLayout.isVisible = state.showCreditHeader
            val showProductChrome = !state.showCalmPage
            noticeLayout.isVisible = state.showProductList && showProductChrome
            homeTicketCard.isVisible = state.showCreditHeader && showProductChrome
            if (state.showProductList && showProductChrome) {
                marqueeView.setTexts()
            } else {
                marqueeView.stop()
            }
            contentLayout.isVisible = !state.showCalmPage
            loanDateStr = state.enableLoanDate
            tvLoan.isEnabled = state.loanEnabled
            if (tvLoan.isEnabled) tvLoan.resetScale() else tvLoan.stopScaleAnimation()

            bankErrorLayout.isVisible = state.showBankError

            timeJob?.cancel()
            if (state.showReviewLayout) {
                startCountdown(60)
            }

            homeAdapter.submitItems(state.products)
            productLayout.isVisible = state.showProductList
            emptyProduct.isVisible = state.showEmptyProducts
            preLayout.isVisible = state.showReviewLayout
            refuseLayout.isVisible = state.showRejectedLayout

            tvPreTips.text =
                String.format(getString(R.string.home_pre_tips), state.enableLoanDate ?: "-")
            val calmTips =
                String.format(getString(R.string.home_calm_tips3), state.enableLoanDate ?: "-")
            binding.calmLayout.tvCalmTips3.setClickableTextWithScale(
                calmTips,
                state.enableLoanDate ?: "-",
                binding.root.context.resolveColorCompat(R.color.text_body)
            )
            if (!state.showAuthenticatedLayout) {
                return@apply
            }

            calmLayout.calmLayout.isVisible = state.showCalmPage
            if (state.showCalmPage) {
                contentLayout.isVisible = false
                return@apply
            }

            if (state.showEmptyProducts) {
                preLayout.isVisible = false
                refuseLayout.isVisible = false
                productLayout.isVisible = false
                bannerView.isVisible = false
                binding.calmLayout.calmLayout.isVisible = false
                topLayout.isVisible = false
            }

        }
    }

    private fun handleHomeEffectEvent(event: Event<HomeEffect>) {
        when (val effect = event.getContentIfNotHandled() ?: return) {
            HomeEffect.ShowAppRating -> {
                activity?.showAppRatingDialog { content ->
                    feedbackVm.submitFeed(content) {
                        getString(R.string.feedback_success).showToastMessage()
                    }
                }
            }

            HomeEffect.NavigateToOrders -> (activity as MainActivity?)?.selectPage(1)
            is HomeEffect.ShowNewProducts -> showNewProductDialogIfNeeded(effect.products)
            is HomeEffect.ShowAvailableCredit -> showCreditDialogIfNeeded(effect)
        }
    }

    private fun showCreditDialogIfNeeded(effect: HomeEffect.ShowAvailableCredit) {
        with(binding.contentLayout) {
            if (hasShownCreditDialog) return
            if (creditDialog?.isShowing == true || newProductDialog?.isShowing == true) return

            val amount = effect.amount.formatAmountWithPrefix(effect.currencySymbol)
            hasShownCreditDialog = true
            creditDialog = root.context.createAvailableCreditDialog(amount) {
                if (!tvLoan.isEnabled) return@createAvailableCreditDialog
                root.context.start<CombinedLoanOfferActivity>()
            }
            creditDialog?.setOnDismissListener {
                creditDialog = null
            }
            creditDialog?.show()
        }
    }

    private fun showNewProductDialogIfNeeded(newProducts: List<HomeProductUi>) {
        if (newProducts.isEmpty()) return
        if (creditDialog?.isShowing == true || newProductDialog?.isShowing == true) return

        val dialogProducts = newProducts.map { productUi ->
            productUi.product.copy(canApply = productUi.canApply)
        }
        newProductDialog = context?.createNewProductDialog(dialogProducts, closeAction = {
            vm.submitTrackingEvent(TrackBean(p = PageHome, act = ACT_clickClose))
        }) {
            if (!binding.contentLayout.tvLoan.isEnabled) return@createNewProductDialog
            vm.submitTrackingEvent(TrackBean(p = PageHome, act = ACT_clickImmediate))
            context?.start<CombinedLoanOfferActivity>()
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
                    root.context.resolveColorCompat(R.color.brand_primary)
                )
            }, end = {
                tvPreTimes.isVisible = false
            }
        ) {
            val fullText = String.format(getString(R.string.home_refuse_times), it)
            tvPreTimes.setClickableTextWithScale(
                fullText,
                it.toString(),
                root.context.resolveColorCompat(R.color.brand_primary)
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
            AgreementSignatureActivity.Companion.launch(
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
            context?.start<LoanOfferActivity> {
                putExtra("product", data)
            }
        }
    }
}
