/*
 *   Copyright 2025-China Telecom Research Institute.
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *        https://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package com.ct.ertclib.dc.core.port.usecase.mini

import android.content.Context
import com.ct.ertclib.dc.core.miniapp.ui.webview.CompletionHandler
import com.ct.ertclib.dc.core.miniapp.ui.widget.MiniAppView

interface IAppMiniUseCase {

    fun hangup(miniAppView: MiniAppView): String?

    fun getCallState(miniAppView: MiniAppView): String?

    fun getMiniAppInfo(miniAppView: MiniAppView, params: Map<String, Any>, handler: CompletionHandler<String?>)

    fun getSDKInfo(miniAppView: MiniAppView, params: Map<String, Any>):String

    fun getScreenInfo(miniAppView: MiniAppView, params: Map<String, Any>):String

    fun startApp(miniAppView: MiniAppView, params: Map<String, Any>, handler: CompletionHandler<String?>)

    fun setWindow(miniAppView: MiniAppView, params: Map<String, Any>, handler: CompletionHandler<String?>)

    fun getRemoteNumber(miniAppView: MiniAppView, handler: CompletionHandler<String?>)

    fun requestStartAdverseApp(miniAppView: MiniAppView): String?

    fun addOrEditContact(miniAppView: MiniAppView, params: Map<String, Any>): String

    fun getContactName(miniAppView: MiniAppView, params: Map<String, Any>): String

    fun getContactList(miniAppView: MiniAppView, params: Map<String, Any>,handler: CompletionHandler<String?>)

    fun setSystemApiLicense(miniAppView: MiniAppView, params: Map<String, Any>): String

    fun openWeb(miniAppView: MiniAppView, params: Map<String, Any>): String

    fun getHttpResult(params: Map<String, Any>, handler: CompletionHandler<String?>)

    fun moveToFront(miniAppView: MiniAppView): String

    fun stopApp(miniAppView: MiniAppView): String

    fun getShareTypeName(miniAppView: MiniAppView, params: Map<String, Any>): String

    fun playDtmfTone(miniAppView: MiniAppView, params: Map<String, Any>): String

    fun setSpeakerphone(miniAppView: MiniAppView, params: Map<String, Any>): String

    fun isSpeakerphoneOn(miniAppView: MiniAppView, params: Map<String, Any>,handler: CompletionHandler<String?>)

    fun setMuted(miniAppView: MiniAppView, params: Map<String, Any>): String

    fun isMuted(miniAppView: MiniAppView, params: Map<String, Any>,handler: CompletionHandler<String?>)

    fun answer(miniAppView: MiniAppView): String?

    fun hangupAsync(miniAppView: MiniAppView,handler: CompletionHandler<String?>)

    fun getCallStateAsync(miniAppView: MiniAppView,handler: CompletionHandler<String?>)

    fun getSDKInfoAsync(miniAppView: MiniAppView, params: Map<String, Any>,handler: CompletionHandler<String?>)

    fun getScreenInfoAsync(miniAppView: MiniAppView, params: Map<String, Any>,handler: CompletionHandler<String?>)

    fun requestStartAdverseAppAsync(miniAppView: MiniAppView,handler: CompletionHandler<String?>)

    fun addOrEditContactAsync(miniAppView: MiniAppView, params: Map<String, Any>,handler: CompletionHandler<String?>)

    fun getContactNameAsync(miniAppView: MiniAppView, params: Map<String, Any>,handler: CompletionHandler<String?>)

    fun setSystemApiLicenseAsync(miniAppView: MiniAppView, params: Map<String, Any>,handler: CompletionHandler<String?>)

    fun openWebAsync(miniAppView: MiniAppView, params: Map<String, Any>,handler: CompletionHandler<String?>)


    fun moveToFrontAsync(miniAppView: MiniAppView,handler: CompletionHandler<String?>)

    fun stopAppAsync(miniAppView: MiniAppView,handler: CompletionHandler<String?>)

    fun getShareTypeNameAsync(miniAppView: MiniAppView, params: Map<String, Any>,handler: CompletionHandler<String?>)

    fun playDtmfToneAsync(miniAppView: MiniAppView, params: Map<String, Any>,handler: CompletionHandler<String?>)

    fun setSpeakerphoneAsync(miniAppView: MiniAppView, params: Map<String, Any>,handler: CompletionHandler<String?>)

    fun setMutedAsync(miniAppView: MiniAppView, params: Map<String, Any>,handler: CompletionHandler<String?>)

    fun answerAsync(miniAppView: MiniAppView,handler: CompletionHandler<String?>)

    fun getPlatformInfo(miniAppView: MiniAppView, params: Map<String, Any>, handler: CompletionHandler<String?>)
}