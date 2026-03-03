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

package com.ct.ertclib.dc.core.ui.activity

import android.Manifest
import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.telecom.TelecomManager
import android.view.View
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.app.ActivityCompat
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.ct.ertclib.dc.core.R
import com.ct.ertclib.dc.core.common.sdkpermission.SDKPermissionUtils
import com.ct.ertclib.dc.core.databinding.ActivityMainBinding
import com.ct.ertclib.dc.core.ui.adapter.MainMiniAppAdapter
import com.ct.ertclib.dc.core.ui.anim.MiniAppAnimator
import com.ct.ertclib.dc.core.ui.viewholder.MainMiniAppViewHolder
import com.ct.ertclib.dc.core.ui.viewmodel.MainViewModel
import com.ct.ertclib.dc.core.ui.viewmodel.MainViewModel.Companion.DELETE_STATUS_ENSURE
import com.ct.ertclib.dc.core.ui.viewmodel.MainViewModel.Companion.DELETE_STATUS_HIDE
import com.ct.ertclib.dc.core.ui.viewmodel.MainViewModel.Companion.DELETE_STATUS_SHOW
import com.ct.ertclib.dc.core.ui.viewmodel.MainViewModel.Companion.PAGE_STATUS_INIT
import com.ct.ertclib.dc.core.ui.viewmodel.MainViewModel.Companion.PAGE_STATUS_LIST
import com.ct.ertclib.dc.core.ui.viewmodel.MainViewModel.Companion.PAGE_STATUS_NOT_OPEN
import com.ct.ertclib.dc.core.ui.viewmodel.MainViewModel.Companion.PAGE_STATUS_NO_HISTORY
import com.ct.ertclib.dc.core.utils.common.LogUtils
import com.ct.ertclib.dc.core.utils.common.ScreenUtils
import com.ct.ertclib.dc.core.utils.extension.startSettingsActivity
import kotlinx.coroutines.launch

class MainActivity : NoManagedBaseToolBarActivity() {

    companion object {
        private const val TAG = "MainActivity"
        private const val TIME_ANIMATION_DELETE = 200L
    }

    private lateinit var binding: ActivityMainBinding
    private lateinit var viewModel: MainViewModel
    private var adapter: MainMiniAppAdapter? = null

    private var dragPosition = -1

