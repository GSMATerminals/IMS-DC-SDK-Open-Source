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

package com.ct.ertclib.dc.core.data.common;

import android.os.Parcel;

import androidx.annotation.NonNull;

import com.ct.ertclib.dc.core.data.message.IMessage;
import com.ct.ertclib.dc.core.data.message.MessageType;

public class FileMessage extends IMessage {

    public String contentType;
    public String md5;
    public String name;
    public String path;
    public long size;

    public FileMessage() {
    }

    public FileMessage(String from, String to) {
        super(from, to);
    }

    public FileMessage(Parcel parcel) {
        this.id = parcel.readString();
        this.from = parcel.readString();
        this.to = parcel.readString();
        this.type = (MessageType) parcel.readParcelable(MessageType.class.getClassLoader());
        this.timestamp = parcel.readLong();
        this.name = parcel.readString();
        this.path = parcel.readString();
        this.md5 = parcel.readString();
        this.size = parcel.readLong();
    }

    @Override
    public String toJson() {
        return null;
    }

    public static final Creator<FileMessage> CREATOR = new Creator<FileMessage>() {
        @Override
        public FileMessage createFromParcel(Parcel in) {
            return new FileMessage(in);
        }

        @Override
        public FileMessage[] newArray(int size) {
            return new FileMessage[0];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeString(this.id);
        dest.writeString(this.from);
        dest.writeString(this.to);
        dest.writeParcelable(this.type, flags);
        dest.writeLong(this.timestamp);
        dest.writeString(this.name);
        dest.writeString(this.path);
        dest.writeString(this.md5);
        dest.writeLong(this.size);
    }

    public String toString() {
        return "FileMessage{name='" + this.name + '\'' + ", path='" + this.path + '\'' + ", md5='" + this.md5 + '\'' + ", size=" + this.size + ", id='" + this.id + '\'' + ", from='" + this.from + '\'' + ", to='" + this.to + '\'' + ", type=" + this.type
                + ", timestamp=" + this.timestamp
                +", messageId=" + this.id
                + "}";
    }
}
