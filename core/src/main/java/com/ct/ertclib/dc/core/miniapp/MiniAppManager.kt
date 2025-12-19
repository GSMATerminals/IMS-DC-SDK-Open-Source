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

package com.ct.ertclib.dc.core.miniapp

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.os.Message
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.telecom.Call
import android.text.TextUtils
import com.blankj.utilcode.util.Utils
import com.blankj.utilcode.util.ZipUtils
import com.ct.ertclib.dc.core.R
import com.ct.ertclib.dc.core.common.NewCallAppSdkInterface
import com.ct.ertclib.dc.core.data.call.CallInfo
import com.ct.ertclib.dc.core.manager.call.NewCallsManager
import com.ct.ertclib.dc.core.port.call.ICallStateListener
import com.ct.ertclib.dc.core.common.PathManager
import com.ct.ertclib.dc.core.constants.CommonConstants
import com.ct.ertclib.dc.core.constants.CommonConstants.ACTION_START_APP_RESPONSE
import com.ct.ertclib.dc.core.data.common.Reason
import com.ct.ertclib.dc.core.data.model.MiniAppInfo
import com.ct.ertclib.dc.core.port.dc.IDcCreateListener
import com.ct.ertclib.dc.core.data.miniapp.DataChannel
import com.ct.ertclib.dc.core.data.miniapp.DataChannelApp
import com.ct.ertclib.dc.core.data.miniapp.DataChannelAppInfo
import com.ct.ertclib.dc.core.manager.call.DCManager
import com.ct.ertclib.dc.core.utils.logger.Logger
import com.ct.ertclib.dc.core.utils.common.FileUtils
import com.ct.ertclib.dc.core.data.model.SupportScene
import com.ct.ertclib.dc.core.port.dc.IControlDcCreateListener
import com.ct.ertclib.dc.core.data.event.MiniAppListGetEvent
import com.ct.ertclib.dc.core.data.event.NotifyEvent
import com.ct.ertclib.dc.core.data.miniapp.MiniAppDownloadResult
import com.ct.ertclib.dc.core.data.miniapp.MiniAppList
import com.ct.ertclib.dc.core.data.miniapp.MiniAppStatus
import com.ct.ertclib.dc.core.common.ConfirmActivity
import com.ct.ertclib.dc.core.miniapp.MiniAppOwnADCImpl.Model
import com.ct.ertclib.dc.core.miniapp.MiniAppOwnADCImpl.OnADCListener
import com.ct.ertclib.dc.core.miniapp.MiniAppOwnADCImpl.OnSendCallback
import com.ct.ertclib.dc.core.miniapp.aidl.IMessageCallback
import com.ct.ertclib.dc.core.port.dc.IAdverseDcCreateListener
import com.ct.ertclib.dc.core.utils.common.XmlUtils
import com.ct.ertclib.dc.core.port.miniapp.IDownloadMiniApp
import com.ct.ertclib.dc.core.port.miniapp.IMiniAppListLoadedCallback
import com.ct.ertclib.dc.core.port.miniapp.IMiniAppStartManager
import com.ct.ertclib.dc.core.port.miniapp.IMiniAppStartCallback
import com.ct.ertclib.dc.core.port.miniapp.IStartAppCallback
import com.ct.ertclib.dc.core.utils.common.JsonUtil
import com.ct.ertclib.dc.core.utils.common.LogUtils
import com.newcalllib.datachannel.V1_0.IImsDataChannel
import com.newcalllib.datachannel.V1_0.ImsDCStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap

