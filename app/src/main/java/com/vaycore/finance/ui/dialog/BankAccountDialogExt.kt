package com.vaycore.finance.ui

import android.content.Context
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import com.vaycore.finance.R
import com.vaycore.finance.base.BaseDialog
import com.vaycore.finance.base.BaseSheetDialog
import com.vaycore.finance.data.local.bean.BankAccountResponse
import com.vaycore.finance.data.local.bean.BankChannelResponse
import com.vaycore.finance.data.local.bean.WalletResponse
import com.vaycore.finance.databinding.BankCardErrorDialogBinding
import com.vaycore.finance.databinding.ChooseAccountsDialogBinding
import com.vaycore.finance.databinding.ChooseBankDialogBinding
import com.vaycore.finance.databinding.ChooseWalletDialogBinding
import com.vaycore.finance.databinding.WithdrawMethodDialogBinding
import com.vaycore.finance.ui.activities.BankCardAddActivity
import com.vaycore.finance.ui.adapters.ChooseAccountsDialogAdapter
import com.vaycore.finance.ui.adapters.ChooseBankDialogAdapter
import com.vaycore.finance.ui.adapters.ChooseWalletDialogAdapter
import com.vaycore.finance.ui.extension.hideKeyboard
import com.vaycore.finance.ui.extension.singleClick
import com.vaycore.finance.util.start
import java.text.Normalizer
import java.util.Locale
import kotlin.math.max

fun Context.showBankCardErrorDialog(
    desc: String = getString(R.string.card_error_tips),
    cancel: String = getString(R.string.already_edited),
    ok: String = getString(R.string.revise),
    cancelAction: () -> Unit = {},
    okAction: () -> Unit,
) {
    object : BaseDialog<BankCardErrorDialogBinding>(
        this,
        BankCardErrorDialogBinding::inflate,
    ) {
        override fun initView() = with(binding) {
            super.initView()
            tvDesc.text = desc
            btnClose.text = cancel
            btnSure.text = ok
            btnClose.singleClick {
                dismiss()
                cancelAction()
            }
            btnSure.singleClick {
                dismiss()
                okAction()
            }
        }
    }.show()
}


fun Context.chooseAccountsDialog(
    cardNo: String?,
    list: List<BankAccountResponse>,
    isRepay: Boolean = false,
    title: String = getString(R.string.choose_account),
    applyAction: (info: BankAccountResponse) -> Unit,
) {
    object : BaseDialog<ChooseAccountsDialogBinding>(this, ChooseAccountsDialogBinding::inflate) {
        override fun initView() = with(binding) {
            tvTitle.text = title
            tvAdd.isVisible = !isRepay
            val currentAccountIndex = list.indexOfFirst { it.bankNo == cardNo }
            val defaultAccountIndex = list.indexOfFirst { it.isDefault == 1 }
            val index = max(0, if (currentAccountIndex >= 0) currentAccountIndex else defaultAccountIndex)
            val itemHeight = resources.getDimensionPixelSize(R.dimen.dp_66)
            val visibleItemCount = list.size.coerceIn(2, 4)
            rvCard.layoutParams = rvCard.layoutParams.apply {
                height = itemHeight * visibleItemCount
            }
            val adapter =
                ChooseAccountsDialogAdapter(index).apply {
                    submitItems(list)
                    setOnItemClickListener { _, index ->
                        selectPosition = index
                        notifyItemRangeChanged(0, itemCount, 0)
                    }
                }
            rvCard.adapter = adapter
            rvCard.post { rvCard.scrollToPosition(index) }
            tvAdd.singleClick {
                dismiss()
                start<BankCardAddActivity>()
            }
            BtnApply.singleClick {
                dismiss()
                applyAction.invoke(adapter.items[adapter.selectPosition])
            }
        }
    }.show()
}

