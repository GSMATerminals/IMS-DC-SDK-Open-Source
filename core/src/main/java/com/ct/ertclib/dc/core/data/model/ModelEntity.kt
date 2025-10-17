package com.ct.ertclib.dc.core.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName

@Entity(tableName = "model_table")
data class ModelEntity(
    @PrimaryKey
    @SerializedName("modelid")
    var modelId: String,
    var modelName: String,
    var modelPath: String,
    var modelVersion: String,
    var modelType: String,
    var icon: String = ""
)