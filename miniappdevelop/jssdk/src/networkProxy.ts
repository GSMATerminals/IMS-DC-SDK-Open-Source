// ==================== networkProxy.ts ====================

import { dcPlugin, bizPlugin } from './index';

// ==================== 类型定义 ====================

interface ProxyRequest {
    id: string;
    url: string;
    method: string;
    headers: Record<string, string>;
    body?: any;
    dcLabel: string;
    timestamp: number;
    timeout?: number;
}

interface ProxyResponse {
    status: number;
    statusText: string;
    headers: Record<string, string>;
    body: string;
    isBinary?: boolean;
}

// ==================== 配置 ====================

const CONFIG = {
    maxChunkSize: 20000,
    minChunkSize: 1024,
    retryDelayMs: 50,
    queueProcessInterval: 100,
    dcLabelPrefixes: ['local_', 'remote_'],
    defaultTimeout: 30000,  // ★★★ 总超时，默认 30 秒 ★★★
};

// ==================== 响应拼包器 ====================

class ResponseBuilder {
    private buffer: Uint8Array[] = [];
    private totalReceived = 0;
    private contentLength = -1;
    private headersParsed = false;
    private isBinary = false;
    private isChunked = false;
    private headerEndIndex = -1;  // ★★★ header 区结束位置（\r\n\r\n 的结束偏移）★★★

    appendData(data: Uint8Array): boolean {
        this.buffer.push(data);
        this.totalReceived += data.length;

        if (!this.headersParsed) {
            this.parseHeaders();
        }

        if (!this.headersParsed) {
            // ★★★ header 还没解析完，继续等 ★★★
            return false;
        }

        if (this.isChunked) {
            // ★★★ chunked：检测终止 chunk (0\r\n\r\n) ★★★
            return this.checkChunkedComplete();
        }

        if (this.contentLength > 0) {
            return this.totalReceived >= this.headerEndIndex + this.contentLength;
        }
        if (this.contentLength === 0) {
            return true;
        }
        // ★★★ fallback：无 Content-Length 且非 chunked，以 header 结束后的全部数据为 body
        return true;
    }

    private parseHeaders(): void {
        if (this.buffer.length === 0) return;

        try {
            const allData = this.concatBuffers();
            const responseStr = new TextDecoder('utf-8').decode(allData);

            // ★★★ 先找 header/body 分界 \r\n\r\n ★★★
            const headerEnd = responseStr.indexOf('\r\n\r\n');
            if (headerEnd < 0) {
                // ★★★ header 还没收完，继续等 ★★★
                return;
            }

            const headerStr = responseStr.substring(0, headerEnd);
            const lines = headerStr.split('\r\n');

            for (const line of lines) {
                const lowerLine = line.toLowerCase();
                if (lowerLine.startsWith('content-length:')) {
                    const value = line.substring(line.indexOf(':') + 1).trim();
                    this.contentLength = parseInt(value, 10);
                    if (isNaN(this.contentLength)) this.contentLength = -1;
                }
                if (lowerLine.startsWith('content-type:')) {
                    const value = line.substring(line.indexOf(':') + 1).trim();
                    if (this.isBinaryContentType(value)) {
                        this.isBinary = true;
                    }
                }
                if (lowerLine.startsWith('transfer-encoding:') && lowerLine.includes('chunked')) {
                    this.isChunked = true;
                }
            }

            // ★★★ 记录 header 区结束偏移（\r\n\r\n 占 4 字节）★★★
            this.headerEndIndex = headerEnd + 4;
            this.headersParsed = true;

            // ★★★ chunked 时不使用 contentLength 判断完成 ★★★
            if (this.isChunked) {
                this.contentLength = -1;
            }
        } catch (e) {
            console.warn('[ResponseBuilder] Parse headers error:', e);
        }
    }

    // ★★★ 检测 chunked 编码是否完整接收（终止符 0\r\n\r\n）★★★
    private checkChunkedComplete(): boolean {
        const allData = this.concatBuffers();
        const bodyData = allData.slice(this.headerEndIndex);
        const bodyStr = new TextDecoder('utf-8').decode(bodyData);
        // ★★★ chunked 终止：最后一个 chunk 大小为 0，后跟 \r\n\r\n
        //     即 body 中包含 "0\r\n\r\n"
        return bodyStr.includes('\r\n0\r\n\r\n') || bodyStr.startsWith('0\r\n\r\n');
    }

    private isBinaryContentType(contentType: string): boolean {
        const binaryTypes = [
            'image/', 'video/', 'audio/',
            'application/octet-stream',
            'application/zip', 'application/pdf',
            'application/x-tar', 'application/gzip',
            'multipart/',
        ];
        const lowerType = contentType.toLowerCase();
        return binaryTypes.some(type => lowerType.startsWith(type));
    }

    private concatBuffers(): Uint8Array {
        const totalLength = this.buffer.reduce((sum, buf) => sum + buf.length, 0);
        const result = new Uint8Array(totalLength);
        let offset = 0;
        for (const buf of this.buffer) {
            result.set(buf, offset);
            offset += buf.length;
        }
        return result;
    }

    getCompleteData(): Uint8Array | null {
        if (!this.headersParsed) return null;

        const allData = this.concatBuffers();
        const headerPart = allData.slice(0, this.headerEndIndex);

        if (this.isChunked) {
            // ★★★ chunked：解码 chunked body，拼接出真实 body ★★★
            const bodyData = allData.slice(this.headerEndIndex);
            const decodedBody = this.decodeChunkedBody(bodyData);
            if (decodedBody === null) return null;

            // ★★★ 重新构造完整响应：header(去掉 Transfer-Encoding) + Content-Length + 真实 body ★★★
            const headerStr = new TextDecoder('utf-8').decode(headerPart);
            const lines = headerStr.split('\r\n');
            const newLines: string[] = [];
            for (const line of lines) {
                const lowerLine = line.toLowerCase();
                if (lowerLine.startsWith('transfer-encoding:')) {
                    // ★★★ 跳过 Transfer-Encoding 头 ★★★
                    continue;
                }
                if (lowerLine.startsWith('content-length:')) {
                    // ★★★ 跳过原有 Content-Length（chunked 响应通常没有，但保险起见）★★★
                    continue;
                }
                newLines.push(line);
            }
            // ★★★ 补上真实的 Content-Length ★★★
            newLines.push(`Content-Length: ${decodedBody.length}`);

            const newHeaderBytes = new TextEncoder().encode(newLines.join('\r\n'));
            const result = new Uint8Array(newHeaderBytes.length + decodedBody.length);
            result.set(newHeaderBytes, 0);
            result.set(decodedBody, newHeaderBytes.length);
            return result;
        }

        if (this.contentLength < 0) {
            // ★★★ fallback：无 Content-Length，返回全部数据 ★★★
            return allData;
        }
        if (this.totalReceived < this.headerEndIndex + this.contentLength) return null;
        // ★★★ 截取 header + contentLength 字节的 body ★★★
        return allData.slice(0, this.headerEndIndex + this.contentLength);
    }

