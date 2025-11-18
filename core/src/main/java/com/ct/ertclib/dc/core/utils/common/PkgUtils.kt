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

package com.ct.ertclib.dc.core.utils.common

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.content.pm.Signature
import android.content.pm.SigningInfo
import android.graphics.drawable.Icon
import android.os.Build
import com.blankj.utilcode.util.SPUtils
import com.ct.ertclib.dc.core.BuildConfig
import com.ct.ertclib.dc.core.R
import com.ct.ertclib.dc.core.ui.activity.MainActivity


object PkgUtils {
    const val SHORTCUT_DID_CALL_NUM_SP_KEY = "shortcut_did_call_num_sp_key"

    const val SAMSUNG = "samsung"
    const val XIAOMI = "xiaomi"
    const val HUAWEI = "huawei"
    const val HONOR = "honor"
    const val OPPO = "oppo"
    const val VIVO = "vivo"
    const val ONEPLUS = "oneplus"
    const val REALME = "realme"
    const val MEIZU = "meizu"
    const val LENOVO = "lenovo"
    const val MOTOROLA = "motorola"
    const val NOKIA = "nokia"
    const val SONY = "sony"
    const val GOOGLE = "google"
    const val ONEPLUS2 = "oneplus" // 注意：有些设备可能使用全小写
    const val LG = "lg"
    const val HTC = "htc"
    const val ASUS = "asus"
    const val ZTE = "zte"
    const val COOLPAD = "coolpad"
    const val GIONEE = "gionee"

    private fun addShortcutDidOnce(){
        // 累加一次
        val num = SPUtils.getInstance().getInt(SHORTCUT_DID_CALL_NUM_SP_KEY,0)
        SPUtils.getInstance().put(SHORTCUT_DID_CALL_NUM_SP_KEY, num+1)
    }

    private fun shortcutCanCall():Boolean{
        // 最多三次
        val num = SPUtils.getInstance().getInt(SHORTCUT_DID_CALL_NUM_SP_KEY,0)
        return num<3
    }

//    // 获取自身签名信息
//    fun getSignature(context: Context): String {
//        try {
//            val signatures: Array<Signature> = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
//                val packageInfo: PackageInfo = context.packageManager.getPackageInfo(
//                    context.packageName,
//                    PackageManager.GET_SIGNING_CERTIFICATES
//                )
//                val signingInfo: SigningInfo = packageInfo.signingInfo
//                signingInfo.apkContentsSigners
//            } else {
//                val packageInfo: PackageInfo = context.packageManager.getPackageInfo(
//                    context.packageName,
//                    PackageManager.GET_SIGNATURES
//                )
//                packageInfo.signatures
//            }
//            val builder = StringBuilder()
//            for (signature in signatures) {
//                builder.append(signature.toCharsString())
//            }
//            return builder.toString()
//        } catch (e: PackageManager.NameNotFoundException) {
//            e.printStackTrace()
//        }
//        return ""
//    }
    /**
     * 获取app的名称
     * @param context
     * @return
     */
    fun getAppName(context:Context) :String{
        var appName = ""
        try {
            val packageManager = context.packageManager
            val packageInfo = packageManager.getPackageInfo(context.packageName, 0)
            val labelRes = packageInfo.applicationInfo?.labelRes
            labelRes?.let { appName = context.resources.getString(labelRes) }
        } catch (e:Exception) {
            e.printStackTrace()
        }
        return appName

    }

    /**
     * 获取app的版本
     * @param context
     * @return
     */
    fun getAppVersion(context:Context) :String{
        var versionName = ""
        try {
            val packageManager = context.packageManager
            val packageInfo = packageManager.getPackageInfo(context.packageName, 0)
            packageInfo.versionName?.let {
                versionName = it
            }
        } catch (e:Exception) {
            e.printStackTrace()
        }
        return versionName

    }

    fun isDebug():Boolean{
        return BuildConfig.DEBUG
    }

    fun checkAndCreatePinnedShortcuts(context:Context) {
//        if (FlavorUtils.getChannelName() == FlavorUtils.CHANNEL_LOCAL){
//            return
//        }
//        if (!isDebug() && shortcutCanCall() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//            val shortcutManager = context.getSystemService(ShortcutManager::class.java)
//            if (shortcutManager != null && shortcutManager.isRequestPinShortcutSupported && shortcutManager.pinnedShortcuts.isEmpty()) {
//                val intent = Intent(context, MainActivity::class.java)
//                intent.setAction(Intent.ACTION_VIEW)
//                intent.putExtra("key", "fromPinnedShortcut")
//                val icon = Icon.createWithResource(context, R.drawable.icon_ct_dc_shortcut)
//                val pinShortcutInfo = ShortcutInfo.Builder(context, "CtNewCall")
//                    .setShortLabel("电信增强通话")
//                    .setLongLabel("电信增强通话")
//                    .setIcon(icon)
//                    .setIntent(intent)
//                    .build()
//                val pinnedShortcutCallbackIntent =
//                    shortcutManager.createShortcutResultIntent(pinShortcutInfo)
//                val successCallback = PendingIntent.getBroadcast(
//                    context, 0,
//                    pinnedShortcutCallbackIntent, PendingIntent.FLAG_IMMUTABLE
//                )
//                shortcutManager.requestPinShortcut(
//                    pinShortcutInfo,
//                    successCallback.intentSender
//                )
//                ToastUtils.showShortToast(context,"已为您创建中国电信增强通话桌面入口-电信增强通话")
//            }
//            addShortcutDidOnce()
//        }
    }

    fun brand():String{
        return try {
            Build.BRAND.lowercase()
        }catch (e: Exception){
            ""
        }
    }
}