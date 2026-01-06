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

package com.ct.ertclib.dc.app.ui.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.telecom.Call
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.ct.ertclib.dc.core.common.NewCallAppSdkInterface
import com.ct.ertclib.dc.app.ui.view.MiniAppItemView
import com.ct.ertclib.dc.app.ui.view.MiniAppViewHolder
import com.ct.ertclib.dc.app.ui.viewmodel.ExpandedFragmentViewModel
import com.ct.ertclib.dc.app.utils.ToastUtils
import com.ct.ertclib.dc.core.data.call.CallInfo
import com.ct.ertclib.dc.core.data.common.Reason
import com.ct.ertclib.dc.core.data.model.MiniAppInfo
import com.ct.ertclib.dc.core.data.model.SupportScene

class MiniAppHistoryAdapter(
    val context: Context,
    val callInfo: CallInfo,
    private val viewModel: ExpandedFragmentViewModel
) : RecyclerView.Adapter<MiniAppViewHolder>() {

    companion object {
        private const val TAG = "MiniAppHistoryAdapter"
    }

    private var miniAppList = mutableListOf<MiniAppInfo>()
    private var itemViewMap = mutableMapOf<String, MiniAppItemView>()

    @SuppressLint("NotifyDataSetChanged")
    fun submitData(newData: List<MiniAppInfo>) {
        NewCallAppSdkInterface.printLog(NewCallAppSdkInterface.INFO_LEVEL, TAG, "submitData")
        miniAppList.clear()
        itemViewMap.clear()
        miniAppList.addAll(newData)
        this.notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MiniAppViewHolder {
        return MiniAppViewHolder(
            MiniAppItemView(
                parent.context
            )
        )
    }

    override fun getItemCount(): Int {
        return miniAppList.size
    }

    override fun onBindViewHolder(holder: MiniAppViewHolder, position: Int) {
        NewCallAppSdkInterface.printLog(NewCallAppSdkInterface.INFO_LEVEL, TAG, "onBindViewHolder  position $position")
        if (position < miniAppList.size) {
            val miniAppInfo = miniAppList[position]
            itemViewMap[miniAppInfo.appId] = holder.itemview
            holder.itemview.bindData(miniAppList[position], callInfo, NewCallAppSdkInterface.floatingBallStyle.value?.let { viewModel.getTextColor(context, it) })
            holder.itemview.setOnClickListener {
                // NewCallAppSdkInterface.startMiniApp(callInfo.telecomCallId, miniAppInfo.appId, ::onStartResult)
                handleStartApp(miniAppInfo)
            }
        }
    }

    private fun onStartResult(appId: String, isSuccess: Boolean, reason: Reason?) {
        NewCallAppSdkInterface.printLog(NewCallAppSdkInterface.INFO_LEVEL, TAG, "startMiniApp $appId isSuccess $isSuccess reason $reason")
    }

    fun refreshTextColor(color: Int) {
        itemViewMap.forEach { s, miniAppItemView ->
            miniAppItemView.refreshTextColor(color)
        }
    }

    private fun handleStartApp(miniAppInfo: MiniAppInfo){
        val callSate = NewCallAppSdkInterface.getCallState(callInfo?.telecomCallId)
        NewCallAppSdkInterface.printLog(NewCallAppSdkInterface.INFO_LEVEL,
            TAG, "handleStartApp miniAppInfo: $miniAppInfo ,callSate: $callSate")
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
            NewCallAppSdkInterface.printLog(NewCallAppSdkInterface.DEBUG_LEVEL,
                TAG, "startMiniApp miniAppInfo.appStatus${miniAppInfo.appStatus}")
            callInfo?.let { info ->
                NewCallAppSdkInterface.startMiniApp(info.telecomCallId, miniAppInfo.appId, ::onStartResult)
            }
        }
    }
}