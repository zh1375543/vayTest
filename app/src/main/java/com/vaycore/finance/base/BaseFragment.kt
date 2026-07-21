package com.vaycore.finance.base

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.databinding.ViewDataBinding
import androidx.viewbinding.ViewBinding

import androidx.annotation.LayoutRes

abstract class BaseFragment<VB : ViewBinding>(@LayoutRes layoutId: Int) : Fragment(layoutId) {

    protected abstract val binding: VB

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupDataBindingLifecycle()
        initView()
        initObserve()
    }

    /** Uses the view lifecycle so bindings stop observing after the Fragment view is destroyed. */
    private fun setupDataBindingLifecycle() {
        (binding as? ViewDataBinding)?.lifecycleOwner = viewLifecycleOwner
    }

    abstract fun initView()

    abstract fun initObserve()
}
