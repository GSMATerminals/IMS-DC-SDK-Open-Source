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

package com.ct.ertclib.dc.feature.testing;

import android.os.RemoteException;
import android.text.TextUtils;

import com.blankj.utilcode.util.GsonUtils;
import com.blankj.utilcode.util.SPUtils;
import com.blankj.utilcode.util.ThreadUtils;
import com.blankj.utilcode.util.Utils;
import com.ct.ertclib.dc.core.utils.logger.Logger;
import com.ct.ertclib.dc.core.utils.common.FileUtils;
import com.ct.ertclib.dc.core.common.PathManager;
import com.ct.ertclib.dc.core.data.model.MiniAppInfo;
import com.ct.ertclib.dc.core.data.common.FileMessage;
import com.ct.ertclib.dc.core.data.message.FileRequest;
import com.ct.ertclib.dc.core.data.message.FileResponse;
import com.ct.ertclib.dc.core.data.message.IMessage;
import com.ct.ertclib.dc.core.data.message.MessageType;
import com.ct.ertclib.dc.core.data.miniapp.MiniAppList;
import com.ct.ertclib.dc.core.data.miniapp.MiniAppStatus;
import com.ct.ertclib.dc.feature.testing.socket.SocketNetworkManager;
import com.newcalllib.datachannel.V1_0.IDCSendDataCallback;
import com.newcalllib.datachannel.V1_0.IImsDCObserver;
import com.newcalllib.datachannel.V1_0.IImsDataChannel;
import com.newcalllib.datachannel.V1_0.ImsDCStatus;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;

public class TestImsDataChannelImpl extends IImsDataChannel.Stub {
    private static final String TAG = "TestNewCall";

    private static final Logger sLogger = Logger.getLogger(TAG);

    private static final String GET_APP_LIST_RSP_HEADERS = "HTTP/1.1 200 OK\r\nContent-Type: application/json\r\nContent-Length: ";

    private static final String STR_RN = "\r\n";

    private INetworkManager mSocketNetworkManager;

    public static final int DC_TYPE_ADC = 2;
    public static final int DC_TYPE_BDC = 1;

    public TestImsDataChannelImpl() {
        mPath = new PathManager().getInternalCacheDirPath(Utils.getApp());
        if (SPUtils.getInstance().getBoolean("mock_socket", false)) {
            mSocketNetworkManager = SocketNetworkManager.Companion.getINSTANCE();
        }
    }

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
        mDcStatus = status;
        try {
            if (mImsObserver != null) {
                mImsObserver.onDataChannelStateChange(status, 0);
            }
        } catch (RemoteException e) {
            sLogger.error(e.getMessage(), e);
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
    }

    @Override
    public void unregisterObserver() throws RemoteException {
        sLogger.info("unregisterObserver");
        mImsObserver = null;
    }

    @Override
    public boolean send(byte[] data, int length, IDCSendDataCallback l) throws RemoteException {
        sLogger.info("send data length = " + length+",mDcStatus="+mDcStatus);
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
        //TODO 通过socket发送数据
        String s = new String(data, StandardCharsets.UTF_8);
        sLogger.info("sendAdcData data[" + s + "]");
//        if (SPUtils.getInstance().getBoolean("mock_socket", false)) {
//            mSocketNetworkManager.sendByte(data);
//        }

        // 先简单回一个消息
        try {
            callback.onSendDataResult(2000);

            byte[] bytes = ("收到你发的" + s + "啦！！！").getBytes(StandardCharsets.UTF_8);
            mImsObserver.onMessage(bytes, bytes.length);
        } catch (Exception e) {
            sLogger.warn("sendAdcData replay", e);
        }
        return true;
    }

    private void handlerResponse(JSONObject jsonObject) {

    }

    private FileOutputStream mFos = null;
    private FileMessage mFileMessage = null;

    private String mPath;

    private long mCount = 0;



