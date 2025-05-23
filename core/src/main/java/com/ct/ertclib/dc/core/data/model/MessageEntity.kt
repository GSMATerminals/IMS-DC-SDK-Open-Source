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
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.ct.ertclib.dc.core.common.MessageStatus

@Entity(
    tableName = "messages"
)
data class MessageEntity(
    @PrimaryKey(autoGenerate = true) var _id: Int = 0,
    var conversationId: Int,
    var senderId: Int, // send contact id
    var status: Int,
    var read: Int = 0,
    var type: String,
    var message: String,
    var mediaUri: String,
    var mediaSize: Long = 0,
    var sentTime: Long = 0,
    var receiveTime: Long = 0,
    var selfId: Int, //receive contact id
    var text: String?,
    @ColumnInfo(defaultValue = "0")
    var direction: Int = 0, // 0, send 1, receive
    var messageId: String,
    var thumbnailUri: String? = null
) : Parcelable {

    constructor(parcel: Parcel) : this(
        parcel.readInt(),
        parcel.readInt(),
        parcel.readInt(),
        parcel.readInt(),
        parcel.readInt(),
        parcel.readString()!!,
        parcel.readString()!!,
        parcel.readString()!!,
        parcel.readLong(),
        parcel.readLong(),
        parcel.readLong(),
        parcel.readInt(),
        parcel.readString(),
        parcel.readInt(),
        parcel.readString()!!,
        parcel.readString()
    ) {
    }

    fun getIsIncoming(): Boolean {
        return status >= MessageStatus.INCOMING_COMPLETE.value
    }


    fun isFailed(): Boolean {
        return when (status) {
            MessageStatus.INCOMING_DOWNLOAD_FAILED.value -> true
            MessageStatus.OUTGOING_FAILED.value -> true
            else -> false
        }
    }

    fun canLoaded(): Boolean {
        return if (getIsIncoming()) {
            status == MessageStatus.INCOMING_COMPLETE.value
        } else {
            true
        }
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(_id)
        parcel.writeInt(conversationId)
        parcel.writeInt(senderId)
        parcel.writeInt(status)
        parcel.writeInt(read)
        parcel.writeString(type)
        parcel.writeString(message)
        parcel.writeString(mediaUri)
        parcel.writeLong(mediaSize)
        parcel.writeLong(sentTime)
        parcel.writeLong(receiveTime)
        parcel.writeInt(selfId)
        parcel.writeString(text)
        parcel.writeInt(direction)
        parcel.writeString(messageId)
        parcel.writeString(thumbnailUri)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<MessageEntity> {
        override fun createFromParcel(parcel: Parcel): MessageEntity {
            return MessageEntity(parcel)
        }

        override fun newArray(size: Int): Array<MessageEntity?> {
            return arrayOfNulls(size)
        }
    }

}
