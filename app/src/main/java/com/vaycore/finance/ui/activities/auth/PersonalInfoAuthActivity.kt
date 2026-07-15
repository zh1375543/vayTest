package com.vaycore.finance.ui.activities.auth

import android.Manifest
import androidx.activity.viewModels
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.core.widget.doOnTextChanged
import androidx.lifecycle.lifecycleScope
import com.vaycore.finance.R
import com.vaycore.finance.App
import com.vaycore.finance.base.BaseActivity
import com.vaycore.finance.data.PageInfoPersonal
import com.vaycore.finance.data.PagePrivacy
import com.vaycore.finance.data.local.bean.ApiRequest
import com.vaycore.finance.data.local.bean.TrackBean
import com.vaycore.finance.databinding.PersonalInfoAuthActivityBinding
import com.vaycore.finance.data.ACT_clickNext
import com.vaycore.finance.data.ACT_in
import com.vaycore.finance.data.ACT_inputAddressEnd
import com.vaycore.finance.data.ACT_inputAddressStart
import com.vaycore.finance.data.ACT_inputEducationEnd
import com.vaycore.finance.data.ACT_inputEducationStart
import com.vaycore.finance.data.ACT_inputIDCardNumberEnd
import com.vaycore.finance.data.ACT_inputIDCardNumberStart
import com.vaycore.finance.data.ACT_inputMaritalStateEnd
import com.vaycore.finance.data.ACT_inputMaritalStateStart
import com.vaycore.finance.data.ACT_inputNameEnd
import com.vaycore.finance.data.ACT_inputNameStart
import com.vaycore.finance.data.ACT_inputSalaryEnd
import com.vaycore.finance.data.ACT_inputSalaryStart
import com.vaycore.finance.data.ACT_inputZaloEnd
import com.vaycore.finance.data.ACT_inputZaloStart
import com.vaycore.finance.data.ACT_selectDateEnd
import com.vaycore.finance.data.ACT_selectDateStart
import com.vaycore.finance.data.local.authConfigList
import com.vaycore.finance.util.PERSON_INFO_COMMIT
import com.vaycore.finance.util.PERSON_INFO_PAGE
import com.vaycore.finance.util.toYmdDateString
import com.vaycore.finance.util.toDmyDateString
import com.vaycore.finance.util.isAdult
import com.vaycore.finance.util.isIdCardValid
import com.vaycore.finance.ui.extension.resetScale
import com.vaycore.finance.util.requestRuntimePermissions
import com.vaycore.finance.util.showToastMessage
import com.vaycore.finance.ui.extension.singleClick
import com.vaycore.finance.util.trackEvent
import com.vaycore.finance.ui.showAddressPickerDialog
import com.vaycore.finance.ui.showConfirmDialog
import com.vaycore.finance.ui.showDatePickerDialog
import com.vaycore.finance.ui.showOptionPickerDialog
import com.vaycore.finance.ui.viewmodels.PersonalInfoViewModel
import com.vaycore.finance.ui.viewmodels.DashboardViewModel
import com.vaycore.finance.util.deviceRiskPermissions
import com.vaycore.finance.util.viewBinding
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.toString

