package com.ct.ertclib.dc.core.miniapp.aidl;

import com.ct.ertclib.dc.core.miniapp.aidl.IDCCallback;
import com.ct.ertclib.dc.core.miniapp.aidl.IParentToMini;
import com.ct.ertclib.dc.core.miniapp.aidl.IMessageCallback;
import com.newcalllib.datachannel.V1_0.IImsDataChannel;
import com.newcalllib.datachannel.V1_0.ImsDCStatus;

interface IMiniToParent {

    int createDC(in String telecomCallId,in String appId, in List<String> lables, in String descrption);
    void registerDCCallBack(in String telecomCallId,in String appId, in IDCCallback idcCallback);
    void registerParentToMiniCallback(in String telecomCallId,in String appId, in IParentToMini iParentToMini);
    void sendMessageToParent(in String telecomCallId,in String appId, in String message,in IMessageCallback iMessageCallback);
    void unregisterDCCallBack(in String telecomCallId,in String appId);
    void unregisterParentToMiniCallback(in String telecomCallId,in String appId);
    void onDataChannelStateChange(in String telecomCallId,in String appId,in IImsDataChannel iImsDataChannel,in ImsDCStatus status, int errCode);
}