    // ★★★ 解码 chunked transfer-encoding body（纯字节偏移，兼容二进制）★★★
    private decodeChunkedBody(bodyData: Uint8Array): Uint8Array | null {
        try {
            const chunks: Uint8Array[] = [];
            let pos = 0;

            while (pos < bodyData.length) {
                // ★★★ 找 chunk size 行的 \r\n（逐字节扫描，避免多字节字符问题）★★★
                let lineEnd = -1;
                for (let i = pos; i < bodyData.length - 1; i++) {
                    if (bodyData[i] === 0x0D && bodyData[i + 1] === 0x0A) {
                        lineEnd = i;
                        break;
                    }
                }
                if (lineEnd < 0) break;

                // ★★★ 解析 chunk size（hex），逐字节读取 ASCII ★★★
                const sizeBytes = bodyData.slice(pos, lineEnd);
                const sizeStr = new TextDecoder('ascii').decode(sizeBytes).trim();
                // ★★★ chunk size 可能带扩展（;ext），取分号前 ★★★
                const semiIndex = sizeStr.indexOf(';');
                const hexStr = semiIndex >= 0 ? sizeStr.substring(0, semiIndex) : sizeStr;
                const chunkSize = parseInt(hexStr, 16);

                if (isNaN(chunkSize) || chunkSize < 0) break;

                pos = lineEnd + 2;  // ★★★ 跳过 \r\n ★★★

                if (chunkSize === 0) {
                    // ★★★ 终止 chunk ★★★
                    break;
                }

                // ★★★ 检查是否有足够的字节 ★★★
                if (pos + chunkSize > bodyData.length) break;

                // ★★★ 直接用字节偏移截取 chunk data（二进制安全）★★★
                chunks.push(bodyData.slice(pos, pos + chunkSize));
                pos += chunkSize;

                // ★★★ 跳过 chunk 后的 \r\n ★★★
                if (pos + 1 < bodyData.length && bodyData[pos] === 0x0D && bodyData[pos + 1] === 0x0A) {
                    pos += 2;
                }
            }

            const totalLength = chunks.reduce((sum, buf) => sum + buf.length, 0);
            const result = new Uint8Array(totalLength);
            let offset = 0;
            for (const buf of chunks) {
                result.set(buf, offset);
                offset += buf.length;
            }
            return result;
        } catch (e) {
            console.warn('[ResponseBuilder] Decode chunked body error:', e);
            return null;
        }
    }

    isBinaryResponse(): boolean {
        return this.isBinary;
    }

    clear(): void {
        this.buffer = [];
        this.totalReceived = 0;
        this.contentLength = -1;
        this.headersParsed = false;
        this.isBinary = false;
        this.isChunked = false;
        this.headerEndIndex = -1;
    }
}

// ==================== PendingResolver 类型 ====================

interface PendingResolver {
    requestId: string;
    enqueueResolve: ((response: ProxyResponse) => void) | null;
    enqueueReject: ((error: Error) => void) | null;
    waitForResolve: ((response: ProxyResponse) => void) | null;
    waitForReject: ((error: Error) => void) | null;
    timer: number;
    dcLabel: string;
    timeout: number;
    createdAt: number;
    resolvedBy?: string;
}

// ==================== ★★★ 组合模式：XHR 代理类 ★★★ ====================

// ★★★ 保存原始 XHR 的全局引用 ★★★
let OriginalXHRRef: any = null;

class ProxyXHR {
    // ★★★ 内部使用真正的 XHR 实例 ★★★
    private _xhr: XMLHttpRequest;
    private _proxyUrl: string = '';
    private _proxyMethod: string = 'GET';
    private _proxyHeaders: Record<string, string> = {};
    private _proxyBody: any = null;
    private _proxyId: string = '';
    private _proxyDcLabel: string = '';
    private _proxyTimeout: number = CONFIG.defaultTimeout;
    private _proxyResponse: ProxyResponse | null = null;
    private _isResolved: boolean = false;

    // ★★★ XHR 标准属性（完全可读写）★★★
    public status: number = 0;
    public statusText: string = '';
    public responseText: string = '';
    public response: any = null;
    public responseType: XMLHttpRequestResponseType = '';
    public responseURL: string = '';
    public timeout: number = CONFIG.defaultTimeout;
    public withCredentials: boolean = false;
    public upload: XMLHttpRequestUpload;
    public readyState: number = 0;

    // ★★★ 事件回调 ★★★
    public onreadystatechange: ((this: XMLHttpRequest, ev: Event) => any) | null = null;
    public onload: ((this: XMLHttpRequest, ev: Event) => any) | null = null;
    public onerror: ((this: XMLHttpRequest, ev: Event) => any) | null = null;
    public onabort: ((this: XMLHttpRequest, ev: Event) => any) | null = null;
    public ontimeout: ((this: XMLHttpRequest, ev: Event) => any) | null = null;
    public onloadend: ((this: XMLHttpRequest, ev: Event) => any) | null = null;
    public onloadstart: ((this: XMLHttpRequest, ev: Event) => any) | null = null;
    public onprogress: ((this: XMLHttpRequest, ev: Event) => any) | null = null;

    // ★★★ 事件监听器 ★★★
    private _listeners: Map<string, Set<EventListenerOrEventListenerObject>> = new Map();

    // ★★★ 静态变量 ★★★
    private static _self: NetworkProxy | null = null;
    private static _requestCounter: number = 0;

    // ★★★ 用于 instanceof 检查 ★★★
    public [Symbol.toStringTag]: string = 'XMLHttpRequest';

    constructor() {
        // ★★★ 使用保存的原始 XHR，避免无限递归 ★★★
        const OriginalXHR = OriginalXHRRef || XMLHttpRequest;
        this._xhr = new OriginalXHR();
        this.upload = this._xhr.upload;
        this.timeout = CONFIG.defaultTimeout;

        console.log('[NetworkProxy] 🏗️ ProxyXHR (组合模式) instance created');
    }

    // ==================== XHR 标准方法 ====================

    // ★★★ 修复: open 中不设置 timeout，在 send 中读取 ★★★
    open(method: string, url: string | URL, async: boolean = true, username?: string | null, password?: string | null): void {
        this._proxyMethod = method.toUpperCase();
        this._proxyUrl = typeof url === 'string' ? url : url.toString();

        console.log('[NetworkProxy] 📤📤📤 XHR OPEN: method=' + this._proxyMethod + ', url=' + this._proxyUrl);

        // ★★★ 判断是否需要代理 ★★★
        if (!ProxyXHR._self || !ProxyXHR._self.shouldProxy(this._proxyUrl)) {
            console.log('[NetworkProxy] 📤 ⏭️ XHR NOT PROXIED, going original');
            this._xhr.open(method, url, async, username || undefined, password || undefined);
            this._proxyDcLabel = '';
            return;
        }

        const dcLabel = ProxyXHR._self.extractDcLabelFromUrl(this._proxyUrl);
        if (!dcLabel) {
            console.log('[NetworkProxy] 📤 ❌ no dcLabel extracted, going original');
            this._xhr.open(method, url, async, username || undefined, password || undefined);
            this._proxyDcLabel = '';
            return;
        }

        console.log('[NetworkProxy] 📤 ✅ XHR will be PROXIED, dcLabel=' + dcLabel);
        this._proxyDcLabel = dcLabel;
        this._proxyId = this.generateId();

        // ★★★ 代理模式下不调用 _xhr.open()，完全由代理接管 ★★★
        this.readyState = 1;
        this._fireEvent('readystatechange');
    }

    setRequestHeader(name: string, value: string): void {
        if (this._proxyDcLabel) {
            // ★★★ 代理模式：保存 header ★★★
            this._proxyHeaders[name] = value;
        } else {
            // ★★★ 非代理模式：转发给真实 XHR ★★★
            this._xhr.setRequestHeader(name, value);
        }
    }

