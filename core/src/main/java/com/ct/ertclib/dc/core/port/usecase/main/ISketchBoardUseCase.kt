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

package com.ct.ertclib.dc.core.port.usecase.main

interface ISketchBoardUseCase {

    fun openSketchBoard(telecomCallId: String, appId: String, paintColor: String, paintWidth: Float)

    fun closeSketchBoard(needNotifyToMini: Boolean = true)

    fun addDrawingInfo(drawingInfo: String)

    fun addRemoteSizeInfo(width: Int, height: Int)

    fun addRemoteWindowSizeInfo(width: Int, height: Int)

    fun initManager()

    fun release()
}