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

package com.ct.ertclib.dc.core.data.event

import com.ct.ertclib.dc.core.data.miniapp.MiniAppList
// miniAppListInfo是全量
class MiniAppListGetEvent(override var id: Int, override var message: String, val miniAppListInfo: MiniAppList?) : Event<String>() {
    companion object {
        const val TO_REFRESH = "TO_REFRESH"
        const val TO_LOADMORE = "TO_LOADMORE"
        const val ON_DOWNLOAD = "ON_DOWNLOAD"
    }
}