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

package com.ct.ertclib.dc.base.port.ec

import android.content.Context


/**
 * 面向终端厂商、各运营商提供私有能力接入的接口定义，参考oemec模块的实现
 * 打包为AAR，供core模块和各拓展能力模块依赖
 */
interface IEC {
    fun init(context: Context, callback: IECCallback?)
    fun request(context: Context,callId:String, appId:String,content:String):Int
    fun releaseMiniApp(context: Context, callId:String, miniAppId:String)
    fun releaseAll(context: Context)
}