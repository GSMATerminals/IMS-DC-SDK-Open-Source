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


import com.ct.ertclib.dc.core.utils.logger.Logger;

import java.io.IOException;
import java.net.ProtocolException;

import okio.Buffer;
import okio.BufferedSource;

public class FixedLengthSource extends AbstractSource {
    private static final String TAG = "FixedLengthSource";
    private static final Logger sLogger = Logger.getLogger(TAG);
    private long bytesRemaining;

    public FixedLengthSource(BufferedSource bufferedSource, long j) throws IOException {
        super(bufferedSource);
        this.bytesRemaining = j;
    }

    @Override
    public long read(Buffer buffer, long byteCount) throws IOException {
        if (byteCount < 0) {
            throw new IllegalArgumentException("byteCount < 0: " + byteCount);
        } else if (!this.closed) {
            if (bytesRemaining == 0) {
                return -1;
            }
            long read = this.bufferedSource.read(buffer, Math.min(bytesRemaining, byteCount));
            if (read != -1) {
                bytesRemaining = this.bytesRemaining - read;
                return read;
            }
            throw new ProtocolException("unexpected end of stream");
        } else {
            throw new IllegalStateException("closed");
        }
    }

    @Override
    public void close() {
        if (!this.closed) {
            this.closed = true;
        }
    }
}
