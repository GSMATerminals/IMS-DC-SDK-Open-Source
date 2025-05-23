/*
 *   Copyright 2025-China Telecom Research Institute.
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package com.ct.ertclib.dc.core.miniapp.ui.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.ct.ertclib.dc.core.utils.common.LogUtils
import com.ct.ertclib.dc.core.R
import com.ct.ertclib.dc.core.data.miniapp.PermissionData

class PermissionRequestAdapter (
    private val context: Context,
    private var permissionList: MutableList<PermissionData>
) : RecyclerView.Adapter<PermissionDeniedViewHolder>() {

    companion object {
        private const val TAG = "PermissionRequestAdapter"
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PermissionDeniedViewHolder {
        LogUtils.debug(TAG, "onCreateViewHolder")
        val view = LayoutInflater.from(parent.context).inflate(R.layout.permission_request_item_layout, parent, false)
        return PermissionDeniedViewHolder(view)
    }

    override fun getItemCount(): Int {
        return permissionList.size
    }

    override fun onBindViewHolder(holder: PermissionDeniedViewHolder, position: Int) {
        holder.permissionTitle.text = permissionList[position].permissionName
        holder.permissionDescription.text = permissionList[position].permissionDescription
    }
}

class PermissionDeniedViewHolder(view: View) : RecyclerView.ViewHolder(view) {
    val item: View = view
    val permissionTitle: TextView = itemView.findViewById(R.id.permission_request_item_title)
    val permissionDescription: TextView = itemView.findViewById(R.id.permission_request_item_description)
}