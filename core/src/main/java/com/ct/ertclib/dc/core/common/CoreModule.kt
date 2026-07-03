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

package com.ct.ertclib.dc.core.common

import com.ct.ertclib.dc.core.manager.common.FileDownloadManager
import com.ct.ertclib.dc.core.manager.common.ModelManager
import com.ct.ertclib.dc.core.manager.context.ActivityManager
import com.ct.ertclib.dc.core.manager.context.AppServiceManager
import com.ct.ertclib.dc.core.manager.screenshare.ScreenShareManager
import com.ct.ertclib.dc.core.manager.screenshare.SketchManager
import com.ct.ertclib.dc.core.miniapp.db.PermissionDbRepo
import com.ct.ertclib.dc.core.port.common.IActivityManager
import com.ct.ertclib.dc.core.port.manager.IAppServiceManager
import com.ct.ertclib.dc.core.port.manager.IFileDownloadManager
import com.ct.ertclib.dc.core.port.manager.IModelManager
import com.ct.ertclib.dc.core.port.manager.IScreenShareManager
import com.ct.ertclib.dc.core.port.manager.ISketchManager
import com.ct.ertclib.dc.core.port.miniapp.IPermissionDbRepo
import com.ct.ertclib.dc.core.port.usecase.main.IAsInfoUseCase
import com.ct.ertclib.dc.core.port.usecase.main.IBootstrapMiniAppUseCase
import com.ct.ertclib.dc.core.port.usecase.main.IScreenShareUseCase
import com.ct.ertclib.dc.core.port.usecase.main.ISketchBoardUseCase
import com.ct.ertclib.dc.core.port.usecase.mini.IDCMiniEventUseCase
import com.ct.ertclib.dc.core.port.usecase.mini.IFileMiniEventUseCase
import com.ct.ertclib.dc.core.port.usecase.mini.IAppMiniUseCase
import com.ct.ertclib.dc.core.port.usecase.mini.IECUseCase
import com.ct.ertclib.dc.core.port.usecase.mini.IPermissionUseCase
import com.ct.ertclib.dc.core.port.usecase.mini.ISystemMiniUseCase
import com.ct.ertclib.dc.core.port.usecase.mini.IScreenShareMiniUseCase
import com.ct.ertclib.dc.core.usecase.main.ScreenShareUseCase
import com.ct.ertclib.dc.core.usecase.main.SketchBoardUseCase
import com.ct.ertclib.dc.core.usecase.miniapp.DCMiniUseCase
import com.ct.ertclib.dc.core.usecase.miniapp.FileMiniUseCase
import com.ct.ertclib.dc.core.usecase.miniapp.AppMiniUseCase
import com.ct.ertclib.dc.core.usecase.common.PermissionUseCase
import com.ct.ertclib.dc.core.usecase.main.AsInfoModuleUseCase
import com.ct.ertclib.dc.core.usecase.main.BootstrapMiniAppUseCase
import com.ct.ertclib.dc.core.usecase.miniapp.ECUseCase
import com.ct.ertclib.dc.core.usecase.miniapp.SystemMiniUseCase
import com.ct.ertclib.dc.core.usecase.miniapp.ScreenShareMiniUseCase
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val coreModule = module {

    single<IDCMiniEventUseCase> { DCMiniUseCase() }
    single<IECUseCase> { ECUseCase() }
    single<IAppMiniUseCase> { AppMiniUseCase(get()) }
    single<IScreenShareMiniUseCase> { ScreenShareMiniUseCase() }

    single<IScreenShareUseCase> { ScreenShareUseCase(get()) }
    single<IScreenShareManager> { ScreenShareManager(androidContext()) }

    single<ISketchBoardUseCase> { SketchBoardUseCase(androidContext(), get(), get()) }
    single<IAppServiceManager> { AppServiceManager(get(), get()) }


    single<ISketchManager> { SketchManager(androidContext(), get()) }

    single<IPermissionUseCase> { PermissionUseCase(androidContext(), get()) }

    single<IPermissionDbRepo> { PermissionDbRepo() }

    single<IActivityManager> { ActivityManager() }

    single<IModelManager> { ModelManager() }

    single<IFileDownloadManager> { FileDownloadManager(androidContext()) }

    single<ISystemMiniUseCase> { SystemMiniUseCase(get()) }
    single<IFileMiniEventUseCase> { FileMiniUseCase(get(), get(), get()) }

    factory<IAsInfoUseCase> { AsInfoModuleUseCase() }
    factory<IBootstrapMiniAppUseCase> { BootstrapMiniAppUseCase() }
    }