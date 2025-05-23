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

package com.ct.ertclib.dc.core.ui.activity

import android.os.Bundle
import androidx.recyclerview.widget.GridLayoutManager
import com.ct.ertclib.dc.core.R
import com.ct.ertclib.dc.core.databinding.LayoutStyleSettingBinding
import com.ct.ertclib.dc.core.ui.adapter.StyleAdapter
import com.ct.ertclib.dc.core.utils.common.LogUtils

class StyleSettingActivity: BaseToolBarActivity() {

    companion object {
        private const val TAG = "StyleSettingActivity"
        private const val EXPANDED_ITEM_SPAN_COUNT = 2
    }

    private lateinit var binding: LayoutStyleSettingBinding
    private var styleAdapter: StyleAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        LogUtils.debug(TAG, "onCreate")
        binding = LayoutStyleSettingBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val gridLayoutManager = GridLayoutManager(this@StyleSettingActivity, EXPANDED_ITEM_SPAN_COUNT)
        binding.styleRecyclerview.layoutManager = gridLayoutManager
        styleAdapter = StyleAdapter(this@StyleSettingActivity)
        binding.styleRecyclerview.adapter = styleAdapter
    }

    override fun onStop() {
        super.onStop()
        styleAdapter?.saveStyleSetting()
    }

    override fun getTooBarTitle(): String {
        return getString(R.string.style_text)
    }
}