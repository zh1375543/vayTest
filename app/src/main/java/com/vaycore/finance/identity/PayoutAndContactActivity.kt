package com.vaycore.finance.identity

import android.annotation.SuppressLint
import android.content.Intent
import android.provider.ContactsContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import com.vaycore.finance.R
import com.vaycore.finance.app.App
import com.vaycore.finance.base.BaseActivity
import com.vaycore.finance.databinding.ActivityPayoutAndContactBinding
import com.vaycore.finance.data.ACT_clickBack
import com.vaycore.finance.data.ACT_clickContinue
import com.vaycore.finance.data.ACT_clickNext
import com.vaycore.finance.data.ACT_in
import com.vaycore.finance.data.ACT_selectContactName1End
import com.vaycore.finance.data.ACT_selectContactName1Start
import com.vaycore.finance.data.ACT_selectContactName2End
import com.vaycore.finance.data.ACT_selectContactName2Start
import com.vaycore.finance.data.ACT_selectContactName3End
import com.vaycore.finance.data.ACT_selectContactName3Start
import com.vaycore.finance.data.PageInfoBank
import com.vaycore.finance.data.authConfigList
import com.vaycore.finance.data.loginInfo
import com.vaycore.finance.data.bean.ApiRequest
import com.vaycore.finance.model.wallet.BankChannelResponse
import com.vaycore.finance.model.identity.RelativesBean
import com.vaycore.finance.data.bean.TrackBean
import com.vaycore.finance.identity.viewmodel.AuthStatusViewModel
import com.vaycore.finance.identity.viewmodel.PersonalInfoViewModel
import com.vaycore.finance.identity.viewmodel.WorkContactViewModel
import com.vaycore.finance.model.wallet.WalletResponse
import com.vaycore.finance.wallet.WalletViewModel
import com.vaycore.finance.util.SUPPLEMENTARY_INFO_COMMIT
import com.vaycore.finance.ui.extension.resetScale
import com.vaycore.finance.util.PermissionCoordinator
import com.vaycore.finance.util.PermissionScenario
import com.vaycore.finance.ui.extension.singleClick
import com.vaycore.finance.util.trackEvent
import com.vaycore.finance.wallet.chooseBankDialog
import com.vaycore.finance.wallet.chooseWalletDialog
import com.vaycore.finance.ui.showConfirmDialog
import com.vaycore.finance.wallet.showWithdrawMethodDialog
import com.vaycore.finance.ui.showOptionPickerDialog
import com.vaycore.finance.util.getContactInfo
import com.vaycore.finance.util.viewBinding
import kotlin.math.max
import kotlin.toString

class PayoutAndContactActivity : BaseActivity<ActivityPayoutAndContactBinding>() {

    private enum class WithdrawMethod {
        BANK,
        WALLET,
    }

    private enum class ContactPickTarget {
        PRIMARY,
        SECONDARY,
        ADDITIONAL,
    }

    override val binding by viewBinding(ActivityPayoutAndContactBinding::inflate)

    private val vm by viewModels<WorkContactViewModel>()
    private val personalVm by viewModels<PersonalInfoViewModel>()
    private val homeVm by viewModels<AuthStatusViewModel>()
    private val accountVm by viewModels<WalletViewModel>()
    private val isCert by lazy { intent.getBooleanExtra("isCert", false) }
    private var shouldShowBottomAction = false
    private var isKeyboardVisible = false
    private var selectedWithdrawMethod: WithdrawMethod? = null

    override fun initView() {
        renderBankEntryState()
        connectWithdrawalInputs()
        connectBankPageActions()
    }

    /** Render the page mode, title and bottom action area for the current auth step. */
    private fun renderBankEntryState() = with(binding) {
        isCertified = isCert
        showBankFields = false
        showWalletFields = false
        setupBottomActionKeyboardBehavior()
        vm.submitTrackingEvent(TrackBean(p = PageInfoBank, act = ACT_in))
        titleBar.setAction("${authConfigList.indexOf("BANK") + 1}/${authConfigList.size}")
        clearWithdrawMethodSelection()
        setBottomActionVisible(false)
        titleBar.showAction(!isCert)
        titleBar.updateTitle(
            if (isCert) getString(R.string.contact_info) else getString(R.string.bank_and_contact),
        )
        if (!isCert) {
            btNext.resetScale()
        }
    }

