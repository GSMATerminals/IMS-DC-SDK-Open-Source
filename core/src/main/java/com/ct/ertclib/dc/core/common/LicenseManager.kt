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

package com.ct.ertclib.dc.core.common

import com.blankj.utilcode.util.Utils
import com.ct.ertclib.dc.core.utils.common.FlavorUtils
import com.ct.ertclib.nativelibs.VerifyHelper
import java.io.InputStreamReader

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
        private var cachedPublicKey: String? = null
        private var instance: LicenseManager?= null
        private val sLock = Object()

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

    private val pkgKey: String by lazy {
        cachedPublicKey ?: loadPublicKeyFromAssets().also {
            cachedPublicKey = it
        }
    }

    private fun loadPublicKeyFromAssets(): String {
        return try {
            val keyFileName = if (FlavorUtils.CHANNEL_LOCAL == FlavorUtils.getChannelName()){
                "local_public_key.pem"
            } else {
                "public_key.pem"
            }
            Utils.getApp().assets.open(keyFileName).use { inputStream ->
                InputStreamReader(inputStream).use { reader ->
                    reader.readText().trim()
                }
            }
        } catch (_: Exception) {
            return ""
        }
    }

    // 调用示例：LicenseManager.getInstance().verifyLicense("aa","6","eKGExEKb140FXgZqNgkP7o210sol6x6ieF9AvWvjbMef2ec7+UtggJumjOyizeNQJ8e9D2PG5Cjxv4jo/aRLMag==")
    fun verifyLicense(miniAppId: String, apiCodes: String, license: String): Boolean {
        return VerifyHelper.getInstance().verifyLicense(miniAppId, apiCodes, license)
    }

    fun parseImgData(img: String): String {
        return VerifyHelper.getInstance().parseImg(img)
    }

    fun verifyMiniAppPkg(zipPath: String): Boolean {
        return VerifyHelper.getInstance().verifyMiniAppPkg(zipPath, pkgKey)
    }

    fun verifyMiniAppFolder(folderPath: String): Boolean {
        return VerifyHelper.getInstance().verifyMiniAppFolder(folderPath, pkgKey)
    }
}