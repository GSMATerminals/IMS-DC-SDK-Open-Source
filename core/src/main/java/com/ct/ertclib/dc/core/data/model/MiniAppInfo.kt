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

package com.ct.ertclib.dc.core.data.model

import android.os.Parcel
import android.os.Parcelable
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import com.ct.ertclib.dc.core.data.miniapp.MiniAppStatus
import com.ct.ertclib.dc.core.data.miniapp.MiniAppProperties
import com.google.gson.annotations.SerializedName

@Entity(
    tableName = "mini_app_info"
)
data class MiniAppInfo(
    @PrimaryKey
    @SerializedName("appid")
    var appId: String,
    var appName: String,
    var appIcon: String?,
    @SerializedName("autolaunch")
    var autoLaunch: Boolean,
    @SerializedName("autoload")
    var autoLoad: Boolean,
    var callId: String,
    @SerializedName("etag")
    var eTag: String,
    @SerializedName("ifWorkWithoutPeerDC")
    var ifWorkWithoutPeerDc: Boolean,
    var isOutgoingCall: Boolean,
    var myNumber: String?,
    // 当前已经安装的最新版本的路径
    var path: String?,
    var phase: String,
    @SerializedName("qos-hint")
    var qosHint: String,
    var remoteNumber: String?,
    var slotId: Int,
    var supportScene: Int,
    @Ignore
    var appStatus: MiniAppStatus? = MiniAppStatus.UNINSTALLED,
    var isStartAfterInstalled: Boolean = false,
    @Ignore
    var appProperties: MiniAppProperties?,
    var lastUseTime:Long = 0,
    var isFromBDC100: Boolean = false,
    var isActiveStart: Boolean = true
) : Parcelable {
    constructor(
        appId: String,
        appName: String,
        appIcon: String?,
        autoLaunch: Boolean,
        autoLoad: Boolean,
        callId: String,
        eTag: String,
        ifWorkWithoutPeerDc: Boolean,
        isOutgoingCall: Boolean,
        myNumber: String?,
        path: String?,
        phase: String,
        qosHint: String,
        remoteNumber: String?,
        slotId: Int,
        supportScene: Int,
        isStartAfterInstalled: Boolean = false,
        lastUseTime:Long
    ) :this(appId, appName, appIcon, autoLaunch, autoLoad, callId, eTag, ifWorkWithoutPeerDc, isOutgoingCall, myNumber, path, phase, qosHint, remoteNumber, slotId, supportScene, appStatus = MiniAppStatus.UNINSTALLED, isStartAfterInstalled, appProperties = null,lastUseTime,isFromBDC100 = false, isActiveStart = true)

    @Ignore
    constructor(
        appId: String,
        appName: String,
        appIcon: String?,
        autoLaunch: Boolean,
        autoLoad: Boolean,
        callId: String,
        eTag: String,
        ifWorkWithoutPeerDc: Boolean,
        isOutgoingCall: Boolean,
        myNumber: String?,
        path: String?,
        phase: String,
        qosHint: String,
        remoteNumber: String?,
        slotId: Int,
        supportScene: Int,
        appStatus: MiniAppStatus?,
        isStartAfterInstalled: Boolean = false,
        appProperties: MiniAppProperties?,
    ) :this(appId, appName, appIcon, autoLaunch, autoLoad, callId, eTag, ifWorkWithoutPeerDc, isOutgoingCall, myNumber, path, phase, qosHint, remoteNumber, slotId, supportScene, appStatus, isStartAfterInstalled, appProperties,lastUseTime = 0,isFromBDC100 = false, isActiveStart = true)

    constructor(parcel: Parcel) : this(
        parcel.readString()!!,
        parcel.readString()!!,
        parcel.readString(),
        parcel.readByte() != 0.toByte(),
        parcel.readByte() != 0.toByte(),
        parcel.readString()!!,
        parcel.readString()!!,
        parcel.readByte() != 0.toByte(),
        parcel.readByte() != 0.toByte(),
        parcel.readString(),
        parcel.readString(),
        parcel.readString()!!,
        parcel.readString()!!,
        parcel.readString(),
        parcel.readInt(),
        parcel.readInt(),
        parcel.readParcelable(MiniAppStatus::class.java.classLoader),
        parcel.readByte() != 0.toByte(),
        parcel.readParcelable(MiniAppProperties::class.java.classLoader),
        parcel.readLong(),
        parcel.readByte() != 0.toByte(),
        parcel.readByte() != 0.toByte(),
    ){}

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(appId)
        parcel.writeString(appName)
        parcel.writeString(appIcon)
        parcel.writeByte(if (autoLaunch) 1 else 0)
        parcel.writeByte(if (autoLoad) 1 else 0)
        parcel.writeString(callId)
        parcel.writeString(eTag)
        parcel.writeByte(if (ifWorkWithoutPeerDc) 1 else 0)
        parcel.writeByte(if (isOutgoingCall) 1 else 0)
        parcel.writeString(myNumber)
        parcel.writeString(path)
        parcel.writeString(phase)
        parcel.writeString(qosHint)
        parcel.writeString(remoteNumber)
        parcel.writeInt(slotId)
        parcel.writeInt(supportScene)
        parcel.writeParcelable(appStatus,flags)
        parcel.writeByte(if (isStartAfterInstalled) 1 else 0)
        parcel.writeParcelable(appProperties, flags)
        parcel.writeLong(lastUseTime)
        parcel.writeByte(if (isFromBDC100) 1 else 0)
        parcel.writeByte(if (isActiveStart) 1 else 0)
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun toString(): String {
        return "MiniAppInfo(appId: $appId, appName: $appName, autoLaunch: $autoLaunch, autoLoad: $autoLoad, callId: $callId, ifWorkWithoutPeerDc: $ifWorkWithoutPeerDc, isOutgoingCall: $isOutgoingCall, myNumber: $myNumber, " +
                "path: $path, qosHint: $qosHint, remoteNumber: $remoteNumber, slotId: $slotId, supportScene: $supportScene, isFromBDC100: $isFromBDC100, isActiveStart: $isActiveStart)"
    }

    companion object CREATOR : Parcelable.Creator<MiniAppInfo> {
        override fun createFromParcel(parcel: Parcel): MiniAppInfo {
            return MiniAppInfo(parcel)
        }

        override fun newArray(size: Int): Array<MiniAppInfo?> {
            return arrayOfNulls(size)
        }
    }
    fun isPhasePreCall(): Boolean{
        return "PRECALL".equals(phase,true)
    }

    fun isPhaseInCall(): Boolean{
        return "INCALL".equals(phase,true)
    }
}

enum class SupportScene(var value : Int) {
    AUDIO(1), VIDEO(2), ALL(3)// 应用程序支持使用的场景，取值定义如下：1：仅音频通话场景支持;2：仅视频通话场景支持;3：音频和视频通话场景均支持
}
