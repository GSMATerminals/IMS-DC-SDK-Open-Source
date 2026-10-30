/*
 * Copyright 2025-China Telecom Research Institute.
 * All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ct.ertclib.dc.core.data.miniapp;

import com.ct.ertclib.dc.core.utils.common.XmlAlias;
import com.ct.ertclib.dc.core.utils.common.XmlAttribute;

@XmlAlias("DataChannel")
public class DataChannel {
    @XmlAttribute
    private String dcId;
    @XmlAlias("DcLabel")
    private String dcLabel;
    @XmlAlias("StreamId")
    private String streamId;
    @XmlAlias("Subprotocol")
    private String subProtocol;
    @XmlAlias("Ordered")
    private String ordered;
    @XmlAlias("Priority")
    private String priority;
    @XmlAlias("UseCase")
    private String useCase;
    @XmlAlias("AutoAcceptDcSetup")
    private String autoAcceptDcSetup;
    @XmlAlias("Bandwidth")
    private String bandwidth;
    @XmlAlias("QosHint")
    private String qosHint;
    @XmlAlias("MaxRetr")
    private String maxRetr;
    @XmlAlias("MaxTime")
    private String maxTime;

    public String getAutoAcceptDcSetup() {
        return this.autoAcceptDcSetup;
    }

    public String getBandwidth() {
        return this.bandwidth;
    }

    public String getDcId() {
        return this.dcId;
    }

    public String getDcLabel() {
        return this.dcLabel;
    }

    public String getMaxRetr() {
        return this.maxRetr;
    }

    public String getMaxTime() {
        return this.maxTime;
    }

    public String getOrdered() {
        return this.ordered;
    }

    public String getPriority() {
        return this.priority;
    }

    public String getQosHint() {
        return this.qosHint;
    }

    public String getStreamId() {
        return this.streamId;
    }

    public String getSubProtocol() {
        return this.subProtocol;
    }

    public String getUseCase() {
        return this.useCase;
    }

    public void setAutoAcceptDcSetup(String str) {
        this.autoAcceptDcSetup = str;
    }

    public void setBandwidth(String str) {
        this.bandwidth = str;
    }

    public void setDcId(String str) {
        this.dcId = str;
    }

    public void setDcLabel(String str) {
        this.dcLabel = str;
    }

    public void setMaxRetr(String str) {
        this.maxRetr = str;
    }

    public void setMaxTime(String str) {
        this.maxTime = str;
    }

    public void setOrdered(String str) {
        this.ordered = str;
    }

    public void setPriority(String str) {
        this.priority = str;
    }

    public void setQosHint(String str) {
        this.qosHint = str;
    }

    public void setStreamId(String str) {
        this.streamId = str;
    }

    public void setSubProtocol(String str) {
        this.subProtocol = str;
    }

    public void setUseCase(String str) {
        this.useCase = str;
    }

    public String toString() {
        return "DataChannel{dcId='" + this.dcId + '\'' + ", streamId='" + this.streamId + '\'' + ", dcLabel='" + this.dcLabel + '\'' + ", useCase='" + this.useCase + '\'' + ", subProtocol='" + this.subProtocol + '\'' + ", ordered='" + this.ordered + '\'' + ", maxRetr='" + this.maxRetr + '\'' + ", maxTime='" + this.maxTime + '\'' + ", priority='" + this.priority + '\'' + ", autoAcceptDcSetup='" + this.autoAcceptDcSetup + '\'' + ", bandwidth='" + this.bandwidth + '\'' + ", qosHint='" + this.qosHint + '\'' + "}";
    }
}
