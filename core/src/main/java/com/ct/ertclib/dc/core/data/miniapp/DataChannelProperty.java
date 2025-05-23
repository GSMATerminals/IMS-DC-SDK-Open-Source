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

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamImplicit;

import java.util.List;

/**
 *
 * <DataChannelProperties>
 *     <DataChannelApp appId="fileShare">
 *         <DataChannel dcId="dcId">
 *             <StreamId></StreamId>
 *             <DcLabel>fileShare</DcLabel>
 *             <UseCase>1</UseCase>
 *             <Subprotocol></Subprotocol>
 *             <Ordered></Ordered>
 *             <MaxRetr></MaxRetr>
 *             <MaxTime></MaxTime>
 *             <Priority></Priority>
 *             <AutoAcceptDcSetup></AutoAcceptDcSetup>
 *             <Bandwidth></Bandwidth>
 *             <QosHint></QosHint>
 *         </DataChannel>
 *     </DataChannelApp>
 *     <DataChannelApp>
 * 		...
 *     </DataChannelApp>
 * </DataChannelProperties>
 *
 */

@XStreamAlias("DataChannelProperties")
public class DataChannelProperty {
    @XStreamImplicit(itemFieldName = "DataChannelApp")
    public List<DataChannelApp> dataChannelAppList;

    public List<DataChannelApp> getDataChannelAppList() {
        return dataChannelAppList;
    }

    public void setDataChannelAppList(List<DataChannelApp> dataChannelAppList) {
        this.dataChannelAppList = dataChannelAppList;
    }

    @Override
    public String toString() {
        return "DataChannelProperty{" +
                "mDataChannelApps=" + dataChannelAppList +
                '}';
    }
}
