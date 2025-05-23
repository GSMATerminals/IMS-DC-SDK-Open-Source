package com.ct.ertclib.dc.core.miniapp.aidl;

import com.newcalllib.datachannel.V1_0.IImsDataChannel;

interface IDCCallback {

    void onDcCreated(in String callId, in String streamId, in IImsDataChannel iImsDataChannel);
}