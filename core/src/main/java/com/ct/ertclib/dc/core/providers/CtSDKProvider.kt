/*
 * Copyright 2025-China Telecom Research Institute.
 * All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ct.ertclib.dc.core.providers
import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import com.blankj.utilcode.util.Utils
import com.ct.ertclib.dc.core.common.sdkpermission.SDKPermissionUtils

class CtSDKProvider : ContentProvider() {

    companion object {
        const val CHECK_SDK_PERMISSIONS = "checkSDKPermissions"
    }

    override fun onCreate(): Boolean {
        // 这里可以初始化你的数据
        return true
    }

    override fun call(method: String, arg: String?, extras: Bundle?): Bundle? {
        return when (method) {
            CHECK_SDK_PERMISSIONS -> checkSDKPermissions(extras)
            else -> throw IllegalArgumentException("Unknown method: $method")
        }
        return super.call(method, arg, extras)
    }

    private fun checkSDKPermissions(bundle: Bundle?): Bundle? {
        return Bundle().apply {
            putBoolean("hasAllPermissions", SDKPermissionUtils.hasAllPermissions(Utils.getApp()))
        }
    }


    override fun query(uri: Uri, projection: Array<String>?, selection: String?, selectionArgs: Array<String>?, sortOrder: String?): Cursor? = null
    override fun getType(uri: Uri): String? = null
    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<String>?): Int = 0
    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<String>?): Int = 0


}