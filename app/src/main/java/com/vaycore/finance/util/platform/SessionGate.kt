package com.vaycore.finance.util.platform

import android.content.Context
import com.vaycore.finance.data.isLogin
import com.vaycore.finance.identity.LoginActivity
import com.vaycore.finance.util.start

fun Context.requireLogin(whenLoggedIn: () -> Unit) {
    if (isLogin) {
        whenLoggedIn()
    } else {
        start<LoginActivity>()
    }
}
