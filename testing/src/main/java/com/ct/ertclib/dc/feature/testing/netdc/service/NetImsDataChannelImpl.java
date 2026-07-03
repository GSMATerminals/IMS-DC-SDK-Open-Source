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

package com.ct.ertclib.dc.feature.testing.netdc.service;

import android.os.RemoteException;

import androidx.annotation.NonNull;

import com.ct.ertclib.dc.core.utils.common.FileUtils;
import com.ct.ertclib.dc.core.utils.logger.Logger;
import com.ct.ertclib.dc.net.websocket.InterfaceHandler;
import com.newcalllib.datachannel.V1_0.IDCSendDataCallback;
import com.newcalllib.datachannel.V1_0.IImsDCObserver;
import com.newcalllib.datachannel.V1_0.IImsDataChannel;
import com.newcalllib.datachannel.V1_0.ImsDCStatus;

import org.jetbrains.annotations.NotNull;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;

public class NetImsDataChannelImpl extends IImsDataChannel.Stub {
    private static final String TAG = "NetImsDataChannelImpl";
    private static final Logger sLogger = Logger.getLogger(TAG);
    public static final int DC_TYPE_ADC = 2;
    public static final int DC_TYPE_BDC = 1;
    private int mDcType;

    public void setDcTyp(int dcTypeBdc) {
        mDcType = dcTypeBdc;
    }

    private int mSlotId;
    public void setSlotId(int slotId) {
        mSlotId = slotId;
    }

    public int getSlotId() {
        return mSlotId;
    }

    private String mTelecomCallId;

    public void setTelecomCallId(String telecomCallId) {
        mTelecomCallId = telecomCallId;
    }

    private String mTelephonyNumber;

    public String getTelephonyNumber() {
        return mTelephonyNumber;
    }

    public void setTelephonyNumber(String telephonyNumber) {
        this.mTelephonyNumber = telephonyNumber;
    }

    private String mDcLabel;

    public void setDcLabel(String label) {
        mDcLabel = label;
    }

    private String mStreamId;

    public void setStreamId(String streamId) {
        mStreamId = streamId;
    }

    private ImsDCStatus mDcStatus;
    public void setDcStatus(ImsDCStatus status) {
        boolean hasChanged = mDcStatus != null && mDcStatus != status;
        mDcStatus = status;
        if (hasChanged){
            try {
                if (mImsObserver != null) {
                    mImsObserver.onDataChannelStateChange(status, 0);
                }
            } catch (RemoteException e) {
                sLogger.error(e.getMessage(), e);
            }
        }
    }

    private IImsDCObserver mImsObserver;
    @Override
    public void registerObserver(IImsDCObserver l) throws RemoteException {
        if (l == null) {
            sLogger.info("registerObserver dcOberver is null");
            return;
        }
        mImsObserver = l;
        InterfaceHandler.INSTANCE.addDCDataCallback(mDcLabel,new InterfaceHandler.DCDataCallback(){
            @Override
            public void onDownlinkADC(@NotNull String callId, @NonNull byte[] data) {
                try {
                    mImsObserver.onMessage(data, data.length);
                } catch (RemoteException e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public void onDownlinkBDC(@NotNull String callId, @NotNull byte[] data) {
                try {
                    mImsObserver.onMessage(data, data.length);
                } catch (RemoteException e) {
                    throw new RuntimeException(e);
                }
            }
        });
    }

    @Override
    public void unregisterObserver() throws RemoteException {
        sLogger.info("unregisterObserver");
        mImsObserver = null;
    }

    @Override
    public boolean send(byte[] data, int length, IDCSendDataCallback l) throws RemoteException {
        if (mDcStatus == ImsDCStatus.DC_STATE_OPEN) {
            if (mDcType == DC_TYPE_BDC) {
                return sendBdcData(data, length, l);
            } else if (mDcType == DC_TYPE_ADC) {
                sLogger.info("sendAdcData  length="+length+", data="+ new String(data, StandardCharsets.UTF_8));
                return sendAdcData(data, length, l);
            }
            return true;
        }
        return true;
    }

    private boolean sendAdcData(byte[] data, int length, IDCSendDataCallback callback) {
        sLogger.info("sendAdcData  length="+length+", data="+ new String(data, StandardCharsets.UTF_8));
        try {
            InterfaceHandler.INSTANCE.sendUplinkADC(mTelecomCallId,mDcLabel,FileUtils.INSTANCE.byteArrayToBase64(data));
            callback.onSendDataResult(20000);
        } catch (RemoteException e) {
            sLogger.error(e.getMessage(), e);
        }
        return true;
    }

    private boolean sendBdcData(byte[] data, int length, IDCSendDataCallback callback) {
        sLogger.info("sendBdcData  length="+length+", data="+ Arrays.toString(data));
        try {
            InterfaceHandler.INSTANCE.sendUplinkBDC(mTelecomCallId,mDcLabel,FileUtils.INSTANCE.byteArrayToBase64(data));
            callback.onSendDataResult(20000);
        } catch (RemoteException e) {
            sLogger.error(e.getMessage(), e);
        }
        return true;
    }

    @Override
    public void close() throws RemoteException {
        ArrayList<String> list = new ArrayList<>();
        list.add(mDcLabel);
        InterfaceHandler.INSTANCE.closeADC(mTelecomCallId,list);
    }

    @Override
    public String getDcLabel() throws RemoteException {
        return mDcLabel;
    }

    @Override
    public String getSubProtocol() throws RemoteException {
        return null;
    }

    @Override
    public long bufferedAmount() throws RemoteException {
        return 33 * 1024;//33k
    }

    @Override
    public int getDCType() throws RemoteException {
        return mDcType;
    }

    @Override
    public ImsDCStatus getState() throws RemoteException {
        return mDcStatus;
    }

    @Override
    public String getTelecomCallId() throws RemoteException {
        return mTelecomCallId;
    }

    @Override
    public String getStreamId() throws RemoteException {
        return mStreamId;
    }

    @Override
    public String getPhoneNumber() throws RemoteException {
        return mTelephonyNumber;
    }

    public boolean isClosed() {
        return this.mDcStatus == ImsDCStatus.DC_STATE_CLOSED;
    }
}
