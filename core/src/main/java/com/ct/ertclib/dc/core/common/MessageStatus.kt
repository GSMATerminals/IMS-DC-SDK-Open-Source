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

enum class MessageStatus(val value: Int) {
    // Outgoing
    OUTGOING_COMPLETE(1),
    OUTGOING_DELIVERED(2),
    OUTGOING_DRAFT(3),
    OUTGOING_YET_TO_SEND(4),
    OUTGOING_SENDING(5),
    OUTGOING_RESENDING(6),
    OUTGOING_AWAITING_RETRY(7),
    OUTGOING_FAILED(8),

    // Incoming
    INCOMING_COMPLETE(100),
    INCOMING_YET_TO_MANUAL_DOWNLOAD(101),
    INCOMING_RETRYING_MANUAL_DOWNLOAD(102),
    INCOMING_MANUAL_DOWNLOADING(103),
    INCOMING_RETRYING_AUTO_DOWNLOAD(104),
    INCOMING_AUTO_DOWNLOADING(105),
    INCOMING_DOWNLOAD_FAILED(106)
}