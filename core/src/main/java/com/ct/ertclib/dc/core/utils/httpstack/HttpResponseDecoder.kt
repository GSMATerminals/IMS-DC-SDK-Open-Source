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

import com.ct.ertclib.dc.core.utils.httpstack.source.AbstractSource
import com.ct.ertclib.dc.core.utils.httpstack.source.ChunkedSource
import com.ct.ertclib.dc.core.utils.httpstack.source.FixedLengthSource
import com.ct.ertclib.dc.core.utils.httpstack.source.UnknownLengthSource
import com.ct.ertclib.dc.core.utils.logger.Logger
import okhttp3.ResponseBody
import okio.BufferedSource
import okio.buffer

object HttpResponseDecoder {
    private const val TAG = "HttpResponseDecoder"
    private val sLogger = Logger.getLogger(TAG)
    fun getResponseBody(
        httpStackResp: HttpStackResponse,
        bufferedSource: BufferedSource
    ): ResponseBody {

        sLogger.info("HttpResponseDecoder getResponseBody")
        val abstractSource: AbstractSource
        if (sLogger.isDebugActivated) {
            sLogger.debug("getResponseBody-httpStackResp:$httpStackResp")
        }

        if (!isResponseHaveBody(httpStackResp)) {
            sLogger.info("getResponseBody not have body, new FixedSource")
            abstractSource = FixedLengthSource(bufferedSource, 0)
        } else {
            val encodingHeader = httpStackResp.header("Transfer-Encoding")
            val lengthHeader = httpStackResp.header("Content-Length")

            if (sLogger.isDebugActivated) {
                sLogger.debug("getResponseBody encodingHeader:$encodingHeader, lengthHeader:$lengthHeader")
            }
            abstractSource = if ("chunked".equals(encodingHeader, true)) {
                sLogger.info("getResponseBody chunked source")
                ChunkedSource(bufferedSource)
            } else {
                if (lengthHeader.isNullOrEmpty()) {
                    sLogger.info("getResponseBody UnknownLengthSource")
                    UnknownLengthSource(bufferedSource)
                } else {
                    sLogger.info("getResponseBody FixedLengthSource")
                    FixedLengthSource(bufferedSource, lengthHeader.toLong())
                }
            }
        }
        return HttpResponseBody(httpStackResp.headers, abstractSource.buffer())
    }

    private fun isResponseHaveBody(httpStackResp: HttpStackResponse): Boolean {
        val code = httpStackResp.code
        if ((code in 100..199) || code == 204 || code == 304) {
            val contentLength = httpStackResp.headers.get("Content-length")
            val encode = httpStackResp.headers.get("Transfer-Encoding")
            if (!contentLength.isNullOrEmpty()) {
                val length = contentLength.toLong()
                return length != -1L || "chunked".equals(encode, true)
            }
        }
        return true
    }
}