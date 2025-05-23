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

package com.ct.ertclib.dc.core.utils.httpstack;

import java.io.IOException;
import java.net.ProtocolException;

import okhttp3.Headers;
import okhttp3.Request;
import okhttp3.internal.Util;

import okio.Buffer;
import okio.BufferedSink;
import okio.BufferedSource;
import okio.ForwardingTimeout;
import okio.Sink;
import okio.Source;
import okio.Timeout;

public final class HttpRequestDecoder {

    private long headerLimit = 262144;
    final BufferedSink sink;
    final BufferedSource source;
    int state = 0;

    public HttpRequestDecoder(BufferedSource bufferedSource, BufferedSink bufferedSink) {
        this.source = bufferedSource;
        this.sink = bufferedSink;
    }

 // okhttp3.internal.http.HttpCodec
    public Sink createRequestBody(Request request, long j) {
        if ("chunked".equalsIgnoreCase(request.header("Transfer-Encoding"))) {
            return newChunkedSink();
        }
        if (j != -1) {
            return newFixedLengthSink(j);
        }
        throw new IllegalStateException("Cannot stream a request body without chunked encoding or a known content length!");
    }

    public void writeRequest(Headers headers, String str) throws IOException {
        if (this.state == 0) {
            this.sink.writeUtf8(str).writeUtf8("\r\n");
            int size = headers.size();
            for (int i = 0; i < size; i++) {
                this.sink.writeUtf8(headers.name(i)).writeUtf8(": ").writeUtf8(headers.value(i)).writeUtf8("\r\n");
            }
            this.sink.writeUtf8("\r\n");
            this.state = 1;
            return;
        }
        throw new IllegalStateException("state: " + this.state);
    }

    public Sink newChunkedSink() {
        if (this.state == 1) {
            this.state = 2;
            return new ChunkedSink();
        }
        throw new IllegalStateException("state: " + this.state);
    }

    public Sink newFixedLengthSink(long j) {
        if (this.state == 1) {
            this.state = 2;
            return new FixedLengthSink(j);
        }
        throw new IllegalStateException("state: " + this.state);
    }

    public void detachTimeout(ForwardingTimeout forwardingTimeout) {
        Timeout delegate = forwardingTimeout.delegate();
        forwardingTimeout.setDelegate(Timeout.NONE);
        delegate.clearDeadline();
        delegate.clearTimeout();
    }

    public final class FixedLengthSink implements Sink {
        private long bytesRemaining;
        private boolean closed;
        private final ForwardingTimeout fForwardingTimeout = new ForwardingTimeout(HttpRequestDecoder.this.sink.timeout());

        FixedLengthSink(long j) {
            this.bytesRemaining = j;
        }

        @Override // okio.Sink
        public Timeout timeout() {
            return this.fForwardingTimeout;
        }

        @Override // okio.Sink
        public void write(Buffer buffer, long j) throws IOException {
            if (!this.closed) {
                Util.checkOffsetAndCount(buffer.size(), 0, j);
                if (j <= this.bytesRemaining) {
                    HttpRequestDecoder.this.sink.write(buffer, j);
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
                HttpRequestDecoder.this.sink.flush();
            }
        }

        @Override // java.io.Closeable, java.lang.AutoCloseable, okio.Sink
        public void close() throws IOException {
            if (!this.closed) {
                this.closed = true;
                if (this.bytesRemaining <= 0) {
                    HttpRequestDecoder.this.detachTimeout(this.fForwardingTimeout);
                    HttpRequestDecoder.this.state = 3;
                    return;
                }
                throw new ProtocolException("unexpected end of stream");
            }
        }
    }

    public final class ChunkedSink implements Sink {
        private boolean closed;
        private final ForwardingTimeout cForwardingTimeout = new ForwardingTimeout(HttpRequestDecoder.this.sink.timeout());

        ChunkedSink() {
        }

        @Override // okio.Sink
        public Timeout timeout() {
            return this.cForwardingTimeout;
        }

        @Override // okio.Sink
        public void write(Buffer buffer, long j) throws IOException {
            if (this.closed) {
                throw new IllegalStateException("closed");
            } else if (j != 0) {
                HttpRequestDecoder.this.sink.writeHexadecimalUnsignedLong(j);
                HttpRequestDecoder.this.sink.writeUtf8("\r\n");
                HttpRequestDecoder.this.sink.write(buffer, j);
                HttpRequestDecoder.this.sink.writeUtf8("\r\n");
            }
        }

        @Override // okio.Sink, java.io.Flushable
        public synchronized void flush() throws IOException {
            if (!this.closed) {
                HttpRequestDecoder.this.sink.flush();
            }
        }

        @Override // java.io.Closeable, java.lang.AutoCloseable, okio.Sink
        public synchronized void close() throws IOException {
            if (!this.closed) {
                this.closed = true;
                HttpRequestDecoder.this.sink.writeUtf8("0\r\n\r\n");
                HttpRequestDecoder.this.detachTimeout(this.cForwardingTimeout);
                HttpRequestDecoder.this.state = 3;
            }
        }
    }

    private abstract class AbstractSource implements Source {
        protected long bytesRead;
        protected boolean closed;
        protected final ForwardingTimeout aForwardingTimeout;

        private AbstractSource() {
            this.aForwardingTimeout = new ForwardingTimeout(HttpRequestDecoder.this.source.timeout());
            this.bytesRead = 0;
        }

        @Override // okio.Source
        public Timeout timeout() {
            return this.aForwardingTimeout;
        }

        @Override // okio.Source
        public long read(Buffer buffer, long j) throws IOException {
            try {
                long read = HttpRequestDecoder.this.source.read(buffer, j);
                if (read > 0) {
                    this.bytesRead += read;
                }
                return read;
            } catch (IOException e) {
                endOfInput();
                throw e;
            }
        }

        public final void endOfInput() {
            if (HttpRequestDecoder.this.state != 6) {
                if (HttpRequestDecoder.this.state == 5) {
                    HttpRequestDecoder.this.detachTimeout(this.aForwardingTimeout);
                    HttpRequestDecoder.this.state = 6;

                    return;
                }
                throw new IllegalStateException("state: " + HttpRequestDecoder.this.state);
            }
        }
    }
}