    private void checkAndCreateDir(String path) {
        File file = new File(path);
        if (!file.exists() || !file.isDirectory()) {
            boolean mkdirsResult = file.mkdirs();
            sLogger.info("checkAndCreateDir mkdir filepath:" + path + " result:" + mkdirsResult);
        }
    }
    private void parseMessage(byte[] data, int length) {
        String mBoundary = "";
        String content = new String(data, StandardCharsets.UTF_8);
//        if (sLogger.isDebugActivated()) {
//            sLogger.debug("parseMessage, content:[" + content + "]");
//        }




        /* file info byte64
         */

        /* foot
        --bd017fda-d6df-4f7e-9dfb-14ca864e7111--
         */

        /* request header 收到的第一个消息
        POST http://www.test.com HTTP/1.1 \r\n
        Host: www.test.com \r\n
        Connection: keep-alive \r\n
        Content-Type: multipart/form-data; boundary=bd017fda-d6df-4f7e-9dfb-14ca864e7111 \r\n
        Content-Length: 3970929 \r\n \r\n */



        boolean isContainMultiPart = content.contains("Content-Type: multipart/form-data;");
        boolean isNotReceiveEnd ;
        if (isContainMultiPart) {
            String[] requestHeaders = content.split("\r\n");
            sLogger.info("multipart size:" + requestHeaders.length);

            for (int i = 0; i < requestHeaders.length; i++) {
                String split = requestHeaders[i];
                if (split.contains("boundary=")) {
                    mBoundary = split.substring(split.indexOf("boundary=") + 9);
                    break;
                }
            }
        }

        if (sLogger.isDebugActivated()) {
            sLogger.debug("mBoundary:"+mBoundary);
        }

        if (content.contains("Content-Disposition:")) {
        /* part header 收到的第二个消息
        --bd017fda-d6df-4f7e-9dfb-14ca864e7111 \r\n
        Content-Disposition: form-data; name="15388916503_15388916503_4d392ee9-c1fb-45ff-aeb4-e4a56eb2821a,15388916503,15388916503,F240E5CE3A81D5AEA42C018150FD12DE,0,3970565,3970565"; filename="IMG_20231028110641691.jpg" \r\n
        Content-Type: image/jpeg; charset=utf-8 \r\n
        Content-Length: 3970565 \r\n\r\n
         */
            String[] partHeaders = content.split("\r\n");
            sLogger.info("partHeaders size:" + partHeaders.length);

            int index = 0;
            while (index < partHeaders.length) {
                String partHeader = partHeaders[index];
                sLogger.info("parseMessage index=" + index+",partHeader:["+partHeader+"]");
                if (partHeader.contains("Content-Disposition:")) {
                    String[] split = partHeader.split(";");
                    String nameSplit = split[1];
//                    nameSplit： name="15388916503_15388916503_4d392ee9-c1fb-45ff-aeb4-e4a56eb2821a,15388916503,15388916503,F240E5CE3A81D5AEA42C018150FD12DE,0,3970565,3970565";
                    String nameStr = nameSplit.substring(nameSplit.indexOf("name=") + 6, nameSplit.lastIndexOf("\""));
                    if (sLogger.isDebugActivated()) {
                        sLogger.debug("nameStr:" + nameStr);
                    }

                    String[] infoSplit = nameStr.split(",");
                    String idStr = infoSplit[0];
                    String fromStr = infoSplit[1];
                    String toStr = infoSplit[2];
                    String md5Str = infoSplit[3];
                    String startStr = infoSplit[4];
                    String endStr = infoSplit[5];
                    String lengthStr = infoSplit[6];


                    String fileNameSplit = split[2];
                    String fileNameStr = fileNameSplit.substring(fileNameSplit.indexOf("filename=") + 10, fileNameSplit.lastIndexOf("\""));
                    if (sLogger.isDebugActivated()) {
                        sLogger.debug("fileNameStr:" + fileNameStr);
                    }
                    try {
                        if (mFos == null) {
                            checkAndCreateDir(mPath);
                            if (com.blankj.utilcode.util.FileUtils.isFileExists(mPath + "/" + fileNameStr)) {
                                String fileName = fileNameStr.substring(0, fileNameStr.lastIndexOf("."));
                                String fileType = fileNameStr.substring(fileNameStr.lastIndexOf("."));
                                fileNameStr = fileName + "_" + new SimpleDateFormat("yyyyMMdd_HHmmss").format(Long.valueOf(System.currentTimeMillis())) + fileType;
                            }
                            if (sLogger.isDebugActivated()) {
                                sLogger.debug("outPutStream mPath:["+mPath+"]"+",fileNameStr:"+fileNameStr);
                            }
                            mFos = new FileOutputStream(mPath + "/" + fileNameStr);
                        }
                    } catch (Exception e) {
                        sLogger.error("fileOutStream mpath:" + mPath + ",filename:" + fileNameStr, e);
                    }

                    if (mFileMessage == null) {
                        mFileMessage = new FileMessage();
                    }
                    mFileMessage.from = fromStr;
                    mFileMessage.to = toStr;
                    mFileMessage.md5 = md5Str;
                    mFileMessage.name = fileNameStr;
                    mFileMessage.path = mPath + "/" + fileNameStr;
                    try {
                        mFileMessage.size = Long.parseLong(lengthStr);
                    } catch (Exception e) {
                        sLogger.error("parseLong lengthStr:" + lengthStr, e);
                        mFileMessage.size = 0;
                    }

                } else if (partHeader.contains("Content-Length:")) {
                    String contentLengthStr = partHeader.substring(partHeader.indexOf("Content-Length:") + 15).trim();
                    long contentLength = -1;
                    try {
                        contentLength = Long.parseLong(contentLengthStr);
                    } catch (Exception e) {
                        sLogger.error("parseMessage contentLengthStr:" + contentLengthStr);
                    }
                    sLogger.debug("parseMessage contentLengthStr=" + contentLengthStr + ",contentLength:" + contentLength);
                } else if (partHeader.contains("Content-Type:")) {
                    String contentTypeStr = partHeader.substring(partHeader.indexOf("Content-Type:") + 13).trim();
                    if (mFileMessage == null) {
                        mFileMessage = new FileMessage();
                    }
                    if (contentTypeStr.startsWith("image")) {
                        mFileMessage.type = MessageType.TYPE_IMAGE;
                    } else if (contentTypeStr.startsWith("video")) {
                        mFileMessage.type = MessageType.TYPE_VIDEO;
                    } else {
                        mFileMessage.type = MessageType.TYPE_FILE;
                    }
                    mFileMessage.contentType = contentTypeStr;
                    mCount = 0;
//                    onReceiveStart(mFileMessage);
                    sLogger.info("onReceiveStart:"+mFileMessage);
                }
                index++;
            }
            isContainMultiPart = true;
        }

        if (content.contains("--" + mBoundary + "--")) {
            try {
                if (mFos != null) {
                    mFos.close();
                }
            } catch (Exception e) {
                sLogger.error("parseMessage close fos", e);
            }
            sLogger.debug("parseMessage end:" + mCount);
//            onReceiveComplete(mFileMessage);
            sLogger.info("onReceiveComplete:"+mFileMessage);
            mCount = 0;
            isNotReceiveEnd = true;
        } else {
            isNotReceiveEnd = isContainMultiPart;
        }

        if (!isNotReceiveEnd) {
            try {
                if (mFos != null) {
//                    byte[] decode = Base64.decode(data, 0);
                    mCount += data.length;
                    mFos.write(data);
//                    onReceiveProgress(mFileMessage, mCount);
                    sLogger.info("onReceiveProgress mConunt:"+mCount+",fileMessage"+mFileMessage);

                }
            } catch (Exception e) {
                sLogger.error("parseMessage write decode", e);
            }
        }
    }

