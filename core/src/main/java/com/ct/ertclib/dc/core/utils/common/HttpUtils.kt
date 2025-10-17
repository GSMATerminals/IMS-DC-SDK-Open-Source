package com.ct.ertclib.dc.core.utils.common

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import java.io.IOException
import java.util.concurrent.TimeUnit

object HttpUtils {

    private const val MAX_LINK_TIME = 15L
    private const val TAG = "HttpUtils"

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(MAX_LINK_TIME, TimeUnit.SECONDS)
        .readTimeout(MAX_LINK_TIME, TimeUnit.SECONDS)
        .writeTimeout(MAX_LINK_TIME, TimeUnit.SECONDS)
        .build()

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    fun sendGetRequest(url: String, headers: Map<String, String>, resultCallback: (String) -> Unit) {
        val builder = Request.Builder().url(url)
        headers.forEach { (entry, value) ->
            builder.addHeader(entry, value)
        }
        val request = builder.get().build()
        val call = okHttpClient.newCall(request)
        scope.launch {
            kotlin.runCatching {
                call.enqueue(object : Callback {
                    override fun onFailure(call: Call, e: IOException) {
                        LogUtils.error(TAG, "sendGetRequest failed : ${e.message}")
                        resultCallback.invoke("")
                    }

                    override fun onResponse(call: Call, response: Response) {
                        LogUtils.debug(TAG, "sendGetRequest onResponse")
                        val responseBody = response.body?.string()
                        responseBody?.let {
                            resultCallback.invoke(it)
                        } ?: run {
                            resultCallback.invoke("")
                        }
                    }
                })
            }.onFailure {
                LogUtils.error(TAG, "sendGetRequest onFailure failed : ${it.message}")
                resultCallback.invoke("")
            }
        }
    }

    fun sendPostRequest(url: String, headers: Map<String, String>, paramsJson: String, mediaType: String, resultCallback: (String) -> Unit) {
        val requestBody = RequestBody.create(mediaType.toMediaTypeOrNull(), paramsJson)
        val builder = Request.Builder().url(url)
        headers.forEach { (entry, value) ->
            builder.addHeader(entry, value)
        }
        val request = builder.post(requestBody).build()
        scope.launch {
            kotlin.runCatching {
                val call = okHttpClient.newCall(request)
                call.enqueue(object : Callback {
                    override fun onFailure(call: Call, e: IOException) {
                        LogUtils.error(TAG, "sendPostRequest failed: $e")
                        resultCallback.invoke("")
                    }

                    override fun onResponse(call: Call, response: Response) {
                        val responseBody = response.body?.string()
                        responseBody?.let {
                            resultCallback.invoke(it)
                        } ?: run {
                            resultCallback.invoke("")
                        }
                    }
                })
            }.onFailure {
                LogUtils.debug(TAG, "sendPostRequest onFailure : $it")
                resultCallback.invoke("")
            }
        }
    }
}