class PersonalInfoAuthActivity :
    BaseActivity<PersonalInfoAuthActivityBinding>() {

    override val binding by viewBinding(PersonalInfoAuthActivityBinding::inflate)
    private val isCert by lazy {
        intent.getBooleanExtra("isCert", false)
    }
    private val vm by viewModels<PersonalInfoViewModel>()
    private val homeVm by viewModels<DashboardViewModel>()
    private val debounceTime = 500L  // treat as input finished after 500ms idle
    private var shouldShowBottomAction = false
    private var isKeyboardVisible = false

    private var startSalaryTime: Long = 0L
    private var salaryJob: Job? = null
    private var startNameTime: Long = 0L
    private var nameJob: Job? = null
    private var startZaloTime: Long = 0L
    private var zaloJob: Job? = null
    private var startIDTime: Long = 0L
    private var idJob: Job? = null

    override fun initView() = with(binding) {
        setupBottomActionKeyboardBehavior()
        trackEvent(PERSON_INFO_PAGE)
        vm.recordEvent(
            TrackBean(
                p = PageInfoPersonal,
                act = ACT_in,
            )
        )
        titleBar.setNavigationAction { handleBack() }
        onBackAction(vm) {
            handleBack()
        }
        titleBar.setAction(
            "${authConfigList.indexOf("ID") + 1}/${authConfigList.size}"
        )
        if (isCert) {
            authProgressView.isVisible = false
        } else {
            authProgressView.bind(
                requiredTypes = authConfigList.filterNot { it.isBlank() },
                currentType = "ID",
                title = getString(R.string.kyc_title),
                desc = getString(R.string.auth_personal_desc),
            )
        }
        nameView.getEditText().doOnTextChanged { _, _, _, _ ->
            val now = System.currentTimeMillis()
            // 1. first input → record start time
            if (startNameTime == 0L) {
                startNameTime = now
                vm.recordEvent(
                    TrackBean(
                        p = PageInfoPersonal,
                        act = ACT_inputNameStart,
                        result = now.toString()
                    )
                )
            }
            // 2. typing → reset end timer
            nameJob?.cancel()
            nameJob = lifecycleScope.launch {
                delay(debounceTime)
                // 3. user stopped typing → record end time
                vm.recordEvent(
                    TrackBean(
                        p = PageInfoPersonal,
                        act = ACT_inputNameEnd,
                        result = System.currentTimeMillis().toString()
                    )
                )
            }
        }
        idCardView.getEditText().doOnTextChanged { _, _, _, _ ->
            val now = System.currentTimeMillis()
            // 1. first input → record start time
            if (startIDTime == 0L) {
                startIDTime = now
                vm.recordEvent(
                    TrackBean(
                        p = PageInfoPersonal,
                        act = ACT_inputIDCardNumberStart,
                        result = now.toString()
                    )
                )
            }
            // 2. typing → reset end timer
            idJob?.cancel()
            idJob = lifecycleScope.launch {
                delay(debounceTime)
                // 3. user stopped typing → record end time
                vm.recordEvent(
                    TrackBean(
                        p = PageInfoPersonal,
                        act = ACT_inputIDCardNumberEnd,
                        result = System.currentTimeMillis().toString()
                    )
                )
            }
        }
        zaloView.getEditText().doOnTextChanged { _, _, _, _ ->
            val now = System.currentTimeMillis()
            // 1. first input → record start time
            if (startZaloTime == 0L) {
                startZaloTime = now
                vm.recordEvent(
                    TrackBean(
                        p = PageInfoPersonal,
                        act = ACT_inputZaloStart,
                        result = now.toString()
                    )
                )
            }
            // 2. typing → reset end timer
            zaloJob?.cancel()
            zaloJob = lifecycleScope.launch {
                delay(debounceTime)
                // 3. user stopped typing → record end time
                vm.recordEvent(
                    TrackBean(
                        p = PageInfoPersonal,
                        act = ACT_inputZaloEnd,
                        result = System.currentTimeMillis().toString()
                    )
                )
            }
        }
        monthlyView.getEditText().doAfterTextChanged {
            it?.let {
                var input = it.toString()
                // remove leading zeros, but keep "0" itself
                if (input.length > 1 && input.startsWith("0") && !input.startsWith("0.")) {
                    input = input.replaceFirst("^0+".toRegex(), "")
                    if (input.isEmpty()) input = "0"
                    monthlyView.setText(input)
                    monthlyView.getEditText().setSelection(input.length)
                }
                monthlyView.hideError()
            }
        }
        monthlyView.getEditText().doOnTextChanged { _, _, _, _ ->
            val now = System.currentTimeMillis()
            // 1. first input → record start time
            if (startSalaryTime == 0L) {
                startSalaryTime = now
                vm.recordEvent(
                    TrackBean(
                        p = PageInfoPersonal,
                        act = ACT_inputSalaryStart,
                        result = now.toString()
                    )
                )
            }
            // 2. typing → reset end timer
            salaryJob?.cancel()
            salaryJob = lifecycleScope.launch {
                delay(debounceTime)
                // 3. user stopped typing → record end time
                vm.recordEvent(
                    TrackBean(
                        p = PageInfoPersonal,
                        act = ACT_inputSalaryEnd,
                        result = System.currentTimeMillis().toString()
                    )
                )
            }
        }
        genderView.setOnClick {
            vm.getEnums {
                val genderList = it.gender ?: arrayListOf()
                showOptionPickerDialog(
                    genderList.indexOfFirst { it1 -> it1.info == genderView.getText() },
                    genderList
                ) { index ->
                    genderView.setText(genderList[index].info)
                    genderView.hideError()
                    genderStatus = it.gender?.get(index)?.state
                }
            }
        }
        birthView.setOnClick {
            vm.recordEvent(
                TrackBean(
                    p = PageInfoPersonal,
                    act = ACT_selectDateStart,
                    result = System.currentTimeMillis().toString()
                )
            )
            showDatePickerDialog { dateStr ->
                birthView.setText(dateStr)
                birthView.hideError()
                vm.recordEvent(
                    TrackBean(
                        p = PageInfoPersonal,
                        act = ACT_selectDateEnd,
                        result = System.currentTimeMillis().toString()
                    )
                )
            }
        }
        educationView.setOnClick {
            vm.recordEvent(
                TrackBean(
                    p = PageInfoPersonal,
                    act = ACT_inputEducationStart,
                    result = System.currentTimeMillis().toString()
                )
            )
            vm.getEnums {
                val genderList = it.education ?: arrayListOf()
                showOptionPickerDialog(
                    genderList.indexOfFirst { it1 -> educationView.getText() == it1.info },
                    genderList
                ) { index ->
                    educationView.setText(genderList[index].info)
                    educationView.hideError()
                    eduStatus = it.education?.get(index)?.state
                    vm.recordEvent(
                        TrackBean(
                            p = PageInfoPersonal,
                            act = ACT_inputEducationEnd,
                            result = System.currentTimeMillis().toString()
                        )
                    )
                }
            }
        }
        marryView.setOnClick {
            vm.recordEvent(
                TrackBean(
                    p = PageInfoPersonal,
                    act = ACT_inputMaritalStateStart,
                    result = System.currentTimeMillis().toString()
                )
            )
            vm.getEnums {
                val genderList = it.maritalStatus ?: arrayListOf()
                showOptionPickerDialog(
                    genderList.indexOfFirst { it1 -> it1.info == marryView.getText() },
                    genderList
                ) { index ->
                    marryView.setText(genderList[index].info)
                    marryView.hideError()
                    marStatus = it.maritalStatus?.get(index)?.state
                    vm.recordEvent(
                        TrackBean(
                            p = PageInfoPersonal,
                            act = ACT_inputMaritalStateEnd,
                            result = System.currentTimeMillis().toString()
                        )
                    )
                }
            }
        }
        provinceView.setOnClick {
            vm.recordEvent(
                TrackBean(
                    p = PageInfoPersonal,
                    act = ACT_inputAddressStart,
                    result = System.currentTimeMillis().toString()
                )
            )
            provinceId = null
            cityId = null
            areaId = null
            provinceView.setText("")
            showAddressPickerDialog(vm) { it, pId, cId, aId ->
                provinceView.setText(it)
                provinceView.hideError()
                provinceId = pId
                cityId = cId
                areaId = aId
//                LogUtil.d("pId$pId|$cityId|$areaId")
                vm.recordEvent(
                    TrackBean(
                        p = PageInfoPersonal,
                        act = ACT_inputAddressEnd,
                        result = System.currentTimeMillis().toString()
                    )
                )
            }
        }
        zaloView.setEnableEdit(!isCert)
        nameView.setEnableEdit(!isCert)
        genderView.setEnableEdit(!isCert)
        birthView.setEnableEdit(!isCert)
        idCardView.setEnableEdit(!isCert)
        educationView.setEnableEdit(!isCert)
        marryView.setEnableEdit(!isCert)
        provinceView.setEnableEdit(!isCert)
        addressView.setEnableEdit(!isCert)
        titleBar.showAction(!isCert)
        setBottomActionVisible(!isCert)
        if (bottomActionLayout.isVisible) {
            btNext.resetScale()
        }
        btNext.singleClick {
            if (nameView.getText().isBlank()) {
                nameView.showError()
                scrollView.scrollTo(0, nameView.top)
                return@singleClick
            }
            if (genderView.getText().isBlank()) {
                genderView.showError()
                scrollView.scrollTo(0, genderView.top)
                return@singleClick
            }
            if (birthView.getText().isBlank() || !birthView.getText().isAdult()) {
                birthView.showError()
                scrollView.scrollTo(0, birthView.top)
                if (!birthView.getText().isAdult()) {
                    getString(R.string.under_18).showToastMessage()
                }
                return@singleClick
            }
            if (!idCardView.getText().isIdCardValid()) {
                getString(R.string.id_number_error).showToastMessage()
                idCardView.showError()
                scrollView.scrollTo(0, idCardView.top)
                return@singleClick
            }
            if (educationView.getText().isBlank()) {
                educationView.showError()
                scrollView.scrollTo(0, educationView.top)
                return@singleClick
            }
            if (monthlyView.getText().isBlank()) {
                monthlyView.showError()
                scrollView.scrollTo(0, monthlyView.top)
                return@singleClick
            }
            if (marryView.getText().isBlank()) {
                marryView.showError()
                scrollView.scrollTo(0, marryView.top)
                return@singleClick
            }
            if (zaloView.getText().isBlank()) {
                zaloView.showError()
                scrollView.scrollTo(0, zaloView.top)
                return@singleClick
            }
            if (provinceId == null || cityId == null || areaId == null) {
                provinceView.showError()
                scrollView.scrollTo(0, provinceView.top)
                return@singleClick
            }
            if (addressView.getText().isBlank()) {
                addressView.showError()
                scrollView.scrollTo(0, addressView.top)
                return@singleClick
            }
            requestRuntimePermissions(
                deviceRiskPermissions, refuseAction = { _, pList ->
                    vm.recordEvent(pList.map { it1 ->
                        TrackBean(
                            p = PagePrivacy,
                            act = when (it1?.permissionName) {
                                Manifest.permission.ACCESS_COARSE_LOCATION -> "gps"
                                Manifest.permission.READ_PHONE_STATE -> "device"
                                Manifest.permission.READ_SMS -> "sms"
                                Manifest.permission.READ_CALL_LOG -> "callRecords"
                                else -> "notification"
                            },
                            result = "reject"
                        )
                    })
                }) {
                vm.recordEvent(
                    TrackBean(
                        p = PageInfoPersonal,
                        act = ACT_clickNext,
                    )
                )
                vm.recordEvent(it.map { it1 ->
                    TrackBean(
                        p = PagePrivacy,
                        act = when (it1?.permissionName) {
                            Manifest.permission.ACCESS_COARSE_LOCATION -> "gps"
                            Manifest.permission.READ_PHONE_STATE -> "device"
                            Manifest.permission.READ_SMS -> "sms"
                            Manifest.permission.READ_CALL_LOG -> "callRecords"
                            else -> "notification"
                        },
                        result = "agree"
                    )
                })
                requestRuntimePermissions(deviceRiskPermissions) {
                    App.appViewModel.postRiskInfo(PageInfoPersonal) { isSuccess ->
                        if (isSuccess) {
                            submit()
                        }
                    }
                }
            }
        }
        loadingLayout.setOnRetryClickListener {
            loadingLayout.showLoading()
            vm.getPersonalInfo {
                loadingLayout.showError()
            }
        }
        loadingLayout.showLoading()
        vm.getPersonalInfo {
            loadingLayout.showError()
        }
    }

    private fun submit() {
        trackEvent(PERSON_INFO_COMMIT)
        vm.submitPersonalInfo(
            ApiRequest(
                education = eduStatus.toString(),
                sex = genderStatus.toString(),
                marryState = marStatus.toString(),
                userName = binding.nameView.getText(),
                cardNo = binding.idCardView.getText(),
                birthDate = binding.birthView.getText().toYmdDateString(),
                province = provinceId.toString(),
                address = binding.addressView.getText(),
                region = areaId.toString(),
                city = cityId.toString(),
                zaloAccount = binding.zaloView.getText(),
                salary = binding.monthlyView.getText()
//                                userCommunicationRecordStr = Gson().toJson(getCallLog()).encodeBase64()
            )
        )
    }

    private var genderStatus: Int? = null
    private var eduStatus: Int? = null
    private var marStatus: Int? = null
    private var provinceId: Long? = null
    private var cityId: Long? = null
    private var areaId: Long? = null

    override fun initObserve() = with(vm) {
        super.initObserve()
        personalResult.observe(this@PersonalInfoAuthActivity) {
            binding.apply {
                loadingLayout.showContent()
                it?.let {
                    nameView.setText(it.firstName)
                    idCardView.setText(it.cardNo)
                    genderView.setText(it.sexStr)
                    birthView.setText(it.birthDateStr?.toDmyDateString())
                    educationView.setText(it.educationStr)
                    marryView.setText(it.marryStateStr)
                    addressView.setText(it.currentAddress)
                    zaloView.setText(it.zaloAccount)
                    monthlyView.setText(if (it.salary == null) "" else it.salary.toString())
                    if (it.provinceStr != null) {
                        provinceView.setText(
                            String.format(
                                "%s/%s/%s", it.provinceStr, it.cityStr, it.regionStr
                            )
                        )
                    }
                    genderStatus = it.sex
                    eduStatus = it.education
                    marStatus = it.marryState
                    provinceId = it.province
                    cityId = it.city
                    areaId = it.region
                }
            }
        }
        homeVm.userAuthStatusResult.observe(this@PersonalInfoAuthActivity) {
            it?.routeToNextAuthStep(this@PersonalInfoAuthActivity)
            finish()
        }
        submitResult.observe(this@PersonalInfoAuthActivity) {
            homeVm.getUserAuthStatus()
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

    private fun handleBack() {
        if (shouldShowBottomAction) {
            val list = authConfigList.filterNot { it1 -> it1.isBlank() }
            val step =
                list.size - max(0, list.indexOf("ID"))
            showConfirmDialog(
                desc = String.format(
                    getString(R.string.auth_exit_confirm),
                    step.toString()
                ),
                cancel = getString(R.string.give_up),
                ok = getString(R.string.continue_str),
                highLight = step.toString(),
                cancelAction = { finish() }
            ) {}
        } else {
            finish()
        }
    }
}
