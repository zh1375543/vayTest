package com.vaycore.finance.loan

import android.view.View
import androidx.activity.viewModels
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.vaycore.finance.R
import com.vaycore.finance.base.BaseActivity
import com.vaycore.finance.data.ACT_clickApply
import com.vaycore.finance.data.ACT_clickConfirm
import com.vaycore.finance.data.ACT_in
import com.vaycore.finance.data.ACT_userAppBankMyCard
import com.vaycore.finance.data.PageProductDetail
import com.vaycore.finance.data.AGREEMENT_ABOUT
import com.vaycore.finance.data.LEASE_AGREEMENT
import com.vaycore.finance.data.PAWN_AGREEMENT
import com.vaycore.finance.data.bean.ClickablePart
import com.vaycore.finance.data.bean.TrackBean
import com.vaycore.finance.data.loginInfo
import com.vaycore.finance.databinding.ActivityLoanProductBinding
import com.vaycore.finance.model.loan.ProductBean
import com.vaycore.finance.model.wallet.BankAccountResponse
import com.vaycore.finance.web.WebViewActivity
import com.vaycore.finance.wallet.chooseAccountsDialog
import com.vaycore.finance.ui.extension.resetScale
import com.vaycore.finance.ui.extension.setSpannableClickableTexts
import com.vaycore.finance.ui.extension.singleClick
import com.vaycore.finance.wallet.showBankCardErrorDialog
import com.vaycore.finance.ui.showLoanAgreementDialog
import com.vaycore.finance.util.LoanEventUtil
import com.vaycore.finance.util.LogUtil
import com.vaycore.finance.util.ORDER_COMMIT
import com.vaycore.finance.util.context.getColor2
import com.vaycore.finance.util.deviceRiskPermissions
import com.vaycore.finance.util.formatAmountWithPrefix
import com.vaycore.finance.util.requestRuntimePermissions
import com.vaycore.finance.util.start
import com.vaycore.finance.util.toJsonString
import com.vaycore.finance.util.trackEvent
import com.vaycore.finance.util.viewBinding
import com.vaycore.finance.wallet.BankCardListActivity
import com.vaycore.finance.wallet.WalletViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

class LoanProductActivity : BaseActivity<ActivityLoanProductBinding>() {

    override val binding by viewBinding(ActivityLoanProductBinding::inflate)
    private val vm by viewModels<LoanProductViewModel>()
    private val accountVm by viewModels<WalletViewModel>()

    private val product by lazy { intent.getParcelableExtra<ProductBean>("product") }
    private var cardInfo: BankAccountResponse? = null

    private val loanEvent by lazy { LoanEventUtil.Companion.instance }
    private lateinit var leaseUrl: String
    private lateinit var pawnUrl: String
    private var isAddCard = false

