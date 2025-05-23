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

package com.ct.ertclib.dc.core.miniapp.db

import com.ct.ertclib.dc.core.common.NewCallDatabase
import com.ct.ertclib.dc.core.data.model.PermissionModel
import com.ct.ertclib.dc.core.port.miniapp.IPermissionDbRepo
import kotlinx.coroutines.flow.Flow

class PermissionDbRepo: IPermissionDbRepo {

    private val permissionInfoDao by lazy { NewCallDatabase.getInstance().permissionDao() }

    override fun insertOrUpdate(permissionModel: PermissionModel) {
        permissionInfoDao.insertOrUpdate(permissionModel)
    }

    override fun getAll(): Flow<List<PermissionModel>> {
        return permissionInfoDao.getAll()
    }

    override fun getPermissionModelById(appId: String): PermissionModel? {
        return permissionInfoDao.getOne(appId)
    }
}