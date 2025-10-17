package com.ct.ertclib.dc.core.port.manager

import com.ct.ertclib.dc.core.data.model.ModelEntity


interface IModelManager {

    fun insertOrUpdate(modelEntity: ModelEntity)

    fun delete(modelEntity: ModelEntity)

    fun getAllModel(): List<ModelEntity>

    fun getModelById(modelId: String): ModelEntity?
}