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

import com.ct.ertclib.dc.core.miniapp.ui.webview.CompletionHandler
import com.ct.ertclib.dc.core.miniapp.ui.widget.MiniAppView

interface IScreenShareMiniUseCase {

    fun startScreenShare(miniAppView: MiniAppView,params: Map<String, Any>, handler: CompletionHandler<String?>)

    fun stopScreenShare(miniAppView: MiniAppView): String

    fun requestScreenShareAbility(miniAppView: MiniAppView,handler: CompletionHandler<String?>)

    fun openSketchBoard(miniAppView: MiniAppView,params: Map<String, Any>): String

    fun closeSketchBoard(miniAppView: MiniAppView,): String

    fun addDrawingInfo(miniAppView: MiniAppView,params: Map<String, Any>): String

    fun addRemoteSizeInfo(miniAppView: MiniAppView,params: Map<String, Any>): String

    fun setPrivacyMode(miniAppView: MiniAppView,params: Map<String, Any>): String

    fun addRemoteWindowSizeInfo(miniAppView: MiniAppView,params: Map<String, Any>): String

    fun stopScreenShareAsync(miniAppView: MiniAppView, handler: CompletionHandler<String?>)

    fun openSketchBoardAsync(miniAppView: MiniAppView,params: Map<String, Any>, handler: CompletionHandler<String?>)

    fun closeSketchBoardAsync(miniAppView: MiniAppView,handler: CompletionHandler<String?>)

    fun addDrawingInfoAsync(miniAppView: MiniAppView,params: Map<String, Any>, handler: CompletionHandler<String?>)

    fun addRemoteSizeInfoAsync(miniAppView: MiniAppView,params: Map<String, Any>, handler: CompletionHandler<String?>)

    fun setPrivacyModeAsync(miniAppView: MiniAppView,params: Map<String, Any>, handler: CompletionHandler<String?>)

    fun addRemoteWindowSizeInfoAsync(miniAppView: MiniAppView,params: Map<String, Any>, handler: CompletionHandler<String?>)
}