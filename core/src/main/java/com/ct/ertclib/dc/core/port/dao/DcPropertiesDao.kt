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
import com.ct.ertclib.dc.core.data.model.DataChannelPropertyEntity

@Dao
interface DcPropertiesDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertProperty(dcProperty: DataChannelPropertyEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertProperties(dcProperties: List<DataChannelPropertyEntity>)

    @Query("SELECT * FROM dc_properties")
    fun queryAll(): List<DataChannelPropertyEntity>

    @Query("SELECT * FROM dc_properties WHERE appId = :appId LIMIT 1")
    fun queryByAppId(appId: String): DataChannelPropertyEntity

    @Delete
    fun deleteProperty(dcProperty: DataChannelPropertyEntity)

    @Query("DELETE FROM dc_properties WHERE appId = :appId")
    fun deletePropertyByAppId(appId: String)

    @Query("DELETE FROM dc_properties")
    fun deleteAll()
}