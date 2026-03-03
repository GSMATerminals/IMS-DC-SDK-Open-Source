package com.ct.ertclib.dc.core.ui.anim

import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.view.View
import android.view.animation.OvershootInterpolator
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.RecyclerView

class MiniAppAnimator : DefaultItemAnimator() {

    // 处理添加 Item 的动画
    override fun animateAdd(holder: RecyclerView.ViewHolder?): Boolean {
        holder ?: return super.animateAdd(holder)

        // 设置初始状态：透明 + 向上偏移 + 缩小
        holder.itemView.alpha = 0f

        // 组合动画：透明度恢复 + 位置归位 + 缩放恢复
        val animatorSet = AnimatorSet().apply {
            playTogether(
                ObjectAnimator.ofFloat(holder.itemView, View.ALPHA, 0f, 1f),
            )
            duration = 150
            interpolator = OvershootInterpolator(1.0f)
        }

        animatorSet.addListener(object : Animator.AnimatorListener {
            override fun onAnimationStart(animation: Animator) {}
            override fun onAnimationEnd(animation: Animator) {
                // 动画结束后重置视图状态，避免复用问题
                holder.itemView.alpha = 1f
                dispatchAddFinished(holder)
            }
            override fun onAnimationCancel(animation: Animator) {
                dispatchAddFinished(holder)
            }
            override fun onAnimationRepeat(animation: Animator) {}
        })

        animatorSet.start()
        return true
    }

    override fun animateRemove(holder: RecyclerView.ViewHolder?): Boolean {
        holder ?: return super.animateRemove(holder)

        val animatorSet = AnimatorSet().apply {
            playTogether(
                ObjectAnimator.ofFloat(holder.itemView, View.ALPHA, 1f, 0f),
            )
            duration = 150
            interpolator = OvershootInterpolator(1.0f)
        }

        animatorSet.addListener(object : Animator.AnimatorListener {
            override fun onAnimationStart(animation: Animator) {}
            override fun onAnimationEnd(animation: Animator) {
                // 动画结束后重置视图状态
                holder.itemView.alpha = 1f
                dispatchRemoveFinished(holder)
            }
            override fun onAnimationCancel(animation: Animator) {
                dispatchRemoveFinished(holder)
            }
            override fun onAnimationRepeat(animation: Animator) {}
        })

        animatorSet.start()
        return true
    }

}