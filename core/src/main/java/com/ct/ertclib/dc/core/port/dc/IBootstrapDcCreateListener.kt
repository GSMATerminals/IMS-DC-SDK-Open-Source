package com.ct.ertclib.dc.core.port.dc

import com.newcalllib.datachannel.V1_0.IImsDataChannel

interface IBootstrapDcCreateListener {

    fun onBootstrapAppDataChannelCreated(
        telecomCallId: String,
        appId: String,
        streamId: String,
        imsDataChannel: IImsDataChannel
    )
}