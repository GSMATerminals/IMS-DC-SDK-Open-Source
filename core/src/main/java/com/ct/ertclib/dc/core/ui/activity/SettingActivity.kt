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
import com.ct.ertclib.dc.core.utils.logger.Logger
import com.ct.ertclib.dc.core.R
import com.ct.ertclib.dc.core.constants.CommonConstants.PARAMS_APP_ID
import com.ct.ertclib.dc.core.constants.CommonConstants.PARAMS_CALL_ID
import com.ct.ertclib.dc.core.constants.CommonConstants.PARAMS_VERSION_CODE
import com.ct.ertclib.dc.core.ui.fragment.SettingPreferenceFragment

class SettingActivity: BaseToolBarActivity() {

    companion object {
        private const val TAG = "SettingActivity"
    }

    private val logger = Logger.getLogger(TAG)
    var appId = ""
    var callId = ""
    var version = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.layout_setting)
        supportFragmentManager.beginTransaction().replace(R.id.settings_layout, SettingPreferenceFragment()).commit()
        intent.getStringExtra(PARAMS_APP_ID)?.let {
            appId = it
        }
        intent.getStringExtra(PARAMS_CALL_ID)?.let {
            callId = it
        }
        intent.getStringExtra(PARAMS_VERSION_CODE)?.let {
            version = it
        }
        logger.info("onCreate, appId: $appId, callId: $callId, version: $version")
    }

    override fun getTooBarTitle(): String {
        return getString(R.string.setting_preference_title)
    }
}