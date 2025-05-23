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

package com.ct.ertclib.dc.core.miniapp.ui.viewmodel


import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import android.provider.Settings
import android.view.Gravity
import android.view.LayoutInflater
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ct.ertclib.dc.core.utils.logger.Logger
import com.ct.ertclib.dc.core.R
import com.ct.ertclib.dc.core.data.miniapp.PermissionData
import com.ct.ertclib.dc.core.data.model.MiniAppInfo
import com.ct.ertclib.dc.core.miniapp.ui.adapter.PermissionRequestAdapter
import com.ct.ertclib.dc.core.port.usecase.mini.IPermissionUseCase
import com.ct.ertclib.dc.core.utils.common.PermissionUtils
import com.hjq.permissions.OnPermissionCallback
import com.hjq.permissions.XXPermissions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class MiniAppViewModel : ViewModel(), KoinComponent {

    companion object {
        private const val TAG = "MiniAppViewModel"

    }

    private val logger = Logger.getLogger(TAG)
    private val permissionUseCase: IPermissionUseCase by inject()
    private var mediaPlayer: MediaPlayer? = null

    fun startGrantPermission(
        context: Context,
        miniAppInfo: MiniAppInfo,
        onMiniAppPermissionRequest: (MutableList<PermissionData>) -> Unit,
        onPermissionDenied: () -> Unit,
        ) {
        logger.info("startGrantPermission")
        miniAppInfo.appProperties?.permissions?.let { allPermission ->
            viewModelScope.launch(Dispatchers.IO) {
                val systemPermission = PermissionUtils.convertToSystemPermissions(allPermission)
                val permissionMap = permissionUseCase.getPermission(miniAppInfo.appId)
                val allPermissionsAfterFilter = allPermission.toMutableList()
                val systemNoGrantedPermission = mutableListOf<String>()
                val miniAppNoGrantedPermission = mutableListOf<String>()
                withContext(Dispatchers.Main) {
                    //筛选出系统未授权权限
                    for (permission in systemPermission) {
                        if (!permissionUseCase.isSystemPermissionGranted(permission)) {
                            systemNoGrantedPermission.add(permission)
                            allPermissionsAfterFilter.remove(PermissionUtils.convertToSingleMiniAppPermissions(permission))
                        }
                    }
                    //筛选出除系统未授权的权限外，sdk未对小程序进行授权的权限
                    for (permission in allPermissionsAfterFilter) {
                        if ((permissionMap[permission] != true)) {
                            miniAppNoGrantedPermission.add(permission)
                        }
                    }
                    logger.info("startGrantPermission, allPermission: $allPermission, permissionMap: $permissionMap, miniAppNoGrantedPermission: $miniAppNoGrantedPermission")
                    if (systemNoGrantedPermission.isNotEmpty()) {
                        XXPermissions.with(context)
                            .permission(systemPermission)
                            .request(object : OnPermissionCallback {
                                override fun onGranted(
                                    permissions: MutableList<String>,
                                    allGranted: Boolean
                                ) {
                                    logger.info("MiniApp has all permissions: $allGranted")
                                    val permissionGrantedMap: MutableMap<String, Boolean> = mutableMapOf()
                                    permissionGrantedMap.putAll(PermissionUtils.convertToMiniAppPermissions(permissions).associateWith { true })
                                    savePermissionGrantedResults(miniAppInfo.appId, permissionGrantedMap)
                                    onMiniAppPermissionRequest.invoke(PermissionUtils.convertPermissionDataList(miniAppNoGrantedPermission))
                                }

                                override fun onDenied(
                                    deniedPermissions: MutableList<String>,
                                    doNotAskAgain: Boolean
                                ) {
                                    logger.info("MiniApp lacks the following permissions: $deniedPermissions")
                                    if (doNotAskAgain) {
                                        showPermissionGuidedDialog(context, miniAppInfo, deniedPermissions, onPermissionDenied)
                                    } else {
                                        onPermissionDenied.invoke()
                                    }
                                }
                            })
                    } else {
                        onMiniAppPermissionRequest.invoke(PermissionUtils.convertPermissionDataList(miniAppNoGrantedPermission))
                    }
                }
            }
        }
    }

    fun savePermissionGrantedResults(appId: String, map: MutableMap<String, Boolean>) {
        viewModelScope.launch(Dispatchers.IO) {
            permissionUseCase.savePermission(appId, map, isMainProcess = false)
        }
    }

    fun refreshPermission(appId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            permissionUseCase.refreshPermissionMapFromRepo(appId)
        }
    }

    private fun showPermissionGuidedDialog(context: Context, miniAppInfo: MiniAppInfo, permissionList: MutableList<String>, onPermissionDenied: () -> Unit) {
        val builder = AlertDialog.Builder(context)
        val layout = LayoutInflater.from(context).inflate(R.layout.permission_denied_tips_layout, null)
        builder.setView(layout)
        val tips = layout.findViewById<TextView>(R.id.permission_dialog_tips)
        tips.text = context.resources.getString(R.string.open_permission_tips, miniAppInfo.appName)
        val recyclerView = layout.findViewById<RecyclerView>(R.id.denied_permission_recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
        recyclerView.adapter = PermissionRequestAdapter(context, PermissionUtils.convertToSystemPermissionData(permissionList))
        val negativeText = layout.findViewById<TextView>(R.id.permission_dialog_negative_text)
        val positiveText = layout.findViewById<TextView>(R.id.permission_dialog_positive_text)
        val dialog = builder.create()
        dialog.setCanceledOnTouchOutside(false)
        val window = dialog.window
        window?.let {
            val attributes = it.attributes
            attributes.gravity = Gravity.BOTTOM
            attributes.y = context.resources.getDimensionPixelSize(R.dimen.dialog_bottom_margin)
            it.setBackgroundDrawableResource(R.drawable.dialog_window_background)
        }
        negativeText.setOnClickListener {
            onPermissionDenied.invoke()
            dialog.dismiss()
        }
        positiveText.setOnClickListener {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            intent.data = Uri.fromParts("package", context.packageName, null)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            onPermissionDenied.invoke()
            dialog.dismiss()
        }
        dialog.show()
    }

    // 同时只能有一个在播放
    fun playVoice(path: String) {
        mediaPlayer?.release()
        mediaPlayer = MediaPlayer().apply {
            setDataSource(path)
            prepareAsync()
            setOnPreparedListener { it.start() }
            setOnCompletionListener { release() }
        }
    }

    fun stopPlayVoice() {
        mediaPlayer?.release()
        mediaPlayer = null
    }
}