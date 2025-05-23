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

package com.ct.ertclib.dc.core.common

import android.content.Context
import com.ct.ertclib.dc.core.data.model.MessageEntity
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class CompressManager {
    private val mCompressTask = mutableListOf<MessageEntity>()

    private var mCompressService: ExecutorService? = null
    private var mContext: Context? = null

    constructor(context: Context?) {
        mContext = context
        mCompressService = Executors.newSingleThreadExecutor()
    }

    fun setCompressTask(messageEntity: List<MessageEntity>) {
        mCompressTask.clear()
        mCompressTask.addAll(messageEntity)
    }

    fun startCompress() {
        if (mCompressService!!.isShutdown) {
            mCompressService = Executors.newSingleThreadExecutor()
        }
        val compressRunnable = CompressRunnable(mContext!!, mCompressTask)
        mCompressService?.execute(compressRunnable)
    }
}