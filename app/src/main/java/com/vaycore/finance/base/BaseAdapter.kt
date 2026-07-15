package com.vaycore.finance.base

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding

abstract class BaseAdapter<T, VB : ViewBinding>(
    private val bindingInflater: (LayoutInflater, ViewGroup?, Boolean) -> VB,
) : RecyclerView.Adapter<BaseAdapter.BaseViewHolder<T, VB>>() {

    val items = mutableListOf<T>()
    lateinit var context: Context

    private val interactions = InteractionDispatcher<T>()

    // RecyclerView lifecycle
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder<T, VB> {
        val binding = bindingInflater(LayoutInflater.from(parent.context), parent, false)
        context = parent.context
        return BaseViewHolder(
            binding = binding,
            onItemClick = interactions::dispatchItemClick,
            onItemLongClick = interactions::dispatchItemLongClick,
        )
    }

    override fun onBindViewHolder(holder: BaseViewHolder<T, VB>, position: Int) {
        val item = items[position]

        bindItem(holder.binding, item, position)
        holder.updateBoundItem(item, position)
        bindChildClickListeners(holder.binding, item, position)
    }

    override fun getItemCount(): Int = items.size

    // Subclass binding hooks
    /** Subclasses implement item binding here. */
    abstract fun bindItem(binding: VB, item: T, position: Int)

    /** Override to set child view click listeners. */
    open fun bindChildClickListeners(binding: VB, item: T, position: Int) = Unit

    // Child click dispatch
    protected fun dispatchChildClick(view: View, item: T, position: Int) {
        interactions.dispatchChildClick(view, item, position)
    }

    // Data mutation APIs
    fun setItems(newItems: List<T>) {
        val oldSize = items.size
        replaceItems(newItems)
        notifyReplacement(oldSize, newItems.size)
    }

    fun submitItems(lists: List<T>?, callback: DiffUtil.ItemCallback<T>? = null) {
        val newItems = lists ?: emptyList()
        if (callback == null) {
            setItems(newItems)
            return
        }

        if (isDataSame(newItems, callback)) return

        val oldItems = items.toList()
        when {
            oldItems.isEmpty() && newItems.isNotEmpty() -> {
                replaceItems(newItems)
                notifyItemRangeInserted(0, newItems.size)
            }

            oldItems.isNotEmpty() && newItems.isEmpty() -> {
                replaceItems(newItems)
                notifyItemRangeRemoved(0, oldItems.size)
            }

            else -> applyDiff(oldItems, newItems, callback)
        }
    }

    fun addItems(newItems: List<T>) {
        if (newItems.isEmpty()) return

        val startPosition = items.size
        items.addAll(newItems)
        notifyItemRangeInserted(startPosition, newItems.size)
    }

    fun addItem(item: T) {
        items.add(item)
        notifyItemInserted(items.lastIndex)
    }

    fun addItem(position: Int, item: T) {
        items.add(position, item)
        notifyItemInserted(position)
    }

    fun removeItem(position: Int) {
        if (position !in items.indices) return

        items.removeAt(position)
        notifyItemRemoved(position)
    }

    fun removeItem(item: T) {
        removeItem(items.indexOf(item))
    }

    fun updateItem(position: Int, item: T) {
        if (position !in items.indices) return

        items[position] = item
        notifyItemChanged(position)
    }

    fun clearItems() {
        val size = items.size
        if (size == 0) return

        items.clear()
        notifyItemRangeRemoved(0, size)
    }

    fun getItem(position: Int): T? = items.getOrNull(position)

    // Listener configuration
    fun setOnItemClickListener(listener: (T, Int) -> Unit) {
        interactions.itemClickListener = listener
    }

    fun setOnItemLongClickListener(listener: (T, Int) -> Boolean) {
        interactions.itemLongClickListener = listener
    }

    fun setOnChildClickListener(listener: (View, T, Int) -> Unit) {
        interactions.childClickListener = listener
    }

    // Data update internals
    private fun replaceItems(newItems: List<T>) {
        items.clear()
        items.addAll(newItems)
    }

    private fun notifyReplacement(oldSize: Int, newSize: Int) {
        when {
            oldSize == 0 && newSize > 0 -> notifyItemRangeInserted(0, newSize)
            oldSize > 0 && newSize == 0 -> notifyItemRangeRemoved(0, oldSize)
            oldSize > 0 -> notifyDataSetChanged()
        }
    }

    private fun isDataSame(newItems: List<T>, callback: DiffUtil.ItemCallback<T>): Boolean {
        if (items.size != newItems.size) return false

        return items.indices.all { index ->
            callback.areItemsTheSame(items[index]!!, newItems[index]!!) &&
                callback.areContentsTheSame(items[index]!!, newItems[index]!!)
        }
    }

    private fun applyDiff(
        oldItems: List<T>,
        newItems: List<T>,
        callback: DiffUtil.ItemCallback<T>,
    ) {
        val diffResult = DiffUtil.calculateDiff(AdapterDiffCallback(oldItems, newItems, callback))
        replaceItems(newItems)
        diffResult.dispatchUpdatesTo(this)
    }

    class BaseViewHolder<T, VB : ViewBinding>(
        val binding: VB,
        private val onItemClick: (T, Int) -> Unit,
        private val onItemLongClick: (T, Int) -> Boolean,
    ) : RecyclerView.ViewHolder(binding.root) {

        private var boundItem: BoundItem<T>? = null

        init {
            binding.root.setOnClickListener {
                boundItem?.let { onItemClick(it.item, it.position) }
            }
            binding.root.setOnLongClickListener {
                boundItem?.let { onItemLongClick(it.item, it.position) } ?: false
            }
        }

        fun updateBoundItem(item: T, position: Int) {
            boundItem = BoundItem(item, position)
        }
    }

    private data class BoundItem<T>(
        val item: T,
        val position: Int,
    )

    private class InteractionDispatcher<T> {
        var itemClickListener: ((T, Int) -> Unit)? = null
        var itemLongClickListener: ((T, Int) -> Boolean)? = null
        var childClickListener: ((View, T, Int) -> Unit)? = null

        fun dispatchItemClick(item: T, position: Int) {
            itemClickListener?.invoke(item, position)
        }

        fun dispatchItemLongClick(item: T, position: Int): Boolean {
            return itemLongClickListener?.invoke(item, position) ?: false
        }

        fun dispatchChildClick(view: View, item: T, position: Int) {
            childClickListener?.invoke(view, item, position)
        }
    }

    private class AdapterDiffCallback<T>(
        private val oldItems: List<T>,
        private val newItems: List<T>,
        private val itemCallback: DiffUtil.ItemCallback<T>,
    ) : DiffUtil.Callback() {

        override fun getOldListSize(): Int = oldItems.size

        override fun getNewListSize(): Int = newItems.size

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return itemCallback.areItemsTheSame(
                oldItems[oldItemPosition]!!,
                newItems[newItemPosition]!!,
            )
        }

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return itemCallback.areContentsTheSame(
                oldItems[oldItemPosition]!!,
                newItems[newItemPosition]!!,
            )
        }
    }
}

class QuickAdapter<T, VB : ViewBinding>(
    bindingInflater: (LayoutInflater, ViewGroup?, Boolean) -> VB,
    private val itemBinder: (VB, T, Int) -> Unit,
) : BaseAdapter<T, VB>(bindingInflater) {
    override fun bindItem(binding: VB, item: T, position: Int) {
        itemBinder(binding, item, position)
    }
}

fun <T, VB : ViewBinding> createAdapter(
    bindingInflater: (LayoutInflater, ViewGroup?, Boolean) -> VB,
    bind: VB.(item: T, position: Int) -> Unit,
): QuickAdapter<T, VB> {
    return QuickAdapter(bindingInflater) { binding, item, position ->
        binding.bind(item, position)
    }
}
