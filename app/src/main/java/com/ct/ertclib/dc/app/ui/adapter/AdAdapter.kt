package com.ct.ertclib.dc.app.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.ct.ertclib.dc.app.databinding.ItemAdBinding
import com.ct.ertclib.dc.core.data.model.AdItem

class AdAdapter(
    private val onItemClick: (AdItem) -> Unit
) : RecyclerView.Adapter<AdAdapter.BannerViewHolder>() {

    private var items: List<AdItem> = emptyList()

    fun submitData(newItems: List<AdItem>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BannerViewHolder {
        val binding = ItemAdBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return BannerViewHolder(binding, onItemClick)
    }

    override fun onBindViewHolder(holder: BannerViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount() = items.size

    class BannerViewHolder(
        private val binding: ItemAdBinding,
        private val onItemClick: (AdItem) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(adItem: AdItem) {
            // 使用 Glide 加载图片，ShapeableImageView 自动处理圆角
            Glide.with(binding.root.context)
                .load(adItem.imageUrl)
                .placeholder(android.R.drawable.ic_menu_gallery)
                .error(android.R.drawable.ic_menu_report_image)
                .transition(DrawableTransitionOptions.withCrossFade())
                .into(binding.adImage)

            // 设置点击事件
            binding.root.setOnClickListener {
                onItemClick(adItem)
            }
        }
    }
}