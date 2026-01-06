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

import android.graphics.drawable.Drawable
import android.view.Gravity
import android.widget.TextView
import androidx.appcompat.content.res.AppCompatResources
import androidx.appcompat.widget.Toolbar
import com.ct.ertclib.dc.core.R

open class BaseToolBarActivity: BaseAppCompatActivity() {

    open var toolbar: Toolbar? = null
    private var toolbarTitle: TextView? = null

    override fun onContentChanged() {
        super.onContentChanged()
        toolbar = findViewById(R.id.tool_bar)
        toolbarTitle = findViewById(R.id.tool_bar_title)
        toolbar?.let {
            it.navigationIcon = getNavigationIcon()
            it.setNavigationOnClickListener {
                onBackPressed()
            }
        }
        toolbarTitle?.let {
            it.text = getTooBarTitle()
            if (isCenterStyle()) {
                it.gravity = Gravity.CENTER
                it.setPaddingRelative(0, 0, 0, 0)
            } else {
                it.gravity = Gravity.CENTER_VERTICAL or Gravity.START
                it.setPaddingRelative(resources.getDimensionPixelSize(R.dimen.toolbar_layout_padding_start), 0, 0, 0)
            }
        }
    }

    open fun getTooBarTitle(): String {
        return ""
    }

    open fun getNavigationIcon(): Drawable? {
        return AppCompatResources.getDrawable(this, R.drawable.icon_back)
    }

    open fun isCenterStyle(): Boolean {
        return true
    }
}