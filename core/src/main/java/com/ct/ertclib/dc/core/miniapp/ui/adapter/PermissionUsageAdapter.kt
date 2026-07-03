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

package com.ct.ertclib.dc.core.miniapp.ui.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.ct.ertclib.dc.core.R
import com.ct.ertclib.dc.core.data.miniapp.PermissionUsageData
import com.ct.ertclib.dc.core.miniapp.ui.viewholder.BaseViewHolder
import com.ct.ertclib.dc.core.miniapp.ui.viewholder.PermissionUsageViewHolder
import com.ct.ertclib.dc.core.utils.common.LogUtils

class PermissionUsageAdapter(
    private val context: Context,
    private var permissionUsageList: MutableList<PermissionUsageData>
) : RecyclerView.Adapter<BaseViewHolder>() {

    companion object {
        private const val TAG = "PermissionUsageAdapter"
        private const val ITEM_TYPE_NORMAL = 0
        private const val ITEM_TYPE_TAIL = 1
    }

    @SuppressLint("NotifyDataSetChanged")
    fun submitList(list: MutableList<PermissionUsageData>) {
        LogUtils.debug(TAG, "submitList")
        permissionUsageList.clear()
        permissionUsageList.addAll(list)
        this.notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder {
        LogUtils.debug(TAG, "onCreateViewHolder")
        if (viewType == ITEM_TYPE_NORMAL) {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.permission_usage_item, parent, false)
            return PermissionUsageViewHolder(view)
        } else {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.layout_item_recently_tips, parent, false)
            return BaseViewHolder(view)
        }
    }

    override fun getItemCount(): Int {
        return permissionUsageList.size + 1//增加列表尾部的tips
    }

    override fun onBindViewHolder(holder: BaseViewHolder, position: Int) {
        LogUtils.debug(TAG, "onBindViewHolder position: $position")
        if (getItemViewType(position) == ITEM_TYPE_NORMAL) {
            (holder as? PermissionUsageViewHolder)?.let {
                it.permissionTitle.text = permissionUsageList[position].permissionTitle
                it.permissionUsageTime.text = permissionUsageList[position].permissionUsageTime
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        return if (position == permissionUsageList.lastIndex + 1) {
            ITEM_TYPE_TAIL
        } else {
            ITEM_TYPE_NORMAL
        }
    }
}