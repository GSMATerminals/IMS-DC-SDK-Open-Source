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

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.appcompat.content.res.AppCompatResources
import com.ct.ertclib.dc.core.R
import com.ct.ertclib.dc.core.common.NewCallAppSdkInterface.SDK_STYLE_WHITE

@SuppressLint("AppCompatCustomView", "MissingInflatedId")
class MiniAppEntryView : FrameLayout {
    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    )

    companion object {
        private const val SHOW_HIDE_ANIMATION_DURATION = 200L
//        private const val HALF_ICON_ALPHA = 0.3F
        private const val HALF_ICON_ALPHA = 1F
    }

    private val icon: ImageView
    private val halfIcon: ImageView
    var isActive = true
    var isLeft = true

    init {
        val rootView = LayoutInflater.from(context).inflate(R.layout.miniapp_entry_view, this, true)
        icon = rootView.findViewById(R.id.float_ball_icon)
        halfIcon = rootView.findViewById(R.id.float_ball_half_icon)
    }

    fun setFloatingBallMode(isActive: Boolean) {
        if (this.isActive == isActive) {
            return
        }
        if (isActive) {
            showIconWithAnimation()
            hideHalfIconWithAnimation()
        } else {
            showHalfIconWithAnimation()
            hideIconWithAnimation()
        }
    }

    fun setFloatingBallIcon(style: Int) {
        when (style) {
            SDK_STYLE_WHITE  -> {
                icon.setImageDrawable(AppCompatResources.getDrawable(context, R.drawable.icon_ball_light))
            }
            else -> {
                icon.setImageDrawable(AppCompatResources.getDrawable(context, R.drawable.icon_ball_normal))
            }
        }
    }

    fun setFloatingHalfBallIcon(style: Int, isLeft: Boolean) {
        this.isLeft = isLeft
        val layoutParams = halfIcon.layoutParams as FrameLayout.LayoutParams
        if (isLeft) {
            layoutParams.apply {
                gravity = Gravity.START
            }
            when (style) {
                SDK_STYLE_WHITE -> halfIcon.setImageDrawable(AppCompatResources.getDrawable(context, R.drawable.floating_half_view_white))
                else -> halfIcon.setImageDrawable(AppCompatResources.getDrawable(context, R.drawable.floating_half_view_left_grey))
            }
        } else {
            layoutParams.apply {
                gravity = Gravity.END
            }
            when (style) {
                SDK_STYLE_WHITE -> halfIcon.setImageDrawable(AppCompatResources.getDrawable(context, R.drawable.floating_half_view_right_white))
                else -> halfIcon.setImageDrawable(AppCompatResources.getDrawable(context, R.drawable.floating_half_view_grey_right))
            }
        }
        halfIcon.layoutParams = layoutParams
    }

    private fun hideIconWithAnimation() {
        val valueAnimation = ValueAnimator.ofFloat(1F, 0F)
        valueAnimation.duration = SHOW_HIDE_ANIMATION_DURATION
        valueAnimation.addUpdateListener { animator ->
            val value = animator.animatedValue as? Float
            value?.let {
                icon.alpha = value
                icon.scaleX = value
                icon.scaleY = value
            }
        }
        valueAnimation?.addListener(object: AnimatorListenerAdapter() {
            override fun onAnimationStart(animation: Animator) {
                icon.pivotX = if (isLeft) 0F else icon.width.toFloat()
            }

            override fun onAnimationEnd(animation: Animator) {
                icon.alpha = 1F
                icon.visibility = View.INVISIBLE
                isActive = false
                icon.pivotX = icon.width.toFloat() / 2
                icon.scaleX = 1F
                icon.scaleY = 1F
            }

            override fun onAnimationCancel(animation: Animator) {
                icon.alpha = 1F
                icon.visibility = View.INVISIBLE
                isActive = false
                icon.pivotX = icon.width.toFloat() / 2
                icon.scaleX = 1F
                icon.scaleY = 1F
            }
        })
        valueAnimation.start()
    }

    private fun showIconWithAnimation() {
        val valueAnimation = ValueAnimator.ofFloat(0F, 1F)
        valueAnimation.duration = SHOW_HIDE_ANIMATION_DURATION
        valueAnimation.startDelay = SHOW_HIDE_ANIMATION_DURATION
        valueAnimation.addUpdateListener { animator ->
            val value = animator.animatedValue as? Float
            value?.let {
                icon.alpha = value
                icon.scaleX = value
                icon.scaleY = value
            }
        }
        valueAnimation?.addListener(object: AnimatorListenerAdapter() {
            override fun onAnimationStart(animation: Animator) {
                icon.pivotX = if (isLeft) 0F else icon.width.toFloat()
                icon.alpha = 0F
                icon.visibility = View.VISIBLE
            }

            override fun onAnimationEnd(animation: Animator) {
                icon.pivotX = icon.width.toFloat() / 2
                icon.alpha = 1F
                isActive = true
                icon.scaleX = 1F
                icon.scaleY = 1F
            }

            override fun onAnimationCancel(animation: Animator) {
                icon.pivotX = icon.width.toFloat() / 2
                icon.alpha = 1F
                isActive = true
                icon.scaleX = 1F
                icon.scaleY = 1F
            }
        })
        valueAnimation.start()
    }

    private fun hideHalfIconWithAnimation() {
        val valueAnimation = ValueAnimator.ofFloat(1F, 0F)
        valueAnimation.duration = SHOW_HIDE_ANIMATION_DURATION
        valueAnimation.addUpdateListener { animator ->
            val value = animator.animatedValue as? Float
            value?.let {
                halfIcon.alpha = value * HALF_ICON_ALPHA
                halfIcon.scaleX = value
                halfIcon.scaleY = value
            }
        }
        valueAnimation?.addListener(object: AnimatorListenerAdapter() {
            override fun onAnimationStart(animation: Animator) {
                halfIcon.pivotX = if (isLeft) 0F else halfIcon.width.toFloat()
                halfIcon.pivotY = halfIcon.height.toFloat() / 2
            }

            override fun onAnimationEnd(animation: Animator) {
                halfIcon.alpha = HALF_ICON_ALPHA
                halfIcon.visibility = View.INVISIBLE
                halfIcon.pivotX = halfIcon.width.toFloat() / 2
                halfIcon.scaleX = 1F
                halfIcon.scaleY = 1F
            }

            override fun onAnimationCancel(animation: Animator) {
                halfIcon.alpha = HALF_ICON_ALPHA
                halfIcon.visibility = View.INVISIBLE
                halfIcon.pivotX = halfIcon.width.toFloat() / 2
                halfIcon.scaleX = 1F
                halfIcon.scaleY = 1F
            }
        })
        valueAnimation.start()
    }

    private fun showHalfIconWithAnimation() {
        val valueAnimation = ValueAnimator.ofFloat(0F, 1F)
        valueAnimation.duration = SHOW_HIDE_ANIMATION_DURATION
        valueAnimation.startDelay = SHOW_HIDE_ANIMATION_DURATION
        valueAnimation.addUpdateListener { animator ->
            val value = animator.animatedValue as? Float
            value?.let {
                halfIcon.alpha = value * HALF_ICON_ALPHA
                halfIcon.scaleX = value
                halfIcon.scaleY = value
            }
        }
        valueAnimation?.addListener(object: AnimatorListenerAdapter() {
            override fun onAnimationStart(animation: Animator) {
                halfIcon.alpha = 0F
                halfIcon.visibility = View.VISIBLE
                halfIcon.pivotX = if (isLeft) 0F else halfIcon.width.toFloat()
                halfIcon.pivotY = halfIcon.height.toFloat() / 2
            }

            override fun onAnimationEnd(animation: Animator) {
                halfIcon.alpha = HALF_ICON_ALPHA
                halfIcon.pivotX = halfIcon.width.toFloat() / 2
                halfIcon.scaleX = 1F
                halfIcon.scaleY = 1F
            }

            override fun onAnimationCancel(animation: Animator) {
                halfIcon.alpha = HALF_ICON_ALPHA
                halfIcon.pivotX = halfIcon.width.toFloat() / 2
                halfIcon.scaleX = 1F
                halfIcon.scaleY = 1F
            }
        })
        valueAnimation.start()
    }
}