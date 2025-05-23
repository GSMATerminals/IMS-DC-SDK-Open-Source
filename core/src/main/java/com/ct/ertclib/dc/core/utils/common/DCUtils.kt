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

package com.ct.ertclib.dc.core.utils.common

import android.content.pm.PackageManager


object DCUtils {

    // 去掉remote和local
    fun ignoreRole(dcLabel:String): String {
        return dcLabel.replace("local_","_").replace("remote_","_")
    }

    fun compareDCLabel(dcLabel1:String,dcLabel2:String):Boolean{
        return dcLabel1.replace("local_","_").replace("remote_","_") == dcLabel2.replace("local_","_").replace("remote_","_")
    }

}