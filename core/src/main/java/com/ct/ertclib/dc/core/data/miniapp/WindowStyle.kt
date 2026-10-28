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

package com.ct.ertclib.dc.core.data.miniapp

import android.os.Parcel
import android.os.Parcelable

data class WindowStyle(
    var isFullScreen:Boolean,//是否全屏
    @Deprecated("")
    var navigationBarColor:String?,//导航栏
    var statusBarColor:String?,//胶囊栏背景颜色
    var statusBarTitleColor: Int = 0,//胶囊栏字体和图标颜色，0:黑色，1:白色
    var width: Float? = null,// 非宽屏模式下窗口宽度占屏幕宽度的百分比(0.1~1.0)，-1表示全屏(等同于1.0)，null表示未配置
    var height: Float? = null,// 非宽屏模式下窗口高度占屏幕高度的百分比(0.1~1.0)，-1表示全屏(等同于1.0)，null表示未配置
    var windowStart: Float? = null,// 非宽屏模式下窗口左上角X坐标占屏幕宽度的百分比(0.0~1.0)
    var windowTop: Float? = null,// 非宽屏模式下窗口左上角Y坐标占屏幕高度的百分比(0.0~1.0)
    var closeGuidePage: Boolean? = null// 是否关闭引导页，仅在四个窗口比例字段都配置时生效
)  : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readByte() != 0.toByte(),
        parcel.readString(),
        parcel.readString(),
        parcel.readInt(),
        if (parcel.readByte() != 0.toByte()) parcel.readFloat() else null,
        if (parcel.readByte() != 0.toByte()) parcel.readFloat() else null,
        if (parcel.readByte() != 0.toByte()) parcel.readFloat() else null,
        if (parcel.readByte() != 0.toByte()) parcel.readFloat() else null,
        if (parcel.readByte() != 0.toByte()) parcel.readInt() != 0 else null
    ) {
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeByte(if (isFullScreen) 1 else 0)
        parcel.writeString(navigationBarColor)
        parcel.writeString(statusBarColor)
        parcel.writeInt(statusBarTitleColor)
        parcel.writeByte(if (width != null) 1 else 0)
        width?.let { parcel.writeFloat(it) }
        parcel.writeByte(if (height != null) 1 else 0)
        height?.let { parcel.writeFloat(it) }
        parcel.writeByte(if (windowStart != null) 1 else 0)
        windowStart?.let { parcel.writeFloat(it) }
        parcel.writeByte(if (windowTop != null) 1 else 0)
        windowTop?.let { parcel.writeFloat(it) }
        parcel.writeByte(if (closeGuidePage != null) 1 else 0)
        closeGuidePage?.let { parcel.writeByte(if (it) 1 else 0) }
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<WindowStyle> {
        override fun createFromParcel(parcel: Parcel): WindowStyle {
            return WindowStyle(parcel)
        }

        override fun newArray(size: Int): Array<WindowStyle?> {
            return arrayOfNulls(size)
        }
    }
}