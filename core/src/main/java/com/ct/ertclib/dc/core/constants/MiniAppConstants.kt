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

package com.ct.ertclib.dc.core.constants

object MiniAppConstants {

    //event
    const val EVENT_DC = "DcEvent"
    const val EVENT_MINI_APP = "MiniAppEvent"
    const val EVENT_FILE = "FileEvent"
    const val EVENT_SCREEN_SHARE = "ScreenShareEvent"
    const val EVENT_EC = "ECEvent"

    //function for dc
    const val FUNCTION_CREATE_DATA_CHANNEL = "createAppDataChannel"
    const val FUNCTION_CLOSE_DATA_CHANNEL = "closeAppDataChannel"
    const val FUNCTION_SEND_DATA = "sendData"
    const val FUNCTION_IS_PEER_SUPPORT_DC = "isPeerSupportDC"
    const val FUNCTION_GET_BUFFER_AMOUNT = "getBufferedAmount"

    //function for ec
    const val FUNCTION_EC_REQUEST = "expandingCapacityRequest"
    const val FUNCTION_EC_REGISTER = "expandingCapacityRegister"

    //function for file
    const val FUNCTION_GET_LOCATION = "getLocation"
    const val FUNCTION_SELECT_FILE = "selectFile"
    const val FUNCTION_GET_FILE_LIST = "getFileList"
    const val FUNCTION_GET_PRIVATE_FOLDER = "getPrivateFolder"
    const val FUNCTION_START_SAVE_FILE = "startSaveFile"
    const val FUNCTION_SAVE_FILE = "saveFile"
    const val FUNCTION_STOP_SAVE_FILE = "stopSaveFile"
    const val FUNCTION_DELETE_FILE = "deleteFile"
    const val FUNCTION_START_READ_FILE = "startReadFile"
    const val FUNCTION_READ_FILE = "readFile"
    const val FUNCTION_STOP_READ_FILE = "stopReadFile"
    const val FUNCTION_DECOMPRESS_FILE = "decompressFile"
    const val FUNCTION_CHECK_FILE_EXISTS = "checkFileOrFolderExists"
    const val FUNCTION_GET_FILE_INFO = "getFileInfo"
    const val FUNCTION_GET_FILE_INFO_ASYNC = "getFileInfoAsync"
    const val FUNCTION_SAVE_UPDATE_KEY_VALUE = "saveUpdateKeyValue"
    const val FUNCTION_GET_KEY_VALUE = "getKeyValue"
    const val FUNCTION_DELETE_KEY_VALUE = "deleteKeyValue"
    const val FUNCTION_PLAY_VOICE = "playVoice"
    const val FUNCTION_STOP_PLAY_VOICE = "stopPlayVoice"

    //function for miniapp
    const val FUNCTION_GET_MINI_APP_INFO = "getMiniAppInfo"
    const val FUNCTION_GET_SDK_INFO = "getSDKInfo"
    const val FUNCTION_GET_SCREEN_INFO = "getScreenInfo"
    const val FUNCTION_START_APP = "startApp"
    const val FUNCTION_SET_WINDOW = "setWindow"
    const val FUNCTION_GET_REMOTE_NUMBER = "getRemoteNumber"
    const val FUNCTION_GET_HTTP_RESULT = "getHttpResult"
    const val FUNCTION_HANG_UP = "hangUp"
    const val FUNCTION_GET_CALL_STATE = "getCallState"
    const val FUNCTION_REQUEST_START_ADVERSE_APP = "requestStartAdverseApp"
    const val FUNCTION_ADD_CONTACT = "addContact"
    const val FUNCTION_GET_CONTACT_NAME = "getContactName"
    const val FUNCTION_GET_CONTACT_LIST = "getContactList"
    const val FUNCTION_SET_SYSTEM_API_LICENSE = "setSystemApiLicense"
    const val FUNCTION_OPEN_WEB = "openWeb"
    const val FUNCTION_MOVE_TO_FRONT = "moveToFront"
    const val FUNCTION_STOP_APP = "stopApp"
    const val FUNCTION_GET_SHARE_TYPE_NAME = "getShareTypeName"
    const val FUNCTION_PLAY_DTMF_TONE = "playDtmfTone"
    const val FUNCTION_SET_SPEAKERPHONE = "setSpeakerphone"
    const val FUNCTION_IS_SPEAKERPHONE_ON = "isSpeakerphoneOn"
    const val FUNCTION_SET_MUTED = "setMuted"
    const val FUNCTION_IS_MUTED = "isMuted"

