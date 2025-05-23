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

package com.ct.ertclib.dc.core.port.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.ct.ertclib.dc.core.data.model.MiniAppInfo

@Dao
interface MiniAppInfoDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsert(vararg miniApps: MiniAppInfo)

    @Update
    fun update(vararg miniApps: MiniAppInfo)

    @Delete
    fun delete(miniApp: MiniAppInfo)

    @Query("SELECT * FROM mini_app_info WHERE appId = :appId")
    fun getOne(appId: String): MiniAppInfo?

    @Query("SELECT * FROM mini_app_info ORDER BY lastUseTime DESC")
    fun getAll(): List<MiniAppInfo>
}