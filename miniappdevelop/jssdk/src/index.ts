// ==================== 类型定义 ====================

interface DcParam {
    DcId: string;
    StreamId?: string;
    UseCase: string;
    Subprotocol?: string;
    Ordered?: string;
    MaxRetr?: string;
    MaxTime?: string;
    Priority?: string;
    AutoAcceptDcSetup?: string;
    Bandwidth?: string;
    QosHint?: string;
}

interface CreateDC2Params {
    dcParams: DcParam[];
}

interface CreateDCParams {
    dcLabels: string[];
    description?: string;
}

interface CloseDCParams {
    dcLabel: string[];
}

interface SendDataParams {
    dcLabel: string;
    data: string | ArrayBuffer;
}

interface QueryDCMaxDataSizeParams {
    dcLabel: string;
}

interface QueryDCFreeBufferSizeParams {
    dcLabel: string;
}

interface QueryImagesParams {
    path?: string;
    offset?: string;
    size?: string;
}

interface QueryVideosParams {
    path?: string;
    offset?: string;
    size?: string;
}

interface CheckFilesParams {
    path: string[];
}

interface QueryFilesParams {
    contentType: string;
    path?: string;
    offset?: string;
    size?: string;
}

interface SaveFileParams {
    file: ArrayBuffer;
    filePathType: number;
    filePath: string;
}

interface SaveKeyValueDataParams {
    key: string;
    value: string;
}

interface GetKeyValueDataParams {
    key: string;
}

interface DeleteKeyValueParams {
    key: string;
}

interface DealCompressedFileParams {
    compressedFilePath: string;
    filePathType: number;
    filePath?: string;
    type?: string;
}

interface ChooseImageParams {
    count?: number;
}

interface GetFileByPathParams {
    filePath: string;
}

interface OpenAppParams {
    /** 目标APP的类型，支持：openBrowser, openCameraToPic, openCameraToVideo, openOtherApp, openFile, openMap, openMiniApp, openMiniAppWithParams */
    jumpType: string;
    /** 目标页面所需的必要参数 */
    extras?: string;
}

interface SetWindowParams {
    isWindowFront?: boolean;
    isFullScreen?: boolean;
    statusBarColor?: string;
    barEnable?: boolean;
    navigationBarColor?: string;
    transparency?: number;
    /** 是否隐藏窗口（切到后台），true:切到后台; false:切到前台 */
    hidden?: boolean;
    /** 标题栏返回箭头、文字和胶囊按钮颜色，0:黑色，1:白色 */
    statusBarTitleColor?: string;
    /** 设置页面标题 */
    pageName?: string;
    /** 窗口宽度占屏幕宽度的百分比(0.1~1.0)，-1表示全屏(等同于1.0) */
    width?: number;
    /** 窗口高度占屏幕高度的百分比(0.1~1.0)，-1表示全屏(等同于1.0) */
    height?: number;
    /** 窗口左上角X坐标占屏幕宽度的百分比(0.0~1.0) */
    windowStart?: number;
    /** 窗口左上角Y坐标占屏幕高度的百分比(0.0~1.0) */
    windowTop?: number;
    /** 是否关闭引导程序，仅在窗口大小位置4个字段都配置时生效 */
    closeGuidePage?: boolean;
}

interface GetContactListParams {
    offset?: string;
    limit?: string;
}

interface SetSpeakerphoneParams {
    speakerphoneOn: boolean;
}

interface SetMutedParams {
    muted: boolean;
}

interface GetContactNameParams {
    contactNumber: string;
}

interface AddOrEditContactParams {
    contactMode: string;
    name: string;
    number: string;
}

interface ShowAlertDialogParams {
    title?: string;
    message: string;
    positiveText?: string;
    negativeText?: string;
}

interface ShowToastParams {
    message: string;
    duration?: number;
}

interface ShowLoadingViewParams {
    loadingText?: string;
    maxDuration?: number;
}

interface SaveCallCacheDataParams {
    data: any;
}

interface PlayDtmfToneParams {
    license: string;
    digit: string;
}

interface ScreenShareParams {
    license: string;
}

interface OpenSketchBoardParams {
    paintColor?: string;
    paintWidth?: string;
}

interface AddDrawingInfoParams {
    drawingInfo: any;
}

interface ExpandingCapacityRegisterParams {
    providerModules: any[];
}

interface ExpandingCapacityRequestParams {
    provider: string;
    module: string;
    func: string;
    data?: any;
}

interface ApiResponse<T = any> {
    code: number;
    message: string;
    data: T;
}

interface DeleteFileParams {
    /** 私有目录下的文件路径 */
    path: string;
}

interface SearchFilesParams {
    /** 搜索关键词，JSON数组字符串，如 '["项目申请书","pdf"]' */
    keywords: string;
}

interface SetSystemApiLicenseParams {
    /** 授权许可证 */
    license: string;
    /** 需要授权的系统API名称，如 "getUserMedia" */
    api: string;
}

// ==================== 导入 dsbridge ====================

// @ts-ignore
import mDCBridge from './bridge.js';
import { networkProxy } from './networkProxy';

// ==================== 工具函数 ====================

function callDcEvent(func: string, params: any, callback?: (v: any) => void): void {
    mDCBridge.call(
        'async',
        {
            event: 'DcEvent',
            function: func,
            params: params
        },
        callback || function (v: any) { }
    );
}

function callFileEvent(func: string, params: any, callback?: (v: any) => void): void {
    mDCBridge.call(
        'async',
        {
            event: 'FileEvent',
            function: func,
            params: params
        },
        callback || function (v: any) { }
    );
}

function callMiniAppEvent(func: string, params: any, callback?: (v: any) => void): void {
    mDCBridge.call(
        'async',
        {
            event: 'MiniAppEvent',
            function: func,
            params: params
        },
        callback || function (v: any) { }
    );
}

function callScreenShareEvent(func: string, params: any, callback?: (v: any) => void): void {
    mDCBridge.call(
        'async',
        {
            event: 'ScreenShareEvent',
            function: func,
            params: params
        },
        callback || function (v: any) { }
    );
}

function callECEvent(func: string, params: any, callback?: (v: any) => void): void {
    mDCBridge.call(
        'async',
        {
            event: 'ECEvent',
            function: func,
            params: params
        },
        callback || function (v: any) { }
    );
}

// ==================== 回调存储 ====================

let dcNotifiedCallback: ((data: any, resolve: any, reject: any, progress: any) => void) | null = null;
let messageNotifiedCallback: ((data: any, resolve: any, reject: any, progress: any) => void) | null = null;
let callStateNotifiedCallback: ((data: any, resolve: any, reject: any, progress: any) => void) | null = null;
let lifecycleChangedCallback: ((data: any, resolve: any, reject: any, progress: any) => void) | null = null;
let drawingInfoNotifyCallback: ((data: any, resolve: any, reject: any, progress: any) => void) | null = null;
let sketchStatusNotifyCallback: ((data: any, resolve: any, reject: any, progress: any) => void) | null = null;
let startAdverseAppResponseNotifyCallback: ((data: any, resolve: any, reject: any, progress: any) => void) | null = null;
let audioDeviceChangeNotifyCallback: ((data: any, resolve: any, reject: any, progress: any) => void) | null = null;
let imeHeightNotifyCallback: ((data: any, resolve: any, reject: any, progress: any) => void) | null = null;
let expandingCapacityNotifyCallback: ((data: any, resolve: any, reject: any, progress: any) => void) | null = null;
let onAlertDialogButtonClickedCallback: ((result: number, resolve: any, reject: any, progress: any) => void) | null = null;

let alertDialogElement: HTMLDivElement | null = null;
let toastTimer: number | null = null;
let toastElement: HTMLDivElement | null = null;

// ==================== RTC 实例管理 ====================

/** 存储 dcLabel -> RTCPeerConnection 的映射 */
const rtcInstanceMap = new Map<string, RTCPeerConnection>();

function getRTCPeerConnectionByLabel(dcLabel: string): RTCPeerConnection | null {
    return rtcInstanceMap.get(dcLabel) || null;
}

function registerRTCInstance(rtc: RTCPeerConnection, labels: string[]): void {
    labels.forEach(label => {
        rtcInstanceMap.set(label, rtc);
    });
}

function unregisterRTCInstance(labels: string[]): void {
    labels.forEach(label => {
        rtcInstanceMap.delete(label);
    });
}

// ==================== 注册回调 ====================

