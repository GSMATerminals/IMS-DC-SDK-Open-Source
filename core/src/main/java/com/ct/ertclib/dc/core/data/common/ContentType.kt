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

package com.ct.ertclib.dc.core.data.common

object ContentType {
    const val TEXT_PLAIN = "text/plain"
    const val TEXT_HTML = "text/html"
    const val TEXT_CSS = "text/html"
    const val IMAGE_PREFIX = "image/"
    const val AUDIO_PREFIX = "audio/"
    const val VIDEO_PREFIX = "video/"
    const val APPLICATION_PREFIX = "application/"


    fun isText(type: String): Boolean {
        return type == TEXT_PLAIN || type == TEXT_HTML || type == TEXT_CSS
    }

    fun isImage(type: String): Boolean {
        return type.startsWith(IMAGE_PREFIX)
    }

    fun isAudio(type: String): Boolean {
        return type.startsWith(AUDIO_PREFIX)
    }

    fun isVideo(type: String): Boolean {
        return type.startsWith(VIDEO_PREFIX)
    }

    fun isFile(type: String): Boolean {
        return type.startsWith(APPLICATION_PREFIX)
    }
}