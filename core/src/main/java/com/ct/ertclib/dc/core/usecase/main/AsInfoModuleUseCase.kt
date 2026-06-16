package com.ct.ertclib.dc.core.usecase.main

import android.util.Log
import com.ct.ertclib.dc.core.constants.CommonConstants
import com.ct.ertclib.dc.core.constants.CommonConstants.AS_MODULE_ADLIST_INFO_EVENT
import com.ct.ertclib.dc.core.constants.CommonConstants.AS_MODULE_PLATFORM_INFO_EVENT
import com.ct.ertclib.dc.core.data.common.ASPlatformInfo
import com.ct.ertclib.dc.core.data.miniapp.AppResponse
import com.ct.ertclib.dc.core.data.miniapp.Module
import com.ct.ertclib.dc.core.data.model.AdItem
import com.ct.ertclib.dc.core.miniapp.MiniAppRootADCImpl
import com.ct.ertclib.dc.core.port.common.IMessageCallback
import com.ct.ertclib.dc.core.port.usecase.main.IAsInfoUseCase
import com.ct.ertclib.dc.core.utils.common.JsonUtil
import com.ct.ertclib.dc.core.utils.logger.Logger
import com.newcalllib.datachannel.V1_0.IImsDataChannel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class AsInfoModuleUseCase: IAsInfoUseCase {

    companion object {
        private const val TAG = "AsInfoModuleUseCase"
    }

    private val sLogger: Logger = Logger.getLogger(TAG)

    private var platformInfo: ASPlatformInfo? = null
    private var adListInfo = mutableListOf<AdItem>()
    private val messageCallbackMap = HashMap<String, MutableList<IMessageCallback?>>()

    private var miniAppRootADCImpl: MiniAppRootADCImpl? = null
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    private val listener: MiniAppRootADCImpl.OnADCListener = object : MiniAppRootADCImpl.OnADCListener {
        override fun onMessage(event: String, data: ByteArray, length: Int) {
            val dataString = String(data)
            when (event) {
                AS_MODULE_PLATFORM_INFO_EVENT -> {
                    val platformInfo = JsonUtil.fromJson(dataString, ASPlatformInfo::class.java)
                    platformInfo?.let { info ->
                        messageCallbackMap[event]?.let {
                            for (callback in it) {
                                callback?.reply(
                                    AppResponse(
                                        CommonConstants.APP_RESPONSE_CODE_SUCCESS,
                                        CommonConstants.APP_RESPONSE_MESSAGE_SUCCESS,
                                        mapOf(
                                            AS_MODULE_PLATFORM_INFO_EVENT to platformInfo)
                                    ).toJson()
                                )
                            }
                            it.clear()
                        }
                        messageCallbackMap.remove(event)
                        this@AsInfoModuleUseCase.platformInfo = info
                    }
                }
                AS_MODULE_ADLIST_INFO_EVENT -> {

                    val parsedArray = JsonUtil.fromJson(dataString, Array<AdItem>::class.java)
                    parsedArray?.let { array ->
                        adListInfo.addAll(array)
                        messageCallbackMap[event]?.let {
                            for (callback in it) {
                                callback?.reply(
                                    AppResponse(
                                        CommonConstants.APP_RESPONSE_CODE_SUCCESS,
                                        CommonConstants.APP_RESPONSE_MESSAGE_SUCCESS,
                                        mapOf(
                                            AS_MODULE_ADLIST_INFO_EVENT to adListInfo)
                                    ).toJson()
                                )
                            }
                            it.clear()
                        }
                    }
                }
            }
        }
    }

    override fun init(adcParamsListener: MiniAppRootADCImpl.OnADCParamsOk) {
        miniAppRootADCImpl = MiniAppRootADCImpl(adcParamsListener)
        miniAppRootADCImpl?.registerListener(Module.AS_INFO, listener)
    }

    override fun createDC() {
        scope.launch {
            delay(3000L)
            miniAppRootADCImpl?.createDC()
        }
    }

    override fun release() {
        miniAppRootADCImpl?.unRegisterListener(Module.AS_INFO)
        miniAppRootADCImpl?.release()
        miniAppRootADCImpl = null
    }

    override fun dispatchEvent(
        event: String,
        params: Map<String, Any>,
        iMessageCallback: IMessageCallback?
    ) {
        when (event) {
            AS_MODULE_PLATFORM_INFO_EVENT -> {
                if (platformInfo == null) {
                    miniAppRootADCImpl?.sendData(Module.AS_INFO, AS_MODULE_PLATFORM_INFO_EVENT, "".toByteArray(), object:
                        MiniAppRootADCImpl.OnSendCallback {
                        override fun onSendDataResult(state: Int) {
                            Log.d(TAG, "send callId event, onSendDataResult state: $state")
                        }
                    })
                    if (messageCallbackMap[event] == null) {
                        messageCallbackMap[event] = mutableListOf(iMessageCallback)
                    } else {
                        messageCallbackMap[event]?.add(iMessageCallback)
                    }
                } else {
                    val replyMessage = AppResponse(
                        CommonConstants.APP_RESPONSE_CODE_SUCCESS,
                        CommonConstants.APP_RESPONSE_MESSAGE_SUCCESS,
                        mapOf(
                            AS_MODULE_PLATFORM_INFO_EVENT to platformInfo)
                    ).toJson()
                    iMessageCallback?.reply(replyMessage)
                }
            }
            AS_MODULE_ADLIST_INFO_EVENT -> {
                if (adListInfo.isEmpty()) {
                    sLogger.debug("send adList query data")
                    miniAppRootADCImpl?.sendData(Module.AS_INFO, AS_MODULE_ADLIST_INFO_EVENT, "".toByteArray(), object:
                        MiniAppRootADCImpl.OnSendCallback {
                        override fun onSendDataResult(state: Int) {
                            sLogger.info("send callId event, onSendDataResult state: $state")
                        }
                    })
                    // 存储回调，等 AS 数据返回后调用
                    if (messageCallbackMap[event] == null) {
                        messageCallbackMap[event] = mutableListOf(iMessageCallback)
                    } else {
                        messageCallbackMap[event]?.add(iMessageCallback)
                    }
                } else {
                    // 本地有数据，直接返回
                    val replyMessage = AppResponse(
                        CommonConstants.APP_RESPONSE_CODE_SUCCESS,
                        CommonConstants.APP_RESPONSE_MESSAGE_SUCCESS,
                        mapOf(
                            AS_MODULE_ADLIST_INFO_EVENT to adListInfo)
                    ).toJson()
                    iMessageCallback?.reply(replyMessage)
                }
            }
        }
    }

    override fun onDCCreated(imsDataChannel: IImsDataChannel) {
        miniAppRootADCImpl?.onDCCreated(imsDataChannel)
    }

    override fun sendData(
        module: Module,
        event: String,
        originData: ByteArray,
        onSendCallback: MiniAppRootADCImpl.OnSendCallback
    ) {
        miniAppRootADCImpl?.sendData(module, event, originData, onSendCallback)
    }

    override fun registerRootListener(
        module: Module,
        listener: MiniAppRootADCImpl.OnADCListener
    ) {
        miniAppRootADCImpl?.registerListener(module, listener)
    }

    override fun unRegisterRootListener(module: Module) {
        miniAppRootADCImpl?.unRegisterListener(module)
    }
}