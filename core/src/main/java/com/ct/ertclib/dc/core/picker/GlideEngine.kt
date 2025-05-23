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

package com.ct.ertclib.dc.core.picker

import android.content.Context
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.luck.picture.lib.engine.ImageEngine
import com.luck.picture.lib.utils.ActivityCompatHelper


object GlideEngine : ImageEngine {

    override fun loadAlbumCover(context: Context, url: String?, imageView: ImageView) {
        if (!ActivityCompatHelper.assertValidRequest(context)) {
            return
        }
        Glide.with(context)
            .asBitmap()
            .load(url)
            .override(180, 180)
            .sizeMultiplier(0.5f)
            .transform(CenterCrop(), RoundedCorners(8))
            .placeholder(com.luck.picture.lib.R.drawable.ps_image_placeholder)
            .into(imageView)
    }

    override fun loadGridImage(context: Context?, url: String?, imageView: ImageView?) {
        if (context != null) {
            if (imageView != null) {
                Glide.with(context)
                    .load(url)
                    .into(imageView)
            }
        }
    }

    override fun loadImage(context: Context?, url: String?, imageView: ImageView?) {
        if (context != null) {
            if (imageView != null) {
                Glide.with(context)
                    .load(url)
                    .into(imageView)
            }
        }
    }

    override fun loadImage(
        context: Context?,
        imageView: ImageView?,
        url: String?,
        maxWidth: Int,
        maxHeight: Int
    ) {
        if (context != null) {
            if (imageView != null) {
                Glide.with(context)
                    .load(url)
                    .override(maxWidth, maxHeight)
                    .into(imageView)
            }
        }
    }
    override fun pauseRequests(context: Context) {
        if (!ActivityCompatHelper.assertValidRequest(context)) {
            return
        }
        Glide.with(context).pauseRequests()
    }

    override fun resumeRequests(context: Context) {
        if (!ActivityCompatHelper.assertValidRequest(context)) {
            return
        }
        Glide.with(context).resumeRequests()
    }
}