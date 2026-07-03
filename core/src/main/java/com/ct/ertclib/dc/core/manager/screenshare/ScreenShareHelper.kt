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

package com.ct.ertclib.dc.core.manager.screenshare

import com.ct.ertclib.dc.core.port.usecase.main.IScreenShareUseCase
import com.ct.ertclib.dc.core.port.usecase.main.ISketchBoardUseCase
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

object ScreenShareHelper: KoinComponent {
    private val screenShareUseCase : IScreenShareUseCase by inject()
    private val sketchBoardUseCase : ISketchBoardUseCase by inject()
    private var isInit = false
    fun init(){
        if (isInit){
            return
        }
        isInit = true
        screenShareUseCase.initManager()
        sketchBoardUseCase.initManager()
    }

    fun release(){
        if (!isInit){
            return
        }
        isInit = false
        screenShareUseCase.release()
        sketchBoardUseCase.release()
    }
}