function registerTelecomCallbacks(): void {
    // 4.1 DC状态变化通知
    mDCBridge.register('dataChannelNotify', (msg: any) => {
        try {
            const data = typeof msg === 'string' ? JSON.parse(msg) : msg;
            const label = data.dcLabel;
            const imsDCStatus = data.imsDCStatus;

            // ★★★ 1. 优先检查是否是代理通道（由 networkProxy 管理，不向外通知）★★★
            const proxy = (window as any).__networkProxy;
            if (proxy && proxy.isProxyDcLabel(label)) {
                // ★★★ 通知 networkProxy 状态变化（用于等待 OPEN）★★★
                proxy.handleDcStateChange(label, imsDCStatus);
                // ★★★ 代理通道的状态变化不向外通知 ★★★
                return;
            }

            // 2. 检查是否是 RTCPeerConnection 管理的 DC
            const rtcInstance = getRTCPeerConnectionByLabel(label);
            if (rtcInstance !== null) {
                rtcInstance.handleDataChannelNotify(data);
                return;
            }

            // 3. 业务回调
            if (dcNotifiedCallback) {
                dcNotifiedCallback(data, () => { }, () => { }, () => { });
            }
        } catch (e) {
            console.error('dataChannelNotify parse error:', e);
        }
    });

    // 4.2 DC通道收到消息
    mDCBridge.register('messageNotify', (msg: any) => {
        try {
            const data = typeof msg === 'string' ? JSON.parse(msg) : msg;
            const dcLabel = data.dcLabel;

            const proxy = (window as any).__networkProxy;
            if (proxy && proxy.isProxyDcLabel(dcLabel) && data.message) {
                try {
                    const binaryString = atob(data.message);
                    const bytes = new Uint8Array(binaryString.length);
                    for (let i = 0; i < binaryString.length; i++) {
                        bytes[i] = binaryString.charCodeAt(i);
                    }
                    const arrayBuffer = bytes.buffer;

                    proxy.handleResponse({ dcLabel, message: arrayBuffer });
                    console.log('[SDK] ✅ Proxy handled message:', dcLabel);
                    return;
                } catch (e) {
                    console.error('[SDK] Proxy handle error:', e);
                    return;
                }
            }

            // 2. 检查是否是 RTCPeerConnection 管理的 DC
            const rtcInstance = getRTCPeerConnectionByLabel(dcLabel);
            if (rtcInstance !== null) {
                rtcInstance.handleMessageNotify(data);
                return;
            }

            // 3. 业务回调
            if (messageNotifiedCallback) {
                let arrayBuffer: ArrayBuffer | null = null;
                if (data.message) {
                    try {
                        const binaryString = atob(data.message);
                        const bytes = new Uint8Array(binaryString.length);
                        for (let i = 0; i < binaryString.length; i++) {
                            bytes[i] = binaryString.charCodeAt(i);
                        }
                        arrayBuffer = bytes.buffer;
                    } catch (e) {
                        console.error('messageNotify base64 decode error:', e);
                    }
                }

                const mobileData = {
                    dcLabel: data.dcLabel || '',
                    message: arrayBuffer
                };

                messageNotifiedCallback(mobileData, () => { }, () => { }, () => { });
            }
        } catch (e) {
            console.error('messageNotify parse error:', e);
        }
    });

    // 4.3 通话状态发生变化
    mDCBridge.register('callStateNotify', (msg: any) => {
        if (callStateNotifiedCallback) {
            try {
                const data = typeof msg === 'string' ? JSON.parse(msg) : msg;
                callStateNotifiedCallback(
                    { callInfo: { callState: data.state } },
                    () => { }, () => { }, () => { }
                );
            } catch (e) {
                console.error('callStateNotify parse error:', e);
            }
        }
    });

    // 4.4 小程序生命周期变化
    mDCBridge.register('miniAppStateNotify', (msg: any) => {
        if (lifecycleChangedCallback) {
            try {
                const data = typeof msg === 'string' ? JSON.parse(msg) : msg;
                const eventMap: Record<string, string> = {
                    'onFinish': 'onFinish',
                    'onBackground': 'onPause',
                    'onFront': 'onResume'
                };
                lifecycleChangedCallback(
                    { event: eventMap[data.miniAppState] || data.miniAppState },
                    () => { }, () => { }, () => { }
                );
            } catch (e) {
                console.error('miniAppStateNotify parse error:', e);
            }
        }
    });

    // 拓展能力回调
    mDCBridge.register('expandingCapacityNotify', (msg: any) => {
        if (expandingCapacityNotifyCallback) {
            try {
                const data = typeof msg === 'string' ? JSON.parse(msg) : msg;
                expandingCapacityNotifyCallback(
                    data,
                    () => { }, () => { }, () => { }
                );
            } catch (e) {
                console.error('expandingCapacityNotify parse error:', e);
            }
        }
    });

    // 4.5 画板轨迹通知 (5.1.4 drawingInfoNotify)
    mDCBridge.register('drawingInfoNotify', (msg: any) => {
        if (drawingInfoNotifyCallback) {
            try {
                const data = typeof msg === 'string' ? JSON.parse(msg) : msg;
                drawingInfoNotifyCallback(
                    data,
                    () => { }, () => { }, () => { }
                );
            } catch (e) {
                console.error('drawingInfoNotify parse error:', e);
            }
        }
    });

    // 4.6 画板状态通知 (5.1.6 sketchStatusNotify)
    mDCBridge.register('sketchStatusNotify', (msg: any) => {
        if (sketchStatusNotifyCallback) {
            try {
                const data = typeof msg === 'string' ? JSON.parse(msg) : msg;
                sketchStatusNotifyCallback(
                    data,
                    () => { }, () => { }, () => { }
                );
            } catch (e) {
                console.error('sketchStatusNotify parse error:', e);
            }
        }
    });

    // 4.7 对端拉起小程序结果通知 (5.1.9 startAdverseAppResponseNotify)
    mDCBridge.register('startAdverseAppResponseNotify', (msg: any) => {
        if (startAdverseAppResponseNotifyCallback) {
            try {
                const data = typeof msg === 'string' ? JSON.parse(msg) : msg;
                startAdverseAppResponseNotifyCallback(
                    data,
                    () => { }, () => { }, () => { }
                );
            } catch (e) {
                console.error('startAdverseAppResponseNotify parse error:', e);
            }
        }
    });

    // 4.8 音频设备变化通知 (5.1.10 audioDeviceChangeNotify)
    mDCBridge.register('audioDeviceChangeNotify', (msg: any) => {
        if (audioDeviceChangeNotifyCallback) {
            try {
                const data = typeof msg === 'string' ? JSON.parse(msg) : msg;
                audioDeviceChangeNotifyCallback(
                    data,
                    () => { }, () => { }, () => { }
                );
            } catch (e) {
                console.error('audioDeviceChangeNotify parse error:', e);
            }
        }
    });

    // 4.9 键盘高度变化通知 (5.1.11 imeHeightNotify)
    mDCBridge.register('imeHeightNotify', (msg: any) => {
        if (imeHeightNotifyCallback) {
            try {
                const data = typeof msg === 'string' ? JSON.parse(msg) : msg;
                imeHeightNotifyCallback(
                    data,
                    () => { }, () => { }, () => { }
                );
            } catch (e) {
                console.error('imeHeightNotify parse error:', e);
            }
        }
    });
}

function initSDK() {
    try {
        // ★★★ 自动初始化 networkProxy ★★★
        networkProxy.init();
        console.log('[SDK] NetworkProxy auto-initialized');
    } catch (error) {
        console.warn('[SDK] NetworkProxy init failed:', error);
    }
}

initSDK();

// 初始化注册回调
registerTelecomCallbacks();

// ==================== 1. DC 插件 ====================

export const dcPlugin = {
    /**
     * 1.1 创建 DC（带 appId 自动获取）
     */
    createDC2(params: CreateDC2Params): Promise<ApiResponse<{ dcLabels: string[] }>> {
        return new Promise((resolve, reject) => {
            const dcParams = params.dcParams || [];

            if (dcParams.length === 0) {
                reject({ code: 0, message: 'dcParams is required and cannot be empty' });
                return;
            }

            for (let i = 0; i < dcParams.length; i++) {
                const p = dcParams[i];
                if (!p.DcId) {
                    reject({ code: 0, message: `dcParams[${i}].DcId is required` });
                    return;
                }
                if (!p.UseCase) {
                    reject({ code: 0, message: `dcParams[${i}].UseCase is required` });
                    return;
                }
            }

            callMiniAppEvent(
                'getMiniAppInfo',
                {},
                (appInfoResult: any) => {
                    try {
                        const appData = typeof appInfoResult === 'string' ? JSON.parse(appInfoResult) : appInfoResult;
                        if (appData.code !== '0') {
                            reject({ code: 0, message: 'Failed to get appId from getMiniAppInfo' });
                            return;
                        }

                        const appId = appData.data?.appId;
                        if (!appId) {
                            reject({ code: 0, message: 'get appId failed' });
                            return;
                        }

                        const dcLabels: string[] = [];
                        const appInfo = {
                            dataChannelApp: {
                                appId: appId,
                                dataChannelList: dcParams.map((p, index) => {
                                    const dcLabel = `local_${appId}_${p.UseCase}_${p.DcId}`;
                                    dcLabels.push(dcLabel);
                                    return {
                                        dcId: p.DcId,
                                        streamId: p.StreamId || '',
                                        dcLabel: dcLabel,
                                        subProtocol: p.Subprotocol || '',
                                        ordered: p.Ordered || '1',
                                        maxRetr: p.MaxRetr || '0',
                                        maxTime: p.MaxTime || '0',
                                        priority: p.Priority || '',
                                        useCase: p.UseCase,
                                        autoAcceptDcSetup: p.AutoAcceptDcSetup || '1',
                                        bandwidth: p.Bandwidth || '',
                                        qosHint: p.QosHint || '',
                                        content: ''
                                    };
                                })
                            }
                        };

                        callDcEvent(
                            'createAppDataChannel',
                            {
                                dcLabels: dcLabels,
                                DataChannelAppInfoXml: '',
                                DataChannelAppInfoJson: JSON.stringify(appInfo)
                            },
                            (v: any) => {
                                try {
                                    const result = typeof v === 'string' ? JSON.parse(v) : v;
                                    if (result.code === '0') {
                                        resolve({
                                            code: 1,
                                            message: 'success',
                                            data: {
                                                dcLabels: dcLabels
                                            }
                                        });
                                    } else {
                                        reject({
                                            code: result.code,
                                            message: result.message || '创建 DC 失败'
                                        });
                                    }
                                } catch (e) {
                                    reject({ code: 0, message: '解析响应失败' });
                                }
                            }
                        );
                    } catch (e) {
                        reject({ code: 0, message: '解析 getMiniAppInfo 响应失败' });
                    }
                }
            );
        });
    },

    /**
     * 1.3 关闭 DC
     */
    closeDC(params: CloseDCParams): Promise<ApiResponse<{}>> {
        return new Promise((resolve, reject) => {
            const dcLabels = params.dcLabel || [];
            if (dcLabels.length === 0) {
                reject({ code: 0, message: 'dcLabel is required' });
                return;
            }

            callDcEvent(
                'closeAppDataChannel',
                { dcLabel: dcLabels },
                (v: any) => {
                    try {
                        const result = typeof v === 'string' ? JSON.parse(v) : v;
                        if (result.code === '0') {
                            resolve({ code: 1, message: 'success', data: {} });
                        } else {
                            reject(result);
                        }
                    } catch (e) {
                        reject({ code: 0, message: '解析响应失败' });
                    }
                }
            );
        });
    },

    /**
     * 1.4 发送 DC 数据
     */
    sendData(params: SendDataParams): Promise<ApiResponse<{}>> {
        return new Promise((resolve, reject) => {
            const { dcLabel, data } = params;
            if (!dcLabel) {
                reject({ code: 0, message: 'dcLabel is required' });
                return;
            }

            let base64Data: string;
            if (data instanceof ArrayBuffer) {
                const bytes = new Uint8Array(data);
                let binary = '';
                for (let i = 0; i < bytes.length; i++) {
                    binary += String.fromCharCode(bytes[i]);
                }
                base64Data = btoa(binary);
            } else if (typeof data === 'string') {
                const encoder = new TextEncoder();
                const uint8Array = encoder.encode(data);
                let binary = '';
                for (let i = 0; i < uint8Array.length; i++) {
                    binary += String.fromCharCode(uint8Array[i]);
                }
                base64Data = btoa(binary);
            } else {
                reject({ code: 0, message: 'data type not supported, must be string or ArrayBuffer' });
                return;
            }

            callDcEvent(
                'sendData',
                {
                    dcLabel: dcLabel,
                    data: base64Data
                },
                (v: any) => {
                    try {
                        const result = typeof v === 'string' ? JSON.parse(v) : v;
                        if (result.code === '0') {
                            resolve({ code: 1, message: 'success', data: {} });
                        } else {
                            reject(result);
                        }
                    } catch (e) {
                        reject({ code: 0, message: '解析响应失败' });
                    }
                }
            );
        });
    },

    /**
     * 1.7 查询对端是否支持 DC
     */
    ifPeerSupportDC(): Promise<ApiResponse<{ ifPeerSupportDC: boolean }>> {
        return new Promise((resolve, reject) => {
            callDcEvent(
                'isPeerSupportDC',
                {},
                (v: any) => {
                    try {
                        const result = typeof v === 'string' ? JSON.parse(v) : v;
                        if (result.code === '0') {
                            resolve({
                                code: 1,
                                message: 'success',
                                data: {
                                    ifPeerSupportDC: (result.data?.isPeerSupportDc) || false
                                }
                            });
                        } else {
                            reject(result);
                        }
                    } catch (e) {
                        reject({ code: 0, message: '解析响应失败' });
                    }
                }
            );
        });
    },

    /**
     * 1.8 查询DC通道数据缓存剩余可用大小
     */
    queryDCFreeBufferSize(params: QueryDCFreeBufferSizeParams): Promise<ApiResponse<{ freeBufferSize: number }>> {
        return new Promise((resolve, reject) => {
            const { dcLabel } = params;
            if (!dcLabel) {
                reject({ code: 0, message: 'dcLabel is required' });
                return;
            }

            callDcEvent(
                'getBufferedAmount',
                { dcLabel: dcLabel },
                (v: any) => {
                    try {
                        const result = typeof v === 'string' ? JSON.parse(v) : v;
                        if (result.code === '0') {
                            resolve({
                                code: 1,
                                message: 'success',
                                data: {
                                    freeBufferSize: (result.data?.bufferedAmount) || 0
                                }
                            });
                        } else {
                            reject(result);
                        }
                    } catch (e) {
                        reject({ code: 0, message: '解析响应失败' });
                    }
                }
            );
        });
    },
};

// ==================== 2. 媒体插件 ====================

