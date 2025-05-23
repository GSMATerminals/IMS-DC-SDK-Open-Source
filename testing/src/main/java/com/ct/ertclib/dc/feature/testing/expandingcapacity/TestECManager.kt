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

package com.ct.ertclib.dc.feature.testing.expandingcapacity

import android.annotation.SuppressLint
import com.ct.ertclib.dc.core.utils.common.JsonUtil
import com.ct.ertclib.dc.core.utils.logger.Logger
import com.ct.ertclib.dc.feature.testing.OEMECBaseData
import com.newcalllib.expandingCapacity.IExpandingCapacity
import com.newcalllib.expandingCapacity.IExpandingCapacityCallback
import java.util.Timer
import java.util.TimerTask

@SuppressLint("StaticFieldLeak")
object TestECManager {
    private val TAG = "TestECManager"
    private val sLogger = Logger.getLogger(TAG)
    var mCallback: IExpandingCapacityCallback? = null
    val mTestExpandingCapacity: TestExpandingCapacity = TestExpandingCapacity()
    private var mTranslateTimer: Timer? = null
    private var mTranslateTask: TimerTask? = null

    fun onUnbind() {
        mCallback = null
        stopTranslate()
    }

    class TestExpandingCapacity : IExpandingCapacity.Stub() {
        override fun request(content: String?) {
            sLogger.info("request content: $content")
            val requestData = content?.let { JsonUtil.fromJson(it, OEMECBaseData::class.java) }
            sLogger.info("request requestData: ${requestData?.module},${requestData?.func}")
            when(requestData?.module){
                "AIVideo" -> {
                    when(requestData.func){
                        "detect" -> {
                            val responseData = OEMECBaseData(
                                "AIVideo",
                                "detectCallback",
                                mutableMapOf("isAI" to true)
                            )
                            val responseString = JsonUtil.toJson(responseData)
                            mCallback?.onCallback(responseString)
                        }
                    }
                }
                "Translate" -> {
                    when(requestData.func){
                        "languageList" -> {
                            val responseData = OEMECBaseData(
                                "Translate",
                                "languageListCallback",
                                mutableMapOf("list" to mutableListOf("Chinese", "English"))
                            )
                            val responseString = JsonUtil.toJson(responseData)
                            mCallback?.onCallback(responseString)
                        }
                        "start" -> {
                            startTranslate()
                        }
                        "stop" -> {
                            stopTranslate()
                        }
                        "voice" -> {

                        }
                    }
                }
                "NewCallSDK" -> {
                    when(requestData.func){
                        "setExpandEnable" -> {
                            val requestDataDetail = content.let { JsonUtil.fromJson(it, ECExpand::class.java) }
                            if (requestDataDetail?.data?.isEnable == true){
                                val responseData = OEMECBaseData(
                                    "NewCallSDK",
                                    "expand",
                                    null
                                )
                                val responseString = JsonUtil.toJson(responseData)
                                mCallback?.onCallback(responseString)
                            } else {
                                val responseData = OEMECBaseData(
                                    "NewCallSDK",
                                    "queryExpandEnable",
                                    null
                                )
                                val responseString = JsonUtil.toJson(responseData)
                                mCallback?.onCallback(responseString)
                            }
                        }
                    }
                }
            }
        }

        override fun setCallback(l: IExpandingCapacityCallback?) {
            if (l != null) {
                mCallback = l
            } else {
                sLogger.info("setCallback l is null")
            }
        }
    }

    fun startTranslate(){
        if (mTranslateTimer == null) {
            mTranslateTimer = Timer()
        }
        if (mTranslateTask == null){
            mTranslateTask =  object :TimerTask(){
                override fun run() {
                    val responseData = OEMECBaseData(
                        "Translate",
                        "translateResultCallback",
                        mutableMapOf(
                            "myOriginal" to "你好",
                            "myTranslate" to "hello",
                            "otherOriginal" to "good morning",
                            "otherTranslate" to "早上好"
                        )
                    )
                    val responseString = JsonUtil.toJson(responseData)
                    mCallback?.onCallback(responseString)
                }
            }
        }
        mTranslateTimer!!.schedule(mTranslateTask, 0, 1000)
    }

    fun stopTranslate(){
        if (mTranslateTimer != null) {
            mTranslateTimer!!.cancel()
            mTranslateTimer = null
        }
        if (mTranslateTask != null){
            mTranslateTask!!.cancel()
            mTranslateTask = null
        }
    }
}