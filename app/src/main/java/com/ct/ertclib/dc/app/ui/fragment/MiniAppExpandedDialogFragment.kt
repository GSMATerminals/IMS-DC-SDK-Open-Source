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

package com.ct.ertclib.dc.app.ui.fragment

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.app.Dialog
import android.content.DialogInterface
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.Point
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.Window
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.core.view.isVisible
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.ct.ertclib.dc.app.R
import com.ct.ertclib.dc.app.databinding.ExpandedMenuDialogBinding
import com.ct.ertclib.dc.app.ui.adapter.AdAdapter
import com.ct.ertclib.dc.core.common.NewCallAppSdkInterface
import com.ct.ertclib.dc.core.data.call.CallInfo
import com.ct.ertclib.dc.core.data.event.MiniAppListGetEvent
import com.ct.ertclib.dc.core.data.miniapp.MiniAppList
import com.ct.ertclib.dc.app.ui.adapter.MiniAppAdapter
import com.ct.ertclib.dc.app.ui.adapter.MiniAppHistoryAdapter
import com.ct.ertclib.dc.app.ui.viewmodel.ExpandedFragmentViewModel
import com.ct.ertclib.dc.core.common.NewCallAppSdkInterface.SDK_MINI_APP_LIST_PAGE_SIZE
import com.ct.ertclib.dc.core.constants.CommonConstants
import com.ct.ertclib.dc.core.data.model.AdItem
import com.ct.ertclib.dc.core.ui.activity.WebActivity
import com.scwang.smart.refresh.footer.ClassicsFooter
import com.scwang.smart.refresh.header.ClassicsHeader
import com.scwang.smart.refresh.layout.api.RefreshLayout
import com.scwang.smart.refresh.layout.listener.OnLoadMoreListener
import com.scwang.smart.refresh.layout.listener.OnRefreshListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.absoluteValue


