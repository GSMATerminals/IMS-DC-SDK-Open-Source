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

package com.ct.ertclib.dc.core.utils.httpstack.source;

import androidx.annotation.NonNull;

import okio.BufferedSource;
import okio.ForwardingTimeout;
import okio.Source;
import okio.Timeout;

public abstract class AbstractSource implements Source {

    public ForwardingTimeout forwardingTimeout;
    public BufferedSource bufferedSource;

    public boolean closed;

    public AbstractSource(BufferedSource bufferedSource) {
        this.bufferedSource = bufferedSource;
        this.forwardingTimeout = new ForwardingTimeout(bufferedSource.timeout());
    }

    @NonNull
    @Override
    public Timeout timeout() {
        return forwardingTimeout;
    }
}
