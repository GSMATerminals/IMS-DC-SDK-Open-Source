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

package com.ct.ertclib.dc.feature.testing

class TestData {

    enum class MessageType {
        CREATE_DC_REQUEST,
        DELETE_DC_NOTIFY,
        DC_STATUS_CHANGED,
        MESSAGE
    }

    var messageType: MessageType? = null
    var dcLabel: String? = null
    var data: ByteArray? = null

    fun setDcLabels(strArr: Array<String?>) {
        val sb = StringBuilder()
        for (append in strArr) {
            sb.append(append)
            sb.append(",")
        }
        dcLabel = sb.toString()
    }

    override fun toString(): String {
        return "TestData(messageType=$messageType, dcLabel=$dcLabel, data=${data?.size})"
    }
}