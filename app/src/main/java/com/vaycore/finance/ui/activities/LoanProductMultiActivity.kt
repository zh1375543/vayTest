package com.vaycore.finance.ui.activities

import androidx.activity.viewModels
import androidx.core.view.isVisible
import com.vaycore.finance.R
import com.vaycore.finance.base.BaseActivity
import com.vaycore.finance.data.ACT_clickApply
import com.vaycore.finance.data.ACT_clickConfirm
import com.vaycore.finance.data.ACT_in
import com.vaycore.finance.data.ACT_userAppBankMyCard
import com.vaycore.finance.data.PageProductDetail
import com.vaycore.finance.data.local.AGREEMENT_ABOUT
import com.vaycore.finance.data.local.LEASE_AGREEMENT
import com.vaycore.finance.data.local.PAWN_AGREEMENT
import com.vaycore.finance.data.local.bean.BankAccountResponse
import com.vaycore.finance.data.local.bean.ClickablePart
import com.vaycore.finance.data.local.bean.TrackBean
import com.vaycore.finance.data.local.loginInfo
import com.vaycore.finance.databinding.ActivityLoanProductMultiBinding
import com.vaycore.finance.ui.adapters.ComboAdapter
import com.vaycore.finance.ui.chooseAccountsDialog
import com.vaycore.finance.ui.extension.resetScale
import com.vaycore.finance.ui.extension.setSpannableClickableTexts
import com.vaycore.finance.ui.extension.singleClick
import com.vaycore.finance.ui.showLoanAgreementDialog
import com.vaycore.finance.ui.viewmodels.LoanApplyViewModel
import com.vaycore.finance.ui.viewmodels.WalletViewModel
import com.vaycore.finance.util.LoanEventUtil
import com.vaycore.finance.util.ORDER_COMMIT
import com.vaycore.finance.util.context.getColor2
import com.vaycore.finance.util.deviceRiskPermissions
import com.vaycore.finance.util.formatAmountWithPrefix
import com.vaycore.finance.util.maskSensitive
import com.vaycore.finance.util.requestRuntimePermissions
import com.vaycore.finance.util.toJsonString
import com.vaycore.finance.util.trackEvent
import com.vaycore.finance.util.viewBinding

class LoanProductMultiActivity : BaseActivity<ActivityLoanProductMultiBinding>() {

    override val binding by viewBinding(ActivityLoanProductMultiBinding::inflate)

    private val togetherAdapter by lazy { ComboAdapter() }
    private val vm by viewModels<LoanApplyViewModel>()
    private val accountVm by viewModels<WalletViewModel>()
    private val loanEvent by lazy { LoanEventUtil.instance }

    private var cardInfo: BankAccountResponse? = null
    private var hasRecordedEnterEvent = false
    private var leaseUrl = LEASE_AGREEMENT
    private var pawnUrl = PAWN_AGREEMENT

    override fun initView() = with(binding) {
        loanEvent.initEventFileUniqueSuffix((loginInfo?.id ?: 111).toString())

        titleBar.setNavigationAction { finish() }
        onBackAction(vm) { finish() }
        rvProduct.adapter = togetherAdapter

        tvAbout.singleClick {
            WebViewActivity.launch(
                this@LoanProductMultiActivity,
                tvAbout.text.toString(),
                AGREEMENT_ABOUT,
            )
        }

        tvPrivacy.setSpannableClickableTexts(
            String.format(
                getString(R.string.product_detail_agreement),
                getString(R.string.lease_contract),
                getString(R.string.mortgage_contract),
            ),
            arrayListOf(
                ClickablePart(
                    getString(R.string.lease_contract),
                    getColor2(R.color.color_7087F8),
                    onClick = {
                        loanEvent.logClickOpenAgreement()
                        WebViewActivity.launch(
                            this@LoanProductMultiActivity,
                            getString(R.string.lease_contract),
                            leaseUrl,
                        )
                    },
                ),
                ClickablePart(
                    getString(R.string.mortgage_contract),
                    getColor2(R.color.color_7087F8),
                    onClick = {
                        loanEvent.logClickOpenAgreement()
                        WebViewActivity.launch(
                            this@LoanProductMultiActivity,
                            getString(R.string.mortgage_contract),
                            pawnUrl,
                        )
                    },
                ),
            ),
        )

        tvChange.singleClick {
            vm.recordEvent(
                TrackBean(
                    p = PageProductDetail,
                    act = ACT_userAppBankMyCard,
                ),
            )
            loanEvent.logClickChooseWallet()
            accountVm.getLoanAccountList { }
        }

        btnApply.resetScale()
        btnApply.singleClick {
            vm.recordEvent(
                TrackBean(
                    p = PageProductDetail,
                    act = ACT_clickApply,
                    result = productIdsForTrack() + "|" + System.currentTimeMillis(),
                ),
            )
            loanEvent.logClickApplyLoan()
            requestRuntimePermissions(deviceRiskPermissions) {
                val (productInstallmentMap, termIdMap) = buildSubmissionMaps()
                trackEvent(ORDER_COMMIT)
                showLoanAgreementDialog(isTogether = true) {
                    vm.recordEvent(
                        TrackBean(
                            p = PageProductDetail,
                            act = ACT_clickConfirm,
                        ),
                    )
                    loanEvent.logClickSubmitLoan()
                    LoanApplyResultActivity.launch(
                        this@LoanProductMultiActivity,
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

        loadingLayout.setOnRetryClickListener {
            loadingLayout.showLoading()
            bottomLayout.isVisible = false
            vm.getTogetherLoan {
                loadingLayout.showError()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        loanEvent.initBaseServerTime(System.currentTimeMillis())
        loanEvent.logViewEnterLoan()
        binding.loadingLayout.showLoading()
        binding.bottomLayout.isVisible = false
        vm.getTogetherLoan {
            binding.loadingLayout.showError()
        }
    }

    override fun initObserve() = with(vm) {
        super.initObserve()
        togetherInfo.observe(this@LoanProductMultiActivity) { loan ->
            loan ?: return@observe

            val products = loan.showProducts.orEmpty().onEach { product ->
                product.canApply = true
                product.isTogether = true
            }
            togetherAdapter.submitItemsWithState(products)

            if (!hasRecordedEnterEvent) {
                hasRecordedEnterEvent = true
                recordEvent(
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
            bindReceivingAccount(cardInfo)

            binding.tvNum.text = products.size.toString()
            binding.tvAmount.text = loan.canApplyAmount.formatAmountWithPrefix(loan.currencySymbol)
            binding.bottomLayout.isVisible = true
            binding.loadingLayout.showContent()
        }

        accountVm.loanAccountList.observe(this@LoanProductMultiActivity) { accounts ->
            accounts ?: return@observe
            chooseAccountsDialog(cardInfo?.bankNo, accounts, false) { card ->
                cardInfo = card
                bindReceivingAccount(card)
            }
        }
    }

    override fun onStop() {
        super.onStop()
        loanEvent.logViewQuitLoan()
        loanEvent.writeLog2File()
    }

    private fun bindReceivingAccount(account: BankAccountResponse?) = with(binding) {
        tvCard.text = (account?.account ?: account?.bankNo).maskSensitive()
        tvAccountType.text = account?.payWay?.lowercase().orEmpty()
        loanTipLayout.isVisible = account?.payWay != "CARD"
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
