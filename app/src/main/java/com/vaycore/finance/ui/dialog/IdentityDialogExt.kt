package com.vaycore.finance.ui

import android.content.Context
import com.vaycore.finance.base.BaseDialog
import com.vaycore.finance.base.BaseSheetDialog
import com.vaycore.finance.data.bean.SelectionOption
import com.vaycore.finance.databinding.AddressDialogBinding
import com.vaycore.finance.databinding.DatePickDialogBinding
import com.vaycore.finance.databinding.KycCardExampleDialogBinding
import com.vaycore.finance.databinding.KycSelfExampleDialogBinding
import com.vaycore.finance.databinding.PickDialogBinding
import com.vaycore.finance.ui.extension.singleClick
import com.vaycore.finance.identity.PersonalInfoViewModel

fun Context.showKycCardExampleDialog(
) {
    object : BaseDialog<KycCardExampleDialogBinding>(
        this,
        KycCardExampleDialogBinding::inflate,
    ) {
        override fun initView() = with(binding) {
            super.initView()
            tvOk.singleClick { dismiss() }
        }
    }.show()
}

fun Context.showKycSelfieExampleDialog(
) {
    object : BaseDialog<KycSelfExampleDialogBinding>(
        this,
        KycSelfExampleDialogBinding::inflate,
    ) {
        override fun initView() = with(binding) {
            super.initView()
            tvOk.singleClick { dismiss() }
        }
    }.show()
}

fun Context.showOptionPickerDialog(
    defPosition: Int,
    list: List<SelectionOption>?,
    action: (position: Int) -> Unit,
) {
    object : BaseSheetDialog<PickDialogBinding>(this, PickDialogBinding::inflate) {
        override fun initView() = with(binding) {
            super.initView()
            wheelView.apply {
                setData(list.orEmpty())
                setDefaultSelected(defPosition)
            }
            tvOk.singleClick {
                dismiss()
                action(wheelView.getSelectedPosition())
            }
        }
    }.show()
}

fun Context.showDatePickerDialog(
    action: (dateStr: String) -> Unit,
) {
    object :
        BaseSheetDialog<DatePickDialogBinding>(this, DatePickDialogBinding::inflate) {
        override fun initView() = with(binding) {
            super.initView()
            root.setOnClickListener { dismiss() }
            tvOk.singleClick {
                dismiss()
                action(dateView.getDateString())
            }
        }
    }.show()
}

fun Context.showAddressPickerDialog(
    vm: PersonalInfoViewModel,
    action: (address: String, provinceId: Long?, cityId: Long?, areaId: Long?) -> Unit,
) {
    AddressPickerDialog(this, vm, action).show()
}

private class AddressPickerDialog(
    context: Context,
    private val viewModel: PersonalInfoViewModel,
    private val onAddressSelected: (
        address: String,
        provinceId: Long?,
        cityId: Long?,
        areaId: Long?,
    ) -> Unit,
) : BaseSheetDialog<AddressDialogBinding>(context, AddressDialogBinding::inflate) {

    private enum class AddressLevel {
        PROVINCE,
        CITY,
        AREA,
    }

    private var provinceId: Long? = null
    private var cityId: Long? = null
    private var areaId: Long? = null
    private var activeTab = AddressLevel.PROVINCE

    override fun initView() {
        super.initView()
        bindLevelSelection()
        bindConfirmAction()
        setOnShowListener { loadAddressLevel(AddressLevel.PROVINCE) }
    }

    private fun loadAddressLevel(level: AddressLevel) = with(binding) {
        activeTab = level
        wheelView.setOnSelectListener(null)

        val parentId = when (level) {
            AddressLevel.PROVINCE -> null
            AddressLevel.CITY -> provinceId.toString()
            AddressLevel.AREA -> cityId.toString()
        }
        viewModel.getAddressList(parentId) { addressList ->
            if (activeTab != level) return@getAddressList

            val applySelection: (SelectionOption) -> Unit = { item ->
                when (level) {
                    AddressLevel.PROVINCE -> {
                        tvProvince.text = item.info
                        provinceId = item.id.toLong()
                    }

                    AddressLevel.CITY -> {
                        tvCity.text = item.info
                        cityId = item.id.toLong()
                    }

                    AddressLevel.AREA -> {
                        tvArea.text = item.info
                        areaId = item.id.toLong()
                    }
                }
            }
            wheelView.setData(addressList)
            wheelView.setOnSelectListener { _, item -> applySelection(item) }
            addressList.firstOrNull()?.let { defaultItem ->
                applySelection(defaultItem)
                wheelView.setDefaultSelected(0)
            }
        }
    }

    private fun bindLevelSelection() = with(binding) {
        tvProvince.singleClick {
            tvProvince.text = ""
            provinceId = null
            tvCity.text = ""
            cityId = null
            tvArea.text = ""
            areaId = null
            loadAddressLevel(AddressLevel.PROVINCE)
        }

        tvCity.singleClick {
            tvCity.text = ""
            cityId = null
            tvArea.text = ""
            areaId = null
            loadAddressLevel(AddressLevel.CITY)
        }

        tvArea.singleClick {
            tvArea.text = ""
            areaId = null
            loadAddressLevel(AddressLevel.AREA)
        }
    }

    private fun bindConfirmAction() = with(binding) {
        tvOk.singleClick {
            if (tvProvince.text.isNullOrBlank() || provinceId == null) {
                loadAddressLevel(AddressLevel.PROVINCE)
                return@singleClick
            }
            if (tvCity.text.isNullOrBlank() || cityId == null) {
                loadAddressLevel(AddressLevel.CITY)
                return@singleClick
            }
            if (tvArea.text.isNullOrBlank() || areaId == null) {
                loadAddressLevel(AddressLevel.AREA)
                return@singleClick
            }

            dismiss()
            onAddressSelected(
                "${tvProvince.text}/${tvCity.text}/${tvArea.text}",
                provinceId,
                cityId,
                areaId,
            )
        }
    }
}