    override fun initView() = with(binding) {
        vm.recordEvent(
            TrackBean(
                p = PageProductDetail,
                act = ACT_in,
                result = product?.id.toString() + "|" + System.currentTimeMillis()
            )
        )
        loanEvent.initEventFileUniqueSuffix((loginInfo?.id ?: 111).toString())
        leaseUrl =
            LEASE_AGREEMENT + "userId=${loginInfo?.id}&productId=${product?.id}&amount=${product?.maxLoanAmount.toString()}"
        pawnUrl =
            PAWN_AGREEMENT + "userId=${loginInfo?.id}&productId=${product?.id}&amount=${product?.maxLoanAmount.toString()}"
        titleBar.setNavigationAction { handleBack() }
        onBackAction(vm) {
            handleBack()
        }
        tvPrivacy.setSpannableClickableTexts(
            String.format(
                getString(R.string.product_detail_agreement),
                getString(R.string.lease_contract),
                getString(R.string.mortgage_contract)
            ), arrayListOf(
                ClickablePart(
                    getString(R.string.lease_contract), getColor2(R.color.color_7087F8), onClick = {
                        loanEvent.logClickOpenAgreement()
                        WebViewActivity.Companion.launch(
                            this@LoanProductActivity, getString(R.string.lease_contract), leaseUrl
                        )
                    }),
                ClickablePart(
                    getString(R.string.mortgage_contract),
                    getColor2(R.color.color_7087F8),
                    onClick = {
                        loanEvent.logClickOpenAgreement()
                        WebViewActivity.Companion.launch(
                            this@LoanProductActivity, getString(R.string.mortgage_contract), pawnUrl
                        )
                    }),
            )
        )
        loadingLayout.setOnRetryClickListener {
            loadingLayout.showLoading()
            vm.getProductDetail(
                PageProductDetail,
                product?.id.toString(), product?.maxLoanAmount.toString()
            ) {
                loadingLayout.showError()
                binding.product = null
            }
        }
        tvChange.singleClick {
            vm.recordEvent(
                TrackBean(
                    p = PageProductDetail,
                    act = ACT_userAppBankMyCard
                )
            )
            loanEvent.logClickChooseWallet()
            accountVm.getLoanAccountList {}
        }
        tvAbout.singleClick {
            WebViewActivity.Companion.launch(
                this@LoanProductActivity, tvAbout.text.toString(), AGREEMENT_ABOUT
            )
        }
        detailView.isVisible = true
        productSummaryView.setExpanded(true)
        productSummaryView.setOnDetailsClickListener {
            val isPlanVisible = !detailView.isVisible
            detailView.isVisible = isPlanVisible
            productSummaryView.setExpanded(isPlanVisible)
            vm.detailResult.value?.isPlanLayoutVisible = isPlanVisible
        }
//        ivDetail.rotation = 180f
//        detailView.isVisible = true
//        ivDetail.setOnClickListener {
//            detailView.isVisible = !detailView.isVisible
//            ivDetail.rotation = if (detailView.isVisible) 180f else 0f
//        }
        detailView.apply {
            onTermChanged = { productId, termId ->
                termIdMap.clear()
                termIdMap[productId] = termId
            }

            onInstallmentChanged = { productId, planNum ->
                productInstallmentMap.clear()
                productInstallmentMap[productId] = planNum
            }

            onPlanSelected = { selectedPlan ->
                bindHeaderDetail(selectedPlan)
            }
        }
        btnApply.resetScale()
        btnApply.singleClick {
            vm.recordEvent(
                TrackBean(
                    p = PageProductDetail,
                    act = ACT_clickApply,
                    result = product?.id.toString() + "|" + System.currentTimeMillis()
                )
            )
            loanEvent.logClickApplyLoan()
            requestRuntimePermissions(deviceRiskPermissions) {
                trackEvent(ORDER_COMMIT)
                showLoanAgreementDialog(
                    productId = product?.id.toString(),
                    amount = product?.loanAmount.toString()
                ) {
                    vm.recordEvent(
                        TrackBean(
                            p = PageProductDetail,
                            act = ACT_clickConfirm,
                        )
                    )
                    loanEvent.logClickSubmitLoan()
                    if (product?.isSign == 0) {
                        ContractSignActivity.Companion.launch(
                            this@LoanProductActivity,
                            cardInfo?.id,
                            null,
                            product?.id.toString(),
                            cardInfo?.id,
                            product?.loanAmount?.toString(),
                            if (productInstallmentMap.isEmpty()) null else productInstallmentMap.toJsonString(),
                            if (termIdMap.isEmpty()) null else termIdMap.toJsonString(),
                            payWay = cardInfo?.payWay ?: "CARD",
                        )
                    } else {
                        LoanApplyResultActivity.Companion.launch(
                            this@LoanProductActivity,
                            null,
                            product?.id.toString(),
                            cardInfo?.id,
                            null,
                            product?.loanAmount?.toString(),
                            if (productInstallmentMap.isEmpty()) null else productInstallmentMap.toJsonString(),
                            if (termIdMap.isEmpty()) null else termIdMap.toJsonString(),
                            payWay = cardInfo?.payWay ?: "CARD",
                        )
                        finish()
                    }
                }
            }
        }
    }

    private fun handleBack() {
        finish()
    }

    private var isFirstEnter = true

