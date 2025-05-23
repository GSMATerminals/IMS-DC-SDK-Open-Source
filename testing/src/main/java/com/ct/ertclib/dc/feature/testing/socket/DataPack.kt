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

import java.io.DataInput
import java.io.DataOutput

class DataPack() : Writable {
    var type: Int = 0
    lateinit var byteArray: ByteArray

    companion object {
        val TYPE_CTRL = 0
        val TYPE_VIDEO = 1
        val TYPE_VOICE = 2
        val TYPE_FILE = 3
        val TYPE_SKETCH = 4
        val TYPE_RAW = 5
    }

    constructor(var1: DataInput) : this() {
        readFields(var1)
    }

    constructor(type: Int, byteArray: ByteArray) : this() {
        this.type = type
        this.byteArray = byteArray
    }

    override fun write(var1: DataOutput) {
        var1.writeInt(type)
        var1.writeInt(byteArray.size)
        var1.write(byteArray)
    }

    override fun readFields(var1: DataInput) {
        type = var1.readInt()
        val len = var1.readInt()
        byteArray = ByteArray(len)
        for (i in 0 until len) {
            byteArray[i] = var1.readByte()
        }
    }

    fun isVideoPack(): Boolean {
        return type == TYPE_VIDEO
    }

    fun isVoicePack(): Boolean {
        return type == TYPE_VOICE
    }

    fun isFilePack(): Boolean {
        return type == TYPE_FILE
    }

    fun isCtrlPack(): Boolean {
        return type == TYPE_CTRL
    }

    fun isSketchPack(): Boolean {
        return type == TYPE_SKETCH
    }

    fun isRawPack(): Boolean {
        return type == TYPE_RAW
    }
}