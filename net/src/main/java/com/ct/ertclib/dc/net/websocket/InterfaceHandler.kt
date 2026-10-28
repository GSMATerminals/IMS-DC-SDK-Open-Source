package com.ct.ertclib.dc.net.websocket

import android.content.Context
import com.ct.ertclib.dc.net.data.ADCState
import com.ct.ertclib.dc.net.utils.Base64Utils
import kotlinx.coroutines.*
import okio.ByteString
import org.json.JSONArray
import org.json.JSONObject
import java.util.logging.Logger

/**
 * 接口管理器
 */
object InterfaceHandler : WebSocketClient.MessageListener {

    private const val TAG = "[SDKLog][InterfaceHandler]"
    private val sLogger = Logger.getLogger(TAG.substring(0, 23))
    private var webSocketClient: WebSocketClient? = null
    private var isConnected = false
    private var isManualDisconnect = false
    private var registerNumber: String = ""
    private var registerId: String = ""

    // 协程相关
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var heartbeatJob: Job? = null

    @Volatile
    private var initCallback: InitCallback? = null

    @Volatile
    private var callCallback: CallCallback? = null

    @Volatile
    private var dcCreateCallback: DCCreateCallback? = null

    private var dcDataCallbackMap = mutableMapOf<String, DCDataCallback>()

    /**
     * 初始化回调接口
     */
    interface InitCallback {
        fun onInitSuccess()
        fun onInitFailed(error: String)
        fun onDisconnected()
    }

    /**
     * 通话模块回调
     */
    interface CallCallback {
        fun onCallAck(
            callId: String,
            callerNumber: String,
            calleeNumber: String,
            requestStatus: String,
            reason: String,
            token: String,
            liveKitUrl: String
        )

        fun onRinging(callId: String, callerNumber: String, callFun: String, token: String, liveKitUrl: String)
        fun onCallEstablished(callId: String, callerNumber: String)
        fun onCallEnd(callId: String, reason: String, callerNumber: String, calleeNumber: String)
    }

    /**
     * BDC模块回调
     */
    interface DCCreateCallback {
        fun onNotifyADC(callId: String, adcList: ArrayList<ADCState>)
    }

    /**
     * ADC模块回调
     */
    interface DCDataCallback {
        fun onDownlinkBDC(callId: String, data: ByteArray)
        fun onDownlinkADC(callId: String, data: ByteArray)
    }

    // ========== 初始化与释放 ==========

    /**
     * 初始化并连接，自动完成注册
     * @param context Context
     * @param serverUrl 服务器地址
     * @param registerNum 注册号码
     * @param authInfo 认证信息
     * @param deviceInfo 设备信息
     * @param initCallback 初始化回调
     * @param callCallback 通话事件回调
     */
    fun init(
        context: Context,
        serverUrl: String,
        registerNum: String,
        authInfo: JSONObject,
        deviceInfo: JSONObject,
        initCallback: InitCallback,
        callCallback: CallCallback
    ) {
        // 保存回调
        this.initCallback = initCallback
        this.callCallback = callCallback
        registerNumber = registerNum

        // 重置状态
        isManualDisconnect = false

        // 创建WebSocket客户端并连接
        webSocketClient = WebSocketClient(serverUrl, this)
        webSocketClient?.connect(false)

        // 保存认证信息和设备信息，供连接成功后注册使用
        this.authInfo = authInfo
        this.deviceInfo = deviceInfo
    }

    private var authInfo: JSONObject? = null
    private var deviceInfo: JSONObject? = null

    /**
     * 释放资源并断开连接，自动注销
     */
    fun release() {
        isManualDisconnect = true

        // 如果已连接且有registerId，先注销
        if (isConnected && registerId.isNotEmpty()) {
            unregisterDevice()

            // 延迟断开，给注销消息留出发送时间
            scope.launch {
                delay(500) // 延迟500ms确保消息发送
                doRelease()
            }
        } else {
            doRelease()
        }
    }

    private fun doRelease() {
        // 停止心跳
        stopHeartbeat()

        // 断开连接
        webSocketClient?.disconnect()
        webSocketClient = null

        // 清空所有回调
        initCallback = null
        callCallback = null
        dcCreateCallback = null
        dcDataCallbackMap.clear()
        authInfo = null
        deviceInfo = null
        registerId = ""
        isConnected = false
    }

