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

package com.ct.ertclib.dc.feature.testing.socket

import androidx.annotation.Keep
import java.io.DataInput
import java.io.DataOutput

class SketchPack : Writable {
    companion object {
        const val TYPE_SKETCH = 2511
    }

    // xml数据
    lateinit var byteArray: ByteArray

    @Keep
    constructor() {

    }

    constructor(xml: String) {
        byteArray = xml.toByteArray(Charsets.UTF_8)
    }

    constructor(bytes: ByteArray) {
        val input = DataInputStreamBuffer(bytes)
        readFields(input)
        input.close()
    }

    override fun write(var1: DataOutput?) {
        var1?.apply {
            val len = byteArray.size
            writeInt(len)
            write(byteArray, 0, len)
        }
    }

    override fun readFields(var1: DataInput?) {
        var1?.apply {
            val len = readInt()
            val bytes = ByteArray(len)
            for (i in 0 until len) {
                bytes[i] = readByte()
            }
            byteArray = bytes
        }
    }
}