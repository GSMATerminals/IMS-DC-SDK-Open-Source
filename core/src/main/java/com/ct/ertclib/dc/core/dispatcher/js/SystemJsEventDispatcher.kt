package com.ct.ertclib.dc.core.dispatcher.js

import com.ct.ertclib.dc.core.constants.MiniAppConstants.FUNCTION_GET_INFORMATION_LIST
import com.ct.ertclib.dc.core.data.bridge.JSRequest
import com.ct.ertclib.dc.core.port.dispatcher.IJsEventDispatcher
import com.ct.ertclib.dc.core.port.usecase.mini.ISystemMiniUseCase
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import com.ct.ertclib.dc.core.miniapp.ui.webview.CompletionHandler
import com.ct.ertclib.dc.core.miniapp.ui.widget.MiniAppView

class SystemJsEventDispatcher : IJsEventDispatcher, KoinComponent {

    private val systemUseCase: ISystemMiniUseCase by inject()

    override fun dispatchAsyncMessage(miniAppView: MiniAppView, request: JSRequest, handler: CompletionHandler<String?>
    ) {
        when (request.function) {
            FUNCTION_GET_INFORMATION_LIST -> {
                systemUseCase.getInformationList(miniAppView, request.params, handler)
            }
        }
    }

    override fun dispatchSyncMessage(miniAppView: MiniAppView, request: JSRequest): String? {
        return ""
    }
}