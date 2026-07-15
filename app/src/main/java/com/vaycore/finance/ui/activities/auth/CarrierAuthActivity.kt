package com.vaycore.finance.ui.activities.auth

import androidx.activity.viewModels
import androidx.core.view.isVisible
import com.vaycore.finance.R
import com.vaycore.finance.ui.adapters.ServiceProviderAdapter
import com.vaycore.finance.base.BaseActivity
import com.vaycore.finance.databinding.CarrierAuthActivityBinding
import com.vaycore.finance.data.ACT_clickBack
import com.vaycore.finance.data.ACT_clickConfirm
import com.vaycore.finance.data.ACT_clickContinue
import com.vaycore.finance.data.ACT_in
import com.vaycore.finance.data.PageServiceProvider
import com.vaycore.finance.data.local.authConfigList
import com.vaycore.finance.data.local.bean.AuthOptionResponse
import com.vaycore.finance.data.local.bean.TrackBean
import com.vaycore.finance.data.local.loginInfo
import com.vaycore.finance.ui.extension.singleClick
import com.vaycore.finance.ui.showConfirmDialog
import com.vaycore.finance.ui.viewmodels.CarrierVerifyViewModel
import com.vaycore.finance.ui.viewmodels.PersonalInfoViewModel
import com.vaycore.finance.util.start
import com.vaycore.finance.util.viewBinding
import kotlin.math.max

class CarrierAuthActivity :
    BaseActivity<CarrierAuthActivityBinding>() {

    override val binding by viewBinding(CarrierAuthActivityBinding::inflate)
    private val isCert by lazy { intent.getBooleanExtra("isCert", false) }
    private val vm by viewModels<CarrierVerifyViewModel>()
    private val personalVm by viewModels<PersonalInfoViewModel>()

    private val serviceAdapter by lazy {
        ServiceProviderAdapter().apply {
            submitItems(
                arrayListOf(
                    AuthOptionResponse(src = R.mipmap.carrier_viettel, "viet", "Viet tel", isCertified = true),
                    AuthOptionResponse(src = R.mipmap.carrier_mobifone, "mobi", "MoBi Fone"),
                    AuthOptionResponse(src = R.mipmap.carrier_vinaphone, "vina", "Vina phone"),
                    AuthOptionResponse(src = R.mipmap.carrier_vietnamobile, "vietnamobile", "Vietna mobile"),
                    AuthOptionResponse(src = R.mipmap.carrier_saymee, "saymee", "Saymee"),
                )
            )
            setOnItemClickListener { _, position ->
                binding.btNext.isEnabled = true
                items.forEachIndexed { i, item -> item.isCertified = i == position }
                notifyItemRangeChanged(0, itemCount, 0)
            }
        }
    }

    override fun initView() = with(binding) {
        vm.recordEvent(
            TrackBean(
                p = PageServiceProvider,
                act = ACT_in
            )
        )
        titleBar.setNavigationAction { handleBack() }
        onBackAction(vm) {
            handleBack()
        }
        titleBar.showAction(!isCert)
        titleBar.setAction(
            "${authConfigList.indexOf("TELECOM") + 1}/${authConfigList.size}"
        )
        if (isCert) {
            authProgressView.isVisible = false
        } else {
            authProgressView.bind(
                requiredTypes = authConfigList.filterNot { it.isBlank() },
                currentType = "TELECOM",
                title = getString(R.string.kyc_title),
                desc = getString(R.string.auth_service_desc),
            )
        }
        rvService.adapter = serviceAdapter
        btNext.singleClick {
            vm.recordEvent(
                TrackBean(
                    p = PageServiceProvider,
                    act = ACT_clickConfirm
                )
            )
            start<CarrierOtpActivity> {
                putExtra("type", serviceAdapter.items.first { it.isCertified }.title)
            }
        }
        loadingLayout.showLoading()
        loadingLayout.setOnRetryClickListener {
            loadingLayout.showLoading()
            personalVm.getPersonalInfo {
                loadingLayout.showError()
            }
        }
        personalVm.getPersonalInfo {
            loadingLayout.showError()
        }
    }

    private fun handleBack() {
        if (!isCert) {
            val list = authConfigList.filterNot { it1 -> it1.isBlank() }
            val step =
                list.size - max(0, list.indexOf("TELECOM"))
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
                            p = PageServiceProvider,
                            act = ACT_clickBack
                        )
                    )
                    finish()
                }
            ) {
                vm.recordEvent(
                    TrackBean(
                        p = PageServiceProvider,
                        act = ACT_clickContinue
                    )
                )
            }
        } else {
            finish()
        }
    }

    //MOBI("mobi", "Mobifone"),
    //    VIET("viet", "Viettel"),
    //    VINA("vina", "Vinaphone"),
    //    SAYMEE("saymee", "Saymee"),
    //    VIETNAMOBILE("vietnamobile", "Vietnamobile"),
    override fun initObserve() =with(vm){
        super.initObserve()
        personalVm.personalResult.observe(this@CarrierAuthActivity) {
            binding.loadingLayout.showContent()
            if (!isCert) {
                binding.unAuthLayout.isVisible = true
                binding.authLayout.isVisible = false
                binding.bottomActionLayout.isVisible = true
            } else {
                binding.unAuthLayout.isVisible = false
                binding.authLayout.isVisible = true
                binding.bottomActionLayout.isVisible = false
                binding.tvPhone.text = "+${loginInfo?.phone ?: ""}"
                binding.ivIcon.setImageResource(
                    when (it?.telecom) {
                        "Viettel" -> R.mipmap.carrier_viettel_on
                        "Mobifone" -> R.mipmap.carrier_mobifone_on
                        "Vinaphone" -> R.mipmap.carrier_vinaphone_on
                        "Vietnamobile" -> R.mipmap.carrier_vietnamobile_on
                        else -> R.mipmap.carrier_saymee_on
                    }
                )
                binding.ivIcon.isVisible = !it?.telecom.isNullOrBlank()
            }
        }
    }
}
