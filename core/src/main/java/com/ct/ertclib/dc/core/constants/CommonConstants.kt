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

package com.ct.ertclib.dc.core.constants

// SDK 内部使用的常量
object CommonConstants {

    const val MINI_APP_ROOT_PATH = "mini_app/"
    const val INDEX_FILE_NAME = "index.html"

    const val DC_ORDER: String = "dc_order"
    const val DC_USECASE: String = "dc_usecase"
    const val DC_BANDWIDTH: String = "dc_bandwidth"
    const val DC_QOSHINT: String = "dc_qoshint"
    const val DC_PRIORITY: String = "dc_priority"
    const val DC_MAXRETR: String = "dc_maxretr"
    const val DC_MAXTIME: String = "dc_maxtime"
    const val DC_AUTOACCEPTDCSETUP: String = "dc_autoacceptdcsetup"
    const val DC_SUBPROTOCOL: String = "dc_subprotocol"

    const val DC_LABEL_CONTROL:String = "miniappcontrollll"
    const val DC_APPID_ROOT:String = "x"//
    const val DC_APPID_RING:String = "xx"//
    const val DC_LABEL_ROOT:String = "p2a"
    const val DC_YI_SHARE:String = "翼分享"

    const val DC_SEND_DATA_OK: Int = 20000 //发送成功
    const val DC_SEND_DATA_CACHE_FULL: Int = 20001 //发送缓存已满
    const val DC_SEND_DATA_CLOSED: Int = 20002 //dc 已失败/断开
    const val DC_SEND_DATA_ERR_ARGUMENTS: Int = 20003 //发送参数错误
    const val DC_SEND_DATA_ERR_UNKNOWN: Int = 20004 //未知错误

    const val PERCENT_CONSTANTS = 100

    const val MINI_APP_LIST_PAGE_SIZE = 100

    const val AS_MODULE_PLATFORM_INFO_EVENT = "platformInfo"
    const val AS_MODULE_ADLIST_INFO_EVENT = "adListInfo"


    const val APP_RESPONSE_CODE_SUCCESS = 0
    const val APP_RESPONSE_MESSAGE_SUCCESS = "success"
    const val APP_DRAWING_INFO_PARAMS = "drawingInfo"
    const val APP_COLOR_PARAMS = "paintColor"
    const val APP_WIDTH_PARAMS = "paintWidth"
    const val APP_REMOTE_WIDTH_PARAM = "width"
    const val APP_REMOTE_HEIGHT_PARAM = "height"
    const val APP_LICENSE_PARAM = "license"
    const val APP_IS_ENABLE = "isEnable"

    const val PARAMS_APP_ID = "app_id"
    const val PARAMS_CALL_ID = "call_id"
    const val PARAMS_VERSION_CODE = "version"

    //message_type
    const val REQUEST_MESSAGE_TYPE = 1
    const val RESPONSE_MESSAGE_TYPE = 16

    const val FLOATING_DISPLAY = 1
    const val FLOATING_DISMISS = 2

    const val SHARE_PREFERENCE_CONSTANTS = "user"
    const val SHARE_PREFERENCE_STYLE_PARAMS = "style"

    const val SDK_PRIVACY_VERSION_URL = ""
    const val SDK_PRIVACY_URL = ""
    const val SDK_USER_SERVICE_URL = ""
}