class MiniAppExpandedDialogFragment(
    private var miniAppList: MiniAppList,
    private var adList: ArrayList<AdItem>,
    private var callInfo: CallInfo?
) :
    BaseMiniAppListDialogFragment() {

    companion object {
        private const val TAG = "MiniAppExpandedDialogFragment"
        private const val EXPANDED_ITEM_SPAN_COUNT = 4
    }

    private lateinit var viewModel: ExpandedFragmentViewModel
    private lateinit var viewBinding: ExpandedMenuDialogBinding
    private var callback: Callback? = null
    private lateinit var miniAppAdapter: MiniAppAdapter
    private lateinit var miniAppHistoryAdapter: MiniAppHistoryAdapter
    private var preTouchY = 0f
    private var isMove = false
    private val positionArray = IntArray(2)
    private var isFirstLaunch = true

    override fun onCreate(savedInstanceState: Bundle?) {
        NewCallAppSdkInterface.printLog(NewCallAppSdkInterface.DEBUG_LEVEL, TAG, "onCreate")
        super.onCreate(savedInstanceState)
        dialog?.requestWindowFeature(Window.FEATURE_NO_TITLE)
        viewModel = ViewModelProvider(this)[ExpandedFragmentViewModel::class.java]
    }

    override fun onStart() {
        super.onStart()
        val window = dialog?.window
        window?.let {
            it.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            it.setGravity(Gravity.BOTTOM)
            it.setWindowAnimations(R.style.ExpandedDialogAnimStyle)
            if (isFirstLaunch) {
                val valueAnimator = ValueAnimator.ofFloat(0F, 1F)
                valueAnimator.duration = 300L
                valueAnimator.addUpdateListener { animator ->
                    val value = animator.animatedValue as? Float
                    value?.let { v ->
                        it.decorView.scaleX = v
                        it.decorView.scaleY = v
                    }
                }
                valueAnimator.addListener(object: AnimatorListenerAdapter() {
                    override fun onAnimationStart(animation: Animator) {
                        isFirstLaunch = false
                    }

                    override fun onAnimationEnd(animation: Animator) {
                        it.decorView.pivotX = it.decorView.width.toFloat() / 2
                        it.decorView.pivotY = it.decorView.height.toFloat() / 2
                    }
                })
                valueAnimator.interpolator = AccelerateDecelerateInterpolator()
                valueAnimator.start()
            }
        }
    }

    override fun onDestroy() {
        NewCallAppSdkInterface.printLog(NewCallAppSdkInterface.DEBUG_LEVEL, TAG, "onDestroy")
        lifecycleScope.cancel()
        super.onDestroy()
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        viewBinding = ExpandedMenuDialogBinding.inflate(layoutInflater)
        dialog.setContentView(viewBinding.root)
        dialog.setCanceledOnTouchOutside(true)
        initView()
        callInfo?.telecomCallId?.let {
            viewModel.historyMiniAppList.postValue(viewModel.getHistoryMiniAppList(it))
        }
        return dialog
    }

    override fun setCallback(callback: Callback) {
        this.callback = callback
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun refreshCallInfo(newCallInfo: CallInfo){
        this.callInfo = newCallInfo
        miniAppAdapter.run { notifyDataSetChanged() }
        miniAppHistoryAdapter.run { notifyDataSetChanged() }
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun refreshAppStatus(){
        miniAppAdapter.run { notifyDataSetChanged() }
        miniAppHistoryAdapter.run { notifyDataSetChanged() }
    }

    private fun onStartMiniAppSuccess() {
        NewCallAppSdkInterface.printLog(NewCallAppSdkInterface.DEBUG_LEVEL, TAG, "onStartMiniAppSuccess")
        callInfo?.telecomCallId?.let {
            viewModel.historyMiniAppList.postValue(viewModel.getHistoryMiniAppList(it))
        }
        dismiss()
    }

    private fun initView() {
        viewBinding.refreshLayout.setRefreshHeader(ClassicsHeader(this@MiniAppExpandedDialogFragment.context))
        viewBinding.refreshLayout.setRefreshFooter(ClassicsFooter(this@MiniAppExpandedDialogFragment.context))
        viewBinding.refreshLayout.setOnRefreshListener(object : OnRefreshListener {
            override fun onRefresh(refreshLayout: RefreshLayout) {
                miniAppList.callId?.let {
                    NewCallAppSdkInterface.emitAppListEvent(
                        MiniAppListGetEvent(
                            0,
                            MiniAppListGetEvent.TO_GET, it,0,CommonConstants.MINI_APP_LIST_PAGE_SIZE,null,null
                        )
                    )
                }
                viewBinding.refreshLayout.finishRefresh(2000)
            }
        })
        viewBinding.refreshLayout.setOnLoadMoreListener(object : OnLoadMoreListener {
            override fun onLoadMore(refreshLayout: RefreshLayout) {
                if (miniAppList.totalAppNum <= (miniAppList.beginIndex.plus(SDK_MINI_APP_LIST_PAGE_SIZE)!!)) {
                    viewBinding.refreshLayout.finishLoadMoreWithNoMoreData()
                } else {
                    miniAppList.callId?.let {
                        NewCallAppSdkInterface.emitAppListEvent(
                            MiniAppListGetEvent(
                                0,
                                MiniAppListGetEvent.TO_GET,
                                it,
                                miniAppList.beginIndex.plus(CommonConstants.MINI_APP_LIST_PAGE_SIZE),
                                CommonConstants.MINI_APP_LIST_PAGE_SIZE,
                                null,
                                null
                            )
                        )
                    }
                    viewBinding.refreshLayout.finishLoadMore(2000)
                }
            }
        })
        activity?.let { activity ->
            val gridLayoutManager = GridLayoutManager(activity, EXPANDED_ITEM_SPAN_COUNT)
            viewBinding.recyclerview.layoutManager = gridLayoutManager
            miniAppAdapter = MiniAppAdapter(activity, callInfo, ::onStartMiniAppSuccess, lifecycleScope, viewModel)
            viewBinding.recyclerview.adapter = miniAppAdapter
            viewBinding.historyRecyclerview.layoutManager = GridLayoutManager(activity, 1).apply { orientation = LinearLayoutManager.HORIZONTAL }
            callInfo?.let {
                miniAppHistoryAdapter = MiniAppHistoryAdapter(activity, callInfo!!, viewModel)
                viewBinding.historyRecyclerview.adapter = miniAppHistoryAdapter
            }

            if (adList.isNotEmpty()) {
                viewBinding.adLayout.visibility = View.VISIBLE
                val adAdapter = AdAdapter{ item ->
                    WebActivity.startActivity(activity, item.h5Url, item.title,null)
                }
                viewBinding.bannerViewpager.apply {
                    adapter = adAdapter
                    offscreenPageLimit = 1  // 预加载1页
                    overScrollMode = View.OVER_SCROLL_NEVER  // 禁用过度滚动

                    // 可选：设置页面间距
                    setPageTransformer { page, position ->
                        page.translationX = position * page.width * 0.01f
                    }
                }
                adAdapter.submitData(adList)
            } else {
                viewBinding.adLayout.visibility = View.GONE
            }
        }
        viewModel.miniAppList.observe(this) { t ->
            (viewBinding.recyclerview.adapter as? MiniAppAdapter)?.submitData(t)
        }
        miniAppList?.applications?.let {
            viewModel.miniAppList.postValue(it)
        }
        viewModel.historyMiniAppList.observe(this) { list ->
            (viewBinding.historyRecyclerview.adapter as? MiniAppHistoryAdapter)?.submitData(list)
            viewBinding.historyTitleLayout.isVisible = (list.isNotEmpty())
        }

        viewBinding.exit.setOnClickListener {
            NewCallAppSdkInterface.printLog(NewCallAppSdkInterface.INFO_LEVEL, TAG, "Exit miniapp dialog")
            dismiss()
        }
        viewBinding.setting.setOnClickListener {
            NewCallAppSdkInterface.printLog(NewCallAppSdkInterface.INFO_LEVEL, TAG, "Jump miniapp dialog setting")
            dismiss()
            activity?.let {
                NewCallAppSdkInterface.startSettingsActivity(it)
            }
        }
        viewBinding.skin.setOnClickListener {
            NewCallAppSdkInterface.printLog(NewCallAppSdkInterface.INFO_LEVEL, TAG, "click skin page")
            activity?.let {
                NewCallAppSdkInterface.startStyleSettingActivity(it)
            }
        }
        lifecycleScope.launch {
            NewCallAppSdkInterface.miniAppListEventFlow.distinctUntilChanged().collect { event ->
                NewCallAppSdkInterface.printLog(NewCallAppSdkInterface.DEBUG_LEVEL, TAG, "collect  miniAppListEventFlow event: ${event.message}")
                if (MiniAppListGetEvent.ON_DOWNLOAD == event.message && event.callId == miniAppList.callId && event.miniAppListInfoAll != null) {
                    withContext(Dispatchers.Main) {
                        miniAppList = event.miniAppListInfoAll!!
                        miniAppList.applications?.let {
                            viewModel.miniAppList.postValue(it)
                        }
                        viewBinding.refreshLayout.finishLoadMore()
                        viewBinding.refreshLayout.finishRefresh()
                    }
                }
            }
        }
        lifecycleScope.launch {
            NewCallAppSdkInterface.floatingBallStyle.collect { style ->
                NewCallAppSdkInterface.printLog(NewCallAppSdkInterface.DEBUG_LEVEL, TAG, "viewModel.style.observe: style: $style")
                refreshPanelUI()
            }
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        refreshPanelUI()
    }

    private fun refreshPanelUI() {
        val style = NewCallAppSdkInterface.floatingBallStyle.value
        activity?.let {
            val textColor = viewModel.getTextColor(it, style)
            val backgroundColor = viewModel.getBackgroundDrawableColor(it, style)
            val iconStyle = viewModel.getIconStyle(style)
            refreshBackgroundColor(backgroundColor)
            refreshTextColor(textColor)
            refreshIcon(iconStyle)
        }
    }

    private fun refreshBackgroundColor(color: Int) {
        val miniAppLayoutBackGround = viewBinding.miniappLayout.background as? GradientDrawable
        miniAppLayoutBackGround?.setColor(color)
        val settingLayoutBackGround = viewBinding.miniappSettingLayout.background as? GradientDrawable
        settingLayoutBackGround?.setColor(color)
    }

    private fun refreshTextColor(color: Int) {
        viewBinding.miniAppTitle.setTextColor(color)
        viewBinding.historyTitle.setTextColor(color)
        viewBinding.exit.setTextColor(color)
        viewBinding.setting.setTextColor(color)
        viewBinding.skin.setTextColor(color)
        viewBinding.firstDivider.setBackgroundColor(color)
        viewBinding.secondDivider.setBackgroundColor(color)
        viewBinding.skin.setTextColor(color)
        miniAppAdapter.refreshTextColor(color)
        miniAppHistoryAdapter.refreshTextColor(color)
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    private fun refreshIcon(iconStyle: Int) {
        when (iconStyle) {
            ExpandedFragmentViewModel.ICON_STYLE_LIGHT -> {
                val exitDrawable = resources.getDrawable(R.drawable.icon_back_light)
                exitDrawable.setBounds(0, 0, exitDrawable.minimumWidth, exitDrawable.minimumHeight)
                viewBinding.exit.setCompoundDrawables(null, exitDrawable, null, null)
                val settingDrawable = resources.getDrawable(R.drawable.icon_setting_light)
                settingDrawable.setBounds(0, 0, settingDrawable.minimumWidth, settingDrawable.minimumHeight)
                viewBinding.setting.setCompoundDrawables(null, settingDrawable, null, null)
                val skinDrawable = resources.getDrawable(R.drawable.icon_skin_light)
                skinDrawable.setBounds(0, 0, skinDrawable.minimumWidth, skinDrawable.minimumHeight)
                viewBinding.skin.setCompoundDrawables(null, skinDrawable, null, null)
            }
            else -> {
                val exitDrawable = resources.getDrawable(R.drawable.icon_back_expanded)
                exitDrawable.setBounds(0, 0, exitDrawable.minimumWidth, exitDrawable.minimumHeight)
                viewBinding.exit.setCompoundDrawables(null, exitDrawable, null, null)
                val settingDrawable = resources.getDrawable(R.drawable.icon_setting_res)
                settingDrawable.setBounds(0, 0, settingDrawable.minimumWidth, settingDrawable.minimumHeight)
                viewBinding.setting.setCompoundDrawables(null, settingDrawable, null, null)
                val skinDrawable = resources.getDrawable(R.drawable.icon_skin_normal)
                skinDrawable.setBounds(0, 0, skinDrawable.minimumWidth, skinDrawable.minimumHeight)
                viewBinding.skin.setCompoundDrawables(null, skinDrawable, null, null)
            }
        }
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        callback?.onDismiss()
        NewCallAppSdkInterface.printLog(NewCallAppSdkInterface.INFO_LEVEL, TAG, "onDismiss")
    }
}