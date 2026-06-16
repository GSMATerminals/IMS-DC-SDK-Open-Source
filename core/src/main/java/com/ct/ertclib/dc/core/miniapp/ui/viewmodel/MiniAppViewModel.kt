package com.ct.ertclib.dc.core.miniapp.ui.viewmodel

import android.content.Context
import android.media.MediaPlayer
import android.os.RemoteException
import android.telecom.Call
import com.ct.ertclib.dc.core.constants.MiniAppConstants
import com.ct.ertclib.dc.core.data.call.CallInfo
import com.ct.ertclib.dc.core.data.event.CloseAdcEvent
import com.ct.ertclib.dc.core.data.miniapp.MiniAppList
import com.ct.ertclib.dc.core.data.model.MiniAppInfo
import com.ct.ertclib.dc.core.manager.call.NewCallsManager
import com.ct.ertclib.dc.core.manager.common.StateFlowManager
import com.ct.ertclib.dc.core.manager.screenshare.ScreenShareHelper
import com.ct.ertclib.dc.core.miniapp.MiniAppManager
import com.ct.ertclib.dc.core.miniapp.db.MiniAppDbRepo
import com.ct.ertclib.dc.core.port.call.ICallStateListener
import com.ct.ertclib.dc.core.port.common.OnPickMediaCallbackListener
import com.ct.ertclib.dc.core.port.dc.IDcCreateListener
import com.ct.ertclib.dc.core.port.usecase.mini.IPermissionUseCase
import com.ct.ertclib.dc.core.utils.common.DCUtils
import com.ct.ertclib.dc.core.utils.common.JsonUtil
import com.ct.ertclib.dc.core.utils.common.MiniAppPermissionHelper
import com.ct.ertclib.dc.core.utils.logger.Logger
import com.newcalllib.datachannel.V1_0.IImsDCObserver
import com.newcalllib.datachannel.V1_0.IImsDataChannel
import com.newcalllib.datachannel.V1_0.ImsDCStatus
import kotlinx.coroutines.*
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.ArrayList
import java.util.Collections

class MiniAppViewModel : KoinComponent {

    companion object {
        private const val TAG = "MiniAppViewModel"
        const val MIC_STATUS_OPEN = 0
        const val MIC_STATUS_MUTE = 1
        const val SPEAKER_STATUS_NORMAL = 0
        const val SPEAKER_STATUS_OPEN = 1
        const val PHONE_BUTTON_HIDE = 0
        const val PHONE_BUTTON_RINGING = 1
        const val PHONE_BUTTON_CALLING = 2
    }

    private val sLogger = Logger.getLogger(TAG)
    private val permissionUseCase: IPermissionUseCase by inject()
    private val miniAppDbRepo: MiniAppDbRepo by lazy { MiniAppDbRepo() }

    private var mediaPlayer: MediaPlayer? = null
    var miniAppInfo: MiniAppInfo? = null
    var callInfo: CallInfo? = null
    var miniAppListInfo: MiniAppList? = null

    val systemApiLicenseMap = mutableMapOf<String, String>()
    private val serialDispatcher = Dispatchers.IO.limitedParallelism(1)
    private val viewModelScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    // UI 状态 - 使用 Observable 模式
    private val _phoneButtonShowStatus = ObservableValue(PHONE_BUTTON_HIDE)
    val phoneButtonShowStatus: ObservableValue<Int> = _phoneButtonShowStatus

    private val _micStatus = ObservableValue(MIC_STATUS_OPEN)
    val micStatus: ObservableValue<Int> = _micStatus

    private val _speakerStatus = ObservableValue(SPEAKER_STATUS_NORMAL)
    val speakerStatus: ObservableValue<Int> = _speakerStatus

    // 事件回调 - 由 View 注入
    private var onCallHandler: ((String, Array<Any>) -> Unit)? = null
    private var onFinish: (() -> Unit)? = null
    private var onSelectFiles: ((Int?, List<String>?, OnPickMediaCallbackListener) -> Unit)? = null
    private var onSetPageName: ((String) -> Unit)? = null
    private var onSetWindowStyle: (() -> Unit)? = null
    private var onCallStateChanged: ((Int) -> Unit)? = null

    private var callStateListener: ICallStateListener? = null

    // 数据通道相关
    val createDCLabelList: MutableList<String> = Collections.synchronizedList(ArrayList())
    val openDCList: MutableList<IImsDataChannel> = Collections.synchronizedList(ArrayList())

    // ==================== 事件回调设置 ====================

