package com.ct.ertclib.dc.core.port.usecase.mini

import com.ct.ertclib.dc.core.miniapp.ui.webview.CompletionHandler
import com.ct.ertclib.dc.core.miniapp.ui.widget.MiniAppView

interface ISystemMiniUseCase {

    fun getInformationList(miniAppView: MiniAppView, params: Map<String, Any>, handler: CompletionHandler<String?>)
}