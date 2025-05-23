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

package com.ct.ertclib.dc.core.common

class LicenseManager {

    enum class ApiCode(val apiCode: String) {
        START_SCREEN_SHARE("0"),
        GET_USER_MEDIA("1"),
        PLAY_DTMF_TONE("2");

        companion object {
            fun fromString(apiCode: String): ApiCode? {
                return values().firstOrNull { it.apiCode == apiCode }
            }
        }
    }

    companion object{
        private var instance: LicenseManager?= null
        private val sLock = Object()

        init {
            System.loadLibrary("license-android")
            instance = LicenseManager()
        }

        fun getInstance(): LicenseManager {
            if (instance != null) return instance!!
            synchronized(sLock) {
                if (instance == null) {
                    instance = LicenseManager()
                }
            }
            return instance!!
        }
    }
    private external fun verify(miniAppId: String, apiCodes: String, license: String): Boolean
    private external fun parseImg(img: String): String

    // 调用示例：LicenseManager.getInstance().verifyLicense("aa","6","eKGExEKb140FXgZqNgkP7o210sol6x6ieF9AvWvjbMef2ec7+UtggJumjOyizeNQJ8e9D2PG5Cjxv4jo/aRLMag==")
    fun verifyLicense(miniAppId: String, apiCodes: String, license: String): Boolean {
        return verify(miniAppId, apiCodes, license)
    }

    fun parseImgData(img: String): String {
        return parseImg(img)
    }
}