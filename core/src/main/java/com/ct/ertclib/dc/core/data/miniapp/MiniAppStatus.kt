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

enum class MiniAppStatus() : Parcelable {
    UNINSTALLED,
    INSTALLED,
    DOWNLOADING,
    STARTING,
    STARTED,
    STOPPED;

    companion object {
        @JvmField
        val CREATOR = object : Parcelable.Creator<MiniAppStatus> {
            override fun createFromParcel(parcel: Parcel): MiniAppStatus {
                return values()[parcel.readInt()]
            }

            override fun newArray(size: Int): Array<MiniAppStatus?> {
                return arrayOfNulls(size)
            }
        }
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(ordinal)
    }

    override fun describeContents(): Int {
        return 0
    }
}