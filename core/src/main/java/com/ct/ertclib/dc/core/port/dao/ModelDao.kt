/*
 * Copyright 2025-China Telecom Research Institute.
 * All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ct.ertclib.dc.core.port.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ct.ertclib.dc.core.data.model.ModelEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ModelDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertOrUpdate(vararg modelEntity: ModelEntity)

    @Delete
    fun delete(modelEntity: ModelEntity)

    @Query("SELECT * FROM model_table WHERE modelId = :modelId")
    fun getModel(modelId: String): ModelEntity?

    @Query("SELECT * FROM model_table")
    fun getAll(): List<ModelEntity>
}