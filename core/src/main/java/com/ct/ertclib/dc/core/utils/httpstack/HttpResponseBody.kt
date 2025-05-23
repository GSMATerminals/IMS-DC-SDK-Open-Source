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

import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody
import okio.BufferedSource

class HttpResponseBody(
    private val stackHeaders: HttpStackHeaders,
    private val bufferedSource: BufferedSource) : ResponseBody() {

    override fun contentLength(): Long {
        val length = stackHeaders.get("Content-Length")
        return if (length.isNullOrEmpty()) {
            -1
        } else {
            length.toLong()
        }
    }

    override fun contentType(): MediaType? {
        val contentType = stackHeaders.get("Content_Type")
        return if (contentType.isNullOrEmpty()) {
            null
        } else {
            contentType.toMediaType()
        }
    }

    override fun source(): BufferedSource {
        return bufferedSource
    }


}