export const mediaPlugin = {

    /**
     * 2.2 查询图片列表
     */
    queryImages(params?: QueryImagesParams): Promise<ApiResponse<any[]>> {
        return new Promise((resolve, reject) => {
            const { path, offset = '0', size = '20' } = params || {};
            const imageSuffix = '.jpg||.jpeg||.png||.gif||.bmp||.webp||.svg||.ico||.tiff||.heic';

            callFileEvent(
                'getFileList',
                {
                    folderPath: path || '/storage/emulated/0',
                    offset: String(offset),
                    count: String(size),
                    fileType: '1',
                    suffix: imageSuffix,
                    sortType: 'timeSort',
                    sortOrder: '1',
                    folderPosition: '1'
                },
                (v: any) => {
                    try {
                        const result = typeof v === 'string' ? JSON.parse(v) : v;
                        if (result.code === '0') {
                            const dataList = result.data || [];
                            const formattedData = dataList.map((item: any) => ({
                                filePath: item.path || '',
                                fileName: item.name || '',
                                folderName: item.path ? item.path.substring(0, item.path.lastIndexOf('/')) : '',
                                size: item.size || 0,
                                fileType: 'image'
                            }));
                            resolve({
                                code: 1,
                                message: 'success',
                                data: formattedData
                            });
                        } else {
                            reject(result);
                        }
                    } catch (e) {
                        reject({ code: 0, message: '解析响应失败' });
                    }
                }
            );
        });
    },

    /**
     * 2.3 查询视频列表
     */
    queryVideos(params?: QueryVideosParams): Promise<ApiResponse<any[]>> {
        return new Promise((resolve, reject) => {
            const { path, offset = '0', size = '20' } = params || {};
            const videoSuffix = '.mp4||.3gp||.avi||.mkv||.mov||.webm||.flv||.wmv||.m4v||.ts';

            callFileEvent(
                'getFileList',
                {
                    folderPath: path || '/storage/emulated/0',
                    offset: String(offset),
                    count: String(size),
                    fileType: '1',
                    suffix: videoSuffix,
                    sortType: 'timeSort',
                    sortOrder: '1',
                    folderPosition: '1'
                },
                (v: any) => {
                    try {
                        const result = typeof v === 'string' ? JSON.parse(v) : v;
                        if (result.code === '0') {
                            const dataList = result.data || [];
                            const formattedData = dataList.map((item: any) => ({
                                filePath: item.path || '',
                                fileName: item.name || '',
                                folderName: item.path ? item.path.substring(0, item.path.lastIndexOf('/')) : '',
                                size: item.size || 0,
                                fileType: 'video'
                            }));
                            resolve({
                                code: 1,
                                message: 'success',
                                data: formattedData
                            });
                        } else {
                            reject(result);
                        }
                    } catch (e) {
                        reject({ code: 0, message: '解析响应失败' });
                    }
                }
            );
        });
    },

    /**
     * 2.4 检查文件是否存在
     */
    checkFiles(params: CheckFilesParams): Promise<ApiResponse<Array<{ path: string; exist: boolean }>>> {
        return new Promise((resolve, reject) => {
            const paths = params.path || [];
            if (paths.length === 0) {
                reject({ code: 0, message: 'path is required and cannot be empty' });
                return;
            }

            const promises = paths.map((path: string) => {
                return new Promise<{ path: string; exist: boolean }>((resolveItem) => {
                    callFileEvent(
                        'getFileInfoAsync',
                        { path: path },
                        (v: any) => {
                            try {
                                const result = typeof v === 'string' ? JSON.parse(v) : v;
                                resolveItem({
                                    path: path,
                                    exist: result.code === '0'
                                });
                            } catch (e) {
                                resolveItem({
                                    path: path,
                                    exist: false
                                });
                            }
                        }
                    );
                });
            });

            Promise.all(promises)
                .then((results) => {
                    resolve({
                        code: 1,
                        message: 'success',
                        data: results
                    });
                })
                .catch(() => {
                    reject({ code: 0, message: '检查文件失败' });
                });
        });
    },

    /**
     * 2.5 查询文件列表
     */
    queryFiles(params: QueryFilesParams): Promise<ApiResponse<any[]>> {
        return new Promise((resolve, reject) => {
            const { contentType, path, offset = '0', size = '20' } = params || {};

            const suffixMap: Record<string, string> = {
                'text/plain': '.txt||.log||.text||.md',
                'application/pdf': '.pdf',
                'image/*': '.jpg||.jpeg||.png||.gif||.bmp||.webp||.svg||.ico||.tiff||.heic',
                'video/*': '.mp4||.3gp||.avi||.mkv||.mov||.webm||.flv||.wmv||.m4v||.ts',
                'audio/*': '.mp3||.wav||.aac||.flac||.ogg||.m4a||.amr',
                'application/zip': '.zip||.rar||.7z||.tar||.gz||.bz2||.xz',
                'application/json': '.json',
                'text/html': '.html||.htm',
                'text/css': '.css',
                'application/javascript': '.js||.mjs',
                'application/xml': '.xml',
                '*': ''
            };

            const suffix = suffixMap[contentType] || '';

            callFileEvent(
                'getFileList',
                {
                    folderPath: path || '',
                    offset: String(offset),
                    count: String(size),
                    fileType: '1',
                    suffix: suffix,
                    sortType: 'timeSort',
                    sortOrder: '1',
                    folderPosition: '1'
                },
                (v: any) => {
                    try {
                        const result = typeof v === 'string' ? JSON.parse(v) : v;
                        if (result.code === '0') {
                            const dataList = result.data || [];
                            const formattedData = dataList.map((item: any) => ({
                                filePath: item.path || '',
                                fileName: item.name || '',
                                folderName: item.path ? item.path.substring(0, item.path.lastIndexOf('/')) : '',
                                size: item.size || 0,
                                fileType: item.isDirectory ? 'folder' : 'file'
                            }));
                            resolve({
                                code: 1,
                                message: 'success',
                                data: formattedData
                            });
                        } else {
                            reject(result);
                        }
                    } catch (e) {
                        reject({ code: 0, message: '解析响应失败' });
                    }
                }
            );
        });
    },

    /**
     * 2.6 保存文件
     */
    saveFile(params: SaveFileParams): Promise<ApiResponse<{ absolutePath: string }>> {
        return new Promise((resolve, reject) => {
            const { file, filePathType, filePath } = params;
            if (!file) {
                reject({ code: 0, message: 'file is required' });
                return;
            }
            if (!filePath) {
                reject({ code: 0, message: 'filePath is required' });
                return;
            }

            const getPrivateFolder = (): Promise<string> => {
                return new Promise((resolveFolder, rejectFolder) => {
                    const type = filePathType === 0 ? 'inner' : 'outer';
                    callFileEvent(
                        'getPrivateFolder',
                        { type: type },
                        (v: any) => {
                            try {
                                const result = typeof v === 'string' ? JSON.parse(v) : v;
                                if (result.code === '0') {
                                    resolveFolder((result.data?.privateFolder) || '');
                                } else {
                                    rejectFolder(result);
                                }
                            } catch (e) {
                                rejectFolder({ code: 0, message: '解析 getPrivateFolder 响应失败' });
                            }
                        }
                    );
                });
            };

            const arrayBufferToBase64 = (buffer: ArrayBuffer): string => {
                const bytes = new Uint8Array(buffer);
                let binary = '';
                for (let i = 0; i < bytes.length; i++) {
                    binary += String.fromCharCode(bytes[i]);
                }
                return btoa(binary);
            };

            getPrivateFolder()
                .then((basePath) => {
                    const fullPath = basePath + filePath;

                    callFileEvent(
                        'startSaveFile',
                        {
                            path: fullPath,
                            append: false
                        },
                        (startResult: any) => {
                            try {
                                const startData = typeof startResult === 'string' ? JSON.parse(startResult) : startResult;
                                if (startData.code !== '0') {
                                    reject({
                                        code: startData.code,
                                        message: startData.message || 'startSaveFile 失败'
                                    });
                                    return;
                                }

                                const base64Data = arrayBufferToBase64(file);
                                callFileEvent(
                                    'saveFile',
                                    { data: base64Data },
                                    (saveResult: any) => {
                                        try {
                                            const saveData = typeof saveResult === 'string' ? JSON.parse(saveResult) : saveResult;
                                            if (saveData.code !== '0') {
                                                reject({
                                                    code: saveData.code,
                                                    message: saveData.message || 'saveFile 失败'
                                                });
                                                return;
                                            }

                                            callFileEvent(
                                                'stopSaveFile',
                                                {},
                                                (stopResult: any) => {
                                                    try {
                                                        const stopData = typeof stopResult === 'string' ? JSON.parse(stopResult) : stopResult;
                                                        if (stopData.code === '0') {
                                                            resolve({
                                                                code: 1,
                                                                message: 'success',
                                                                data: {
                                                                    absolutePath: fullPath
                                                                }
                                                            });
                                                        } else {
                                                            reject({
                                                                code: stopData.code,
                                                                message: stopData.message || 'stopSaveFile 失败'
                                                            });
                                                        }
                                                    } catch (e) {
                                                        reject({ code: 0, message: '解析 stopSaveFile 响应失败' });
                                                    }
                                                }
                                            );
                                        } catch (e) {
                                            reject({ code: 0, message: '解析 saveFile 响应失败' });
                                        }
                                    }
                                );
                            } catch (e) {
                                reject({ code: 0, message: '解析 startSaveFile 响应失败' });
                            }
                        }
                    );
                })
                .catch((err) => {
                    reject({
                        code: err.code || 0,
                        message: err.message || '获取私有目录失败'
                    });
                });
        });
    },

    /**
     * 2.7 保存 KV 键值对
     */
    saveKeyValueData(params: SaveKeyValueDataParams): Promise<ApiResponse<{}>> {
        return new Promise((resolve, reject) => {
            const { key, value } = params;
            if (!key) {
                reject({ code: 0, message: 'key is required' });
                return;
            }

            callFileEvent(
                'saveUpdateKeyValueWithExpiry',
                {
                    key: key,
                    value: value || '',
                    ttl: '2592000000'
                },
                (v: any) => {
                    try {
                        const result = typeof v === 'string' ? JSON.parse(v) : v;
                        if (result.code === '0') {
                            resolve({ code: 1, message: 'success', data: {} });
                        } else {
                            reject(result);
                        }
                    } catch (e) {
                        reject({ code: 0, message: '解析响应失败' });
                    }
                }
            );
        });
    },

    /**
     * 2.8 获取 KV 键值对
     */
    getKeyValueData(params: GetKeyValueDataParams): Promise<ApiResponse<{ value: string }>> {
        return new Promise((resolve, reject) => {
            const { key } = params;
            if (!key) {
                reject({ code: 0, message: 'key is required' });
                return;
            }

            callFileEvent(
                'getKeyValue',
                { key: key },
                (v: any) => {
                    try {
                        const result = typeof v === 'string' ? JSON.parse(v) : v;
                        if (result.code === '0') {
                            resolve({
                                code: 1,
                                message: 'success',
                                data: {
                                    value: (result.data?.value) || ''
                                }
                            });
                        } else {
                            reject(result);
                        }
                    } catch (e) {
                        reject({ code: 0, message: '解析响应失败' });
                    }
                }
            );
        });
    },

    /**
     * 删除 KV 键值对
     */
    deleteKeyValue(params: DeleteKeyValueParams): Promise<ApiResponse<{}>> {
        return new Promise((resolve, reject) => {
            const { key } = params;
            if (!key) {
                reject({ code: 0, message: 'key is required' });
                return;
            }

            callFileEvent(
                'deleteKeyValue',
                { key: key },
                (v: any) => {
                    try {
                        const result = typeof v === 'string' ? JSON.parse(v) : v;
                        if (result.code === '0') {
                            resolve({ code: 1, message: 'success', data: {} });
                        } else {
                            reject(result);
                        }
                    } catch (e) {
                        reject({ code: 0, message: '解析响应失败' });
                    }
                }
            );
        });
    },

    /**
     * 2.9 解压压缩包
     */
    dealCompressedFile(params: DealCompressedFileParams): Promise<ApiResponse<{}>> {
        return new Promise((resolve, reject) => {
            const { compressedFilePath, filePathType, filePath, type } = params;
            if (!compressedFilePath) {
                reject({ code: 0, message: 'compressedFilePath is required' });
                return;
            }

            const getPath = (): Promise<string> => {
                return new Promise((resolvePath) => {
                    if (filePathType === 0) {
                        callFileEvent(
                            'getPrivateFolder',
                            { type: 'inner' },
                            (v: any) => {
                                try {
                                    const result = typeof v === 'string' ? JSON.parse(v) : v;
                                    resolvePath(result.code === '0' ? (result.data?.privateFolder) || '' : '');
                                } catch (e) {
                                    resolvePath('');
                                }
                            }
                        );
                    } else {
                        resolvePath('');
                    }
                });
            };

            getPath().then((basePath) => {
                let destPath = filePath;
                if (!destPath) {
                    destPath = compressedFilePath.replace(/\.(zip|rar|7z|tar|gz|bz2|xz)$/, '');
                } else if (filePathType === 0) {
                    destPath = basePath + filePath;
                }

                callFileEvent(
                    'decompressFile',
                    {
                        srcPath: compressedFilePath,
                        desPath: destPath,
                        compressType: type || ''
                    },
                    (v: any) => {
                        try {
                            const result = typeof v === 'string' ? JSON.parse(v) : v;
                            if (result.code === '0') {
                                resolve({
                                    code: 1,
                                    message: 'success',
                                    data: {}
                                });
                            } else {
                                reject({
                                    code: result.code,
                                    message: result.message || '解压失败'
                                });
                            }
                        } catch (e) {
                            reject({ code: 0, message: '解析响应失败' });
                        }
                    }
                );
            }).catch(() => {
                reject({ code: 0, message: '获取私有目录失败' });
            });
        });
    },

    /**
     * 2.10 获取位置信息
     */
    getLocation(): Promise<ApiResponse<{ longitude: number; latitude: number }>> {
        return new Promise((resolve, reject) => {
            callFileEvent(
                'getLocation',
                {},
                (v: any) => {
                    try {
                        const result = typeof v === 'string' ? JSON.parse(v) : v;
                        if (result.code === '0') {
                            resolve({
                                code: 1,
                                message: 'success',
                                data: {
                                    longitude: parseFloat((result.data?.lon) || 0),
                                    latitude: parseFloat((result.data?.lat) || 0)
                                }
                            });
                        } else {
                            reject(result);
                        }
                    } catch (e) {
                        reject({ code: 0, message: '解析响应失败' });
                    }
                }
            );
        });
    },

    /**
     * 2.12 根据文件路径获取文件内容
     */
    getFileByPath(params: GetFileByPathParams): Promise<ApiResponse<{ data: string }>> {
        return new Promise((resolve, reject) => {
            const { filePath } = params;
            if (!filePath) {
                reject({ code: 0, message: 'filePath is required' });
                return;
            }

            let allData = '';

            callFileEvent(
                'startReadFile',
                { path: filePath },
                (startResult: any) => {
                    try {
                        const startData = typeof startResult === 'string' ? JSON.parse(startResult) : startResult;
                        if (startData.code !== '0') {
                            reject({
                                code: startData.code,
                                message: startData.message || 'startReadFile 失败'
                            });
                            return;
                        }

                        const readNext = (): void => {
                            callFileEvent(
                                'readFile',
                                { length: '100000' },
                                (readResult: any) => {
                                    try {
                                        const readData = typeof readResult === 'string' ? JSON.parse(readResult) : readResult;
                                        if (readData.code !== '0') {
                                            callFileEvent('stopReadFile', {}, () => { });
                                            reject({
                                                code: readData.code,
                                                message: readData.message || 'readFile 失败'
                                            });
                                            return;
                                        }

                                        if (readData.data?.base64Data) {
                                            allData += readData.data.base64Data;
                                        }

                                        if (readData.data?.isEnd) {
                                            callFileEvent(
                                                'stopReadFile',
                                                {},
                                                (stopResult: any) => {
                                                    try {
                                                        const stopData = typeof stopResult === 'string' ? JSON.parse(stopResult) : stopResult;
                                                        if (stopData.code === '0') {
                                                            resolve({
                                                                code: 1,
                                                                message: 'success',
                                                                data: {
                                                                    data: allData
                                                                }
                                                            });
                                                        } else {
                                                            reject({
                                                                code: stopData.code,
                                                                message: stopData.message || 'stopReadFile 失败'
                                                            });
                                                        }
                                                    } catch (e) {
                                                        reject({ code: 0, message: '解析 stopReadFile 响应失败' });
                                                    }
                                                }
                                            );
                                        } else {
                                            readNext();
                                        }
                                    } catch (e) {
                                        reject({ code: 0, message: '解析 readFile 响应失败' });
                                    }
                                }
                            );
                        };

                        readNext();
                    } catch (e) {
                        reject({ code: 0, message: '解析 startReadFile 响应失败' });
                    }
                }
            );
        });
    },

    /**
     * 2.13 选择图片
     */
    chooseImage(params?: ChooseImageParams): Promise<ApiResponse<any[]>> {
        return new Promise((resolve, reject) => {
            const count = (params?.count) || 9;

            callFileEvent(
                'selectFiles',
                {
                    maxSelectable: count,
                    fileTypes: ['image']
                },
                (v: any) => {
                    try {
                        const result = typeof v === 'string' ? JSON.parse(v) : v;
                        if (result.code === '0') {
                            const dataList = result.data || [];
                            const formattedData = dataList.map((item: any) => ({
                                filePath: item.path || '',
                                fileName: item.name || '',
                                size: item.size || 0,
                                width: item.width || 0,
                                height: item.height || 0
                            }));
                            resolve({
                                code: 1,
                                message: 'success',
                                data: formattedData
                            });
                        } else {
                            reject(result);
                        }
                    } catch (e) {
                        reject({ code: 0, message: '解析响应失败' });
                    }
                }
            );
        });
    },

    /**
     * 2.14 选择视频
     */
    chooseVideo(): Promise<ApiResponse<any>> {
        return new Promise((resolve, reject) => {
            callFileEvent(
                'selectFiles',
                {
                    maxSelectable: 1,
                    fileTypes: ['video']
                },
                (v: any) => {
                    try {
                        const result = typeof v === 'string' ? JSON.parse(v) : v;
                        if (result.code === '0') {
                            const dataList = result.data || [];
                            if (dataList.length > 0) {
                                const item = dataList[0];
                                resolve({
                                    code: 1,
                                    message: 'success',
                                    data: {
                                        filePath: item.path || '',
                                        fileName: item.name || '',
                                        size: item.size || 0,
                                        duration: item.duration || 0,
                                        width: item.width || 0,
                                        height: item.height || 0
                                    }
                                });
                            } else {
                                resolve({
                                    code: 1,
                                    message: 'success',
                                    data: {}
                                });
                            }
                        } else {
                            reject(result);
                        }
                    } catch (e) {
                        reject({ code: 0, message: '解析响应失败' });
                    }
                }
            );
        });
    },

    /**
     * 删除私有目录下的文件
     * @param params.path 私有目录下的文件路径
     */
    deleteFile(params: DeleteFileParams): Promise<ApiResponse<{}>> {
        return new Promise((resolve, reject) => {
            const { path } = params;
            if (!path) {
                reject({ code: 0, message: 'path is required' });
                return;
            }

            callFileEvent(
                'deleteFile',
                { path: path },
                (v: any) => {
                    try {
                        const result = typeof v === 'string' ? JSON.parse(v) : v;
                        if (result.code === '0') {
                            resolve({ code: 1, message: 'success', data: {} });
                        } else {
                            reject(result);
                        }
                    } catch (e) {
                        reject({ code: 0, message: '解析响应失败' });
                    }
                }
            );
        });
    },

    /**
     * 根据关键词快速搜索文件
     * @param params.keywords 搜索关键词，JSON数组字符串，如 '["项目申请书","pdf"]'
     */
    searchFiles(params: SearchFilesParams): Promise<ApiResponse<{ files: Array<{ path: string; name: string }> }>> {
        return new Promise((resolve, reject) => {
            const { keywords } = params;
            if (!keywords) {
                reject({ code: 0, message: 'keywords is required' });
                return;
            }

            callFileEvent(
                'quickSearchFileWithKeyWords',
                { keywords: keywords },
                (v: any) => {
                    try {
                        const result = typeof v === 'string' ? JSON.parse(v) : v;
                        if (result.code === '0') {
                            const fileList = Array.isArray(result.data) ? result.data : [];
                            resolve({
                                code: 1,
                                message: 'success',
                                data: {
                                    files: fileList.map((item: any) => ({
                                        path: item.path || '',
                                        name: item.name || ''
                                    }))
                                }
                            });
                        } else {
                            reject(result);
                        }
                    } catch (e) {
                        reject({ code: 0, message: '解析响应失败' });
                    }
                }
            );
        });
    }
};