    // ★★★ 修复: 在 send 中读取 timeout（Axios 在 open 之后、send 之前设置）★★★
    send(body?: Document | XMLHttpRequestBodyInit | null): void {
        // ★★★ 在 send 中读取 timeout ★★★
        this._proxyTimeout = this.timeout || CONFIG.defaultTimeout;

        console.log('[NetworkProxy] 📤📤📤 XHR SEND called, url=' + this._proxyUrl + ', method=' + this._proxyMethod, 'timeout:', this._proxyTimeout);

        if (!this._proxyDcLabel) {
            console.log('[NetworkProxy] 📤 ⏭️ XHR SEND NOT PROXIED, going original');
            // ★★★ 绑定真实 XHR 的事件 ★★★
            this._bindOriginalXHREvents();
            this._xhr.send(body);
            return;
        }

        console.log('[NetworkProxy] 📤 ✅ XHR SEND PROXIED, requestId:', this._proxyId);

        this._proxyBody = body;
        this.readyState = 2;
        this._fireEvent('readystatechange');

        const request: ProxyRequest = {
            id: this._proxyId,
            url: this._proxyUrl,
            method: this._proxyMethod,
            headers: this._proxyHeaders,
            body: this._proxyBody,
            dcLabel: this._proxyDcLabel,
            timestamp: Date.now(),
            timeout: this._proxyTimeout,
        };

        // ★★★ 保存当前 this 引用 ★★★
        const self = this;

        if (!ProxyXHR._self) {
            this._simulateErrorResponse('NetworkProxy not initialized');
            return;
        }

        ProxyXHR._self.enqueueRequest(request)
            .then((response) => {
                if (self._isResolved) {
                    console.log('[NetworkProxy] ⏭️ XHR already resolved, skip, requestId:', self._proxyId);
                    return;
                }
                console.log('[NetworkProxy] 📤 XHR enqueueRequest resolved, requestId:', self._proxyId, 'status:', response.status);
                self._proxyResponse = response;
                self._simulateXHRResponse(response);
            })
            .catch((error) => {
                if (self._isResolved) {
                    console.log('[NetworkProxy] ⏭️ XHR already resolved, skip error, requestId:', self._proxyId);
                    return;
                }
                console.error('[NetworkProxy] ❌ XHR enqueueRequest rejected, requestId:', self._proxyId, error);
                self._simulateErrorResponse(error.message || 'Proxy error');
            });
    }

    abort(): void {
        if (this._proxyDcLabel) {
            this._isResolved = true;
            this.readyState = 0;
            this.status = 0;
            this.statusText = 'abort';
            this._fireEvent('abort');
            this._fireEvent('loadend');
        } else {
            this._xhr.abort();
        }
    }

    // ==================== 获取响应头 ====================

    getAllResponseHeaders(): string {
        if (this._proxyResponse) {
            return Object.entries(this._proxyResponse.headers)
                .map(([k, v]) => `${k}: ${v}`)
                .join('\r\n');
        }
        return this._xhr.getAllResponseHeaders();
    }

    getResponseHeader(name: string): string | null {
        if (this._proxyResponse) {
            return this._proxyResponse.headers[name] || null;
        }
        return this._xhr.getResponseHeader(name);
    }

    overrideMimeType(mime: string): void {
        this._xhr.overrideMimeType(mime);
    }

    // ==================== 事件绑定 ====================

    addEventListener(type: string, listener: EventListenerOrEventListenerObject, options?: boolean | AddEventListenerOptions): void {
        if (!this._listeners.has(type)) {
            this._listeners.set(type, new Set());
        }
        this._listeners.get(type)!.add(listener);

        if (!this._proxyDcLabel) {
            this._xhr.addEventListener(type, listener, options);
        }
    }

    removeEventListener(type: string, listener: EventListenerOrEventListenerObject, options?: boolean | EventListenerOptions): void {
        if (this._listeners.has(type)) {
            this._listeners.get(type)!.delete(listener);
        }
        if (!this._proxyDcLabel) {
            this._xhr.removeEventListener(type, listener, options);
        }
    }

    dispatchEvent(event: Event): boolean {
        // ★★★ 触发所有监听器 ★★★
        const listeners = this._listeners.get(event.type);
        if (listeners) {
            for (const listener of listeners) {
                if (typeof listener === 'function') {
                    listener(event);
                } else if (listener.handleEvent) {
                    listener.handleEvent(event);
                }
            }
        }

        // ★★★ 触发对应的 on* 回调 ★★★
        const handlerName = 'on' + event.type;
        const handler = (this as any)[handlerName];
        if (handler && typeof handler === 'function') {
            try {
                handler.call(this, event);
            } catch (e) {
                console.warn('[NetworkProxy] Error in ' + handlerName + ':', e);
            }
        }

        if (!this._proxyDcLabel) {
            return this._xhr.dispatchEvent(event);
        }
        return true;
    }

    // ==================== 私有方法 ====================

    private _fireEvent(type: string): void {
        try {
            const event = new Event(type);
            this.dispatchEvent(event);
        } catch (e) {
            console.warn('[NetworkProxy] Error firing event ' + type + ':', e);
        }
    }

    private _simulateErrorResponse(message: string): void {
        if (this._isResolved) return;
        this._isResolved = true;

        console.log('[NetworkProxy] 🔴 _simulateErrorResponse, requestId:', this._proxyId);

        this.status = 503;
        this.statusText = 'Service Unavailable';
        this.responseText = message;
        this.response = message;
        this.readyState = 4;

        this._fireEvent('readystatechange');
        this._fireEvent('error');
        this._fireEvent('loadend');
    }

    private _simulateXHRResponse(response: ProxyResponse): void {
        if (this._isResolved) return;
        this._isResolved = true;

        console.log('[NetworkProxy] 🟢 _simulateXHRResponse START, requestId:', this._proxyId, 'status:', response.status);

        let responseData: any = response.body;
        if (response.isBinary && response.body) {
            try {
                const binaryString = atob(response.body);
                const bytes = new Uint8Array(binaryString.length);
                for (let i = 0; i < binaryString.length; i++) {
                    bytes[i] = binaryString.charCodeAt(i);
                }
                responseData = bytes.buffer;
            } catch (e) {
                console.warn('[NetworkProxy] Failed to decode binary XHR response:', e);
                responseData = response.body;
            }
        }

        // ★★★ 直接设置属性（完全可控）★★★
        this.status = response.status;
        this.statusText = response.statusText;
        this.responseText = typeof responseData === 'string' ? responseData : '';
        this.response = responseData;
        this.readyState = 4;

        console.log('[NetworkProxy] ✅ XHR response ready, status:', this.status, 'readyState:', this.readyState);

        // ★★★ 触发 readystatechange ★★★
        this._fireEvent('readystatechange');

        // ★★★ 触发 load ★★★
        this._fireEvent('load');
        this._fireEvent('loadend');

        console.log('[NetworkProxy] 🟢 _simulateXHRResponse END, requestId:', this._proxyId);
    }

    private _bindOriginalXHREvents(): void {
        // ★★★ 非代理模式下，绑定真实 XHR 的事件到当前实例 ★★★
        const self = this;
        const events = ['readystatechange', 'load', 'error', 'abort', 'timeout', 'loadend', 'loadstart', 'progress'];

        for (const event of events) {
            this._xhr.addEventListener(event, function(e) {
                // ★★★ 同步真实 XHR 的属性 ★★★
                self.status = (self._xhr as any).status || 0;
                self.statusText = (self._xhr as any).statusText || '';
                self.responseText = (self._xhr as any).responseText || '';
                self.response = (self._xhr as any).response || null;
                self.readyState = (self._xhr as any).readyState || 0;

                // ★★★ 触发事件 ★★★
                self.dispatchEvent(e);
            });
        }
    }

