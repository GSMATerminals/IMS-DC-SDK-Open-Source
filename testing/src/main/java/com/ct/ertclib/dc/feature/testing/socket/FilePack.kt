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
import java.util.Arrays

class FilePack : Writable {
    companion object {
        const val TYPE_FILE = 3511
    }

    lateinit var byteArray: ByteArray

    /**
     * 0 头
     * 1 内容
     * 2 结束
     */
    var type: Int = 0
    var seq: Int = 0
    var fileLength: Long = 0

    @Keep
    constructor() {

    }

    constructor(bytes: ByteArray) {
        val input = DataInputStreamBuffer(bytes)
        readFields(input)
        input.close()
    }

    constructor(byteArray: ByteArray, type: Int, fileLength: Long, seq: Int) {
        this.byteArray = Arrays.copyOf(byteArray, byteArray.size)
        this.type = type
        this.fileLength = fileLength
        this.seq = seq
    }


    override fun write(var1: DataOutput?) {
        var1?.apply {
            writeInt(type)
            writeLong(fileLength)
            writeInt(seq)
            val len = if (type == 1) fileLength.toInt() else byteArray.size
            writeInt(len)
            write(byteArray, 0, len)
        }
    }

    override fun readFields(var1: DataInput?) {
        var1?.apply {
            type = readInt()
            fileLength = readLong()
            seq = readInt()
            val len = readInt()
            byteArray = ByteArray(len)
            for (i in 0 until len) {
                byteArray[i] = readByte()
            }
        }
    }

    fun isStart(): Boolean {
        return type == 0
    }

    fun isEnd(): Boolean {
        return type == 2
    }
}