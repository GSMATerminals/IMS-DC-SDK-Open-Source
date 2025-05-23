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

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

public enum MessageType implements Parcelable {
    TYPE_TEXT,
    TYPE_IMAGE,
    TYPE_VIDEO,
    TYPE_FILE,
    TYPE_CONTACT,
    TYPE_LOCATION;

    public static final Creator<MessageType> CREATOR = new Creator<MessageType>() {
        @Override
        public MessageType createFromParcel(Parcel in) {
            return MessageType.values()[in.readInt()];
        }

        @Override
        public MessageType[] newArray(int size) {
            return new MessageType[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeInt(ordinal());
    }
}
