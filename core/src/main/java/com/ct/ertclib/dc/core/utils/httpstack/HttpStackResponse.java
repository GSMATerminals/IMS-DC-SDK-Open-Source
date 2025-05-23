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

import androidx.annotation.Nullable;

import java.io.Closeable;

import okhttp3.Handshake;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.ResponseBody;

public final class HttpStackResponse implements Closeable {
    @Nullable
    final ResponseBody responseBody;
    @Nullable
    final HttpStackResponse cacheResponse;
    final int code;
    @Nullable
    final Handshake handshake;
    final HttpStackHeaders headers;
    final String message;
    @Nullable
    final HttpStackResponse networkResponse;
    @Nullable
    final HttpStackResponse priorResponse;
    final Protocol protocol;
    final long receivedResponseAtMillis;
    final Request request;
    final long sentRequestAtMillis;

    HttpStackResponse(Builder builder) {
        this.request = builder.builderRequest;
        this.protocol = builder.builderProtocol;
        this.code = builder.builderCode;
        this.message = builder.builderMessage;
        this.handshake = builder.builderHandshake;
        this.headers = builder.builderHeaderBuild.build();
        this.responseBody = builder.builderResponseBody;
        this.networkResponse = builder.builderNetworkResponse;
        this.cacheResponse = builder.builderCacheResponse;
        this.priorResponse = builder.builderPriorResponse;
        this.sentRequestAtMillis = builder.builderSentRequestAtMillis;
        this.receivedResponseAtMillis = builder.builderReceivedResponseAtMillis;
    }
    public int code() {
        return this.code;
    }

    public boolean isSuccessful() {
        int i = this.code;
        return i >= 200 && i < 300;
    }

    @Nullable
    public String header(String str) {
        return header(str, null);
    }

    @Nullable
    public String header(String str, @Nullable String str2) {
        String str3 = this.headers.get(str);
        return str3 != null ? str3 : str2;
    }

    @Nullable
    public ResponseBody body() {
        return this.responseBody;
    }

    public Builder newBuilder() {
        return new Builder(this);
    }

    @Override // java.io.Closeable, java.lang.AutoCloseable
    public void close() {
        ResponseBody tmpResponseBody = this.responseBody;
        if (tmpResponseBody != null) {
            tmpResponseBody.close();
            return;
        }
        throw new IllegalStateException("response is not eligible for a body and must not be closed");
    }

    public String toString() {
        return "Response{protocol=" + this.protocol + ", code=" + this.code + ", message=" + this.message + ", url=" + this.request.url() + '}';
    }

    public static class Builder {
        @Nullable
        ResponseBody builderResponseBody;
        @Nullable
        HttpStackResponse builderCacheResponse;
        int builderCode;
        @Nullable
        Handshake builderHandshake;
        HttpStackHeaders.Builder builderHeaderBuild;
        String builderMessage;
        @Nullable
        HttpStackResponse builderNetworkResponse;
        @Nullable
        HttpStackResponse builderPriorResponse;
        @Nullable
        Protocol builderProtocol;
        long builderReceivedResponseAtMillis;
        @Nullable
        Request builderRequest;
        long builderSentRequestAtMillis;

        public Builder() {
            this.builderCode = -1;
            this.builderHeaderBuild = new HttpStackHeaders.Builder();
        }

        Builder(HttpStackResponse response) {
            this.builderCode = -1;
            this.builderRequest = response.request;
            this.builderProtocol = response.protocol;
            this.builderCode = response.code;
            this.builderMessage = response.message;
            this.builderHandshake = response.handshake;
            this.builderHeaderBuild = response.headers.newBuilder();
            this.builderResponseBody = response.responseBody;
            this.builderNetworkResponse = response.networkResponse;
            this.builderCacheResponse = response.cacheResponse;
            this.builderPriorResponse = response.priorResponse;
            this.builderSentRequestAtMillis = response.sentRequestAtMillis;
            this.builderReceivedResponseAtMillis = response.receivedResponseAtMillis;
        }

        public Builder request(Request request2) {
            this.builderRequest = request2;
            return this;
        }

        public Builder protocol(Protocol protocol2) {
            this.builderProtocol = protocol2;
            return this;
        }

        public Builder code(int i) {
            this.builderCode = i;
            return this;
        }

        public Builder message(String str) {
            this.builderMessage = str;
            return this;
        }

        public Builder handshake(@Nullable Handshake handshake2) {
            this.builderHandshake = handshake2;
            return this;
        }

        public Builder header(String str, String str2) {
            this.builderHeaderBuild.set(str, str2);
            return this;
        }

        public Builder addHeader(String str, String str2) {
            this.builderHeaderBuild.add(str, str2);
            return this;
        }

        public Builder removeHeader(String str) {
            this.builderHeaderBuild.removeAll(str);
            return this;
        }

        public Builder headers(HttpStackHeaders headers2) {
            this.builderHeaderBuild = headers2.newBuilder();
            return this;
        }

        public Builder body(@Nullable ResponseBody responseBody) {
            this.builderResponseBody = responseBody;
            return this;
        }

        public Builder networkResponse(@Nullable HttpStackResponse response) {
            if (response != null) {
                checkSupportResponse("networkResponse", response);
            }
            this.builderNetworkResponse = response;
            return this;
        }

        public Builder cacheResponse(@Nullable HttpStackResponse response) {
            if (response != null) {
                checkSupportResponse("cacheResponse", response);
            }
            this.builderCacheResponse = response;
            return this;
        }

        private void checkSupportResponse(String str, HttpStackResponse response) {
            if (response.responseBody != null) {
                throw new IllegalArgumentException(str + ".body != null");
            } else if (response.networkResponse != null) {
                throw new IllegalArgumentException(str + ".networkResponse != null");
            } else if (response.cacheResponse != null) {
                throw new IllegalArgumentException(str + ".cacheResponse != null");
            } else if (response.priorResponse != null) {
                throw new IllegalArgumentException(str + ".priorResponse != null");
            }
        }

        public HttpStackResponse build() {
            if (this.builderRequest == null) {
                throw new IllegalStateException("request == null");
            } else if (this.builderProtocol == null) {
                throw new IllegalStateException("protocol == null");
            } else if (this.builderCode < 0) {
                throw new IllegalStateException("code < 0: " + this.builderCode);
            } else if (this.builderMessage != null) {
                return new HttpStackResponse(this);
            } else {
                throw new IllegalStateException("message == null");
            }
        }
    }
}
