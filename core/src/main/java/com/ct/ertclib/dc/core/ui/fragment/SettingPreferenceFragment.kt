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

package com.ct.ertclib.dc.core.ui.fragment



import android.content.Intent
import android.os.Bundle
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.ct.ertclib.dc.core.utils.logger.Logger
import com.ct.ertclib.dc.core.R
import com.ct.ertclib.dc.core.constants.CommonConstants.PARAMS_APP_ID
import com.ct.ertclib.dc.core.constants.CommonConstants.PARAMS_CALL_ID
import com.ct.ertclib.dc.core.ui.activity.PermissionSettingActivity
import com.ct.ertclib.dc.core.ui.activity.SettingActivity
import com.ct.ertclib.dc.core.ui.widget.SettingPreference
import com.ct.ertclib.dc.core.ui.widget.VersionPreference

class SettingPreferenceFragment : PreferenceFragmentCompat(), Preference.OnPreferenceClickListener {

    companion object {
        private const val TAG = "SettingPreferenceFragment"
        private const val KEY_PERMISSION = "permission_setting"
        private const val KEY_VERSION = "permission_version"
    }

    private val logger = Logger.getLogger(TAG)
    private var permissionPreference: SettingPreference? = null
    private var versionPreference: VersionPreference? = null

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.setting_preference, rootKey)
        permissionPreference = findPreference(KEY_PERMISSION)
        versionPreference = findPreference(KEY_VERSION)
        permissionPreference?.let {
            it.onPreferenceClickListener = this
        }
        activity?.let { activity ->
            (activity as? SettingActivity)?.version?.let { version ->
                versionPreference?.setVersion(version)
            }
        }
        view?.setBackgroundColor(resources.getColor(R.color.white_alpha_90))
    }

    override fun onPreferenceClick(preference: Preference): Boolean {
        when (preference.key) {
            KEY_PERMISSION -> {
                logger.info("onPreferenceClick KEY_PERMISSION")
                activity?.let {
                    val intent = Intent(it, PermissionSettingActivity::class.java).apply {
                        (activity as? SettingActivity)?.appId?.let { appId ->
                            putExtra(PARAMS_APP_ID, appId)
                        }
                        (activity as? SettingActivity)?.callId?.let { callId ->
                            putExtra(PARAMS_CALL_ID, callId)
                        }
                    }
                    it.startActivity(intent)
                }
                return true
            }
        }
        return false
    }
}