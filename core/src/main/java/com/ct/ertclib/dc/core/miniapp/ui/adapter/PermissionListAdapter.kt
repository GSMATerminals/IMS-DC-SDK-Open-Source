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

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.PopupWindow
import android.widget.TextView
import androidx.appcompat.content.res.AppCompatResources
import androidx.recyclerview.widget.RecyclerView
import com.ct.ertclib.dc.core.utils.logger.Logger
import com.ct.ertclib.dc.core.R
import com.ct.ertclib.dc.core.data.miniapp.PermissionData
import com.ct.ertclib.dc.core.miniapp.ui.viewholder.PermissionViewHolder
import com.ct.ertclib.dc.core.utils.common.ScreenUtils

class PermissionListAdapter(
    private val context: Context,
    private var permissionList: MutableList<PermissionData>,
    private val onPermissionSelectedClick: (Int, Boolean) -> Unit
) : RecyclerView.Adapter<PermissionViewHolder>() {

    companion object {
        private const val TAG = "PermissionListAdapter"
    }

    private val logger = Logger.getLogger(TAG)
    private var popupWindow: PopupWindow? = null

    @SuppressLint("NotifyDataSetChanged")
    fun submitList(newPermissionList: MutableList<PermissionData>) {
        logger.info("submitList")
        permissionList.clear()
        permissionList.addAll(newPermissionList)
        this.notifyDataSetChanged()
    }

    @SuppressLint("NotifyDataSetChanged")
    fun submitItem(permissionData: PermissionData, position: Int) {
        logger.info("submitList")
        if (position < permissionList.size) {
            permissionList[position] = permissionData
        } else {
            logger.warn("submitItem position bigger than size, error")
        }
        this.notifyItemChanged(position)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PermissionViewHolder {
        logger.info("onCreateViewHolder")
        val view = LayoutInflater.from(parent.context).inflate(R.layout.permission_item_layout, parent, false)
        return PermissionViewHolder(view)
    }

    override fun getItemCount(): Int {
        return permissionList.size
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onBindViewHolder(holder: PermissionViewHolder, position: Int) {
        logger.info("onBindViewHolder, position: $position")
        holder.permissionTitle.text = permissionList[position].permissionName
        holder.permissionDescription.text = permissionList[position].permissionDescription
        holder.permissionStatus.text = if (permissionList[position].willBeGranted) {
            context.resources.getString(R.string.permission_allowed)
        } else {
            context.resources.getString(R.string.permission_denied)
        }

        holder.item.setOnTouchListener { _, motionEvent ->
            if (motionEvent.action == MotionEvent.ACTION_DOWN) {
                val x = motionEvent.x
                val y = motionEvent.y
                logger.info("itemView click")
                showChoosePopupWindow(holder.item, permissionList[position].willBeGranted, position, x.toInt(), y.toInt())
                false
            } else {
                true
            }
        }
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        super.onDetachedFromRecyclerView(recyclerView)
        popupWindow = null
    }

    private fun showChoosePopupWindow(parentView: View, willBeGranted: Boolean, position: Int, xOff: Int = 0, yOff: Int = 0) {
        val view = LayoutInflater.from(context).inflate(R.layout.permission_choose_layout, null, false)
        val allowedText = view.findViewById<TextView>(R.id.permission_choose_allowed)
        val deniedText = view.findViewById<TextView>(R.id.permission_choose_denied)
        if (willBeGranted) {
            allowedText.setTextColor(context.resources.getColor(R.color.permission_blue))
        } else {
            deniedText.setTextColor(context.resources.getColor(R.color.permission_blue))
        }
        popupWindow = PopupWindow(
            view,
            context.resources.getDimensionPixelSize(R.dimen.pop_up_window_width),
            ViewGroup.LayoutParams.WRAP_CONTENT,
            true
        ).apply {
            isTouchable = true

            setBackgroundDrawable(
                AppCompatResources.getDrawable(
                    context,
                    R.drawable.pop_window_background
                )
            )
            val location = IntArray(2)
            parentView.getLocationOnScreen(location)
            showAsDropDown(parentView, calculateXOff(xOff), calculateYOff(location[1] + parentView.height, parentView.height - yOff))
        }

        allowedText.setOnClickListener {
            onPermissionSelectedClick.invoke(position, true)
            popupWindow?.dismiss()
        }
        deniedText.setOnClickListener {
            onPermissionSelectedClick.invoke(position, false)
            popupWindow?.dismiss()
        }
    }

    private fun calculateXOff(xOff: Int): Int {
        return xOff - context.resources.getDimensionPixelSize(R.dimen.pop_up_window_width) / 2
    }

    private fun calculateYOff(viewBottomY: Int, topOff: Int): Int {
        val screenHeight = ScreenUtils.getScreenHeight(context)
        val navigationBarHeight = ScreenUtils.getNavigationBarHeight(context)
        val activityWindowHeight = screenHeight - navigationBarHeight
        val popupWindowHeight = context.resources.getDimensionPixelSize(R.dimen.permission_pop_up_window_height)
        return if (viewBottomY >= activityWindowHeight) {
            -2 * popupWindowHeight
        } else if (viewBottomY + popupWindowHeight >= activityWindowHeight) {
            - popupWindowHeight - topOff
        } else {
            - topOff
        }
    }
}