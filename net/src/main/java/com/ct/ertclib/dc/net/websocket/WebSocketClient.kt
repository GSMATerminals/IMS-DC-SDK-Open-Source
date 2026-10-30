package com.ct.ertclib.dc.net.websocket

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.invoke
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okio.ByteString
import java.util.concurrent.TimeUnit
import java.util.logging.Logger

/**
 * WebSocket客户端 - 仅负责连接管理和消息收发
 */
class WebSocketClient(
    private val serverUrl: String,
    private val messageListener: MessageListener
) {
    companion object {
        private const val TAG = "[SDKLog][WebSocketClient]"
    }
    private val sLogger = Logger.getLogger(TAG.substring(0, 23))
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .pingInterval(5, TimeUnit.SECONDS)
        .build()

    private var webSocket: WebSocket? = null
    private var reconnectAttempts = 0
    private val maxReconnectAttempts = 2
    private var isManualDisconnect = false

    // 协程相关
    private val serialDispatcher = Dispatchers.IO.limitedParallelism(1)
    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val messageQueue = Channel<String>(Channel.UNLIMITED)
    private var sendJob: Job? = null
    private var reconnectJob: Job? = null

    init {
        // 启动消息发送协程
        startMessageSender()
    }

    /**
     * 启动消息发送器
     */
    private fun startMessageSender() {
        sendJob = coroutineScope.launch {
            for (message in messageQueue) {
                try {
                    // 等待连接建立
                    while (!isConnected() && !isManualDisconnect) {
                        delay(100)
                    }

                    if (isManualDisconnect) break

                    // 发送消息
                    val success = try {
                        sLogger.info("发送消息 $message")
                        webSocket?.send(message) ?: false
                    } catch (e: Exception) {
                        sLogger.info("发送消息失败 $e")
                        false
                    }

                    if (!success) {
                        // 发送失败，重新入队
                        messageQueue.send(message)
                        delay(1000) // 等待1秒后重试
                    }
                } catch (e: Exception) {
                    sLogger.info("消息发送协程异常 $e")
                }
            }
        }
    }

    /**
     * 连接WebSocket
     */
    fun connect(isAutoReconnect: Boolean = false) {
        if (!isAutoReconnect){
            reconnectAttempts = 0
        }
        isManualDisconnect = false
        val request = Request.Builder()
            .url(serverUrl)
            .build()
        webSocket = client.newWebSocket(request, createWebSocketCallback())
    }

    /**
     * 发送JSON消息
     */
    fun sendMessage(message: String): Boolean {
        return try {
            // 将消息加入队列
            coroutineScope.launch {
                messageQueue.send(message)
            }
            true
        } catch (e: Exception) {
            sLogger.info("消息入队失败 $e")
            false
        }
    }

    /**
     * 断开连接
     */
    fun disconnect() {
        isManualDisconnect = true
        cancelReconnect()

        // 清空消息队列
        coroutineScope.launch {
            messageQueue.cancel()
        }

        webSocket?.close(1000, "正常关闭")
        webSocket = null

        // 取消所有协程
        sendJob?.cancel()
        reconnectJob?.cancel()
    }

    /**
     * 是否已连接
     */
    fun isConnected(): Boolean = webSocket != null

    /**
     * 取消重连任务
     */
    private fun cancelReconnect() {
        reconnectJob?.cancel()
        reconnectJob = null
    }

    /**
     * 创建WebSocket回调
     */
    private fun createWebSocketCallback(): okhttp3.WebSocketListener {
        return object : okhttp3.WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                sLogger.info("WebSocket连接成功")
                cancelReconnect()
                coroutineScope.launch {
                    messageListener.onConnected()
                }
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                sLogger.info("收到消息 $text")
                coroutineScope.launch(serialDispatcher) {
                    messageListener.onMessage(text)
                }
            }

            override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                sLogger.info("收到二进制消息")
                coroutineScope.launch {
                    messageListener.onBinaryMessage(bytes)
                }
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                sLogger.info("WebSocket关闭中 $reason")
                webSocket.close(1000, null)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                sLogger.info("WebSocket关闭 $reason")
                coroutineScope.launch {
                    messageListener.onDisconnected()
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                sLogger.info("WebSocket错误 $t")
                if (!isManualDisconnect) {
                    attemptReconnect()
                }
            }
        }
    }

    /**
     * 尝试重连
     */
    private fun attemptReconnect() {
        if (reconnectAttempts < maxReconnectAttempts) {
            reconnectAttempts++
            val delay = (reconnectAttempts * 1000).toLong()

            cancelReconnect()

            reconnectJob = coroutineScope.launch {
                delay(delay)
                sLogger.info("尝试重连 ($reconnectAttempts/$maxReconnectAttempts)")
                connect(true)
            }
        } else {
            coroutineScope.launch {
                messageListener.onDisconnected()
            }
        }
    }

    /**
     * 消息监听接口
     */
    interface MessageListener {
        fun onConnected()
        fun onMessage(text: String)
        fun onBinaryMessage(bytes: ByteString)
        fun onDisconnected()
    }
}