    // fields to persist the last UI state across resume
    private var savedTermIndex: Int = -1
    private var savedIsPlanLayoutVisible: Boolean = true
    override fun onResume() {
        super.onResume()
        loanEvent.initBaseServerTime(System.currentTimeMillis())
        loanEvent.logViewEnterLoan()
        if (vm.detailResult.value == null) {
            binding.loadingLayout.showLoading()
            binding.product = null
        }
        if (product != null && isFirstEnter) {
            vm.detailResult.value = product?.apply { isPlanLayoutVisible = true }
            isFirstEnter = false
        } else {
            // save current selection state before refreshing
            vm.detailResult.value?.let { old ->
                savedTermIndex = old.selectedTermIndex ?: -1
                savedIsPlanLayoutVisible = old.isPlanLayoutVisible ?: true
            }
            vm.getProductDetail(
                PageProductDetail,
                product?.id.toString(), product?.loanAmount.toString()
            ) {
                binding.product = null
                binding.loadingLayout.showError()
            }
        }
    }

    override fun onStop() {
        super.onStop()
        loanEvent.logViewQuitLoan()
        loanEvent.writeLog2File()
    }

    private var isShowBankcardError = false
    private val productInstallmentMap: MutableMap<Long?, Int?> = HashMap()
    private val termIdMap: MutableMap<Long?, Long?> = HashMap()
    override fun initObserve() = with(vm) {
        super.initObserve()
        detailResult.observe(this@LoanProductActivity) {
            binding.apply {
                it?.let {
                    binding.product = it
                    loadingLayout.showContent()
                    productSummaryView.bind(
                        it,
                        it.loanAmount.formatAmountWithPrefix(it.currencySymbol),
                    )
                    bindHeaderDetail(it)
                    mergeDetailViewState(it)

                    // if state was never saved (savedTermIndex < 0),
                    // ignore the Bean value (may be GSON default 0) and find the item with defaultSign == 1
                    if (it.selectedTermIndex == null || savedTermIndex < 0) {
                        val defaultSignIndex =
                            it.loanTermConfigDTOList?.indexOfFirst { it1 -> it1.defaultSign == 1 }
                                ?: -1
                        val isDefaultIndex =
                            it.loanTermConfigDTOList?.indexOfFirst { it1 -> it1.isDefault == 1 }
                                ?: -1
                        it.selectedTermIndex = when {
                            defaultSignIndex >= 0 -> defaultSignIndex
                            isDefaultIndex >= 0 -> isDefaultIndex
                            else -> 0
                        }
                    }
                    LogUtil.e("selectIndex1:${it.selectedTermIndex}")

                    if (it.isPlanLayoutVisible == null) {
                        it.isPlanLayoutVisible = true
                    }
                    detailView.isVisible = it.isPlanLayoutVisible ?: true
                    productSummaryView.setExpanded(detailView.isVisible)

                    detailView.setData(it)
                    if (cardInfo == null) {
                        cardInfo = BankAccountResponse(
                            id = it.bankInfoId ?: it.userCashWalletId,
                            bankNo = it.bankNo ?: it.walletAccount,
                            payWay = if (it.bankInfoId != null) "CARD" else "WALLET",
                        )
                        binding.account = cardInfo
                    }
                    if (it.bankInfoPayOutFailSign && !isShowBankcardError) {
                        lifecycleScope.launch {
                            delay(500.milliseconds)
                            isShowBankcardError = true
                            showBankCardErrorDialog(
                                desc = getString(R.string.card_error_tips),
                                cancel = getString(R.string.already_edited),
                                ok = getString(R.string.revise)
                            ) {
                                start<BankCardListActivity>()
                                isAddCard = true
                            }
                        }
                    }
                }
            }
        }
        accountVm.loanAccountList.observe(this@LoanProductActivity) {
            it?.let {
                chooseAccountsDialog(cardInfo?.bankNo, it, false) { card ->
                    cardInfo = card
                    binding.account = card
                }
            }
        }
    }

    fun scrollBottom() {
        binding.scrollView.postDelayed({
            binding.scrollView.fullScroll(View.FOCUS_DOWN)
        }, 200)
    }

    private fun mergeDetailViewState(newProduct: ProductBean) {
        // savedTermIndex == -1 means first load, no merge needed
        if (savedTermIndex < 0) return

        newProduct.selectedTermIndex = savedTermIndex
        newProduct.isPlanLayoutVisible = savedIsPlanLayoutVisible
    }

    private fun bindHeaderDetail(plan: ProductBean) = with(binding) {
        val currencySymbol = plan.currencySymbol ?: product?.currencySymbol
        detailView.bindHeaderDetail(plan, currencySymbol)
    }
}
