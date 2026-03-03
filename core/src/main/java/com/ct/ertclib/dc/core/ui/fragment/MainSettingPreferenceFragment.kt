package com.ct.ertclib.dc.core.ui.fragment

import android.os.Bundle
import androidx.lifecycle.ViewModelProvider
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.ct.ertclib.dc.core.R
import com.ct.ertclib.dc.core.common.NewCallAppSdkInterface.PERMISSION_TYPE_IN_APP
import com.ct.ertclib.dc.core.common.sdkpermission.SDKPermissionUtils
import com.ct.ertclib.dc.core.ui.viewmodel.SettingsViewModel
import com.ct.ertclib.dc.core.ui.widget.SettingPreference
import com.ct.ertclib.dc.core.ui.widget.SwitchPreference
import com.ct.ertclib.dc.core.ui.widget.VersionPreference
import com.ct.ertclib.dc.core.utils.common.FlavorUtils
import com.ct.ertclib.dc.core.utils.common.LogUtils
import com.ct.ertclib.dc.core.utils.common.PkgUtils
import com.ct.ertclib.dc.core.utils.extension.startLocalTestActivity

class MainSettingPreferenceFragment : PreferenceFragmentCompat(), Preference.OnPreferenceClickListener {

    companion object {
        private const val TAG = "MainSettingPreferenceFragment"
        private const val KEY_OPEN_NEW_CALL = "new_call_open_switch_preference"
        private const val KEY_OPEN_FOLLOW = "follow_open_switch_preference"
        private const val KEY_DEBUG = "debug_preference"
        private const val KEY_PRIVACY = "privacy_preference"
        private const val KEY_USER = "user_preference"
        private const val KEY_VERSION = "permission_version"
    }

    private var newCallPreference: SwitchPreference? = null
    private var followPreference: SwitchPreference? = null
    private var debugPreference: SettingPreference? = null
    private var privacyPreference: SettingPreference? = null
    private var userPreference: SettingPreference? = null
    private var versionPreference: VersionPreference? = null

    private lateinit var viewModel: SettingsViewModel

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.main_setting_fragment, rootKey)
        newCallPreference = findPreference(KEY_OPEN_NEW_CALL)
        followPreference = findPreference(KEY_OPEN_FOLLOW)
        debugPreference = findPreference(KEY_DEBUG)
        privacyPreference = findPreference(KEY_PRIVACY)
        userPreference = findPreference(KEY_USER)
        versionPreference = findPreference(KEY_VERSION)

        debugPreference?.let { it.onPreferenceClickListener = this }
        privacyPreference?.let { it.onPreferenceClickListener = this }
        userPreference?.let { it.onPreferenceClickListener = this }

        viewModel = ViewModelProvider(this)[SettingsViewModel::class.java]

        if (FlavorUtils.getChannelName() != FlavorUtils.CHANNEL_LOCAL) {
            debugPreference?.isVisible = false
        }

        activity?.let { activity ->
            versionPreference?.setVersion("V ${PkgUtils.getAppVersion(activity)}")
        }

        newCallPreference?.setSwitchChangeListener { isChecked ->
            LogUtils.debug(TAG, "setSwitchChangeListener status: $isChecked")
            if (!isChecked) {
                SDKPermissionUtils.setNewCallEnable(false)
                newCallPreference?.updateSummary(getString(R.string.close_switch_tips))
            } else {
                activity?.let { activity ->
                    viewModel.checkAndRequestPermission(activity, PERMISSION_TYPE_IN_APP, ::updateView, ::updateView)
                }
            }
        }

        followPreference?.setSwitchChangeListener { isChecked ->
            SDKPermissionUtils.setFellowDialer(isChecked)
        }

    }

    override fun onResume() {
        super.onResume()
        updateView()
    }

    override fun onPreferenceClick(preference: Preference): Boolean {
        when (preference.key) {
            KEY_DEBUG -> {
                LogUtils.info(TAG, "onPreferenceClick KEY_DEBUG")
                activity?.startLocalTestActivity()
                return true
            }
            KEY_PRIVACY -> {
                LogUtils.info(TAG, "onPreferenceClick KEY_PRIVACY")
                activity?.let {
                    SDKPermissionUtils.startPrivacyActivity(it)
                }
                return true
            }
            KEY_USER -> {
                LogUtils.info(TAG, "onPreferenceClick KEY_USER")
                activity?.let {
                    SDKPermissionUtils.startUserServiceActivity(it)
                }
                return true
            }
        }
        return false
    }

    private fun updateView() {
        LogUtils.debug(TAG, "updateView status: ${SDKPermissionUtils.hasAllPermissions(requireActivity())}")
        activity?.let { activity ->
            newCallPreference?.updateSwitchStatus(SDKPermissionUtils.hasAllPermissions(activity))
            followPreference?.updateSwitchStatus(SDKPermissionUtils.isFellowDialer())
            if (newCallPreference?.status == true) {
                newCallPreference?.updateSummary(getString(R.string.open_switch_tips))
            } else {
                newCallPreference?.updateSummary(getString(R.string.close_switch_tips))
            }
        }
    }
}