    private void handlerRequest(JSONObject jsonObject) {
        try {
            String id = jsonObject.getString(IMessage.KEY_ID);
            long timeStamp = jsonObject.getLong(IMessage.KEY_TIMESTAMP);
            String from = null;
            if (jsonObject.has("from")) {
                from = jsonObject.getString("from");
            }
            String to = null;
            if (jsonObject.has("to")) {
                to = jsonObject.getString("to");
            }
            sLogger.info("YBY from:"+from+",to:"+to);

            JSONObject data = jsonObject.getJSONObject("data");
            String request = data.getString("request");
            if (request.equals("file")) {
                FileRequest fileRequest = new FileRequest();
                fileRequest.uuId = id;
                fileRequest.timestamp = timeStamp;
                fileRequest.from = from;
                fileRequest.to = to;
                JSONArray list = data.getJSONArray("list");
                if (list != null && list.length() > 0) {
                    for (int i = 0; i < list.length(); i++) {
                        JSONObject json = list.getJSONObject(i);
                        FileRequest.FileRequestInfo info = new FileRequest.FileRequestInfo();
                        info.name = json.getString("name");
                        info.md5 = json.getString("md5");
                        info.size = json.getLong("size");
                        fileRequest.addFileRequestInfo(info);
                    }
                }
                replyRequest(fileRequest,true);
            }

        } catch (Exception e) {
            sLogger.error("handlerRequest", e);
        }

    }