    /** Connect bank, wallet and contact field interactions. */
    private fun connectWithdrawalInputs() = with(binding) {
        with(withdrawAccountForm) {
            methodSelectionView.setOnClickListener {
                showWithdrawMethodDialog(
                    walletAction = { accountVm.getWalletList() },
                    bankAction = { accountVm.getPayChannelList() },
                )
            }
            bankView.setOnClick { accountVm.getPayChannelList() }
            walletProviderView.setOnClick { accountVm.getWalletList() }
            bankAccountView.getEditText().doAfterTextChanged {
                bankAccountView.hideError()
                if (it.toString() == confirmBankView.getText()) {
                    confirmBankView.hideError()
                }
            }
            confirmBankView.getEditText().doAfterTextChanged {
                if (it.toString() == bankAccountView.getText()) {
                    confirmBankView.hideError()
                }
            }
            walletAccountView.getEditText().doAfterTextChanged {
                walletAccountView.hideError()
                if (it.toString() == confirmWalletAccountView.getText()) {
                    confirmWalletAccountView.hideError()
                }
            }
            confirmWalletAccountView.getEditText().doAfterTextChanged {
                if (it.toString() == walletAccountView.getText()) {
                    confirmWalletAccountView.hideError()
                }
            }
        }
        relativesView.setOnClick {
            vm.getContactEnum {
                val relativesList = it.relatives ?: arrayListOf()
                showOptionPickerDialog(
                    relativesList.indexOfFirst { item -> relativesView.getText() == item.info },
                    relativesList,
                ) { index ->
                    relativesView.setText(relativesList[index].info)
                    relativesView.hideError()
                    relativesStatus = relativesList[index].state
                }
            }
        }
        friendView.setOnClick {
            vm.getContactEnum {
                val relativesList = it.otherRelatives ?: arrayListOf()
                showOptionPickerDialog(
                    relativesList.indexOfFirst { item -> friendView.getText() == item.info },
                    relativesList,
                ) { index ->
                    friendView.setText(relativesList[index].info)
                    friendView.hideError()
                    friendStatus = relativesList[index].state
                }
            }
        }
        additionalContactRelationshipView.setOnClick {
            vm.getContactEnum {
                val relationshipOptions = it.otherRelatives ?: arrayListOf()
                showOptionPickerDialog(
                    relationshipOptions.indexOfFirst {
                        additionalContactRelationshipView.getText() == it.info
                    },
                    relationshipOptions,
                ) { index ->
                    additionalContactRelationshipView.setText(relationshipOptions[index].info)
                    additionalContactRelationshipView.hideError()
                    additionalContactStatus = relationshipOptions[index].state
                }
            }
        }
        relativesPhoneView.setContactClick {
            recordContactPickStart(ACT_selectContactName1Start, ContactPickTarget.PRIMARY)
        }
        friendPhoneView.setContactClick {
            recordContactPickStart(ACT_selectContactName2Start, ContactPickTarget.SECONDARY)
        }
        additionalContactPhoneView.setContactClick {
            recordContactPickStart(ACT_selectContactName3Start, ContactPickTarget.ADDITIONAL)
        }
    }

    /** Connect navigation, validation, permission-gated submission and initial data loading. */
    private fun connectBankPageActions() = with(binding) {
        titleBar.setNavigationAction { confirmPayoutSetupExit() }
        registerTrackedBackHandler(vm) { confirmPayoutSetupExit() }
        btNext.singleClick {
            if (!validateBankPage()) {
                return@singleClick
            }
            PermissionCoordinator.request(this@PayoutAndContactActivity, PermissionScenario.DEVICE_RISK) {
                App.appViewModel.hasDeviceInfo(PageInfoBank) { isPost ->
                    if (isPost) {
                        submit()
                        return@hasDeviceInfo
                    }
                    App.appViewModel.postRiskInfo(PageInfoBank) { isSuccess ->
                        if (isSuccess) {
                            submit()
                        }
                    }
                }
            }
        }
        loadingLayout.setOnRetryClickListener {
            setBottomActionVisible(false)
            loadingLayout.showLoading()
            vm.getContactsInfo { loadingLayout.showError() }
        }
        loadingLayout.showLoading()
        vm.getContactsInfo { loadingLayout.showError() }
        personalVm.getPersonalInfo {}
    }

