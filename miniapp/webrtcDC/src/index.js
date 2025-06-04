import mDsBridge from 'dsbridge'

class RTCDataChannel {
  constructor() {
    this.bufferedAmount = 0
    this.label = null
    this.onclose = null
    this.onclosing = null
    this.onerror = null
    this.onmessage = null
    this.onopen = null
    this.readyState = null
  }
  close() {
    mDsBridge.call("async",`{"event":"DcEvent","function":"closeAppDataChannel","params":{"dcLabel":["${this.label}"]}}`,function(v) {
      console.info(`closeAppDataChannel响应：`+v)
    })
  }
  send(arrayBuffer){
    const base64String = btoa(String.fromCharCode.apply(null, new Uint8Array(arrayBuffer)));// byteArray进行base64编码
    // 发送数据
    mDsBridge.call(
        "async",
        `{"event":"DcEvent","function":"sendData","params":{"dcLabel":"${this.label}","data":"${base64String}"}}`,
        function(v) {
          console.info(`sendData响应：`+v)
        }
    )
  }
  getBufferedAmount() {
    // 查询bufferedAmount
    const s = mDsBridge.call("sync", `{"event":"DcEvent","function":"getBufferedAmount","params":{"dcLabel":"${this.label}"}}`);
    console.info(`getBufferedAmount响应：`+s)
    const response = JSON.parse(s)
    this.bufferedAmount = response.data.bufferedAmount
    return this.bufferedAmount;
  }
  channelDataChannelNotify(imsDCStatus) {
    // DC状态信息
    switch (imsDCStatus) {
      case 0:
        this.readyState = "connecting"
        break
      case 1:
        this.readyState = "open"
        if (this.onopen !== null){
          this.onopen(this,new Event("open"))
        }
        break
      case 2:
        this.readyState = "closing"
        if (this.onclosing !== null){
          this.onclosing(this,new Event("closing"))
        }
        break
      case 3:
        this.readyState = "closed"
        if (this.onclose !== null){
          this.onclose(this,new Event("close"))
        }
        break
    }
  }
  channelMessageNotify(msg){
    console.info("channelMessageNotify 未解析之前："+msg)
    const byteCharacters = atob(msg);// base64解码
    const byteArray = new Uint8Array(byteCharacters.length);
    // 解码后转回byteArray
    for (let i = 0; i < byteCharacters.length; i++) {
      byteArray[i] = byteCharacters.charCodeAt(i)
    }
    console.info("channelMessageNotify 解析后："+byteArray)
    if (this.onmessage !== null){
      console.info("channelMessageNotify 发送："+byteArray)
      const messageEvent= new MessageEvent('message',{data:byteArray.buffer})
      this.onmessage(messageEvent)
    }
  }
}

export class RTCDataChannelEvent{
  constructor() {
    this.channel = null
  }
}

export class RTCConfiguration{

}

export class RTCPeerConnection {
  constructor(configuration) {
    this.configuration = configuration
    this.ondatachannel = null
    this.channelMap = new Map()
    this.dataChannelNotify = (msg) => {
      // DC状态信息
      console.info("dataChannelNotify 收到消息："+msg)
      const response = JSON.parse(msg)
      const label = response.dcLabel
      const imsDCStatus = response.imsDCStatus
      console.info(`dataChannelNotify label：${label} ,imsDCStatus:${imsDCStatus}`)
      let channel = this.channelMap.get(label);
      console.info("dataChannelNotify channel："+channel)
      if (channel === undefined && this.ondatachannel !== null){
        // 对端创建的channel
        channel = new RTCDataChannel()
        channel.label = label
        this.channelMap.set(label,channel)
        this.ondatachannel(this,new RTCDataChannelEvent(channel))
      }
      channel.channelDataChannelNotify(imsDCStatus)
      if (imsDCStatus === 3){
        this.channelMap.delete(label)
      }
    }
    mDsBridge.register("dataChannelNotify",this.dataChannelNotify)
    this.messageNotify = (res) => {
      const response = JSON.parse(res)
      const dcLabel = response.dcLabel
      const msg = response.message// 获取message字段
      const channel = this.channelMap.get(dcLabel)
      channel.channelMessageNotify(msg)
    }
    mDsBridge.register("messageNotify",this.messageNotify)
  }
  createDataChannel(xml, dataChannelDict){
    let label = this.dcLabelFromXml(xml)
    let channel = this.channelMap.get(label);
    if (channel !== undefined){
      return channel;// 已经存在的channel，直接返回
    }
    let dataChannel = new RTCDataChannel()
    dataChannel.label = label
    this.channelMap.set(label,dataChannel)
    this.createADC(label,xml)
    return dataChannel
  }
  close(){
    this.ondatachannel = null
    this.channelMap.clear()
  }

  dcLabelFromXml(xml) {
    return xml.split("<DcLabel>")[1].split("<\\/DcLabel>")[0]
  }
  createADC(dcLabel,xml) {
    console.info(`createADC:`+dcLabel)
    const that = this
    mDsBridge.call(
        "async",`{"event":"DcEvent","function":"createAppDataChannel","params":{"dcLabels":["${dcLabel}"],"DataChannelAppInfoXml":"${xml}"}}`,
        function(v) {
          console.info(`createAppDataChannel响应：`+v)
          const response = JSON.parse(v)
          const code = response.code
          if (code === "1"){
            const channel = that.channelMap.get(dcLabel)
            if (channel !== undefined && channel.onerror !== null){
              channel.onerror()
            }
          }
        }
    )
  }
}

// 这个是关键
window.RTCPeerConnection = RTCPeerConnection;
window.RTCDataChannel = RTCDataChannel;
window.RTCDataChannelEvent = RTCDataChannelEvent;
window.RTCConfiguration = RTCConfiguration;