// ==================== 3. 业务功能插件 ====================

export const bizPlugin = {
    /**
     * 3.1 获取小程序配置信息
     */
    getMiniAppInfo(): Promise<ApiResponse<any>> {
        return new Promise((resolve, reject) => {
            callMiniAppEvent(
                'getMiniAppInfo',
                {},
                (v: any) => {
                    try {
                        const result = typeof v === 'string' ? JSON.parse(v) : v;
                        if (result.code === '0') {
                            const data = result.data || {};
                            resolve({
                                code: 1,
                                message: 'success',
                                data: {
                                    slotId: data.slotId || 0,
                                    telecomCallId: data.telecomCallId || data.callId || '',
                                    appId: data.appId || '',
                                    appName: data.appName || '',
                                    appIcon: data.appIcon || '',
                                    etag: data.etag || data.eTag || '',
                                    phase: data.phase || '',
                                    autoload: data.autoload || false,
                                    autolaunch: data.autolaunch || false,
                                    ifWorkWithoutPeerDC: data.ifWorkWithoutPeerDC || data.ifWorkWithoutPeerDc || false,
                                    supportScene: data.supportScene || 3,
                                    'qos-hint': data['qos-hint'] || data.qosHint || '',
                                    isActiveStart: data.isActiveStart !== undefined ? data.isActiveStart : undefined,
                                    path: data.path || ''
                                }
                            });
                        } else {
                            reject(result);
                        }
                    } catch (e) {
                        reject({ code: 0, message: '解析响应失败' });
                    }
                }
            );
        });
    },

    /**
     * 3.2 启动 Activity
     */
    openApp(params: OpenAppParams): Promise<ApiResponse<any>> {
        return new Promise((resolve, reject) => {
            const { jumpType, extras } = params;

            const typeMap: Record<string, string> = {
                'openBrowser': 'browser',
                'openCameraToPic': 'camera',
                'openCameraToVideo': 'camera',
                'openFile': 'file',
                'openMap': 'map',
                'openMiniApp': 'miniapp',
                'openMiniAppWithParams': 'miniappWithParams'
            };

            const appType = typeMap[jumpType] || 'miniapp';

            let teleExtra = extras || '';
            if (jumpType === 'openCameraToPic') {
                teleExtra = 'picture';
            } else if (jumpType === 'openCameraToVideo') {
                teleExtra = 'video';
            }

            callMiniAppEvent(
                'startApp',
                {
                    appType: appType,
                    extra: teleExtra
                },
                (v: any) => {
                    try {
                        const result = typeof v === 'string' ? JSON.parse(v) : v;
                        if (result.code === '0') {
                            // miniappWithParams 返回 {isStarted: true}，其他返回路径字符串
                            if (appType === 'miniappWithParams' && typeof result.data === 'object') {
                                resolve({
                                    code: 1,
                                    message: 'success',
                                    data: {
                                        isStarted: result.data.isStarted !== undefined ? result.data.isStarted : true
                                    }
                                });
                            } else {
                                resolve({
                                    code: 1,
                                    message: 'success',
                                    data: {
                                        path: typeof result.data === 'string' ? result.data : (result.data?.path || '')
                                    }
                                });
                            }
                        } else {
                            reject(result);
                        }
                    } catch (e) {
                        reject({ code: 0, message: '解析响应失败' });
                    }
                }
            );
        });
    },

    /**
     * 3.3 设置窗口样式
     */
    setWindow(params: SetWindowParams): Promise<ApiResponse<{}>> {
        return new Promise((resolve, reject) => {
            const teleParams: any = {
                hidden: params.hidden !== undefined ? params.hidden : (params.isWindowFront === undefined ? false : !params.isWindowFront),
                isFullScreen: params.isFullScreen || false,
                statusBarColor: params.statusBarColor || '#FFFFFF',
                statusBarTitleColor: params.statusBarTitleColor !== undefined ? params.statusBarTitleColor : (params.barEnable ? '0' : '1'),
                pageName: params.pageName || '',
                navigationBarColor: params.navigationBarColor || '#FFFFFF'
            };

            // 透传文档2 v1.0.7 新增的窗口大小、位置百分比字段
            if (params.width !== undefined) {
                teleParams.width = params.width;
            }
            if (params.height !== undefined) {
                teleParams.height = params.height;
            }
            if (params.windowStart !== undefined) {
                teleParams.windowStart = params.windowStart;
            }
            if (params.windowTop !== undefined) {
                teleParams.windowTop = params.windowTop;
            }
            if (params.closeGuidePage !== undefined) {
                teleParams.closeGuidePage = params.closeGuidePage;
            }

            callMiniAppEvent(
                'setWindow',
                teleParams,
                (v: any) => {
                    try {
                        const result = typeof v === 'string' ? JSON.parse(v) : v;
                        if (result.code === '0') {
                            resolve({ code: 1, message: 'success', data: {} });
                        } else {
                            reject(result);
                        }
                    } catch (e) {
                        reject({ code: 0, message: '解析响应失败' });
                    }
                }
            );
        });
    },

    /**
     * 3.4 获取联系人列表
     */
    getContactList(params?: GetContactListParams): Promise<ApiResponse<{ total: number; list: any[] }>> {
        return new Promise((resolve, reject) => {
            const offset = (params?.offset) || '0';
            const limit = (params?.limit) || '100';

            callMiniAppEvent(
                'getContactList',
                { offset: offset, limit: limit },
                (v: any) => {
                    try {
                        const result = typeof v === 'string' ? JSON.parse(v) : v;
                        if (result.code === '0') {
                            const data = result.data || {};
                            resolve({
                                code: 1,
                                message: 'success',
                                data: {
                                    total: data.total || 0,
                                    list: data.list || []
                                }
                            });
                        } else {
                            reject(result);
                        }
                    } catch (e) {
                        reject({ code: 0, message: '解析响应失败' });
                    }
                }
            );
        });
    },

    /**
     * 3.5 挂断通话
     */
    endCall(): Promise<ApiResponse<{}>> {
        return new Promise((resolve, reject) => {
            callMiniAppEvent(
                'hangUp',
                {},
                (v: any) => {
                    try {
                        const result = typeof v === 'string' ? JSON.parse(v) : v;
                        if (result.code === '0') {
                            resolve({ code: 1, message: 'success', data: {} });
                        } else {
                            reject(result);
                        }
                    } catch (e) {
                        reject({ code: 0, message: '解析响应失败' });
                    }
                }
            );
        });
    },


    /**
     * 接听
     */
    answer(): Promise<ApiResponse<{}>> {
        return new Promise((resolve, reject) => {
            callMiniAppEvent(
                'answer',
                {},
                (v: any) => {
                    try {
                        const result = typeof v === 'string' ? JSON.parse(v) : v;
                        if (result.code === '0') {
                            resolve({ code: 1, message: 'success', data: {} });
                        } else {
                            reject(result);
                        }
                    } catch (e) {
                        reject({ code: 0, message: '解析响应失败' });
                    }
                }
            );
        });
    },

    /**
     * 获取通话状态
     */
    getCallState(): Promise<ApiResponse<{ callState: number; callType: number }>> {
        return new Promise((resolve, reject) => {
            callMiniAppEvent(
                'getCallState',
                {},
                (v: any) => {
                    try {
                        const result = typeof v === 'string' ? JSON.parse(v) : v;
                        if (result.code === '0') {
                            const data = result.data || {};
                            resolve({
                                code: 1,
                                message: 'success',
                                data: {
                                    callState: data.callState !== undefined ? data.callState : 0,
                                    callType: data.callType !== undefined ? data.callType : 0
                                }
                            });
                        } else {
                            reject(result);
                        }
                    } catch (e) {
                        reject({ code: 0, message: '解析响应失败' });
                    }
                }
            );
        });
    },

    /**
     * 设置扬声器
     */
    setSpeakerphone(params: SetSpeakerphoneParams): Promise<ApiResponse<{}>> {
        return new Promise((resolve, reject) => {
            const { speakerphoneOn } = params;
            if (speakerphoneOn === undefined) {
                reject({ code: 0, message: 'speakerphoneOn is required' });
                return;
            }

            callMiniAppEvent(
                'setSpeakerphone',
                { speakerphoneOn: speakerphoneOn },
                (v: any) => {
                    try {
                        const result = typeof v === 'string' ? JSON.parse(v) : v;
                        if (result.code === '0') {
                            resolve({ code: 1, message: 'success', data: {} });
                        } else {
                            reject(result);
                        }
                    } catch (e) {
                        reject({ code: 0, message: '解析响应失败' });
                    }
                }
            );
        });
    },

    /**
     * 查询扬声器状态
     */
    isSpeakerphoneOn(): Promise<ApiResponse<{ isSpeakerphoneOn: boolean }>> {
        return new Promise((resolve, reject) => {
            callMiniAppEvent(
                'isSpeakerphoneOn',
                {},
                (v: any) => {
                    try {
                        const result = typeof v === 'string' ? JSON.parse(v) : v;
                        if (result.code === '0') {
                            resolve({
                                code: 1,
                                message: 'success',
                                data: {
                                    isSpeakerphoneOn: (result.data?.isSpeakerphoneOn) || false
                                }
                            });
                        } else {
                            reject(result);
                        }
                    } catch (e) {
                        reject({ code: 0, message: '解析响应失败' });
                    }
                }
            );
        });
    },

    /**
     * 设置静音
     */
    setMuted(params: SetMutedParams): Promise<ApiResponse<{}>> {
        return new Promise((resolve, reject) => {
            const { muted } = params;
            if (muted === undefined) {
                reject({ code: 0, message: 'muted is required' });
                return;
            }

            callMiniAppEvent(
                'setMuted',
                { muted: muted },
                (v: any) => {
                    try {
                        const result = typeof v === 'string' ? JSON.parse(v) : v;
                        if (result.code === '0') {
                            resolve({ code: 1, message: 'success', data: {} });
                        } else {
                            reject(result);
                        }
                    } catch (e) {
                        reject({ code: 0, message: '解析响应失败' });
                    }
                }
            );
        });
    },

    /**
     * 查询静音状态
     */
    isMuted(): Promise<ApiResponse<{ isMuted: boolean }>> {
        return new Promise((resolve, reject) => {
            callMiniAppEvent(
                'isMuted',
                {},
                (v: any) => {
                    try {
                        const result = typeof v === 'string' ? JSON.parse(v) : v;
                        if (result.code === '0') {
                            resolve({
                                code: 1,
                                message: 'success',
                                data: {
                                    isMuted: (result.data?.isMuted) || false
                                }
                            });
                        } else {
                            reject(result);
                        }
                    } catch (e) {
                        reject({ code: 0, message: '解析响应失败' });
                    }
                }
            );
        });
    },

    /**
     * 获取联系人姓名
     */
    getContactName(params: GetContactNameParams): Promise<ApiResponse<{ name: string }>> {
        return new Promise((resolve, reject) => {
            const { contactNumber } = params;
            if (!contactNumber) {
                reject({ code: 0, message: 'contactNumber is required' });
                return;
            }

            callMiniAppEvent(
                'getContactName',
                { contactNumber: contactNumber },
                (v: any) => {
                    try {
                        const result = typeof v === 'string' ? JSON.parse(v) : v;
                        if (result.code === '0') {
                            resolve({
                                code: 1,
                                message: 'success',
                                data: {
                                    name: (result.data?.name) || ''
                                }
                            });
                        } else {
                            reject(result);
                        }
                    } catch (e) {
                        reject({ code: 0, message: '解析响应失败' });
                    }
                }
            );
        });
    },

    /**
     * 添加或编辑联系人
     */
    addOrEditContact(params: AddOrEditContactParams): Promise<ApiResponse<{}>> {
        return new Promise((resolve, reject) => {
            const { contactMode, name, number } = params;
            if (!contactMode || !name || !number) {
                reject({ code: 0, message: 'contactMode, name and number are required' });
                return;
            }

            callMiniAppEvent(
                'addOrEditContact',
                { contactMode: contactMode, name: name, number: number },
                (v: any) => {
                    try {
                        const result = typeof v === 'string' ? JSON.parse(v) : v;
                        if (result.code === '0') {
                            resolve({ code: 1, message: 'success', data: {} });
                        } else {
                            reject(result);
                        }
                    } catch (e) {
                        reject({ code: 0, message: '解析响应失败' });
                    }
                }
            );
        });
    },

    /**
     * 3.8 关闭当前小程序
     */
    closeWindow(): Promise<ApiResponse<{}>> {
        return new Promise((resolve, reject) => {
            callMiniAppEvent(
                'stopApp',
                {},
                (v: any) => {
                    try {
                        const result = typeof v === 'string' ? JSON.parse(v) : v;
                        if (result.code === '0') {
                            resolve({ code: 1, message: 'success', data: {} });
                        } else {
                            reject(result);
                        }
                    } catch (e) {
                        reject({ code: 0, message: '解析响应失败' });
                    }
                }
            );
        });
    },

    /**
     * 3.10 获取当前OS与新通话SDK的版本
     */
    getVersions(): Promise<ApiResponse<any>> {
        return new Promise((resolve, reject) => {
            callMiniAppEvent(
                'getSDKInfo',
                {},
                (v: any) => {
                    try {
                        const result = typeof v === 'string' ? JSON.parse(v) : v;
                        if (result.code === '0') {
                            resolve({
                                code: 1,
                                message: 'success',
                                data: {
                                    osVersionCode: result.data?.osVersionCode,
                                    osVersionName: result.data?.osVersion,
                                    sdkVersionCode: result.data?.versionCode,
                                    sdkVersionName: result.data?.version,
                                    jsSdkVersionName: '__SDK_VERSION__'
                                }
                            });
                        } else {
                            reject(result);
                        }
                    } catch (e) {
                        reject({ code: 0, message: '解析响应失败' });
                    }
                }
            );
        });
    },


    /**
     * 3.15 开启远端小程序
     */
    openRemoteApp(): Promise<ApiResponse<{ resultCode: number }>> {
        return new Promise((resolve, reject) => {
            callMiniAppEvent(
                'requestStartAdverseApp',
                {},
                (v: any) => {
                    try {
                        const result = typeof v === 'string' ? JSON.parse(v) : v;
                        if (result.code === '0') {
                            resolve({
                                code: 1,
                                message: 'success',
                                data: {
                                    resultCode: 0
                                }
                            });
                        } else {
                            let resultCode = 3;
                            if (result.message?.includes('拒绝')) {
                                resultCode = 1;
                            } else if (result.message?.includes('订阅')) {
                                resultCode = 2;
                            }
                            resolve({
                                code: 1,
                                message: result.message || '请求失败',
                                data: {
                                    resultCode: resultCode
                                }
                            });
                        }
                    } catch (e) {
                        reject({ code: 0, message: '解析响应失败' });
                    }
                }
            );
        });
    },

    /**
     * 3.17 当前小程序页面退至后台
     */
    windowToBack(): Promise<ApiResponse<{}>> {
        return new Promise((resolve, reject) => {
            callMiniAppEvent(
                'setWindow',
                { hidden: true },
                (v: any) => {
                    try {
                        const result = typeof v === 'string' ? JSON.parse(v) : v;
                        if (result.code === '0') {
                            resolve({ code: 1, message: 'success', data: {} });
                        } else {
                            reject(result);
                        }
                    } catch (e) {
                        reject({ code: 0, message: '解析响应失败' });
                    }
                }
            );
        });
    },

    /**
     * 3.18 恢复当前小程序页面至前台
     */
    windowToForeground(): Promise<ApiResponse<{}>> {
        return new Promise((resolve, reject) => {
            callMiniAppEvent(
                'setWindow',
                { hidden: false },
                (v: any) => {
                    try {
                        const result = typeof v === 'string' ? JSON.parse(v) : v;
                        if (result.code === '0') {
                            resolve({ code: 1, message: 'success', data: {} });
                        } else {
                            reject(result);
                        }
                    } catch (e) {
                        reject({ code: 0, message: '解析响应失败' });
                    }
                }
            );
        });
    },


    /**
     * 3.21 展示对话框
     */
    showAlertDialog(params: ShowAlertDialogParams): Promise<ApiResponse<{}>> {
        return new Promise((resolve) => {
            const { title, message, positiveText, negativeText } = params;

            if (alertDialogElement) {
                alertDialogElement.remove();
                alertDialogElement = null;
            }

            const overlay = document.createElement('div');
            overlay.style.cssText = `
            position: fixed;
            top: 0;
            left: 0;
            right: 0;
            bottom: 0;
            display: flex;
            align-items: center;
            justify-content: center;
            background-color: rgba(0, 0, 0, 0.5);
            z-index: 99999;
            font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif;
        `;

            const dialog = document.createElement('div');
            dialog.style.cssText = `
            background: #ffffff;
            border-radius: 12px;
            padding: 0;
            max-width: 320px;
            width: 85%;
            box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
            overflow: hidden;
        `;

            const titleEl = document.createElement('div');
            titleEl.style.cssText = `
            padding: 20px 24px 8px 24px;
            font-size: 17px;
            font-weight: 600;
            color: #1a2332;
            text-align: center;
            line-height: 1.4;
        `;
            titleEl.textContent = title || '提示';

            const msgEl = document.createElement('div');
            msgEl.style.cssText = `
            padding: 8px 24px 20px 24px;
            font-size: 15px;
            color: #5a6a7a;
            text-align: center;
            line-height: 1.5;
            word-break: break-word;
        `;
            msgEl.textContent = message;

            const btnContainer = document.createElement('div');
            btnContainer.style.cssText = `
            display: flex;
            border-top: 1px solid #eaedf2;
        `;

            const hasNegative = negativeText && negativeText.trim() !== '';

            const positiveBtn = document.createElement('button');
            positiveBtn.textContent = positiveText || '确定';
            positiveBtn.style.cssText = `
            flex: 1;
            padding: 14px 0;
            border: none;
            background: transparent;
            font-size: 16px;
            font-weight: 600;
            color: #1a73e8;
            cursor: pointer;
            font-family: inherit;
            ${hasNegative ? 'border-right: 1px solid #eaedf2;' : ''}
        `;
            positiveBtn.onclick = () => {
                // 触发回调：1 表示确定
                if (onAlertDialogButtonClickedCallback) {
                    onAlertDialogButtonClickedCallback(1, () => {}, () => {}, () => {});
                }
                if (alertDialogElement) {
                    alertDialogElement.remove();
                    alertDialogElement = null;
                }
            };

            if (hasNegative) {
                const negativeBtn = document.createElement('button');
                negativeBtn.textContent = negativeText || '取消';
                negativeBtn.style.cssText = `
                flex: 1;
                padding: 14px 0;
                border: none;
                background: transparent;
                font-size: 16px;
                font-weight: 500;
                color: #6c7a8a;
                cursor: pointer;
                font-family: inherit;
            `;
                negativeBtn.onclick = () => {
                    // 触发回调：0 表示取消
                    if (onAlertDialogButtonClickedCallback) {
                        onAlertDialogButtonClickedCallback(0, () => {}, () => {}, () => {});
                    }
                    if (alertDialogElement) {
                        alertDialogElement.remove();
                        alertDialogElement = null;
                    }
                };
                btnContainer.appendChild(negativeBtn);
            }
            btnContainer.appendChild(positiveBtn);

            dialog.appendChild(titleEl);
            dialog.appendChild(msgEl);
            dialog.appendChild(btnContainer);
            overlay.appendChild(dialog);
            document.body.appendChild(overlay);

            alertDialogElement = overlay;

            resolve({ code: 1, message: 'success', data: {} });
        });
    },

    /**
     * 3.22 隐藏对话框
     */
    hideAlertDialog(): Promise<ApiResponse<{}>> {
        return new Promise((resolve, reject) => {
            if (alertDialogElement) {
                alertDialogElement.remove();
                alertDialogElement = null;
            }
            resolve({ code: 1, message: 'success', data: {} });
        });
    },

    /**
     * 3.23 展示 Toast
     */
    showToast(params: ShowToastParams): Promise<ApiResponse<{}>> {
        return new Promise((resolve, reject) => {
            const { message, duration } = params;

            if (toastTimer) {
                clearTimeout(toastTimer);
                toastTimer = null;
            }
            if (toastElement) {
                toastElement.remove();
                toastElement = null;
            }

            let showDuration = 2000;
            if (duration === 1) {
                showDuration = 3500;
            } else if (typeof duration === 'number' && duration > 1) {
                showDuration = duration;
            }

            const toast = document.createElement('div');
            toast.style.cssText = `
            position: fixed;
            left: 50%;
            bottom: 80px;
            transform: translateX(-50%);
            background: rgba(0, 0, 0, 0.78);
            color: #ffffff;
            padding: 10px 24px;
            border-radius: 24px;
            font-size: 14px;
            font-weight: 500;
            font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif;
            box-shadow: 0 8px 30px rgba(0, 0, 0, 0.25);
            max-width: 85vw;
            min-width: 80px;
            text-align: center;
            z-index: 99999;
            border: 1px solid rgba(255, 255, 255, 0.08);
            pointer-events: none;
            word-break: break-word;
            line-height: 1.4;
            text-shadow: 0 1px 2px rgba(0, 0, 0, 0.2);
        `;
            toast.textContent = message;

            document.body.appendChild(toast);
            toastElement = toast;

            toastTimer = window.setTimeout(() => {
                if (toastElement) {
                    toastElement.remove();
                    toastElement = null;
                }
                toastTimer = null;
            }, showDuration);

            resolve({ code: 1, message: 'success', data: {} });
        });
    },

    /**
     * 3.24 展示加载框
     */
    showLoadingView(params?: ShowLoadingViewParams): Promise<ApiResponse<{}>> {
        return new Promise((resolve, reject) => {
            const { loadingText, maxDuration } = params || {};
            const text = loadingText || '加载中...';
            const duration = maxDuration || 10000;

            // 如果已经存在加载框，先移除旧的
            (this as any).hideLoadingView();

            // 创建加载框 DOM 元素
            const loadingDiv = document.createElement('div');
            loadingDiv.id = 'jssdk_loading_view';
            loadingDiv.style.cssText = `
        position: fixed;
        top: 0;
        left: 0;
        right: 0;
        bottom: 0;
        display: flex;
        align-items: center;
        justify-content: center;
        background-color: rgba(0, 0, 0, 0.5);
        z-index: 9999;
        animation: jssdk_fadeIn 0.3s ease;
      `;

            const contentDiv = document.createElement('div');
            contentDiv.style.cssText = `
        display: flex;
        flex-direction: column;
        align-items: center;
        justify-content: center;
        background-color: rgba(0, 0, 0, 0.75);
        border-radius: 8px;
        padding: 24px 32px;
        min-width: 120px;
        min-height: 100px;
      `;

            const spinner = document.createElement('div');
            spinner.style.cssText = `
        width: 40px;
        height: 40px;
        border: 4px solid rgba(255, 255, 255, 0.2);
        border-top-color: #ffffff;
        border-radius: 50%;
        animation: jssdk_spin 0.8s linear infinite;
      `;

            const textEl = document.createElement('div');
            textEl.textContent = text;
            textEl.style.cssText = `
        color: #ffffff;
        font-size: 14px;
        margin-top: 16px;
        font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif;
      `;

            contentDiv.appendChild(spinner);
            contentDiv.appendChild(textEl);
            loadingDiv.appendChild(contentDiv);
            document.body.appendChild(loadingDiv);

            const style = document.createElement('style');
            style.id = 'jssdk_loading_style';
            style.textContent = `
        @keyframes jssdk_spin {
          from { transform: rotate(0deg); }
          to { transform: rotate(360deg); }
        }
        @keyframes jssdk_fadeIn {
          from { opacity: 0; }
          to { opacity: 1; }
        }
      `;
            document.head.appendChild(style);

            (window as any).__jssdk_loading_view = {
                element: loadingDiv,
                style: style,
                timer: null
            };

            if (duration > 0) {
                (window as any).__jssdk_loading_view.timer = setTimeout(() => {
                    (this as any).hideLoadingView();
                }, duration);
            }

            resolve({ code: 1, message: 'success', data: {} });
        });
    },

    /**
     * 3.25 隐藏加载框
     */
    hideLoadingView(): Promise<ApiResponse<{}>> {
        return new Promise((resolve, reject) => {
            try {
                if ((window as any).__jssdk_loading_view?.timer) {
                    clearTimeout((window as any).__jssdk_loading_view.timer);
                }

                if ((window as any).__jssdk_loading_view?.element) {
                    (window as any).__jssdk_loading_view.element.remove();
                }

                if ((window as any).__jssdk_loading_view?.style) {
                    (window as any).__jssdk_loading_view.style.remove();
                }

                (window as any).__jssdk_loading_view = null;

                resolve({ code: 1, message: 'success', data: {} });
            } catch (e) {
                reject({ code: 0, message: '隐藏加载框失败' });
            }
        });
    },

    /**
     * 3.26 保存临时缓存数据
     */
    saveCallCacheData(params: SaveCallCacheDataParams): Promise<ApiResponse<{}>> {
        return new Promise((resolve, reject) => {
            const { data } = params;
            if (!data) {
                reject({ code: 0, message: 'data is required' });
                return;
            }

            callMiniAppEvent(
                'getMiniAppInfo',
                {},
                (appInfoResult: any) => {
                    try {
                        const appData = typeof appInfoResult === 'string' ? JSON.parse(appInfoResult) : appInfoResult;
                        if (appData.code !== '0') {
                            reject({ code: 0, message: 'Failed to get appId' });
                            return;
                        }

                        const telecomCallId = appData.data?.telecomCallId;
                        if (!telecomCallId) {
                            reject({ code: 0, message: 'get telecomCallId failed' });
                            return;
                        }

                        const cacheKey = telecomCallId + '_callCache';
                        const cacheValue = JSON.stringify(data);

                        callFileEvent(
                            'saveUpdateKeyValueWithExpiry',
                            {
                                key: cacheKey,
                                value: cacheValue,
                                ttl: '259200000'
                            },
                            (v: any) => {
                                try {
                                    const result = typeof v === 'string' ? JSON.parse(v) : v;
                                    if (result.code === '0') {
                                        resolve({ code: 1, message: 'success', data: {} });
                                    } else {
                                        reject(result);
                                    }
                                } catch (e) {
                                    reject({ code: 0, message: '解析响应失败' });
                                }
                            }
                        );
                    } catch (e) {
                        reject({ code: 0, message: '解析 getMiniAppInfo 响应失败' });
                    }
                }
            );
        });
    },

    /**
     * 3.27 获取临时缓存数据
     */
    getCallCacheData(): Promise<ApiResponse<any>> {
        return new Promise((resolve, reject) => {
            callMiniAppEvent(
                'getMiniAppInfo',
                {},
                (appInfoResult: any) => {
                    try {
                        const appData = typeof appInfoResult === 'string' ? JSON.parse(appInfoResult) : appInfoResult;
                        if (appData.code !== '0') {
                            reject({ code: 0, message: 'Failed to get appId' });
                            return;
                        }

                        const telecomCallId = appData.data?.telecomCallId;
                        if (!telecomCallId) {
                            reject({ code: 0, message: 'get telecomCallId failed' });
                            return;
                        }

                        const cacheKey = telecomCallId + '_callCache';

                        callFileEvent(
                            'getKeyValue',
                            { key: cacheKey },
                            (v: any) => {
                                try {
                                    const result = typeof v === 'string' ? JSON.parse(v) : v;
                                    if (result.code === '0') {
                                        const cacheValue = (result.data?.value) || '';
                                        let parsedData = {};
                                        try {
                                            parsedData = cacheValue ? JSON.parse(cacheValue) : {};
                                        } catch (e) {
                                            parsedData = {};
                                        }
                                        resolve({
                                            code: 1,
                                            message: 'success',
                                            data: parsedData
                                        });
                                    } else {
                                        resolve({
                                            code: 1,
                                            message: 'success',
                                            data: {}
                                        });
                                    }
                                } catch (e) {
                                    reject({ code: 0, message: '解析响应失败' });
                                }
                            }
                        );
                    } catch (e) {
                        reject({ code: 0, message: '解析 getMiniAppInfo 响应失败' });
                    }
                }
            );
        });
    },

    /**
     * 获取对方号码
     */
    getRemoteNumber(): Promise<ApiResponse<{ remoteNumber: string }>> {
        return new Promise((resolve, reject) => {
            callMiniAppEvent(
                'getRemoteNumber',
                {},
                (v: any) => {
                    try {
                        const result = typeof v === 'string' ? JSON.parse(v) : v;
                        if (result.code === '0') {
                            resolve({
                                code: 1,
                                message: 'success',
                                data: {
                                    remoteNumber: (result.data?.remoteNumber) || ''
                                }
                            });
                        } else {
                            reject(result);
                        }
                    } catch (e) {
                        reject({ code: 0, message: '解析响应失败' });
                    }
                }
            );
        });
    },

    /**
     * 播放 DTMF
     */
    playDtmfTone(params: PlayDtmfToneParams): Promise<ApiResponse<{}>> {
        return new Promise((resolve, reject) => {
            const { license, digit } = params;
            if (!license || !digit) {
                reject({ code: 0, message: 'license and digit are required' });
                return;
            }

            callMiniAppEvent(
                'playDtmfTone',
                { license: license, digit: digit },
                (v: any) => {
                    try {
                        const result = typeof v === 'string' ? JSON.parse(v) : v;
                        if (result.code === '0') {
                            resolve({ code: 1, message: 'success', data: {} });
                        } else {
                            reject(result);
                        }
                    } catch (e) {
                        reject({ code: 0, message: '解析响应失败' });
                    }
                }
            );
        });
    },

    /**
     * 设置系统API授权许可
     * @param params.license 授权许可证
     * @param params.api 需要授权的系统API名称，如 "getUserMedia"
     */
    setSystemApiLicense(params: SetSystemApiLicenseParams): Promise<ApiResponse<{}>> {
        return new Promise((resolve, reject) => {
            const { license, api } = params;
            if (!license) {
                reject({ code: 0, message: 'license is required' });
                return;
            }
            if (!api) {
                reject({ code: 0, message: 'api is required' });
                return;
            }

            callMiniAppEvent(
                'setSystemApiLicense',
                { license: license, api: api },
                (v: any) => {
                    try {
                        const result = typeof v === 'string' ? JSON.parse(v) : v;
                        if (result.code === '0') {
                            resolve({ code: 1, message: 'success', data: {} });
                        } else {
                            reject(result);
                        }
                    } catch (e) {
                        reject({ code: 0, message: '解析响应失败' });
                    }
                }
            );
        });
    }
};

