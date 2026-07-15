package com.vaycore.finance.ui

import android.content.Context
import com.vaycore.finance.base.BaseDialog
import com.vaycore.finance.base.BaseSheetDialog
import com.vaycore.finance.data.local.bean.SelectionOption
import com.vaycore.finance.databinding.AddressDialogBinding
import com.vaycore.finance.databinding.DatePickDialogBinding
import com.vaycore.finance.databinding.KycCardExampleDialogBinding
import com.vaycore.finance.databinding.KycSelfExampleDialogBinding
import com.vaycore.finance.databinding.PickDialogBinding
import com.vaycore.finance.ui.extension.singleClick
import com.vaycore.finance.ui.viewmodels.PersonalInfoViewModel

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
    object : BaseSheetDialog<AddressDialogBinding>(this, AddressDialogBinding::inflate) {
        override fun initView() = with(binding) {
            super.initView()
            var provinceId: Long? = null
            var cityId: Long? = null
            var areaId: Long? = null

            // Currently active tab: 0=province, 1=city, 2=area
            var activeTab: Int

            fun loadProvinceWheel() {
                activeTab = 0
                wheelView.setOnSelectListener(null) //  Clear old listener first
                vm.getAddressList { provinceList ->
                    if (activeTab != 0) return@getAddressList //  Prevent stale async callback
                    wheelView.setData(provinceList)
                    wheelView.setOnSelectListener { _, item ->
                        tvProvince.text = item.info
                        provinceId = item.id.toLong()
                    }
                    if (provinceList.isNotEmpty()) {
                        tvProvince.text = provinceList[0].info
                        provinceId = provinceList[0].id.toLong()
                        wheelView.setDefaultSelected(0)
                    }
                }
            }

            fun loadCityWheel() {
                activeTab = 1
                wheelView.setOnSelectListener(null) //  Clear old listener first
                vm.getAddressList(provinceId.toString()) { cityList ->
                    if (activeTab != 1) return@getAddressList //  Prevent stale async callback
                    wheelView.setData(cityList)
                    wheelView.setOnSelectListener { _, item ->
                        tvCity.text = item.info
                        cityId = item.id.toLong()
                    }
                    if (cityList.isNotEmpty()) {
                        wheelView.setDefaultSelected(0)
                        tvCity.text = cityList[0].info
                        cityId = cityList[0].id.toLong()
                    }
                }
            }

            fun loadAreaWheel() {
                activeTab = 2
                wheelView.setOnSelectListener(null) //  Clear old listener first
                vm.getAddressList(cityId.toString()) { areaList ->
                    if (activeTab != 2) return@getAddressList //  Prevent stale async callback
                    wheelView.setData(areaList)
                    wheelView.setOnSelectListener { _, item ->
                        tvArea.text = item.info
                        areaId = item.id.toLong()
                    }
                    if (areaList.isNotEmpty()) {
                        wheelView.setDefaultSelected(0)
                        tvArea.text = areaList[0].info
                        areaId = areaList[0].id.toLong()
                    }
                }
            }

            setOnShowListener { loadProvinceWheel() }

            tvProvince.singleClick {
                tvProvince.text = ""
                provinceId = null
                tvCity.text = ""
                cityId = null
                tvArea.text = ""
                areaId = null
                loadProvinceWheel()
            }

            tvCity.singleClick {
                tvCity.text = ""
                cityId = null
                tvArea.text = ""
                areaId = null
                loadCityWheel()
            }

            tvArea.singleClick {
                tvArea.text = ""
                areaId = null
                loadAreaWheel()
            }

            tvOk.singleClick {
                if (tvProvince.text.isNullOrBlank() || provinceId == null) {
                    loadProvinceWheel(); return@singleClick
                }
                if (tvCity.text.isNullOrBlank() || cityId == null) {
                    loadCityWheel(); return@singleClick
                }
                if (tvArea.text.isNullOrBlank() || areaId == null) {
                    loadAreaWheel(); return@singleClick
                }
                dismiss()
                action(
                    "${tvProvince.text}/${tvCity.text}/${tvArea.text}",
                    provinceId, cityId, areaId
                )
            }
        }
    }.show()
}
