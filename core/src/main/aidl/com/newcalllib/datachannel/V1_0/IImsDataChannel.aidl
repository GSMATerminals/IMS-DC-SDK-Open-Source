package com.newcalllib.datachannel.V1_0;

import com.newcalllib.datachannel.V1_0.IDCSendDataCallback;
import com.newcalllib.datachannel.V1_0.IImsDCObserver;
import com.newcalllib.datachannel.V1_0.ImsDCStatus;

interface IImsDataChannel {

    void registerObserver(in IImsDCObserver l);

    void unregisterObserver();

    boolean send(in byte[] data, int length, in IDCSendDataCallback l);

    void close();

    String getDcLabel();

    String getSubProtocol();

    long bufferedAmount();

    int getDCType();

    ImsDCStatus getState();

    String getTelecomCallId();

    String getStreamId();

    String getPhoneNumber();
}