// ==================== 4. 回调插件 ====================

export const callbackPlugin = {
    /**
     * DC 状态变化通知
     */
    dcNotified(callback: (data: any, resolve: any, reject: any, progress: any) => void): void {
        dcNotifiedCallback = callback;
    },

    /**
     * DC 消息通知
     */
    messageNotified(callback: (data: any, resolve: any, reject: any, progress: any) => void): void {
        messageNotifiedCallback = callback;
    },

    /**
     * 通话状态变化通知
     */
    callStateNotified(callback: (data: any, resolve: any, reject: any, progress: any) => void): void {
        callStateNotifiedCallback = callback;
    },

    /**
     * 生命周期变化通知
     */
    onLifecycleChanged(callback: (data: any, resolve: any, reject: any, progress: any) => void): void {
        lifecycleChangedCallback = callback;
    },

    /**
     * 对话框按钮点击事件
     */
    onAlertDialogButtonClicked(callback: (result: number, resolve: any, reject: any, progress: any) => void): void {
        onAlertDialogButtonClickedCallback = callback;
    },

    /**
     * 画板轨迹通知 - 电信特有
     */
    drawingInfoNotify(callback: (data: any, resolve: any, reject: any, progress: any) => void): void {
        drawingInfoNotifyCallback = callback;
    },

    /**
     * 画板状态通知 - 电信特有
     */
    sketchStatusNotify(callback: (data: any, resolve: any, reject: any, progress: any) => void): void {
        sketchStatusNotifyCallback = callback;
    },

    /**
     * 对端响应拉起小程序通知 - 电信特有
     */
    startAdverseAppResponseNotify(callback: (data: any, resolve: any, reject: any, progress: any) => void): void {
        startAdverseAppResponseNotifyCallback = callback;
    },

    /**
     * 音频设备变化通知 - 电信特有
     */
    audioDeviceChangeNotify(callback: (data: any, resolve: any, reject: any, progress: any) => void): void {
        audioDeviceChangeNotifyCallback = callback;
    },

    /**
     * 输入法高度变化通知 - 电信特有
     */
    imeHeightNotify(callback: (data: any, resolve: any, reject: any, progress: any) => void): void {
        imeHeightNotifyCallback = callback;
    },

    /**
     * 拓展能力通知 - 电信特有
     */
    expandingCapacityNotify(callback: (data: any, resolve: any, reject: any, progress: any) => void): void {
        expandingCapacityNotifyCallback = callback;
    }
};

