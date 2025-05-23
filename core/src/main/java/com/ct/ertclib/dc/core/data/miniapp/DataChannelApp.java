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

package com.ct.ertclib.dc.core.data.miniapp;

import com.thoughtworks.xstream.annotations.XStreamAsAttribute;
import com.thoughtworks.xstream.annotations.XStreamImplicit;

import java.util.ArrayList;
import java.util.List;

public class DataChannelApp {
    @XStreamAsAttribute
    private String appId;
    @XStreamImplicit
    private List<DataChannel> dataChannelList;

    public void addDataChannel(DataChannel dataChannel) {
        if (this.dataChannelList == null) {
            this.dataChannelList = new ArrayList();
        }
        this.dataChannelList.add(dataChannel);
    }

    public String getAppId() {
        return this.appId;
    }

    public List<DataChannel> getDataChannelList() {
        return this.dataChannelList;
    }

    public void setAppId(String str) {
        this.appId = str;
    }

    public void setDataChannelList(List<DataChannel> list) {
        this.dataChannelList = list;
    }

    public String toString() {
        return "DataChannelApp{appId='" + this.appId + '\'' + ", dataChannelList=" + this.dataChannelList + "}";
    }
}
