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

import android.Manifest
import android.annotation.SuppressLint
import android.content.ContentResolver
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.ContactsContract
import android.telecom.Call
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import com.blankj.utilcode.util.Utils
import com.ct.ertclib.dc.core.common.sdkpermission.SDKPermissionUtils
import com.ct.ertclib.dc.core.data.call.Contact
import com.ct.ertclib.dc.core.port.dao.ContactDao
import com.macoli.reflect_helper.ReflectHelper
import java.util.Arrays
import kotlin.collections.mutableListOf


object CallUtils {
    private const val TAG = "CallUtils"
    @SuppressLint("MissingPermission")
    fun isCtCall(subId: Int): Boolean {

        val ctMccMncList = mutableListOf("460-03","460-05", "460-11", "460-12")
        val mccMnc = getMccMnc(Utils.getApp(), subId)
        LogUtils.debug(TAG,"mccMnc:${mccMnc?.contentToString()}")
        mccMnc?.apply {
            var mcc = get(0).toString()
            var mnc = get(1).toString()
            if (mnc.length == 1) {
                mnc = "0$mnc"
            }
            if (ctMccMncList.contains("$mcc-$mnc")) {
                return true
            }
        }
        return false
    }

    /**
     * Get MCC/MNC of an SIM subscription
     *
     * @param context the Context to use
     * @param subId the SIM subId
     * @return a non-empty array with exactly two elements, first is mcc and last is mnc.
     */
    @SuppressLint("MissingPermission", "NewApi")
    private fun getMccMnc(context: Context, subId: Int): IntArray? {
        val mccMnc = intArrayOf(0, 0)
        val subscriptionManager = SubscriptionManager.from(context)
        val subInfo = subscriptionManager.getActiveSubscriptionInfo(subId)
        if (subInfo != null) {
            mccMnc[0] = subInfo.mcc
            mccMnc[1] = subInfo.mnc
        }
        return mccMnc
    }

    @SuppressLint("NewApi")
    fun isConference(call: Call?): Boolean {
        if (call == null) {
            LogUtils.debug(TAG,"CallsManager isConference - failed due to onCallAdded call is null")
            return false
        }
        return call.details.callProperties == 1
    }

    @SuppressLint("NewApi")
    fun isOutgoingCall(call: Call?): Boolean {
        if (call == null) {
            LogUtils.debug(TAG,"CallsManager isOutgoingCall - failed due to onCallAdded call is null")
            return false
        }

        return call.details.callDirection == 1
    }

    @SuppressLint("NewApi")
    fun getRemoteNumber(call: Call?): String? {
        if (call == null) {
            LogUtils.debug(TAG,"CallsManager getRemoteNumber - failed due to onCallAdded call is null")
            return null
        }

        if (call.details.gatewayInfo != null) {
            return call.details.gatewayInfo.originalAddress.schemeSpecificPart
        }

        if (call.details.handle != null) {
            return call.details.handle.schemeSpecificPart
        }
        return null
    }

    @SuppressLint("NewApi")
    fun getTelecomCallId(call: Call?): String? {
        if (call == null) {
            LogUtils.debug(TAG,"CallsManager getTelecomCallId - failed due to onCallAdded call is null")
            return null
        }

        val details = call.details
        try {
            val method = ReflectHelper.getDeclaredMethod(
                details.javaClass,
                "getTelecomCallId",
                arrayOfNulls<Class<*>>(0)
            )
            return method.invoke(details) as String?
        } catch (e: Exception) {
            LogUtils.debug(TAG,"CallsManager getTelecomCallId${e.printStackTrace()}")
        }
        return null
    }

    @SuppressLint("MissingPermission", "NewApi")
    fun getSubId(call: Call?): Int {
        if (call == null) {
            LogUtils.debug(TAG,"CallsManager getSubId - failed due to onCallAdded call is null")
            return -1
        }
        if (!SDKPermissionUtils.checkPermissions(Utils.getApp(), Manifest.permission.READ_PHONE_STATE)) {
            LogUtils.debug(TAG,"CallsManager getSubId - failed due to no android.permission.READ_PHONE_STATE")
            return -1
        }

        return (Utils.getApp().getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager)
            .getSubscriptionId(call.details.accountHandle)
    }

    @SuppressLint("NewApi")
    fun getSlotId(subId: Int): Int {
        return SubscriptionManager.getSlotIndex(subId)
    }

    @SuppressLint("Range")
    fun getContactName( context: Context,phoneNumber: String): String? {
        var contactName: String? = null
        val contentResolver = context.contentResolver
        val uri = Uri.withAppendedPath(
            ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
            Uri.encode(phoneNumber)
        )
        val cursor = contentResolver.query(
            uri,
            arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME),
            null, null, null
        )

        if (cursor != null) {
            if (cursor.moveToFirst()) {
                contactName = cursor.getString(
                    cursor.getColumnIndex(
                        ContactsContract.PhoneLookup.DISPLAY_NAME
                    )
                )
            }
            cursor.close()
        }

        return contactName
    }

    // TODO: license控制

    /**
     * 获取联系人列表（分页）
     * @param offset 起始位置（从 0 开始）
     * @param limit 查询多少个
     * @return 联系人列表
     */
    fun getContactList(context: Context,offset: Int,limit:Int): List<Contact> {
        val contacts = mutableListOf<Contact>()
        val contentResolver: ContentResolver = context.contentResolver

        // 查询的列
        val projection = arrayOf(
            ContactsContract.Contacts._ID,
            ContactsContract.Contacts.DISPLAY_NAME
        )

        // 查询联系人
        val cursor: Cursor? = contentResolver.query(
            ContactsContract.Contacts.CONTENT_URI,
            projection,
            null,
            null,
            "${ContactsContract.Contacts.DISPLAY_NAME} ASC LIMIT $limit OFFSET $offset"
        )

        cursor?.use {
            val idColumn = it.getColumnIndex(ContactsContract.Contacts._ID)
            val nameColumn = it.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME)

            while (it.moveToNext()) {
                val id = it.getLong(idColumn)
                val name = it.getString(nameColumn)
                val phoneNumber = getPhoneNumber(contentResolver, id)
                contacts.add(Contact(id, name, phoneNumber))
            }
        }

        return contacts
    }

    /**
     * 获取联系人总数
     * @return 联系人总数，如果失败则返回 -1
     */
    fun getContactCount(context: Context): Int {
        val contentResolver: ContentResolver = context.contentResolver

        // 查询的列
        val projection = arrayOf(ContactsContract.Contacts._ID)

        // 查询联系人
        val cursor: Cursor? = contentResolver.query(
            ContactsContract.Contacts.CONTENT_URI,
            projection,
            null,
            null,
            null
        )

        return cursor?.use {
            it.count // 返回联系人总数
        } ?: -1 // 如果 cursor 为 null，返回 -1
    }

    /**
     * 获取联系人的电话号码
     */
    private fun getPhoneNumber(contentResolver: ContentResolver, contactId: Long): String? {
        var phoneNumber: String? = null
        val phoneCursor: Cursor? = contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER),
            "${ContactsContract.CommonDataKinds.Phone.CONTACT_ID} = ?",
            arrayOf(contactId.toString()),
            null
        )

        phoneCursor?.use {
            if (it.moveToFirst()) {
                val numberColumn = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                phoneNumber = it.getString(numberColumn)
            }
        }

        return phoneNumber
    }

}