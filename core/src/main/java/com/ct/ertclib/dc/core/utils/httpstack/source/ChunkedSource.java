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

public class ChunkedSource extends AbstractSource {

    private static final String TAG = "ChunkedSource";
    private static final Logger sLogger = Logger.getLogger(TAG);
    private long bytesRemainingInChunk = -1;
    private boolean hasMoreChunks = true;

    public ChunkedSource(BufferedSource bufferedSource) {
        super(bufferedSource);
    }

    @Override
    public long read(Buffer buffer, long byteCount) throws IOException {
        if (sLogger.isDebugActivated()) {
            sLogger.debug("ChunkedSource read byteCount:" + byteCount);
        }
        if (byteCount < 0) {
            throw new IllegalArgumentException("byteCount < 0: " + byteCount);
        } else if (this.closed) {
            throw new IllegalStateException("closed");
        } else if (!this.hasMoreChunks) {
            return -1;
        } else {
            if (bytesRemainingInChunk == 0 || bytesRemainingInChunk == -1) {
                if (sLogger.isDebugActivated()) {
                    sLogger.debug("ChunkedSource read Chunk size begin");
                }
                readChunkSize();
                if (!this.hasMoreChunks) {
                    return -1;
                }
            }
            long read = this.bufferedSource.read(buffer, Math.min(byteCount, this.bytesRemainingInChunk));
            if (read != -1) {
                this.bytesRemainingInChunk -= read;
                if (sLogger.isDebugActivated()) {
                    sLogger.debug("ChunkedSource read=" + read);
                }
                return read;
            }
            throw new ProtocolException("unexpected end of stream");
        }
    }

    private void readChunkSize() throws IOException {
        if (this.bytesRemainingInChunk != -1) {
            this.bufferedSource.readUtf8LineStrict();
        }
        try {
            this.bytesRemainingInChunk = this.bufferedSource.readHexadecimalUnsignedLong();
            if (sLogger.isDebugActivated()) {
                sLogger.debug("ChunkedSource bytesRemainingInChunk =" + bytesRemainingInChunk);
            }
            String trim = this.bufferedSource.readUtf8LineStrict().trim();
            if (this.bytesRemainingInChunk < 0 || (!trim.isEmpty() && !trim.startsWith(";"))) {
                throw new ProtocolException("expected chunk size and optional extensions but was \"" + this.bytesRemainingInChunk + trim + "\"");
            } else if (this.bytesRemainingInChunk == 0) {
                this.hasMoreChunks = false;
            }
        } catch (NumberFormatException e) {
            throw new ProtocolException(e.getMessage());
        }
    }

    @Override
    public void close() {
        if (!this.closed) {
            this.closed = true;
        }
    }
}
