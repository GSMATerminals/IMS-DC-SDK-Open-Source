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

import android.content.Context
import android.text.TextUtils
import com.blankj.utilcode.util.FileUtils
import com.ct.ertclib.dc.core.utils.logger.Logger
import com.ct.ertclib.dc.core.constants.CommonConstants.INDEX_FILE_NAME
import com.ct.ertclib.dc.core.constants.CommonConstants.MINI_APP_ROOT_PATH
import java.io.File

class PathManager {

    companion object {
        private const val TAG = "PathManager"
    }

    private val sLogger = Logger.getLogger(TAG)

    fun getInternalFilesDirPath(context: Context): String {
        return context.filesDir.path + File.separator
    }

    fun getInternalCacheDirPath(context: Context): String {
        return context.cacheDir.path + File.separator
    }

    fun getMiniAppDir(context: Context,mkdir: Boolean = true): File? {
        val dir = getInternalFilesDirPath(context) + MINI_APP_ROOT_PATH
        if (mkdir) {
            FileUtils.createOrExistsDir(dir)
        }
        return FileUtils.getFileByPath(dir)
    }

    fun getMiniAppHtml(context: Context,appId: String, mkdir: Boolean = true): File? {
        val dir = getInternalFilesDirPath(context) + MINI_APP_ROOT_PATH
        if (mkdir) {
            FileUtils.createOrExistsDir(dir)
        }
        return FileUtils.getFileByPath(
            dir + appId + File.separator + INDEX_FILE_NAME
        )
    }

    fun createCacheFile(context: Context,dirName: String = "", fileName: String): File? {
        val child = if (dirName.isNotEmpty()) {
            "$dirName${File.separator}$fileName"
        } else {
            fileName
        }
        val f = File(getInternalCacheDirPath(context), child)
        return if (FileUtils.createOrExistsFile(f)) f else null
    }

    fun getMiniAppInnerSpace(context: Context,appId: String): String {
        return getInternalFilesDirPath(context) + appId + File.separator
    }

    fun getMiniAppOuterSpace(appId: String): String {
        return "/sdcard/ctnewcall/" + appId + File.separator
    }

    /**
     *
     * 对比版本号
     * 前者大返回 -1 相等返回 0 后者大返回 1
     *
     */
    fun compareVersion(oldVersion: String, newVersion: String): Int {
        if (TextUtils.isEmpty(oldVersion) && !TextUtils.isEmpty(newVersion)) return 1
        if (!TextUtils.isEmpty(oldVersion) && TextUtils.isEmpty(newVersion)) return -1

        val oldVersion1 = oldVersion.replace("x", "99999")
        val newVersion1 = newVersion.replace("x", "99999")
        val split1 = oldVersion1.split(".")
        val split2 = newVersion1.split(".")
        val min = split1.size.coerceAtMost(split2.size)
        var idx = 0
        var result = 0
        try {
            while (idx < min) {
                if (split1[idx].toInt() > split2[idx].toInt()) {
                    result = -1
                    break
                } else if (split1[idx].toInt() < split2[idx].toInt()) {
                    result = 1
                    break
                } else {
                    ++idx
                }
            }
        } catch (e: NumberFormatException) {
            result = 0
        }
        return result
    }

    fun getMaxPath(path1:String,path2:String):String{
        val list1 = path1.split("/")
        val list2 = path2.split("/")
        if (list1.isNotEmpty() && list2.isNotEmpty()){
            val version1 = list1[list1.size - 1]
            val version2 = list2[list2.size - 1]
            return if (compareVersion(version1,version2)>0){
                path2
            } else {
                path1
            }
        }
        return path1
    }
}
