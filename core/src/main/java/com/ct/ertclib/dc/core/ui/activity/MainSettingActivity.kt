/*
 * Copyright 2025-China Telecom Research Institute.
 * All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ct.ertclib.dc.core.ui.activity

import android.os.Bundle
import androidx.lifecycle.ViewModelProvider
import com.ct.ertclib.dc.core.R
import com.ct.ertclib.dc.core.databinding.MainSettingLayoutBinding
import com.ct.ertclib.dc.core.ui.fragment.MainSettingPreferenceFragment
import com.ct.ertclib.dc.core.ui.viewmodel.SettingsViewModel

class MainSettingActivity: NoManagedBaseToolBarActivity() {

    companion object {
        private const val TAG = "MainSettingActivity"
    }

    private lateinit var binding: MainSettingLayoutBinding
    private lateinit var viewModel: SettingsViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = MainSettingLayoutBinding.inflate(layoutInflater)
        setContentView(binding.root)
        viewModel = ViewModelProvider(this)[SettingsViewModel::class.java]

        initView()
    }

    override fun getTooBarTitle(): String {
        return resources.getString(R.string.setting)
    }

    private fun initView() {
        supportFragmentManager.beginTransaction().replace(R.id.settings_layout, MainSettingPreferenceFragment()).commit()
    }
}