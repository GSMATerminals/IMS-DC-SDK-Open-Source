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

package com.ct.ertclib.dc.feature.testing.netdc.ui.adapter

import com.ct.ertclib.dc.feature.testing.R
import com.ct.ertclib.dc.feature.testing.databinding.ItemRecentCallBinding
import com.ct.ertclib.dc.net.data.RecentCall

class RecentCallsAdapter(
    private val onItemClick: (String,String) -> Unit
) : androidx.recyclerview.widget.RecyclerView.Adapter<RecentCallsAdapter.ViewHolder>() {

    private val calls = mutableListOf<RecentCall>()
    class ViewHolder(val binding: ItemRecentCallBinding) :
        androidx.recyclerview.widget.RecyclerView.ViewHolder(binding.root)

    fun updateCalls(newCalls: MutableList<RecentCall>) {
        calls.clear()
        calls.addAll(newCalls)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemRecentCallBinding.inflate(
            android.view.LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val call = calls[position]
        holder.binding.apply {
            tvName.text = call.name
            tvPhoneNumber.text = call.phoneNumber
            tvTime.text = call.time

            val iconRes = if (call.isOutgoing)
                R.drawable.ic_call_made else R.drawable.ic_call_received
            ivCallType.setImageResource(iconRes)
            if(call.isVideoCall == true){
                btnVideoCall.visibility = android.view.View.VISIBLE
                btnCall.visibility = android.view.View.GONE
            } else {
                btnVideoCall.visibility = android.view.View.GONE
                btnCall.visibility = android.view.View.VISIBLE
            }
            val type = if(call.isVideoCall == true) "video" else "voice"
            root.setOnClickListener { onItemClick(call.phoneNumber,type) }
        }
    }

    override fun getItemCount() = calls.size
}