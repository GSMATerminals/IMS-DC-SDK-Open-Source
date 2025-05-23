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

package com.ct.ertclib.dc.app.ui.view

import android.content.Context
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.telecom.Call
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.IntDef
import androidx.appcompat.content.res.AppCompatResources
import com.bumptech.glide.Glide
import com.ct.ertclib.dc.app.R
import com.ct.ertclib.dc.core.common.NewCallAppSdkInterface
import com.ct.ertclib.dc.core.common.NewCallAppSdkInterface.SDK_PERCENT_CONSTANTS
import com.ct.ertclib.dc.core.data.call.CallInfo
import com.ct.ertclib.dc.core.data.model.MiniAppInfo

class MiniAppItemView : LinearLayout {

    @IntDef(
        Status.STATUS_UNKNOWN,
        Status.STATUS_READY,
        Status.STATUS_DOWNLOAD,
        Status.STATUS_PAUSE,
        Status.STATUS_FINISH,
        Status.STATUS_ERROR
    )
    @Retention(AnnotationRetention.SOURCE)
    annotation class Status {
        companion object {
            const val STATUS_UNKNOWN: Int = -1
            const val STATUS_READY: Int = 0
            const val STATUS_DOWNLOAD: Int = 1
            const val STATUS_PAUSE: Int = 2
            const val STATUS_FINISH: Int = 3
            const val STATUS_ERROR: Int = 4
        }
    }

    private var data: MiniAppInfo? = null

    @Status
    var status: Int = Status.STATUS_UNKNOWN

    private lateinit var iconIv: ImageView
    lateinit var titleTv: TextView
    private lateinit var progressBar: CircleProgressBar
    private lateinit var disableView: View

    constructor(context: Context?) : super(context) {
        init()
    }

    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs) {
        init()
    }

    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        init()
    }

    private fun init() {
        val inflate = LayoutInflater.from(context).inflate(R.layout.miniapp_item_view, this, true)
        iconIv = inflate.findViewById(R.id.miniapp_icon)
        titleTv = inflate.findViewById(R.id.miniapp_title)
        progressBar = inflate.findViewById(R.id.progress_bar)
        disableView = inflate.findViewById(R.id.disable_view)
    }

    fun bindData(data: MiniAppInfo, callInfo: CallInfo, textColor: Int?) {
        this.data = data
        Glide.with(context)
            .load(data.appIcon)
            .placeholder(com.ct.ertclib.dc.core.R.drawable.icon_miniapp)
            .into(iconIv)
        titleTv.text = data.appName
        if (!NewCallAppSdkInterface.supportScene(data) || callInfo.state == Call.STATE_HOLDING || !NewCallAppSdkInterface.supportDC(data) || !NewCallAppSdkInterface.supportPhase(data)){
            disableView.visibility = VISIBLE
            titleTv.alpha = 0.8F
        } else {
            disableView.visibility = GONE
            titleTv.alpha = 1.0F
        }
        textColor?.let {
            titleTv.setTextColor(it)
        }
    }

    fun bindSimple(name: String, drawableRes: Int, textColor: Int?) {
        disableView.visibility = GONE
        iconIv.setImageDrawable(AppCompatResources.getDrawable(context, drawableRes))
        titleTv.text = name
        textColor?.let {
            titleTv.setTextColor(it)
        }
    }

    fun updateStatus(@Status status: Int) {
        this.status = status
        progressBar.status = when (status) {

            Status.STATUS_DOWNLOAD -> {
                CircleProgressBar.Status.Loading
            }

            Status.STATUS_ERROR -> {
                CircleProgressBar.Status.Error
            }

            Status.STATUS_FINISH -> {
                CircleProgressBar.Status.Finish
            }

            Status.STATUS_PAUSE -> {
                CircleProgressBar.Status.Pause
            }

            Status.STATUS_READY -> {
                progressBar.visibility = GONE
                CircleProgressBar.Status.Finish
            }

            Status.STATUS_UNKNOWN -> {
                progressBar.visibility = GONE
                CircleProgressBar.Status.Waiting
            }

            else -> {
                CircleProgressBar.Status.Waiting
            }
        }
    }

    fun updateProgress(progress: Int) {
        progressBar.progress = progress
        if (progress < SDK_PERCENT_CONSTANTS) {
            updateStatus(Status.STATUS_DOWNLOAD)
            progressBar.visibility = VISIBLE
            val colorMatrix = ColorMatrix()
            colorMatrix.setSaturation(0f)
            iconIv.colorFilter = ColorMatrixColorFilter(colorMatrix)
        } else {
            updateStatus(Status.STATUS_FINISH)
            iconIv.colorFilter = null
            postDelayed({
                progressBar.visibility = GONE
                updateStatus(Status.STATUS_READY)
            }, 1000)
        }
    }

    fun refreshTextColor(color: Int) {
        titleTv.setTextColor(color)
    }

}