    override var iconCallback: (() -> Unit)? = {
        this@MainActivity.startSettingsActivity()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        viewModel = ViewModelProvider(this)[MainViewModel::class.java]
        initView()
        initViewModel()
        // 不干涉通话流程，在这里更新一下版本号，在检查权限时做判断。可能在下次通话时才会提示用户。
        SDKPermissionUtils.updatePrivacyVersion()
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshStatus(this@MainActivity)
        val telecomManager = this.getSystemService(Context.TELECOM_SERVICE) as TelecomManager
        val isInCall = if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_PHONE_STATE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            false
        } else {
            // can be in dialing, ringing, active or holding states
            telecomManager.isInCall
        }
        adapter?.isInCall = isInCall
    }

    override fun getTooBarTitle(): String {
        return resources.getString(R.string.main_activity_title)
    }

    override fun isCenterStyle(): Boolean {
        return true
    }

    override fun getNavigationIcon(): Drawable? {
        return null
    }

    override fun getToolbarIcon(): Drawable? {
        return AppCompatResources.getDrawable(this@MainActivity, R.drawable.icon_mini_setting)
    }

    private fun initView() {
        binding.mainPageOpen.setOnClickListener {
            this@MainActivity.startSettingsActivity()
        }
        adapter = MainMiniAppAdapter(this)
        binding.recyclerview.layoutManager = GridLayoutManager(this,4)
        binding.recyclerview.adapter = adapter
        binding.recyclerview.itemAnimator = MiniAppAnimator()
        itemTouchHelper.attachToRecyclerView(binding.recyclerview)
    }

    private fun initViewModel() {
        viewModel.pageStatus.observe(this) { status ->
            LogUtils.debug(TAG, "pageStatus observe status : $status")
            when (status) {
                PAGE_STATUS_INIT -> {
                    binding.mainPageImage.isVisible = false
                    binding.mainPageTitle.isVisible = false
                    binding.mainPageDescription.isVisible = false
                    binding.mainPageOpen.isVisible = false
                    binding.miniappTitle.isVisible = false
                    binding.recyclerview.isVisible = false
                }
                PAGE_STATUS_NOT_OPEN -> {
                    binding.mainPageImage.isVisible = true
                    binding.mainPageImage.setImageDrawable(AppCompatResources.getDrawable(this@MainActivity, R.drawable.icon_main_page_close))
                    binding.mainPageTitle.isVisible = true
                    binding.mainPageTitle.text = resources.getString(R.string.main_page_title_close)
                    binding.mainPageDescription.isVisible = true
                    binding.mainPageDescription.text = resources.getString(R.string.main_page_description_close)
                    binding.mainPageOpen.isVisible = true
                    binding.mainPageOpen.text = resources.getString(R.string.open_btn_title)
                    binding.miniappTitle.isVisible = false
                    binding.recyclerview.isVisible = false
                }
                PAGE_STATUS_NO_HISTORY -> {
                    binding.mainPageImage.isVisible = true
                    binding.mainPageImage.setImageDrawable(AppCompatResources.getDrawable(this@MainActivity, R.drawable.icon_main_page_no_history))
                    binding.mainPageTitle.isVisible = true
                    binding.mainPageTitle.text = resources.getString(R.string.main_page_title_no_history)
                    binding.mainPageDescription.isVisible = true
                    binding.mainPageDescription.text = resources.getString(R.string.main_page_description_no_history)
                    binding.mainPageOpen.isVisible = false
                    binding.miniappTitle.isVisible = false
                    binding.recyclerview.isVisible = false
                }
                PAGE_STATUS_LIST -> {
                    binding.mainPageImage.isVisible = false
                    binding.mainPageTitle.isVisible = false
                    binding.mainPageDescription.isVisible = false
                    binding.mainPageOpen.isVisible = false
                    binding.miniappTitle.isVisible = true
                    binding.recyclerview.isVisible = true
                    viewModel.miniAppInfos.value?.let { adapter?.submitList(it) }
                }
            }
        }

        viewModel.miniAppInfos.observe(this) { list ->
            LogUtils.debug(TAG, "miniAppInfos observe list : ${list.joinToString()}")
            when (viewModel.pageStatus.value) {
                PAGE_STATUS_NO_HISTORY, PAGE_STATUS_LIST -> {
                    if (list.isEmpty()) {
                        viewModel.pageStatus.postValue(PAGE_STATUS_NO_HISTORY)
                    }
                    lifecycleScope.launch {
                        adapter?.submitList(list)
                    }
                }
            }
        }

        viewModel.deleteStatus.observe(this) { status ->
            LogUtils.debug(TAG, "initViewModel deleteStatus: $status")
            when (status) {
                DELETE_STATUS_HIDE -> {
                    hideDeleteLayout()
                }
                DELETE_STATUS_SHOW -> {
                    showDeleteLayout()
                    binding.deleteText.text = resources.getString(R.string.delete_string)
                }
                DELETE_STATUS_ENSURE -> {
                    binding.deleteLayout.isVisible = true
                    binding.deleteText.text = resources.getString(R.string.delete_string_ensure)
                }
            }
        }
    }

    private fun hideDeleteLayout() {
        if (binding.deleteLayout.isVisible) {
            val valueAnimation = ValueAnimator.ofInt(0, resources.getDimensionPixelSize(R.dimen.delete_layout_height))
            valueAnimation.duration = TIME_ANIMATION_DELETE
            valueAnimation.addUpdateListener { animator ->
                val value = animator.animatedValue as? Int
                value?.let {
                    binding.deleteLayout.translationY = it.toFloat()
                }
            }
            valueAnimation?.addListener(object: AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    binding.deleteLayout.isVisible = false
                    binding.deleteLayout.translationY = 0F
                }
            })
            valueAnimation.start()
        }
    }

    private fun showDeleteLayout() {
        if (!binding.deleteLayout.isVisible) {
            val valueAnimation = ValueAnimator.ofInt(resources.getDimensionPixelSize(R.dimen.delete_layout_height), 0)
            valueAnimation.duration = TIME_ANIMATION_DELETE
            valueAnimation.addUpdateListener { animator ->
                val value = animator.animatedValue as? Int
                value?.let {
                    binding.deleteLayout.translationY = it.toFloat()
                }
            }
            valueAnimation?.addListener(object: AnimatorListenerAdapter() {
                override fun onAnimationStart(animation: Animator) {
                    binding.deleteLayout.isVisible = true
                }
            })
            valueAnimation.start()
        }
    }

    private val itemTouchHelper = ItemTouchHelper(object : ItemTouchHelper.Callback() {
        private var viewHolder: RecyclerView.ViewHolder? = null
        private var isDragging = false

        override fun getMovementFlags(
            recyclerView: RecyclerView,
            viewHolder: RecyclerView.ViewHolder
        ): Int {
            val dragFlags = ItemTouchHelper.UP or ItemTouchHelper.DOWN or ItemTouchHelper.START or ItemTouchHelper.END
            return makeMovementFlags(dragFlags, 0)
        }

        override fun onMove(
            recyclerView: RecyclerView,
            viewHolder: RecyclerView.ViewHolder,
            target: RecyclerView.ViewHolder
        ): Boolean {
            return false
        }

        override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {

        }

        override fun onSelectedChanged(viewHolder: RecyclerView.ViewHolder?, actionState: Int) {
            super.onSelectedChanged(viewHolder, actionState)
            when (actionState) {
                ItemTouchHelper.ACTION_STATE_DRAG -> {
                    isDragging = true
                    this.viewHolder = viewHolder
                    (viewHolder as? MainMiniAppViewHolder)?.textItem?.visibility = View.INVISIBLE
                    viewHolder?.let { dragPosition = it.adapterPosition }
                }
                ItemTouchHelper.ACTION_STATE_IDLE -> {
                    isDragging = false
                    if (viewModel.deleteStatus.value == DELETE_STATUS_ENSURE) {
                        LogUtils.debug(TAG, "onSelectedChanged delete position: $dragPosition")
                        this.viewHolder?.itemView?.isVisible = false
                        viewModel.deleteMiniAppInfo(dragPosition)
                    }
                    dragPosition = -1
                    this.viewHolder = null
                    viewModel.deleteStatus.postValue(DELETE_STATUS_HIDE)
                }
            }
        }

        override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
            super.clearView(recyclerView, viewHolder)
            (viewHolder as? MainMiniAppViewHolder)?.textItem?.visibility = View.VISIBLE
        }

        override fun onChildDraw(
            c: Canvas,
            recyclerView: RecyclerView,
            viewHolder: RecyclerView.ViewHolder,
            dX: Float,
            dY: Float,
            actionState: Int,
            isCurrentlyActive: Boolean
        ) {
            super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
            if (isDragging) {
                val itemLocation = IntArray(2)
                viewHolder.itemView.getLocationOnScreen(itemLocation)
                val itemBottom = itemLocation[1] + viewHolder.itemView.height
                if (itemBottom < ScreenUtils.getScreenHeight(this@MainActivity) - resources.getDimensionPixelSize(R.dimen.delete_layout_height)) {
                    if (viewModel.deleteStatus.value != DELETE_STATUS_SHOW) {
                        viewModel.deleteStatus.postValue(DELETE_STATUS_SHOW)
                    }
                } else {
                    if (viewModel.deleteStatus.value != DELETE_STATUS_ENSURE) {
                        viewModel.deleteStatus.postValue(DELETE_STATUS_ENSURE)
                    }
                }
            }
        }
    })
}