    private void replyRequest(FileRequest fileRequest, boolean isAccept) {
        FileResponse response = new FileResponse(1);
        response.accept = isAccept;
        for (FileRequest.FileRequestInfo info : fileRequest.fileInfoList) {
            if (info != null) {
                FileResponse.FileResponseInfo responseInfo = new FileResponse.FileResponseInfo();
                responseInfo.name = info.name;
                responseInfo.start = 0;
                responseInfo.end = info.size;
                response.addResponseInfo(responseInfo);
            }
        }
        String message = response.parseJsonString();
//        String message = fileRequest.getJsonString();
        sLogger.info("YBY reply request message:"+message);

        byte[] bytes = message.getBytes(StandardCharsets.UTF_8);

        try {
            mImsObserver.onMessage(bytes, bytes.length);
        } catch (Exception e) {
            sLogger.warn("sendAdcData replay", e);
        }
    }

    private boolean sendBdcData(byte[] data, int length, IDCSendDataCallback callback) {
        sLogger.info("sendBdcData data="+ Arrays.toString(data));
        try {
            callback.onSendDataResult(20000);
            String request = new String(data, StandardCharsets.UTF_8);
            sLogger.info( "sendBdcData request="+request);
            if (request.contains("applicationlist")) {
                String[] response = createAppListResponse();
                sLogger.debug("sendBdcData response[0]:" + response[0]);
                byte[] bytes = response[0].getBytes();
                mImsObserver.onMessage(bytes, bytes.length);
                sLogger.debug("sendBdcData response[1]" + response[1]);
                byte[] bytes2 = response[1].getBytes();
                mImsObserver.onMessage(bytes2, bytes2.length);
            } else if (request.contains("applications?appid=")) {
                String appPath = SPUtils.getInstance().getString("appPath");
                sLogger.info( "sendBdcData appPath="+appPath);
                byte[] fileBytes = FileUtils.INSTANCE.getFileBytes(appPath);
                if (fileBytes == null) {
                    byte[] bytes = "HTTP/1.1 404 not found\r\n\r\n\r\n".getBytes();
                    mImsObserver.onMessage(bytes, bytes.length);
                    return true;
                }
                StringBuilder sb = new StringBuilder("HTTP/1.1 200 OK\r\nContent-Type: application/zip\r\nContent-Length: ");
                sb.append(fileBytes.length);
                sb.append(STR_RN);
                sb.append("etag: " + SPUtils.getInstance().getString("eTag") + "\r\n\r\n");
                byte[] bytes = sb.toString().getBytes();
                mImsObserver.onMessage(bytes, bytes.length);
                mImsObserver.onMessage(fileBytes, fileBytes.length);
            }

        } catch (RemoteException e) {
            sLogger.error(e.getMessage(), e);
        }

        return true;
    }

    private String[] createAppListResponse() {

        ArrayList<MiniAppInfo> miniAppInfoList = new ArrayList<>();

        MiniAppInfo miniAppInfo = new MiniAppInfo(
                "fileShare",
                "内容分享",
                null,
                false,
                false,
                mTelecomCallId,
                "",
                false,
                false,
                "123456",
                "/storage/emulated/0/Android/data/com.ct.ertclib.dc.debug/files/dist",
                "INCALL",
                "1234",
                null,
                1,
                1,
                MiniAppStatus.INSTALLED,
                false,
                null
        );
        miniAppInfoList.add(miniAppInfo);

        MiniAppInfo miniAppInfo2 = new MiniAppInfo(
                "sketchpad",
                "标记画图",
                null,
                false,
                false,
                mTelecomCallId,
                "",
                false,
                false,
                "123456",
                "/storage/emulated/0/Android/data/com.ct.ertclib.dc.debug/files/dist",
                "INCALL",
                "1234",
                null,
                1,
                1,
                MiniAppStatus.INSTALLED,
                false,
                null
        );
        miniAppInfoList.add(miniAppInfo2);



        if (!TextUtils.isEmpty(SPUtils.getInstance().getString("appPath"))) {
            MiniAppInfo miniAppInfo3 = new MiniAppInfo(
                    SPUtils.getInstance().getString("appId"),
                    SPUtils.getInstance().getString("appName"),
                    null,
                    false,
                    true,
                    mTelecomCallId,
                    System.currentTimeMillis()+"",
                    false,
                    false,
                    null,
                    null,
                    "INCALL",
                    "1234",
                    null,
                    1,
                    1,
                    MiniAppStatus.UNINSTALLED,
                    false,
                    null
            );
            miniAppInfoList.add(miniAppInfo3);
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