// ==================== 5. 屏幕共享与画板插件 ====================

export const screenSharePlugin = {
    startScreenShare(params: ScreenShareParams): Promise<ApiResponse<{ resultStatus: string }>> {
        return new Promise((resolve, reject) => {
            const { license } = params;
            if (!license) {
                reject({ code: 0, message: 'license is required' });
                return;
            }

            callScreenShareEvent(
                'startScreenShare',
                { license: license },
                (v: any) => {
                    try {
                        const result = typeof v === 'string' ? JSON.parse(v) : v;
                        if (result.code === '0') {
                            resolve({
                                code: 1,
                                message: 'success',
                                data: {
                                    resultStatus: (result.data?.resultStatus) || '0'
                                }
                            });
                        } else {
                            reject(result);
                        }
                    } catch (e) {
                        reject({ code: 0, message: '解析响应失败' });
                    }
                }
            );
        });
    },

    stopScreenShare(): Promise<ApiResponse<{}>> {
        return new Promise((resolve, reject) => {
            callScreenShareEvent(
                'stopScreenShare',
                {},
                (v: any) => {
                    try {
                        const result = typeof v === 'string' ? JSON.parse(v) : v;
                        if (result.code === '0') {
                            resolve({ code: 1, message: 'success', data: {} });
                        } else {
                            reject(result);
                        }
                    } catch (e) {
                        reject({ code: 0, message: '解析响应失败' });
                    }
                }
            );
        });
    },

    requestScreenShareAbility(): Promise<ApiResponse<{ result: string }>> {
        return new Promise((resolve, reject) => {
            callScreenShareEvent(
                'requestScreenShareAbility',
                {},
                (v: any) => {
                    try {
                        const result = typeof v === 'string' ? JSON.parse(v) : v;
                        if (result.code === '0') {
                            resolve({
                                code: 1,
                                message: 'success',
                                data: {
                                    result: (result.data?.result) || '0'
                                }
                            });
                        } else {
                            reject(result);
                        }
                    } catch (e) {
                        reject({ code: 0, message: '解析响应失败' });
                    }
                }
            );
        });
    },

    openSketchBoard(params?: OpenSketchBoardParams): Promise<ApiResponse<{}>> {
        return new Promise((resolve, reject) => {
            callScreenShareEvent(
                'openSketchBoard',
                {
                    paintColor: (params?.paintColor) || '#ffff4444',
                    paintWidth: (params?.paintWidth) || '8.0'
                },
                (v: any) => {
                    try {
                        const result = typeof v === 'string' ? JSON.parse(v) : v;
                        if (result.code === '0') {
                            resolve({ code: 1, message: 'success', data: {} });
                        } else {
                            reject(result);
                        }
                    } catch (e) {
                        reject({ code: 0, message: '解析响应失败' });
                    }
                }
            );
        });
    },

    closeSketchBoard(): Promise<ApiResponse<{}>> {
        return new Promise((resolve, reject) => {
            callScreenShareEvent(
                'closeSketchBoard',
                {},
                (v: any) => {
                    try {
                        const result = typeof v === 'string' ? JSON.parse(v) : v;
                        if (result.code === '0') {
                            resolve({ code: 1, message: 'success', data: {} });
                        } else {
                            reject(result);
                        }
                    } catch (e) {
                        reject({ code: 0, message: '解析响应失败' });
                    }
                }
            );
        });
    },

    addDrawingInfo(params: AddDrawingInfoParams): Promise<ApiResponse<{}>> {
        return new Promise((resolve, reject) => {
            const { drawingInfo } = params;
            if (!drawingInfo) {
                reject({ code: 0, message: 'drawingInfo is required' });
                return;
            }

            callScreenShareEvent(
                'addDrawingInfo',
                { drawingInfo: drawingInfo },
                (v: any) => {
                    try {
                        const result = typeof v === 'string' ? JSON.parse(v) : v;
                        if (result.code === '0') {
                            resolve({ code: 1, message: 'success', data: {} });
                        } else {
                            reject(result);
                        }
                    } catch (e) {
                        reject({ code: 0, message: '解析响应失败' });
                    }
                }
            );
        });
    }
};

