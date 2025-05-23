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

package com.ct.ertclib.dc.core.manager.call;

import android.os.RemoteException;
import android.text.TextUtils;

import androidx.annotation.NonNull;

import com.blankj.utilcode.util.ThreadUtils;
import com.ct.ertclib.dc.core.utils.logger.Logger;
import com.ct.ertclib.dc.core.common.NewCallDatabase;
import com.ct.ertclib.dc.core.port.dao.DcPropertiesDao;
import com.ct.ertclib.dc.core.data.model.DataChannelPropertyEntity;
import com.ct.ertclib.dc.core.miniapp.MiniAppManager;
import com.ct.ertclib.dc.core.port.dc.IDcCreateListener;
import com.newcalllib.datachannel.V1_0.IImsDataChannel;

import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class LocalAppDcManager implements IDcCreateListener {

    private static final String TAG = "LocalAppDcManager";
    private static final Logger sLogger = Logger.getLogger(TAG);

    private static final Object sLock = new Object();

    private String mTelecomCallId;

    public static final int SEND_FILE_DC = 1;

    public static final int SEND_SKETCH_DC = 2;

    public static final int SEND_SCREEN_DC = 3;


    private final Hashtable<String, LocalAppDcCreateListener> mDcCreateListeners = new Hashtable<>();

    private HashMap<String, DataChannelPropertyEntity> mDcProp = new HashMap<>();

    @Override
    public void onDataChannelCreated(@NonNull String telecomCallId, @NonNull String streamId, @NonNull IImsDataChannel imsDataChannel) {
        if (sLogger.isDebugActivated()) {
            sLogger.debug("onDataChannelCreated, telecomCallId:" + telecomCallId + ",streamId:" + streamId);
        }

        if (imsDataChannel == null) {
            sLogger.debug("onDataChannelCreated dc is null");
            return;
        }
        try {
            String dcLabel = imsDataChannel.getDcLabel();
            if (dcLabel == null) {
                sLogger.debug("onDataChannelCreated dcLabel is null");
                return;
            }

            String[] s = dcLabel.split("_");
            if (s != null && s.length >= 4) {
                LocalAppDcCreateListener localAppDcCreateListener = mDcCreateListeners.get(s[3]);
                sLogger.info("local dc manager onDataChannelCreated label:" + s[3] + ",listener:" + localAppDcCreateListener);
                if (localAppDcCreateListener != null) {
                    localAppDcCreateListener.onDataChannelCreated(imsDataChannel);
                    return;
                }
            }
            sLogger.debug("dcLabel is error dcLabel:" + dcLabel);
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    public void release() {
        sLogger.debug("localDcManager release...");
        MiniAppManager miniAppPackageManager = MiniAppManager.Companion.getAppPackageManager(mTelecomCallId);
        if (miniAppPackageManager != null){
            miniAppPackageManager.unregisterAppDataChannelCallbackInternal("fileShare");
//            miniAppPackageManager.unregisterAppDataChannelCallbackInternal("sketchpad");
//            miniAppPackageManager.unregisterAppDataChannelCallbackInternal("screenShare");
        }
        mDcCreateListeners.clear();
        mInstance = null;
    }

    public interface LocalAppDcCreateListener {
        void onDataChannelCreated(IImsDataChannel imsDataChannel);
    }

    private static LocalAppDcManager mInstance;

    public static LocalAppDcManager getInstance() {
        if (mInstance != null) {
            return mInstance;
        }

        synchronized (sLock) {
            if (mInstance == null) {
                mInstance = new LocalAppDcManager();
            }
        }
        return mInstance;
    }

    public void init(String telecomCallId) {
        sLogger.info("local dc manager init dc manager telecomCallId:" + telecomCallId);
        mTelecomCallId = telecomCallId;
        mDcCreateListeners.clear();
        initData();
        MiniAppManager miniAppPackageManager = MiniAppManager.Companion.getAppPackageManager(mTelecomCallId);
        if (miniAppPackageManager != null){
            miniAppPackageManager.registerAppDataChannelCallbackInternal("fileShare", this);
//            miniAppPackageManager.registerAppDataChannelCallbackInternal("sketchpad", this);
//            miniAppPackageManager.registerAppDataChannelCallbackInternal("screenShare", this);
        }
    }

    private void initData() {
        ThreadUtils.getIoPool().submit(new Runnable() {
            @Override
            public void run() {
                DcPropertiesDao dao = NewCallDatabase.Companion.getInstance().dcPropertiesDao();
                List<DataChannelPropertyEntity> all = dao.queryAll();
                mDcProp.clear();
                Map<String, DataChannelPropertyEntity> collect = all.stream().collect(
                        Collectors.toMap(
                                DataChannelPropertyEntity::getAppId,
                                item -> item
                        )
                );
                mDcProp.putAll(collect);
            }
        });
    }

    public void addAdcCreateListener(String dcLabel, LocalAppDcCreateListener appDcCreateListener) {
        mDcCreateListeners.put(dcLabel, appDcCreateListener);
    }

    public synchronized void createAppDataChannels(String[] dcIds) {
        if (dcIds == null || dcIds.length == 0) {
            sLogger.debug("createAppDataChannels dcIds is null");
            return;
        }
        String[] dcLabels = new String[dcIds.length];
        for (int i = 0; i < dcIds.length; i++) {
            dcLabels[i] = "local_fileShare_1_" + dcIds[i];
        }
        sLogger.info("createAppDataChannels, callId:" + mTelecomCallId);
        Objects.requireNonNull(MiniAppManager.Companion.getAppPackageManager(mTelecomCallId)).createApplicationDataChannelsInternal("fileShare", dcLabels, getFileShareAppInfo(dcLabels[0], dcIds[0]));
    }

    public synchronized void createAppDataChannel(int type, String dcId) {
        if (TextUtils.isEmpty(dcId)) {
            sLogger.debug("createAppDataChannel dcIds is null");
            return;
        }

        MiniAppManager miniAppPackageManager = MiniAppManager.Companion.getAppPackageManager(mTelecomCallId);
        if (miniAppPackageManager != null){
            if (type == SEND_FILE_DC) {
                String label = "local_fileShare_1_" + dcId;
                miniAppPackageManager.createApplicationDataChannelsInternal("fileShare", new String[]{label}, getFileShareAppInfo(label, dcId));
            } else if (type == SEND_SKETCH_DC) {
                String label = "local_sketchpad_1_" + dcId;
                miniAppPackageManager.createApplicationDataChannelsInternal("sketchpad", new String[]{label}, getSketchAppInfo(label, dcId));
            } else if (type == SEND_SCREEN_DC) {
                String label = "local_screenShare_1_" + dcId;
                miniAppPackageManager.createApplicationDataChannelsInternal("screenShare", new String[]{label}, getScreenShareAppInfo(label, dcId));
            }
        }
    }

    private String getDcLabel(String appId) {
        DataChannelPropertyEntity property = mDcProp.get(appId);
        if (property != null)
            return property.getDcLabel();
        else
            return "";
    }

    private String getDcId(String appId) {
        DataChannelPropertyEntity property = mDcProp.get(appId);
        if (property != null)
            return property.getDcId();
        else
            return "";
    }

    private String getStreamId(String appId) {
        DataChannelPropertyEntity property = mDcProp.get(appId);
        if (property != null)
            return property.getStreamId();
        else
            return "";
    }

    private String getDcUseCase(String appId) {
        DataChannelPropertyEntity property = mDcProp.get(appId);
        if (property != null)
            return property.getUseCase();
        else
            return "1";
    }

    private String getDcOrder(String appId) {
        DataChannelPropertyEntity property = mDcProp.get(appId);
        if (property != null)
            return property.getOrdered();
        else
            return "";
    }

    private String getDcBandwidth(String appId) {
        DataChannelPropertyEntity property = mDcProp.get(appId);
        if (property != null)
            return property.getBandwidth();
        else
            return "";
    }

    private String getDcQosHint(String appId) {
        DataChannelPropertyEntity property = mDcProp.get(appId);
        if (property != null)
            return property.getQosHint();
        else
            return "";
    }

    private String getDcPriority(String appId) {
        DataChannelPropertyEntity property = mDcProp.get(appId);
        if (property != null)
            return property.getPriority();
        else
            return "";
    }

    private String getDcMaxRetr(String appId) {
        DataChannelPropertyEntity property = mDcProp.get(appId);
        if (property != null)
            return property.getMaxRetr();
        else
            return "";
    }

    private String getDcMaxTime(String appId) {
        DataChannelPropertyEntity property = mDcProp.get(appId);
        if (property != null)
            return property.getMaxTime();
        else
            return "";
    }

    private String getDcAutoAcceptDcSetup(String appId) {
        DataChannelPropertyEntity property = mDcProp.get(appId);
        if (property != null)
            return property.getAutoAcceptDcSetup();
        else
            return "";
    }

    private String getDcSubprotocol(String appId) {
        DataChannelPropertyEntity property = mDcProp.get(appId);
        if (property != null)
            return property.getSubprotocol();
        else
            return "";
    }

    private String getFileShareAppInfo(String dcLabel, String appId) {
        String dcXml = genDcXml("fileShare", dcLabel, appId);
        if (sLogger.isDebugActivated()) {
            sLogger.debug("getFileShareAppInfo" + dcXml);
        }
        return dcXml;
    }

    private String getSketchAppInfo(String dcLabel, String appId) {
        String dcXml = genDcXml("sketchpad", dcLabel, appId);
        if (sLogger.isDebugActivated()) {
            sLogger.debug("getSketchAppInfo" + dcXml);
        }
        return dcXml;
    }

    private String getScreenShareAppInfo(String dcLabel, String appId) {
        String dcXml = genDcXml("screenShare", dcLabel, appId);
        if (sLogger.isDebugActivated()) {
            sLogger.debug("getScreenShareAppInfo" + dcXml);
        }
        return dcXml;
    }

    private String genDcXml(String appId, String dcLabel, String dcId) {
        return "<DataChannelAppInfo>" +
                "<DataChannelApp appId=\"" + appId + "\">" +
                "<DataChannel dcId=\"" + dcId + "\">" +
                "<StreamId>" + getStreamId(appId) + "</StreamId>" +
                "<DcLabel>" + dcLabel + "</DcLabel>" +
                "<UseCase>" + getDcUseCase(appId) + "</UseCase>" +
                "<Subprotocol>" + getDcSubprotocol("screenShare") + "</Subprotocol>" +
                "<Ordered>" + getDcOrder(appId) + "</Ordered>" +
                "<MaxRetr>" + getDcMaxRetr(appId) + "</MaxRetr>" +
                "<MaxTime>" + getDcMaxTime(appId) + "</MaxTime>" +
                "<Priority>" + getDcPriority(appId) + "</Priority>" +
                "<AutoAcceptDcSetup>" + getDcAutoAcceptDcSetup(appId) + "</AutoAcceptDcSetup>" +
                "<Bandwidth>" + getDcBandwidth(appId) + "</Bandwidth>" +
                "<QosHint>" + getDcQosHint(appId) + "</QosHint>" +
                "</DataChannel>" +
                "</DataChannelApp>" +
                "</DataChannelAppInfo>";
    }

    private String genDcXml(String appId) {
        return "<DataChannelAppInfo>" +
                "<DataChannelApp appId=\"" + appId + "\">" +
                "<DataChannel dcId=\"" + getDcId(appId) + "\">" +
                "<StreamId>" + getStreamId(appId) + "</StreamId>" +
                "<DcLabel>" + getDcLabel(appId) + "</DcLabel>" +
                "<UseCase>" + getDcUseCase(appId) + "</UseCase>" +
                "<Subprotocol>" + getDcSubprotocol("screenShare") + "</Subprotocol>" +
                "<Ordered>" + getDcOrder(appId) + "</Ordered>" +
                "<MaxRetr>" + getDcMaxRetr(appId) + "</MaxRetr>" +
                "<MaxTime>" + getDcMaxTime(appId) + "</MaxTime>" +
                "<Priority>" + getDcPriority(appId) + "</Priority>" +
                "<AutoAcceptDcSetup>" + getDcAutoAcceptDcSetup(appId) + "</AutoAcceptDcSetup>" +
                "<Bandwidth>" + getDcBandwidth(appId) + "</Bandwidth>" +
                "<QosHint>" + getDcQosHint(appId) + "</QosHint>" +
                "</DataChannel>" +
                "</DataChannelApp>" +
                "</DataChannelAppInfo>";
    }
}
