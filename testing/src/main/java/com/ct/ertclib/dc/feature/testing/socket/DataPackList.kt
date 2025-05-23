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

import java.util.concurrent.LinkedBlockingQueue


class DataPackList(val cacheSize: Int) {

    private val videoList: LinkedBlockingQueue<Writable> = LinkedBlockingQueue(cacheSize)
    private val voiceList: LinkedBlockingQueue<Writable> = LinkedBlockingQueue(cacheSize)
    private val fileList: LinkedBlockingQueue<Writable> = LinkedBlockingQueue(cacheSize)
    private val crtlList: LinkedBlockingQueue<Writable> = LinkedBlockingQueue(cacheSize)
    private val sketchList: LinkedBlockingQueue<Writable> = LinkedBlockingQueue(cacheSize)
    private val rawList: LinkedBlockingQueue<Writable> = LinkedBlockingQueue(cacheSize)

    fun getVideoPack(remove: Boolean = false): Writable? {
        val tmp = videoList.peek()
        if (remove) {
            videoList.remove(tmp)
        }
        return tmp
    }

    fun getVoicePack(remove: Boolean = false): Writable? {
        val tmp = voiceList.peek()
        if (remove) {
            voiceList.remove(tmp)
        }
        return tmp
    }

    fun getFilePack(remove: Boolean = false): Writable? {
        val tmp = fileList.peek()
        if (remove) {
            fileList.remove(tmp)
        }
        return tmp
    }

    fun getCRTPack(remove: Boolean = false): Writable? {
        val tmp = crtlList.peek()
        if (remove) {
            crtlList.remove(tmp)
        }
        return tmp
    }

    fun getSketchPack(remove: Boolean = false): Writable? {
        val tmp = sketchList.peek()
        if (remove) {
            sketchList.remove(tmp)
        }
        return tmp
    }

    fun getRawPack(remove: Boolean = false): Writable? {
        val tmp = rawList.peek()
        if (remove) {
            rawList.remove(tmp)
        }
        return tmp
    }

    /**
     * 主动添加的
     * @param writable Writable
     */
    fun putDataPack(writable: Writable) {
        when (writable) {
            is VideoPack -> videoList.put(writable)
            is VoicePack -> voiceList.put(writable)
            is FilePack -> fileList.put(writable)
            is CtrlPack -> crtlList.put(writable)
            is SketchPack -> sketchList.put(writable)
            is RawBytePack -> rawList.put(writable)
        }
    }

    /**
     * 从socket中拿到的
     * @param _object ByteArray
     */
    fun putDataPack(_object: ByteArray) {
        val tmp = DataInputStreamBuffer(_object)
        val dataPack = DataPack(tmp)
        tmp.close()
        if (dataPack.isCtrlPack()) {
            crtlList.put(SketchPack(dataPack.byteArray))
        } else if (dataPack.isVideoPack()) {
            videoList.put(VideoPack(dataPack.byteArray))
        } else if (dataPack.isVoicePack()) {
            voiceList.put(VoicePack(dataPack.byteArray))
        } else if (dataPack.isFilePack()) {
            fileList.put(FilePack(dataPack.byteArray))
        } else if (dataPack.isSketchPack()) {
            sketchList.put(SketchPack(dataPack.byteArray))
        } else if (dataPack.isRawPack()) {
            rawList.put(RawBytePack(dataPack.byteArray))
        }
    }
}