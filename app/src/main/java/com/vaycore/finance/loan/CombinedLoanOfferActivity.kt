package com.vaycore.finance.loan

import androidx.activity.viewModels
import androidx.core.view.isVisible
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
import com.vaycore.finance.databinding.ActivityCombinedLoanOfferBinding
import com.vaycore.finance.model.wallet.BankAccountResponse
import com.vaycore.finance.browser.WebViewActivity
import com.vaycore.finance.loan.adapter.ComboAdapter
import com.vaycore.finance.loan.viewmodel.LoanApplyViewModel
import com.vaycore.finance.wallet.chooseAccountsDialog
import com.vaycore.finance.ui.extension.resetScale
import com.vaycore.finance.ui.extension.setSpannableClickableTexts
import com.vaycore.finance.ui.extension.singleClick
import com.vaycore.finance.ui.showLoanAgreementDialog
import com.vaycore.finance.util.loanevent.LoanEvent
import com.vaycore.finance.util.loanevent.LoanEventRecorder
import com.vaycore.finance.util.ORDER_COMMIT
import com.vaycore.finance.util.context.resolveColorCompat
import com.vaycore.finance.util.PermissionCoordinator
import com.vaycore.finance.util.PermissionScenario
import com.vaycore.finance.util.toJsonString
import com.vaycore.finance.util.trackEvent
import com.vaycore.finance.util.viewBinding
import com.vaycore.finance.wallet.WalletViewModel

class CombinedLoanOfferActivity : BaseActivity<ActivityCombinedLoanOfferBinding>() {

    override val binding by viewBinding(ActivityCombinedLoanOfferBinding::inflate)

    private val togetherAdapter by lazy { ComboAdapter() }
    private val vm by viewModels<LoanApplyViewModel>()
    private val accountVm by viewModels<WalletViewModel>()

    private var cardInfo: BankAccountResponse? = null
    private var hasRecordedEnterEvent = false
    private var leaseUrl = LEASE_AGREEMENT
    private var pawnUrl = PAWN_AGREEMENT

    override fun initView() {
        prepareBundleScreen()
        connectBundleUtilities()
        connectBundleApplication()
    }

    private fun prepareBundleScreen() = with(binding) {
        LoanEventRecorder.setEventFileSuffix((loginInfo?.id ?: 111).toString())

        titleBar.setNavigationAction { finish() }
        registerTrackedBackHandler(vm) { finish() }
        rvProduct.adapter = togetherAdapter
    }

    private fun connectBundleUtilities() = with(binding) {
        tvAbout.singleClick {
            WebViewActivity.Companion.launch(
                this@CombinedLoanOfferActivity,
                tvAbout.text.toString(),
                AGREEMENT_ABOUT,
            )
        }
        tvChange.singleClick {
            vm.submitTrackingEvent(
                TrackBean(
                    p = PageProductDetail,
                    act = ACT_userAppBankMyCard,
                ),
            )
            LoanEventRecorder.record(LoanEvent.CLICK_CHOOSE_WALLET)
            accountVm.getLoanAccountList { }
        }

        loadingLayout.setOnRetryClickListener {
            loadingLayout.showLoading()
            bottomLayout.isVisible = false
            vm.getTogetherLoan {
                loadingLayout.showError()
            }
        }
    }

