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