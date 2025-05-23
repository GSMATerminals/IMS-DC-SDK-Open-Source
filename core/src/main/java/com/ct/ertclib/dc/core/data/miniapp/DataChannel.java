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
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;

@XStreamAlias("DataChannel")
public class DataChannel {
    @XStreamAlias("AutoAcceptDcSetup")
    private String autoAcceptDcSetup;
    @XStreamAlias("Bandwidth")
    private String bandwidth;
    @XStreamAsAttribute
    private String dcId;
    @XStreamAlias("DcLabel")
    private String dcLabel;
    @XStreamAlias("MaxRetr")
    private String maxRetr;
    @XStreamAlias("MaxTime")
    private String maxTime;
    @XStreamAlias("Ordered")
    private String ordered;
    @XStreamAlias("Priority")
    private String priority;
    @XStreamAlias("QosHint")
    private String qosHint;
    @XStreamAlias("StreamId")
    private String streamId;
    @XStreamAlias("Subprotocol")
    private String subProtocol;
    @XStreamAlias("UseCase")
    private String useCase;

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