class MiniAppManager(private val callInfo: CallInfo) :
    ICallStateListener, IControlDcCreateListener ,IAdverseDcCreateListener{

    companion object {
        private const val TAG = "MiniAppManager"
        private val sLogger: Logger = Logger.getLogger(TAG)
        private const val QOS_HINT_HEAD = "<QosHint>"
        private const val QOS_HINT_TAIL = "</QosHint>"
        private const val ACTIVE_START_TYPE = 1
        private const val PASSIVE_START_TYPE = 2
        private const val CONST_IMS_BDC_CLOSE: Int = 0
        private const val CONST_MINI_APP_LIST_LOADED: Int = 1
        private const val CONST_MINI_APP_DOWNLOADED: Int = 2
        private const val CONST_START_MINI_APP: Int = 3

        val mMiniAppPMMap = ConcurrentHashMap<String, MiniAppManager>()
        @SuppressLint("StaticFieldLeak")
        private var mCallsManager: NewCallsManager? = null


        fun setCallsManager(callsManager: NewCallsManager) {
            mCallsManager = callsManager
        }

        private var mNetworkManager: DCManager? = null

        fun setNetworkManager(networkManager: DCManager) {
            mNetworkManager = networkManager
        }

        fun getAppPackageManager(telecomCallId: String?): MiniAppManager? {
            if (telecomCallId == null){
                return null
            }
            val pm =  mMiniAppPMMap[telecomCallId]
            sLogger.debug("getAppPackageManager-pm:$pm, telecomCallId:${telecomCallId}")
            return pm
        }

        fun hangUp(telecomCallId: String){
            mCallsManager?.hangUp(telecomCallId)
        }
        fun answer(telecomCallId: String){
            mCallsManager?.answer(telecomCallId)
        }
        fun playDtmfTone(telecomCallId: String,digit: Char){
            mCallsManager?.playDtmfTone(telecomCallId,digit)
        }
        fun setSpeakerphone(on: Boolean){
            mCallsManager?.setSpeakerphone(on)
        }
        fun isSpeakerphoneOn(): Boolean{
            return mCallsManager?.isSpeakerphoneOn() == true
        }
        fun setMuted(muted: Boolean){
            mCallsManager?.setMuted(muted)
        }
        fun isMuted(): Boolean{
            return mCallsManager?.isMuted() == true
        }
        fun isVideoCall(telecomCallId: String):Boolean{
            return if (mCallsManager == null) false else mCallsManager!!.isVideoCall(telecomCallId)
        }
        fun supportScene(data: MiniAppInfo):Boolean{
            return !((data.supportScene == SupportScene.VIDEO.value && !isVideoCall(data.callId))//配置只能视频但当前非视频
                    || (data.supportScene == SupportScene.AUDIO.value && isVideoCall(data.callId)))//配置只能音频但当前非音频
        }

        fun supportPhase(data: MiniAppInfo):Boolean{
            return (data.isPhasePreCall() && getAppPackageManager(data.callId)?.callInfo?.isRinging() == true)//配置了接通前可用
                    || (data.isPhaseInCall() && getAppPackageManager(data.callId)?.callInfo?.isInCall() == true)//配置了接通后可用
        }

        fun supportDC(data: MiniAppInfo):Boolean{
            val pm =  mMiniAppPMMap[data.callId]
            var isSupportedByPeerDC = pm?.mMiniAppListInfo?.ifPeerSupport
            if (isSupportedByPeerDC == null){
                isSupportedByPeerDC = false
            }
            return data.ifWorkWithoutPeerDc || isSupportedByPeerDC //小程序不支持单边DC，且对端不支持DC，会返回false
        }
    }

    val historyStartAppList: MutableList<MiniAppInfo> = mutableListOf()

    private var mTag: String? = null
    private var mHandlerThread: HandlerThread? = null
    private var mHandler: MiniAppPMHandler


    private var miniAppStartManager: IMiniAppStartManager? = null

    private var mIsBDCOpen = false

    @Volatile
    private var mMiniAppListInfo: MiniAppList? = null
    // 本次通话中被动拉起的小程序,目前设计成只可能来自remote
    private val mPassivelyMiniAppMap = ConcurrentHashMap<String, MiniAppInfo>()
    private val mRejectPassivelyMiniAppCountMap = ConcurrentHashMap<String, Int>()
    private var mMiniAppListCallback: IMiniAppListLoadedCallback? = null
    private var mDownloadMiniApp: IDownloadMiniApp? = null
    private val mStartAppCallback = ConcurrentHashMap<String, IStartAppCallback>()
    private val mStartAppMap = ConcurrentHashMap<String, MiniAppInfo>()//本次通话中，打开过的应用
    private val mMiniAppConsultControlImplMap = ConcurrentHashMap<String, MiniAppConsultControlImpl>()
    private var didInCallAutoLoad = false
    private var didPreCallAutoLoad = false
    private var mAutoloadInCallMiniApp: MiniAppInfo? = null
    private var mAutoloadPreCallMiniApp: MiniAppInfo? = null
    @Volatile
    private var mCallState: Int = 0
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    private var mMiniAppOwnADCImpl:MiniAppOwnADCImpl? = null

    init {
        val telecomCallId = callInfo.telecomCallId
        mTag = "MiniAppManager[$telecomCallId]"
        mHandlerThread = HandlerThread(mTag)
        mHandlerThread!!.start()
        mHandler = MiniAppPMHandler(mHandlerThread!!.looper)
        mMiniAppPMMap[telecomCallId] = this
        if (sLogger.isDebugActivated) {
            sLogger.debug("$mTag init")
        }

        mMiniAppOwnADCImpl = MiniAppOwnADCImpl(object :MiniAppOwnADCImpl.OnADCParamsOk{
            override fun onCreateADCParams(
                appId: String,
                toTypedArray: Array<String>,
                description: String
            ): Int {
                return createApplicationDataChannelsInternal(appId, toTypedArray, description)
            }
        })
    }

    fun getMiniAppInfo(appId: String?): MiniAppInfo? {
        // todo 如果本端小程序拉起，不一定能找到
        if (mPassivelyMiniAppMap[appId]!=null){
            return mPassivelyMiniAppMap[appId]
        }
        if (mMiniAppListInfo == null) {
            if (sLogger.isDebugActivated) {
                sLogger.debug("getMiniAppInfo-appId:$appId mMiniAppList is null")
            }
            return null
        }
        val applications = mMiniAppListInfo!!.applications
        applications?.forEach {
            if (it.appId == appId) {
                if (sLogger.isDebugActivated) {
                    sLogger.debug("getMiniAppInfo-appId:$appId MiniAppInfo:$it")
                }
                return it
            }
        }
        return null
    }

    fun isPeerSupportDc(): Boolean {
        if (sLogger.isDebugActivated) {
            sLogger.debug("isPeerSupportDc mMiniAppList:$mMiniAppListInfo")
        }
        return mMiniAppListInfo?.ifPeerSupport ?: false
    }

    fun getMiniAppInfoList() : List<MiniAppInfo>? {
        return mMiniAppListInfo?.applications
    }

    fun getMiniAppList() : MiniAppList? {
        return mMiniAppListInfo
    }

    inner class MiniAppPMHandler(looper: Looper) : Handler(looper) {
        override fun handleMessage(msg: Message) {
            sLogger.info("$mTag handleMessage ${msg.what}")
            when (msg.what) {
                CONST_IMS_BDC_CLOSE -> {
                    stopMiniApps()
                    handleDcClosed()
                }

                CONST_MINI_APP_LIST_LOADED -> {
                    handleReceiveMiniAppList(msg.obj as MiniAppList)
                }

                CONST_MINI_APP_DOWNLOADED -> {
                    handleMiniAppDownloaded(
                        msg.obj as MiniAppDownloadResult,
                        msg.data.getByteArray("app")
                    )
                }

                CONST_START_MINI_APP -> {
                    handleStartMiniApp(msg.obj as String)
                }

                else -> sLogger.info("$mTag not handle message ${msg.what}")
            }
        }
    }

    private fun startAutoLoadInCallApp() {
        mStartAppMap.values.forEach {
            if (it.isPhasePreCall()) {
                miniAppStartManager?.stopMiniApp(Utils.getApp(), it.callId,it.appId)
            }
        }
        startAutoloadApp()
    }

    private fun handleDcClosed() {
        mDownloadMiniApp = null
        mMiniAppListCallback = null
        mPassivelyMiniAppMap.clear()
        mRejectPassivelyMiniAppCountMap.clear()
        mStartAppMap.clear()
        mStartAppCallback.clear()
        mMiniAppConsultControlImplMap.clear()
        mMiniAppOwnADCImpl?.release()
        mMiniAppOwnADCImpl = null
        mAutoloadInCallMiniApp = null
        mAutoloadPreCallMiniApp = null
        didInCallAutoLoad = false
        didPreCallAutoLoad = false
    }

    private fun handleStartMiniApp(appId: String) {
        sLogger.info("$mTag handleStartMiniApp appId:$appId")
        if (!mIsBDCOpen) {
            if (sLogger.isDebugActivated) sLogger.debug("$mTag handleStartMiniApp bdc is not open")
            return
        }

        val startedMiniApp = getStartedApp(appId)
        if (startedMiniApp != null) {
            if (sLogger.isDebugActivated) sLogger.debug("$mTag handleStartMiniApp appId:$appId already started")
            //将已经启动的app放到最前面
            startMiniAppInternal(startedMiniApp)
            return
        }

        val miniAppInfo = getMiniAppInfo(appId)
        if (miniAppInfo == null) {
            if (sLogger.isDebugActivated) sLogger.debug("$mTag handleStartMiniApp not found app")
            handleStartMiniAppFailed(appId, Reason.UNKNOWN)
            return
        }
        if (!(supportScene(miniAppInfo) && supportDC(miniAppInfo) && supportPhase(miniAppInfo))) {
            if (sLogger.isDebugActivated) sLogger.debug("$mTag handleStartMiniApp not support")
            handleStartMiniAppFailed(appId, Reason.UNKNOWN)
            return
        }

        if (MiniAppStatus.STARTING == miniAppInfo.appStatus) {
            if (sLogger.isDebugActivated) sLogger.debug("$mTag handleStartMiniApp appId: $appId is starting")
            return
        } else if (MiniAppStatus.DOWNLOADING == miniAppInfo.appStatus) {
            if (sLogger.isDebugActivated) sLogger.debug("$mTag handleStartMiniApp appId: $appId is isDownloading")
            return
        } else {
            val eTag = miniAppInfo.eTag
            if (eTag.isEmpty()) {
                if (sLogger.isDebugActivated) sLogger.debug("$mTag handleStartMiniApp appId: $appId no version")
                handleStartMiniAppFailed(appId, Reason.UNKNOWN)
                return
            } else {
                val currentAppStatus = if (isInstalledApp(miniAppInfo)) {
                    MiniAppStatus.INSTALLED
                } else {
                    MiniAppStatus.UNINSTALLED
                }

                if (currentAppStatus == MiniAppStatus.UNINSTALLED || miniAppInfo.path.isNullOrEmpty()) {
                    miniAppInfo.appStatus = MiniAppStatus.DOWNLOADING
                    miniAppInfo.isStartAfterInstalled = true
                    onMiniAppDownloadProgressUpdated(appId, 0)
                    mDownloadMiniApp?.downloadMiniApp(miniAppInfo)
                        ?: sLogger.info("$mTag download appId:$appId download manager is null")
                    return
                }
                miniAppInfo.appStatus = MiniAppStatus.STARTING
                startMiniAppInternal(miniAppInfo)
            }
        }
    }

    private fun isInstalledApp(miniAppInfo: MiniAppInfo): Boolean {
        val path = miniAppInfo.path
        if (path.isNullOrEmpty()) {
            val miniAppPath = FileUtils.getMiniAppPath(
                Utils.getApp(),
                miniAppInfo.appId,
                miniAppInfo.appProperties?.version ?: ""
            )
            if (!TextUtils.isEmpty(miniAppPath) && FileUtils.isFileExists(miniAppPath)) {
                miniAppInfo.path = miniAppPath
                return true
            }
            return false
        }
        return path.endsWith(miniAppInfo.eTag)
    }

    private fun getStartedApp(appId: String): MiniAppInfo? {
        return mStartAppMap[appId]
    }

    private fun handleReceiveMiniAppList(miniAppList: MiniAppList) {
        if (sLogger.isDebugActivated) {
            sLogger.debug("$mTag handleReceiveMiniAppList miniAppList:$miniAppList")
        }
        if (!mIsBDCOpen) {
            sLogger.debug("$mTag handleReceiveMiniAppList bdc not open")
            return
        }

        val applications = miniAppList.applications
        if (applications == null) {
            sLogger.debug("$mTag handleReceiveMiniAppList applications is null")
            return
        }
        if (mCallsManager == null) {
            sLogger.debug("$mTag handleReceiveMiniAppList callsManager is null")
            return
        }
        val iterator = applications.iterator()
        while (iterator.hasNext()){
            val miniAppInfo = iterator.next()
            if (miniAppInfo.appId == CommonConstants.DC_APPID_OWN){
                iterator.remove()
            }
        }

        val isFirstPage = miniAppList.beginIndex == 0
        val callId = miniAppList.callId
        val callInfo = callId?.let { mCallsManager?.getCallInfo(it) }
        if (callInfo == null) {
            sLogger.debug("$mTag handleReceiveMiniAppList call info is null")
            return
        }
        // 处理自动拉起小程序
        mAutoloadInCallMiniApp = null
        mAutoloadPreCallMiniApp = null
        applications.forEach { miniAppInfo ->
            miniAppInfo.slotId = callInfo.slotId
            miniAppInfo.callId = callId
            miniAppInfo.remoteNumber = callInfo.remoteNumber
            miniAppInfo.myNumber = callInfo.myNumber
            miniAppInfo.isOutgoingCall = callInfo.isOutgoingCall
            miniAppInfo.path = getInstalledPath(miniAppInfo.appId)
            if (miniAppInfo.autoLoad && isFirstPage){
                if (miniAppInfo.isPhasePreCall() && mAutoloadPreCallMiniApp == null ) {
                    mAutoloadPreCallMiniApp = miniAppInfo
                } else if (miniAppInfo.isPhaseInCall() && mAutoloadInCallMiniApp == null) {
                    mAutoloadInCallMiniApp = miniAppInfo
                }
            }
        }

        if (isFirstPage){
            mMiniAppListInfo = miniAppList
            if (mMiniAppListCallback == null) {
                sLogger.debug("$mTag, handleReceiveMiniAppList mMiniAppListCallback is null")
                return
            }
            mMiniAppListCallback!!.onMiniAppListLoaded()
            startAutoloadApp()
        } else {
            mMiniAppListInfo?.applications?.addAll(applications)
            mMiniAppListInfo?.beginIndex = miniAppList.beginIndex
        }
        NewCallAppSdkInterface.emitAppListEvent(MiniAppListGetEvent(0,MiniAppListGetEvent.ON_DOWNLOAD, mMiniAppListInfo))
    }

    // 区分接通前和接通后
    fun startAutoloadApp(){
        var miniAppInfo : MiniAppInfo? = null
        if (callInfo.isRinging() && !didPreCallAutoLoad && mAutoloadPreCallMiniApp != null && supportScene(mAutoloadPreCallMiniApp!!) && supportDC(mAutoloadPreCallMiniApp!!)){
            didPreCallAutoLoad = true
            miniAppInfo = mAutoloadPreCallMiniApp
        } else if (callInfo.isInCall() && !didInCallAutoLoad && mAutoloadInCallMiniApp != null && supportScene(mAutoloadInCallMiniApp!!) && supportDC(mAutoloadInCallMiniApp!!)){
            didInCallAutoLoad = true
            miniAppInfo = mAutoloadInCallMiniApp
        }
        miniAppInfo?.let {
            startMiniApp(it.appId, null)
        }
    }

    fun startMiniApp(appId: String, startCallback: IStartAppCallback?, startType: Int = ACTIVE_START_TYPE,isStartByOthers:Boolean = false,startByOthersParams:String? = null) {
        if (sLogger.isDebugActivated) {
            sLogger.debug("startMiniApp appId: $appId, startType: $startType")
        }
        if (startCallback != null) {
            mStartAppCallback[appId] = startCallback
        }
        getMiniAppInfo(appId)?.isActiveStart = (startType == ACTIVE_START_TYPE)
        getMiniAppInfo(appId)?.isStartByOthers = isStartByOthers
        getMiniAppInfo(appId)?.startByOthersParams = startByOthersParams
        val startAppMsg = mHandler.obtainMessage(CONST_START_MINI_APP)
        startAppMsg.obj = appId
        startAppMsg.sendToTarget()
    }

    fun queryMiniAPpStatus(appId: String, startCallback: IStartAppCallback) {
        LogUtils.debug(TAG, "queryMiniAPpStatus appId: $appId")
        mStartAppCallback[appId]?.let {
            val progress = it.progress
            mStartAppCallback[appId] = startCallback
            startCallback.onDownloadProgressUpdated(appId, progress)
        }
    }

    // 获取当前缓存的最新版本
    private fun getInstalledPath(appId: String): String? {
        val appPath = "${Utils.getApp().getDir("miniApps", Context.MODE_PRIVATE)}${File.separator}$appId"
        val pathFiles = FileUtils.getPathFiles(appPath)
        if (pathFiles.isNullOrEmpty()) {
            sLogger.debug("$mTag getInstalledPath is null, appPath:$appPath")
            return null
        }
        var path = pathFiles[0].path
        pathFiles.forEach{
            path = PathManager().getMaxPath(path,it.path)
        }
        return path
    }

    private fun handleMiniAppDownloaded(
        miniAppDownloadResult: MiniAppDownloadResult,
        byteArray: ByteArray?
    ) {
        if (!mIsBDCOpen) {
            sLogger.debug("$mTag handleMiniAppDownloaded - do not handle due bdc is closed")
            return
        }

        val appId = miniAppDownloadResult.appId
        if (!miniAppDownloadResult.isSuccessful) {
            if (sLogger.isDebugActivated) {
                sLogger.debug("$mTag handleMiniAppDownloaded $appId download failed")
            }
            handleStartMiniAppFailed(appId, Reason.DOWNLOAD_FAILED)
            return
        }

        val miniAppInfo = getMiniAppInfo(appId)
        if (miniAppInfo == null) {
            sLogger.info("$mTag handleMiniAppDownloaded- not found app")
        } else if (!installMiniApp(miniAppInfo, miniAppDownloadResult.appVersion!!, byteArray!!)) {
            sLogger.info("$mTag handleMiniAppDownloaded- install failed")
            handleStartMiniAppFailed(appId, Reason.INSTALL_FAILED)
        } else {
            sLogger.info("$mTag handleMiniAppDownloaded- startMiniAppInternal isStartAfterInstalled:${miniAppInfo.isStartAfterInstalled}")
            if (miniAppInfo.isStartAfterInstalled) {
                startMiniAppInternal(miniAppInfo)
            }
        }
    }

    private fun installMiniApp(
        miniAppInfo: MiniAppInfo,
        appVersion: String,
        data: ByteArray
    ): Boolean {
        val appId = miniAppInfo.appId

        val filePathBuilder = StringBuilder()
        filePathBuilder
            .append(Utils.getApp().getDir("miniApps", Context.MODE_PRIVATE))
            .append(File.separator)
            .append(appId)
            .append(File.separator)
            .append(appVersion)
        val filePath = filePathBuilder.toString()
        if (!FileUtils.isFileExists(filePath)) {
            //不存在存储小程序
            try {
                //将数据写到沙盒cache里面
                val cacheFile = PathManager().createCacheFile(Utils.getApp(),fileName = "tmp_miniapp.zip")
                val fileOutputStream = FileOutputStream(cacheFile)
                fileOutputStream.write(data)
                fileOutputStream.close()
                //解压小程序
                ZipUtils.unzipFile(cacheFile!!.absolutePath, filePath)
                //删除cache
                FileUtils.deletePath(cacheFile.absolutePath)
                val path = miniAppInfo.path
                if (filePath != path) {
                    miniAppInfo.path = filePath
                    path?.let {
                        FileUtils.deletePath(it)
                    }
                }
                if (sLogger.isDebugActivated) {
                    sLogger.debug("$mTag install appId:$appId, filePath:$filePath,oldPath:$path")
                }
            } catch (e: IOException) {
                if (sLogger.isDebugActivated) {
                    sLogger.error("$mTag install appId:$appId", e)
                }
                return false
            }
        }
        miniAppInfo.path = filePath
        miniAppInfo.appStatus = MiniAppStatus.INSTALLED
        miniAppInfo.eTag = appVersion
        return true
    }

    private fun handleStartMiniAppFailed(appId: String?, reason: Reason) {
        val miniAppInfo = getMiniAppInfo(appId)
        if (miniAppInfo == null) {
            sLogger.debug("$mTag handleStartMiniAppFailed app not found")
            return
        }
        miniAppInfo.appStatus = MiniAppStatus.STOPPED
        mStartAppCallback[appId] ?: return
        sLogger.debug("$mTag handleStartMiniAppFailed notify app start result")
        scope.launch(Dispatchers.Main){
            mStartAppCallback[appId]?.onStartResult(appId!!, false, reason)
            mStartAppCallback.remove(appId)
        }
    }

    private fun handleStartMiniAppSuccess(telecomCallId: String,appId: String?) {
        val miniAppInfo = getMiniAppInfo(appId)
        if (miniAppInfo == null) {
            sLogger.debug("$mTag handleStartMiniAppSuccess app not found")
            return
        }
        miniAppInfo.appStatus = MiniAppStatus.STARTED

        appId?.let {
            mStartAppMap[it] = miniAppInfo
            historyStartAppList.removeIf { miniAppInfo -> miniAppInfo.appId == it }
            historyStartAppList.add(0, miniAppInfo)
        }

        mStartAppCallback[appId] ?: return

        sLogger.debug("$mTag handleStartMiniAppSuccess notify app start result,miniAppInfo.appProperties: ${miniAppInfo.appProperties}")
        mStartAppCallback[appId]!!.onStartResult(appId!!, true, null)
        mStartAppCallback.remove(appId)
        // 业务发起方发起建立SDK协商机制
        if (!mPassivelyMiniAppMap.containsKey(appId) && miniAppInfo.appProperties?.shouldCreateControlADC == true && mMiniAppListInfo?.ifPeerSupport == true){//小程序不是被动打开的，是业务发起方
            mMiniAppConsultControlImplMap[appId] = MiniAppConsultControlImpl(appId,object : MiniAppConsultControlImpl.OnControlListener{
                override fun onCreateADCParams(
                    appId: String,
                    toTypedArray: Array<String>,
                    description: String
                ): Int {
                    return createApplicationDataChannelsInternal(appId, toTypedArray, description)
                }

                override fun onControlDCStateChange(status: ImsDCStatus?, errCode: Int) {
                    // 根据小程序的配置
                    sLogger.debug("$mTag miniAppInfo.appProperties: ${miniAppInfo.appProperties}")
                    if (status == ImsDCStatus.DC_STATE_OPEN && miniAppInfo.appProperties?.shouldStartRemoteApp == true){
                        // 延时1s
                        scope.launch {
                            delay(1000)
                            requestStartAdverseApp(appId)
                        }
                    }
                }

                override fun onRequestStartApp(appInfo:MiniAppInfo) {

                }

                override fun onResponseStartApp(option:String) {
                    val event = NotifyEvent(
                        ACTION_START_APP_RESPONSE,
                        mutableMapOf("option" to option)
                    )
                    MiniAppStartManager.sendMessageToMiniApp(telecomCallId,appId,JsonUtil.toJson(event),object : IMessageCallback.Stub() {
                        override fun reply(message: String?) {
                            if (sLogger.isDebugActivated) {
                                sLogger.debug("onResponseStartApp sendMessageToMiniApp:$message")
                            }
                        }
                    })
                }

            })
            mMiniAppConsultControlImplMap[appId]?.createDC(miniAppInfo)
        }
    }

    private fun startMiniAppInternal(miniAppInfo: MiniAppInfo) {
        sLogger.debug("$mTag startMiniAppInternal miniAppManager:$miniAppStartManager")

        miniAppStartManager?.startMiniApp(Utils.getApp(), miniAppInfo, callInfo, mMiniAppListInfo, object : IMiniAppStartCallback{
            override fun onMiniAppStarted() {
                handleStartMiniAppSuccess(miniAppInfo.callId,miniAppInfo.appId)
            }
            override fun onMiniAppStartFailed(reason: Reason) {
                handleStartMiniAppFailed(miniAppInfo.appId, reason)
            }
        })
    }

    private fun stopMiniApps() {
        mMiniAppListInfo = null
        mStartAppMap.forEach { (_, value) ->
            miniAppStartManager?.stopMiniApp(Utils.getApp(), value.callId,value.appId)
        }
    }

    fun setMiniAppStartManager(miniAppStartManager: IMiniAppStartManager) {
        this.miniAppStartManager = miniAppStartManager
    }

    fun registerMiniAppListLoadedListener(miniAppListLoadedCallback: IMiniAppListLoadedCallback) {
        if (sLogger.isDebugActivated) {
            sLogger.debug("registerMiniAppListLoadedListener")
        }
        if (mMiniAppListCallback != null) {
            sLogger.debug("registerMiniAppListLoadedListener failed due to already registered")
        } else {
            this.mMiniAppListCallback = miniAppListLoadedCallback
        }
    }

    fun setDownloadAppListener(downloadAppListener: IDownloadMiniApp) {
        mDownloadMiniApp = downloadAppListener
    }

    fun onImsBDCOpen() {
        mIsBDCOpen = true
    }

    fun onImsBDCClose() {
        mIsBDCOpen = false
        mHandler.sendMessageAtFrontOfQueue(mHandler.obtainMessage(CONST_IMS_BDC_CLOSE))
    }

    fun onMiniAppDownloaded(miniAppDownloadResult: MiniAppDownloadResult, data: ByteArray?) {
        if (sLogger.isDebugActivated) {
            sLogger.debug("$mTag onMiniAppDownloaded -miniAppDownloadResult$miniAppDownloadResult")
        }

        val downloadMessage = mHandler.obtainMessage(CONST_MINI_APP_DOWNLOADED)
        downloadMessage.obj = miniAppDownloadResult
        val bundle = Bundle()
        bundle.putByteArray("app", data)
        downloadMessage.data = bundle
        downloadMessage.sendToTarget()
    }

    fun onMiniAppListLoaded(miniAppList: MiniAppList) {
        if (sLogger.isDebugActivated) {
            sLogger.debug("$mTag onMiniAppListLoaded:$miniAppList")
        }
        val miniAppListMessage = mHandler.obtainMessage(CONST_MINI_APP_LIST_LOADED)
        miniAppListMessage.obj = miniAppList
        miniAppListMessage.sendToTarget()
    }

    fun onMiniAppDownloadProgressUpdated(appId: String, progress: Int) {
        mStartAppCallback[appId]?.let {
            it.onDownloadProgressUpdated(appId, progress)
            it.progress = progress
        }
    }

    fun unregisterMiniAppListLoadedCallback() {
        if (sLogger.isDebugActivated) {
            sLogger.debug("$mTag unregisterMiniAppListLoadedCallback")
        }
        mMiniAppListCallback = null
    }

    fun getCacheAppVersion(miniApp: MiniAppInfo): String {
        val path = miniApp.path
        if (path.isNullOrEmpty()) {
            sLogger.debug("$mTag getCacheAppVersion no cache appId:$path")
            return ""
        }
        return FileUtils.getLastPathName(path)
    }

    override fun onCallAdded(context: Context, callInfo: CallInfo) {
        if (sLogger.isDebugActivated) sLogger.debug("$mTag onCallAdded")
        val telecomCallId = callInfo.telecomCallId
        mTag = "MiniAppManager[$telecomCallId]"
        mMiniAppPMMap[telecomCallId] = this
        onCallStateChanged(callInfo, callInfo.state)
        mNetworkManager?.registerControlAppDataChannelCallback(telecomCallId,this)
        mNetworkManager?.registerAdverseAppDataChannelCallback(telecomCallId,this)
    }

    override fun onCallRemoved(context: Context, callInfo: CallInfo) {
        if (sLogger.isDebugActivated) sLogger.debug("$mTag onCallRemoved")
        mCallState = callInfo.state
        miniAppStartManager?.clearBackgroundTaskList()
        miniAppStartManager = null
        mMiniAppPMMap.remove(callInfo.telecomCallId)
        mNetworkManager?.unregisterAdverseAppDataChannelCallback(callInfo.telecomCallId)
        mNetworkManager?.unregisterControlAppDataChannelCallback(callInfo.telecomCallId)
        mMiniAppConsultControlImplMap.clear()
    }

    override fun onCallStateChanged(callInfo: CallInfo, state: Int) {
        if (sLogger.isDebugActivated) sLogger.debug("$mTag onCallStateChanged callState:$state,callInfo$callInfo, mCallState:$mCallState")

        if (mCallState == state) {
            sLogger.info("onCallStateChanged same state not do again...")
            return
        }
        if (state == Call.STATE_DISCONNECTING || state == Call.STATE_DISCONNECTED) {
            stopMiniApps()
        } else if (state == Call.STATE_HOLDING && mCallState == Call.STATE_ACTIVE){
            //在呼叫保持状态下，不做任何处理
        } else {
            startAutoLoadInCallApp()
        }
        mCallState = state
    }

    override fun onAudioDeviceChange() {
        sLogger.info("onAudioDeviceChange")
    }

    fun createApplicationDataChannelsInternal(
        appId: String,
        toTypedArray: Array<String>,
        description: String
    ): Int {

        if (sLogger.isDebugActivated) {
            sLogger.debug("$mTag createApplicationDataChannelsInternal appId:$appId, labels:$toTypedArray, description:$description")
        }
        if (mNetworkManager == null) {
            if (sLogger.isDebugActivated) {
                sLogger.debug("$mTag createApplicationDataChannelsInternal networkManager is null")
            }
            return 1
        }

        var modifiedDescription = description
        if (!isSupportQosHit(description)) {
            val appQosHint = if (mStartAppMap[appId]?.qosHint?.contains(",") == true) {
                mStartAppMap[appId]?.qosHint?.split(",")?.first()
            } else {
                mStartAppMap[appId]?.qosHint
            }
            appQosHint?.let {
                val beginIndex = description.indexOf(QOS_HINT_HEAD)
                val lastIndex = description.indexOf(QOS_HINT_TAIL) + QOS_HINT_TAIL.length
                if (lastIndex < description.length) {
                    modifiedDescription = description.replaceRange(beginIndex, lastIndex, "$QOS_HINT_HEAD$it$QOS_HINT_TAIL")
                }
            }
            sLogger.debug("createApplicationDataChannelsInternal replace qosHint")
        }
        if (!isUseCaseCorrect(description)) {
            if (sLogger.isDebugActivated) {
                sLogger.debug("$mTag createApplicationDataChannelsInternal not allow create adc isUseCaseCorrect is false")
            }
            return 3
        }
        return mNetworkManager?.createApplicationDataChannels(
            callInfo.slotId,
            callInfo.telecomCallId,
            callInfo.remoteNumber,
            toTypedArray,
            modifiedDescription
        ) ?: 1
    }

    private fun isSupportQosHit(description: String): Boolean {

        try {
            val classes = arrayOf<Class<*>>(
                DataChannelAppInfo::class.java,
                DataChannelApp::class.java,
                DataChannel::class.java
            )
            val dataChannelAppInfo =
                XmlUtils.parseXml(description, classes, DataChannelAppInfo::class.java)
            if (sLogger.isDebugActivated) {
                sLogger.debug("$mTag isSupportQosHit description:$description, DataChannelAppInfo:$dataChannelAppInfo")
            }
            if (dataChannelAppInfo != null) {
                val dataChannelApp = dataChannelAppInfo.dataChannelApp ?: return false
                val dataChannelList = dataChannelApp.dataChannelList ?: return false
                if (dataChannelList.isEmpty()) return false
                dataChannelList.forEach { dataChannel ->
                    val qosHint = dataChannel.qosHint
                    if (!TextUtils.isEmpty(qosHint)) {
                        mStartAppMap[dataChannelApp.appId]?.let { appInfo ->
                            if (!appInfo.qosHint.contains(qosHint)){
                                return false
                            }
                        }
                    } else {
                        return false
                    }
                }

                sLogger.info("isSupportQosHit is true")
                return true
            }
        } catch (e: Exception) {
            if (sLogger.isDebugActivated) {
                sLogger.error("$mTag isSupportQosHit", e)
            }
        }
        return true
    }

    private fun isUseCaseCorrect(description: String):Boolean{
        try {
            val classes = arrayOf<Class<*>>(
                DataChannelAppInfo::class.java,
                DataChannelApp::class.java,
                DataChannel::class.java
            )
            val dataChannelAppInfo =
                XmlUtils.parseXml(description, classes, DataChannelAppInfo::class.java)
            if (sLogger.isDebugActivated) {
                sLogger.debug("$mTag isUseCaseCorrect description:$description, DataChannelAppInfo:$dataChannelAppInfo")
            }
            if (dataChannelAppInfo != null) {
                val dataChannelApp = dataChannelAppInfo.dataChannelApp ?: return false
                val dataChannelList = dataChannelApp.dataChannelList ?: return false
                if (dataChannelList.isEmpty()) return false
                dataChannelList.forEach { dataChannel ->
                    val useCase = dataChannel.useCase
                    val dcLabel = dataChannel.dcLabel
                    if (TextUtils.isEmpty(useCase)){
                        sLogger.info("dcLabel $dcLabel useCase is null")
                        return false
                    }
                    val labelList = dcLabel.split("_")
                    if (labelList.size != 4 || labelList[2] != useCase){
                        sLogger.info("dcLabel $dcLabel useCase is wrong")
                        return false
                    }
                }

                sLogger.info("isUseCaseCorrect is true")
                return true
            }
        } catch (e: Exception) {
            if (sLogger.isDebugActivated) {
                sLogger.error("$mTag isUseCaseCorrect", e)
            }
        }
        return true
    }

    fun requestStartAdverseApp(appId: String){
        // 只允许业务发起方调用
        if (!mPassivelyMiniAppMap.containsKey(appId)){
            mStartAppMap[appId]?.let { mMiniAppConsultControlImplMap[appId]?.requestStartAdverseApp(it) }
        }
    }

    fun registerAppDataChannelCallbackInternal(appId: String, createListener: IDcCreateListener) {
        if (sLogger.isDebugActivated) {
            sLogger.debug("$mTag registerAppDataChannelCallbackInternal appId$appId, createListener:$createListener")
        }
        if (mNetworkManager == null) {
            if (sLogger.isDebugActivated) {
                sLogger.debug("$mTag registerAppDataChannelCallbackInternal networkManager is null")
            }
            return
        }
        mNetworkManager?.registerAppDataChannelCallback(callInfo.telecomCallId, appId, createListener)
    }


    fun registerCallStateChangeCallbackInternal(
        appId: String,
        callStateListener: ICallStateListener
    ) {
        if (sLogger.isDebugActivated) {
            sLogger.debug("$mTag registerCallStateChangeCallbackInternal appId$appId, createListener:$callStateListener")
        }
        if (mCallsManager == null) {
            sLogger.debug("$mTag registerCallStateChangeCallbackInternal callsManager is null")
            return
        }
        val startedApp = getStartedApp(appId)
        if (startedApp == null) {
            if (sLogger.isDebugActivated) {
                sLogger.debug("$mTag registerCallStateChangeCallbackInternal start app is null")
            }
            return
        }
        mCallsManager?.addCallStateListener(startedApp.callId, callStateListener)
    }

    fun unregisterAppDataChannelCallbackInternal(appId: String) {
        if (sLogger.isDebugActivated) {
            sLogger.debug("$mTag unregisterAppDataChannelCallbackInternal appId$appId")
        }
        if (mNetworkManager == null) {
            if (sLogger.isDebugActivated) {
                sLogger.debug("$mTag registerAppDataChannelCallbackInternal networkManager is null")
            }
            return
        }
        val startedApp = getStartedApp(appId)
        if (startedApp == null) {
            if (sLogger.isDebugActivated) {
                sLogger.debug("$mTag registerAppDataChannelCallbackInternal start app is null")
            }
            return
        }
        mNetworkManager?.unregisterAppDataChannelCallback(startedApp.callId, appId)
        startedApp.appStatus = MiniAppStatus.STOPPED
        mMiniAppConsultControlImplMap[appId]?.release()
        mMiniAppConsultControlImplMap.remove(appId)
    }

    fun unregisterCallStateListenerInternal(
        appId: String,
        iCallStateListener: ICallStateListener
    ) {
        if (sLogger.isDebugActivated) {
            sLogger.debug("$mTag unregisterCallStateListenerInternal appId$appId, iCallStateListener:$iCallStateListener")
        }
        if (mCallsManager == null) {
            if (sLogger.isDebugActivated) {
                sLogger.debug("$mTag unregisterCallStateListenerInternal mCallsManager is null")
            }
            return
        }
        val miniAppInfo = getMiniAppInfo(appId)
        if (miniAppInfo == null) {
            if (sLogger.isDebugActivated) {
                sLogger.debug("$mTag unregisterCallStateListenerInternal start app is null")
            }
            return
        }
        mCallsManager?.removeCallStateListener(miniAppInfo.callId, iCallStateListener)
    }

    override fun onControlDataChannelCreated(
        telecomCallId: String,
        appId: String,
        streamId: String,
        imsDataChannel: IImsDataChannel
    ) {
        //业务接收方协商控制adc,因为发起方不为null
        if (mMiniAppConsultControlImplMap[appId] == null){
            mMiniAppConsultControlImplMap[appId] = MiniAppConsultControlImpl(appId,object : MiniAppConsultControlImpl.OnControlListener{
                override fun onCreateADCParams(
                    appId: String,
                    toTypedArray: Array<String>,
                    description: String
                ): Int {
                    return 0
                }

                override fun onControlDCStateChange(status: ImsDCStatus?, errCode: Int) {
                    sLogger.info("onControlDataChannelCreated new MiniAppConsultControlImpl")
                }

                override fun onRequestStartApp(appInfo: MiniAppInfo) {
                    startMiniAppByAdverse(telecomCallId,appInfo,imsDataChannel.dcLabel.startsWith("remote"))
                }

                override fun onResponseStartApp(option:String) {

                }

            })
            sLogger.info("onControlDataChannelCreated new MiniAppConsultControlImpl")
        }
        // 发起方和接收方都要处理
        mMiniAppConsultControlImplMap[appId]?.onDCCreated(imsDataChannel)
    }

    private fun startMiniAppByAdverse(telecomCallId: String, appInfo: MiniAppInfo, isFromBDC100: Boolean){
        var rejectCount = mRejectPassivelyMiniAppCountMap[appInfo.appId]
        // 最多拒绝3次，之后就不在提示
        if (rejectCount != null && rejectCount >= 3) {
            sLogger.info("onRemoteDataChannelCreated reject start miniApp")
            return
        }
        var miniApp = mPassivelyMiniAppMap[appInfo.appId]
        sLogger.info("onRemoteDataChannelCreated miniApp:${miniApp}")
        if (miniApp == null || miniApp.appStatus == MiniAppStatus.UNINSTALLED || miniApp.appStatus == MiniAppStatus.STOPPED){
            // 振动
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val vibratorManager =
                        Utils.getApp().getSystemService(VibratorManager::class.java)
                    vibratorManager.defaultVibrator
                } else {
                    Utils.getApp().getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                }
                val pattern = longArrayOf(500, 300)
                val effect = VibrationEffect.createWaveform(pattern, -1)
                vibrator.vibrate(effect)
            }
            if (miniApp == null){
                miniApp = MiniAppInfo(appInfo.appId,
                    appInfo.appName+"",
                    appInfo.appIcon,
                    appInfo.autoLaunch,
                    appInfo.autoLoad,
                    telecomCallId,
                    appInfo.eTag,
                    appInfo.ifWorkWithoutPeerDc,
                    !appInfo.isOutgoingCall,
                    callInfo.myNumber,
                    null,
                    appInfo.phase,
                    appInfo.qosHint,
                    callInfo.remoteNumber,
                    callInfo.slotId,
                    appInfo.supportScene,
                    MiniAppStatus.UNINSTALLED,
                    true,
                    null,
                    0,
                    isFromBDC100,
                    false,
                    false,
                    null)
            }
            ConfirmActivity.startConfirm(
                Utils.getApp(), Utils.getApp().resources.getString(R.string.start_miniapp_tips, miniApp.appName),
                object : ConfirmActivity.ConfirmCallback {
                    override fun onAccept() {
                        mPassivelyMiniAppMap[appInfo.appId] = miniApp
                        sLogger.debug("onAccept miniapp path: $miniApp")
                        startMiniApp(appInfo.appId, object : IStartAppCallback() {
                            override fun onStartResult(appId: String, isSuccess: Boolean, reason: Reason?) {
                                if (sLogger.isDebugActivated) {
                                    sLogger.debug("$mTag startMiniAppByAdverse $appId isSuccess $isSuccess reason $reason")
                                }
                                mMiniAppConsultControlImplMap[appId]?.responseStartAppResult(MiniAppConsultControlImpl.START_APP_OPTION_AGREE)
                            }

                            override fun onDownloadProgressUpdated(appId: String, progress: Int) {

                            }
                        }, startType = PASSIVE_START_TYPE)
                    }

                    override fun onCancel() {
                        if (rejectCount == null){
                            mRejectPassivelyMiniAppCountMap[appInfo.appId] = 0
                        } else {
                            mRejectPassivelyMiniAppCountMap[appInfo.appId] = rejectCount++
                        }
                        mMiniAppConsultControlImplMap[appInfo.appId]?.responseStartAppResult(MiniAppConsultControlImpl.START_APP_OPTION_REJECT)
                    }
                },
                Utils.getApp().resources.getString(R.string.btn_agree),
                Utils.getApp().resources.getString(R.string.btn_refuse)
            )
        }
    }

    override fun onAdverseDataChannelCreated(
        telecomCallId: String,
        appId: String,
        streamId: String,
        imsDataChannel: IImsDataChannel
    ) {
        // TODO: 按照《中国电信5G增强实时通信终端与平台接口技术要求》下载失败时要拆ADC
        // a2p通过这种方式触发小程序的启动
        if (imsDataChannel.dcLabel.contains("_2_")){
            // 尝试从小程序列表中获取小程序信息，如果没有就new一个
            var miniAppInfo : MiniAppInfo? = null
            val list = getMiniAppInfoList()
            if (list != null) {
                for (app in list){
                    if (app.appId == appId){
                        miniAppInfo = app
                        break
                    }
                }
            }
            if (miniAppInfo == null){
                miniAppInfo = MiniAppInfo(appId,
                    "",
                    null,
                    false,
                    false,
                    telecomCallId,
                    "99.99.99",
                    false,
                    false,
                    callInfo.myNumber,
                    null,
                    "",
                    "",
                    callInfo.remoteNumber,
                    callInfo.slotId,
                    2,
                    MiniAppStatus.UNINSTALLED,
                    true,
                    null,
                    0,
                    imsDataChannel.dcLabel.startsWith("remote_"),
                    false,
                    false,
                    null)
            }
            startMiniAppByAdverse(telecomCallId, miniAppInfo,imsDataChannel.dcLabel.startsWith("remote_"))
        }

    }

    override fun onOwnDataChannelCreated(
        telecomCallId: String,
        appId: String,
        streamId: String,
        imsDataChannel: IImsDataChannel
    ) {
        mMiniAppOwnADCImpl?.onDCCreated(imsDataChannel)
    }

    fun callState():Int{
        return mCallState
    }

    fun sendOwnData(model: Model, originData:ByteArray, onSendCallback: OnSendCallback){
        mMiniAppOwnADCImpl?.sendData(model, originData, onSendCallback)
    }

    fun registerOwnListener(model: Model, listener: OnADCListener){
        mMiniAppOwnADCImpl?.registerListener(model, listener)
    }

    fun unRegisterOwnListener(model: Model){
        mMiniAppOwnADCImpl?.unRegisterListener(model)
    }
}