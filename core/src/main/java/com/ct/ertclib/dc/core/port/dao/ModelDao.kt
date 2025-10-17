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