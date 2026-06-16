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

package com.ct.ertclib.dc.core.dispatcher.js

import com.ct.ertclib.dc.core.constants.MiniAppConstants.FUNCTION_CHECK_FILE_EXISTS
import com.ct.ertclib.dc.core.constants.MiniAppConstants.FUNCTION_DECOMPRESS_FILE
import com.ct.ertclib.dc.core.constants.MiniAppConstants.FUNCTION_DELETE_FILE
import com.ct.ertclib.dc.core.constants.MiniAppConstants.FUNCTION_DELETE_KEY_VALUE
import com.ct.ertclib.dc.core.constants.MiniAppConstants.FUNCTION_FILE_DOWNLOAD
import com.ct.ertclib.dc.core.constants.MiniAppConstants.FUNCTION_GET_FILE_INFO
import com.ct.ertclib.dc.core.constants.MiniAppConstants.FUNCTION_GET_FILE_INFO_ASYNC
import com.ct.ertclib.dc.core.constants.MiniAppConstants.FUNCTION_GET_FILE_LIST
import com.ct.ertclib.dc.core.constants.MiniAppConstants.FUNCTION_GET_KEY_VALUE
import com.ct.ertclib.dc.core.constants.MiniAppConstants.FUNCTION_GET_LOCATION
import com.ct.ertclib.dc.core.constants.MiniAppConstants.FUNCTION_GET_PRIVATE_FOLDER
import com.ct.ertclib.dc.core.constants.MiniAppConstants.FUNCTION_PLAY_VOICE
import com.ct.ertclib.dc.core.constants.MiniAppConstants.FUNCTION_READ_FILE
import com.ct.ertclib.dc.core.constants.MiniAppConstants.FUNCTION_SAVE_FILE
import com.ct.ertclib.dc.core.constants.MiniAppConstants.FUNCTION_SAVE_UPDATE_KEY_VALUE
import com.ct.ertclib.dc.core.constants.MiniAppConstants.FUNCTION_SAVE_UPDATE_KEY_VALUE_WITH_EXPIRY
import com.ct.ertclib.dc.core.constants.MiniAppConstants.FUNCTION_QUICK_SEARCH_FILE
import com.ct.ertclib.dc.core.constants.MiniAppConstants.FUNCTION_QUICK_SEARCH_KEY_WORDS
import com.ct.ertclib.dc.core.constants.MiniAppConstants.FUNCTION_SELECT_FILE
import com.ct.ertclib.dc.core.constants.MiniAppConstants.FUNCTION_SELECT_FILES
import com.ct.ertclib.dc.core.constants.MiniAppConstants.FUNCTION_START_READ_FILE
import com.ct.ertclib.dc.core.constants.MiniAppConstants.FUNCTION_START_SAVE_FILE
import com.ct.ertclib.dc.core.constants.MiniAppConstants.FUNCTION_STOP_PLAY_VOICE
import com.ct.ertclib.dc.core.constants.MiniAppConstants.FUNCTION_STOP_READ_FILE
import com.ct.ertclib.dc.core.constants.MiniAppConstants.FUNCTION_STOP_SAVE_FILE
import com.ct.ertclib.dc.core.data.bridge.JSRequest
import com.ct.ertclib.dc.core.port.dispatcher.IJsEventDispatcher
import com.ct.ertclib.dc.core.port.usecase.mini.IFileMiniEventUseCase
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import com.ct.ertclib.dc.core.miniapp.ui.webview.CompletionHandler
import com.ct.ertclib.dc.core.miniapp.ui.widget.MiniAppView

class FileJsEventDispatcher : IJsEventDispatcher, KoinComponent {

    private val fileEventUseCase: IFileMiniEventUseCase by inject()

