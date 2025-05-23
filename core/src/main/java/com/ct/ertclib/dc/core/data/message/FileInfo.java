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

package com.ct.ertclib.dc.core.data.message;

import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

public class FileInfo implements Parcelable {

    public String displayName;

    public long id;

    public int mCount;

    public boolean mIsDirectory;

    public String mimeType;

    public String path;

    public long size;

    public Uri uri;

    public FileInfo(){}

    protected FileInfo(Parcel in) {
        displayName = in.readString();
        id = in.readLong();
        mCount = in.readInt();
        mIsDirectory = in.readByte() != 0;
        mimeType = in.readString();
        path = in.readString();
        size = in.readLong();
        uri = in.readParcelable(Uri.class.getClassLoader());
    }

    public static final Creator<FileInfo> CREATOR = new Creator<FileInfo>() {
        @Override
        public FileInfo createFromParcel(Parcel in) {
            return new FileInfo(in);
        }

        @Override
        public FileInfo[] newArray(int size) {
            return new FileInfo[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeString(displayName);
        dest.writeLong(id);
        dest.writeInt(mCount);
        dest.writeByte((byte) (mIsDirectory ? 1 : 0));
        dest.writeString(mimeType);
        dest.writeString(path);
        dest.writeLong(size);
        dest.writeParcelable(uri, flags);
    }

    public boolean isImage() {
        return mimeType != null && mimeType.startsWith("image");
    }

    public boolean isVideo() {
        return mimeType != null && mimeType.startsWith("video");
    }
}