    fun setEventCallbacks(
        onCallHandler: ((String, Array<Any>) -> Unit)? = null,
        onFinish: (() -> Unit)? = null,
        onSelectFiles: ((Int?, List<String>?, OnPickMediaCallbackListener) -> Unit)? = null,
        onSetPageName: ((String) -> Unit)? = null,
        onSetWindowStyle: (() -> Unit)? = null,
        onCallStateChanged: ((Int) -> Unit)? = null
    ) {
        this.onCallHandler = onCallHandler
        this.onFinish = onFinish
        this.onSelectFiles = onSelectFiles
        this.onSetPageName = onSetPageName
        this.onSetWindowStyle = onSetWindowStyle
        this.onCallStateChanged = onCallStateChanged
    }

    fun init() {
        registerDCCallBack()
        registerParentToMiniCallback()
        ScreenShareHelper.init()
    }

    // ==================== 音频控制 ====================

    fun toggleMic() {
        val newStatus = if (_micStatus.value == MIC_STATUS_MUTE) MIC_STATUS_OPEN else MIC_STATUS_MUTE
        _micStatus.value = newStatus
        setMicMute(newStatus == MIC_STATUS_MUTE)
    }

    fun toggleSpeaker() {
        val newStatus = if (_speakerStatus.value == SPEAKER_STATUS_NORMAL) SPEAKER_STATUS_OPEN else SPEAKER_STATUS_NORMAL
        _speakerStatus.value = newStatus
        setSpeakerOn(newStatus == SPEAKER_STATUS_OPEN)
    }

    private fun setMicMute(mute: Boolean) {
        NewCallsManager.instance.setMuted(mute)
    }

    private fun setSpeakerOn(open: Boolean) {
        NewCallsManager.instance.setSpeakerphone(open)
    }

    fun hangup() {
        callInfo?.telecomCallId?.let { NewCallsManager.instance.hangUp(it) }
    }

    fun answer() {
        callInfo?.telecomCallId?.let { NewCallsManager.instance.answer(it) }
    }

    // ==================== 音频播放 ====================

    fun playVoice(path: String) {
        mediaPlayer?.release()
        mediaPlayer = MediaPlayer().apply {
            setDataSource(path)
            prepareAsync()
            setOnPreparedListener { it.start() }
            setOnCompletionListener { release() }
        }
    }

    fun stopPlayVoice() {
        mediaPlayer?.release()
        mediaPlayer = null
    }

    // ==================== 权限管理 ====================

    fun checkMiniAppPermissions(
        context: Context,
        miniAppInfo: MiniAppInfo,
        onPermissionsResult: () -> Unit,
        onPermissionNotGranted: () -> Unit
    ) {
        MiniAppPermissionHelper.checkPermissions(
            context = context,
            miniAppInfo = miniAppInfo,
            callback = object : MiniAppPermissionHelper.PermissionCallback {
                override fun onAllPermissionsGranted() {
                    onPermissionsResult()
                }

                override fun onPermissionDenied() {
                    onPermissionNotGranted()
                }
            }
        )
    }
    fun refreshPermission() {
        viewModelScope.launch(Dispatchers.IO) {
            miniAppInfo?.appId?.let { permissionUseCase.refreshPermissionMapFromRepo(it) }
        }
    }

    // ==================== 数据持久化 ====================

    fun saveMiniAppToDb(miniApp: MiniAppInfo) {
        sLogger.debug("saveMiniAppToDb miniApp: $miniApp")
        miniAppDbRepo.upsert(miniApp)
    }

    fun updatePhoneButtonVisibility(showPhoneButton: Boolean, callState: Int) {
        if (showPhoneButton && callInfo != null) {
            _phoneButtonShowStatus.value = when (callState) {
                Call.STATE_RINGING -> {
                    PHONE_BUTTON_RINGING
                }
                Call.STATE_ACTIVE -> {
                    PHONE_BUTTON_CALLING
                }
                else -> {
                    PHONE_BUTTON_HIDE
                }
            }
        } else {
            _phoneButtonShowStatus.value = PHONE_BUTTON_HIDE
        }
    }

    // ==================== WebView 通知 ====================

    fun notifyIMEHeight(height: Int) {
        onCallHandler?.invoke(
            MiniAppConstants.FUNCTION_IME_HEIGHT_NOTIFY,
            arrayOf(JsonUtil.toJson(mapOf("imeHeight" to height)))
        )
    }

    fun notifyMiniAppState(state: String) {
        onCallHandler?.invoke(
            MiniAppConstants.FUNCTION_MINI_APP_NOTIFY,
            arrayOf(JsonUtil.toJson(mapOf("miniAppState" to state)))
        )
    }

