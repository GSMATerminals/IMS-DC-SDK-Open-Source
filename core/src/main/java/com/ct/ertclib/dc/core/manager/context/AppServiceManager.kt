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

package com.ct.ertclib.dc.core.manager.context

import com.ct.ertclib.dc.core.utils.logger.Logger
import com.ct.ertclib.dc.core.port.manager.IAppServiceManager
import com.ct.ertclib.dc.core.port.usecase.main.IScreenShareUseCase
import com.ct.ertclib.dc.core.port.usecase.main.ISketchBoardUseCase

class AppServiceManager(
    private val screenShareUseCase: IScreenShareUseCase,
    private val sketchBoardUseCase: ISketchBoardUseCase
) : IAppServiceManager {

    companion object {
        private const val TAG = "AppServiceManager"
    }

    private val logger = Logger.getLogger(TAG)

    override fun initManager() {
        logger.info("initManager")
        screenShareUseCase.initManager()
        sketchBoardUseCase.initManager()
    }

    override fun release() {
        logger.info("release")
        screenShareUseCase.release()
        sketchBoardUseCase.release()
    }
}