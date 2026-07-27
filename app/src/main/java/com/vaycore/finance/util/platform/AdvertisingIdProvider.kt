package com.vaycore.finance.util.platform

import android.content.Context
import com.google.android.gms.ads.identifier.AdvertisingIdClient

object AdvertisingIdProvider {

    fun get(context: Context): String = runCatching {
        AdvertisingIdClient.getAdvertisingIdInfo(context).id.orEmpty()
    }.getOrDefault("")
}
