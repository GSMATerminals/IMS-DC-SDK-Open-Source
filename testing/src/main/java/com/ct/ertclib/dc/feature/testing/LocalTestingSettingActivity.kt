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

package com.ct.ertclib.dc.feature.testing

import android.Manifest.permission.MANAGE_EXTERNAL_STORAGE
import android.content.Intent
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.AdapterView.OnItemSelectedListener
import android.widget.ArrayAdapter
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.blankj.utilcode.util.SPUtils
import com.ct.ertclib.dc.core.utils.logger.Logger
import com.ct.ertclib.dc.core.utils.common.FileUtils
import com.ct.ertclib.dc.core.data.miniapp.DataChannelProperty
import com.ct.ertclib.dc.core.data.miniapp.DataChannel
import com.ct.ertclib.dc.core.data.miniapp.DataChannelApp
import com.ct.ertclib.dc.core.data.miniapp.DataChannelAppInfo
import com.ct.ertclib.dc.core.ui.activity.BaseAppCompatActivity
import com.ct.ertclib.dc.core.utils.common.ToastUtils
import com.ct.ertclib.dc.core.utils.common.XmlUtils
import com.ct.ertclib.dc.feature.testing.databinding.ActivityLocalTestingSettingBinding
import com.hjq.permissions.OnPermissionCallback
import com.hjq.permissions.XXPermissions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter


class LocalTestingSettingActivity : BaseAppCompatActivity() {

    companion object {
        private const val TAG = "SettingActivity"
    }

