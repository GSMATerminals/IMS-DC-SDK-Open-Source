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

package com.ct.ertclib.dc.core.data.miniapp

data class CreateAdcParams(
    val slotId: Int,
    val callId: String,
    val remoteNumber: String?,
    val labels: Array<String>,
    val description: String

) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as CreateAdcParams

        if (slotId != other.slotId) return false
        if (callId != other.callId) return false
        if (remoteNumber != other.remoteNumber) return false
        if (!labels.contentEquals(other.labels)) return false
        if (description != other.description) return false

        return true
    }

    override fun hashCode(): Int {
        var result = slotId
        result = 31 * result + callId.hashCode()
        result = 31 * result + (remoteNumber?.hashCode() ?: 0)
        result = 31 * result + labels.contentHashCode()
        result = 31 * result + description.hashCode()
        return result
    }
}