    private generateId(): string {
        ProxyXHR._requestCounter++;
        return `proxy_${Date.now()}_${ProxyXHR._requestCounter}`;
    }

    // ==================== 静态初始化 ====================

    static setNetworkProxy(proxy: NetworkProxy | null): void {
        ProxyXHR._self = proxy;
    }
}

// ==================== 网络代理类 ====================

export class NetworkProxy {
    private static instance: NetworkProxy | null = null;

    private isInitialized = false;

    private proxyDcLabels = new Set<string>();
    private activeProxyDcLabels = new Set<string>();
    private creatingChannels = new Map<string, Promise<void>>();

    private requestQueues = new Map<string, ProxyRequest[]>();
    private processingFlags = new Map<string, boolean>();

    private pendingResolvers = new Map<string, PendingResolver>();

    private responseQueue: ProxyResponse[] = [];
    private responseBuilders = new Map<string, ResponseBuilder>();

    private requestCounter = 0;

    private constructor() {}

    static getInstance(): NetworkProxy {
        if (!NetworkProxy.instance) {
            NetworkProxy.instance = new NetworkProxy();
        }
        return NetworkProxy.instance;
    }

    // ==================== 初始化 ====================

    async init(retryCount: number = 3): Promise<void> {
        if ((window as any).__networkProxyInitialized) {
            console.log('[NetworkProxy] 🔵 init() called but already initialized globally, skip');
            if ((window as any).__networkProxy && (window as any).__networkProxy !== this) {
                const globalProxy = (window as any).__networkProxy;
                this.proxyDcLabels = globalProxy.proxyDcLabels;
                this.activeProxyDcLabels = globalProxy.activeProxyDcLabels;
                ProxyXHR.setNetworkProxy(this);
            }
            return;
        }

        if (this.isInitialized) {
            console.warn('[NetworkProxy] Already initialized');
            return;
        }

        console.log('[NetworkProxy] 🔵 init() called, isInitialized:', this.isInitialized);

        this.isInitialized = true;
        (window as any).__networkProxy = this;
        (window as any).__networkProxyInitialized = true;

        // ★★★ 设置 ProxyXHR 的静态引用 ★★★
        ProxyXHR.setNetworkProxy(this);

        console.log('[NetworkProxy] 🔵 Calling interceptFetch()...');
        this.interceptFetch();
        console.log('[NetworkProxy] 🔵 Calling interceptXHR()...');
        this.interceptXHR();

        setInterval(() => {
            for (const [dcLabel, queue] of this.requestQueues) {
                if (queue.length > 0) {
                    this.processQueue(dcLabel);
                }
            }
        }, CONFIG.queueProcessInterval);

        console.log('[NetworkProxy] ✅ Initialized');
    }

    // ==================== 核心判断 ====================

    shouldProxy(url: string): boolean {
        console.log('[NetworkProxy] 🔍 shouldProxy() called, url:', url);

        const dcLabel = this.extractDcLabelFromUrl(url);
        console.log('[NetworkProxy] 🔍 extracted dcLabel:', dcLabel);

        if (!dcLabel) {
            console.log('[NetworkProxy] 🔍 ❌ no dcLabel extracted, return false');
            return false;
        }

        for (const prefix of CONFIG.dcLabelPrefixes) {
            if (dcLabel.startsWith(prefix)) {
                this.proxyDcLabels.add(dcLabel);
                console.log('[NetworkProxy] 🔍 ✅ dcLabel "' + dcLabel + '" starts with "' + prefix + '", PROXY YES, recorded');
                return true;
            }
        }

        console.log('[NetworkProxy] 🔍 ❌ dcLabel "' + dcLabel + '" does NOT start with local_/remote_, PROXY NO');
        return false;
    }

    extractDcLabelFromUrl(url: string): string | null {
        console.log('[NetworkProxy] 🔍 extractDcLabelFromUrl() input:', url);
        try {
            const urlObj = new URL(url);
            const hostname = urlObj.hostname;
            console.log('[NetworkProxy] 🔍 extracted hostname:', hostname);
            return hostname;
        } catch (e) {
            console.log('[NetworkProxy] 🔍 ❌ extractDcLabelFromUrl error:', e);
            return null;
        }
    }

    isProxyDcLabel(dcLabel: string): boolean {
        const has = this.proxyDcLabels.has(dcLabel);
        console.log('[NetworkProxy] 🔍 isProxyDcLabel:', dcLabel, 'has:', has, 'proxyDcLabels:', Array.from(this.proxyDcLabels));
        return has;
    }

    isChannelOpen(dcLabel: string): boolean {
        return this.activeProxyDcLabels.has(dcLabel);
    }

    // ==================== 处理 DC 状态变化通知 ====================

    handleDcStateChange(dcLabel: string, imsDCStatus: number): void {
        const status = typeof imsDCStatus === 'string' ? parseInt(imsDCStatus, 10) : imsDCStatus;

        console.log('[NetworkProxy] 📢 DC state change:', dcLabel, 'status:', status);

        if (status === 1) {
            this.activeProxyDcLabels.add(dcLabel);
            console.log('[NetworkProxy] ✅ Channel OPEN, added to active set:', dcLabel);

            if (this.requestQueues.has(dcLabel) && this.requestQueues.get(dcLabel)!.length > 0) {
                this.processQueue(dcLabel);
            }
        }

        if (status === 3) {
            this.activeProxyDcLabels.delete(dcLabel);
            console.log('[NetworkProxy] ❌ Channel CLOSED, removed from active set:', dcLabel);
        }
    }

    // ==================== 拦截器 ====================

    private interceptFetch(): void {
        console.log('[NetworkProxy] 🔵 interceptFetch() called, typeof window.fetch:', typeof window.fetch);

        const originalFetch = window.fetch;
        const self = this;

        window.fetch = async (input: RequestInfo | URL, init?: RequestInit): Promise<Response> => {
            const url = typeof input === 'string' ? input : input instanceof URL ? input.toString() : input.url;
            const method = init?.method || 'GET';
            const timeout = CONFIG.defaultTimeout;

            console.log('[NetworkProxy] 📤📤📤 FETCH REQUEST DETECTED: method=' + method + ', url=' + url);

            if (self.shouldProxy(url)) {
                console.log('[NetworkProxy] 📤 ✅ FETCH will be PROXIED');
                try {
                    const dcLabel = self.extractDcLabelFromUrl(url)!;

                    console.log('[NetworkProxy] 📤 Fetch:', method, url, '→', dcLabel);

                    const headers = (init?.headers as Record<string, string>) || {};
                    const body = (init?.body as any) || null;

                    const request: ProxyRequest = {
                        id: self.generateId(),
                        url: url,
                        method: method,
                        headers: headers,
                        body: body,
                        dcLabel: dcLabel,
                        timestamp: Date.now(),
                        timeout: timeout,
                    };

                    console.log('[NetworkProxy] 📤 FETCH calling enqueueRequest, requestId:', request.id);
                    const response = await self.enqueueRequest(request);
                    console.log('[NetworkProxy] 📤 FETCH enqueueRequest returned, status:', response.status);

                    let responseBody: any = response.body;
                    const responseHeaders = new Headers(response.headers);

                    if (response.isBinary && response.body) {
                        try {
                            const binaryString = atob(response.body);
                            const bytes = new Uint8Array(binaryString.length);
                            for (let i = 0; i < binaryString.length; i++) {
                                bytes[i] = binaryString.charCodeAt(i);
                            }
                            responseBody = bytes.buffer;
                        } catch (e) {
                            console.warn('[NetworkProxy] Failed to decode binary response:', e);
                            responseBody = response.body;
                        }
                    }

                    return new Response(responseBody, {
                        status: response.status,
                        statusText: response.statusText,
                        headers: responseHeaders,
                    });

                } catch (error) {
                    console.error('[NetworkProxy] ❌ Fetch proxy failed:', error);
                    return originalFetch.call(window, input, init);
                }
            } else {
                console.log('[NetworkProxy] 📤 ⏭️ FETCH NOT PROXIED, going original');
            }

            return originalFetch.call(window, input, init);
        };

        console.log('[NetworkProxy] ✅ fetch interceptor installed, typeof window.fetch is now:', typeof window.fetch);
    }

