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

class RawBytePack : Writable {
    companion object {
        const val TYPE_RAW = 1511

        fun buildRawPack(data: ByteArray): RawBytePack {
            val rawBytePack = RawBytePack()
            rawBytePack.byteArray = data.copyOf(data.size)
            return rawBytePack
        }
    }

    lateinit var byteArray: ByteArray

    @Keep
    constructor()

    constructor(bytes: ByteArray) {
        val input = DataInputStreamBuffer(bytes)
        readFields(input)
        input.close()
    }

    override fun write(var1: DataOutput?) {
        var1?.apply {
            writeInt(byteArray.size)
            write(byteArray)
        }
    }

    override fun readFields(var1: DataInput?) {
        var1?.apply {
            val len = readInt()
            byteArray = ByteArray(len)
            for (i in 0 until len) {
                byteArray[i] = readByte()
            }
        }
    }
}