    fun notifyAudioDeviceChange() {
        onCallHandler?.invoke(MiniAppConstants.FUNCTION_AUDIO_DEVICE_NOTIFY, arrayOf())
    }

    fun notifyCallStateChange(newCallState: Int) {
        callInfo?.state = newCallState
        onCallStateChanged?.invoke(newCallState)
        onCallHandler?.invoke(
            MiniAppConstants.FUNCTION_CALL_STATE_NOTIFY,
            arrayOf(JsonUtil.toJson(mapOf("state" to newCallState)))
        )
    }

    fun setWindowStyle() {
        onSetWindowStyle?.invoke()
    }

    fun notifyPageNameChanged(pageName: String) {
        onSetPageName?.invoke(pageName)
    }

    fun callHandler(function: String, params: Array<Any>) {
        onCallHandler?.invoke(function, params)
    }

    // ==================== 数据通道 ====================

    fun registerDCCallBack() {
        sLogger.debug("registerDCCallBack callInfo: $callInfo, miniAppInfo: $miniAppInfo")
        val appId = miniAppInfo?.appId
        val telecomCallId = callInfo?.telecomCallId
        appId?.let {
            MiniAppManager.getAppPackageManager(telecomCallId)?.registerAppDataChannelCallbackInternal(it, DcCreateListener())
        }
    }

    fun unregisterDCCallBack() {
        val appId = miniAppInfo?.appId
        val telecomCallId = callInfo?.telecomCallId
        appId?.let {
            MiniAppManager.getAppPackageManager(telecomCallId)?.unregisterAppDataChannelCallbackInternal(it)
        }
    }

    fun registerParentToMiniCallback(){
        miniAppInfo?.appId?.let { appId ->
            callStateListener = object : ICallStateListener {
                override fun onCallAdded(context: Context, callInfo: CallInfo) {
                    if (sLogger.isDebugActivated) {
                        sLogger.debug("CallStatusListener onCallAdded")
                    }
                    notifyCallStateChange(callInfo.state)
                }

                override fun onCallRemoved(context: Context, callInfo: CallInfo) {
                    if (sLogger.isDebugActivated) {
                        sLogger.debug("CallStatusListener onCallRemoved")
                    }
                    notifyCallStateChange(callInfo.state)
                }

                override fun onCallStateChanged(callInfo: CallInfo, state: Int) {
                    if (sLogger.isDebugActivated) {
                        sLogger.debug("CallStatusListener onCallStateChanged")
                    }
                    notifyCallStateChange(callInfo.state)
                }

                override fun onAudioDeviceChange() {
                    notifyAudioDeviceChange()
                }
            }
            callStateListener?.let { MiniAppManager.getAppPackageManager(callInfo?.telecomCallId)?.registerCallStateChangeCallbackInternal(appId, it) }
        }
    }

    fun unregisterParentToMiniCallback() {
        callInfo?.telecomCallId?.let {
            miniAppInfo?.appId?.let { appId ->
                callStateListener?.let { iCallStateListener ->
                    MiniAppManager.getAppPackageManager(it)?.unregisterCallStateListenerInternal(appId, iCallStateListener)
                }
            }
        }
    }

    fun createDC(labels: List<String>, description: String): Int? {
        val appId = miniAppInfo?.appId
        val telecomCallId = callInfo?.telecomCallId

        if (labels.isEmpty() || appId.isNullOrEmpty() || description.isEmpty()) {
            return 1
        }

        labels.forEach {
            if (it.contains("_1_") && MiniAppManager.getAppPackageManager(telecomCallId)?.isPeerSupportDc() != true) {
                sLogger.error("cannot create P2P when peer not support DC")
                return 1
            }
        }

        return MiniAppManager.getAppPackageManager(telecomCallId)
            ?.createApplicationDataChannelsInternal(appId, labels.toTypedArray(), description) ?: 1
    }

    fun closeDC(label: String) {
        sLogger.debug("closeDC label:$label")
        createDCLabelList.remove(label)
        openDCList.firstOrNull { DCUtils.compareDCLabel(it.dcLabel, label) }?.let {
            onDataChannelStateChanged(it, ImsDCStatus.DC_STATE_CLOSED, 0)
            if (it.state != ImsDCStatus.DC_STATE_CLOSING && it.state != ImsDCStatus.DC_STATE_CLOSED) {
                it.unregisterObserver()
                it.close()
            }
        }
        onCallHandler?.invoke(
            MiniAppConstants.FUNCTION_NOTIFY_DATA_CHANNEL,
            arrayOf(JsonUtil.toJson(mapOf("dcLabel" to label, "imsDCStatus" to ImsDCStatus.DC_STATE_CLOSED.ordinal)))
        )
    }

