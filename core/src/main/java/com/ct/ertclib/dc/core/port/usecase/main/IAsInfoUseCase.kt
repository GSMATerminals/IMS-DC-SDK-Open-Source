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

package com.ct.ertclib.dc.core.port.usecase.main


import com.ct.ertclib.dc.core.data.miniapp.Module
import com.ct.ertclib.dc.core.miniapp.MiniAppRootADCImpl
import com.ct.ertclib.dc.core.port.common.IMessageCallback
import com.newcalllib.datachannel.V1_0.IImsDataChannel

interface IAsInfoUseCase {

    fun init(adcParamsListener: MiniAppRootADCImpl.OnADCParamsOk)

    fun createDC()

    fun release()

    fun dispatchEvent(event: String, params: Map<String, Any>, iMessageCallback: IMessageCallback?)

    fun onDCCreated(imsDataChannel: IImsDataChannel)

    fun sendData(
        module: Module,
        event: String,
        originData: ByteArray,
        onSendCallback: MiniAppRootADCImpl.OnSendCallback
    )

    fun registerRootListener(
        module: Module,
        listener: MiniAppRootADCImpl.OnADCListener
    )

    fun unRegisterRootListener(module: Module)
}