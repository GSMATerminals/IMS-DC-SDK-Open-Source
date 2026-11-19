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
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ct.ertclib.dc.net.data

data class ConfigData(
    val webSocketUrl: String,
    val phoneNumber: String,
    val signature: String,
    val sdkCertificate: String? = null
) {
    /**
     * 验证证书
     * @return 验证结果
     */
    fun validateSDKCertificate(): Boolean {
        // todo 开发者自己实现
        return true
    }
}
