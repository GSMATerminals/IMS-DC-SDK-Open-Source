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

package com.ct.ertclib.dc.feature.testing.socket

class SocketData{
    var type: Int = 0 // 1:创建ADC,2:普通数据,3:呼叫,4:挂断,5:接听
    var createLabels: ArrayList<String> ?= null // 要创建的ADC的labels
    var dataLabel: String ?= null // 收发数据ADC的label
    var data: String ?= null // 数据,byteArray的base64编码
}
