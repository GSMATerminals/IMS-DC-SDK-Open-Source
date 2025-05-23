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

package com.ct.ertclib.dc.core.common

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.net.Uri
import android.widget.ImageView
import androidx.annotation.DrawableRes
import com.blankj.utilcode.util.SizeUtils
import com.bumptech.glide.Glide
import com.bumptech.glide.load.Transformation
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
import com.ct.ertclib.dc.core.constants.ContextConstants
import com.ct.ertclib.dc.core.utils.common.UriUtils
import jp.wasabeef.glide.transformations.RoundedCornersTransformation


/**
 * 指定圆角
 * @param cornerType RoundedCornersTransformation.CornerType
 */
fun ImageView.load(
    url: String?,
    round: Float = 0f,
    cornerType: RoundedCornersTransformation.CornerType,
    @DrawableRes placeImg: Int? = null
) {
    if (this.context == null) return
    if (this.context is Activity) {
        val activity = this.context as Activity
        if (activity.isDestroyed || activity.isFinishing) {
            return
        }
    }
    val option = RequestOptions()
        .diskCacheStrategy(DiskCacheStrategy.ALL)
        .skipMemoryCache(false)
        .dontAnimate()

    if (placeImg != null) {
        option.placeholder(placeImg)
    }

    if (round != 0f) {
        option.transform(
            CenterCrop(),
            RoundedCornersTransformation(round.toInt(), 0, cornerType)
        )
    }

    Glide.with(this.context)
        .load(url ?: "")
        .apply(option)
        .into(this)
}

/**
 * Glide加载图片，可以指定圆角弧度。
 *
 * @param url 图片地址
 * @param round 圆角，单位dp
 * @param placeImg 占位图
 */
fun ImageView.load(url: String?, round: Float = 0f, @DrawableRes placeImg: Int? = null) {
    if (this.context == null) return
    if (this.context is Activity) {
        val activity = this.context as Activity
        if (activity.isDestroyed || activity.isFinishing) {
            return
        }
    }
    val option = RequestOptions()
        .diskCacheStrategy(DiskCacheStrategy.ALL)//全部缓存，原图与压缩图
        .skipMemoryCache(false)//注:是否跳过内存缓存，设置为false，如为true的话每次闪烁也正常~
        .dontAnimate()//取消Glide自带的动画

    if (placeImg != null) {
        option.placeholder(placeImg)
    }

    if (round != 0f) {
        option.transform(CenterCrop(), RoundedCorners(SizeUtils.dp2px(round)))
    }

    Glide.with(this.context)
        .load(url ?: "")
        .apply(option)
        .into(this)
}

fun ImageView.load(bitmap: Bitmap?, round: Float = 0f, @DrawableRes placeImg: Int? = null) {
    if (this.context == null) return
    if (this.context is Activity) {
        val activity = this.context as Activity
        if (activity.isDestroyed || activity.isFinishing) {
            return
        }
    }
    val option = RequestOptions()
        .diskCacheStrategy(DiskCacheStrategy.ALL)
        .skipMemoryCache(false)
        .dontAnimate()

    if (placeImg != null) {
        option.error(placeImg)
            .transform(CenterCrop())
    }

    if (round != 0f) {
        option.transform(CenterCrop(), RoundedCorners(SizeUtils.dp2px(round)))
    }

    Glide.with(this.context)
        .load(bitmap)
        .apply(option)
        .into(this)
}

fun ImageView.load(
    url: String,
    @DrawableRes placeImg: Int? = null,
    vararg transformations: Transformation<Bitmap>
) {
    if (this.context == null) return
    if (this.context is Activity) {
        val activity = this.context as Activity
        if (activity.isDestroyed || activity.isFinishing) {
            return
        }
    }
    val option = RequestOptions()
    if (transformations != null) {
        transformations.forEach {
            option.transform(it)
        }
    }
    val parse = Uri.parse(url)
    if (UriUtils.isContentUri(parse)) {
        this.setImageURI(parse)
    } else {
        if (placeImg == null) {
            Glide.with(this.context).load(url).apply(option).into(this)
        } else {
            Glide.with(this.context).load(url).apply(option).placeholder(placeImg).into(this)
        }
    }
}

fun ImageView.load(
    @DrawableRes img: Int,
    @DrawableRes placeImg: Int? = null,
    vararg transformations: Transformation<Bitmap>
) {
    if (this.context == null) return
    if (this.context is Activity) {
        val activity = this.context as Activity
        if (activity.isDestroyed || activity.isFinishing) {
            return
        }
    }
    val option = RequestOptions();
    if (transformations != null) {
        transformations.forEach {
            option.transform(it)
        }
    }
    if (placeImg == null) {
        Glide.with(this.context).load(img).apply(option).into(this)
    } else {
        Glide.with(this.context).load(img).apply(option).placeholder(placeImg).into(this)
    }
}

fun Context.startImagePreview(imgSrc: String) {
    val intent = Intent(ContextConstants.INTENT_PREVIEW_IMAGE)
    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
    intent.putExtra("imgSrc", imgSrc)
    this.startActivity(intent)
}

fun generateNumberAvatar(number: String, size: Int): Bitmap {
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val paint = Paint().apply {
        isAntiAlias = true
        color = Color.parseColor("#FFC107") // 设置颜色
    }

    canvas.drawCircle(size.toFloat() / 2, size.toFloat() / 2, size.toFloat() / 2, paint)

    paint.color = Color.WHITE // 修改颜色为白色
    paint.textSize = size.toFloat() / 2
    paint.textAlign = Paint.Align.CENTER

    val x = size.toFloat() / 2
    val y = size.toFloat() / 2 - (paint.descent() + paint.ascent()) / 2

    val textToDraw = if (number.length > 4) {
        number.substring(number.length - 4)
    } else {
        number
    }

    canvas.drawText(textToDraw, x, y, paint)

    return bitmap
}

