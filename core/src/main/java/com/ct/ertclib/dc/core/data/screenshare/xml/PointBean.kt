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

package com.ct.ertclib.dc.core.data.screenshare.xml

import java.nio.ByteBuffer
import java.nio.ByteOrder

data class PointBean(
    var x: Float,
    var y: Float
) {
    fun getByteArray(): ByteArray {
        val buffer: ByteBuffer = ByteBuffer.allocate(Float.SIZE_BYTES * 2)
        buffer.order(ByteOrder.BIG_ENDIAN)
        buffer.putFloat(x)
        buffer.putFloat(y)
        return buffer.array()
    }

    companion object {
        fun fromByteArray(byteArray: ByteArray): PointBean {
            val buffer: ByteBuffer = ByteBuffer.allocate(byteArray.size)
            buffer.order(ByteOrder.BIG_ENDIAN)
            buffer.put(byteArray)
            //从0开始
            buffer.rewind()
            val point = PointBean(0f, 0f)
            point.x = buffer.float
            point.y = buffer.float
            return point
        }

        fun fromByteArray2List(byteArray: ByteArray): MutableList<PointBean> {
            val buffer: ByteBuffer = ByteBuffer.allocate(byteArray.size)
            buffer.order(ByteOrder.BIG_ENDIAN)
            buffer.put(byteArray)
            val count = buffer.position() / Float.SIZE_BYTES

            val result: MutableList<PointBean> = ArrayList()
            buffer.rewind()
            for (i in 0 until count step 2) {
                val point = PointBean(0f, 0f)
                point.x = buffer.float
                point.y = buffer.float
                result.add(point)
            }
            return result
        }
    }
}