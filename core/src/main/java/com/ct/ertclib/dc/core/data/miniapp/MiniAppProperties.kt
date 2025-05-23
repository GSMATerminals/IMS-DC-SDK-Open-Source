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

data class MiniAppProperties(
    val appId: String?,
    val category: String?,
    val permissions: List<String>?,
    val priority: Int,
    val version: String?,
    val windowStyle: WindowStyle?,
    val shouldCreateControlADC: Boolean?,// 用户点击启动后，是否允许SDK创建控制ADC
    val shouldStartRemoteApp: Boolean?,// 用户点击启动后，是否请求对端也启动小程序，shouldCreateControlADC为true时有效
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readString(),
        parcel.readString(),
        parcel.createStringArrayList(),
        parcel.readInt(),
        parcel.readString(),
        parcel.readParcelable(WindowStyle::class.java.classLoader),
        parcel.readByte() != 0.toByte(),
        parcel.readByte() != 0.toByte()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(appId)
        parcel.writeString(category)
        parcel.writeStringList(permissions)
        parcel.writeInt(priority)
        parcel.writeString(version)
        parcel.writeParcelable(windowStyle, flags)
        parcel.writeByte(if (shouldCreateControlADC == true) 1 else 0)
        parcel.writeByte(if (shouldStartRemoteApp == true) 1 else 0)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<MiniAppProperties> {
        override fun createFromParcel(parcel: Parcel): MiniAppProperties {
            return MiniAppProperties(parcel)
        }

        override fun newArray(size: Int): Array<MiniAppProperties?> {
            return arrayOfNulls(size)
        }
    }
}