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

package com.ct.ertclib.dc.core.utils.extension

import android.content.Context
import android.content.Intent
import android.provider.ContactsContract
import com.ct.ertclib.dc.core.constants.ContextConstants
import com.ct.ertclib.dc.core.constants.ContextConstants.TYPE_CONTACT_CONTACT
import com.ct.ertclib.dc.core.constants.ContextConstants.TYPE_CONTACT_PERSON
import com.ct.ertclib.dc.core.constants.ContextConstants.TYPE_CONTACT_RAW
import com.ct.ertclib.dc.core.data.call.CallInfo


fun Context.startAddContactActivity(name: String, number: String) {
    val intent = Intent(Intent.ACTION_INSERT, ContactsContract.Contacts.CONTENT_URI)
    intent.putExtra(ContactsContract.Intents.Insert.NAME, name)
    intent.putExtra(ContactsContract.Intents.Insert.PHONE, number)
    this.startActivity(intent)
}


fun Context.startEditContactActivity(number: String) {
    val intent = Intent(Intent.ACTION_INSERT_OR_EDIT)
    intent.setType(TYPE_CONTACT_PERSON)
    intent.setType(TYPE_CONTACT_CONTACT)
    intent.setType(TYPE_CONTACT_RAW)
    intent.putExtra(ContactsContract.Intents.Insert.PHONE, number)
    this.startActivity(intent)
}

fun Context.startLocalTestActivity() {
    val intent = Intent(ContextConstants.INTENT_TEST_ACTIVITY)
    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
    this.startActivity(intent)
}

fun Context.startSettingsActivity() {
    val intent = Intent(ContextConstants.INTENT_SETTINGS_ACTIVITY)
    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
    this.startActivity(intent)
}