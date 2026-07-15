/*
 * Copyright (c) 2020. Dylan Cai
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.vaycore.finance.base

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.WindowManager
import androidx.viewbinding.ViewBinding
import com.vaycore.finance.R

abstract class BaseDialog<VB : ViewBinding>(
    private val context: Context,
    private val inflate: (LayoutInflater) -> VB,
    themeResId: Int = R.style.CustomDialog,
) : Dialog(context, themeResId) {

    val binding: VB by lazy { inflate(layoutInflater) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        val horizontalPadding = context.resources.getDimensionPixelSize(R.dimen.dp_20)
        window?.decorView?.setPadding(horizontalPadding, 0, horizontalPadding, 0)
        window?.attributes?.apply {
            width = WindowManager.LayoutParams.MATCH_PARENT
            height = WindowManager.LayoutParams.WRAP_CONTENT
            gravity = Gravity.CENTER
        }
        initView()
    }

    open fun initView() {}

    override fun show() {
        if (!canShowDialog()) return
        try {
            super.show()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun canShowDialog(): Boolean {
        val activity = context as? Activity ?: return false

        return when {
            activity.isFinishing -> false
            activity.isDestroyed -> false
            activity.window?.decorView?.windowToken == null -> false
            activity.window?.attributes?.token == null -> false
            else -> true
        }
    }
}