fun Context.chooseBankDialog(list: List<BankChannelResponse>, chooseAction: (BankChannelResponse) -> Unit) {
    object : BaseSheetDialog<ChooseBankDialogBinding>(this, ChooseBankDialogBinding::inflate) {
        override fun initView() = with(binding) {
            super.initView()
            val adapter = ChooseBankDialogAdapter().apply {
                setOnItemClickListener { item, _ ->
                    chooseAction.invoke(item)
                    dismiss()
                }
            }
            adapter.submitItems(list)
            tvEmpty.isVisible = false
            rvBank.adapter = adapter
            etSearch.doAfterTextChanged { text ->
                val newList = filterBankChannels(list, text?.toString().orEmpty())
                adapter.submitItems(newList)
                tvEmpty.isVisible = newList.isEmpty()
                rvBank.isVisible = newList.isNotEmpty()
            }
            root.setOnClickListener {
                etSearch.hideKeyboard()
            }
            tvEmpty.setOnClickListener { etSearch.hideKeyboard() }
        }
    }.show()
}

fun Context.chooseWalletDialog(list: List<WalletResponse>, chooseAction: (WalletResponse) -> Unit) {
    object : BaseSheetDialog<ChooseWalletDialogBinding>(this, ChooseWalletDialogBinding::inflate) {
        override fun initView() = with(binding) {
            super.initView()
            val adapter = ChooseWalletDialogAdapter().apply {
                setOnItemClickListener { item, _ ->
                    chooseAction(item)
                    dismiss()
                }
            }
            adapter.submitItems(list)
            tvEmpty.isVisible = false
            rvWallet.adapter = adapter
            etSearch.doAfterTextChanged { text ->
                val filteredItems = filterWallets(list, text?.toString().orEmpty())
                adapter.submitItems(filteredItems)
                tvEmpty.isVisible = filteredItems.isEmpty()
                rvWallet.isVisible = filteredItems.isNotEmpty()
            }
            root.setOnClickListener { etSearch.hideKeyboard() }
            tvEmpty.setOnClickListener { etSearch.hideKeyboard() }
        }
    }.show()
}

fun Context.showWithdrawMethodDialog(
    walletAction: () -> Unit,
    bankAction: () -> Unit,
) {
    object : BaseDialog<WithdrawMethodDialogBinding>(
        this,
        WithdrawMethodDialogBinding::inflate,
    ) {
        override fun initView() = with(binding) {
            super.initView()
            cancelView.singleClick { dismiss() }
            eWalletOptionView.singleClick {
                dismiss()
                walletAction()
            }
            bankOptionView.singleClick {
                dismiss()
                bankAction()
            }
        }
    }.show()
}

private fun filterBankChannels(
    list: List<BankChannelResponse>,
    keyword: String,
): List<BankChannelResponse> {
    val query = keyword.normalizeSearchKeyword()
    if (query.isBlank()) return list

    return list.mapNotNull { item ->
        val longCode = item.longCode.normalizeSearchKeyword()
        val bankName = item.bankName.normalizeSearchKeyword()
        val rank = when {
            longCode == query || bankName == query -> 0
            longCode.startsWith(query) || bankName.startsWith(query) -> 1
            longCode.contains(query) || bankName.contains(query) -> 2
            else -> return@mapNotNull null
        }
        rank to item
    }.sortedBy { it.first }
        .map { it.second }
}

private fun filterWallets(
    list: List<WalletResponse>,
    keyword: String,
): List<WalletResponse> {
    val query = keyword.normalizeSearchKeyword()
    if (query.isBlank()) return list

    return list.mapNotNull { item ->
        val searchableFields = listOf(
            item.walletName,
            item.walletCode,
            item.walletType,
            item.walletDesc,
        ).map { it.normalizeSearchKeyword() }

        val rank = when {
            searchableFields.any { it == query } -> 0
            searchableFields.any { it.startsWith(query) } -> 1
            searchableFields.any { it.contains(query) } -> 2
            else -> return@mapNotNull null
        }
        rank to item
    }.sortedBy { it.first }
        .map { it.second }
}

private fun String?.normalizeSearchKeyword(): String {
    val value = this?.trim().orEmpty()
    if (value.isBlank()) return ""
    return Normalizer.normalize(value, Normalizer.Form.NFD)
        .replace("\\p{Mn}+".toRegex(), "")
        .lowercase(Locale.ROOT)
}
