package com.ct.ertclib.dc.core.utils.common

import android.Manifest
import android.annotation.SuppressLint
import android.app.ActivityManager
import android.content.Context
import android.net.wifi.WifiManager
import android.os.BatteryManager
import android.os.Build
import android.telephony.CellInfoLte
import android.telephony.CellInfoNr
import android.telephony.CellSignalStrengthNr
import android.telephony.PhoneStateListener
import android.telephony.SignalStrength
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import java.io.BufferedReader
import java.io.FileInputStream
import java.io.InputStreamReader


object SystemUtils {

    const val TAG = "SystemUtils"

    const val INVALID_RSSI = -200

    fun getBatteryPercentage(context: Context): Int {
        val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
        return batteryManager?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: -1
    }


    fun getAvailableMemory(context: Context): Long {

        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager?.getMemoryInfo(memoryInfo)
        return memoryInfo.availMem
    }

    fun getWiFiRssi(context: Context): Int {
        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
        val wifiInfo = wifiManager?.connectionInfo
        return wifiInfo?.rssi ?: INVALID_RSSI
    }

    @RequiresApi(Build.VERSION_CODES.R)
    @RequiresPermission(Manifest.permission.ACCESS_FINE_LOCATION)
    @SuppressLint("MissingPermission", "NewApi")
    fun getMobileRSRP(context: Context, slotId: Int): Int {
        val defaultSubId = SubscriptionManager.getSubscriptionId(slotId)
        val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
        telephonyManager?.createForSubscriptionId(defaultSubId)
        val cellInfos = telephonyManager?.allCellInfo
        cellInfos?.let {
            it.forEach { cellInfo ->
                val cellInfoNr = cellInfo as? CellInfoNr
                cellInfoNr?.let {
                    val cellStrength = cellInfoNr.cellSignalStrength as? CellSignalStrengthNr
                    val rsrp = cellStrength?.ssRsrp
                    return rsrp ?: INVALID_RSSI
                } ?: run {
                    val cellInfoLTE = cellInfo as? CellInfoLte
                    val cellStrength = cellInfoLTE?.cellSignalStrength
                    val rsrp = cellStrength?.rsrp
                    return rsrp ?: INVALID_RSSI
                }

            }
        }
        return INVALID_RSSI
    }

    fun getCpuCoreNum(): Int {
        val coreCount = readCpuCoreCount()
        LogUtils.debug(TAG, "getCpuCoreNum : $coreCount")
        return coreCount
    }

    private fun readCpuCoreCount(): Int {
        var cores = readCoreFile("/sys/devices/system/cpu/online")
        if (cores <= 0) {
            cores = readCoreFile("/sys/devices/system/cpu/possible")
        }
        if (cores <= 0) {
            cores = readCpuInfo()
        }
        return cores
    }

    private fun readCoreFile(path: String): Int {
        return try {
            val fis = FileInputStream(path)
            val br = BufferedReader(InputStreamReader(fis))
            val line = br.readLine()
            br.close()

            if (line != null && line.isNotEmpty()) {
                val ranges = line.split(",")
                var totalCores = 0

                for (range in ranges) {
                    if (range.contains("-")) {
                        val parts = range.split("-")
                        val start = parts[0].toInt()
                        val end = parts[1].toInt()
                        totalCores += (end - start + 1)
                    } else {
                        totalCores += 1
                    }
                }

                totalCores
            } else {
                -1
            }
        } catch (e: Exception) {
            e.printStackTrace()
            -1
        }
    }

    private fun readCpuInfo(): Int {
        return try {
            val fis = FileInputStream("/proc/cpuinfo")
            val br = BufferedReader(InputStreamReader(fis))
            var count = 0

            var line: String?
            while (br.readLine().also { line = it } != null) {
                if (line?.startsWith("processor") == true) {
                    count++
                }
            }

            br.close()
            count
        } catch (e: Exception) {
            e.printStackTrace()
            -1
        }
    }
}