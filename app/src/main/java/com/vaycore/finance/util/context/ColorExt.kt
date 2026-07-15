package com.vaycore.finance.util.context

import android.content.Context
import androidx.annotation.ColorRes
import androidx.core.content.ContextCompat

fun Context.getColor2(@ColorRes id: Int): Int = ContextCompat.getColor(this, id)
