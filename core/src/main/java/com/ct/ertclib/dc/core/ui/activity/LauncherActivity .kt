/*
 * Copyright 2025-China Telecom Research Institute.
 * All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ct.ertclib.dc.core.ui.activity

import android.content.Intent
import android.os.Bundle
import com.ct.ertclib.dc.core.utils.common.FlavorUtils
import com.ct.ertclib.dc.core.utils.extension.startLocalTestActivity
import com.ct.ertclib.dc.core.utils.extension.startNetDCDialerActivity

class LauncherActivity : BaseAppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (FlavorUtils.getChannelName() == FlavorUtils.CHANNEL_LOCAL) {
            startLocalTestActivity()
        } else {
            startNetDCDialerActivity()
        }
        finish()
    }
}

