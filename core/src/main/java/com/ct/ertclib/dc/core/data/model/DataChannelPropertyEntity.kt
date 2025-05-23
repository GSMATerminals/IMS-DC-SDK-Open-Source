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

package com.ct.ertclib.dc.core.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "dc_properties")
data class DataChannelPropertyEntity(
    @PrimaryKey(autoGenerate = true) var _id: Int = 0,
    var appId: String,
    var dcId: String? = null,
    var streamId: String? = null,
    var dcLabel: String? = null,
    var useCase: String? = null,
    var subprotocol: String? = null,
    var ordered: String? = null,
    var maxRetr: String? = null,
    var maxTime: String? = null,
    var priority: String? = null,
    var autoAcceptDcSetup: String? = null,
    var bandwidth: String? = null,
    var qosHint: String? = null
)
