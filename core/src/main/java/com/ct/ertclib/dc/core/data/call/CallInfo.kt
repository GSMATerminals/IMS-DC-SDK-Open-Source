/*
 *   Copyright 2025-China Telecom Research Institute.
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *        https://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package com.ct.ertclib.dc.core.data.call

import android.os.Parcel
import android.os.Parcelable
import android.telecom.Call

data class CallInfo(
    var slotId: Int = -1,
    var telecomCallId: String,
    var state: Int,
    var remoteNumber: String?,
    var myNumber: String?,
    var videoState: Int,
    var isConference: Boolean,
    var isOutgoingCall: Boolean,
    var isCtCall: Boolean
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readInt(),
        parcel.readString()!!,
        parcel.readInt(),
        parcel.readString(),
        parcel.readString(),
        parcel.readInt(),
        parcel.readByte() != 0.toByte(),
        parcel.readByte() != 0.toByte(),
        parcel.readByte() != 0.toByte()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(slotId)
        parcel.writeString(telecomCallId)
        parcel.writeInt(state)
        parcel.writeString(remoteNumber)
        parcel.writeString(myNumber)
        parcel.writeInt(videoState)
        parcel.writeByte(if (isConference) 1 else 0)
        parcel.writeByte(if (isOutgoingCall) 1 else 0)
        parcel.writeByte(if (isOutgoingCall) 1 else 0)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<CallInfo> {
        override fun createFromParcel(parcel: Parcel): CallInfo {
            return CallInfo(parcel)
        }

        override fun newArray(size: Int): Array<CallInfo?> {
            return arrayOfNulls(size)
        }
    }

    fun isInCall() : Boolean {
        return this.state == Call.STATE_ACTIVE
    }

    fun isRinging() : Boolean {
        return this.state == Call.STATE_RINGING
    }

}