    // ==================== ★★★ 组合模式：interceptXHR ★★★ ====================

    private interceptXHR(): void {
        console.log('[NetworkProxy] 🔵 interceptXHR() called, typeof window.XMLHttpRequest:', typeof window.XMLHttpRequest);

        // ★★★ 保存原始 XHR 引用 ★★★
        OriginalXHRRef = window.XMLHttpRequest;

        // ★★★ 替换为 ProxyXHR（组合模式）★★★
        window.XMLHttpRequest = ProxyXHR as any;

        // ★★★ 保持原型链，让 instanceof 检查通过 ★★★
        Object.setPrototypeOf(window.XMLHttpRequest, OriginalXHRRef);

        console.log('[NetworkProxy] ✅ XHR interceptor installed (组合模式)');
    }

    // ==================== 通道自动创建 ====================

    private async ensureChannel(dcLabel: string): Promise<void> {
        console.log('[NetworkProxy] 🔵 ensureChannel called:', dcLabel);

        if (this.activeProxyDcLabels.has(dcLabel)) {
            console.log('[NetworkProxy] ✅ Channel already OPEN:', dcLabel);
            return;
        }

        const creating = this.creatingChannels.get(dcLabel);
        if (creating) {
            console.log('[NetworkProxy] ⏳ Channel is being created, waiting...', dcLabel);
            await creating;
            return;
        }

        console.log('[NetworkProxy] 🔨 Creating channel:', dcLabel);
        const createPromise = this.doCreateChannel(dcLabel);
        this.creatingChannels.set(dcLabel, createPromise);

        try {
            await createPromise;
            console.log('[NetworkProxy] ✅ Channel created and OPEN:', dcLabel);
        } finally {
            this.creatingChannels.delete(dcLabel);
        }
    }

    // ★★★ 修复：移除内部超时，完全由 enqueueRequest 的总超时控制 ★★★
    private async doCreateChannel(dcLabel: string): Promise<void> {
        const parts = dcLabel.split('_');
        if (parts.length < 4) {
            throw new Error('Invalid dcLabel format: ' + dcLabel);
        }

        const useCase = parts[2];
        const dcId = parts.slice(3).join('_');

        console.log('[NetworkProxy] 📋 Creating channel with useCase:', useCase, 'dcId:', dcId);

        // ★★★ 不设置超时，让 enqueueRequest 的总超时控制 ★★★
        const result = await dcPlugin.createDC2({
            dcParams: [{
                DcId: dcId,
                UseCase: useCase,
                Subprotocol: 'http',
                Ordered: '1',
                MaxRetr: '0',
                MaxTime: '0',
                AutoAcceptDcSetup: '1',
            }]
        });

        if (result.code !== 1) {
            throw new Error('Create channel failed: ' + result.message);
        }

        console.log('[NetworkProxy] 📋 createDC2 success, waiting for OPEN...', dcLabel);

        // ★★★ 等待 OPEN 也不设置超时，由总超时控制 ★★★
        while (true) {
            if (this.activeProxyDcLabels.has(dcLabel)) {
                console.log('[NetworkProxy] ✅ Channel OPEN detected:', dcLabel);
                return;
            }
            await this.delay(100);
        }
    }

    // ==================== 队列管理 ====================

    enqueueRequest(request: ProxyRequest): Promise<ProxyResponse> {
        console.log('[NetworkProxy] 🟢 enqueueRequest START, requestId:', request.id, 'dcLabel:', request.dcLabel);

        return new Promise((resolve, reject) => {
            const dcLabel = request.dcLabel;
            const requestId = request.id;
            // ★★★ 使用总超时（从上层传入）★★★
            const timeout = request.timeout || CONFIG.defaultTimeout;

            console.log('[NetworkProxy] 🟢 enqueueRequest Promise created, requestId:', requestId, 'timeout:', timeout);

            const pending: PendingResolver = {
                requestId: requestId,
                enqueueResolve: resolve,
                enqueueReject: reject,
                waitForResolve: null,
                waitForReject: null,
                timer: 0,
                dcLabel: dcLabel,
                timeout: timeout,
                createdAt: Date.now(),
            };

            this.pendingResolvers.set(requestId, pending);
            console.log('[NetworkProxy] 🟢 pendingResolvers stored, requestId:', requestId, 'total pending:', this.pendingResolvers.size);

            // ★★★ 总超时定时器（覆盖整个流程：createDC + OPEN + HTTP）★★★
            let globalTimer: number | null = window.setTimeout(() => {
                console.warn('[NetworkProxy] ⏰ Total timeout for request:', dcLabel, 'request:', requestId, 'timeout:', timeout);

                const pendingNow = this.pendingResolvers.get(requestId);
                if (!pendingNow) {
                    console.warn('[NetworkProxy] ⏰ Total timeout: pending already removed, requestId:', requestId);
                    return;
                }

                if (pendingNow.timer === -1) {
                    console.warn('[NetworkProxy] ⏰ Total timeout: already timed out, requestId:', requestId);
                    return;
                }

                if (pendingNow.enqueueResolve === null) {
                    console.warn('[NetworkProxy] ⏰ Total timeout: enqueueResolve is null, requestId:', requestId);
                    return;
                }

                console.log('[NetworkProxy] ⏰ Total timeout: calling enqueueResolve with 504, requestId:', requestId);
                const resolveFn = pendingNow.enqueueResolve;
                pendingNow.enqueueResolve = null;
                pendingNow.enqueueReject = null;
                pendingNow.resolvedBy = 'total_timeout';
                resolveFn({
                    status: 504,
                    statusText: 'Gateway Timeout',
                    headers: {
                        'Content-Type': 'text/plain',
                        'X-DC-Error': 'Total timeout',
                    },
                    body: `Request total timeout after ${timeout}ms (createDC + channelOpen + HTTP)`,
                    isBinary: false,
                });
                pendingNow.timer = -1;
                console.log('[NetworkProxy] ⏰ Total timeout: enqueueResolve called, requestId:', requestId);
            }, timeout);

            pending.timer = globalTimer as any;

            console.log('[NetworkProxy] 🟢 Calling ensureChannel, requestId:', requestId);
            this.ensureChannel(dcLabel)
                .then(() => {
                    console.log('[NetworkProxy] 🟢 ensureChannel success, requestId:', requestId);
                    if (globalTimer) {
                        clearTimeout(globalTimer);
                        globalTimer = null;
                    }

                    const pendingCheck = this.pendingResolvers.get(requestId);
                    if (pendingCheck && pendingCheck.timer === -1) {
                        console.log('[NetworkProxy] 🟢 ensureChannel success but already timed out, requestId:', requestId);
                        this.pendingResolvers.delete(requestId);
                        return;
                    }

                    if (!this.requestQueues.has(dcLabel)) {
                        this.requestQueues.set(dcLabel, []);
                    }
                    this.requestQueues.get(dcLabel)!.push(request);

                    const pendingUpdate = this.pendingResolvers.get(requestId);
                    if (pendingUpdate) {
                        pendingUpdate.timer = 0;
                    }

                    console.log('[NetworkProxy] 🟢 Request queued, calling processQueue, requestId:', requestId);
                    this.processQueue(dcLabel);
                })
                .catch((error) => {
                    console.log('[NetworkProxy] 🔴 ensureChannel failed, requestId:', requestId, error);
                    if (globalTimer) {
                        clearTimeout(globalTimer);
                        globalTimer = null;
                    }

                    const pendingCatch = this.pendingResolvers.get(requestId);
                    if (pendingCatch && pendingCatch.timer !== -1) {
                        if (pendingCatch.enqueueResolve) {
                            console.log('[NetworkProxy] 🔴 ensureChannel failed: calling enqueueResolve with 503, requestId:', requestId);
                            const resolveFn = pendingCatch.enqueueResolve;
                            pendingCatch.enqueueResolve = null;
                            pendingCatch.enqueueReject = null;
                            pendingCatch.resolvedBy = 'channel_failed';
                            resolveFn({
                                status: 503,
                                statusText: 'Service Unavailable',
                                headers: { 'Content-Type': 'text/plain' },
                                body: 'DC channel creation failed: ' + error.message,
                                isBinary: false,
                            });
                            console.log('[NetworkProxy] 🔴 enqueueResolve called with 503, requestId:', requestId);
                        } else {
                            console.warn('[NetworkProxy] 🔴 ensureChannel failed but enqueueResolve is null, requestId:', requestId);
                        }
                        this.pendingResolvers.delete(requestId);
                    } else {
                        console.warn('[NetworkProxy] 🔴 ensureChannel failed but pending already processed, requestId:', requestId);
                    }
                });
        });
    }

