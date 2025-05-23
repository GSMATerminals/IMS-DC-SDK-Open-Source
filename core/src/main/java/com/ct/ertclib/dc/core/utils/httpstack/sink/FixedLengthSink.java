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
import java.net.ProtocolException;

import okhttp3.internal.Util;
import okio.Buffer;
import okio.Sink;

public class FixedLengthSink extends AbstractSink {

    private long bytesRemaining;
    private boolean closed;

    public FixedLengthSink(Sink sink, long byteCount) {
        super(sink);
        this.bytesRemaining = byteCount;

    }

    @Override // okio.Sink
    public void write(Buffer buffer, long j) throws IOException {
        if (!this.closed) {
            Util.checkOffsetAndCount(buffer.size(), 0, j);
            if (j <= this.bytesRemaining) {
                super.write(buffer, j);
                this.bytesRemaining -= j;
                return;
            }
            throw new ProtocolException("expected " + this.bytesRemaining + " bytes but received " + j);
        }
        throw new IllegalStateException("closed");
    }

    @Override // okio.Sink, java.io.Flushable
    public void flush() throws IOException {
        if (!this.closed) {
            this.mSink.flush();
        }
    }

    @Override // java.io.Closeable, java.lang.AutoCloseable, okio.Sink
    public void close() throws IOException {
        if (!this.closed) {
            this.closed = true;
            if (this.bytesRemaining <= 0) {
                super.close();
                return;
            }
            throw new ProtocolException("unexpected end of stream");
        }
    }
}
