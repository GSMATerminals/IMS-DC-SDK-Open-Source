package com.newcalllib.datachannel.V1_0;

import com.newcalllib.datachannel.V1_0.ImsDCStatus;

interface IImsDCObserver {

    void onDataChannelStateChange(in ImsDCStatus status, int errCode);

    void onMessage(in byte[] data, int length);
}