// ==================== 6. 拓展能力插件 ====================

export const ecPlugin = {
    expandingCapacityRegister(params: ExpandingCapacityRegisterParams): Promise<ApiResponse<{}>> {
        return new Promise((resolve, reject) => {
            const { providerModules } = params;
            if (!providerModules || !Array.isArray(providerModules)) {
                reject({ code: 0, message: 'providerModules is required and must be an array' });
                return;
            }

            callECEvent(
                'expandingCapacityRegister',
                { providerModules: providerModules },
                (v: any) => {
                    try {
                        const result = typeof v === 'string' ? JSON.parse(v) : v;
                        if (result.code === '0') {
                            resolve({ code: 1, message: 'success', data: {} });
                        } else {
                            reject(result);
                        }
                    } catch (e) {
                        reject({ code: 0, message: '解析响应失败' });
                    }
                }
            );
        });
    },

    expandingCapacityRequest(params: ExpandingCapacityRequestParams): Promise<ApiResponse<{}>> {
        return new Promise((resolve, reject) => {
            const { provider, module, func, data } = params;
            if (!provider || !module || !func) {
                reject({ code: 0, message: 'provider, module and func are required' });
                return;
            }

            callECEvent(
                'expandingCapacityRequest',
                {
                    provider: provider,
                    module: module,
                    func: func,
                    data: data || {}
                },
                (v: any) => {
                    try {
                        const result = typeof v === 'string' ? JSON.parse(v) : v;
                        if (result.code === '0') {
                            resolve({ code: 1, message: 'success', data: {} });
                        } else {
                            reject(result);
                        }
                    } catch (e) {
                        reject({ code: 0, message: '解析响应失败' });
                    }
                }
            );
        });
    },

    expandingCapacityQuery(): Promise<ApiResponse<any>> {
        return new Promise((resolve, reject) => {
            callECEvent(
                'expandingCapacityQuery',
                {},
                (v: any) => {
                    try {
                        const result = typeof v === 'string' ? JSON.parse(v) : v;
                        if (result.code === '0') {
                            resolve({
                                code: 1,
                                message: 'success',
                                data: result.data || {}
                            });
                        } else {
                            reject(result);
                        }
                    } catch (e) {
                        reject({ code: 0, message: '解析响应失败' });
                    }
                }
            );
        });
    }
};

