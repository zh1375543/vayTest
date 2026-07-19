package com.vaycore.finance.ui.activities.auth

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
import com.vaycore.finance.App
import com.vaycore.finance.base.BaseActivity
import com.vaycore.finance.databinding.BankAccountAuthActivityBinding
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
import com.vaycore.finance.data.local.authConfigList
import com.vaycore.finance.data.local.loginInfo
import com.vaycore.finance.data.local.bean.ApiRequest
import com.vaycore.finance.data.local.bean.BankChannelResponse
import com.vaycore.finance.data.local.bean.RelativesBean
import com.vaycore.finance.data.local.bean.TrackBean
import com.vaycore.finance.data.local.bean.WalletResponse
import com.vaycore.finance.ui.viewmodels.WalletViewModel
import com.vaycore.finance.util.SUPPLEMENTARY_INFO_COMMIT
import com.vaycore.finance.ui.extension.resetScale
import com.vaycore.finance.util.requestRuntimePermissions
import com.vaycore.finance.ui.extension.singleClick
import com.vaycore.finance.util.trackEvent
import com.vaycore.finance.ui.chooseBankDialog
import com.vaycore.finance.ui.chooseWalletDialog
import com.vaycore.finance.ui.showConfirmDialog
import com.vaycore.finance.ui.showWithdrawMethodDialog
import com.vaycore.finance.ui.showOptionPickerDialog
import com.vaycore.finance.ui.viewmodels.PersonalInfoViewModel
import com.vaycore.finance.ui.viewmodels.WorkContactViewModel
import com.vaycore.finance.ui.viewmodels.DashboardViewModel
import com.vaycore.finance.util.getContactInfo
import com.vaycore.finance.util.deviceRiskPermissions
import com.vaycore.finance.util.viewBinding
import kotlin.math.max
import kotlin.toString

class BankAccountAuthActivity : BaseActivity<BankAccountAuthActivityBinding>() {

    private enum class WithdrawMethod {
        BANK,
        WALLET,
    }

    private enum class ContactPickTarget {
        PRIMARY,
        SECONDARY,
        ADDITIONAL,
    }

    override val binding by viewBinding(BankAccountAuthActivityBinding::inflate)

    private val vm by viewModels<WorkContactViewModel>()
    private val personalVm by viewModels<PersonalInfoViewModel>()
    private val homeVm by viewModels<DashboardViewModel>()
    private val accountVm by viewModels<WalletViewModel>()
    private val isCert by lazy { intent.getBooleanExtra("isCert", false) }
    private var shouldShowBottomAction = false
    private var isKeyboardVisible = false
    private var selectedWithdrawMethod: WithdrawMethod? = null

