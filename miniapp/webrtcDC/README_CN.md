## 说明
本项目实现GSMA TS.66《IMS data channel API specification CR1040》中的接口，旨在方便5G增强通话小程序开发者能够使用统一的API创建和使用DC数据通道，推动互联互通。
## 一、编译  
npm run build  
会在dist目录下生成webrtcDC.js文件。  

## 二、使用示例 
小程序开发时，本地引入编译好的webrtcDC.js库文件，API的调用与标准库中的webrtc一致
```angular2html
// 引入webrtcDC.js库文件
import 'xxx/xxx/webrtcDC.js'

export default {
  data(){
    return {
      appId :"333",
      dcId :"test",
      dcLabel :"local_333_1_test",
      imsDCStatus:-1,
      pc:null,
      dataChannel:null,
    }
  },
  mounted() {
    // 创建peer connection
    this.pc = new RTCPeerConnection();
    const that = this
    // 监听来自对端创建的数据通道
    this.pc.ondatachannel = (event) => {
      that.dataChannel = event.channel;
      that.initDataChannelEvent()
    }
  },
  methods: {
    initDataChannelEvent() {
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
    createAppDataChannel() {
      // 创建application data channel
      ElMessage.info("创建数据通道")
      const xml = `<DataChannelAppInfo><DataChannelApp appId=\\"${this.appId}\\"><DataChannel dcId=\\"${this.dcId}\\"><DcLabel>${this.dcLabel}<\\/DcLabel><Subprotocol><\\/Subprotocol><Ordered>1<\\/Ordered><MaxRetr>0<\\/MaxRetr><MaxTime>0<\\/MaxTime><Priority><\\/Priority><UseCase>1<\\/UseCase><AutoAcceptDcSetup>0<\\/AutoAcceptDcSetup><Bandwidth>500<\\/Bandwidth><QosHint>loss=0.0002;latency=600<\\/QosHint><\\/DataChannel><\\/DataChannelApp><\\/DataChannelAppInfo>`
      this.dataChannel = this.pc.createDataChannel(xml,null)
      this.initDataChannelEvent()
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
    messageNotify(byteArray) {
      // 根据业务处理byteArray
      // 1.可转成字符串，获取json等
      const stringFromByteArray = new TextDecoder().decode(byteArray);
      ElMessage.success(`收到DC数据字符串：${stringFromByteArray}`)
      // 2.也可按文件处理，保存文件等
    },
    str2ByteArray(str) {
      return new TextEncoder().encode(str)
    },
  }
}
```

## 三、运行环境
webrtcDC.js的webrtc接口是基于中国电信5G增强通话SDK提供的js api实现的，因此，使用本库开发的小程序需要在有5G增强通话SDK的环境下运行。