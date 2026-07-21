package com.vaycore.finance.ui.binding

import androidx.databinding.BindingAdapter
import androidx.recyclerview.widget.RecyclerView

/** Adapter contract used by XML Data Binding to submit RecyclerView items. */
interface BindableItemsAdapter {
    fun submitBindingItems(items: List<*>?)
}

/** Submits observable list data without coupling a layout to a concrete adapter type. */
@BindingAdapter("items")
fun RecyclerView.bindItems(items: List<*>?) {
    (adapter as? BindableItemsAdapter)?.submitBindingItems(items)
}
