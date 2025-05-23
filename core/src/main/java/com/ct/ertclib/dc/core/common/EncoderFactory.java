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

package com.ct.ertclib.dc.core.common;

import android.text.TextUtils;

public class EncoderFactory {
    public static final String BASE64 = "Base64";

    /**
     * 默认返回Base64编码器
     *
     * @param encoding 编码格式
     * @return BaseEncoder编码器
     */
    public static BaseEncoder getEncoder(String encoding) {
        BaseEncoder encoder;

        switch (encoding) {
            case BASE64:
            default:
                encoder = new Base64Encoder();
        }

        return encoder;
    }
}
