package com.ct.ertclib.dc.core.port.usecase.mini

import android.content.Context
import wendu.dsbridge.CompletionHandler

interface ISystemMiniUseCase {

    fun getInformationList(context: Context, params: Map<String, Any>, handler: CompletionHandler<String?>)
}