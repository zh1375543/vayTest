package com.vaycore.finance.util.platform

import android.content.Context
import android.graphics.Typeface

object AppFontInstaller {

    fun install(context: Context, mediumFontAsset: String, boldFontAsset: String) {
        val medium = Typeface.createFromAsset(context.assets, mediumFontAsset)
        val bold = Typeface.createFromAsset(context.assets, boldFontAsset)

        replaceTypefaceField("DEFAULT", medium)
        replaceTypefaceField("DEFAULT_BOLD", bold)

        runCatching {
            Typeface::class.java.getDeclaredField("sDefaults").apply {
                isAccessible = true
                set(null, arrayOf(medium, bold, Typeface.SANS_SERIF, Typeface.SERIF, Typeface.MONOSPACE))
            }

            @Suppress("UNCHECKED_CAST")
            val systemFontMap = Typeface::class.java.getDeclaredField("sSystemFontMap").run {
                isAccessible = true
                get(null) as MutableMap<String, Typeface>
            }
            systemFontMap["sans-serif"] = medium
            systemFontMap["sans-serif-medium"] = medium
            systemFontMap["sans-serif-bold"] = bold
            systemFontMap["sans-serif-light"] = medium
            systemFontMap["sans-serif-condensed"] = medium
        }
    }

    private fun replaceTypefaceField(fieldName: String, typeface: Typeface) {
        runCatching {
            Typeface::class.java.getDeclaredField(fieldName).apply {
                isAccessible = true
                set(null, typeface)
            }
        }
    }
}
