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
        return true
    }
}
