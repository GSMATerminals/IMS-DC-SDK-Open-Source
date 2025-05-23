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

package com.newcalllib.sharescreen;

import android.os.Parcel;
import android.os.Parcelable;

public enum ScreenShareStatus implements Parcelable {
    SUCCESS,
    FAILURE;

    public static final Creator<ScreenShareStatus> CREATOR = new Creator<ScreenShareStatus>() {
        public ScreenShareStatus createFromParcel(Parcel in) {
            return ScreenShareStatus.fromInteger(in.readInt());
        }

        public ScreenShareStatus[] newArray(int size) {
            return new ScreenShareStatus[size];
        }
    };

    private ScreenShareStatus() {
    }

    public static ScreenShareStatus fromInteger(int value) {
        return values()[value];
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.toInteger());
    }

    public int toInteger() {
        return this.ordinal();
    }
}