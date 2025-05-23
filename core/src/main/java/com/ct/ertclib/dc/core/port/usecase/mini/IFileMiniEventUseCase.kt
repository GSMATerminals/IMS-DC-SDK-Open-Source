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

interface IFileMiniEventUseCase {

    fun getLocation(context: Context, handler: CompletionHandler<String?>)

    fun selectFile(context: Context, handler: CompletionHandler<String?>)

    fun saveFile(context: Context, params: Map<String, Any>, handler: CompletionHandler<String?>)

    fun readFile(context: Context, params: Map<String, Any>, handler: CompletionHandler<String?>)

    fun decompressFile(context: Context, params: Map<String, Any>, handler: CompletionHandler<String?>)

    fun getFileList(context: Context, params: Map<String, Any>): String?

    fun getPrivateFolder(context: Context, params: Map<String, Any>): String?

    fun startSaveFile(context: Context, params: Map<String, Any>): String?

    fun stopSaveFile(context: Context): String?

    fun startReadFile(context: Context, params: Map<String, Any>): String?

    fun stopReadFile(context: Context): String?

    fun checkFileOrFolderExists(context: Context, params: Map<String, Any>): String?

    fun getFileInfo(context: Context, params: Map<String, Any>): String?

    fun getFileInfoAsync(context: Context, params: Map<String, Any>, handler: CompletionHandler<String?>)

    fun deleteFile(context: Context, params: Map<String, Any>): String?

    fun saveUpdateKeyValue(context: Context, params: Map<String, Any>): String?

    fun getKeyValue(context: Context, params: Map<String, Any>): String?

    fun deleteKeyValue(context: Context, params: Map<String, Any>): String?

    fun playVoice(context: Context, params: Map<String, Any>): String?

    fun stopPlayVoice(context: Context, params: Map<String, Any>): String?
}