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

package com.ct.ertclib.dc.core.data.miniapp

import android.os.Parcel
import android.os.Parcelable

data class WindowStyle(
    var isFullScreen:Boolean,//是否全屏
    var navigationBarColor:String?,//导航栏背景色，isFullScreen为false时有效
    var statusBarColor:String?,//状态栏背景色，isFullScreen为false时有效
    var statusBarTitleColor: Int = 0//状态栏字体和图标颜色，0:黑色，1:白色
)  : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readByte() != 0.toByte(),
        parcel.readString(),
        parcel.readString(),
        parcel.readInt()
    ) {
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeByte(if (isFullScreen) 1 else 0)
        parcel.writeString(navigationBarColor)
        parcel.writeString(statusBarColor)
        parcel.writeInt(statusBarTitleColor)
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