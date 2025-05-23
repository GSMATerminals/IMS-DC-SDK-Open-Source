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

package com.ct.ertclib.dc.core.ui.widget

import android.content.Context
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ct.ertclib.dc.core.utils.logger.Logger
import com.ct.ertclib.dc.core.R
import com.ct.ertclib.dc.core.data.miniapp.PermissionData
import com.ct.ertclib.dc.core.data.model.MiniAppInfo
import com.ct.ertclib.dc.core.miniapp.ui.adapter.PermissionListAdapter
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog

class PermissionBottomSheetDialog(
    private val context: Context,
    private val miniApp: MiniAppInfo,
    private val permissionDataList: MutableList<PermissionData>,
    private val onPositiveButtonClick: (List<PermissionData>) -> Unit,
    private val onNegativeButtonClick: () -> Unit
    ) : BottomSheetDialog(context, R.style.bottomSheetDialog) {

    companion object {
        private const val TAG = "PermissionBottomSheetDialog"
    }

    private var titleText: TextView?= null
    private var permissionRecyclerView: RecyclerView? = null
    private var okButton: Button? = null
    private var cancelButton: TextView? = null
    private val logger = Logger.getLogger(TAG)
    private var adapter: PermissionListAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initView()
    }

    fun reshow() {
        if (this.isShowing) {
            this.dismiss()
            initView()
            this.show()
        }
    }

    private fun initView() {
        setContentView(R.layout.permission_request_layout)
        titleText = findViewById(R.id.request_permission_panel_title)
        permissionRecyclerView = findViewById(R.id.permission_recycler_view)
        okButton = findViewById(R.id.permission_ok_button)
        cancelButton = findViewById(R.id.permission_cancel_button)

        adapter = PermissionListAdapter(context, permissionDataList, ::onPermissionSelectedClick)
        permissionRecyclerView?.layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
        permissionRecyclerView?.adapter = adapter
        setCanceledOnTouchOutside(false)

        getBehavior().apply {
            state = BottomSheetBehavior.STATE_EXPANDED
        }


        okButton?.setOnClickListener {
            logger.info("click ok")
            onPositiveButtonClick.invoke(permissionDataList)
            dismiss()
        }

        cancelButton?.setOnClickListener {
            logger.info("click cancel")
            onNegativeButtonClick.invoke()
            dismiss()
        }
        titleText?.text = context.resources.getString(R.string.permission_request_title, miniApp.appName)
    }

    private fun onPermissionSelectedClick(position: Int, isAllowed: Boolean) {
        logger.info("onPermissionSelectedClick, position: $position, isAllowed: $isAllowed")
        if (position < permissionDataList.size) {
            permissionDataList[position].willBeGranted = isAllowed
            adapter?.submitItem(permissionDataList[position], position)
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        onNegativeButtonClick.invoke()
    }

}