    //function for screen share
    const val FUNCTION_START_SCREEN_SHARE = "startScreenShare"
    const val FUNCTION_STOP_SCREEN_SHARE = "stopScreenShare"
    const val FUNCTION_REQUEST_SCREEN_SHARE_ABILITY = "requestScreenShareAbility"
    const val FUNCTION_OPEN_SKETCH_BOARD = "openSketchBoard"
    const val FUNCTION_CLOSE_SKETCH_BOARD = "closeSketchBoard"
    const val FUNCTION_DRAWING_INFO = "addDrawingInfo"
    const val FUNCTION_REMOTE_SIZE_INFO = "addRemoteSizeInfo"
    const val FUNCTION_SET_PRIVACY_MODE = "setScreenSharePrivacyMode"
    const val FUNCTION_REMOTE_WINDOW_SIZE_INFO = "addRemoteWindowSizeInfo"

    //function for notify
    const val FUNCTION_NOTIFY_DATA_CHANNEL = "dataChannelNotify"
    const val FUNCTION_NOTIFY_MESSAGE = "messageNotify"
    const val FUNCTION_CALL_STATE_NOTIFY = "callStateNotify"
    const val FUNCTION_MINI_APP_NOTIFY = "miniAppStateNotify"
    const val FUNCTION_DRAWING_INO_NOTIFY = "drawingInfoNotify"
    const val FUNCTION_SKETCH_STATUS_NOTIFY = "sketchStatusNotify"
    const val FUNCTION_SCREEN_SHARE_NOTIFY = "screenShareStatusNotify"
    const val FUNCTION_SCREEN_SIZE_NOTIFY = "screenSizeNotify"
    const val FUNCTION_EC_NOTIFY = "expandingCapacityNotify"
    const val FUNCTION_START_ADVERSE_APP_RESPONSE_NOTIFY = "startAdverseAppResponseNotify"
    const val FUNCTION_AUDIO_DEVICE_NOTIFY = "audioDeviceChangeNotify"
    const val FUNCTION_VIDEO_WINDOW_NOTIFY = "videoWindowNotify"

    //response status
    const val RESPONSE_SUCCESS_CODE = "0"
    const val RESPONSE_FAILED_CODE = "-1"
    const val RESPONSE_SUCCESS_MESSAGE = "success"
    const val RESPONSE_FAILED_MESSAGE = "failed"

    //Screen Share Event Result
    const val START_SHARE_SUCCESS = "0"
    const val START_SHARE_FAILED_OCCUPIED = "-1"
    const val START_SHARE_SERVICE_ERROR = "-2"
    const val START_SHARE_LICENSE_ERROR = "-3"
    const val REQUEST_ABILITY_SUCCESS = "0"
    const val REQUEST_ABILITY_FAILED = "1"
    const val CONTACT_ADD_MODE = "0"
    const val CONTACT_EDIT_MODE = "1"

    const val ROLE_SHARE_SIDE = 1
    const val ROLE_WATCH_SIDE = 2

    const val START_SHARE_PARAMS = "resultStatus"
    const val REQUEST_ABILITY_PARAMS = "result"
    const val IS_PEER_SUPPORT_DC_PARAMS = "isPeerSupportDc"
    const val ADD_CONTACT_NAME_PARAM = "name"
    const val ADD_CONTACT_NUMBER_PARAM = "number"
    const val ADD_CONTACT_MODE = "contactMode"
    const val GET_CONTACT_NAME_NUMBER_PARAM = "contactNumber"
    const val GET_CONTACT_LIST_OFFSET_PARAM = "offset"
    const val GET_CONTACT_LIST_LIMIT_PARAM = "limit"
    const val KEY_PARAM = "key"
    const val VALUE_PARAM = "value"
    const val DIGIT = "digit"
    const val SPEAKERPHONE_ON = "speakerphoneOn"
    const val MUTED = "muted"
    const val IS_SPEAKERPHONE_ON = "isSpeakerphoneOn"
    const val IS_MUTED = "isMuted"

    const val LICENSE = "license"
    const val API = "api"
    const val URL = "url"
    const val TITLE = "title"
    const val HTTP_WAY = "httpWay"
    const val PARAMS_JSON = "paramsJson"
    const val PARAMS_MEDIA_TYPE = "mediaType"
    const val PARAMS_HEADER = "header"

    const val NOTIFY_STATUS_PARAM = "status"
    const val NOTIFY_DRAWING_INFO_PARAM = "drawingInfo"
    const val NOTIFY_WIDTH_PARAM = "width"
    const val NOTIFY_HEIGHT_PARAM = "height"

    const val STYLE_DEFAULT = 0
    const val STYLE_WHITE = 1

    const val HTTP_GET_WAY = "GET"
    const val HTTP_POST_WAY = "POST"
    const val MEDIA_TYPE_DEFAULT = "application/json;charset=utf-8"
    const val PARAMS_RESPONSE = "response"
}