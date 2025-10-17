package com.ct.ertclib.dc.core.dispatcher.js

import android.content.Context
import com.ct.ertclib.dc.core.constants.MiniAppConstants.FUNCTION_FILE_DOWNLOAD
import com.ct.ertclib.dc.core.constants.MiniAppConstants.FUNCTION_GET_INFORMATION_LIST
import com.ct.ertclib.dc.core.data.bridge.JSRequest
import com.ct.ertclib.dc.core.port.dispatcher.IJsEventDispatcher
import com.ct.ertclib.dc.core.port.usecase.mini.ISystemMiniUseCase
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import wendu.dsbridge.CompletionHandler

class SystemJsEventDispatcher : IJsEventDispatcher, KoinComponent {

    private val systemUseCase: ISystemMiniUseCase by inject()

    override fun dispatchAsyncMessage(
        context: Context,
        request: JSRequest,
        handler: CompletionHandler<String?>
    ) {
        when (request.function) {
            FUNCTION_GET_INFORMATION_LIST -> {
                systemUseCase.getInformationList(context, request.params, handler)
            }
        }
    }

    override fun dispatchSyncMessage(context: Context, request: JSRequest): String? {
        return ""
    }
}