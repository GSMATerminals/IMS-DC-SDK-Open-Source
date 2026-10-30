<template>
  <div class="mini-app-container">
    <div class="header">
      <h2>{{ appName }}</h2>
    </div>
    <div class="content">
      <!-- DC数据通道 -->
      <div class="section">
        <h3>DC数据通道</h3>
        <button @click="createDC2">创建DC通道</button>
        <button @click="sendData">发送数据</button>
        <button @click="closeDC">关闭DC通道</button>
        <button @click="ifPeerSupportDC">查询对端是否支持DC</button>
        <button @click="queryDCFreeBufferSize">查询DC缓存大小</button>
      </div>
      <!-- 媒体插件 -->
      <div class="section">
        <h3>媒体插件</h3>
        <button @click="queryImages">查询图片</button>
        <button @click="queryVideos">查询视频</button>
        <button @click="checkFiles">检查文件</button>
        <button @click="queryFiles">查询文件</button>
        <button @click="saveFile">保存文件</button>
        <button @click="saveKeyValueData">保存KV数据</button>
        <button @click="getKeyValueData">获取KV数据</button>
        <button @click="deleteKeyValue">删除KV数据</button>
        <button @click="dealCompressedFile">解压缩文件</button>
        <button @click="getLocation">获取定位</button>
        <button @click="getFileByPath">按路径获取文件</button>
        <button @click="chooseImage">选择图片</button>
        <button @click="chooseVideo">选择视频</button>
        <button @click="deleteFile">删除文件</button>
        <button @click="searchFiles">搜索文件</button>
      </div>
      <!-- 业务功能插件 -->
      <div class="section">
        <h3>业务功能插件</h3>
        <button @click="getMiniAppInfo">获取小程序信息</button>
        <button @click="openApp">打开应用</button>
        <button @click="setWindow">设置窗口</button>
        <button @click="getContactList">获取联系人列表</button>
        <button @click="endCall">挂断通话</button>
        <button @click="answer">接听通话</button>
        <button @click="getCallState">获取通话状态</button>
        <button @click="setSpeakerphone">设置扬声器</button>
        <button @click="isSpeakerphoneOn">查询扬声器状态</button>
        <button @click="setMuted">设置静音</button>
        <button @click="isMuted">查询静音状态</button>
        <button @click="getContactName">获取联系人名称</button>
        <button @click="addOrEditContact">添加/编辑联系人</button>
        <button @click="closeWindow">关闭窗口</button>
        <button @click="getVersions">获取版本信息</button>
        <button @click="openRemoteApp">打开远程应用</button>
        <button @click="windowToBack">窗口置后</button>
        <button @click="windowToForeground">窗口置前</button>
        <button @click="showAlertDialog">显示对话框</button>
        <button @click="hideAlertDialog">隐藏对话框</button>
        <button @click="showToast">显示Toast</button>
        <button @click="showLoadingView">显示Loading</button>
        <button @click="hideLoadingView">隐藏Loading</button>
        <button @click="saveCallCacheData">保存通话缓存</button>
        <button @click="getCallCacheData">获取通话缓存</button>
        <button @click="getRemoteNumber">获取对端号码</button>
        <button @click="playDtmfTone">播放DTMF</button>
        <button @click="setSystemApiLicense">设置系统API授权</button>
      </div>
      <!-- 屏幕共享与画板 -->
      <div class="section">
        <h3>屏幕共享与画板</h3>
        <button @click="startScreenShare">开始屏幕共享</button>
        <button @click="stopScreenShare">停止屏幕共享</button>
        <button @click="requestScreenShareAbility">请求共享能力</button>
        <button @click="openSketchBoard">打开画板</button>
        <button @click="closeSketchBoard">关闭画板</button>
        <button @click="addDrawingInfo">添加绘图信息</button>
      </div>
      <!-- 拓展能力 -->
      <div class="section">
        <h3>拓展能力</h3>
        <button @click="expandingCapacityRegister">注册拓展能力</button>
        <button @click="expandingCapacityRequest">请求拓展能力</button>
        <button @click="expandingCapacityQuery">查询拓展能力</button>
      </div>
      <!-- HTTP代理 -->
      <div class="section">
        <h3>HTTP代理</h3>
        <button @click="httpGet">HTTP GET</button>
        <button @click="httpPost">HTTP POST</button>
        <button @click="httpPostFormData">HTTP POST FormData</button>
        <button @click="httpPut">HTTP PUT</button>
        <button @click="httpDelete">HTTP DELETE</button>
        <button @click="httpPatch">HTTP PATCH</button>
        <button @click="httpHead">HTTP HEAD</button>
      </div>
      <!-- 日志区域 -->
      <div class="log-area">
        <p v-for="(log, index) in logs" :key="index">{{ log }}</p>
      </div>
    </div>
  </div>
