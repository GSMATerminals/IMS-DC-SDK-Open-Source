/*
 *   Copyright 2025-China Telecom Research Institute.
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package com.ct.ertclib.dc.core.common

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.ct.ertclib.dc.core.utils.common.PkgUtils

object NativeApp {

    /**
     * type
     *  1    高德地图com.autonavi.minimap
     *  2    百度地图com.baidu.BaiduMap
     *
     */
    fun openMap(context: Context, type: String,lat: Double, lon: Double,title:String):Int{
        try {
            val intent = getIntent(context,type,lat, lon,title)
            if (intent != null){
                context.startActivity(intent)
                return 0
            } else {
                return -1
            }
        } catch (e:ActivityNotFoundException){
            Toast.makeText(context, "请先安装高德地图", Toast.LENGTH_SHORT).show()
            return -1
        }
    }

    private fun getIntent(context: Context,type: String,lat: Double, lon: Double,title:String): Intent? {
        val appName = PkgUtils.getAppName(context)
        when (type) {
            "1" ->{
                val sb = StringBuilder("androidamap://viewMap?sourceApplication=").append(appName)
                sb.append("&lat=").append(lat).append("&lon=").append(lon).append("&poiname=").append(title)
                return Intent("android.intent.action.VIEW", Uri.parse(sb.toString())).setPackage("com.autonavi.minimap")
            }
            "2" ->{
                val sb = StringBuilder("baidumap://map/marker?")
                sb.append("location=").append(lat).append(",").append(lon).append("&title=").append(title)
                return Intent("android.intent.action.VIEW", Uri.parse(sb.toString())).setPackage("com.baidu.BaiduMap")
            }
            else ->{
                return null
            }
        }
    }

    fun openBrowser(context: Context, url: String?) :Int{
        if (url.isNullOrEmpty()){
            return -1
        }
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            context.startActivity(intent)
            return 0
        } catch (e:ActivityNotFoundException){
            Toast.makeText(context, "请先安装高德地图", Toast.LENGTH_SHORT).show()
            return -1
        }

    }
}