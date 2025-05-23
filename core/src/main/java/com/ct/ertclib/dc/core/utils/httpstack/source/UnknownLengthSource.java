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

import java.io.IOException;

import okio.Buffer;
import okio.BufferedSource;

public class UnknownLengthSource extends AbstractSource {
    private boolean inputExhausted;

    public UnknownLengthSource(BufferedSource source) {
        super(source);
    }

    @Override // okio.Source, okhttp3.internal.http1.Http1Codec.AbstractSource
    public long read(Buffer buffer, long byteCount) throws IOException {
        if (byteCount < 0) {
            throw new IllegalArgumentException("byteCount < 0: " + byteCount);
        } else if (this.closed) {
            throw new IllegalStateException("closed");
        } else if (this.inputExhausted) {
            return -1;
        } else {
            long read = this.bufferedSource.read(buffer, byteCount);
            if (read != -1) {
                return read;
            }
            this.inputExhausted = true;
            return -1;
        }
    }

    @Override // okio.Source, java.io.Closeable, java.lang.AutoCloseable
    public void close() {
        if (!this.closed) {
            this.closed = true;
        }
    }
}
