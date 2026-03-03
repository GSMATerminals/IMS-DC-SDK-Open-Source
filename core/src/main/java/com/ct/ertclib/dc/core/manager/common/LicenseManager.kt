package com.ct.ertclib.dc.core.manager.common

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
        return VerifyHelper.Companion.getInstance().verifyLicense(miniAppId, apiCodes, license)
    }

    fun parseImgData(img: String): String {
        return VerifyHelper.Companion.getInstance().parseImg(img)
    }

    fun verifyMiniAppPkg(zipPath: String): Boolean {
        return VerifyHelper.Companion.getInstance().verifyMiniAppPkg(zipPath, pkgKey)
    }

    fun verifyMiniAppFolder(folderPath: String): Boolean {
        return VerifyHelper.Companion.getInstance().verifyMiniAppFolder(folderPath, pkgKey)
    }
}