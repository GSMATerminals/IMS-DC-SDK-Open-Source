/*
 *   Copyright 2025-China Telecom Research Institute.
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *        https://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package com.ct.ertclib.dc.feature.testing.socket

import com.blankj.utilcode.util.SPUtils
import com.ct.ertclib.dc.core.utils.common.JsonUtil
import com.ct.ertclib.dc.core.utils.logger.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.DataInputStream
import java.io.DataOutputStream
import java.net.ServerSocket
import java.net.Socket
import java.util.Base64

object DCSocketManager {
    private val TAG = "DCSocket"
    private val sLogger = Logger.getLogger(TAG)
    private val msgObserverMap: HashMap<String, (ByteArray) -> Unit> = HashMap()
    private var role = ""
    private var isInitSocket = false
    private val port = 9001

    // Socket 相关变量
    private var serverSocket: ServerSocket? = null
    private var clientSocket: Socket? = null
    private var dataInputStream: DataInputStream? = null
    private var dataOutputStream: DataOutputStream? = null

    private var adcObserver: ((ArrayList<String>?) -> Unit)? = null
    private var callObserver: ((String) -> Unit)? = null

    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    fun initSocket() {
        sLogger.info("initSocket")
        if (isInitSocket) return
        isInitSocket = true
        sLogger.info("initSocket true")
        scope.launch(Dispatchers.IO) {
            role = SPUtils.getInstance().getString("tcpRole")
            sLogger.info("initSocket start role:$role")
            if (role == "client") {
                // 客户端等2秒再发起连接，保证服务端先启动
                delay(2000)
                val host = SPUtils.getInstance().getString("host")
                connectToServer(host)
            } else if (role == "server") {
                // 启动ServerSocket
                startServer()
            }
        }
    }


    private fun connectToServer(host: String) {
        scope.launch(Dispatchers.IO) {
            try {
                clientSocket = Socket(host, port)
                sLogger.info("Connecting to server...")
                dataInputStream = DataInputStream(clientSocket!!.getInputStream())
                dataOutputStream = DataOutputStream(clientSocket!!.getOutputStream())
                sLogger.info("Connected to server.")
                // 启动接收数据的协程
                startReceivingData()
            } catch (e: Exception) {
                e.printStackTrace()
                // 连接失败，可以尝试重连
                reconnectToServer(host)
            }
        }
    }

    private fun reconnectToServer(host: String) {
        scope.launch(Dispatchers.IO) {
            while (isInitSocket && role == "client") {
                try {
                    delay(5000) // 5秒后重试
                    clientSocket = Socket(host, port)
                    dataInputStream = DataInputStream(clientSocket!!.getInputStream())
                    dataOutputStream = DataOutputStream(clientSocket!!.getOutputStream())
                    startReceivingData()
                    break // 连接成功，退出重连循环
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    private fun startServer() {
        scope.launch(Dispatchers.IO) {
            try {
                serverSocket = ServerSocket(port)
                while (isInitSocket && role == "server") {
                    try {
                        val socket = serverSocket!!.accept()
                        sLogger.info("Accepted connection from ${socket.inetAddress.hostAddress}.")
                        // 处理新连接，关闭之前的连接
                        clientSocket?.close()
                        clientSocket = socket
                        dataInputStream = DataInputStream(socket.getInputStream())
                        dataOutputStream = DataOutputStream(socket.getOutputStream())
                        sLogger.info("Accepted connection.")
                        startReceivingData()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                sLogger.error("Failed to start server.", e)
            }
        }
    }

    private fun startReceivingData() {
        scope.launch(Dispatchers.IO) {
            while (isInitSocket && clientSocket != null && clientSocket!!.isConnected) {
                try {
                    val length = dataInputStream!!.readInt()
                    if (length > 0) {
                        val buffer = ByteArray(length)
                        dataInputStream!!.readFully(buffer)
                        val jsonString = String(buffer)
                        dealSocketData(jsonString.toByteArray())
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    break
                }
            }
        }
    }

    private fun dealSocketData(byteArray: ByteArray) {
        try {
            val jsonString = String(byteArray)
            sLogger.info("Received data: $jsonString")
            val socketData = parseSocketData(jsonString)

            when (socketData?.type) {
                1 -> { // 对端创建ADC
                    adcObserver?.invoke(socketData.createLabels)
                }
                2 -> { // 普通数据
                    val label = socketData.dataLabel ?: ""
                    val data = socketData.data ?: ""
                    val decodedData = Base64.getDecoder().decode(data)
                    msgObserverMap[label]?.invoke(decodedData)
                }
                3 -> { // 对方发起呼叫
                    callObserver?.invoke("added")
                }
                4 -> { // 对方挂断
                    callObserver?.invoke("hangup")
                }
                5 -> { // 对方接听
                    callObserver?.invoke("active")
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun parseSocketData(jsonString: String): SocketData? {
        val socketData = JsonUtil.fromJson(jsonString, SocketData::class.java)
        return socketData
    }

    fun notifyCallAdded() {
        val socketData = SocketData().apply {
            type = 3
            data = ""
        }
        sendSocketData(socketData)
    }

    fun notifyCallActive() {
        val socketData = SocketData().apply {
            type = 5
            data = ""
        }
        sendSocketData(socketData)
    }

    fun notifyHangUp() {
        val socketData = SocketData().apply {
            type = 4
            data = ""
        }
        sendSocketData(socketData)
    }

    fun notifyCreateADC(labels: ArrayList<String>) {
        if (labels.isNotEmpty()) {
            val socketData = SocketData().apply {
                type = 1
                createLabels = labels
                data = ""
            }
            sendSocketData(socketData)
        }
    }

    fun registerADCObserver(adcObserver: (ArrayList<String>?) -> Unit) {
//        sLogger.info("registerADCObserver")
        this.adcObserver = adcObserver
    }

    fun unRegisterADCObserver() {
        this.adcObserver = null
    }

    fun registerCallObserver(callObserver: (String) -> Unit) {
        this.callObserver = callObserver
    }

    fun unRegisterCallObserver() {
        this.callObserver = null
    }
    fun registerMsgObserver(label: String, observer: (ByteArray) -> Unit) {
        msgObserverMap[label] = observer
    }

    fun sendData(label: String, byteArray: ByteArray) {
        val encodedData = Base64.getEncoder().encodeToString(byteArray)
        val socketData = SocketData().apply {
            type = 2
            dataLabel = label
            data = encodedData
        }
        sendSocketData(socketData)
    }

    private fun sendSocketData(socketData: SocketData) {
        scope.launch(Dispatchers.IO) {
            try {
                val jsonString = JsonUtil.toJson(socketData)
                sLogger.info("Sending data: $jsonString")
                val data = jsonString.toByteArray()
                dataOutputStream?.apply {
                    writeInt(data.size)
                    write(data)
                    flush()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun destroy() {
        role = ""
        msgObserverMap.clear()
        adcObserver = null

        try {
            dataInputStream?.close()
            dataOutputStream?.close()
            clientSocket?.close()
            serverSocket?.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        dataInputStream = null
        dataOutputStream = null
        clientSocket = null
        serverSocket = null

        isInitSocket = false
        sLogger.info("destroy")
    }
}