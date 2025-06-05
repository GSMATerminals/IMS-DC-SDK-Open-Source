<template>
  <div>
    <div class="my-text">数据通道操作：</div>
    <button class="my-button" v-on:click="createAppDataChannel">创建application data channel</button>
    <br>
    <br>
    <button class="my-button" v-on:click="closeAppDataChannel">关闭application data channel</button>
    <br>
    <br>
    <button class="my-button" v-on:click="getBufferedAmount">查询bufferedAmount</button>
    <br>
    <br>
    <button class="my-button" v-on:click="sendData(str2ByteArray(`Hello World`))">发送数据</button>
    <br>
    <br>
    <div class="my-text">文件操作：</div>
    <button class="my-button" v-on:click="getLocation">获取当前位置</button>
    <br>
    <br>
    <button class="my-button" v-on:click="selectAndCopyFile">选择并复制文件到私有目录</button>
    <br>
    <br>
    <button class="my-button" v-on:click="selectAndDeleteFile">选择并删除私有目录文件</button>
    <br>
    <br>
    <button class="my-button" v-on:click="getFileList">获取文件列表</button>
    <br>
    <br>
    <button class="my-button" v-on:click="getPrivateFolder('inner')">获取私有目录</button>
    <br>
    <br>
    <button class="my-button" v-on:click="selectAndDecompressFile">选择并解压文件到私有目录(请用私有目录文件测试)</button>
    <br>
    <br>
    <button class="my-button" v-on:click="checkFileOrFolderExists">判断文件/文件夹是否存在</button>
    <br>
    <br>
    <button class="my-button" v-on:click="captureImage()">调用摄像头获取视频流</button>
    <br>
    <br>
<!--    <button class="my-button" v-on:click="stopCaptureImage()">关闭摄像头</button>-->
<!--    <br>-->
<!--    <br>-->
    <video id="video" width="200" height="200" autoplay style="object-fit:cover;background-color: #181818"></video>
    <br>
    <br>
    <button class="my-button" v-on:click="openCamera('picture')">拍照</button>
    <br>
    <br>
    <button class="my-button" v-on:click="openCamera('video')">录像</button>
    <br>
    <br>
    <button class="my-button" v-on:click="playVoice(`${this.appPath}/1.mp3`)">播放mp3</button>
    <br>
    <br>
    <button class="my-button" v-on:click="stopPlayVoice()">停止播放mp3</button>
    <br>
    <br>
    <div class="my-text">小程序操作：</div>
    <button class="my-button" v-on:click="getMiniAppInfo">获取小程序信息</button>
    <br>
    <br>
    <button class="my-button" v-on:click="setWindow(false,false,'#0000FF',1,'#FF0000','测试小程序')">非全屏</button>
    <br>
    <br>
    <button class="my-button" v-on:click="setWindow(false,true)">全屏</button>
    <br>
    <br>
    <button class="my-button" v-on:click="hangUp">挂断电话</button>
    <br>
    <br>
    <button class="my-button" v-on:click="getCallState">获取当前通话状态</button>
    <br>
    <br>
    <button class="my-button" v-on:click="getRemoteNumber">获取对端号码</button>
    <br>
    <br>
    <button class="my-button" v-on:click="getHttpResult">调用http接口</button>
    <br>
    <br>
    <button class="my-button" v-on:click="requestStartAdverseApp">拉起对端小程序</button>
    <br>
    <br>
    <button class="my-button" v-on:click="stopApp">关闭小程序</button>
    <br>
    <br>
    <button class="my-button" v-on:click="openGaoDeMap">打开高德地图</button>
    <br>
    <br>
    <button class="my-button" v-on:click="openBaiduMap">打开百度地图</button>
    <br>
    <br>
    <button class="my-button" v-on:click="openBrowser">打开浏览器</button>
    <br>
    <br>
    <button class="my-button" v-on:click="setSpeakerphone(true)">开启免提</button>
    <br>
    <br>
    <button class="my-button" v-on:click="setSpeakerphone(false)">关闭免提</button>
    <br>
    <br>
    <button class="my-button" v-on:click="isSpeakerphoneOn">免提是否开启</button>
    <br>
    <br>
    <button class="my-button" v-on:click="setMuted(true)">开启静音</button>
    <br>
    <br>
    <button class="my-button" v-on:click="setMuted(false)">关闭静音</button>
    <br>
    <br>
    <button class="my-button" v-on:click="isMuted">静音是否开启</button>
    <br>
    <br>
    <button class="my-button" v-on:click="playDtmfTone('1')">播放DTMF</button>
    <br>
    <br>
    <div class="my-text">浮窗操作：</div>
    <button class="my-button" v-on:click="addViews()">浮窗显示html</button>
    <br>
    <br>
    <button class="my-button" v-on:click="removeViews()">删除浮窗</button>
    <br>
    <div class="my-text">联系人操作：</div>
    <button class="my-button" v-on:click="addContact">添加联系人</button>
    <br>
    <br>
    <button class="my-button" v-on:click="editContact">编辑联系人</button>
    <br>
    <br>
    <button class="my-button" v-on:click="getContactName">获取联系人姓名</button>
    <br>
    <br>
    <button class="my-button" v-on:click="getContactList">获取联系人列表</button>
    <br>
    <br>
    <div class="my-text">拓展能力：</div>
    <button class="my-button" v-on:click="requestECAIDetect()">AI视频检测</button>
    <br>
    <br>
    <button class="my-button" v-on:click="requestECTranslateLanguageList">实时翻译语言列表</button>
    <br>
    <br>
    <button class="my-button" v-on:click="requestECStartTranslate()">开始实时翻译</button>
    <br>
    <br>
    <button class="my-button" v-on:click="requestECStopTranslate">停止实时翻译</button>
    <br>
    <br>
    <button class="my-button" v-on:click="requestECTranslateVoice">切换实时翻译音频类型</button>
    <br>
    <br>
    <br>
  </div>