    override fun dispatchAsyncMessage(miniAppView: MiniAppView, request: JSRequest, handler: CompletionHandler<String?>) {
        when (request.function) {
            FUNCTION_GET_LOCATION -> { fileEventUseCase.getLocation(miniAppView, handler) }
            FUNCTION_SELECT_FILE -> { fileEventUseCase.selectFile(miniAppView, handler) }
            FUNCTION_SELECT_FILES -> { fileEventUseCase.selectFiles(miniAppView, request.params, handler) }
            FUNCTION_SAVE_FILE -> { fileEventUseCase.saveFile(miniAppView, request.params, handler) }
            FUNCTION_READ_FILE -> { fileEventUseCase.readFile(miniAppView, request.params, handler) }
            FUNCTION_DECOMPRESS_FILE -> { fileEventUseCase.decompressFile(miniAppView, request.params, handler) }
            FUNCTION_GET_FILE_INFO_ASYNC -> { fileEventUseCase.getFileInfoAsync(miniAppView, request.params, handler) }
            FUNCTION_QUICK_SEARCH_FILE -> { fileEventUseCase.quickSearchFile(miniAppView,request.params,  handler) }
            FUNCTION_QUICK_SEARCH_KEY_WORDS -> { fileEventUseCase.quickSearchFileWithKeyWords(miniAppView, request.params, handler) }
            FUNCTION_FILE_DOWNLOAD -> { fileEventUseCase.fileDownload(miniAppView, request.params, handler) }

            FUNCTION_GET_FILE_LIST -> { fileEventUseCase.getFileListAsync(miniAppView, request.params, handler) }
            FUNCTION_GET_PRIVATE_FOLDER -> { fileEventUseCase.getPrivateFolderAsync(miniAppView, request.params, handler) }
            FUNCTION_START_SAVE_FILE -> { fileEventUseCase.startSaveFileAsync(miniAppView, request.params, handler) }
            FUNCTION_STOP_SAVE_FILE -> { fileEventUseCase.stopSaveFileAsync(miniAppView, handler) }
            FUNCTION_DELETE_FILE -> { fileEventUseCase.deleteFileAsync(miniAppView, request.params, handler) }
            FUNCTION_START_READ_FILE -> { fileEventUseCase.startReadFileAsync(miniAppView, request.params, handler) }
            FUNCTION_STOP_READ_FILE -> { fileEventUseCase.stopReadFileAsync(miniAppView, handler) }
            FUNCTION_CHECK_FILE_EXISTS -> { fileEventUseCase.checkFileOrFolderExistsAsync(miniAppView, request.params, handler) }
            FUNCTION_GET_FILE_INFO -> { fileEventUseCase.getFileInfoAsync(miniAppView, request.params, handler) }
            FUNCTION_SAVE_UPDATE_KEY_VALUE -> { fileEventUseCase.saveUpdateKeyValueAsync(miniAppView, request.params, handler) }
            FUNCTION_SAVE_UPDATE_KEY_VALUE_WITH_EXPIRY -> { fileEventUseCase.saveUpdateKeyValueWithExpiryAsync(miniAppView, request.params, handler) }
            FUNCTION_GET_KEY_VALUE -> { fileEventUseCase.getKeyValueAsync(miniAppView, request.params, handler) }
            FUNCTION_DELETE_KEY_VALUE -> { fileEventUseCase.deleteKeyValueAsync(miniAppView, request.params, handler) }
            FUNCTION_PLAY_VOICE -> { fileEventUseCase.playVoiceAsync(miniAppView, request.params, handler) }
            FUNCTION_STOP_PLAY_VOICE -> { fileEventUseCase.stopPlayVoiceAsync(miniAppView, request.params, handler) }
        }
    }

    override fun dispatchSyncMessage(miniAppView: MiniAppView, request: JSRequest): String? {
        when (request.function) {
            FUNCTION_GET_FILE_LIST -> { return fileEventUseCase.getFileList(miniAppView, request.params) }
            FUNCTION_GET_PRIVATE_FOLDER -> { return fileEventUseCase.getPrivateFolder(miniAppView, request.params) }
            FUNCTION_START_SAVE_FILE -> { return fileEventUseCase.startSaveFile(miniAppView, request.params) }
            FUNCTION_STOP_SAVE_FILE -> { return fileEventUseCase.stopSaveFile(miniAppView) }
            FUNCTION_DELETE_FILE -> { return fileEventUseCase.deleteFile(miniAppView, request.params) }
            FUNCTION_START_READ_FILE -> { return fileEventUseCase.startReadFile(miniAppView, request.params) }
            FUNCTION_STOP_READ_FILE -> { return fileEventUseCase.stopReadFile(miniAppView) }
            FUNCTION_CHECK_FILE_EXISTS -> { return fileEventUseCase.checkFileOrFolderExists(miniAppView, request.params) }
            FUNCTION_GET_FILE_INFO -> { return fileEventUseCase.getFileInfo(miniAppView, request.params) }
            FUNCTION_SAVE_UPDATE_KEY_VALUE -> { return fileEventUseCase.saveUpdateKeyValue(miniAppView, request.params) }
            FUNCTION_SAVE_UPDATE_KEY_VALUE_WITH_EXPIRY -> { return fileEventUseCase.saveUpdateKeyValueWithExpiry(miniAppView, request.params) }
            FUNCTION_GET_KEY_VALUE -> { return fileEventUseCase.getKeyValue(miniAppView, request.params) }
            FUNCTION_DELETE_KEY_VALUE -> { return fileEventUseCase.deleteKeyValue(miniAppView, request.params) }
            FUNCTION_PLAY_VOICE -> { return fileEventUseCase.playVoice(miniAppView, request.params) }
            FUNCTION_STOP_PLAY_VOICE -> { return fileEventUseCase.stopPlayVoice(miniAppView, request.params) }
        }
        return ""
    }
}