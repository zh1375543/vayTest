package com.vaycore.finance.ui.sidepage.adapter

import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.vaycore.finance.databinding.ItemRecordPhotoBinding
import com.vaycore.finance.ui.extension.loadImage
import com.vaycore.finance.ui.extension.singleClick

class RecordPhotoAdapter(
    private val onDelete: ((Uri) -> Unit)? = null,
    private val onPhotoClick: ((Uri) -> Unit)? = null,
) : RecyclerView.Adapter<RecordPhotoAdapter.PhotoViewHolder>() {

    private val items = mutableListOf<Uri>()

    fun submitItems(newItems: List<Uri>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PhotoViewHolder {
        return PhotoViewHolder(
            ItemRecordPhotoBinding.inflate(LayoutInflater.from(parent.context), parent, false),
        )
    }

    override fun onBindViewHolder(holder: PhotoViewHolder, position: Int) {
        val photo = items[position]
        holder.binding.ivPhoto.loadImage(photo)
        holder.binding.ivDelete.isVisible = onDelete != null
        holder.binding.ivDelete.singleClick { onDelete?.invoke(photo) }
        holder.binding.root.singleClick { onPhotoClick?.invoke(photo) }
    }

    override fun getItemCount(): Int = items.size

    class PhotoViewHolder(val binding: ItemRecordPhotoBinding) :
        RecyclerView.ViewHolder(binding.root)
}
