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
import com.ct.ertclib.dc.core.data.model.MiniAppInfo
import com.google.gson.annotations.SerializedName

data class MiniAppList(
    var appNum: Int,
    var applications: ArrayList<MiniAppInfo>?,
    var beginIndex: Int,
    var callId: String?,
    @SerializedName("ifPeerSupportDC")
    var ifPeerSupport: Boolean,
    var remoteNumber: String?,
    var totalAppNum: Int
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readInt(),
        parcel.createTypedArrayList(MiniAppInfo),
        parcel.readInt(),
        parcel.readString(),
        parcel.readByte() != 0.toByte(),
        parcel.readString(),
        parcel.readInt()
    ) {
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(appNum)
        parcel.writeTypedList(applications)
        parcel.writeInt(beginIndex)
        parcel.writeString(callId)
        parcel.writeByte(if (ifPeerSupport) 1 else 0)
        parcel.writeString(remoteNumber)
        parcel.writeInt(totalAppNum)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<MiniAppList> {
        override fun createFromParcel(parcel: Parcel): MiniAppList {
            return MiniAppList(parcel)
        }

        override fun newArray(size: Int): Array<MiniAppList?> {
            return arrayOfNulls(size)
        }
    }

}
