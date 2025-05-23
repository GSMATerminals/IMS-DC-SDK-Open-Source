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

package com.ct.ertclib.dc.core.port.manager

import android.content.Context
import com.ct.ertclib.dc.core.data.call.CallInfo
import com.ct.ertclib.dc.core.data.model.MiniAppInfo
import com.ct.ertclib.dc.core.miniapp.aidl.IMessageCallback
import com.ct.ertclib.dc.core.port.common.OnPickMediaCallbackListener
import com.ct.ertclib.dc.core.port.miniapp.IMiniApp
import com.newcalllib.datachannel.V1_0.IImsDataChannel
import com.newcalllib.datachannel.V1_0.ImsDCStatus

interface IMiniToParentManager {

    var miniAppInterface: IMiniApp?

    val createDCLabelList : MutableList<String>?
    val openDCList: MutableList<IImsDataChannel>?
    val systemApiLicenseMap: MutableMap<String, String>

    fun bindService(context: Context)

    fun unBindService(context: Context)

    fun createDC(dcLabels: List<String>, description: String) : Int?

    fun closeDC(label: String)

    fun onDataChannelStateChanged(dc: IImsDataChannel, status: ImsDCStatus?, errorCode: Int)

    fun sendMessageToParent(message: String,callback: IMessageCallback.Stub?)

    fun getCallInfo(): CallInfo?

    fun getMiniAppInfo(): MiniAppInfo?

    fun selectFile(callback: OnPickMediaCallbackListener)

    fun callHandler(method: String, args: Array<Any>)

    fun stopApp()
}