    private val sLogger = Logger.getLogger(TAG)
    private lateinit var mBinding: ActivityLocalTestingSettingBinding
    private lateinit var mViewModel: SettingViewModel
    private lateinit var mSPUtils: SPUtils
    private var dcAction = ""
    private var dcPackage = ""
    private var dcClass = ""
    private var isServer = false
    private var ip = "127.0.0.1"
    private var port = 9988
    private var isSkipGetList = false
    private var isSkipCheckMccMnc = false
    private var mockTest = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mBinding = ActivityLocalTestingSettingBinding.inflate(layoutInflater)
        setContentView(mBinding.root)
        supportActionBar?.hide()
        mViewModel = ViewModelProvider(this)[SettingViewModel::class.java]
        init()
    }

    private fun init() {
        mSPUtils = SPUtils.getInstance()
        mockTest = mSPUtils.getBoolean("mock_socket", false)
        mBinding.swMockTest.isChecked = mockTest
        dcAction = mSPUtils.getString("dcAction")
        dcPackage = mSPUtils.getString("dcPackage")
        dcClass = mSPUtils.getString("dcClass")
        sLogger.info("getDCInfo: action = $dcAction; package = $dcPackage; class = $dcClass")
        mBinding.etServerAction.setText(dcAction)
        mBinding.etServerPackage.setText(dcPackage)
        mBinding.etServerClass.setText(dcClass)

        isServer = mSPUtils.getBoolean("isServer", false)
//        sLogger.info("getIp:" + getIp())
//        ip = mSPUtils.getString("ip", getIp())
        port = mSPUtils.getInt("port", 9988)
        mBinding.swServer.isChecked = isServer
        mBinding.etIp.setText(ip)
        mBinding.etPort.setText(port.toString())

        isSkipGetList = mSPUtils.getBoolean("skip_get_list", false)
        sLogger.info("init isSkipGetList:$isSkipGetList")
        mBinding.skipGetList.isChecked = isSkipGetList
        isSkipCheckMccMnc = mSPUtils.getBoolean("skip_check_mcc_mnc", false)
        mBinding.skipCheckMccMnc.isChecked = isSkipCheckMccMnc

        mBinding.dcPath.text = mSPUtils.getString("dc_properties_path", "")
        mBinding.dcTime.text = "上一次修改时间：" + mSPUtils.getString("dc_properties_time", "")

        mBinding.sendOkTimeout.isChecked = mSPUtils.getBoolean("isTimeOut")

        val timeOut = mSPUtils.getString("timeOut", "")
        if (timeOut.isNotBlank() || timeOut.isNotEmpty()) {
            mBinding.timeInput.setText(timeOut)
        }
        mBinding.multiDc.isChecked = mSPUtils.getBoolean("multiDc")

        mBinding.btnSave.setOnClickListener {
            mockTest = mBinding.swMockTest.isChecked
            isServer = mBinding.swServer.isChecked
            ip = mBinding.etIp.text.toString()
            port = mBinding.etPort.text.toString().toInt()
            dcAction = mBinding.etServerAction.text.toString()
            dcPackage = mBinding.etServerPackage.text.toString()
            dcClass = mBinding.etServerClass.text.toString()

            mSPUtils.put("mock_socket", mockTest)

            mSPUtils.put("dcAction", dcAction)
            mSPUtils.put("dcPackage", dcPackage)
            mSPUtils.put("dcClass", dcClass)

            mSPUtils.put("isServer", isServer)
            mSPUtils.put("ip", ip)
            mSPUtils.put("port", port)

            mSPUtils.put("appPath", mBinding.path.text.toString())
            mSPUtils.put("appId", mBinding.etAppId.text.toString())
            mSPUtils.put("appName", mBinding.etAppName.text.toString())
            mSPUtils.put("eTag", "${System.currentTimeMillis()}")

            sLogger.info("save isSkipGetList:${mBinding.skipGetList.isChecked} ")
            mSPUtils.put("skip_get_list", mBinding.skipGetList.isChecked)
            mSPUtils.put("skip_check_mcc_mnc", mBinding.skipCheckMccMnc.isChecked)

            mSPUtils.put("timeOut", mBinding.timeInput.text.toString())
            mSPUtils.put("isTimeOut", mBinding.sendOkTimeout.isChecked)
            mSPUtils.put("multiDc", mBinding.multiDc.isChecked)
            ToastUtils.showShortToast(this, "保存成功")
        }

        mBinding.btnSelect.setOnClickListener {
            if (XXPermissions.isGranted(this, MANAGE_EXTERNAL_STORAGE)) {
                selectMiniApp()
                return@setOnClickListener
            }
            XXPermissions.with(this)
                .permission(MANAGE_EXTERNAL_STORAGE)
                .request(object : OnPermissionCallback {
                    override fun onGranted(permissions: MutableList<String>, all: Boolean) {
                        if (all) {
                            selectMiniApp()
                        } else {
                            ToastUtils.showShortToast(this@LocalTestingSettingActivity, "获取权限成功，部分权限未正常授予")
                        }
                    }

                    override fun onDenied(permissions: MutableList<String>, never: Boolean) {
                        if (never) {
                            ToastUtils.showShortToast(this@LocalTestingSettingActivity, "被永久拒绝授权，请手动授予权限")
                            // 如果是被永久拒绝就跳转到应用权限系统设置页面
                            XXPermissions.startPermissionActivity(this@LocalTestingSettingActivity, permissions)
                        } else {
                            ToastUtils.showShortToast(this@LocalTestingSettingActivity, "获取权限失败")
                        }
                    }


                })
        }
        mBinding.btnSelectDcProp.setOnClickListener {
            if (XXPermissions.isGranted(this, MANAGE_EXTERNAL_STORAGE)) {
                selectDcProp()
                return@setOnClickListener
            }
            XXPermissions.with(this)
                .permission(MANAGE_EXTERNAL_STORAGE)
                .request(object : OnPermissionCallback {
                    override fun onGranted(permissions: MutableList<String>, all: Boolean) {
                        if (all) {
                            selectDcProp()
                        } else {
                            ToastUtils.showShortToast(this@LocalTestingSettingActivity, "获取权限成功，部分权限未正常授予")
                        }
                    }

                    override fun onDenied(permissions: MutableList<String>, never: Boolean) {
                        if (never) {
                            ToastUtils.showShortToast(this@LocalTestingSettingActivity, "被永久拒绝授权，请手动授予权限")
                            // 如果是被永久拒绝就跳转到应用权限系统设置页面
                            XXPermissions.startPermissionActivity(this@LocalTestingSettingActivity, permissions)
                        } else {
                            ToastUtils.showShortToast(this@LocalTestingSettingActivity, "获取权限失败")
                        }
                    }


                })
        }

        mBinding.btnClearDcProp.setOnClickListener {
            clearDcProp()
        }

        val spinnerChoices = arrayOf("厂商", "DEFAULT", "OPPO", "VIVO", "ZTE", "XIAOMI","HONOR","SAMSUNG")
        val spinnerAdapter =
            ArrayAdapter(this, R.layout.vendor_spinner_item, R.id.vendor_name, spinnerChoices)
        mBinding.spinner.onItemSelectedListener = object : OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                when (position) {
                    0 -> {

                    }
                    //DEFAULT
                    1 -> {
                        mBinding.etServerPackage.setText(this@LocalTestingSettingActivity.baseContext.packageName)
                        mBinding.etServerClass.setText("com.ct.ertclib.dc.feature.testing.TestImsDataChannelService")
                        mBinding.etServerAction.setText("com.newcalllib.datachannel.V1_0.ImsDataChannelService")
                    }
                    //OPPO
                    2 -> {
                        mBinding.etServerPackage.setText("com.cmcc.dcservice")
                        mBinding.etServerClass.setText("com.cmcc.dcservice.service.api.ImsDataChannelService")
                        mBinding.etServerAction.setText("com.newcalllib.datachannel.V1_0.ImsDataChannelService")
                    }
                    //VIVO
                    3 -> {
                        mBinding.etServerPackage.setText("com.dcservice")
                        mBinding.etServerClass.setText("com.dcservice.DataChannelService")
                        mBinding.etServerAction.setText("com.newcalllib.datachannel.V1_0.ImsDataChannelService")
                    }
                    //ZTE
                    4 -> {
                        mBinding.etServerPackage.setText("com.spreadtrum.ims")
                        mBinding.etServerClass.setText("com.spreadtrum.ims.datachannel.ImsDataChannelService")
                        mBinding.etServerAction.setText("com.newcalllib.datachannel.V1_0.ImsDataChannelService")
                    }
                    //XIAOMI
                    5 -> {
                        mBinding.etServerPackage.setText("com.android.phone")
                        mBinding.etServerClass.setText("com.android.phone.dcservice.MiuiDcService")
                        mBinding.etServerAction.setText("com.newcalllib.datachannel.V1_0.ImsDataChannelService")
                    }
                    //HONOR
                    6 -> {
                        mBinding.etServerPackage.setText("vendor.qti.imsdatachannel")
                        mBinding.etServerClass.setText("vendor.qti.imsdatachannel.HnImsDataChannelService")
                        mBinding.etServerAction.setText("com.newcalllib.datachannel.V1_0.ImsDataChannelService")
                    }
                    //SAMSUNG
                    7 -> {
                        mBinding.etServerPackage.setText("com.samsung.android.imsdcservice")
                        mBinding.etServerClass.setText("com.samsung.android.imsdcservice.CtcImsDataChannelService")
                        mBinding.etServerAction.setText("com.newcalllib.datachannel.V1_0.ImsDataChannelServiceCtc")
                    }
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {

            }
        }
        mBinding.spinner.adapter = spinnerAdapter
        mBinding.spinner.setSelection(0)
    }

    private fun clearDcProp() {
        lifecycleScope.launch(Dispatchers.IO) {
            mViewModel.clearProperties()
            launch(Dispatchers.Main) {
                ToastUtils.showShortToast(this@LocalTestingSettingActivity, "清除完成")
                mBinding.dcPath.text = ""
                mBinding.dcTime.text = ""
                mSPUtils.put("dc_properties_path", "")
                mSPUtils.put("dc_properties_time", "")
            }
        }
    }

    private fun selectMiniApp() {
        val intent = Intent("android.intent.action.GET_CONTENT")
        intent.type = "application/zip"
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        startActivityForResult(Intent.createChooser(intent, "选择小程序安装包"), 1)
    }

    private fun selectDcProp() {
        val intent = Intent("android.intent.action.GET_CONTENT")
        intent.type = "text/xml"
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        startActivityForResult(Intent.createChooser(intent, "选择DC配置文件"), 2)
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        sLogger.info("requestCode:$requestCode")
        if (requestCode == 1) {
            val uri = data?.data
            sLogger.info("uri:$uri")
            if (uri == null) mBinding.path.text = "选择路径为空" else {
                mBinding.path.text = FileUtils.getPath(this, uri)?.let {
                    sLogger.info("path:$it")
                    it
                } ?: "获取到的路径为空"
            }
        } else if (requestCode == 2) {
            val uri = data?.data
            sLogger.info("uri:$uri")
            if (uri == null) mBinding.dcPath.text = "选择路径为空"
            else {
                mBinding.dcPath.text = FileUtils.getPath(this, uri)?.let {
                    sLogger.info("path:$it")
                    it
                } ?: "获取到的路径为空"
                parseDcProp(mBinding.dcPath.text.toString())
            }
        }
    }

    private fun getIp() =
        intToIp((applicationContext.getSystemService(WIFI_SERVICE) as WifiManager).connectionInfo.ipAddress)

    private fun intToIp(ip: Int) =
        "${(ip and 0xFF)}.${(ip shr 8 and 0xFF)}.${(ip shr 16 and 0xFF)}.${(ip shr 24 and 0xFF)}"

    private fun parseDcProp(path: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            val res = FileUtils.readTextFromFile(File(path))
            try {
                val classes = arrayOf<Class<*>>(
                    DataChannelProperty::class.java,
                    DataChannelAppInfo::class.java,
                    DataChannelApp::class.java,
                    DataChannel::class.java
                )
                val parseXml = XmlUtils.parseXml(res, classes, DataChannelProperty::class.java)
                mViewModel.insertProperties(parseXml)
                launch(Dispatchers.Main) {
                    ToastUtils.showShortToast(this@LocalTestingSettingActivity, "添加完成")
                    mSPUtils.put("dc_properties_path", path)
                    val currentDateTime = LocalDateTime.now()
                    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                    val formattedDateTime = currentDateTime.format(formatter)
                    mSPUtils.put("dc_properties_time", formattedDateTime)
                    mBinding.dcTime.text = "上一次修改时间：$formattedDateTime"
                }
            } catch (e: Exception) {
                sLogger.error("parseDcProp Error!" + e.message, e)
                ToastUtils.showShortToast(this@LocalTestingSettingActivity, "添加异常")
            }
        }
    }
}