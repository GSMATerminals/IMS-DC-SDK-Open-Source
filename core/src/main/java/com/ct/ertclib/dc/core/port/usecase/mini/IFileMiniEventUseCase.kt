/*
 * Copyright 2025-China Telecom Research Institute.
 * All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ct.ertclib.dc.core.port.usecase.mini

import com.ct.ertclib.dc.core.miniapp.ui.webview.CompletionHandler
import com.ct.ertclib.dc.core.miniapp.ui.widget.MiniAppView

interface IFileMiniEventUseCase {

    fun getLocation(miniAppView: MiniAppView, handler: CompletionHandler<String?>)

    fun selectFile(miniAppView: MiniAppView, handler: CompletionHandler<String?>)

    fun selectFiles(miniAppView: MiniAppView,  params: Map<String, Any>, handler: CompletionHandler<String?>)

    fun saveFile(miniAppView: MiniAppView, params: Map<String, Any>, handler: CompletionHandler<String?>)

    fun readFile(miniAppView: MiniAppView, params: Map<String, Any>, handler: CompletionHandler<String?>)

    fun decompressFile(miniAppView: MiniAppView, params: Map<String, Any>, handler: CompletionHandler<String?>)

    fun getFileList(miniAppView: MiniAppView, params: Map<String, Any>): String?

    fun getPrivateFolder(miniAppView: MiniAppView, params: Map<String, Any>): String?

    fun startSaveFile(miniAppView: MiniAppView, params: Map<String, Any>): String?

    fun stopSaveFile(miniAppView: MiniAppView): String?

    fun startReadFile(miniAppView: MiniAppView, params: Map<String, Any>): String?

    fun stopReadFile(miniAppView: MiniAppView): String?

    fun checkFileOrFolderExists(miniAppView: MiniAppView, params: Map<String, Any>): String?

    fun getFileInfo(miniAppView: MiniAppView, params: Map<String, Any>): String?

    fun getFileInfoAsync(miniAppView: MiniAppView, params: Map<String, Any>, handler: CompletionHandler<String?>)

    fun deleteFile(miniAppView: MiniAppView, params: Map<String, Any>): String?

    fun saveUpdateKeyValue(miniAppView: MiniAppView, params: Map<String, Any>): String?

    fun saveUpdateKeyValueWithExpiry(miniAppView: MiniAppView, params: Map<String, Any>): String?

    fun getKeyValue(miniAppView: MiniAppView, params: Map<String, Any>): String?

    fun deleteKeyValue(miniAppView: MiniAppView, params: Map<String, Any>): String?

    fun playVoice(miniAppView: MiniAppView, params: Map<String, Any>): String?

    fun stopPlayVoice(miniAppView: MiniAppView, params: Map<String, Any>): String?

    fun quickSearchFile(miniAppView: MiniAppView, params: Map<String, Any>, handler: CompletionHandler<String?>)

    fun quickSearchFileWithKeyWords(miniAppView: MiniAppView, params: Map<String, Any>, handler: CompletionHandler<String?>)

    fun fileDownload(miniAppView: MiniAppView, params: Map<String, Any>, handler: CompletionHandler<String?>)

    fun getFileListAsync(miniAppView: MiniAppView, params: Map<String, Any>, handler: CompletionHandler<String?>)

    fun getPrivateFolderAsync(miniAppView: MiniAppView, params: Map<String, Any>, handler: CompletionHandler<String?>)

    fun startSaveFileAsync(miniAppView: MiniAppView, params: Map<String, Any>, handler: CompletionHandler<String?>)

    fun stopSaveFileAsync(miniAppView: MiniAppView, handler: CompletionHandler<String?>)

    fun startReadFileAsync(miniAppView: MiniAppView, params: Map<String, Any>, handler: CompletionHandler<String?>)

    fun stopReadFileAsync(miniAppView: MiniAppView, handler: CompletionHandler<String?>)

    fun checkFileOrFolderExistsAsync(miniAppView: MiniAppView, params: Map<String, Any>, handler: CompletionHandler<String?>)

    fun deleteFileAsync(miniAppView: MiniAppView, params: Map<String, Any>, handler: CompletionHandler<String?>)

    fun saveUpdateKeyValueAsync(miniAppView: MiniAppView, params: Map<String, Any>, handler: CompletionHandler<String?>)

    fun saveUpdateKeyValueWithExpiryAsync(miniAppView: MiniAppView, params: Map<String, Any>, handler: CompletionHandler<String?>)

    fun getKeyValueAsync(miniAppView: MiniAppView, params: Map<String, Any>, handler: CompletionHandler<String?>)

    fun deleteKeyValueAsync(miniAppView: MiniAppView, params: Map<String, Any>, handler: CompletionHandler<String?>)

    fun playVoiceAsync(miniAppView: MiniAppView, params: Map<String, Any>, handler: CompletionHandler<String?>)

    fun stopPlayVoiceAsync(miniAppView: MiniAppView, params: Map<String, Any>, handler: CompletionHandler<String?>)

}