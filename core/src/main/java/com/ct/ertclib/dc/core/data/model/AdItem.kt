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