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

package com.ct.ertclib.dc.core.port.usecase.mini

import com.ct.ertclib.dc.core.data.miniapp.PermissionUsageData

interface IPermissionUseCase {

    suspend fun getPermission(appId: String): MutableMap<String, Boolean>

    suspend fun savePermission(appId: String,  map: MutableMap<String, Boolean>, isMainProcess: Boolean, callId: String = "")

    fun checkPermissionAndRecord(appId: String, permissions: List<String>, needRecord: Boolean = true): Boolean

    fun isSystemPermissionGranted(permission: String): Boolean

    suspend fun refreshPermissionMapFromRepo(appId: String)

    fun getPermissionUsage(appId: String, timeStamp: Long): MutableList<PermissionUsageData>

    fun insertPermissionUsages(appId: String, miniPermissionNames: List<String>)
}