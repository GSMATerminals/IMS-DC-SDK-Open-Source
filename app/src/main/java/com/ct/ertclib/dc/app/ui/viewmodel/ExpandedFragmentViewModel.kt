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

package com.ct.ertclib.dc.app.ui.viewmodel

import android.content.Context
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.ct.ertclib.dc.app.R
import com.ct.ertclib.dc.core.common.NewCallAppSdkInterface
import com.ct.ertclib.dc.core.common.NewCallAppSdkInterface.SDK_STYLE_WHITE
import com.ct.ertclib.dc.core.data.model.MiniAppInfo
import com.ct.ertclib.dc.core.utils.common.ScreenUtils


class ExpandedFragmentViewModel : ViewModel() {

    companion object {
        const val ICON_STYLE_NORMAL = 0
        const val ICON_STYLE_LIGHT = 1
        private const val TAG = "ExpandedFragmentViewModel"
    }

    var miniAppList: MutableLiveData<List<MiniAppInfo>> = MutableLiveData()

    val historyMiniAppList: MutableLiveData<MutableList<MiniAppInfo>> = MutableLiveData()

    fun getHistoryMiniAppList(telecomId: String): MutableList<MiniAppInfo> {
        return NewCallAppSdkInterface.getHistoryMiniAppList(telecomId)
    }

    fun getTextColor(context: Context, style: Int): Int {
        return when (style) {
            SDK_STYLE_WHITE -> context.resources.getColor(R.color.miniapp_name_title_color_light)
            else -> context.resources.getColor(R.color.miniapp_name_title_color)
        }
    }

    fun getBackgroundDrawableColor(context:Context, style: Int): Int {
        return when (style) {
            SDK_STYLE_WHITE -> context.resources.getColor(com.ct.ertclib.dc.core.R.color.expanded_dialog_bg_color_white)
            else -> context.resources.getColor(com.ct.ertclib.dc.core.R.color.expanded_dialog_bg_color)
        }
    }

    fun getIconStyle(style: Int): Int {
        return when (style) {
            SDK_STYLE_WHITE -> ICON_STYLE_LIGHT
            else -> ICON_STYLE_NORMAL
        }
    }

    fun getPanelExpandedPivotY(context: Context?, startY: Int): Float {
        if (context == null) return 0F
        val screenHeight = ScreenUtils.getScreenHeight(context)
        val panelHeight =
            context.resources.getDimensionPixelSize(com.ct.ertclib.dc.core.R.dimen.expanded_menu_height_width) + context.resources.getDimensionPixelSize(
                com.ct.ertclib.dc.core.R.dimen.expanded_menu_setting_height
            ) + context.resources.getDimensionPixelSize(com.ct.ertclib.dc.core.R.dimen.expanded_margin_bottom)
        val floatingBallHeight = context.resources.getDimensionPixelSize(R.dimen.half_floatingBall_height)
        return if (screenHeight - startY - floatingBallHeight / 2 >= panelHeight) {
            0F
        } else {
            (panelHeight - (screenHeight - startY) + floatingBallHeight).toFloat()
        }
    }
}