package com.vaycore.finance.ui.activities.mine

import androidx.activity.viewModels
import com.vaycore.finance.R
import com.vaycore.finance.base.BaseActivity
import com.vaycore.finance.databinding.AuthCenterActivityBinding
import com.vaycore.finance.data.local.authConfigList
import com.vaycore.finance.data.local.bean.AuthOptionResponse
import com.vaycore.finance.data.local.bean.UserAuthStatusResponse
import com.vaycore.finance.ui.activities.auth.BankAccountAuthActivity
import com.vaycore.finance.ui.activities.auth.CarrierAuthActivity
import com.vaycore.finance.ui.activities.auth.KycAuthActivity
import com.vaycore.finance.ui.activities.auth.PersonalInfoAuthActivity
import com.vaycore.finance.ui.adapters.AuthEntryAdapter
import com.vaycore.finance.ui.viewmodels.DashboardViewModel
import com.vaycore.finance.util.PROFILE_PAGE
import com.vaycore.finance.util.start
import com.vaycore.finance.util.trackEvent
import com.vaycore.finance.util.viewBinding

class AuthCenterActivity : BaseActivity<AuthCenterActivityBinding>() {

    override val binding by viewBinding(AuthCenterActivityBinding::inflate)

    private val vm by viewModels<DashboardViewModel>()

    private val authAdapter by lazy {
        AuthEntryAdapter().apply {
            setOnItemClickListener { item, _ ->
                when (if (item.isCertified) item.title else items.first { !it.isCertified }.title) {
                    getString(R.string.kyc_certification) -> {
                        context.start<KycAuthActivity> {
                            putExtra("isCert", item.isCertified)
                        }
                    }

                    getString(R.string.personal_info) -> {
                        context.start<PersonalInfoAuthActivity> {
                            putExtra("isCert", item.isCertified)
                        }
                    }

                    getString(R.string.service_provider) -> {
                        context.start<CarrierAuthActivity> {
                            putExtra("isCert", item.isCertified)
                        }
                    }

                    else -> {
                        context.start<BankAccountAuthActivity> {
                            putExtra("isCert", item.isCertified)
                        }
                    }
                }
            }
        }
    }

    override fun initView() = with(binding) {
        trackEvent(PROFILE_PAGE)
        rvAuth.adapter = authAdapter
        loadingLayout.showLoading()
        loadingLayout.setOnRetryClickListener {
            loadingLayout.showLoading()
            vm.getUserAuthStatus {
                loadingLayout.showError()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        vm.getUserAuthStatus {
            binding.loadingLayout.showError()
        }
    }

    override fun initObserve() = with(vm) {
        super.initObserve()
        userAuthStatusResult.observe(this@AuthCenterActivity) {
            it?.let { auth ->
                binding.loadingLayout.showContent()
                val list = authConfigList.map { type ->
                    val (titleRes, iconRes, checker) =
                        authMap[type.uppercase()]
                            ?: authMap.getValue("TELECOM")

                    AuthOptionResponse(
                        title = getString(titleRes),
                        isCertified = checker(auth),
                        src = iconRes
                    )
                }.toMutableList()
                authAdapter.submitItems(list)
            }
        }
    }

    private val authMap by lazy {
        mapOf(
            "KYC" to Triple(
                R.string.kyc_certification,
                R.mipmap.ic_cert_kyc
            ) { bean: UserAuthStatusResponse ->
                bean.kycState == "30"
            },
            "ID" to Triple(
                R.string.personal_info,
                R.mipmap.ic_cert_personal
            ) { bean ->
                bean.idState == "30"
            },
            "BANK" to Triple(
                R.string.contact_info,
                R.mipmap.ic_cert_submit_infor
            ) { bean ->
                bean.bankCardState == "30"
            },
            "TELECOM" to Triple(
                R.string.service_provider,
                R.mipmap.ic_cert_service
            ) { bean ->
                bean.telecomPermissionState == "30"
            }
        )
    }
}
