package com.ct.ertclib.dc.feature.testing.socket

import android.content.Context
import android.net.wifi.WifiManager
import android.util.Log
import java.net.NetworkInterface
import java.net.Inet4Address

class HotspotIpHelper(private val context: Context) {

    /**
     * 获取热点IP地址
     */
    fun getHotspotIpAddress(): String? {
        return try {
            // 方法1: 通过网络接口获取
            getHotspotIpFromInterfaces() ?:
            // 方法2: 通过WifiManager获取（备用）
            getHotspotIpFromWifiManager()
        } catch (e: Exception) {
            Log.e("HotspotIpHelper", "Error getting hotspot IP: ${e.message}")
            null
        }
    }

    /**
     * 通过遍历网络接口获取热点IP
     */
    private fun getHotspotIpFromInterfaces(): String? {
        return try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val networkInterface = interfaces.nextElement()

                // 检查接口名称，热点接口通常包含 "wlan", "ap", "softap" 等
                val interfaceName = networkInterface.name
                if (interfaceName.contains("wlan") ||
                    interfaceName.contains("ap") ||
                    interfaceName.contains("softap")) {

                    val addresses = networkInterface.inetAddresses
                    while (addresses.hasMoreElements()) {
                        val address = addresses.nextElement()
                        // 获取IPv4地址，热点通常是192.168.x.x或类似私有地址
                        if (address is Inet4Address && !address.isLoopbackAddress) {
                            val ip = address.hostAddress
                            if (isHotspotIp(ip)) {
                                return ip
                            }
                        }
                    }
                }
            }
            null
        } catch (e: Exception) {
            Log.e("HotspotIpHelper", "Error getting IP from interfaces: ${e.message}")
            null
        }
    }

    /**
     * 通过WifiManager获取热点IP（需要系统权限或root）
     */
    private fun getHotspotIpFromWifiManager(): String? {
        return try {
            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            val method = wifiManager.javaClass.getDeclaredMethod("getWifiApConfiguration")
            method.isAccessible = true
            val config = method.invoke(wifiManager)

            if (config != null) {
                val ipAddressField = config.javaClass.getDeclaredField("ipAddress")
                ipAddressField.isAccessible = true
                val ipAddress = ipAddressField.getInt(config)

                // 将整型IP转换为字符串格式
                if (ipAddress != 0) {
                    return convertIntToIp(ipAddress)
                }
            }
            null
        } catch (e: Exception) {
            Log.e("HotspotIpHelper", "Error getting IP from WifiManager: ${e.message}")
            null
        }
    }

    /**
     * 判断是否为热点IP（私有地址范围）
     */
    private fun isHotspotIp(ip: String): Boolean {
        return when {
            ip.startsWith("192.168.") -> true
            ip.startsWith("10.") -> true
            ip.startsWith("172.") -> {
                val secondPart = ip.split(".")[1].toInt()
                secondPart in 16..31
            }
            else -> false
        }
    }

    /**
     * 将整型IP地址转换为字符串格式
     */
    private fun convertIntToIp(ip: Int): String {
        return "${(ip and 0xFF)}.${(ip shr 8 and 0xFF)}.${(ip shr 16 and 0xFF)}.${(ip shr 24 and 0xFF)}"
    }

    /**
     * 获取所有网络接口信息（用于调试）
     */
    fun getAllNetworkInterfaces(): List<String> {
        val interfaceInfo = mutableListOf<String>()
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val networkInterface = interfaces.nextElement()
                val info = StringBuilder()
                info.append("Interface: ${networkInterface.name}\n")
                info.append("Display: ${networkInterface.displayName}\n")

                val addresses = networkInterface.inetAddresses
                while (addresses.hasMoreElements()) {
                    val address = addresses.nextElement()
                    info.append("  IP: ${address.hostAddress} (${address.javaClass.simpleName})\n")
                }

                interfaceInfo.add(info.toString())
            }
        } catch (e: Exception) {
            interfaceInfo.add("Error: ${e.message}")
        }
        return interfaceInfo
    }
}