package com.ct.ertclib.dc.core.port.usecase.main


import com.ct.ertclib.dc.core.data.miniapp.Module
import com.ct.ertclib.dc.core.miniapp.MiniAppRootADCImpl
import com.ct.ertclib.dc.core.port.common.IMessageCallback
import com.newcalllib.datachannel.V1_0.IImsDataChannel

interface IAsInfoUseCase {

    fun init(adcParamsListener: MiniAppRootADCImpl.OnADCParamsOk)

    fun createDC()

    fun release()

    fun dispatchEvent(event: String, params: Map<String, Any>, iMessageCallback: IMessageCallback?)

    fun onDCCreated(imsDataChannel: IImsDataChannel)

    fun sendData(
        module: Module,
        event: String,
        originData: ByteArray,
        onSendCallback: MiniAppRootADCImpl.OnSendCallback
    )

    fun registerRootListener(
        module: Module,
        listener: MiniAppRootADCImpl.OnADCListener
    )

    fun unRegisterRootListener(module: Module)
}