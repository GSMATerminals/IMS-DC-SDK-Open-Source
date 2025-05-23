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

package com.ct.ertclib.dc.core.data.common

import android.os.Parcel
import android.os.Parcelable

class MediaInfo() : Parcelable {
    var id: Long = 0
    var bucketId: Long = 0
    var displayName: String? = null
    var bucketDisplayName: String? = null
    var path: String? = null
    var absolutePath: String? = null
    var mimeType: String? = null
    var width: Int = 0
    var height: Int = 0
    var cropPath: String? = null
    var editorPath: String? = null
    var cropWidth: Int = 0
    var cropHeight: Int = 0
    var cropOffsetX: Int = 0
    var cropOffsetY: Int = 0
    var cropAspectRatio: Float = 0F
    var duration: Long = 0
    var size: Long = 0
    var dateAdded: Long = 0
    var orientation: Int = 0
    var editorData: String? = null
    var sandboxPath: String? = null
    var originalPath: String? = null
    var compressPath: String? = null
    var watermarkPath: String? = null
    var customizeExtra: String? = null
    var videoThumbnailPath: String? = null
    var isEnabledMask: Boolean = false
    var isDirectory: Boolean = false
    var lastModified: Long = 0

    constructor(parcel: Parcel) : this() {
        id = parcel.readLong()
        bucketId = parcel.readLong()
        displayName = parcel.readString()
        bucketDisplayName = parcel.readString()
        path = parcel.readString()
        absolutePath = parcel.readString()
        mimeType = parcel.readString()
        width = parcel.readInt()
        height = parcel.readInt()
        cropPath = parcel.readString()
        editorPath = parcel.readString()
        cropWidth = parcel.readInt()
        cropHeight = parcel.readInt()
        cropOffsetX = parcel.readInt()
        cropOffsetY = parcel.readInt()
        cropAspectRatio = parcel.readFloat()
        duration = parcel.readLong()
        size = parcel.readLong()
        dateAdded = parcel.readLong()
        orientation = parcel.readInt()
        editorData = parcel.readString()
        sandboxPath = parcel.readString()
        originalPath = parcel.readString()
        compressPath = parcel.readString()
        watermarkPath = parcel.readString()
        customizeExtra = parcel.readString()
        videoThumbnailPath = parcel.readString()
        isEnabledMask = parcel.readByte() != 0.toByte()
        isDirectory = parcel.readByte() != 0.toByte()
        lastModified = parcel.readLong()
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeLong(id)
        parcel.writeLong(bucketId)
        parcel.writeString(displayName)
        parcel.writeString(bucketDisplayName)
        parcel.writeString(path)
        parcel.writeString(absolutePath)
        parcel.writeString(mimeType)
        parcel.writeInt(width)
        parcel.writeInt(height)
        parcel.writeString(cropPath)
        parcel.writeString(editorPath)
        parcel.writeInt(cropWidth)
        parcel.writeInt(cropHeight)
        parcel.writeInt(cropOffsetX)
        parcel.writeInt(cropOffsetY)
        parcel.writeFloat(cropAspectRatio)
        parcel.writeLong(duration)
        parcel.writeLong(size)
        parcel.writeLong(dateAdded)
        parcel.writeInt(orientation)
        parcel.writeString(editorData)
        parcel.writeString(sandboxPath)
        parcel.writeString(originalPath)
        parcel.writeString(compressPath)
        parcel.writeString(watermarkPath)
        parcel.writeString(customizeExtra)
        parcel.writeString(videoThumbnailPath)
        parcel.writeByte(if (isEnabledMask) 1 else 0)
        parcel.writeByte(if (isDirectory) 1 else 0)
        parcel.writeLong(lastModified)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<MediaInfo> {
        override fun createFromParcel(parcel: Parcel): MediaInfo {
            return MediaInfo(parcel)
        }

        override fun newArray(size: Int): Array<MediaInfo?> {
            return arrayOfNulls(size)
        }
    }
}