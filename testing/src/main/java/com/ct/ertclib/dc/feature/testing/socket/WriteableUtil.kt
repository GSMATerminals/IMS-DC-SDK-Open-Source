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

class WriteableUtil {
    companion object {
        fun toByteArray(w: Writable): ByteArray {
            val tmp = DataOutputStreamBuffer()
            w.write(tmp)
            tmp.flush()
            return tmp.toByteArray()
        }

        fun <T> parse(byteArray: ByteArray, clazz: Class<T>): T {
            val tmp = clazz.newInstance()
            val bytes = DataInputStreamBuffer(byteArray)
            tmp as Writable
            tmp.readFields(bytes)
            return tmp
        }

        fun buildCtrlPack(writable: Writable): ByteArray {
            val pack = DataPack(DataPack.TYPE_CTRL, writable.toByteArray())
            return pack.toByteArray()
        }

        fun buildVideoPack(writable: Writable): ByteArray {
            val pack = DataPack(DataPack.TYPE_VIDEO, writable.toByteArray())
            return pack.toByteArray()
        }

        fun buildVoicePack(writable: Writable): ByteArray {
            val pack = DataPack(DataPack.TYPE_VOICE, writable.toByteArray())
            return pack.toByteArray()
        }

        fun buildFilePack(writable: Writable): ByteArray {
            val pack = DataPack(DataPack.TYPE_FILE, writable.toByteArray())
            return pack.toByteArray()
        }

        fun buildSketchPack(writable: Writable): ByteArray {
            val pack = DataPack(DataPack.TYPE_SKETCH, writable.toByteArray())
            return pack.toByteArray()
        }

        fun buildRawPack(writable: Writable): ByteArray {
            val pack = DataPack(DataPack.TYPE_RAW, writable.toByteArray())
            return pack.toByteArray()
        }
    }
}

fun Writable.toByteArray(): ByteArray {
    return WriteableUtil.toByteArray(this)
}