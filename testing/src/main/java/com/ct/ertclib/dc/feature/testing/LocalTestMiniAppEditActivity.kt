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

package com.ct.ertclib.dc.feature.testing

import android.Manifest.permission.MANAGE_EXTERNAL_STORAGE
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import com.blankj.utilcode.util.SPUtils
import com.ct.ertclib.dc.core.utils.logger.Logger
import com.ct.ertclib.dc.core.utils.common.FileUtils
import com.ct.ertclib.dc.core.data.model.MiniAppInfo
import com.ct.ertclib.dc.core.ui.activity.BaseAppCompatActivity
import com.ct.ertclib.dc.core.utils.common.Base64Utils
import com.ct.ertclib.dc.core.utils.common.JsonUtil
import com.ct.ertclib.dc.core.utils.common.ToastUtils
import com.ct.ertclib.dc.feature.testing.databinding.ActivityLocalTestMiniAppEditBinding
import com.hjq.permissions.OnPermissionCallback
import com.hjq.permissions.XXPermissions

/** * 新增或修改小程序 */
class LocalTestMiniAppEditActivity : BaseAppCompatActivity() {
    companion object {
        private const val TAG = "LocalTestMiniAppEditActivity"
    }

    private val sLogger = Logger.getLogger(TAG)
    private lateinit var binding: ActivityLocalTestMiniAppEditBinding
    private var oldAppId: String = ""
    private var oldAppPath: String = ""
    private var oldAppInfo: MiniAppInfo? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        intent.getStringExtra("appId")?.let { oldAppId = it }
        binding = ActivityLocalTestMiniAppEditBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.hide()
        init()
    }

    private fun init() {
        if (oldAppId.isNotEmpty()) {
            SPUtils.getInstance().getString("TestMiniAppList")?.let { apps ->
                var strs = apps.split(",")
                strs.forEach { item ->
                    val split = item.split("&zipPath=")
                    val temp = JsonUtil.fromJson(Base64Utils.decodeFromBase64(split[0]), MiniAppInfo::class.java)
                    if (oldAppId == temp?.appId) {
                        oldAppInfo = temp
                        oldAppPath = split[1]
                        return@forEach
                    }
                }
            }
            binding.btnDelete.text = getString(R.string.delete_btn)
        }

        if (oldAppInfo != null) {
            binding.etAppId.setText(oldAppInfo?.appId)
            binding.etAppName.setText(oldAppInfo?.appName)
            binding.etVersion.setText(oldAppInfo?.eTag)
            binding.etScene.setText(oldAppInfo?.supportScene.toString())
            binding.swAutoload.isChecked = oldAppInfo?.autoLoad == true
            binding.swPhase.isChecked = oldAppInfo?.phase == "PRECALL"
        }

        if (oldAppPath.isNotEmpty()) {
            binding.path.text = oldAppPath
        }

        binding.backIcon.setOnClickListener { onBack() }
        binding.btnDelete.setOnClickListener {
            if (oldAppId.isEmpty()) {
                onBack()
            } else {
                // ===== 编辑模式：btnDelete = "删除" =====
                // 删除操作与是否修改无关，直接确认删除
                AlertDialog.Builder(this@LocalTestMiniAppEditActivity)
                    .setTitle(R.string.confirm)
                    .setMessage(getString(R.string.delete_miniappp_tips))
                    .setPositiveButton(com.ct.ertclib.dc.core.R.string.ok_btn) { dialog, _ ->
                        dialog.dismiss()
                        // 执行删除逻辑（原代码）
                        SPUtils.getInstance().getString("TestMiniAppList")?.let { apps ->
                            var strs = apps.split(",")
                            var builder = StringBuilder()
                            strs.forEach { item ->
                                try {
                                    val split = item.split("&zipPath=")
                                    val temp = JsonUtil.fromJson(Base64Utils.decodeFromBase64(split[0]), MiniAppInfo::class.java)
                                    if (temp?.appId != oldAppId) {
                                        if (builder.isNotEmpty()) {
                                            builder.append(",")
                                        }
                                        builder.append(item)
                                    }
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }
                            SPUtils.getInstance().put("TestMiniAppList", builder.toString())
                        }
                        ToastUtils.showShortToast(this, R.string.delete_successful)
                        finish()
                    }
                    .setNegativeButton(com.ct.ertclib.dc.core.R.string.cancel_btn) { dialog, _ ->
                        dialog.dismiss()
                    }
                    .show()
            }
        }

        binding.btnSave.setOnClickListener {
            if (binding.etAppId.text.isNullOrEmpty() || binding.etAppName.text.isNullOrEmpty() ||
                binding.etVersion.text.isNullOrEmpty() || binding.etScene.text.isNullOrEmpty() ||
                binding.path.text.isNullOrEmpty()
            ) {
                ToastUtils.showShortToast(this, R.string.edit_all_info)
                return@setOnClickListener
            }

            val miniAppInfo = MiniAppInfo(
                appId = binding.etAppId.text.toString(),
                appName = binding.etAppName.text.toString(),
                appIcon = null,
                autoLaunch = false,
                autoLoad = binding.swAutoload.isChecked == true,
                callId = "",
                eTag = binding.etVersion.text.toString(),
                ifWorkWithoutPeerDc = false,
                isOutgoingCall = false,
                myNumber = null,
                path = null,
                phase = if (binding.swPhase.isChecked == true) "PRECALL" else "INCALL",
                qosHint = "loss=0.0002;latency=600",
                remoteNumber = null,
                slotId = 0,
                supportScene = binding.etScene.text.toString().toInt(),
                isStartAfterInstalled = true,
                lastUseTime = 0
            )

            var builder = StringBuilder()
            SPUtils.getInstance().getString("TestMiniAppList")?.let { apps ->
                var strs = apps.split(",")
                strs.forEach { item ->
                    try {
                        val split = item.split("&zipPath=")
                        val temp = JsonUtil.fromJson(
                            Base64Utils.decodeFromBase64(split[0]),
                            MiniAppInfo::class.java
                        )
                        if (miniAppInfo.appId != temp?.appId && miniAppInfo.phase == temp?.phase && miniAppInfo.autoLoad && temp.autoLoad) {
                            ToastUtils.showShortToast(
                                this,
                                "${miniAppInfo.phase}${getString(R.string.exist_autoload)}：${temp.appName}"
                            )
                            return@setOnClickListener
                        }
                        if (oldAppId != temp?.appId || miniAppInfo.appId != temp.appId) {
                            if (builder.isNotEmpty()) {
                                builder.append(",")
                            }
                            builder.append(item)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
            if (builder.isNotEmpty()) {
                builder.insert(0, ",")
            }
            builder.insert(0, Base64Utils.encodeToBase64(JsonUtil.toJson(miniAppInfo)) + "&zipPath=" + binding.path.text.toString())
            SPUtils.getInstance().put("TestMiniAppList", builder.toString())
            ToastUtils.showShortToast(this, R.string.saved_ok)
            finish()
        }

        binding.btnSelect.setOnClickListener {
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
                            ToastUtils.showShortToast(this@LocalTestMiniAppEditActivity, R.string.miss_some_permissions)
                        }
                    }

                    override fun onDenied(permissions: MutableList<String>, never: Boolean) {
                        if (never) {
                            ToastUtils.showShortToast(this@LocalTestMiniAppEditActivity, R.string.miss_permission_forever)
                            XXPermissions.startPermissionActivity(this@LocalTestMiniAppEditActivity, permissions)
                        } else {
                            ToastUtils.showShortToast(this@LocalTestMiniAppEditActivity, R.string.got_permissions)
                        }
                    }
                })
        }
    }

    private fun selectMiniApp() {
        val intent = Intent("android.intent.action.GET_CONTENT")
        intent.type = "application/zip"
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        startActivityForResult(Intent.createChooser(intent, getString(R.string.choose_package)), 1)
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        sLogger.info("requestCode:$requestCode")
        if (requestCode == 1) {
            val uri = data?.data
            sLogger.info("uri:$uri")
            if (uri != null) {
                binding.path.text = FileUtils.getPath(this, uri)?.let { sLogger.info("path:$it"); it }
            }
        }
    }

    // ====== 新增：判断是否已修改 ======
    private fun isModified(): Boolean {
        val currentAppId = binding.etAppId.text.toString()
        val currentAppName = binding.etAppName.text.toString()
        val currentVersion = binding.etVersion.text.toString()
        val currentScene = binding.etScene.text.toString()
        val currentPath = binding.path.text.toString()
        val currentAutoload = binding.swAutoload.isChecked
        val currentPhase = binding.swPhase.isChecked // true 表示 PRECALL

        // 如果是新增（oldAppInfo == null），只要任一字段非空即视为已修改
        if (oldAppInfo == null) {
            return currentAppId.isNotEmpty() ||
                    currentAppName.isNotEmpty() ||
                    currentVersion.isNotEmpty() ||
                    currentScene.isNotEmpty() ||
                    currentPath.isNotEmpty() ||
                    currentAutoload ||
                    currentPhase
        }

        // 如果是编辑，逐项对比
        return currentAppId != oldAppInfo?.appId ||
                currentAppName != oldAppInfo?.appName ||
                currentVersion != oldAppInfo?.eTag ||
                currentScene != oldAppInfo?.supportScene.toString() ||
                currentPath != oldAppPath ||
                currentAutoload != (oldAppInfo?.autoLoad == true) ||
                currentPhase != (oldAppInfo?.phase == "PRECALL")
    }

    private fun onBack() {
        // ✅ 最小修改：仅当有修改时才弹窗
        if (isModified()) {
            AlertDialog.Builder(this@LocalTestMiniAppEditActivity)
                .setTitle(R.string.confirm)
                .setMessage(R.string.discard_edit)
                .setPositiveButton(com.ct.ertclib.dc.core.R.string.ok_btn) { dialog, which ->
                    dialog.dismiss()
                    finish()
                }
                .setNegativeButton(com.ct.ertclib.dc.core.R.string.cancel_btn) { dialog, which ->
                    dialog.dismiss()
                }
                .show()
        } else {
            finish()
        }
    }

    override fun onBackPressed() {
        onBack()
    }
}