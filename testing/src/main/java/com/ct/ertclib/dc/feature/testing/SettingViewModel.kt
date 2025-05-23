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

package com.ct.ertclib.dc.feature.testing

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.ct.ertclib.dc.core.common.NewCallDatabase
import com.ct.ertclib.dc.core.port.dao.DcPropertiesDao
import com.ct.ertclib.dc.core.data.model.DataChannelPropertyEntity
import com.ct.ertclib.dc.core.data.miniapp.DataChannelProperty

class SettingViewModel(application: Application) : AndroidViewModel(application) {
    private val propertyDao: DcPropertiesDao by lazy {
        NewCallDatabase.getInstance().dcPropertiesDao()
    }

    val isConnected = MutableLiveData(false)

    fun insertProperties(dcproperties: DataChannelProperty?) {
        if (dcproperties == null) return
        val toInsert = dcproperties.dataChannelAppList.map {
            DataChannelPropertyEntity(0, it.appId).apply {
                val dataChannel = it.dataChannelList[0]
                dcId = dataChannel.dcId
                streamId = dataChannel.streamId
                dcLabel = dataChannel.dcLabel
                useCase = dataChannel.useCase
                subprotocol = dataChannel.subProtocol
                ordered = dataChannel.ordered
                maxRetr = dataChannel.maxRetr
                maxTime = dataChannel.maxTime
                priority = dataChannel.priority
                autoAcceptDcSetup = dataChannel.autoAcceptDcSetup
                bandwidth = dataChannel.bandwidth
                qosHint = dataChannel.qosHint
            }
        }
        propertyDao.insertProperties(toInsert)
    }

    fun clearProperties() {
        propertyDao.deleteAll()
    }
}