    private fun connectBundleApplication() = with(binding) {
        btnApply.resetScale()
        btnApply.singleClick {
            vm.submitTrackingEvent(
                TrackBean(
                    p = PageProductDetail,
                    act = ACT_clickApply,
                    result = productIdsForTrack() + "|" + System.currentTimeMillis(),
                ),
            )
            LoanEventRecorder.record(LoanEvent.CLICK_APPLY_LOAN)
            PermissionCoordinator.request(this@CombinedLoanOfferActivity, PermissionScenario.DEVICE_RISK) {
                val (productInstallmentMap, termIdMap) = buildSubmissionMaps()
                trackEvent(ORDER_COMMIT)
                showLoanAgreementDialog(isTogether = true) {
                    vm.submitTrackingEvent(
                        TrackBean(
                            p = PageProductDetail,
                            act = ACT_clickConfirm,
                        ),
                    )
                    LoanEventRecorder.record(LoanEvent.CLICK_SUBMIT_LOAN)
                    ApplicationOutcomeActivity.Companion.launch(
                        this@CombinedLoanOfferActivity,
                        ArrayList(togetherAdapter.items),
                        null,
                        cardInfo?.id ?: 0L,
                        null,
                        null,
                        if (productInstallmentMap.isEmpty()) {
                            null
                        } else {
                            productInstallmentMap.toJsonString()
                        },
                        if (termIdMap.isEmpty()) {
                            null
                        } else {
                            termIdMap.toJsonString()
                        },
                        payWay = cardInfo?.payWay.orEmpty(),
                    )
                    finish()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        LoanEventRecorder.initializeBaseServerTime(System.currentTimeMillis())
        LoanEventRecorder.record(LoanEvent.VIEW_ENTER_LOAN)
        binding.loadingLayout.showLoading()
        binding.bottomLayout.isVisible = false
        vm.getTogetherLoan {
            binding.loadingLayout.showError()
        }
    }

    override fun initObserve() = with(vm) {
        super.initObserve()
        togetherInfo.observe(this@CombinedLoanOfferActivity) { loan ->
            loan ?: return@observe

            val products = loan.showProducts.orEmpty().onEach { product ->
                product.canApply = true
                product.isTogether = true
            }
            togetherAdapter.submitItemsWithState(products)

            if (!hasRecordedEnterEvent) {
                hasRecordedEnterEvent = true
                submitTrackingEvent(
                    TrackBean(
                        p = PageProductDetail,
                        act = ACT_in,
                        result = productIdsForTrack() + "|" + System.currentTimeMillis(),
                    ),
                )
            }

            val agreementProductIds = productIdsForTrack()
            leaseUrl = LEASE_AGREEMENT +
                "userId=${loginInfo?.id}&productId=$agreementProductIds&amount=${loan.canApplyAmount}"
            pawnUrl = PAWN_AGREEMENT +
                "userId=${loginInfo?.id}&productId=$agreementProductIds&amount=${loan.canApplyAmount}"

            if (cardInfo == null) {
                cardInfo = BankAccountResponse(
                    id = loan.userCashWalletId ?: loan.bankInfoId,
                    bankNo = loan.walletAccount ?: loan.bankNo,
                    payWay = if (loan.userCashWalletId != null) "WALLET" else "CARD",
                )
            }
            binding.togetherInfo = loan
            binding.account = cardInfo
            binding.bottomLayout.isVisible = true
            binding.loadingLayout.showContent()
        }

        accountVm.loanAccountList.observe(this@CombinedLoanOfferActivity) { accounts ->
            accounts ?: return@observe
            chooseAccountsDialog(cardInfo?.bankNo, accounts, false) { card ->
                cardInfo = card
                binding.account = card
            }
        }
    }

    override fun onStop() {
        super.onStop()
        LoanEventRecorder.record(LoanEvent.VIEW_QUIT_LOAN)
        LoanEventRecorder.flush()
    }

    private fun productIdsForTrack(): String = togetherAdapter.items.joinToString(",") { product ->
        (product.id ?: product.productId).toString()
    }

    private fun buildSubmissionMaps(): Pair<MutableMap<Long?, Int?>, MutableMap<Long?, Long?>> {
        val productInstallmentMap = mutableMapOf<Long?, Int?>()
        val termIdMap = mutableMapOf<Long?, Long?>()

        togetherAdapter.items.forEach { product ->
            val plans = product.loanTermConfigDTOList
            if (plans.isNullOrEmpty()) return@forEach

            val selectedIndex = (product.selectedTermIndex ?: 0).coerceIn(plans.indices)
            val selectedPlan = plans[selectedIndex]
            val productId = product.id ?: product.productId

            termIdMap[productId] = selectedPlan.id
            productInstallmentMap[productId] =
                selectedPlan.productInstallmentPlanDTOList?.firstOrNull()?.planNums
        }

        return productInstallmentMap to termIdMap
    }
}
