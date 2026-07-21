package com.vaycore.finance.ui.navigation

import android.content.Context
import android.content.Intent
import com.vaycore.finance.data.local.activityUrl
import com.vaycore.finance.data.local.isLogin
import com.vaycore.finance.ui.activities.MainActivity
import com.vaycore.finance.ui.sidepage.PortalActivity

/** Resolves and opens the correct home experience for the current session. */
object MainNavigator {

    private const val EXTRA_PAGE = "page"
    private const val EXTRA_IS_FROM_AUTH = "isFromAuth"

    fun launch(
        context: Context,
        page: Int = 0,
        isFromAuth: Boolean = false,
        clearTask: Boolean = false,
    ) {
        val target = when (resolveDestination()) {
            MainDestination.PORTAL -> PortalActivity::class.java
            MainDestination.MAIN -> MainActivity::class.java
        }
        val intent = Intent(context, target).apply {
            putExtra(EXTRA_PAGE, page)
            putExtra(EXTRA_IS_FROM_AUTH, isFromAuth)
            flags = if (clearTask) {
                Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            } else {
                Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
        }
        context.startActivity(intent)
    }

    internal fun resolveDestination(): MainDestination {
        if (!isLogin) return MainDestination.PORTAL
        return if (activityUrl.isNotBlank()) MainDestination.PORTAL else MainDestination.MAIN
    }
}

internal enum class MainDestination {
    PORTAL,
    MAIN,
}
