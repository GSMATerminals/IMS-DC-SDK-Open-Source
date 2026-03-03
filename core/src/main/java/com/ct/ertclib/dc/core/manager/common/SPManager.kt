package com.ct.ertclib.dc.core.manager.common

import com.blankj.utilcode.util.SPUtils
import com.ct.ertclib.dc.core.utils.logger.Logger

class SPManager {

    companion object {
        private const val TAG = "SPManager"
        const val MINI_APP_SP_KEYS_KEY = "miniAppSpKeysKey"
        const val MINI_APP_SP_EXPIRY_ITEM_SPLIT_KEY = "miniAppSpExpiryItemSplitKeysKey"
        const val MINI_APP_SP_EXPIRY_SPLIT_KEY = "miniAppSpExpirySplitKeysKey"

        val instance: SPManager by lazy {
            SPManager()
        }
    }
    private val sLogger: Logger = Logger.getLogger(TAG)

    fun clearExpiredData(){
        val keysStr = SPUtils.getInstance().getString(MINI_APP_SP_KEYS_KEY, "")
        if (keysStr.isNotEmpty()) {
            val keys = keysStr.split(MINI_APP_SP_EXPIRY_ITEM_SPLIT_KEY)
            val builder = StringBuilder()

            keys.forEach { item ->
                try {
                    val parts = item.split(MINI_APP_SP_EXPIRY_SPLIT_KEY)
                    // 检查分割后的数组是否有足够的元素
                    if (parts.size >= 2) {
                        val key = parts[0]
                        val expiryTime = parts[1].toLong()

                        if (System.currentTimeMillis() < expiryTime) {
                            if (builder.isNotEmpty()) {
                                builder.append(MINI_APP_SP_EXPIRY_ITEM_SPLIT_KEY)
                            }
                            builder.append(item)
                        } else {
                            SPUtils.getInstance().remove(key)
                        }
                    } else {
                        // 如果格式不正确，可以选择移除该项或记录错误
                        sLogger.warn("Invalid SP expiry item format: $item")
                        // 移除格式错误的数据
                        if (parts.isNotEmpty()) {
                            SPUtils.getInstance().remove(parts[0])
                        }
                    }
                } catch (e: Exception) {
                    sLogger.error("Error processing SP expiry item: $item", e)
                    e.printStackTrace()
                }
            }

            SPUtils.getInstance().put(MINI_APP_SP_KEYS_KEY, builder.toString())
        }
    }

    fun saveUpdateKeyValueWithExpiry(key : String,value : String,ttl : Long){
        SPUtils.getInstance().put(key,value)
        // 存有效期，用于在SDK启动的时候清除过期的key
        val expiryTime = System.currentTimeMillis() + ttl
        val keysStr = SPUtils.getInstance().getString(MINI_APP_SP_KEYS_KEY,"")
        val builder = StringBuilder(keysStr)
        if (builder.isNotEmpty()){
            builder.append(MINI_APP_SP_EXPIRY_ITEM_SPLIT_KEY)
        }
        builder.append("${key}${MINI_APP_SP_EXPIRY_SPLIT_KEY}${expiryTime}")
        SPUtils.getInstance().put(MINI_APP_SP_KEYS_KEY, builder.toString())
    }

    fun deleteKeyValue(key : String){
        SPUtils.getInstance().remove(key)
        val keysStr = SPUtils.getInstance().getString(MINI_APP_SP_KEYS_KEY,"")
        val keys = keysStr.split(MINI_APP_SP_EXPIRY_ITEM_SPLIT_KEY)
        val builder = StringBuilder()
        keys.forEach {
            try {
                val appIdKey = it.split(MINI_APP_SP_EXPIRY_SPLIT_KEY)[0]
                if (appIdKey === key){
                    SPUtils.getInstance().remove(key)
                } else {
                    if (builder.isNotEmpty()){
                        builder.append(MINI_APP_SP_EXPIRY_ITEM_SPLIT_KEY)
                    }
                    builder.append(it)
                }
            } catch (e: Exception){
                e.printStackTrace()
            }
        }
        SPUtils.getInstance().put(MINI_APP_SP_KEYS_KEY, builder.toString())
    }

    fun getKeyValue(key: String): String {
        return SPUtils.getInstance().getString(key)
    }
}