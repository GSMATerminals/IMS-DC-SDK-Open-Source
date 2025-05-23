package com.newcalllib.datachannel.V1_0;

import com.newcalllib.datachannel.V1_0.IImsDataChannel;

interface IImsDataChannelCallback {

    void onBootstrapDataChannelResponse(in IImsDataChannel dc);

    void onApplicationDataChannelResponse(in IImsDataChannel dc);
}
