/*
 *   Copyright 2025-China Telecom Research Institute.
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package com.ct.ertclib.dc.core.port.usecase.mini

import android.content.Context
import wendu.dsbridge.CompletionHandler

interface IAppMiniUseCase {

    fun hangup(context: Context): String?

    fun getCallState(context: Context): String?

    fun getMiniAppInfo(context: Context, params: Map<String, Any>, handler: CompletionHandler<String?>)

    fun getSDKInfo(context: Context, params: Map<String, Any>):String

    fun getScreenInfo(context: Context, params: Map<String, Any>):String

    fun startApp(context: Context, params: Map<String, Any>, handler: CompletionHandler<String?>)

    fun setWindow(context: Context, params: Map<String, Any>, handler: CompletionHandler<String?>)

    fun getRemoteNumber(context: Context, handler: CompletionHandler<String?>)

    fun requestStartAdverseApp(context: Context): String?

    fun addOrEditContact(context: Context, params: Map<String, Any>): String

    fun getContactName(context: Context, params: Map<String, Any>): String

    fun getContactList(context: Context, params: Map<String, Any>,handler: CompletionHandler<String?>)

    fun setSystemApiLicense(context: Context, params: Map<String, Any>): String

    fun openWeb(context: Context, params: Map<String, Any>): String

    fun getHttpResult(params: Map<String, Any>, handler: CompletionHandler<String?>)

    fun moveToFront(): String

    fun stopApp(): String

    fun getShareTypeName(context: Context, params: Map<String, Any>): String

    fun playDtmfTone(context: Context, params: Map<String, Any>): String

    fun setSpeakerphone(context: Context, params: Map<String, Any>): String

    fun isSpeakerphoneOn(context: Context, params: Map<String, Any>,handler: CompletionHandler<String?>)

    fun setMuted(context: Context, params: Map<String, Any>): String

    fun isMuted(context: Context, params: Map<String, Any>,handler: CompletionHandler<String?>)
}