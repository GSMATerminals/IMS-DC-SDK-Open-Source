package com.ct.ertclib.dc.net.utils

import android.util.Base64

/**
 * 配置信息解密器
 * 用于解密Python端生成的加密配置
 * 使用单例模式，全局只有一个实例
 */
object ConfigDecrypt {

    private val SCRAMBLE_TABLE = byteArrayOf(
        0x2A.toByte(), 0x3C.toByte(), 0x4E.toByte(), 0x5F.toByte(),
        0x6A.toByte(), 0x7B.toByte(), 0x8C.toByte(), 0x9D.toByte(),
        0xAE.toByte(), 0xBF.toByte(), 0xC1.toByte(), 0xD2.toByte(),
        0xE3.toByte(), 0xF4.toByte(), 0x05.toByte(), 0x16.toByte()
    )

    // 头部魔数
    private val HEADER = "ERTC".toByteArray()

    /**
     * 解密配置信息
     * @param encryptedBase64 Base64编码的加密字符串
     * @return 解密后的ConfigInfo，失败返回null
     */
    fun decrypt(encryptedBase64: String?): ConfigInfo? {
        if (encryptedBase64.isNullOrEmpty()) {
            return null
        }

        return try {
            // 1. Base64解码
            val decoded = Base64.decode(encryptedBase64, Base64.DEFAULT)

            // 2. 验证头部
            if (decoded.size < 4) return null
            for (i in HEADER.indices) {
                if (decoded[i] != HEADER[i]) return null
            }

            // 3. 移除头部
            val encrypted = decoded.copyOfRange(4, decoded.size)

            // 4. 反转字节顺序
            val reversed = ByteArray(encrypted.size)
            for (i in encrypted.indices) {
                reversed[i] = encrypted[encrypted.size - 1 - i]
            }

            // 5. 异或解密
            val decrypted = ByteArray(reversed.size)
            for (i in reversed.indices) {
                val xorKey = SCRAMBLE_TABLE[i % SCRAMBLE_TABLE.size].toInt() and 0xFF
                val encryptedByte = reversed[i].toInt() and 0xFF
                decrypted[i] = (encryptedByte xor xorKey).toByte()
            }

            // 6. 验证校验和
            if (decrypted.isEmpty()) return null
            val checksum = decrypted[decrypted.size - 1].toInt() and 0xFF
            val dataWithoutChecksum = decrypted.copyOfRange(0, decrypted.size - 1)

            var calculatedChecksum = 0
            for (b in dataWithoutChecksum) {
                calculatedChecksum = (calculatedChecksum + (b.toInt() and 0xFF)) and 0xFF
            }

            if (calculatedChecksum != checksum) return null

            // 7. 解析数据
            val dataStr = String(dataWithoutChecksum, Charsets.UTF_8)
            val parts = dataStr.split("|")
            if (parts.size != 2) return null

            val expireTime = parts[0].toLongOrNull() ?: return null
            val webSocketUrl = parts[1]

            ConfigInfo(expireTime, webSocketUrl)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * 解密并验证URL和过期时间
     * @param encryptedBase64 加密的字符串
     * @param expectedUrl 期望的WebSocket URL
     * @return true表示验证通过，false表示验证失败
     */
    fun decryptAndValidate(encryptedBase64: String?, expectedUrl: String): Boolean {
        val info = decrypt(encryptedBase64)

        if (info == null) {
            return false
        }

        if (info.webSocketUrl != expectedUrl) {
            return false
        }

        val currentTime = System.currentTimeMillis()
        if (currentTime > info.expireTime) {
            return false
        }

        return true
    }

}

/**
 * 解密后的配置信息
 */
data class ConfigInfo(
    val expireTime: Long,      // 到期时间戳（毫秒）
    val webSocketUrl: String   // WebSocket URL
)