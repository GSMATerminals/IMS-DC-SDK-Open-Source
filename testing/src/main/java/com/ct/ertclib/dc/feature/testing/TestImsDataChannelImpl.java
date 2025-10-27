/*
 *   Copyright 2025-China Telecom Research Institute.
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *        https://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package com.ct.ertclib.dc.feature.testing;

import android.os.RemoteException;

import com.blankj.utilcode.util.GsonUtils;
import com.blankj.utilcode.util.SPUtils;
import com.blankj.utilcode.util.ThreadUtils;
import com.ct.ertclib.dc.core.utils.common.Base64Utils;
import com.ct.ertclib.dc.core.utils.common.JsonUtil;
import com.ct.ertclib.dc.core.utils.logger.Logger;
import com.ct.ertclib.dc.core.utils.common.FileUtils;
import com.ct.ertclib.dc.core.data.model.MiniAppInfo;
import com.ct.ertclib.dc.core.data.miniapp.MiniAppList;
import com.ct.ertclib.dc.feature.testing.socket.DCSocketManager;
import com.newcalllib.datachannel.V1_0.IDCSendDataCallback;
import com.newcalllib.datachannel.V1_0.IImsDCObserver;
import com.newcalllib.datachannel.V1_0.IImsDataChannel;
import com.newcalllib.datachannel.V1_0.ImsDCStatus;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;

public class TestImsDataChannelImpl extends IImsDataChannel.Stub {
    private static final String TAG = "TestImsDataChannelImpl";

    private static final Logger sLogger = Logger.getLogger(TAG);

    private static final String GET_APP_LIST_RSP_HEADERS = "HTTP/1.1 200 OK\r\nContent-Type: application/json\r\nContent-Length: ";

    private static final String STR_RN = "\r\n";

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
        setDcStatus(ImsDCStatus.DC_STATE_OPEN);
        DCSocketManager.INSTANCE.registerMsgObserver(mDcLabel, msg -> {
            try {
                mImsObserver.onMessage(msg, msg.length);
            } catch (RemoteException e) {
                throw new RuntimeException(e);
            }
            return null;
        });
    }

    @Override
    public void unregisterObserver() throws RemoteException {
        sLogger.info("unregisterObserver");
        mImsObserver = null;
    }

    @Override
    public boolean send(byte[] data, int length, IDCSendDataCallback l) throws RemoteException {
//        sLogger.info("send data length = " + length+",mDcStatus="+mDcStatus);
        if (mDcStatus == ImsDCStatus.DC_STATE_OPEN) {
            if (mDcType == DC_TYPE_BDC) {
                return sendBdcData(data, length, l);
            } else if (mDcType == DC_TYPE_ADC) {
                return sendAdcData(data, length, l);
            }
            return true;
        }
        return true;
    }

    private boolean sendAdcData(byte[] data, int length, IDCSendDataCallback callback) {
        DCSocketManager.INSTANCE.sendData(mDcLabel,data);
        // 先简单回一个消息
        try {
            callback.onSendDataResult(20000);
        } catch (Exception e) {
            sLogger.warn("sendAdcData replay", e);
        }
        return true;
    }

    private boolean sendBdcData(byte[] data, int length, IDCSendDataCallback callback) {
        sLogger.info("sendBdcData data="+ Arrays.toString(data));
        try {
            callback.onSendDataResult(20000);
            String request = new String(data, StandardCharsets.UTF_8);
            sLogger.info( "sendBdcData request="+request);
            if (request.contains("applicationlist")) {
                String[] split1 = request.split("applicationlist\\?begin-index=");
                sLogger.info( "sendBdcData split1="+ Arrays.toString(split1));
                String[] split2 = split1[1].split("&app-num=");
                String beginIndex = split2[0];
                String[] split3 = split2[1].split("&sdkVersion=");
                String pageSize = split3[0];
                String[] response = createAppListResponse(Integer.parseInt(beginIndex), Integer.parseInt(pageSize));
                sLogger.debug("sendBdcData response[0]:" + response[0]);
                byte[] bytes = response[0].getBytes();
                mImsObserver.onMessage(bytes, bytes.length);
                sLogger.debug("sendBdcData response[1]" + response[1]);
                byte[] bytes2 = response[1].getBytes();
                mImsObserver.onMessage(bytes2, bytes2.length);
            } else if (request.contains("applications?appid=")) {
                String zipPath = "";
                MiniAppInfo miniAppInfo = null;
                String[] split1 = request.split("applications\\?appid=");
                String[] split2 = split1[1].split("&sdkVersion=");
                String appId = split2[0];
                // 查找appId匹配的zipPath
                String apps = SPUtils.getInstance().getString("TestMiniAppList");
                if (!apps.isEmpty()) {
                    String[] strs = apps.split(",");
                    for (String str : strs) {
                        String[] split = str.split("&zipPath=");
                        String appInfoJsonStr = split[0];
                        miniAppInfo = JsonUtil.INSTANCE.fromJson(Base64Utils.INSTANCE.decodeFromBase64(appInfoJsonStr), MiniAppInfo.class);
                        if (miniAppInfo != null && miniAppInfo.getAppId().equals(appId)){
                            zipPath = split[1];
                            break;
                        }
                    }
                }
                sLogger.info( "sendBdcData zipPath="+zipPath);
                byte[] fileBytes = FileUtils.INSTANCE.getFileBytes(zipPath);
                if (fileBytes == null) {
                    byte[] bytes = "HTTP/1.1 404 not found\r\n\r\n\r\n".getBytes();
                    mImsObserver.onMessage(bytes, bytes.length);
                    return true;
                }
                StringBuilder sb = new StringBuilder("HTTP/1.1 200 OK\r\nContent-Type: application/zip\r\nContent-Length: ");
                sb.append(fileBytes.length);
                sb.append(STR_RN);
                sb.append("etag: " + miniAppInfo.getETag() + "\r\n\r\n");
                byte[] bytes = sb.toString().getBytes();
                mImsObserver.onMessage(bytes, bytes.length);
                mImsObserver.onMessage(fileBytes, fileBytes.length);
            }

        } catch (RemoteException e) {
            sLogger.error(e.getMessage(), e);
        }

        return true;
    }

    private String[] createAppListResponse(int beginIndex, int pageSize) {
        ArrayList<MiniAppInfo> miniAppInfoList = new ArrayList<>();
        // 读取配置的小程序列表
        String apps = SPUtils.getInstance().getString("TestMiniAppList");
        sLogger.info("createAppListResponse apps="+apps);
        if (!apps.isEmpty()) {
            int index = 0;
            String[] strs = apps.split(",");
            for (String str : strs) {
                try {
                    String[] split = str.split("&zipPath=");
                    String appInfoJsonStr = split[0];
                    sLogger.info("createAppListResponse appInfoJsonStr="+appInfoJsonStr);
                    if (index >= beginIndex && index < beginIndex + pageSize){
                        MiniAppInfo info = JsonUtil.INSTANCE.fromJson(Base64Utils.INSTANCE.decodeFromBase64(appInfoJsonStr), MiniAppInfo.class);
                        sLogger.info("createAppListResponse info="+info);
                        miniAppInfoList.add(info);
                    }
                } catch (Exception e){
                    e.printStackTrace();
                }
                index++;
            }
        }
        MiniAppList miniAppList = new MiniAppList(
                miniAppInfoList.size(),
                miniAppInfoList,
                0,
                mTelecomCallId,
                true,
                null,
                miniAppInfoList.size()
        );
        String jsons = GsonUtils.toJson(miniAppList);
        sLogger.info("createAppListResponse jsons="+jsons);

        return new String[]{GET_APP_LIST_RSP_HEADERS + jsons.getBytes().length + STR_RN + STR_RN, jsons};
    }

    @Override
    public void close() throws RemoteException {

        setDcStatus(ImsDCStatus.DC_STATE_CLOSING);
        TestImsDataChannelManager.INSTANCE.close(mDcLabel);
        ThreadUtils.getMainHandler().postDelayed(()->{
            setDcStatus(ImsDCStatus.DC_STATE_CLOSED);
        },1000);
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
