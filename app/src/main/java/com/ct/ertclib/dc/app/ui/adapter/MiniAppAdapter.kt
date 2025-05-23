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

package com.ct.ertclib.dc.app.ui.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.telecom.Call
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.ct.ertclib.dc.app.R
import com.ct.ertclib.dc.app.ui.view.MiniAppViewHolder
import com.ct.ertclib.dc.app.ui.viewmodel.ExpandedFragmentViewModel
import com.ct.ertclib.dc.app.utils.ToastUtils
import com.ct.ertclib.dc.core.common.NewCallAppSdkInterface
import com.ct.ertclib.dc.core.common.NewCallAppSdkInterface.SDK_PERCENT_CONSTANTS
import com.ct.ertclib.dc.core.data.call.CallInfo
import com.ct.ertclib.dc.core.data.common.Reason
import com.ct.ertclib.dc.core.data.miniapp.StableExpandedItem
import com.ct.ertclib.dc.core.data.model.MiniAppInfo
import com.ct.ertclib.dc.core.data.model.SupportScene
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


class MiniAppAdapter(
    val context: Context,
    private val callInfo: CallInfo?,
    private val onStartSuccess: () -> Unit,
    private val scope: CoroutineScope,
    private val viewModel: ExpandedFragmentViewModel
) : RecyclerView.Adapter<MiniAppViewHolder>() {

    companion object {
        private const val TAG = "MiniAppAdapter"
    }

    private var miniAppList = mutableListOf<MiniAppInfo>()
    private var itemViewMap = mutableMapOf<String, com.ct.ertclib.dc.app.ui.view.MiniAppItemView>()
    private var fileShareIconMap = mapOf(
        "01" to R.drawable.icon_fileshare_01,
        "02" to R.drawable.icon_fileshare_02,
        "03" to R.drawable.icon_fileshare_03,
        "04" to R.drawable.icon_fileshare_04,
        "05" to R.drawable.icon_fileshare_05,
        "06" to R.drawable.icon_fileshare_06,
        "07" to R.drawable.icon_fileshare_07,
        "08" to R.drawable.icon_fileshare_08,
        "09" to R.drawable.icon_fileshare_09,
        "10" to R.drawable.icon_fileshare_10,
        "11" to R.drawable.icon_fileshare_11,
        "12" to R.drawable.icon_fileshare_12,
        "13" to R.drawable.icon_fileshare_13,
        "14" to R.drawable.icon_fileshare_14,
        "15" to R.drawable.icon_fileshare_15,
        "16" to R.drawable.icon_fileshare_16,
        "17" to R.drawable.icon_fileshare_17
    )

    private val stableItemList = arrayListOf<StableExpandedItem>()

    @SuppressLint("NotifyDataSetChanged")
    fun submitData(newData: List<MiniAppInfo>?) {
        scope.launch(Dispatchers.IO){
            miniAppList.clear()
            stableItemList.clear()
            itemViewMap.clear()
            newData?.forEach { miniAppInfo ->
                if (miniAppInfo.appId == NewCallAppSdkInterface.YI_SHARE_APP_ID && !miniAppInfo.appIcon.isNullOrEmpty()){
                    // 拆分该小程序的子功能
                    val parseInfo = NewCallAppSdkInterface.parseImgData(miniAppInfo.appIcon!!)
                    if (parseInfo.isNotEmpty()){
                        val list = parseInfo.split("&")
                        if (list.isNotEmpty()){
                            list.forEach {
                                if (it.length > 2){
                                    val name = it.dropLast(2)
                                    val iconIndex = it.takeLast(2)
                                    var resId: Int = fileShareIconMap[iconIndex] ?: -1
                                    if (resId == -1){
                                        resId = R.drawable.icon_fileshare_yi
                                    }
                                    stableItemList.add(StableExpandedItem(name, resId){
                                        NewCallAppSdkInterface.saveShareType(name)
                                        handleStartApp(miniAppInfo)
                                    })
                                }
                            }
                        }
                    }
                }
                miniAppList.add(miniAppInfo)
            }
            scope.launch(Dispatchers.Main){
                this@MiniAppAdapter.notifyDataSetChanged()
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MiniAppViewHolder {
        return MiniAppViewHolder(
            com.ct.ertclib.dc.app.ui.view.MiniAppItemView(
                parent.context
            )
        )
    }

    override fun getItemCount(): Int {
        return miniAppList.size + stableItemList.size
    }

    override fun onBindViewHolder(holder: MiniAppViewHolder, position: Int) {
        if (position < stableItemList.size) {
            bindHeader(holder, position)
        } else {
            bindContent(holder, position - stableItemList.size)
        }

    }

    fun refreshTextColor(color: Int) {
        itemViewMap.forEach { s, miniAppItemView ->
            miniAppItemView.refreshTextColor(color)
        }
    }

    private fun bindHeader(holder: MiniAppViewHolder, position: Int) {
        NewCallAppSdkInterface.printLog(NewCallAppSdkInterface.DEBUG_LEVEL, TAG, "bindHeader position: $position, item: ${stableItemList[position]}")
        if (position < stableItemList.size) {
            stableItemList[position].let { item ->
                itemViewMap[item.name] = holder.itemview
                holder.itemview.bindSimple(item.name, item.drawableRes, NewCallAppSdkInterface.floatingBallStyle.value?.let { viewModel.getTextColor(context, it) })
                holder.itemview.setOnClickListener {
                    item.onCLickListener.invoke()
                }
                NewCallAppSdkInterface.floatingBallStyle.value?.let {
                    holder.itemview.titleTv.setTextColor(viewModel.getTextColor(context, it))
                }
            }
        }
    }

    private fun bindContent(holder: MiniAppViewHolder, position: Int) {
        val miniAppInfo = miniAppList[position]
        NewCallAppSdkInterface.printLog(
            NewCallAppSdkInterface.INFO_LEVEL,
            TAG,
            "bindContent miniAppInfo isVideoCall: ${NewCallAppSdkInterface.isVideoCall(miniAppInfo.callId)},  miniAppInfo: ${miniAppInfo.appName} ${miniAppInfo.supportScene},SupportScene.VIDEO.ordinal: ${SupportScene.VIDEO.value}"
        )
        itemViewMap[miniAppInfo.appId] = holder.itemview
        if (callInfo != null) {
            holder.itemview.bindData(miniAppInfo, callInfo, NewCallAppSdkInterface.floatingBallStyle.value?.let { viewModel.getTextColor(context, it) })
        }
        holder.itemview.setOnClickListener {
            handleStartApp(miniAppInfo)
        }
        NewCallAppSdkInterface.floatingBallStyle.value?.let {
            holder.itemview.titleTv.setTextColor(viewModel.getTextColor(context, it))
        }
    }

    private fun onStartResult(appId: String, isSuccess: Boolean, reason: Reason?) {
        NewCallAppSdkInterface.printLog(NewCallAppSdkInterface.DEBUG_LEVEL, TAG, "startMiniApp $appId isSuccess $isSuccess reason $reason")
        if (isSuccess) {
            itemViewMap[appId]?.updateProgress(SDK_PERCENT_CONSTANTS)
            onStartSuccess.invoke()
        } else {
            itemViewMap[appId]?.updateProgress(0)
            ToastUtils.showShortToast(context, com.ct.ertclib.dc.core.R.string.toast_download_mini_app_failed)
        }
    }

    private fun onDownloadProgressUpdated(appId: String, progress: Int) {
        NewCallAppSdkInterface.printLog(NewCallAppSdkInterface.INFO_LEVEL, TAG, "onDownloadProgressUpdated appId: $appId ,progress: $progress")
        scope.launch(Dispatchers.Main) {
            itemViewMap[appId]?.updateProgress(progress)
        }
    }

    private fun handleStartApp(miniAppInfo: MiniAppInfo){
        val callSate = NewCallAppSdkInterface.getCallState(callInfo?.telecomCallId)
        NewCallAppSdkInterface.printLog(NewCallAppSdkInterface.INFO_LEVEL, TAG, "handleStartApp miniAppInfo: $miniAppInfo ,callSate: $callSate")
        if (callSate == Call.STATE_HOLDING){
            ToastUtils.showShortToast(context, context.resources.getString(com.ct.ertclib.dc.core.R.string.holder_call_state_tips))
        } else if (miniAppInfo.supportScene == SupportScene.VIDEO.value && !NewCallAppSdkInterface.isVideoCall(miniAppInfo.callId)){//配置只能音频但当前非音频
            ToastUtils.showShortToast(context, "${miniAppInfo.appName}仅视频通话时可用")
        } else if (miniAppInfo.supportScene == SupportScene.AUDIO.value && NewCallAppSdkInterface.isVideoCall(miniAppInfo.callId)){//配置只能音频但当前非音频
            ToastUtils.showShortToast(context, "${miniAppInfo.appName}语音通话时可用")
        } else if (!NewCallAppSdkInterface.supportDC(miniAppInfo)) {
            ToastUtils.showShortToast(context, "${miniAppInfo.appName}对端网络不支持")
        } else if (callSate != Call.STATE_ACTIVE && miniAppInfo.isPhaseInCall()) {
            ToastUtils.showShortToast(context, "${miniAppInfo.appName}在接通后可用")
        } else if (callSate == Call.STATE_ACTIVE && miniAppInfo.isPhasePreCall()) {
            ToastUtils.showShortToast(context, "${miniAppInfo.appName}在接通前可用")
        } else {
            NewCallAppSdkInterface.printLog(NewCallAppSdkInterface.DEBUG_LEVEL, TAG, "startMiniApp miniAppInfo.appStatus${miniAppInfo.appStatus}")
            //状态在MiniAppPackageManagerImpl里面控制了，后续考虑加一下进度，收到成功或失败给提示。这里不判断状态
            callInfo?.let { info ->
                NewCallAppSdkInterface.startMiniApp(info.telecomCallId, miniAppInfo.appId, ::onStartResult, ::onDownloadProgressUpdated)
            }
        }
    }
}
