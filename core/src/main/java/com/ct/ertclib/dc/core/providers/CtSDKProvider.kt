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