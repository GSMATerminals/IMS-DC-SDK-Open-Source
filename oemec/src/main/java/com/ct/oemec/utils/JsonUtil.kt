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

package com.ct.oemec.utils

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import java.lang.Exception

object JsonUtil {
    private val mGson: Gson = GsonBuilder().disableHtmlEscaping().create()

    private fun getDefaultGson(): Gson {
        return mGson
    }

    fun <T> fromJson(json: String, classOfT: Class<T>): T? {
        kotlin.runCatching {
            return getDefaultGson().fromJson(json, classOfT)
        }.onFailure {

        }
        return null
    }

    fun toJson(obj: Any): String {
        kotlin.runCatching {
            return getDefaultGson().toJson(obj)
        }.onFailure {
        }
        return ""
    }

    fun strToJsonObject(str: String): JsonObject? {
        try {
            return mGson.fromJson(str, JsonObject::class.java)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    fun strToJsonArray(str: String): JsonArray? {
        try {
            return mGson.fromJson(str, JsonArray::class.java)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }
}