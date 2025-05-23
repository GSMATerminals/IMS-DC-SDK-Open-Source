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

package com.ct.ertclib.dc.core.utils.common

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.provider.MediaStore
import android.text.TextUtils
import android.util.Base64
import java.io.File


object BitmapUtils {
    fun getBitmap(drawable: Drawable, width: Int, height: Int, config: Bitmap.Config?): Bitmap {
        var config = config
        if (drawable is BitmapDrawable) {
            if (config == null || drawable.bitmap.config == config) {
                if (width == drawable.bitmap.width && height == drawable.bitmap.height) {
                    return drawable.bitmap
                }

                return Bitmap.createScaledBitmap(drawable.bitmap, width, height, true)
            }
        }
        val bounds = drawable.bounds
        val left = bounds.left
        val top = bounds.top
        val right = bounds.right
        val bottom = bounds.bottom
        if (config == null) {
            config = Bitmap.Config.ARGB_8888
        }
        val createBitmap = Bitmap.createBitmap(width, height, config)
        drawable.setBounds(0, 0, width, height)
        drawable.draw(Canvas(createBitmap))
        drawable.setBounds(left, top, right, bottom)
        return createBitmap
    }

    @Throws(Exception::class)
    fun getBitmapFromBase64(base64: String): Bitmap {
        var tmp = base64
        val indexOf = tmp.indexOf("base64,", 0, true)
        if (indexOf != -1) {
            tmp = tmp.substring(indexOf + "base64,".length)
        }
        val decode = Base64.decode(tmp, Base64.NO_WRAP)
        return BitmapFactory.decodeByteArray(decode, 0, decode.size)
    }

    fun getBitmapFromPath(context: Context,path:String):Bitmap?{
        if (!TextUtils.isEmpty(path) && FileUtils.isUri(path)){
            return MediaStore.Images.Media.getBitmap(context.contentResolver, Uri.parse(path));
        } else {
            val file = File(path)
            return if (file.exists()){
                BitmapFactory.decodeFile(path)
            } else {
                null
            }
        }
    }

    fun saveImageToGallery(context: Context, bitmapImage: Bitmap) {
        // 创建ContentValues对象
        val values = ContentValues()

//        // 设置图片名称
//        values.put(MediaStore.Images.Media.DISPLAY_NAME, "图片名称")
//        // 设置图片描述
//        values.put(MediaStore.Images.Media.DESCRIPTION, "图片描述")
        // 设置图片格式
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")


        // 获取ContentResolver
        val contentResolver = context.contentResolver

        try {
            // 将图片保存到系统相册
            val uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            if (uri != null) {
                // 将Uri转换为字节输出流
                val imageOut = contentResolver.openOutputStream(uri)
                // 使用JPEG格式将Bitmap图像写入输出流
                bitmapImage.compress(Bitmap.CompressFormat.JPEG, 100, imageOut!!)
                // 关闭输出流
                imageOut.close()
                val intent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, uri)
                context.sendBroadcast(intent)
            }
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
    }


}