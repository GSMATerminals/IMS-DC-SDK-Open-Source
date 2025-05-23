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

package com.ct.ertclib.dc.core.utils.httpstack.sink;

import java.io.IOException;

import okio.Buffer;
import okio.Sink;
import okio.Timeout;

public abstract class AbstractSink implements Sink {

    public final Sink mSink;

    public AbstractSink(Sink sink) {
        mSink = sink;
    }

    public void write(Buffer buffer, long byteCount) throws IOException {
        mSink.write(buffer, byteCount);
    }

    public Timeout timeout() {
        return this.mSink.timeout();
    }

    public void close() throws IOException {
        this.mSink.close();
    }

    public void flush() throws IOException {
        this.mSink.flush();
    }

    public String toString() {
        return getClass().getSimpleName() + '(' + this.mSink + ')';
    }
}
