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

package com.ct.ertclib.dc.app.utils

import android.content.Context
import android.widget.Toast

object ToastUtils {

    fun showShortToast(context: Context, resId: Int) {
        val toast = Toast.makeText(context, context.resources.getString(resId), Toast.LENGTH_SHORT)
        toast.show()
    }

    fun showShortToast(context: Context, toastString: String) {
        val toast = Toast.makeText(context, toastString, Toast.LENGTH_SHORT)
        toast.show()
    }

    fun showLongToast(context: Context, resId: Int) {
        val toast = Toast.makeText(context, context.resources.getString(resId), Toast.LENGTH_LONG)
        toast.show()
    }

    fun showLongToast(context: Context, toastString: String) {
        val toast = Toast.makeText(context, toastString, Toast.LENGTH_LONG)
        toast.show()
    }
}