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

package com.ct.ertclib.dc.core.data.common;

import android.content.Context;
import android.text.TextUtils;
import com.ct.ertclib.dc.core.utils.common.ToastUtils;

public enum DcResult {

    DC_OPENING,
    DC_OPENED,
    DC_OPEN_FAILED,
    DC_CLOSED,
    DC_SEND_SUCCESS,

    DC_SEND_FAILED;

    public static boolean isSendSuccess(Context context, DcResult status) {
        String tip = null;
        if (status == DC_OPENING) {
            tip = "网络正在准备中";
        } else if (status == DC_OPEN_FAILED) {
            tip = "网络异常";
        } else if (status == DC_SEND_SUCCESS) {
            return true;
        } else if (status == DC_SEND_FAILED) {
            tip = "发送失败";
        }
        if (!TextUtils.isEmpty(tip)) {
            ToastUtils.INSTANCE.showShortToast(context, tip);
        }
        return false;
    }
}
