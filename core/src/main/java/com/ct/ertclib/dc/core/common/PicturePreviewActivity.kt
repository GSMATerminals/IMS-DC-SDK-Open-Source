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

import android.net.Uri
import android.os.Bundle
import android.text.TextUtils
import androidx.core.view.isVisible
import com.ct.ertclib.dc.core.databinding.ActivityPicturePreviewBinding
import com.ct.ertclib.dc.core.picker.pickCamera
import com.ct.ertclib.dc.core.ui.activity.BaseAppCompatActivity
import com.ct.ertclib.dc.core.utils.common.BitmapUtils
import com.ct.ertclib.dc.core.utils.common.FileUtils
import com.ct.ertclib.dc.core.utils.common.ToastUtils
import com.ct.ertclib.dc.core.utils.common.UriUtils

class PicturePreviewActivity : BaseAppCompatActivity() {
    private lateinit var mViewBinding: ActivityPicturePreviewBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mViewBinding = ActivityPicturePreviewBinding.inflate(layoutInflater)
        setContentView(mViewBinding.root)
        supportActionBar?.hide()
        val imgSrc = intent.getStringExtra("imgSrc")
        mViewBinding.ivPreview.load(imgSrc)
        mViewBinding.ivPreview.setOnClickListener {
            mViewBinding.toolBar.isVisible = !mViewBinding.toolBar.isVisible
            mViewBinding.save.isVisible = !mViewBinding.save.isVisible
        }
        mViewBinding.ivBack.setOnClickListener {
            finish()
        }
        var isSaved = false
        mViewBinding.save.setOnClickListener {
            if (!isSaved){
                if (imgSrc != null) {
                    BitmapUtils.getBitmapFromPath(this@PicturePreviewActivity,imgSrc)
                        ?.let { it1 ->
                            BitmapUtils.saveImageToGallery(this@PicturePreviewActivity, it1)
                            isSaved = true
                            mViewBinding.save.text = "已保存至相册"
                            ToastUtils.showShortToast(this@PicturePreviewActivity,"保存成功")
                        }
                }
            } else {
                ToastUtils.showShortToast(this@PicturePreviewActivity,"已保存")
            }

        }
    }
}