    /**
     * 是否已连接
     */
    fun isConnected(): Boolean = isConnected

    // ========== 设置各模块回调 ==========

    fun setDCCreateCallback(callback: DCCreateCallback?) {
        this.dcCreateCallback = callback
    }

    fun addDCDataCallback(label: String, callback: DCDataCallback) {
        dcDataCallbackMap[label] = callback
    }

    fun clearDCDataCallback() {
        dcDataCallbackMap.clear()
    }

    // ========== 消息接收（下行分发） ==========

    override fun onConnected() {
        isConnected = true
        startHeartbeat()

        // 连接成功后自动注册，已经注册过就不再注册
        if (registerId.isEmpty()) {
            registerDevice()
        }
    }

    override fun onMessage(text: String) {
        try {
            val json = JSONObject(text)
            val funName = json.optString("fun")
            val data = json.optJSONObject("data") ?: JSONObject()

            when (funName) {
                "downHeart" -> {
                    // 心跳响应，不需要回调给上层
                }
                "registerResult" -> {
                    val result = data.optInt("code")
                    if (result == 0) {
                        // 保存 registerId
                        val info = data.optJSONObject("info")
                        registerId = info?.optString("registerId") ?: ""
                        initCallback?.onInitSuccess()
                    } else {
                        initCallback?.onInitFailed(data.optString("message"))
                    }
                }

                "callAck" -> {
                    val callId = data.optString("callId")
                    val callerNumber = data.optString("callerNumber")
                    val calleeNumber = data.optString("calleeNumber")
                    val requestStatus = data.optString("requestStatus")
                    val reason = data.optString("reason")
                    val token = data.optString("token")
                    val liveKitUrl = data.optString("liveKitUrl")
                    callCallback?.onCallAck(callId, callerNumber, calleeNumber, requestStatus, reason, token, liveKitUrl)
                }

                "ringing" -> {
                    val callId = data.optString("callId")
                    val callerNumber = data.optString("callerNumber")
                    val callFun = data.optString("callFun")
                    val token = data.optString("token")
                    val liveKitUrl = data.optString("liveKitUrl")
                    callCallback?.onRinging(callId, callerNumber, callFun, token, liveKitUrl)
                }

                "callEstablished" -> {
                    val callId = data.optString("callId")
                    val callerNumber = data.optString("callerNumber")
                    callCallback?.onCallEstablished(callId, callerNumber)
                }

                "callEnd" -> {
                    val callId = data.optString("callId")
                    val reason = data.optString("hangupReason")
                    val callerNumber = data.optString("callerNumber")
                    val calleeNumber = data.optString("calleeNumber")
                    callCallback?.onCallEnd(callId, reason, callerNumber, calleeNumber)
                }

                "downlinkBDC" -> {
                    val callId = data.optString("callId")
                    val label = data.optString("label")
                    val base64String = data.optString("base64String")
                    val data = Base64Utils.base64ToByteArray(base64String)
                    dcDataCallbackMap[label]?.onDownlinkBDC(callId, data)
                }

                "notifyADC" -> {
                    val callId = data.optString("callId")
                    val adcJsonArray = data.optJSONArray("adcList")

                    val adcStateList = ArrayList<ADCState>()
                    adcJsonArray?.takeIf { it.length() > 0 }?.let { jsonArray ->
                        (0 until jsonArray.length())
                            .mapNotNull { index -> jsonArray.optJSONObject(index) } // 过滤掉null
                            .forEach { jsonObject ->
                                adcStateList.add(
                                    ADCState(
                                        label = jsonObject.optString("label"),
                                        state = jsonObject.optString("state")
                                    )
                                )
                            }
                    }

                    dcCreateCallback?.onNotifyADC(callId, adcStateList)
                }
                "downlinkADC" -> {
                    val callId = data.optString("callId")
                    val label = data.optString("label")
                    val base64String = data.optString("base64String")
                    val data = Base64Utils.base64ToByteArray(base64String)
                    dcDataCallbackMap[label]?.onDownlinkADC(callId, data)
                }

                else -> sLogger.info("未知消息类型: $funName text: $text")
            }
        } catch (e: Exception) {
            sLogger.info("解析消息失败 $e")
        }
    }

