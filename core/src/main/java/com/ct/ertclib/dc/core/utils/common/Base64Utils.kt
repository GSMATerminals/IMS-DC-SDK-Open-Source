package com.ct.ertclib.dc.core.utils.common

import android.util.Base64

object Base64Utils {

    // Base64 编码
    fun encodeToBase64(input: String): String {
        val bytes = input.toByteArray(Charsets.UTF_8)
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }

    // Base64 解码
    fun decodeFromBase64(input: String): String {
        val bytes = Base64.decode(input, Base64.NO_WRAP)
        return String(bytes, Charsets.UTF_8)
    }
}