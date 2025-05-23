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

package com.ct.ertclib.dc.core.utils.httpstack

import com.ct.ertclib.dc.core.utils.httpstack.sink.FixedLengthSink
import com.ct.ertclib.dc.core.utils.logger.Logger
import com.ct.ertclib.dc.core.data.common.HttpResponseResult
import okhttp3.Request
import okhttp3.internal.http.RequestLine
import okhttp3.internal.http.StatusLine.Companion.parse
import okio.buffer
import okio.sink
import okio.source
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.net.Proxy

object HttpStackHelper {
    private const val TAG = "HttpStackHelper"
    private val sLogger = Logger.getLogger(TAG)

    fun verify(byteArray: ByteArray): HttpResponseResult {
        sLogger.info("verify-byteArray:" + byteArray.size)
        val byteArrayLength = byteArray.size
        val byteArrayInputStream = ByteArrayInputStream(byteArray)
        val bufferedSource = byteArrayInputStream.source().buffer()
        val result = HttpResponseResult(isComplete = false, downloadProgress = 0F)
        try {
            val readUtf8LineStrict = bufferedSource.readUtf8LineStrict()
            val statusLine = parse(readUtf8LineStrict)
            sLogger.debug("verify-statusLine=$statusLine")
            val lineLength = readUtf8LineStrict.length + "\r\n".toByteArray().size
            var contentLength = byteArrayLength - lineLength
            val headerBuilder = HttpStackHeaders.Builder()
            while (true) {
                val header = bufferedSource.readUtf8LineStrict()
                if (header.isEmpty()) {
                    break
                }

                val headerLength = header.toByteArray().size
                contentLength = contentLength - headerLength - "\r\n".toByteArray().size
                headerBuilder.addLenient(header)
                sLogger.info("verify headerLength:$headerLength, contentLength:$contentLength")
            }

            val httpStackHeaders = headerBuilder.build()
            val bufferBodyByteLength = contentLength - "\r\n".toByteArray().size
            val contentLengthValue = httpStackHeaders.get("Content-Length")
            return if (contentLengthValue.isNullOrEmpty()) {
                if (sLogger.isDebugActivated) {
                    sLogger.debug("verify-contentLengthValue is null, return true")
                }
                result
            } else {
                sLogger.info("verify-bufferBodyByteLength:$bufferBodyByteLength,contentLengthValue:$contentLengthValue")
                result.apply {
                    isComplete = (bufferBodyByteLength >= contentLengthValue.toLong())
                    downloadProgress = bufferBodyByteLength / contentLengthValue.toFloat()
                }

            }
        } catch (e: IOException) {
            if (sLogger.isDebugActivated) sLogger.error("verify", e)
            return result.apply {
                isComplete = false
            }
        } finally {
            byteArrayInputStream.close()
            bufferedSource.close()
        }
    }

    fun decode(request: Request?, data: ByteArray) : HttpStackResponse? {
        sLogger.info("decode-data:" + data.size)
        val byteArrayInputStream = ByteArrayInputStream(data)

        val bufferedSource = byteArrayInputStream.source().buffer()

        val readStatusString = bufferedSource.readUtf8LineStrict()
        if (sLogger.isDebugActivated) {
            sLogger.debug("decode-readUtf8LineStrict:$readStatusString")
        }

        val statusLineParse = parse(readStatusString)
        if (sLogger.isDebugActivated) {
            sLogger.debug("decode-statusLineParse:$statusLineParse")
        }

        val respBuilder = HttpStackResponse.Builder()
        respBuilder.protocol(statusLineParse.protocol)
        respBuilder.code(statusLineParse.code)
        respBuilder.message(statusLineParse.message)
        respBuilder.builderRequest = request

        val headsBuilder = HttpStackHeaders.Builder()
        try {
            while (true) {
                val headerLine = bufferedSource.readUtf8LineStrict()
                if (headerLine.isEmpty()) {
                    respBuilder.builderHeaderBuild = HttpStackHeaders(headsBuilder).newBuilder()
                    var httpStackResponse = respBuilder.build()

                    val newBuilder = httpStackResponse.newBuilder()
                    newBuilder.builderResponseBody =
                        HttpResponseDecoder.getResponseBody(httpStackResponse, bufferedSource)
                    httpStackResponse = newBuilder.build()
                    if (sLogger.isDebugActivated) {
                        sLogger.debug("decode HttpStackResponse：$httpStackResponse")
                    }
                    return httpStackResponse
                }
                headsBuilder.addLenient(headerLine)
                if (sLogger.isDebugActivated) {
                    sLogger.debug("decode headerLine：$headerLine")
                }
            }
        } catch (e : IOException) {
            if (sLogger.isDebugActivated) {
                sLogger.error("decode", e)
            }
        }
        return null
    }

    fun getRequestData(request:Request) : ByteArray {
        val byteArrayOutputStream = ByteArrayOutputStream()

        val buffer = byteArrayOutputStream.sink().buffer()

        val httpRequestDecode =
            HttpRequestDecoder(null, buffer)

        var requestLine = RequestLine.get(request, Proxy.Type.HTTP)
        if (requestLine.startsWith("GET http://")) {
            requestLine = requestLine.replaceFirst("http:/", "")
        }
        if (requestLine.contains("applicationlist/?")) {
            requestLine = requestLine.replaceFirst("applicationlist/?", "applicationlist?")
        }
        if (requestLine.contains("applications/?")) {
            requestLine = requestLine.replaceFirst("applications/?", "applications?")
        }
        sLogger.debug("getRequestData requestLine:$requestLine")
        httpRequestDecode.writeRequest(request.headers, requestLine)

        val body = request.body
        if (body != null) {
            val contentLength = body.contentLength()
            val bufferSink = FixedLengthSink(
                httpRequestDecode.createRequestBody(request, contentLength),
                contentLength
            ).buffer()
            body.writeTo(bufferSink)
            bufferSink.close()
        }
        httpRequestDecode.sink.flush()

        val toByteArray = byteArrayOutputStream.toByteArray()
        buffer.close()
        byteArrayOutputStream.close()
        return toByteArray

    }

}