    private async processQueue(dcLabel: string): Promise<void> {
        if (this.processingFlags.get(dcLabel)) return;

        const queue = this.requestQueues.get(dcLabel);
        if (!queue || queue.length === 0) return;

        this.processingFlags.set(dcLabel, true);
        console.log('[NetworkProxy] 🟣 processQueue START, dcLabel:', dcLabel, 'queue length:', queue.length);

        try {
            while (queue.length > 0) {
                const request = queue.shift()!;
                console.log('[NetworkProxy] 🟣 processQueue processing request:', request.id);
                try {
                    console.log('[NetworkProxy] 🟣 Calling sendRequest, requestId:', request.id);
                    const response = await this.sendRequest(request);
                    console.log('[NetworkProxy] 🟣 sendRequest returned, requestId:', request.id, 'status:', response.status);

                    const pending = this.pendingResolvers.get(request.id);
                    if (pending) {
                        if (pending.timer === -1) {
                            console.log('[NetworkProxy] 🟣 pending already timed out, skip, requestId:', request.id);
                            this.pendingResolvers.delete(request.id);
                            continue;
                        }

                        // ★★★ 清除主定时器 ★★★
                        if (pending.timer && pending.timer > 0) {
                            clearTimeout(pending.timer);
                            pending.timer = 0;
                        }

                        // ★★★ 清除备用定时器 ★★★
                        if ((pending as any)._backupTimer) {
                            clearTimeout((pending as any)._backupTimer);
                            (pending as any)._backupTimer = null;
                        }

                        if (pending.enqueueResolve) {
                            console.log('[NetworkProxy] 🟣 Calling enqueueResolve from processQueue, requestId:', request.id);
                            const resolveFn = pending.enqueueResolve;
                            pending.enqueueResolve = null;
                            pending.enqueueReject = null;
                            pending.resolvedBy = 'processQueue';
                            resolveFn(response);
                            console.log('[NetworkProxy] 🟣 enqueueResolve called from processQueue, requestId:', request.id);
                        } else {
                            console.warn('[NetworkProxy] 🟣 enqueueResolve is null in processQueue, requestId:', request.id);
                        }
                        this.pendingResolvers.delete(request.id);
                    } else {
                        console.warn('[NetworkProxy] 🟣 No pending found in processQueue, requestId:', request.id);
                    }
                } catch (error) {
                    console.error('[NetworkProxy] ❌ Request failed:', request.id, error);
                    const pending = this.pendingResolvers.get(request.id);
                    if (pending) {
                        if (pending.timer === -1) {
                            this.pendingResolvers.delete(request.id);
                            continue;
                        }
                        if (pending.timer && pending.timer > 0) {
                            clearTimeout(pending.timer);
                            pending.timer = 0;
                        }
                        // ★★★ 清除备用定时器 ★★★
                        if ((pending as any)._backupTimer) {
                            clearTimeout((pending as any)._backupTimer);
                            (pending as any)._backupTimer = null;
                        }
                        if (pending.enqueueReject) {
                            pending.enqueueReject(error as Error);
                        }
                        this.pendingResolvers.delete(request.id);
                    }
                }
                await this.delay(CONFIG.retryDelayMs);
            }
        } finally {
            this.processingFlags.set(dcLabel, false);
            console.log('[NetworkProxy] 🟣 processQueue END, dcLabel:', dcLabel);
        }
    }

    // ==================== 发送逻辑 ====================

    private async sendRequest(request: ProxyRequest): Promise<ProxyResponse> {
        console.log('[NetworkProxy] 🟡 sendRequest START, requestId:', request.id);
        await this.waitForBufferSpace(request.dcLabel);
        const requestData = await this.buildHttpRequest(request);
        await this.sendChunkedWithCallback(request.dcLabel, requestData);
        console.log('[NetworkProxy] 🟡 sendRequest calling waitForResponse, requestId:', request.id);
        const response = await this.waitForResponse(request.id);
        console.log('[NetworkProxy] 🟡 sendRequest waitForResponse returned, requestId:', request.id, 'status:', response.status);
        return response;
    }

    private waitForResponse(requestId: string): Promise<ProxyResponse> {
        console.log('[NetworkProxy] 🟡 waitForResponse START, requestId:', requestId);
        return new Promise((resolve, reject) => {
            const pending = this.pendingResolvers.get(requestId);
            if (!pending) {
                console.error('[NetworkProxy] 🟡 No pending found in waitForResponse, requestId:', requestId);
                reject(new Error('No pending resolver found for request: ' + requestId));
                return;
            }

            const timeout = pending.timeout || CONFIG.defaultTimeout;

            pending.waitForResolve = resolve;
            pending.waitForReject = reject;

            console.log('[NetworkProxy] ⏳ Waiting for HTTP response, requestId:', requestId, '备用超时:', timeout, 'ms');

            const backupTimer = setTimeout(() => {
                console.warn('[NetworkProxy] ⏰ 备用超时触发, requestId:', requestId);

                if (pending.waitForResolve === null) {
                    console.log('[NetworkProxy] ⏰ 备用超时但已处理, requestId:', requestId);
                    return;
                }

                const errorResponse: ProxyResponse = {
                    status: 504,
                    statusText: 'Gateway Timeout',
                    headers: {
                        'Content-Type': 'text/plain',
                        'X-DC-Error': 'Response timeout (备用超时)',
                    },
                    body: `Response timeout after ${timeout}ms`,
                    isBinary: false,
                };

                // ★★★ 1. 调用 waitForResolve（返回给 sendRequest）★★★
                const waitResolveFn = pending.waitForResolve;
                pending.waitForResolve = null;
                pending.waitForReject = null;
                waitResolveFn(errorResponse);

                // ★★★ 2. 调用 enqueueResolve（返回给 XHR/上层）★★★
                if (pending.enqueueResolve) {
                    console.log('[NetworkProxy] ⏰ 备用超时: 调用 enqueueResolve 返回504给上层, requestId:', requestId);
                    const enqueueResolveFn = pending.enqueueResolve;
                    pending.enqueueResolve = null;
                    pending.enqueueReject = null;
                    pending.resolvedBy = 'backup_timeout';
                    enqueueResolveFn(errorResponse);
                }

                pending.timer = -1;
                this.pendingResolvers.delete(requestId);

                console.log('[NetworkProxy] ✅ 备用超时返回504, requestId:', requestId);
            }, timeout);

            (pending as any)._backupTimer = backupTimer;
        });
    }