    fun selectFiles(maxSelectable: Int?, fileTypes: List<String>?, callback: OnPickMediaCallbackListener){
        onSelectFiles?.invoke(maxSelectable, fileTypes, callback)
    }

    fun finishMiniApp() {
        onFinish?.invoke()
    }

    private fun onDataChannelStateChanged(dc: IImsDataChannel, status: ImsDCStatus?, errorCode: Int) {
        sLogger.info("onDataChannelStateChanged")
        if (status == ImsDCStatus.DC_STATE_CLOSED) {
            StateFlowManager.emitCloseAdcEvent(
                CloseAdcEvent(0, CloseAdcEvent.CLOSE_ADC, miniAppInfo?.appId, dc)
            )
        }
    }

    // ==================== 内部类 ====================

    inner class DcCreateListener : IDcCreateListener {
        override fun onDataChannelCreated(telecomCallId: String, streamId: String, imsDataChannel: IImsDataChannel) {
            try {
                sLogger.info("IDCCallback.Stub onDcCreated callId:$telecomCallId, streamId:$streamId, dcLabel:${imsDataChannel.dcLabel}")
                val dcLabel = imsDataChannel.dcLabel
                if (!createDCLabelList.contains(dcLabel)) {
                    createDCLabelList.add(dcLabel)
                }
                try {
                    imsDataChannel.registerObserver(ImsDCObserverImpl(dcLabel, imsDataChannel))
                } catch (e: RemoteException) {
                    sLogger.error("IDCCallback.Stub onDcCreated register observer error:${e.message}")
                }
                onDataChannelStateChange(imsDataChannel)
            } catch (e: RemoteException) {
                sLogger.error("DcCreateListener onDataChannelCreated", e)
            }
        }
    }

    inner class ImsDCObserverImpl(
        private val label: String,
        private val dc: IImsDataChannel
    ) : IImsDCObserver.Stub() {

        override fun onDataChannelStateChange(status: ImsDCStatus?, errCode: Int) {
            sLogger.debug("ImsDCObserverImpl onDataChannelStateChange status:$status, label:$label")
            if (status == ImsDCStatus.DC_STATE_CLOSED) {
                StateFlowManager.emitCloseAdcEvent(
                    CloseAdcEvent(0, CloseAdcEvent.CLOSE_ADC, miniAppInfo?.appId, dc)
                )
            }
            onDataChannelStateChange(dc)
        }

        override fun onMessage(data: ByteArray?, length: Int) {
            if (data == null) return
            viewModelScope.launch(serialDispatcher) {
                val map = mapOf(
                    "dcLabel" to label,
                    "message" to android.util.Base64.encodeToString(data, android.util.Base64.NO_WRAP)
                )
                onCallHandler?.invoke(
                    MiniAppConstants.FUNCTION_NOTIFY_MESSAGE,
                    arrayOf(JsonUtil.toJson(map))
                )
            }
        }
    }

    private fun onDataChannelStateChange(dc: IImsDataChannel?) {
        if (dc == null) return
        val state = dc.state
        val exist = openDCList.firstOrNull { DCUtils.compareDCLabel(it.dcLabel, dc.dcLabel) }
        if (ImsDCStatus.DC_STATE_OPEN == state && exist == null) {
            openDCList.add(dc)
        }
        if (ImsDCStatus.DC_STATE_CLOSED == state && exist != null) {
            openDCList.remove(exist)
            createDCLabelList.remove(exist.dcLabel)
        }
        viewModelScope.launch(Dispatchers.Main) {
            val map = mapOf("dcLabel" to dc.dcLabel, "imsDCStatus" to state.ordinal)
            onCallHandler?.invoke(
                MiniAppConstants.FUNCTION_NOTIFY_DATA_CHANNEL,
                arrayOf(JsonUtil.toJson(map))
            )
        }
    }

    fun onCleared() {
        stopPlayVoice()
        ScreenShareHelper.release()
        unregisterDCCallBack()
        unregisterParentToMiniCallback()
        viewModelScope.cancel()
    }
}



// 简单的 Observable 类，用于浮窗场景替代 LiveData
class ObservableValue<T>(initialValue: T) {
    var value: T = initialValue
        set(value) {
            field = value
            observers.forEach { it.invoke(value) }
        }

    private val observers = mutableListOf<(T) -> Unit>()

    fun observe(observer: (T) -> Unit) {
        observers.add(observer)
        observer.invoke(value)
    }

    fun removeObserver(observer: (T) -> Unit) {
        observers.remove(observer)
    }
}