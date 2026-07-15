package com.vaycore.finance.util

import android.view.LayoutInflater
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.viewbinding.ViewBinding
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

/** Activity ViewBinding property delegate */
inline fun <reified VB : ViewBinding> AppCompatActivity.viewBinding(
    crossinline inflate: (LayoutInflater) -> VB
) = lazy {
    inflate(layoutInflater).also { setContentView(it.root) }
}

/** Fragment ViewBinding property delegate */
fun <VB : ViewBinding> Fragment.viewBinding(
    bind: (View) -> VB
) = FragmentViewBindingDelegate(bind)

class FragmentViewBindingDelegate<VB : ViewBinding>(
    private val bind: (View) -> VB
) : ReadOnlyProperty<Fragment, VB> {
    private var binding: VB? = null

    override fun getValue(thisRef: Fragment, property: KProperty<*>): VB {
        binding?.let { return it }

        val lifecycle = thisRef.viewLifecycleOwner.lifecycle
        if (!lifecycle.currentState.isAtLeast(Lifecycle.State.INITIALIZED)) {
            throw IllegalStateException("Should not attempt to get bindings when Fragment views are destroyed.")
        }

        thisRef.viewLifecycleOwner.lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onDestroy(owner: LifecycleOwner) {
                binding = null
            }
        })

        return bind(thisRef.requireView()).also { binding = it }
    }
}