</template>

<script>
import {ElMessage, ElMessageBox} from 'element-plus';
import 'element-plus/dist/index.css'
// 导入webrtcDC.js库文件，以支持通过Ts.66定义的接口使用DC数据通道。webrtcDC.js库的实现源码见miniapp/webrtcDC
import '../../local_modules/webrtc/webrtcDC.js'

export default {
  data(){
    return {
      appId :"333",
      dcId :"test",
      dcLabel :"local_333_1_test",
      imsDCStatus:-1,
      role:-1,// -1初始状态；0发起方；1接收方
      voiceType:1,
      appPath:"",
      pc:null,
      dataChannel:null,
    }
  },
  mounted() {
    this.pc = new RTCPeerConnection();
    const that = this
    this.pc.ondatachannel = (event) => {
      that.dataChannel = event.channel;
      that.initDataChannel()
    }
    //注册回调函数
    window.dsBridge.register("callStateNotify",this.callStateNotify)
    window.dsBridge.register("miniAppStateNotify",this.miniAppStateNotify)
    window.dsBridge.register("expandingCapacityNotify",this.expandingCapacityNotify)
    window.dsBridge.register("startAdverseAppResponseNotify",this.startAdverseAppResponseNotify)
    // 注册电信的浮窗能力
    this.registerEC("CT","FloatView")
  },
  methods: {
    initDataChannel() {
      // 监听数据通道事件
      this.dataChannel.onopen = () => {
        console.log("数据通道已打开");
        this.imsDCStatus = 1
      };
      this.dataChannel.onclose = () => {
        console.log("数据通道已关闭");
        this.imsDCStatus = -1
      };
      this.dataChannel.onmessage = (event) => {
        console.log("收到消息:", event.data);
        this.messageNotify(event.data)
      };
      this.dataChannel.onerror = (error) => {
        console.error("数据通道错误:", error);
      };
    },
    captureImage() {
      // 设置license
      this.setSystemApiLicense("ktL0q+IvjbkYe94Lk+rzNBy8+eRDdcFj6M9zXGjCzNw42Yf8Toscwc6b7Yu3g+iLc8iVXbKrPA2jPp2JEKjopwQ==")
      if (navigator.mediaDevices && navigator.mediaDevices.getUserMedia) {
        navigator.mediaDevices.getUserMedia({ video: true })
            .then(function(stream) {
              const video = document.getElementById('video');
              video.style.transform = 'scaleX(-1)';
              video.srcObject = stream;
              video.play();
            })
            .catch(function(error) {
              ElMessage.error('Error accessing media devices: ', error);
            });
      } else {
        ElMessage.error('getUserMedia is not supported');
      }
    },
    // stopCaptureImage() {
    //   if (navigator.mediaDevices && navigator.mediaDevices.getUserMedia) {
    //     navigator.mediaDevices.getUserMedia({ video: true })
    //         .then(function(stream) {
    //           stream.getTracks().forEach(track =>{
    //             track.stop();
    //           })
    //         })
    //         .catch(function(error) {
    //           ElMessage.error('Error accessing media devices: ', error);
    //         });
    //   } else {
    //     ElMessage.error('getUserMedia is not supported');
    //   }
    // },
    createAppDataChannel() {
      // 创建application data channel
      ElMessage.info("创建数据通道")
      const xml = `<DataChannelAppInfo><DataChannelApp appId=\\"${this.appId}\\"><DataChannel dcId=\\"${this.dcId}\\"><DcLabel>${this.dcLabel}<\\/DcLabel><Subprotocol><\\/Subprotocol><Ordered>1<\\/Ordered><MaxRetr>0<\\/MaxRetr><MaxTime>0<\\/MaxTime><Priority><\\/Priority><UseCase>1<\\/UseCase><AutoAcceptDcSetup>0<\\/AutoAcceptDcSetup><Bandwidth>500<\\/Bandwidth><QosHint>loss=0.0002;latency=600<\\/QosHint><\\/DataChannel><\\/DataChannelApp><\\/DataChannelAppInfo>`
      this.dataChannel = this.pc.createDataChannel(xml,null)
      this.initDataChannel()
    },
    closeAppDataChannel() {
      // 关闭application data channel
      ElMessage.info("关闭数据通道")
      this.dataChannel.close()
    },
    sendData(byteArray) {
      // 发送数据
      ElMessage.info(`数据通道状态：${this.imsDCStatus}`)
      if (this.imsDCStatus === 1){
        this.dataChannel.send(byteArray)
      } else {
        ElMessage.error(`dc通道未建立`)
      }
    },
    getBufferedAmount() {
      // 查询bufferedAmount
      const s = this.dataChannel.getBufferedAmount();
      ElMessage.success(`JS收到SDK响应：${s}`)
    },
    getLocation() {
      // 获取当前位置
      window.dsBridge.call("async",`{"event":"FileEvent","function":"getLocation","params":{}}`,function(v) {
        ElMessage.success(`getLocation JS收到SDK响应：${v}`)
      })
    },
    selectFile(f) {
      // 选择文件
      window.dsBridge.call("async",`{"event":"FileEvent","function":"selectFile","params":{}}`,f)
    },
    getFileList() {
      // 获取指定文件类型列表
      const s = window.dsBridge.call(
          "sync",
          `{"event":"FileEvent","function":"getFileList","params":{"folderPath":"/sdcard","offset":"0","count":"20","fileType":"0","sortType":"sizeSort","sortOrder":"1","folderPosition":"1","suffix":""}}`
      );
      ElMessage.success(`getFileList JS收到SDK响应：${s}`)
    },
    getPrivateFolder(type) {
      // 删除文件
      const s = window.dsBridge.call("sync", `{"event":"FileEvent","function":"getPrivateFolder","params":{"type":"${type}"}}`)
      ElMessage.success(`getPrivateFolder JS收到SDK响应：`+s)
      return s
    },
    startSaveFile(path) {
      // 开始保存文件
      const s = window.dsBridge.call("sync", `{"event":"FileEvent","function":"startSaveFile","params":{"path":"${path}","append":true}}`);
      ElMessage.success(`startSaveFile JS收到SDK响应：${s}`)
    },
    saveFile(byteArray,f) {
      // 保存文件
      const base64String = btoa(String.fromCharCode.apply(null, byteArray));// byteArray进行base64编码
      window.dsBridge.call("async", `{"event":"FileEvent","function":"saveFile","params":{"data":"${base64String}"}}`,f);
    },
    stopSaveFile() {
      // 结束保存文件
      const s = window.dsBridge.call("sync", `{"event":"FileEvent","function":"stopSaveFile","params":{}}`);
      ElMessage.success(`stopSaveFile JS收到SDK响应：${s}`)
    },
    deleteFile(path) {
      // 删除文件
      const s = window.dsBridge.call("sync", `{"event":"FileEvent","function":"deleteFile","params":{"path":"${path}"}}`);
      ElMessage.success(`deleteFile JS收到SDK响应：${s}`)
    },
    startReadFile(path) {
      // 开始读取文件
      const s = window.dsBridge.call("sync", `{"event":"FileEvent","function":"startReadFile","params":{"path":"${path}"}}`);
      ElMessage.success(`startReadFile JS收到SDK响应：${s}`)
    },
    readFile(length,f) {
      // 读取文件
      window.dsBridge.call("async", `{"event":"FileEvent","function":"readFile","params":{"length":"${length}"}}`,f);
    },
    stopReadFile() {
      // 结束读取文件
      const s = window.dsBridge.call("sync", `{"event":"FileEvent","function":"stopReadFile","params":{}}`);
      ElMessage.success(`stopReadFile JS收到SDK响应：${s}`)
    },
    decompressFile(srcPath,desPath,f) {
      // 解压文件
      window.dsBridge.call("async", `{"event":"FileEvent","function":"decompressFile","params":{"srcPath":"${srcPath}","desPath":"${desPath}","compressType":"zip"}}`,f);
    },
    checkFileOrFolderExists() {
      // 判断文件/文件夹是否存在
      const s = window.dsBridge.call("sync", `{"event":"FileEvent","function":"checkFileOrFolderExists","params":{"path":["/sdcard/haha","/sdcard/7.png"]}}`);
      ElMessage.success(`checkFileOrFolderExists JS收到SDK响应：${s}`)
    },
    getFileInfo(path) {
      // 判断文件/文件夹是否存在
      window.dsBridge.call("async", `{"event":"FileEvent","function":"getFileInfoAsync","params":{"path":"${path}"}}`,function(v) {
        ElMessage.success(`getFileInfo JS收到SDK响应：${v}`)
      });
    },
    hangUp() {
      // 挂断电话
      const s = window.dsBridge.call("sync", `{"event":"MiniAppEvent","function":"hangUp","params":{}}`);
      ElMessage.success(`hangUp JS收到SDK响应：${s}`)
    },
    getCallState() {
      // 获取通话状态
      const s = window.dsBridge.call("sync", `{"event":"MiniAppEvent","function":"getCallState","params":{}}`);
      ElMessage.success(`getCallState JS收到SDK响应：${s}`)
    },
    getRemoteNumber() {
      // 获取对端号码
      window.dsBridge.call("async", `{"event":"MiniAppEvent","function":"getRemoteNumber","params":{}}`,function(v) {
        ElMessage.success(`getRemoteNumber JS收到SDK响应：${v}`)
      });
    },
    requestStartAdverseApp() {
      // 拉起对端小程序
      const s = window.dsBridge.call("sync", `{"event":"MiniAppEvent","function":"requestStartAdverseApp","params":{}}`);
      ElMessage.success(`requestStartAdverseApp JS收到SDK响应：${s}`)
    },
    setSystemApiLicense(l) {
      // 设置系统api license
      const s = window.dsBridge.call("sync", `{"event":"MiniAppEvent","function":"setSystemApiLicense","params":{"license":"${l}","api":"getUserMedia"}}`);
      ElMessage.success(`setSystemApiLicense JS收到SDK响应：${s}`)
    },
    getMiniAppInfo() {
      const that = this;
      // 获取小程序信息
      window.dsBridge.call("async", `{"event":"MiniAppEvent","function":"getMiniAppInfo","params":{}}`,function(v) {
        ElMessage.success(`getMiniAppInfo JS收到SDK响应：${v}`)
        that.appPath = JSON.parse(v).data.path
      });
    },
    startApp(appType,extra,f) {
      // 打开app组件
      window.dsBridge.call("async",`{"event":"MiniAppEvent","function":"startApp","params":{"appType":"${appType}","extra":"${extra}"}}`,f)
    },
    stopApp() {
      // 关闭小程序
      window.dsBridge.call("sync",`{"event":"MiniAppEvent","function":"stopApp","params":{}}`)
    },
    getHttpResult() {
      // 打开app组件
      const paramsJson = `{"name":"test","password":"test"}`
      const base64Params = btoa(String.fromCharCode.apply(null, this.str2ByteArray(paramsJson)))
      const headerJson = `{"head1":"test","head2":"test"}`
      const base64Header = btoa(String.fromCharCode.apply(null, this.str2ByteArray(headerJson)))
      window.dsBridge.call("async",`{"event":"MiniAppEvent","function":"getHttpResult","params":{"url":"https://www.baidu.com","httpWay":"POST","mediaType":"text/pain","header":"${base64Header}","paramsJson":"${base64Params}"}}`, function(v) {
        const data = atob(JSON.parse(v).data.response)
        ElMessage.success(`getHttpResult JS收到SDK响应：${data}`)
      })
    },
    addContact() {
      const s = window.dsBridge.call("sync", `{"event":"MiniAppEvent","function":"addContact","params":{"contactMode":"0", "number":"19129952167"}}`);
      ElMessage.success(`addContact JS收到SDK响应：${s}`)

    },
    editContact() {
      const s = window.dsBridge.call("sync", `{"event":"MiniAppEvent","function":"addContact","params":{"contactMode":"1", "number":"19129952157"}}`);
      ElMessage.success(`editContact JS收到SDK响应：${s}`)
    },
    getContactName() {
      // 需配置MINIAPP_READ_CONTACTS权限
      const s = window.dsBridge.call("sync", `{"event":"MiniAppEvent","function":"getContactName","params":{"contactNumber":"19120050940"}}`);
      ElMessage.success(`getContactName JS收到SDK响应：${s}`)
    },
    getContactList() {
      // 需配置MINIAPP_READ_CONTACTS权限
      window.dsBridge.call(
          "async",
          `{"event":"MiniAppEvent","function":"getContactList","params":{"offset":"4","limit":"4"}}`,
          function(v) {
            ElMessage.success(`getContactList JS收到SDK响应：${v}`)
          }
      );
    },
    setWindow(hidden,isFullScreen,statusBarColor,statusBarTitleColor,navigationBarColor,pageName) {
      // 设置SDK窗口属性
      window.dsBridge.call(
          "async",
          `{"event":"MiniAppEvent","function":"setWindow","params":{"hidden":${hidden},"isFullScreen":${isFullScreen},"statusBarColor": "${statusBarColor}","statusBarTitleColor": ${statusBarTitleColor},"navigationBarColor": "${navigationBarColor}","pageName": "${pageName}"}}`,
          function(v) {
            ElMessage.success(`setWindow JS收到SDK响应：${v}`)
          }
      );

    },
    stopScreenShare(){
      // 停止屏幕共享
      const s = window.dsBridge.call("sync", `{"event":"ScreenShareEvent","function":"stopScreenShare","params":{}}`);
      ElMessage.success(`stopScreenShare JS收到SDK响应：${s}`)
    },
    openSketchBoard(){
      // 开启画板
      const s = window.dsBridge.call("sync", `{"event":"ScreenShareEvent","function":"openSketchBoard","params":{}}`);
      ElMessage.success(`openSketchBoard JS收到SDK响应：${s}`)
    },
    closeSketchBoard(){
      // 关闭画板
      const s = window.dsBridge.call("sync", `{"event":"ScreenShareEvent","function":"closeSketchBoard","params":{}}`);
      ElMessage.success(`closeSketchBoard JS收到SDK响应：${s}`)
    },
    addDrawingInfo(data,role){
      // 绘制数据到画板
      const s = window.dsBridge.call("sync", `{"event":"ScreenShareEvent","function":"addDrawingInfo","params":{"drawingInfo":"${data}","role":${role}}}`);
      ElMessage.success(`addDrawingInfo JS收到SDK响应：${s}`)
    },
    requestECAIDetect() {
      // 调用拓展能力--AI视频检测
      const s = window.dsBridge.call("sync",`{"event":"ECEvent","function":"expandingCapacityRequest","params":{"provider":"OEM","module":"AIVideo","func":"detect","data":{}}}`)
      ElMessage.success(`调用AI视频检测：${s}`)
    },
    requestECStartTranslate() {
      // 调用拓展能力--开始实时翻译
      const s = window.dsBridge.call("sync",`{"event":"ECEvent","function":"expandingCapacityRequest","params":{"provider":"OEM","module":"Translate","func":"start","data":{"meTo":"English","otherTo":"Chinese"}}}`)
      ElMessage.success(`开始实时翻译：${s}`)
    },
    requestECStopTranslate() {
      // 调用拓展能力--停止实时翻译
      const s = window.dsBridge.call("sync",`{"event":"ECEvent","function":"expandingCapacityRequest","params":{"provider":"OEM","module":"Translate","func":"stop","data":{}}}`)
      ElMessage.success(`停止实时翻译：${s}`)
    },
    requestECTranslateLanguageList() {
      // 调用拓展能力--获取实时翻译可选语种
      const s = window.dsBridge.call("sync",`{"event":"ECEvent","function":"expandingCapacityRequest","params":{"provider":"OEM","module":"Translate","func":"languageList","data":{}}}`)
      ElMessage.success(`获取实时翻译可选语种：${s}`)
    },
    requestECTranslateVoice() {
      // 调用拓展能力--设置实时翻译音频通道声音类型
      const s = window.dsBridge.call("sync",`{"event":"ECEvent","function":"expandingCapacityRequest","params":{"provider":"OEM","module":"Translate","func":"voice","data":{"type":"${this.voiceType}"}}}`)
      ElMessage.success(`设置实时翻译音频通道声音类型：${s}`)
      this.voiceType = this.voiceType%2 +1
    },
    readAndSave(){
      const that = this;
      that.readFile(100000, function (readFileResponseJson){
        const readFileResponse = JSON.parse(readFileResponseJson)
        const readFileResponseCode = readFileResponse.code
        if (readFileResponseCode!=="0"){
          return
        }
        const isEnd = readFileResponse.data.isEnd
        if (isEnd){
          // 停止读取文件
          that.stopReadFile()
          // 停止写入文件
          that.stopSaveFile()
          return
        }
        const base64Data = readFileResponse.data.base64Data// 获取message字段
        const byteCharacters = atob(base64Data);// base64解码
        const byteArray = new Uint8Array(byteCharacters.length);
        // 解码后转回byteArray
        for (let i = 0; i < byteCharacters.length; i++) {
          byteArray[i] = byteCharacters.charCodeAt(i)
        }
        that.saveFile(byteArray,function (saveFileResponseJson) {
          const saveFileResponse = JSON.parse(saveFileResponseJson)
          const saveFileResponseCode = saveFileResponse.code
          if (saveFileResponseCode==="0"){
            that.readAndSave()
          }
        })
      })

    },
    selectAndCopyFile() {
      const that = this;
      // 选择文件
      this.selectFile(function(v) {
        ElMessage.success(`selectAndCopyFile JS收到SDK响应：${v}`)
        const response = JSON.parse(v)
        const path = response.data.path
        const getPrivateFolderResponseJson = that.getPrivateFolder('outer')
        const getPrivateFolderResponse = JSON.parse(getPrivateFolderResponseJson)
        const privateFolder = getPrivateFolderResponse.data.privateFolder
        const newPath = privateFolder+"copy-"+response.data.name
        // 开始读取文件
        that.startReadFile(path)
        // 开始写入文件
        that.startSaveFile(newPath)

        // 读取并写入
        that.readAndSave()
      })
    },
    selectAndDeleteFile() {
      const that = this;
      // 选择文件
      this.selectFile(function(v) {
        ElMessage.success(`selectAndDeleteFile JS收到SDK响应：${v}`)
        const response = JSON.parse(v)
        const path = response.data.path
        // 删除文件
        that.deleteFile(path)
      })
    },
    selectAndDecompressFile() {
      const that = this;
      // 选择文件
      this.selectFile(function(v) {
        ElMessage.success(`selectFile JS收到SDK响应：${v}`)
        const response = JSON.parse(v)
        const path = response.data.path
        const getPrivateFolderResponseJson = that.getPrivateFolder('outer')
        const getPrivateFolderResponse = JSON.parse(getPrivateFolderResponseJson)
        const privateFolder = getPrivateFolderResponse.data.privateFolder
        const name = response.data.name.replace(".zip","")
        // 解压缩
        const newPath = privateFolder+name
        that.decompressFile(path,newPath,function(v) {
          ElMessage.success(`decompressFile JS收到SDK响应：${v}`)
        })
      })
    },

    openCamera(extra) {
      const that = this;
      // 打开系统相机
      this.startApp("camera",extra,function(v) {
        ElMessage.success(`startApp JS收到SDK响应：${v}`)
        const response = JSON.parse(v)
        const path = response.data
        that.getFileInfo(path)
      })
    },
    playVoice(path) {
      const s = window.dsBridge.call("sync", `{"event":"FileEvent","function":"playVoice","params":{"path":"${path}"}}`);
      ElMessage.success(`playVoice JS收到SDK响应：${s}`)
    },
    stopPlayVoice() {
      const s = window.dsBridge.call("sync", `{"event":"FileEvent","function":"stopPlayVoice","params":{}}`);
      ElMessage.success(`stopPlayVoice JS收到SDK响应：${s}`)
    },
    openGaoDeMap(){
      // 高德坐标（GCJ20）
      const extra = "type=1&lat=23.135146&lng=113.358444&title=广东电信科技大厦"
      this.startApp("map",extra,function(v){
        ElMessage.success(`startApp JS收到SDK响应：${v}`)
      })
    },
    openBaiduMap(){
      // 百度坐标（BD09）
      const extra = "type=2&lat=23.140949&lng=113.364965&title=广东电信科技大厦"
      this.startApp("map",extra,function(v){
        ElMessage.success(`startApp JS收到SDK响应：${v}`)
      })
    },
    openBrowser(){
      this.startApp("browser","https://www.baidu.com",function(v){
        ElMessage.success(`startApp JS收到SDK响应：${v}`)
      })
    },
    setSpeakerphone(param){
      const s = window.dsBridge.call("sync", `{"event":"MiniAppEvent","function":"setSpeakerphone","params":{"speakerphoneOn":${param}}}`);
      ElMessage.success(`setSpeakerphone JS收到SDK响应：${s}`)
    },
    isSpeakerphoneOn(){
      window.dsBridge.call("async", `{"event":"MiniAppEvent","function":"isSpeakerphoneOn","params":{}}`,function(v) {
        ElMessage.success(`isSpeakerphoneOn JS收到SDK响应：${v}`)
      });
    },
    setMuted(param){
      const s = window.dsBridge.call("sync", `{"event":"MiniAppEvent","function":"setMuted","params":{"muted":${param}}}`);
      ElMessage.success(`setMuted JS收到SDK响应：${s}`)
    },
    isMuted(){
      window.dsBridge.call("async", `{"event":"MiniAppEvent","function":"isMuted","params":{}}`,function(v) {
        ElMessage.success(`isMuted JS收到SDK响应：${v}`)
      });
    },
    playDtmfTone(param){
      const license = "k/rINulXm/H7Yc3IgOQeZzmdC49hhXtmQ2/24lIKSmHsRytJkPvMQCGQOUEjbp1Ity1gm0Mmh7ojZg9EKEoum4w=="
      const s = window.dsBridge.call("sync", `{"event":"MiniAppEvent","function":"playDtmfTone","params":{"digit":"${param}","license":"${license}"}}`);
      ElMessage.success(`setMuted JS收到SDK响应：${s}`)
    },

    // 添加html和icon示例
    addViews(){
      // 将小程序退至后台
      this.setWindow(true)
      // 添加一个随通话界面消失和出现的html
      this.addHtmlView('1',false,20,400,1000,400)
      // 添加一个不随通话界面消失和出现的html
      this.addHtmlView('2',true,20,900,1000,400)
      // 在通话界面上添加一个icon
      this.addIconView('3',800,400,200,200)
    },

    // 删除html和icon示例
    removeViews(){
      this.removeView('1')
      this.removeView('2')
      this.removeView('3')
    },

    addHtmlView(id,showOutCallPage,left,top,width,height){
      const that = this
      const bs = this.getHtmlBase64("添加了一个文本html",height)
      // 需要license验证,向研究院提供小程序id申请license
      const license = "ciQzVYVwwrSZxZICFgM3f04kxsqq/AlHxtuIFqMnN9EamEWFEr3Dx2bCGgbiAAqAFdbACh/hNeDP88hqCt3Pt9C5/AsJsMIHie5K5O0z8Gs0="
      const s = window.dsBridge.call("sync",`{"event":"ECEvent","function":"expandingCapacityRequest","params":{"provider":"CT","module":"FloatView","func":"addHtmlView","data":{"license":"${license}","viewId":"${id}","htmlBase64":"${bs}","showOutCallPage":${showOutCallPage},"left":"${left}","top":"${top}","width":"${width}","height":"${height}"}}}`)
      ElMessage.success(`addHtmlView JS收到SDK响应：${s}`)
      // 每秒更新更新一次浮窗内容
      setInterval(function() {
        const timestamp = new Date().getTime();
        that.updateHtmlView('2',`小程序在后台更新浮窗内容: ${timestamp}`,height)
      }, 1000);
    },
    updateHtmlView(id,content,height){
      const bs = this.getHtmlBase64(`${content}`,height)
      const s = window.dsBridge.call("sync",`{"event":"ECEvent","function":"expandingCapacityRequest","params":{"provider":"CT","module":"FloatView","func":"updateHtmlView","data":{"viewId":"${id}","htmlBase64":"${bs}"}}}`)
      ElMessage.success(`updateHtmlView JS收到SDK响应：${s}`)
    },

    getHtmlBase64(content,height){
      const htmlContent = `
          <!DOCTYPE html>
          <html lang="en">
          <head>
              <meta charset="UTF-8">
              <meta name="viewport" content="width=device-width, initial-scale=1.0">
              <title></title>
              <style>
                  html, body {
                      width: 100%;
                      height: 100%;
                  }
                  .custom-style {
                      border-radius: 10px;
                      width: 100%;
                      height: 100%;
                      background-color: #f3d19edd;
                      color: black;
                      font-size: 16px;
                      display: flex;
                      align-items: center;
                  }
              </style>
          </head>
          <body>
              <div class="custom-style">
                  ${content}
              </div>
          </body>
          </html>
        `;

      const byteArray = this.str2ByteArray(htmlContent)
      // byteArray进行base64编码
      return btoa(String.fromCharCode.apply(null, byteArray))
    },
    addIconView(id,left,top,width,height){
      // 需要license验证,向研究院提供小程序id申请license
      const license = "sM0wDOXR+5iY2eaz0bsZ+rc1+iVlMVBhxOfHtYco6ehLZFT1RoYvFuSI+Oj5Ceih/BnJs2iLH9CSorjna7OcCwoLfhJy58LVtp0o6GgiEyyU="
      // icon的base64数据
      const bs  = "data:image/png;base64,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"
      const s = window.dsBridge.call("sync",`{"event":"ECEvent","function":"expandingCapacityRequest","params":{"provider":"CT","module":"FloatView","func":"addIconView","data":{"license":"${license}","viewId":"${id}","iconBase64":"${bs}","left":"${left}","top":"${top}","width":"${width}","height":"${height}"}}}`)
      ElMessage.success(`addIconView JS收到SDK响应：${s}`)
    },
    removeView(id){
      const s = window.dsBridge.call("sync",`{"event":"ECEvent","function":"expandingCapacityRequest","params":{"provider":"CT","module":"FloatView","func":"removeView","data":{"viewId":"${id}"}}}`)
      ElMessage.success(`removeView JS收到SDK响应：${s}`)
    },

    //下面是js函数给Android调用
    callStateNotify(msg) {
      // 呼叫状态信息
      ElMessage.success(`callStateNotify JS收到SDK 呼叫状态信息：${msg}`)
    },
    miniAppStateNotify(msg) {
      // 小程序生命周期信息
      ElMessage.success(`miniAppStateNotify JS收到SDK 小程序状态：${msg}`)
    },
    expandingCapacityNotify(msg){
      console.log(`expandingCapacityNotify JS收到SDK 拓展能力数据：${msg}`)
      const response = JSON.parse(msg)
    },
    startAdverseAppResponseNotify(msg){
      console.log(`startAdverseAppResponseNotify JS收到SDK：${msg}`)
      const response = JSON.parse(msg)
    },

    // 内部函数
    messageNotify(byteArray) {
      // 根据业务处理byteArray
      // 1.可转成字符串，获取json等
      const stringFromByteArray = new TextDecoder().decode(byteArray);
      ElMessage.success(`收到DC数据字符串：${stringFromByteArray}`)
      // 2.也可按文件处理，保存文件等
      // this.saveFile("/sdcard/Download/save.zip",byteArray)
    },
    str2ByteArray(str) {
       return new TextEncoder().encode(str)
    },
  }
}
</script>
<style>
.my-button {
  border: 2px solid #434346;
  border-radius: 5px;
  width: 200px;
  height: 40px;
}
.my-text {
  color: #434346;
}
/* 在全局样式文件中添加 */
.el-message {
  white-space: pre-line; /* 允许换行 */
  max-width: 80%; /* 限制最大宽度 */
  word-break: break-all; /* 长单词换行 */
}
</style>