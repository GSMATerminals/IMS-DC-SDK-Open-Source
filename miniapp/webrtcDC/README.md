# WebRTC Data Channel Implementation for IMS DC

This project implements the interfaces specified in GSMA TS.66 "IMS data channel API specification CR1040", aiming to provide 5G New Calling IMS DC Application developers with a unified API for creating and using DC (Data Channel) to facilitate interoperability.

## Features
- Implements GSMA TS.66 standard interfaces
- Provides WebRTC-compatible API for data channel operations
- Supports both creating and receiving data channels
- Enables message exchange and binary data transfer
- Includes channel status monitoring and error handling

## Build  
```bash
npm run build  
```
This will generate the webrtcDC.js file in the dist directory. 

## Usage Example 
Import the compiled webrtcDC.js library locally. The API usage is consistent with standard WebRTC libraries.
```angular2html
// Import webrtcDC.js library
import 'xxx/xxx/webrtcDC.js'

export default {
  data() {
    return {
      appId: "333",
      dcId: "test",
      dcLabel: "local_333_1_test",
      imsDCStatus: -1,
      pc: null,
      dataChannel: null,
    }
  },
  mounted() {
    // Create peer connection
    this.pc = new RTCPeerConnection();
    const that = this
    // Listen for data channels created by the remote peer
    this.pc.ondatachannel = (event) => {
      that.dataChannel = event.channel;
      that.initDataChannelEvent()
    }
  },
  methods: {
    initDataChannelEvent() {
      // Set up data channel event listeners
      this.dataChannel.onopen = () => {
        console.log("Data channel opened");
        this.imsDCStatus = 1
      };
      this.dataChannel.onclose = () => {
        console.log("Data channel closed");
        this.imsDCStatus = -1
      };
      this.dataChannel.onmessage = (event) => {
        console.log("Message received:", event.data);
        this.messageNotify(event.data)
      };
      this.dataChannel.onerror = (error) => {
        console.error("Data channel error:", error);
      };
    },
    createAppDataChannel() {
      // Create application data channel
      const xml = `<DataChannelAppInfo><DataChannelApp appId=\\"${this.appId}\\"><DataChannel dcId=\\"${this.dcId}\\"><DcLabel>${this.dcLabel}<\\/DcLabel><Subprotocol><\\/Subprotocol><Ordered>1<\\/Ordered><MaxRetr>0<\\/MaxRetr><MaxTime>0<\\/MaxTime><Priority><\\/Priority><UseCase>1<\\/UseCase><AutoAcceptDcSetup>0<\\/AutoAcceptDcSetup><Bandwidth>500<\\/Bandwidth><QosHint>loss=0.0002;latency=600<\\/QosHint><\\/DataChannel><\\/DataChannelApp><\\/DataChannelAppInfo>`
      this.dataChannel = this.pc.createDataChannel(xml, null)
      this.initDataChannelEvent()
    },
    closeAppDataChannel() {
      // Close application data channel
      this.dataChannel.close()
    },
    sendData(byteArray) {
      // Send data through the channel
      if (this.imsDCStatus === 1) {
        this.dataChannel.send(byteArray)
      } else {
        console.error("DC channel not established")
      }
    },
    getBufferedAmount() {
      // Query buffered amount
      return this.dataChannel.getBufferedAmount();
    },
    messageNotify(byteArray) {
      // Process received data
      // 1. Can convert to string for JSON processing
      const stringFromByteArray = new TextDecoder().decode(byteArray);
      console.log("Received DC data string:", stringFromByteArray)
      // 2. Can also process as binary data (e.g., for files)
    },
    str2ByteArray(str) {
      // Convert string to byte array
      return new TextEncoder().encode(str)
    },
  }
}
```

## Runtime Environment
The WebRTC interfaces in webrtcDC.js are implemented based on the JS API provided by IMS DC SDK. Therefore, IMS DC Applications developed using this library need to run in an environment with the IMS DC SDK.