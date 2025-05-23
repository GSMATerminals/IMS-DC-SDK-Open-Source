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

package com.newcalllib.datachannel.V1_0;

import android.os.Parcel;
import android.os.Parcelable;

public enum ImsDCStatus implements Parcelable {
    DC_STATE_CONNECTING,
    DC_STATE_OPEN,
    DC_STATE_CLOSING,
    DC_STATE_CLOSED;

    public static final Creator<ImsDCStatus> CREATOR = new Creator<ImsDCStatus>() {
        public ImsDCStatus createFromParcel(Parcel in) {
            return ImsDCStatus.fromInteger(in.readInt());
        }

        public ImsDCStatus[] newArray(int size) {
            return new ImsDCStatus[size];
        }
    };

    ImsDCStatus() {
    }

    public static ImsDCStatus fromInteger(int value) {
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