    private fun validateBankPage(): Boolean = with(binding) {
        with(withdrawAccountForm) {
            when (selectedWithdrawMethod) {
                WithdrawMethod.BANK -> {
                    if (bankView.getText().isBlank()) {
                        bankView.showError()
                        scrollView.scrollTo(0, bankView.top)
                        return false
                    }
                    if (holderView.getText().isBlank()) {
                        holderView.showError()
                        scrollView.scrollTo(0, holderView.top)
                        return false
                    }
                    if (bankAccountView.getText().isBlank()) {
                        bankAccountView.showError()
                        scrollView.scrollTo(0, bankAccountView.top)
                        return false
                    }
                    if (confirmBankView.getText() != bankAccountView.getText()) {
                        confirmBankView.showError()
                        scrollView.scrollTo(0, confirmBankView.top)
                        return false
                    }
                }
                WithdrawMethod.WALLET -> {
                    if (walletProviderView.getText().isBlank()) {
                        walletProviderView.showError()
                        scrollView.scrollTo(0, walletProviderView.top)
                        return false
                    }
                    if (walletAccountView.getText().isBlank()) {
                        walletAccountView.showError()
                        scrollView.scrollTo(0, walletAccountView.top)
                        return false
                    }
                    if (confirmWalletAccountView.getText() != walletAccountView.getText()) {
                        confirmWalletAccountView.showError()
                        scrollView.scrollTo(0, confirmWalletAccountView.top)
                        return false
                    }
                }
                null -> {
                    tvWithdrawMethodError.isVisible = true
                    methodSelectionView.performClick()
                    return false
                }
            }
        }
        if (relativesView.getText().isBlank()) {
            relativesView.showError()
            scrollView.scrollTo(0, relativesView.top)
            return false
        }
        if (relativesNameView.getText().isBlank()) {
            relativesNameView.showError()
            scrollView.scrollTo(0, relativesNameView.top)
            return false
        }
        if (relativesPhoneView.getText().isBlank()) {
            relativesPhoneView.showError()
            scrollView.scrollTo(0, relativesPhoneView.top)
            return false
        }
        if (friendView.getText().isBlank()) {
            friendView.showError()
            scrollView.scrollTo(0, friendView.top)
            return false
        }
        if (friendNameView.getText().isBlank()) {
            friendNameView.showError()
            scrollView.scrollTo(0, friendNameView.top)
            return false
        }
        if (friendPhoneView.getText().isBlank()) {
            friendPhoneView.showError()
            scrollView.scrollTo(0, friendPhoneView.top)
            return false
        }
        return true
    }

    private fun recordContactPickStart(event: String, target: ContactPickTarget) {
        vm.submitTrackingEvent(
            TrackBean(
                p = PageInfoBank,
                act = event,
                result = System.currentTimeMillis().toString(),
            ),
        )
        contactPickTarget = target
        pickContact()
    }