// ==================== 创建 plugins 对象 ====================

export const plugins = {
    dcPlugin,
    mediaPlugin,
    bizPlugin,
    callbackPlugin,
    screenSharePlugin,
    ecPlugin
};

// 默认导出 plugins
export default plugins;

// ==================== 类型定义 ====================

type ReadyState = 'connecting' | 'open' | 'closing' | 'closed';

interface RTCDataChannelEventInit {
    channel: RTCDataChannel;
}

interface MessageEventInit {
    data: ArrayBuffer;
}

interface RTCPeerConnectionConfiguration {
    [key: string]: any;
}

interface DataChannelNotifyResponse {
    dcLabel: string;
    imsDCStatus: number;
}

interface MessageNotifyResponse {
    dcLabel: string;
    message: string;
}

interface CreateDataChannelResponse {
    code: string;
}

// ==================== MessageEvent Polyfill ====================

class MessageEvent {
    type: string;
    data: any;
    bubbles: boolean;
    cancelable: boolean;

    constructor(type: string, eventInitDict: MessageEventInit) {
        this.type = type;
        this.data = eventInitDict.data;
        this.bubbles = false;
        this.cancelable = false;
    }
}

// ==================== RTCDataChannel ====================

export class RTCDataChannel {
    bufferedAmount: number = 0;
    label: string | null = null;
    readyState: ReadyState = 'connecting';

    onclose: ((channel: RTCDataChannel, event: Event) => void) | null = null;
    onclosing: ((channel: RTCDataChannel, event: Event) => void) | null = null;
    onerror: ((errorCode: number) => void) | null = null;
    onmessage: ((event: MessageEvent) => void) | null = null;
    onopen: ((channel: RTCDataChannel, event: Event) => void) | null = null;

    /**
     * 关闭 DC 通道
     */
    close(): void {
        if (!this.label) return;
        mDCBridge.call(
            'async',
            {
                event: 'DcEvent',
                function: 'closeAppDataChannel',
                params: {
                    dcLabel: [this.label]
                }
            },
            () => {}
        );
    }

    /**
     * 发送数据（ArrayBuffer）
     */
    send(arrayBuffer: ArrayBuffer): void {
        if (!this.label) return;
        const bytes = new Uint8Array(arrayBuffer);
        let binary = '';
        for (let i = 0; i < bytes.length; i++) {
            binary += String.fromCharCode(bytes[i]);
        }
        const base64String = btoa(binary);

        const that = this;
        mDCBridge.call(
            'async',
            {
                event: 'DcEvent',
                function: 'sendData',
                params: {
                    dcLabel: this.label,
                    data: base64String
                }
            },
            function(v: string) {
                try {
                    const res = JSON.parse(v);
                    if (that.onerror !== null && res.code !== '0') {
                        that.onerror(res.code);
                    }
                } catch (e) {
                    // ignore
                }
            }
        );
    }

    /**
     * 查询 buffer 剩余大小
     */
    getBufferedAmount(callback: (amount: number) => void): void {
        if (!this.label) {
            callback(0);
            return;
        }
        mDCBridge.call(
            'async',
            {
                event: 'DcEvent',
                function: 'getBufferedAmount',
                params: {
                    dcLabel: this.label
                }
            },
            (v: string) => {
                try {
                    const response = JSON.parse(v);
                    this.bufferedAmount = response.data?.bufferedAmount || 0;
                    callback(this.bufferedAmount);
                } catch (e) {
                    callback(0);
                }
            }
        );
    }

    /**
     * 处理 DC 状态通知
     */
    channelDataChannelNotify(imsDCStatus: number): void {
        switch (imsDCStatus) {
            case 0:
                this.readyState = 'connecting';
                break;
            case 1:
                this.readyState = 'open';
                if (this.onopen !== null) {
                    this.onopen(this, new Event('open'));
                }
                break;
            case 2:
                this.readyState = 'closing';
                if (this.onclosing !== null) {
                    this.onclosing(this, new Event('closing'));
                }
                break;
            case 3:
                this.readyState = 'closed';
                if (this.onclose !== null) {
                    this.onclose(this, new Event('close'));
                }
                break;
            default:
                break;
        }
    }

    /**
     * 处理 DC 消息通知
     */
    channelMessageNotify(msg: string): void {
        try {
            const byteCharacters = atob(msg);
            const byteArray = new Uint8Array(byteCharacters.length);
            for (let i = 0; i < byteCharacters.length; i++) {
                byteArray[i] = byteCharacters.charCodeAt(i);
            }
            if (this.onmessage !== null) {
                const messageEvent = new MessageEvent('message', { data: byteArray.buffer });
                this.onmessage(messageEvent);
            }
        } catch (e) {
            // 解码失败时忽略
        }
    }
}

// ==================== RTCDataChannelEvent ====================

export class RTCDataChannelEvent extends Event {
    channel: RTCDataChannel;
    bubbles: boolean = false;
    cancelable: boolean = false;

    constructor(type: string, eventInitDict: RTCDataChannelEventInit) {
        super(type);
        this.channel = eventInitDict.channel;
    }
}

// ==================== RTCConfiguration ====================

export class RTCConfiguration {
    // 根据实际需要扩展
}

// ==================== RTCPeerConnection ====================

export class RTCPeerConnection {
    configuration: RTCPeerConnectionConfiguration;
    ondatachannel: ((event: RTCDataChannelEvent) => void) | null = null;
    channelMap: Map<string, RTCDataChannel> = new Map();

    constructor(configuration: RTCPeerConnectionConfiguration = {}) {
        this.configuration = configuration;
    }

    /**
     * ★ 新增：处理 DC 状态通知（由 registerTelecomCallbacks 调用）
     */
    handleDataChannelNotify(data: { dcLabel: string; imsDCStatus: number }): void {
        const label = data.dcLabel;
        const imsDCStatus = data.imsDCStatus;

        let channel = this.channelMap.get(label);

        if (channel === undefined && this.ondatachannel !== null) {
            // 对端创建的 channel
            channel = new RTCDataChannel();
            channel.label = label;
            this.channelMap.set(label, channel);
            // ★ 注册到实例映射
            registerRTCInstance(this, [label]);
            const event = new RTCDataChannelEvent('datachannel', { channel });
            this.ondatachannel(event);
        }

        if (channel) {
            channel.channelDataChannelNotify(imsDCStatus);
            if (imsDCStatus === 3) {
                this.channelMap.delete(label);
                unregisterRTCInstance([label]);
            }
        }
    }

    /**
     * ★ 新增：处理 DC 消息通知（由 registerTelecomCallbacks 调用）
     */
    handleMessageNotify(data: { dcLabel: string; message: string }): void {
        const dcLabel = data.dcLabel;
        const msg = data.message;
        const channel = this.channelMap.get(dcLabel);
        if (channel) {
            channel.channelMessageNotify(msg);
        }
    }

    /**
     * 从 JSON 中提取 DcLabel
     */
    private dcLabelFromJson(json: string): string {
        try {
            const parsed = JSON.parse(json);
            return parsed?.dcLabel || '';
        } catch (e) {
            return '';
        }
    }

    /**
     * 创建 DC 通道
     */
    createDataChannel(jsonStr: string, dataChannelDict?: any): RTCDataChannel {
        const label = this.dcLabelFromJson(jsonStr);
        let channel = this.channelMap.get(label);
        if (channel !== undefined) {
            return channel;
        }

        channel = new RTCDataChannel();
        channel.label = label;
        this.channelMap.set(label, channel);
        // ★ 注册到实例映射
        registerRTCInstance(this, [label]);
        this.createADC(label, jsonStr);
        return channel;
    }

    /**
     * 实际创建 DC（调用 Native）
     */
    private createADC(dcLabel: string, jsonStr: string): void {
        const that = this;
        mDCBridge.call(
            'async',
            {
                event: 'DcEvent',
                function: 'createAppDataChannel',
                params: {
                    dcLabels: [dcLabel],
                    DataChannelAppInfoXml: '',
                    DataChannelAppInfoJson: jsonStr
                }
            },
            function(v: string) {
                try {
                    const response: CreateDataChannelResponse = JSON.parse(v);
                    const code = response.code;
                    if (code !== '0') {
                        const channel = that.channelMap.get(dcLabel);
                        if (channel !== undefined && channel.onerror !== null) {
                            channel.onerror(parseInt(code));
                        }
                    }
                } catch (e) {
                    // ignore
                }
            }
        );
    }

    /**
     * 关闭连接，清理资源
     */
    close(): void {
        this.ondatachannel = null;
        // ★ 从实例映射中移除
        const labels = Array.from(this.channelMap.keys());
        unregisterRTCInstance(labels);
        this.channelMap.clear();
        try {
            // dsbridge 可能没有 unregister 方法
        } catch (e) {
            // ignore
        }
    }

    /**
     * 获取所有通道
     */
    getChannels(): Map<string, RTCDataChannel> {
        return this.channelMap;
    }

    /**
     * 获取指定通道
     */
    getChannel(label: string): RTCDataChannel | undefined {
        return this.channelMap.get(label);
    }

    /**
     * 删除指定通道
     */
    deleteChannel(label: string): boolean {
        const deleted = this.channelMap.delete(label);
        if (deleted) {
            unregisterRTCInstance([label]);
        }
        return deleted;
    }
}

// ==================== 挂载到全局 ====================

// 将类挂载到 window 上，供全局使用
if (typeof window !== 'undefined') {
    (window as any).RTCPeerConnection = RTCPeerConnection;
    (window as any).RTCDataChannel = RTCDataChannel;
    (window as any).RTCDataChannelEvent = RTCDataChannelEvent;
    (window as any).RTCConfiguration = RTCConfiguration;
}