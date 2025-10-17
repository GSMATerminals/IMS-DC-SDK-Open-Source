package com.ct.ertclib.dc.core.manager.common

import com.ct.ertclib.dc.core.common.NewCallDatabase
import com.ct.ertclib.dc.core.data.model.ModelEntity
import com.ct.ertclib.dc.core.port.manager.IModelManager

class ModelManager: IModelManager {

    private val modelDao by lazy { NewCallDatabase.getInstance().modelDao() }

    override fun insertOrUpdate(modelEntity: ModelEntity) {
        modelDao.insertOrUpdate(modelEntity)
    }

    override fun delete(modelEntity: ModelEntity) {
        modelDao.delete(modelEntity)
    }

    override fun getAllModel(): List<ModelEntity> {
        return modelDao.getAll()
    }

    override fun getModelById(modelId: String): ModelEntity? {
        return modelDao.getModel(modelId)
    }
}