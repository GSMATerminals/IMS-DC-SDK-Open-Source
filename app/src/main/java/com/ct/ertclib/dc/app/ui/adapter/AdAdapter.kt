/*
 * Copyright 2025-China Telecom Research Institute.
 * All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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