    private async sendChunkedWithCallback(dcLabel: string, data: Uint8Array): Promise<void> {
        let offset = 0;

        while (offset < data.length) {
            let freeBufferSize = 0;
            try {
                const result = await dcPlugin.queryDCFreeBufferSize({ dcLabel });
                freeBufferSize = result.data?.freeBufferSize || 0;
            } catch (e) {}

            const maxAllowed = CONFIG.maxChunkSize - freeBufferSize;
            let chunkSize = Math.min(CONFIG.maxChunkSize, data.length - offset);

            if (maxAllowed > CONFIG.minChunkSize) {
                chunkSize = Math.min(maxAllowed, chunkSize);
            } else if (freeBufferSize < CONFIG.minChunkSize) {
                await this.delay(CONFIG.retryDelayMs);
                continue;
            }

            if (chunkSize <= 0) {
                await this.delay(CONFIG.retryDelayMs);
                continue;
            }

            const chunk = data.slice(offset, offset + chunkSize);
            const arrayBuffer = chunk.buffer.slice(chunk.byteOffset, chunk.byteOffset + chunk.byteLength);

            await dcPlugin.sendData({
                dcLabel: dcLabel,
                data: arrayBuffer,
            });

            offset += chunkSize;
        }
    }

    // ==================== 响应处理 ====================

    handleResponse(data: { dcLabel: string; message: ArrayBuffer }): void {
        try {
            const rawData = new Uint8Array(data.message);
            const dcLabel = data.dcLabel;

            let builder = this.responseBuilders.get(dcLabel);
            if (!builder) {
                builder = new ResponseBuilder();
                this.responseBuilders.set(dcLabel, builder);
            }

            const isComplete = builder.appendData(rawData);

            if (isComplete) {
                const completeData = builder.getCompleteData();
                if (completeData) {
                    const isBinary = builder.isBinaryResponse();
                    const response = this.parseHttpResponse(completeData, isBinary);
                    if (response) {
                        let found = false;
                        for (const [requestId, pending] of this.pendingResolvers) {
                            if (pending.dcLabel === dcLabel &&
                                pending.timer !== -1 &&
                                pending.waitForResolve !== null) {

                                // ★★★ 清除主定时器 ★★★
                                if (pending.timer && pending.timer > 0) {
                                    clearTimeout(pending.timer);
                                }

                                // ★★★ 清除备用定时器 ★★★
                                if ((pending as any)._backupTimer) {
                                    clearTimeout((pending as any)._backupTimer);
                                    (pending as any)._backupTimer = null;
                                }

                                console.log('[NetworkProxy] 📥 handleResponse: calling waitForResolve, requestId:', requestId);
                                const resolveFn = pending.waitForResolve;
                                pending.waitForResolve = null;
                                pending.waitForReject = null;
                                pending.resolvedBy = 'handleResponse';
                                resolveFn(response);
                                // ★★★ 不删除 pendingResolvers，让 processQueue 负责调用 enqueueResolve 回传给上层 XHR ★★★
                                found = true;
                                console.log('[NetworkProxy] ✅ Response resolved (waitForResolve), pending kept for processQueue:', dcLabel, 'requestId:', requestId);
                                break;
                            }
                        }

                        if (!found) {
                            this.responseQueue.push(response);
                            console.log('[NetworkProxy] 📦 Response queued for:', dcLabel);
                        }
                    }
                }
                this.responseBuilders.delete(dcLabel);
            }

        } catch (e) {
            console.error('[NetworkProxy] ❌ Handle response error:', e);
        }
    }

    // ==================== 核心：构造 HTTP 请求 ====================

    private async buildHttpRequest(request: ProxyRequest): Promise<Uint8Array> {
        const { method, url, headers, body } = request;

        let path: string;
        let host: string;
        try {
            const urlObj = new URL(url);
            path = urlObj.pathname + urlObj.search;
            host = urlObj.host;
        } catch (e) {
            path = url;
            host = '';
        }

        const headerLines: string[] = [];
        headerLines.push(`${method} ${path} HTTP/1.1`);
        if (host) {
            headerLines.push(`Host: ${host}`);
        }

        const headerMap: Record<string, string> = {};
        for (const [key, value] of Object.entries(headers)) {
            const lowerKey = key.toLowerCase();
            if (lowerKey !== 'host' && lowerKey !== 'connection' &&
                lowerKey !== 'content-length') {
                headerMap[key] = value;
            }
        }

        let bodyBytes: Uint8Array = new Uint8Array(0);
        let contentType: string | null = null;

        if (body) {
            if (body instanceof ArrayBuffer) {
                bodyBytes = new Uint8Array(body);
                contentType = headers['Content-Type'] || 'application/octet-stream';
            } else if (body instanceof Blob) {
                const arrayBuffer = await (body as Blob).arrayBuffer();
                bodyBytes = new Uint8Array(arrayBuffer);
                contentType = (body as Blob).type || 'application/octet-stream';
            } else if (body instanceof File) {
                const arrayBuffer = await (body as File).arrayBuffer();
                bodyBytes = new Uint8Array(arrayBuffer);
                contentType = (body as File).type || 'application/octet-stream';
            } else if (body instanceof FormData) {
                const result = await this.buildMultipartFormData(body as FormData);
                bodyBytes = result.bodyBytes;
                contentType = result.contentType;
            } else if (typeof body === 'string') {
                bodyBytes = new TextEncoder().encode(body);
                contentType = headers['Content-Type'] || 'text/plain; charset=utf-8';
            } else {
                const bodyStr = JSON.stringify(body);
                bodyBytes = new TextEncoder().encode(bodyStr);
                contentType = headers['Content-Type'] || 'application/json; charset=utf-8';
            }
        }

        for (const [key, value] of Object.entries(headerMap)) {
            if (key.toLowerCase() !== 'content-type') {
                headerLines.push(`${key}: ${value}`);
            }
        }

        if (contentType) {
            headerLines.push(`Content-Type: ${contentType}`);
        }
        headerLines.push(`Content-Length: ${bodyBytes.length}`);
        headerLines.push('');
        headerLines.push('');

        const headerBytes = new TextEncoder().encode(headerLines.join('\r\n'));

        const result = new Uint8Array(headerBytes.length + bodyBytes.length);
        result.set(headerBytes, 0);
        result.set(bodyBytes, headerBytes.length);

        return result;
    }

