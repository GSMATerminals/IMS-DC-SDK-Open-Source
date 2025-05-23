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

import android.os.Parcelable;

import java.util.UUID;

public abstract class IMessage implements Parcelable {

    public static final String KEY_FROM = "from";
    public static final String KEY_ID = "id";
    public static final String KEY_TIMESTAMP = "timestamp";
    public static final String KEY_TO = "to";
    public static final String KEY_TYPE = "type";

    public String from;
    public String to;
    public String id;
    public long timestamp = System.currentTimeMillis();
    public MessageType type;

    public IMessage(){}
    public IMessage(String from, String to) {
        this.from = from;
        this.to = to;
        this.id = from + "_" + to + "_" + UUID.randomUUID().toString();
    }

    public String getId() {
        if (this.id == null) {
            this.id = from + "_" + to + "_" + UUID.randomUUID().toString();
        }
        return id;
    }

    public void setId(String id) {
        this.id  = id;
    }

    public abstract String toJson();
}
