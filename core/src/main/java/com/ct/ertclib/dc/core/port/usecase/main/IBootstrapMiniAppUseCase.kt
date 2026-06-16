package com.ct.ertclib.dc.core.port.usecase.main

import com.newcalllib.datachannel.V1_0.IImsDataChannel

interface IBootstrapMiniAppUseCase {
    fun onDataChannelCreated(imsDataChannel: IImsDataChannel)
    fun containsDCLabel(label: String): Boolean
    fun getBootstrapDC(label: String): IImsDataChannel?
    fun release()
}