    private fun setupBottomActionKeyboardBehavior() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.bottomActionLayout) { _, insets ->
            isKeyboardVisible = insets.isVisible(WindowInsetsCompat.Type.ime())
            updateBottomActionVisibility()
            insets
        }
        ViewCompat.requestApplyInsets(binding.bottomActionLayout)
    }

    private fun setBottomActionVisible(visible: Boolean) {
        shouldShowBottomAction = visible
        updateBottomActionVisibility()
    }

    private fun updateBottomActionVisibility() {
        binding.bottomActionLayout.isVisible = shouldShowBottomAction && !isKeyboardVisible
    }

    private fun selectWithdrawMethod(method: WithdrawMethod) = with(binding.withdrawAccountForm) {
        selectedWithdrawMethod = method
        tvWithdrawMethodError.isVisible = false
        val iconRes = if (method == WithdrawMethod.BANK) {
            R.mipmap.ic_bank_select_bg
        } else {
            R.mipmap.ic_wallet_select_bg
        }
        val iconSize = resources.getDimensionPixelSize(R.dimen.dp_36)
        val icon = AppCompatResources.getDrawable(this@PayoutAndContactActivity, iconRes)?.apply {
            setBounds(0, 0, iconSize, iconSize)
        }
        val arrowSize = resources.getDimensionPixelSize(R.dimen.dp_24)
        val arrow = AppCompatResources.getDrawable(this@PayoutAndContactActivity, R.mipmap.mine_right)?.apply {
            setBounds(0, 0, arrowSize, arrowSize)
        }
        methodSelectionView.setCompoundDrawablesRelative(icon, null, arrow, null)
        methodSelectionView.text = getString(
            if (method == WithdrawMethod.BANK) R.string.bank else R.string.e_wallet,
        )
        binding.showBankFields = method == WithdrawMethod.BANK
        binding.showWalletFields = method == WithdrawMethod.WALLET
    }

    private fun clearWithdrawMethodSelection() = with(binding.withdrawAccountForm) {
        selectedWithdrawMethod = null
        tvWithdrawMethodError.isVisible = false
        val arrowSize = resources.getDimensionPixelSize(R.dimen.dp_24)
        val arrow = AppCompatResources.getDrawable(this@PayoutAndContactActivity, R.mipmap.mine_right)?.apply {
            setBounds(0, 0, arrowSize, arrowSize)
        }
        methodSelectionView.setCompoundDrawablesRelative(null, null, arrow, null)
        methodSelectionView.text = getString(R.string.please_select)
        binding.showBankFields = false
        binding.showWalletFields = false
    }

    private fun confirmPayoutSetupExit() {
        if (shouldShowBottomAction) {
            val step =
                authConfigList.size - max(0, authConfigList.indexOf("BANK"))
            showConfirmDialog(
                desc = String.format(
                    getString(R.string.auth_exit_confirm),
                    step.toString()
                ),
                cancel = getString(R.string.give_up),
                ok = getString(R.string.continue_str),
                highLight = step.toString(),
                cancelAction = {
                    vm.submitTrackingEvent(
                        TrackBean(
                            p = PageInfoBank,
                            act = ACT_clickBack
                        )
                    )
                    finish()
                }
            ) {
                vm.submitTrackingEvent(
                    TrackBean(
                        p = PageInfoBank,
                        act = ACT_clickContinue
                    )
                )
            }
        } else {
            finish()
        }
    }

    private fun submit() {
        vm.submitTrackingEvent(TrackBean(p = PageInfoBank, act = ACT_clickNext))
        trackEvent(SUPPLEMENTARY_INFO_COMMIT)
        val isWallet = selectedWithdrawMethod == WithdrawMethod.WALLET
        val contactEntries = arrayListOf(
            RelativesBean(
                relativesStatus,
                binding.relativesNameView.getText(),
                binding.relativesPhoneView.getText(),
            ),
            RelativesBean(
                friendStatus,
                binding.friendNameView.getText(),
                binding.friendPhoneView.getText(),
            ),
        )
        if (
            additionalContactStatus != null &&
            binding.additionalContactNameView.getText().isNotBlank() &&
            binding.additionalContactPhoneView.getText().isNotBlank()
        ) {
            contactEntries += RelativesBean(
                additionalContactStatus,
                binding.additionalContactNameView.getText(),
                binding.additionalContactPhoneView.getText(),
            )
        }
        vm.submitBankAndCtsInfo(
            ApiRequest(
                bankInfoId = if (isWallet) null else bankBean?.countryId?.toString(),
                bankId = if (isWallet) null else bankBean?.id?.toString(),
                accountUser = binding.withdrawAccountForm.holderView.getText(),
                bankNo = if (isWallet) null else binding.withdrawAccountForm.bankAccountView.getText(),
                bankCode = if (isWallet) null else bankBean?.bankCode,
                bankName = if (isWallet) null else bankBean?.bankName,
                payWay = if (isWallet) "WALLET" else "CARD",
                walletId = if (isWallet) walletBean?.id else null,
                accountCode = if (isWallet) binding.withdrawAccountForm.walletAccountView.getText().trim() else null,
                relativesInfoVOList = contactEntries,
            )
        )
    }

    private var bankBean: BankChannelResponse? = null
    private var walletBean: WalletResponse? = null
    private var relativesStatus: Int? = null
    private var friendStatus: Int? = null
    private var additionalContactStatus: Int? = null
    override fun initObserve() = with(vm) {
        super.initObserve()
        accountVm.payChannelList.observe(this@PayoutAndContactActivity) {
            val channelList = it ?: arrayListOf()
            chooseBankDialog(
                channelList
            ) { bean ->
                selectWithdrawMethod(WithdrawMethod.BANK)
                binding.withdrawAccountForm.bankView.setText(bean.bankName)
                binding.withdrawAccountForm.bankView.hideError()
                bankBean = bean
            }
        }
        accountVm.walletList.observe(this@PayoutAndContactActivity) {
            val walletItems = it ?: arrayListOf()
            chooseWalletDialog(walletItems) { wallet ->
                selectWithdrawMethod(WithdrawMethod.WALLET)
                binding.withdrawAccountForm.walletProviderView.setText(wallet.walletName)
                binding.withdrawAccountForm.walletProviderView.hideError()
                walletBean = wallet
                fillWalletAccountFromLoginPhone()
            }
        }
        contractResult.observe(this@PayoutAndContactActivity) {
            binding.apply {
                loadingLayout.showContent()
                setBottomActionVisible(!isCert)
                it?.let {
                    relativesStatus = it.relatives
                    friendStatus = it.otherRelatives
                    relativesView.setText(it.relativesStr)
                    relativesNameView.setText(it.relativesName)
                    relativesPhoneView.setText(it.relativesMobile)
                    friendView.setText(it.otherRelativesStr)
                    friendNameView.setText(it.otherName)
                    friendPhoneView.setText(it.otherMobile)
                    withdrawAccountForm.holderView.setText(it.accountUser)
                }
            }
        }
        submitBankAndCtsResult.observe(this@PayoutAndContactActivity) {
            homeVm.getUserAuthStatus()
        }
        personalVm.personalResult.observe(this@PayoutAndContactActivity) {
            binding.withdrawAccountForm.holderView.setText(it?.firstName)
        }
        homeVm.userAuthStatusResult.observe(this@PayoutAndContactActivity) {
            it?.routeToNextAuthStep(this@PayoutAndContactActivity)
            finish()
        }
    }

    private fun fillWalletAccountFromLoginPhone() {
        with(binding.withdrawAccountForm) {
            if (walletAccountView.getText().isNotBlank()) return
            val phone = loginInfo?.phone.orEmpty()
            if (phone.isBlank()) return
            val walletAccount = if (phone.startsWith('0')) phone else "0$phone"
            walletAccountView.setText(walletAccount)
            confirmWalletAccountView.setText(walletAccount)
        }
    }

    private var contactPickTarget = ContactPickTarget.PRIMARY

    @SuppressLint("Range")
    private val pickContactLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                result.data?.data?.let {
                    it.getContactInfo { name, number ->
                        when (contactPickTarget) {
                            ContactPickTarget.PRIMARY -> {
                                binding.relativesNameView.setText(name)
                                binding.relativesPhoneView.setText(number)
                                vm.submitTrackingEvent(
                                    TrackBean(
                                        p = PageInfoBank,
                                        act = ACT_selectContactName1End,
                                        result = System.currentTimeMillis().toString(),
                                    ),
                                )
                            }
                            ContactPickTarget.SECONDARY -> {
                                binding.friendNameView.setText(name)
                                binding.friendPhoneView.setText(number)
                                vm.submitTrackingEvent(
                                    TrackBean(
                                        p = PageInfoBank,
                                        act = ACT_selectContactName2End,
                                        result = System.currentTimeMillis().toString(),
                                    ),
                                )
                            }
                            ContactPickTarget.ADDITIONAL -> {
                                binding.additionalContactNameView.setText(name)
                                binding.additionalContactPhoneView.setText(number)
                                vm.submitTrackingEvent(
                                    TrackBean(
                                        p = PageInfoBank,
                                        act = ACT_selectContactName3End,
                                        result = System.currentTimeMillis().toString(),
                                    ),
                                )
                            }
                        }
                    }
                }
            }
        }

    fun pickContact() {
        val intent = Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI)
        intent.type = ContactsContract.CommonDataKinds.Phone.CONTENT_TYPE
        pickContactLauncher.launch(intent)
    }
}
