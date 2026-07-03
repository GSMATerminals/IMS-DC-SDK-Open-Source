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

package com.ct.ertclib.dc.core.data.model

import android.os.Parcel
import android.os.Parcelable

data class AdItem(
    val id: String,
    val imageUrl: String,  // 图片链接
    val h5Url: String,     // H5链接
    val title: String      // 标题
) : Parcelable {

    // 从 Parcel 中读取数据恢复对象
    constructor(parcel: Parcel) : this(
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: ""
    )

    // 将对象数据写入 Parcel
    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(id)
        parcel.writeString(imageUrl)
        parcel.writeString(h5Url)
        parcel.writeString(title)
    }

    override fun describeContents(): Int {
        return 0
    }

    // 必须提供的 CREATOR
    companion object CREATOR : Parcelable.Creator<AdItem> {
        override fun createFromParcel(parcel: Parcel): AdItem {
            return AdItem(parcel)
        }

        override fun newArray(size: Int): Array<AdItem?> {
            return arrayOfNulls(size)
        }
    }
}