    override fun onBinaryMessage(bytes: ByteString) {
        sLogger.info("收到二进制消息，长度: ${bytes.size}")
    }

    override fun onDisconnected() {
        isConnected = false
        registerId = ""
        stopHeartbeat()
        if (!isManualDisconnect) {
            initCallback?.onDisconnected()
        }
    }

    // ========== 心跳 ==========

    private fun startHeartbeat() {
        stopHeartbeat()
        heartbeatJob = scope.launch {
            while (isConnected) {
                sendMessage("upHeart", JSONObject().apply {
                    put("time", System.currentTimeMillis() / 1000)
                })
                delay(5000) // 每5秒发送一次
            }
        }
    }

    private fun stopHeartbeat() {
        heartbeatJob?.cancel()
        heartbeatJob = null
    }


    // ========== 注册 ==========
    /**
     * 注册
     */
    private fun registerDevice() {
        val auth = authInfo ?: JSONObject()
        val device = deviceInfo ?: JSONObject()

        val data = JSONObject().apply {
            put("registerNumber", registerNumber)
            put("authInfo", auth)
            put("deviceInfo", device)
        }

        sendMessage("register", data)
    }

    /**
     * 注销
     */
    private fun unregisterDevice() {
        val data = JSONObject().apply {
            put("registerNumber", registerNumber)
            put("registerId", registerId)
            put("unregisterReason", "user_logout")
        }

        sendMessage("unregister", data)
    }

    // ========== 消息发送（上行封装） ==========

    private fun sendMessage(funName: String, data: JSONObject = JSONObject()): Boolean {
        return if (isConnected) {
            val message = JSONObject().apply {
                put("fun", funName)
                put("data", data)
            }.toString()
            webSocketClient?.sendMessage(message) ?: false
        } else {
            sLogger.info("发送消息失败: WebSocket未连接")
            false
        }
    }

    // ========== 对外API：通话模块 ==========

    fun makeCall(
        callerNumber: String,
        calleeNumber: String,
        callFun: String = "voice",
        callSource: String = "app"
    ): Boolean {
        val data = JSONObject().apply {
            put("callerNumber", callerNumber)
            put("calleeNumber", calleeNumber)
            put("callFun", callFun)
            put("callSource", callSource)
        }
        return sendMessage("call", data)
    }

    fun answerCall(callId: String): Boolean {
        val data = JSONObject().apply {
            put("callId", callId)
            put("answerTime", System.currentTimeMillis() / 1000)
        }
        return sendMessage("answer", data)
    }

    fun hangupCall(callId: String, reason: String = "user_hangup"): Boolean {
        val data = JSONObject().apply {
            put("callId", callId)
            put("hangupReason", reason)
        }
        return sendMessage("hangup", data)
    }

    // ========== 对外API：BDC模块 ==========

    fun sendUplinkBDC(callId: String, label: String, base64String: String): Boolean {
        val data = JSONObject().apply {
            put("callId", callId)
            put("label", label)
            put("base64String", base64String)
        }
        return sendMessage("uplinkBDC", data)
    }

    // ========== 对外API：ADC模块 ==========

    fun createADC(callId: String, labels: ArrayList<String>): Boolean {
        val data = JSONObject().apply {
            put("callId", callId)
            put("labels", JSONArray().apply {
                labels.forEach { put(it) }
            })
        }
        return sendMessage("createADC", data)
    }

    fun sendUplinkADC(callId: String, label: String, base64String: String): Boolean {
        val data = JSONObject().apply {
            put("callId", callId)
            put("label", label)
            put("base64String", base64String)
        }
        return sendMessage("uplinkADC", data)
    }

    fun closeADC(callId: String, labels: ArrayList<String>): Boolean {
        val data = JSONObject().apply {
            put("callId", callId)
            put("labels", JSONArray().apply {
                labels.forEach { put(it) }
            })
        }
        return sendMessage("closeADC", data)
    }
}