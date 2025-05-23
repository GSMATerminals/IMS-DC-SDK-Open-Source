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

package com.ct.ertclib.dc.core.ui.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.ct.ertclib.dc.core.R
import com.ct.ertclib.dc.core.common.NewCallAppSdkInterface
import com.ct.ertclib.dc.core.constants.CommonConstants.SHARE_PREFERENCE_CONSTANTS
import com.ct.ertclib.dc.core.constants.CommonConstants.SHARE_PREFERENCE_STYLE_PARAMS
import com.ct.ertclib.dc.core.constants.MiniAppConstants.STYLE_DEFAULT
import com.ct.ertclib.dc.core.constants.MiniAppConstants.STYLE_WHITE
import com.ct.ertclib.dc.core.data.miniapp.StyleItemData
import com.ct.ertclib.dc.core.utils.common.LogUtils

class StyleAdapter(private val context: Context) : RecyclerView.Adapter<StyleViewHolder>() {

    companion object {
        private const val TAG = "StyleAdapter"
    }

    private val styleDataList = mutableListOf(
        StyleItemData(R.string.style_black, R.drawable.style_normal),
        StyleItemData(R.string.style_white, R.drawable.style_light)
    )

    private val viewHolderMap = mutableMapOf<Int, StyleViewHolder>()
    private var nowStyle = NewCallAppSdkInterface.floatingBallStyle.value

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StyleViewHolder {
        LogUtils.debug(TAG, "onCreateViewHolder")
        val view = LayoutInflater.from(parent.context).inflate(R.layout.style_item_layout, parent, false)
        return StyleViewHolder(view)
    }

    override fun getItemCount(): Int {
        LogUtils.debug(TAG, "getItemCount: ${styleDataList.size}")
        return styleDataList.size
    }

    override fun onBindViewHolder(holder: StyleViewHolder, position: Int) {
        LogUtils.debug(TAG, "onBindViewHolder")
        viewHolderMap[position] = holder
        holder.styleImage.setImageDrawable(ContextCompat.getDrawable(context, styleDataList[position].styleImageResId))
        holder.styleText.text = context.getString(styleDataList[position].styleTextResId)
        if (position == getPositionWithStyle()) {
            holder.chooseImage.isVisible = true
        }
        holder.itemView.setOnClickListener {
            LogUtils.debug(TAG, "click styleItem position: $position")
            nowStyle = getStyleWithPosition(position)
            NewCallAppSdkInterface.floatingBallStyle.postValue(nowStyle)
            viewHolderMap.forEach { (key, holder) ->
                holder.chooseImage.isVisible = (key == position)
            }
        }
    }

    fun saveStyleSetting() {
        val sharePreference = context.getSharedPreferences(SHARE_PREFERENCE_CONSTANTS, Context.MODE_PRIVATE)
        val editor = sharePreference.edit()
        nowStyle?.let { editor.putInt(SHARE_PREFERENCE_STYLE_PARAMS, it) }
        editor.apply()
    }

    private fun getStyleWithPosition(position: Int): Int {
        return when (position) {
            1 -> STYLE_WHITE
            else -> STYLE_DEFAULT
        }
    }

    private fun getPositionWithStyle(): Int {
        return when (nowStyle) {
            STYLE_WHITE -> 1
            else -> 0
        }
    }
 }

class StyleViewHolder(view: View) : RecyclerView.ViewHolder(view) {
    val styleImage: AppCompatImageView = itemView.findViewById(R.id.style_image)
    val styleText: TextView = itemView.findViewById(R.id.style_text)
    val chooseImage: AppCompatImageView = itemView.findViewById(R.id.choose_image)
}