</template>

<script>
import plugins from '../../local_modules/jssdk'
import axios from 'axios'

export default {
  name: 'MiniApp',
  data() {
    return {
      appName: '示例小程序',
      dcLabels: [],
      logs: [],
      registered: false
    }
  },
  mounted() {
    this.initCallbacks()
    this.getMiniAppInfo()
  },
  methods: {
    addLog(type, label, detail) {
      const time = new Date().toLocaleTimeString()
      const detailStr = detail ? (typeof detail === 'string' ? detail : JSON.stringify(detail)) : ''
      this.logs.push(`[${time}] [${type}] ${label}${detailStr ? ' ' + detailStr : ''}`)
    },

    callApi(plugin, action, params, desc) {
      this.addLog('request', `📤 ${desc}`, { plugin: plugin.constructor.name, action, params })
      return plugin[action](params).then(data => {
        this.addLog('response', `📥 ${desc}`, data)
        return data
      }).catch(error => {
        this.addLog('error', `❌ ${desc}`, error?.message || error)
        throw error
      })
    },

    initCallbacks() {
      if (this.registered) return
      this.registered = true

      // DC状态变化通知
      plugins.callbackPlugin.dcNotified((data) => {
        this.addLog('callback', '📋 DC状态变化', data)
      })

      // DC消息通知
      plugins.callbackPlugin.messageNotified((data) => {
        this.addLog('callback', '📋 DC消息通知', data)
      })

      // 通话状态变化通知
      plugins.callbackPlugin.callStateNotified((data) => {
        this.addLog('callback', '📋 通话状态变化', data)
      })

      // 生命周期变化通知
      plugins.callbackPlugin.onLifecycleChanged((data) => {
        this.addLog('callback', '📋 生命周期变化', data)
      })

      // 对话框按钮点击通知
      plugins.callbackPlugin.onAlertDialogButtonClicked((result) => {
        this.addLog('callback', '📋 对话框按钮点击', result)
      })

      // 逆向应用响应通知
      plugins.callbackPlugin.startAdverseAppResponseNotify((data) => {
        this.addLog('callback', '📋 逆向应用响应', data)
      })

      // 音频设备变化通知
      plugins.callbackPlugin.audioDeviceChangeNotify((data) => {
        this.addLog('callback', '📋 音频设备变化', data)
      })

      // 输入法高度变化通知
      plugins.callbackPlugin.imeHeightNotify((data) => {
        this.addLog('callback', '📋 输入法高度变化', data)
      })

      // 画板绘图信息通知
      plugins.callbackPlugin.drawingInfoNotify((data) => {
        this.addLog('callback', '📋 画板绘图信息', data)
      })

      // 画板状态通知
      plugins.callbackPlugin.sketchStatusNotify((data) => {
        this.addLog('callback', '📋 画板状态', data)
      })

      // 拓展能力通知
      plugins.callbackPlugin.expandingCapacityNotify((data) => {
        this.addLog('callback', '📋 拓展能力通知', data)
      })
    },

    // ==================== 1. DC数据通道接口 ====================

    createDC2() {
      this.callApi(plugins.dcPlugin, 'createDC2', {
        dcParams: [{
          DcId: "test1",
          UseCase: "2"
        }]
      }, '创建DC通道').then(data => {
        if (data.code === 1) {
          this.dcLabels = data.data.dcLabels
        }
      })
    },

    sendData() {
      if (this.dcLabels.length === 0) {
        this.addLog('warning', '⚠️ 请先创建DC通道')
        return
      }
      this.callApi(plugins.dcPlugin, 'sendData', {
        dcLabel: this.dcLabels[0],
        data: "Hello World"
      }, '发送数据')
    },

    closeDC() {
      if (this.dcLabels.length === 0) {
        this.addLog('warning', '⚠️ 请先创建DC通道')
        return
      }
      this.callApi(plugins.dcPlugin, 'closeDC', {
        dcLabel: [this.dcLabels[0]]
      }, '关闭DC通道').then(data => {
        if (data.code === 1) {
          this.dcLabels = []
        }
      })
    },

    ifPeerSupportDC() {
      this.callApi(plugins.dcPlugin, 'ifPeerSupportDC', null, '查询对端是否支持DC')
    },

    queryDCFreeBufferSize() {
      if (this.dcLabels.length === 0) {
        this.addLog('warning', '⚠️ 请先创建DC通道')
        return
      }
      this.callApi(plugins.dcPlugin, 'queryDCFreeBufferSize', {
        dcLabel: this.dcLabels[0]
      }, '查询DC缓存大小')
    },

    // ==================== 2. 媒体插件接口 ====================

    queryImages() {
      this.callApi(plugins.mediaPlugin, 'queryImages', {
        path: "/storage/emulated/0/DCIM",
        offset: "0",
        size: "20"
      }, '查询图片')
    },

    queryVideos() {
      this.callApi(plugins.mediaPlugin, 'queryVideos', {
        path: "/storage/emulated/0/DCIM",
        offset: "0",
        size: "20"
      }, '查询视频')
    },

    checkFiles() {
      this.callApi(plugins.mediaPlugin, 'checkFiles', {
        path: ["/storage/emulated/0/test.jpg", "/sdcard/test.txt"]
      }, '检查文件')
    },

    queryFiles() {
      this.callApi(plugins.mediaPlugin, 'queryFiles', {
        contentType: "application/pdf",
        path: "/storage/emulated/0",
        offset: "0",
        size: "20"
      }, '查询文件')
    },

    saveFile() {
      this.callApi(plugins.mediaPlugin, 'saveFile', {
        file: fileData,
        filePathType: 0,
        filePath: "/test/test.txt"
      }, '保存文件')
    },

    saveKeyValueData() {
      this.callApi(plugins.mediaPlugin, 'saveKeyValueData', {
        key: "myKey",
        value: "myValue"
      }, '保存KV数据')
    },

    getKeyValueData() {
      this.callApi(plugins.mediaPlugin, 'getKeyValueData', {
        key: "myKey"
      }, '获取KV数据')
    },

    deleteKeyValue() {
      this.callApi(plugins.mediaPlugin, 'deleteKeyValue', {
        key: "myKey"
      }, '删除KV数据')
    },

    dealCompressedFile() {
      this.callApi(plugins.mediaPlugin, 'dealCompressedFile', {
        compressedFilePath: "/storage/emulated/0/test.zip",
        filePathType: 1,
        filePath: "/storage/emulated/0/test_dir",
        type: "zip"
      }, '解压缩文件')
    },

    getLocation() {
      this.callApi(plugins.mediaPlugin, 'getLocation', null, '获取定位')
    },

    getFileByPath() {
      this.callApi(plugins.mediaPlugin, 'getFileByPath', {
        filePath: "/storage/emulated/0/test.txt"
      }, '按路径获取文件')
    },

    chooseImage() {
      this.callApi(plugins.mediaPlugin, 'chooseImage', {
        count: 3
      }, '选择图片')
    },

    chooseVideo() {
      this.callApi(plugins.mediaPlugin, 'chooseVideo', null, '选择视频')
    },

    deleteFile() {
      this.callApi(plugins.mediaPlugin, 'deleteFile', {
        path: "/test/test.txt"
      }, '删除文件')
    },

    searchFiles() {
      this.callApi(plugins.mediaPlugin, 'searchFiles', {
        keywords: '["项目申请书","pdf"]'
      }, '搜索文件')
    },

    // ==================== 3. 业务功能插件接口 ====================

    getMiniAppInfo() {
      this.callApi(plugins.bizPlugin, 'getMiniAppInfo', null, '获取小程序信息').then(data => {
        if (data.code === 1) {
          this.appName = data.data.appName
        }
      })
    },

    openApp() {
      this.callApi(plugins.bizPlugin, 'openApp', {
        jumpType: "openCameraToPic",
        extras: ""
      }, '打开应用')
    },

    setWindow() {
      this.callApi(plugins.bizPlugin, 'setWindow', {
        isFullScreen: false,
        statusBarColor: "#FFFFFF",
        pageName: "我的小程序",
        width: 1.0,
        height: 0.5
      }, '设置窗口')
    },

    getContactList() {
      this.callApi(plugins.bizPlugin, 'getContactList', {
        offset: "0",
        limit: "50"
      }, '获取联系人列表')
    },

    endCall() {
      this.callApi(plugins.bizPlugin, 'endCall', null, '挂断通话')
    },

    answer() {
      this.callApi(plugins.bizPlugin, 'answer', null, '接听通话')
    },

    getCallState() {
      this.callApi(plugins.bizPlugin, 'getCallState', null, '获取通话状态')
    },

    setSpeakerphone() {
      this.callApi(plugins.bizPlugin, 'setSpeakerphone', {
        speakerphoneOn: true
      }, '设置扬声器')
    },

    isSpeakerphoneOn() {
      this.callApi(plugins.bizPlugin, 'isSpeakerphoneOn', null, '查询扬声器状态')
    },

    setMuted() {
      this.callApi(plugins.bizPlugin, 'setMuted', {
        muted: true
      }, '设置静音')
    },

    isMuted() {
      this.callApi(plugins.bizPlugin, 'isMuted', null, '查询静音状态')
    },

    getContactName() {
      this.callApi(plugins.bizPlugin, 'getContactName', {
        contactNumber: "13800138000"
      }, '获取联系人名称')
    },

    addOrEditContact() {
      this.callApi(plugins.bizPlugin, 'addOrEditContact', {
        contactMode: "phone",
        name: "张三",
        number: "13800138000"
      }, '添加/编辑联系人')
    },

    closeWindow() {
      this.callApi(plugins.bizPlugin, 'closeWindow', null, '关闭窗口')
    },

    getVersions() {
      this.callApi(plugins.bizPlugin, 'getVersions', null, '获取版本信息')
    },

    openRemoteApp() {
      this.callApi(plugins.bizPlugin, 'openRemoteApp', null, '打开远程应用')
    },

    windowToBack() {
      this.callApi(plugins.bizPlugin, 'windowToBack', null, '窗口置后')
    },

    windowToForeground() {
      this.callApi(plugins.bizPlugin, 'windowToForeground', null, '窗口置前')
    },

    showAlertDialog() {
      this.callApi(plugins.bizPlugin, 'showAlertDialog', {
        title: "提示",
        message: "确认删除？",
        positiveText: "确认",
        negativeText: "取消"
      }, '显示对话框')
    },

    hideAlertDialog() {
      this.callApi(plugins.bizPlugin, 'hideAlertDialog', null, '隐藏对话框')
    },

    showToast() {
      this.callApi(plugins.bizPlugin, 'showToast', {
        message: "操作成功",
        duration: 3000
      }, '显示Toast')
    },

    showLoadingView() {
      this.callApi(plugins.bizPlugin, 'showLoadingView', {
        loadingText: "正在加载...",
        maxDuration: 5000
      }, '显示Loading')
    },

    hideLoadingView() {
      this.callApi(plugins.bizPlugin, 'hideLoadingView', null, '隐藏Loading')
    },

    saveCallCacheData() {
      this.callApi(plugins.bizPlugin, 'saveCallCacheData', {
        data: { key1: "value1", key2: "value2" }
      }, '保存通话缓存')
    },

    getCallCacheData() {
      this.callApi(plugins.bizPlugin, 'getCallCacheData', null, '获取通话缓存')
    },

    getRemoteNumber() {
      this.callApi(plugins.bizPlugin, 'getRemoteNumber', null, '获取对端号码')
    },

    playDtmfTone() {
      this.callApi(plugins.bizPlugin, 'playDtmfTone', {
        license: "your_license",
        digit: "1"
      }, '播放DTMF')
    },

    setSystemApiLicense() {
      this.callApi(plugins.bizPlugin, 'setSystemApiLicense', {
        license: "your_license",
        api: "getUserMedia"
      }, '设置系统API授权')
    },

    // ==================== 4. 屏幕共享与画板接口 ====================

    startScreenShare() {
      this.callApi(plugins.screenSharePlugin, 'startScreenShare', {
        license: "your_license"
      }, '开始屏幕共享')
    },

    stopScreenShare() {
      this.callApi(plugins.screenSharePlugin, 'stopScreenShare', null, '停止屏幕共享')
    },

    requestScreenShareAbility() {
      this.callApi(plugins.screenSharePlugin, 'requestScreenShareAbility', null, '请求共享能力')
    },

    openSketchBoard() {
      this.callApi(plugins.screenSharePlugin, 'openSketchBoard', {
        paintColor: "#ff0000ff",
        paintWidth: "5.0"
      }, '打开画板')
    },

    closeSketchBoard() {
      this.callApi(plugins.screenSharePlugin, 'closeSketchBoard', null, '关闭画板')
    },

    addDrawingInfo() {
      this.callApi(plugins.screenSharePlugin, 'addDrawingInfo', {
        drawingInfo: { x: 100, y: 200, type: "move" }
      }, '添加绘图信息')
    },

    // ==================== 5. 拓展能力接口 ====================

    expandingCapacityRegister() {
      this.callApi(plugins.ecPlugin, 'expandingCapacityRegister', {
        providerModules: [{ provider: "xxx", module: "yyy" }]
      }, '注册拓展能力')
    },

    expandingCapacityRequest() {
      this.callApi(plugins.ecPlugin, 'expandingCapacityRequest', {
        provider: "xxx",
        module: "yyy",
        func: "zzz",
        data: { key: "value" }
      }, '请求拓展能力')
    },

    expandingCapacityQuery() {
      this.callApi(plugins.ecPlugin, 'expandingCapacityQuery', null, '查询拓展能力')
    },

    // ==================== 1.4 HTTP代理 ====================

    async httpGet() {
      try {
        const url = 'https://local_333_0_dc1/api/test/get'
        this.addLog('request', '📤 Axios GET', { url })
        const response = await axios.get(url)
        this.addLog('response', '📥 Axios GET 响应', {
          status: response.status,
          statusText: response.statusText,
          data: response.data
        })
        return response
      } catch (err) {
        this.addLog('error', '❌ Axios GET 失败', err?.message || err)
        throw err
      }
    },

    async httpPost() {
      try {
        const url = 'https://local_333_0_dc1/api/test/post'
        const body = { username: 'testuser', password: '123456' }
        this.addLog('request', '📤 Axios POST', { url, body })
        const response = await axios.post(url, body)
        this.addLog('response', '📥 Axios POST 响应', {
          status: response.status,
          statusText: response.statusText,
          data: response.data
        })
        return response
      } catch (err) {
        this.addLog('error', '❌ Axios POST 失败', err?.message || err)
        throw err
      }
    },

    async httpPostFormData() {
      try {
        const url = 'https://local_333_0_dc1/api/test/post'
        const formData = new FormData()
        formData.append('username', 'testuser')
        formData.append('password', '123456')
        formData.append('timestamp', Date.now().toString())
        this.addLog('request', '📤 Axios POST FormData', { url })
        const response = await axios.post(url, formData, {
          headers: { 'Content-Type': 'multipart/form-data' }
        })
        this.addLog('response', '📥 Axios POST 响应', {
          status: response.status,
          statusText: response.statusText,
          data: response.data
        })
        return response
      } catch (err) {
        this.addLog('error', '❌ Axios POST 失败', err?.message || err)
        throw err
      }
    },

    async httpPut() {
      try {
        const url = 'https://local_333_0_dc1/api/test/put'
        const body = { name: 'updated', value: 'new' }
        this.addLog('request', '📤 Axios PUT', { url, body })
        const response = await axios.put(url, body)
        this.addLog('response', '📥 Axios PUT 响应', {
          status: response.status,
          statusText: response.statusText,
          data: response.data
        })
        return response
      } catch (err) {
        this.addLog('error', '❌ Axios PUT 失败', err?.message || err)
        throw err
      }
    },

    async httpDelete() {
      try {
        const url = 'https://local_333_0_dc1/api/test/delete'
        this.addLog('request', '📤 Axios DELETE', { url })
        const response = await axios.delete(url)
        this.addLog('response', '📥 Axios DELETE 响应', {
          status: response.status,
          statusText: response.statusText,
          data: response.data
        })
        return response
      } catch (err) {
        this.addLog('error', '❌ Axios DELETE 失败', err?.message || err)
        throw err
      }
    },

    async httpPatch() {
      try {
        const url = 'https://local_333_0_dc1/api/test/patch'
        const body = { field: 'patched' }
        this.addLog('request', '📤 Axios PATCH', { url, body })
        const response = await axios.patch(url, body)
        this.addLog('response', '📥 Axios PATCH 响应', {
          status: response.status,
          statusText: response.statusText,
          data: response.data
        })
        return response
      } catch (err) {
        this.addLog('error', '❌ Axios PATCH 失败', err?.message || err)
        throw err
      }
    },

    async httpHead() {
      try {
        const url = 'https://local_333_0_dc1/api/test/head'
        this.addLog('request', '📤 Axios HEAD', { url })
        const response = await axios.head(url)
        this.addLog('response', '📥 Axios HEAD 响应', {
          status: response.status,
          statusText: response.statusText,
          headers: response.headers
        })
        return response
      } catch (err) {
        this.addLog('error', '❌ Axios HEAD 失败', err?.message || err)
        throw err
      }
    }
  }
}
</script>

<style scoped>
.mini-app-container {
  display: flex;
  flex-direction: column;
  height: 100vh;
  background-color: #f5f5f5;
}
.header {
  background-color: #4CAF50;
  color: white;
  padding: 16px;
  text-align: center;
}
.content {
  flex: 1;
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 12px;
  overflow-y: auto;
}
.section {
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  padding: 12px;
}
.section h3 {
  margin: 0 0 8px 0;
  font-size: 14px;
  color: #333;
}
.section button {
  padding: 8px 16px;
  margin: 4px;
  font-size: 14px;
  border: none;
  border-radius: 6px;
  background-color: #4CAF50;
  color: white;
  cursor: pointer;
}
.section button:active {
  background-color: #45a049;
}
.log-area {
  flex: 1;
  overflow-y: auto;
  background-color: #333;
  color: #0f0;
  padding: 12px;
  border-radius: 8px;
  font-family: monospace;
  font-size: 12px;
  max-height: 300px;
}
.log-area p {
  margin: 2px 0;
}
</style>