    private async buildMultipartFormData(formData: FormData): Promise<{ bodyBytes: Uint8Array; contentType: string }> {
        const boundary = this.generateBoundary();
        const CRLF = '\r\n';
        const parts: Uint8Array[] = [];

        const entries: Array<[string, any]> = [];
        (formData as any).forEach((value: any, key: string) => {
            entries.push([key, value]);
        });

        for (const [key, value] of entries) {
            parts.push(new TextEncoder().encode(`--${boundary}${CRLF}`));

            if (value instanceof File) {
                const file = value as File;
                const filename = file.name || 'blob';
                const mimeType = file.type || 'application/octet-stream';

                parts.push(new TextEncoder().encode(
                    `Content-Disposition: form-data; name="${key}"; filename="${filename}"${CRLF}`
                ));
                parts.push(new TextEncoder().encode(`Content-Type: ${mimeType}${CRLF}`));
                parts.push(new TextEncoder().encode(CRLF));

                const arrayBuffer = await file.arrayBuffer();
                parts.push(new Uint8Array(arrayBuffer));
                parts.push(new TextEncoder().encode(CRLF));

            } else if (value instanceof Blob) {
                const blob = value as Blob;
                const mimeType = blob.type || 'application/octet-stream';

                parts.push(new TextEncoder().encode(
                    `Content-Disposition: form-data; name="${key}"; filename="blob"${CRLF}`
                ));
                parts.push(new TextEncoder().encode(`Content-Type: ${mimeType}${CRLF}`));
                parts.push(new TextEncoder().encode(CRLF));

                const arrayBuffer = await blob.arrayBuffer();
                parts.push(new Uint8Array(arrayBuffer));
                parts.push(new TextEncoder().encode(CRLF));

            } else {
                const strValue = String(value);
                parts.push(new TextEncoder().encode(
                    `Content-Disposition: form-data; name="${key}"${CRLF}${CRLF}`
                ));
                parts.push(new TextEncoder().encode(strValue));
                parts.push(new TextEncoder().encode(CRLF));
            }
        }

        parts.push(new TextEncoder().encode(`--${boundary}--${CRLF}`));

        const totalLength = parts.reduce((sum, part) => sum + part.length, 0);
        const result = new Uint8Array(totalLength);
        let offset = 0;
        for (const part of parts) {
            result.set(part, offset);
            offset += part.length;
        }

        const contentType = `multipart/form-data; boundary=${boundary}`;

        return { bodyBytes: result, contentType };
    }

    private generateBoundary(): string {
        const chars = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789';
        let boundary = '----WebAppBoundary_';
        for (let i = 0; i < 16; i++) {
            boundary += chars.charAt(Math.floor(Math.random() * chars.length));
        }
        return boundary;
    }

    private parseHttpResponse(data: Uint8Array, isBinary: boolean): ProxyResponse | null {
        try {
            if (isBinary) {
                const base64Data = this.uint8ArrayToBase64(data);
                let status = 200;
                let statusText = 'OK';
                const headers: Record<string, string> = {};

                try {
                    const text = new TextDecoder('utf-8').decode(data.slice(0, 1000));
                    const lines = text.split('\r\n');
                    const statusLine = lines[0];
                    const statusMatch = statusLine.match(/HTTP\/\d+\.\d+\s+(\d+)\s+(.*)/);
                    if (statusMatch) {
                        status = parseInt(statusMatch[1], 10);
                        statusText = statusMatch[2] || 'OK';
                    }
                    for (const line of lines) {
                        if (line === '') break;
                        const colonIndex = line.indexOf(':');
                        if (colonIndex > 0) {
                            headers[line.substring(0, colonIndex).trim()] =
                                line.substring(colonIndex + 1).trim();
                        }
                    }
                } catch (e) {
                    // 忽略解析错误
                }

                return {
                    status,
                    statusText,
                    headers,
                    body: base64Data,
                    isBinary: true,
                };
            }

            const text = new TextDecoder('utf-8').decode(data);
            const lines = text.split('\r\n');

            if (lines.length === 0) return null;

            const statusLine = lines[0];
            const statusMatch = statusLine.match(/HTTP\/\d+\.\d+\s+(\d+)\s+(.*)/);
            if (!statusMatch) return null;

            const status = parseInt(statusMatch[1], 10);
            const statusText = statusMatch[2] || '';

            const headers: Record<string, string> = {};
            let bodyStart = 0;

            for (let i = 1; i < lines.length; i++) {
                const line = lines[i];
                if (line === '') {
                    bodyStart = i + 1;
                    break;
                }
                const colonIndex = line.indexOf(':');
                if (colonIndex > 0) {
                    headers[line.substring(0, colonIndex).trim()] =
                        line.substring(colonIndex + 1).trim();
                }
            }

            const body = lines.slice(bodyStart).join('\r\n');

            return {
                status,
                statusText,
                headers,
                body,
                isBinary: false,
            };

        } catch (e) {
            console.error('[NetworkProxy] Parse HTTP response error:', e);
            return null;
        }
    }

    private async waitForBufferSpace(dcLabel: string): Promise<void> {
        for (let i = 0; i < 50; i++) {
            try {
                const result = await dcPlugin.queryDCFreeBufferSize({ dcLabel });
                const freeBufferSize = result.data?.freeBufferSize || 0;
                if (freeBufferSize > CONFIG.minChunkSize) {
                    return;
                }
                await this.delay(CONFIG.retryDelayMs);
            } catch (e) {
                await this.delay(CONFIG.retryDelayMs);
            }
        }
    }

    private generateId(): string {
        this.requestCounter++;
        return `proxy_${Date.now()}_${this.requestCounter}`;
    }

    private delay(ms: number): Promise<void> {
        return new Promise(resolve => setTimeout(resolve, ms));
    }

    private uint8ArrayToBase64(uint8Array: Uint8Array): string {
        let binary = '';
        for (let i = 0; i < uint8Array.length; i++) {
            binary += String.fromCharCode(uint8Array[i]);
        }
        return btoa(binary);
    }

    // ==================== 生命周期 ====================

    destroy(): void {
        console.log('[NetworkProxy] 🔥🔥🔥 destroy() called!');

        for (const [requestId, pending] of this.pendingResolvers) {
            if (pending.timer && pending.timer > 0) {
                clearTimeout(pending.timer);
                pending.timer = 0;
            }
            if (pending.waitForReject) {
                pending.waitForReject(new Error('Proxy destroyed'));
            }
            if (pending.enqueueReject) {
                pending.enqueueReject(new Error('Proxy destroyed'));
            }
        }
        this.pendingResolvers.clear();

        this.requestQueues.clear();
        this.processingFlags.clear();
        this.responseQueue = [];
        this.responseBuilders.clear();
        this.proxyDcLabels.clear();
        this.activeProxyDcLabels.clear();
        this.creatingChannels.clear();

        this.isInitialized = false;
        ProxyXHR.setNetworkProxy(null);
        (window as any).__networkProxy = null;
        (window as any).__networkProxyInitialized = false;
        console.log('[NetworkProxy] Destroyed');
    }

    getStats(): string {
        return `
NetworkProxy Stats:
  - Proxy DcLabels: ${Array.from(this.proxyDcLabels).join(', ')}
  - Active Proxy DcLabels: ${Array.from(this.activeProxyDcLabels).join(', ')}
  - Creating Channels: ${Array.from(this.creatingChannels.keys()).join(', ')}
  - Request Queues: ${this.requestQueues.size}
  - Pending Resolvers: ${this.pendingResolvers.size}
  - Response Queue: ${this.responseQueue.length}
        `.trim();
    }
}

export const networkProxy = NetworkProxy.getInstance();