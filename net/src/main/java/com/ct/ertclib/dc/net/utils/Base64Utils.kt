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

package com.ct.ertclib.dc.net.utils

import android.util.Base64

object Base64Utils {

    fun byteArrayToBase64(byteArray: ByteArray): String {
        val base64String = Base64.encodeToString(byteArray, Base64.NO_WRAP);
        return base64String
    }

    fun base64ToByteArray(base64String: String): ByteArray {
        val byteArray = Base64.decode(base64String, Base64.NO_WRAP);
        return byteArray
    }
}