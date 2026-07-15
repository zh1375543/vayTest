package com.vaycore.finance.ui.activities.mine

import androidx.activity.viewModels
import com.vaycore.finance.base.BaseActivity
import com.vaycore.finance.databinding.ContactsActivityBinding
import com.vaycore.finance.data.local.bean.CustomerContactConfig
import com.vaycore.finance.ui.adapters.ContactWayAdapter
import com.vaycore.finance.ui.viewmodels.DashboardViewModel
import com.vaycore.finance.util.viewBinding

class ContactsActivity : BaseActivity<ContactsActivityBinding>() {

    override val binding by viewBinding(ContactsActivityBinding::inflate)
    private val vm by viewModels<DashboardViewModel>()

    private val contactAdapter by lazy { ContactWayAdapter() }

    override fun initView() = with(binding) {
        rvCustomer.adapter = contactAdapter
        vm.getUnAuthData()
    }

    override fun initObserve() = with(vm) {
        super.initObserve()
        unAuthResult.observe(this@ContactsActivity) {
            val list = mutableListOf<CustomerContactConfig>()
            it?.customerPhone?.let { phone ->
                list.add(
                    CustomerContactConfig(
                        enTitle = "Phone Number",
                        vernacularTitle = "Số điện thoại",
                        content = phone,
                        buttonType = 2
                    )
                )
            }
            it?.customerEmail?.let { email ->
                list.add(
                    CustomerContactConfig(
                        enTitle = "Email",
                        vernacularTitle = "Email",
                        content = email,
                        buttonType = 1
                    )
                )
            }
            it?.customerConfigs?.let { configs ->
                list.addAll(configs)
            }
            contactAdapter.submitItems(list)
        }
    }
}
