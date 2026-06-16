package com.ct.ertclib.dc.feature.testing.netdc.utils

import java.net.URI
import java.net.URISyntaxException
import kotlin.text.contains

object CheckUtil {
    fun isValidWebSocketUrl(url: String): Boolean {
        return try {
            val trimmedUrl = url.trim()

            // 检查协议
            if (!trimmedUrl.startsWith("ws://") && !trimmedUrl.startsWith("wss://")) {
                return false
            }

            // 使用 URI 解析验证
            val uri = URI(trimmedUrl)

            // 检查主机名是否为空
            if (uri.host.isNullOrBlank()) {
                return false
            }

            // 检查端口范围
            if (uri.port < -1 || uri.port > 65535) {
                return false
            }

            // 路径不能包含非法字符
            val path = uri.path
            if (path != null && path.contains("//")) {
                return false
            }

            true
        } catch (e: URISyntaxException) {
            false
        } catch (e: Exception) {
            false
        }
    }

    fun isValidRegisterNumber(number: String): Boolean {
        val trimmed = number.trim()

        // 检查是否为空
        if (trimmed.isEmpty()) {
            return false
        }

        // 检查长度：合理的电话号码长度（根据实际需求调整）
        if (trimmed.length < 5 || trimmed.length > 15) {
            return false
        }

        // 必须是纯数字
        if (!trimmed.all { it.isDigit() }) {
            return false
        }

        return true
    }

}