    override fun initView() = with(binding) {
        with(withdrawAccountForm) {
        setupBottomActionKeyboardBehavior()
        vm.recordEvent(TrackBean(p = PageInfoBank, act = ACT_in))
        titleBar.setNavigationAction { handleBack() }
        onBackAction(vm) {
            handleBack()
        }
        titleBar.setAction("${authConfigList.indexOf("BANK") + 1}/${authConfigList.size}")
        clearWithdrawMethodSelection()
        methodSelectionView.setOnClickListener {
            showWithdrawMethodDialog(
                walletAction = {
                    accountVm.getWalletList()
                },
                bankAction = {
                    accountVm.getPayChannelList()
                },
            )
        }
        setBottomActionVisible(false)
        bankView.setOnClick {
            accountVm.getPayChannelList()
        }
        walletProviderView.setOnClick {
            accountVm.getWalletList()
        }
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
        relativesView.setOnClick {
            vm.getContactEnum {
                val relativesList = it.relatives ?: arrayListOf()
                showOptionPickerDialog(
                    relativesList.indexOfFirst { it1 -> relativesView.getText() == it1.info },
                    relativesList
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
                    relativesList.indexOfFirst { it1 -> friendView.getText() == it1.info },
                    relativesList
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
            vm.recordEvent(
                TrackBean(
                    p = PageInfoBank,
                    act = ACT_selectContactName1Start,
                    result = System.currentTimeMillis().toString()
                )
            )
            contactPickTarget = ContactPickTarget.PRIMARY
            pickContact()
        }
        friendPhoneView.setContactClick {
            vm.recordEvent(
                TrackBean(
                    p = PageInfoBank,
                    act = ACT_selectContactName2Start,
                    result = System.currentTimeMillis().toString()
                )
            )
            contactPickTarget = ContactPickTarget.SECONDARY
            pickContact()
        }
        additionalContactPhoneView.setContactClick {
            vm.recordEvent(
                TrackBean(
                    p = PageInfoBank,
                    act = ACT_selectContactName3Start,
                    result = System.currentTimeMillis().toString(),
                ),
            )
            contactPickTarget = ContactPickTarget.ADDITIONAL
            pickContact()
        }
        btNext.singleClick {
            when (selectedWithdrawMethod) {
                WithdrawMethod.BANK -> {
                    if (bankView.getText().isBlank()) {
                        bankView.showError()
                        scrollView.scrollTo(0, bankView.top)
                        return@singleClick
                    }
                    if (holderView.getText().isBlank()) {
                        holderView.showError()
                        scrollView.scrollTo(0, holderView.top)
                        return@singleClick
                    }
                    if (bankAccountView.getText().isBlank()) {
                        bankAccountView.showError()
                        scrollView.scrollTo(0, bankAccountView.top)
                        return@singleClick
                    }
                    if (confirmBankView.getText() != bankAccountView.getText()) {
                        confirmBankView.showError()
                        scrollView.scrollTo(0, confirmBankView.top)
                        return@singleClick
                    }
                }
                WithdrawMethod.WALLET -> {
                    if (walletProviderView.getText().isBlank()) {
                        walletProviderView.showError()
                        scrollView.scrollTo(0, walletProviderView.top)
                        return@singleClick
                    }
                    if (walletAccountView.getText().isBlank()) {
                        walletAccountView.showError()
                        scrollView.scrollTo(0, walletAccountView.top)
                        return@singleClick
                    }
                    if (confirmWalletAccountView.getText() != walletAccountView.getText()) {
                        confirmWalletAccountView.showError()
                        scrollView.scrollTo(0, confirmWalletAccountView.top)
                        return@singleClick
                    }
                }
                null -> {
                    tvWithdrawMethodError.isVisible = true
                    methodSelectionView.performClick()
                    return@singleClick
                }
            }
            if (relativesView.getText().isBlank()) {
                relativesView.showError()
                scrollView.scrollTo(0, relativesView.top)
                return@singleClick
            }
            if (relativesNameView.getText().isBlank()) {
                relativesNameView.showError()
                scrollView.scrollTo(0, relativesNameView.top)
                return@singleClick
            }
            if (relativesPhoneView.getText().isBlank()) {
                relativesPhoneView.showError()
                scrollView.scrollTo(0, relativesPhoneView.top)
                return@singleClick
            }
            if (friendView.getText().isBlank()) {
                friendView.showError()
                scrollView.scrollTo(0, friendView.top)
                return@singleClick
            }
            if (friendNameView.getText().isBlank()) {
                friendNameView.showError()
                scrollView.scrollTo(0, friendNameView.top)
                return@singleClick
            }
            if (friendPhoneView.getText().isBlank()) {
                friendPhoneView.showError()
                scrollView.scrollTo(0, friendPhoneView.top)
                return@singleClick
            }
            requestRuntimePermissions(deviceRiskPermissions) {
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
            vm.getContactsInfo {
                loadingLayout.showError()
            }
        }
        loadingLayout.showLoading()
        vm.getContactsInfo {
            loadingLayout.showError()
        }
        personalVm.getPersonalInfo {}
        tvContactTips.isVisible = !isCert
        titleBar.showAction(!isCert)
        relativesView.setEnableEdit(!isCert)
        relativesNameView.setEnableEdit(!isCert)
        relativesPhoneView.setEnableEdit(!isCert)
        friendView.setEnableEdit(!isCert)
        friendNameView.setEnableEdit(!isCert)
        friendPhoneView.setEnableEdit(!isCert)
        additionalContactRelationshipView.setEnableEdit(!isCert)
        additionalContactNameView.setEnableEdit(!isCert)
        additionalContactPhoneView.setEnableEdit(!isCert)
        relativesPhoneView.setContactVisible(!isCert)
        friendPhoneView.setContactVisible(!isCert)
        additionalContactPhoneView.setContactVisible(!isCert)
        additionalContactContainer.isVisible = !isCert
        confirmLayout.isVisible = !isCert
        titleBar.updateTitle(
            if (isCert) getString(R.string.contact_info) else getString(R.string.bank_and_contact)
        )
            if (!isCert) {
                btNext.resetScale()
            }
        }
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
        val icon = AppCompatResources.getDrawable(this@BankAccountAuthActivity, iconRes)?.apply {
            setBounds(0, 0, iconSize, iconSize)
        }
        val arrowSize = resources.getDimensionPixelSize(R.dimen.dp_24)
        val arrow = AppCompatResources.getDrawable(this@BankAccountAuthActivity, R.mipmap.mine_right)?.apply {
            setBounds(0, 0, arrowSize, arrowSize)
        }
        methodSelectionView.setCompoundDrawablesRelative(icon, null, arrow, null)
        methodSelectionView.text = getString(
            if (method == WithdrawMethod.BANK) R.string.bank else R.string.e_wallet,
        )
        bankFieldsLayout.isVisible = method == WithdrawMethod.BANK
        walletFieldsLayout.isVisible = method == WithdrawMethod.WALLET
    }

    private fun clearWithdrawMethodSelection() = with(binding.withdrawAccountForm) {
        selectedWithdrawMethod = null
        tvWithdrawMethodError.isVisible = false
        val arrowSize = resources.getDimensionPixelSize(R.dimen.dp_24)
        val arrow = AppCompatResources.getDrawable(this@BankAccountAuthActivity, R.mipmap.mine_right)?.apply {
            setBounds(0, 0, arrowSize, arrowSize)
        }
        methodSelectionView.setCompoundDrawablesRelative(null, null, arrow, null)
        methodSelectionView.text = getString(R.string.please_select)
        bankFieldsLayout.isVisible = false
        walletFieldsLayout.isVisible = false
    }

    private fun handleBack() {
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
                    vm.recordEvent(
                        TrackBean(
                            p = PageInfoBank,
                            act = ACT_clickBack
                        )
                    )
                    finish()
                }
            ) {
                vm.recordEvent(
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
        vm.recordEvent(TrackBean(p = PageInfoBank, act = ACT_clickNext))
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
        accountVm.payChannelList.observe(this@BankAccountAuthActivity) {
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
        accountVm.walletList.observe(this@BankAccountAuthActivity) {
            val walletItems = it ?: arrayListOf()
            chooseWalletDialog(walletItems) { wallet ->
                selectWithdrawMethod(WithdrawMethod.WALLET)
                binding.withdrawAccountForm.walletProviderView.setText(wallet.walletName)
                binding.withdrawAccountForm.walletProviderView.hideError()
                walletBean = wallet
                fillWalletAccountFromLoginPhone()
            }
        }
        contractResult.observe(this@BankAccountAuthActivity) {
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
        submitBankAndCtsResult.observe(this@BankAccountAuthActivity) {
            homeVm.getUserAuthStatus()
        }
        personalVm.personalResult.observe(this@BankAccountAuthActivity) {
            binding.withdrawAccountForm.holderView.setText(it?.firstName)
        }
        homeVm.userAuthStatusResult.observe(this@BankAccountAuthActivity) {
            it?.routeToNextAuthStep(this@BankAccountAuthActivity)
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
                                vm.recordEvent(
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
                                vm.recordEvent(
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
                                vm.recordEvent(
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
