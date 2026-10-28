import android.content.Context
import com.twilio.audioswitch.AudioDevice
import io.livekit.android.LiveKit
import io.livekit.android.audio.AudioSwitchHandler
import io.livekit.android.events.RoomEvent
import io.livekit.android.events.collect
import io.livekit.android.room.Room
import io.livekit.android.room.track.VideoTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch
import livekit.org.webrtc.SurfaceViewRenderer
import java.util.logging.Logger

class LiveKitManager(
    private val appContext: Context,
    private val remoteVideoRenderer: SurfaceViewRenderer,
    private val localVideoRenderer: SurfaceViewRenderer
) {
    private val TAG = "[SDKLog][LiveKitManager]"
    private val sLogger = Logger.getLogger(TAG.substring(0, 23))

    private var sessionScope: CoroutineScope? = null
    private var room: Room? = null
    private var isConnected = false
    private var isCallActive = false

    // 会话状态
    private var currentIsVideoCall = false
    private var audioSwitchHandler: AudioSwitchHandler? = null

    // 回调接口
    var listener: Listener? = null

    interface Listener {
        /** 房间连接成功（仅信令连接） */
        fun onRoomConnected()

        /** 通话激活成功（音视频已发布和订阅） */
        fun onCallActivated()

        /** 通话暂停（音视频已停止） */
        fun onCallDeactivated()

        /** 本地视频轨道已就绪 */
        fun onLocalVideoReady()

        /** 远端视频轨道已就绪 */
        fun onRemoteVideoReady()

        /** 房间断开连接 */
        fun onRoomDisconnected()

        /** 错误 */
        fun onError(error: String)
    }

    // ==================== 对外 API ====================

    /**
     * 初始化 LiveKit
     */
    fun init(listener: Listener) {
        this.listener = listener
        room = LiveKit.create(appContext)

        // 获取 AudioSwitchHandler 并配置
        audioSwitchHandler = room?.audioHandler as? AudioSwitchHandler

        // 强制使用听筒
        audioSwitchHandler?.preferredDeviceList = listOf(
            AudioDevice.Earpiece::class.java
        )

        room?.initVideoRenderer(remoteVideoRenderer)
        room?.initVideoRenderer(localVideoRenderer)
        localVideoRenderer.setMirror(true)
        remoteVideoRenderer.setMirror(true)
    }

    fun connectRoom(url: String, token: String, isVideoCall: Boolean) {
        sLogger.info("connectRoom - url: $url, isVideoCall: $isVideoCall")

        if (isConnected) {
            sLogger.info("Already connected, disconnecting first")
            disconnectRoom()
        }

        val currentRoom = room
        if (currentRoom == null) {
            sLogger.info("Room not initialized, call init() first")
            listener?.onError("LiveKit not initialized")
            return
        }

        currentIsVideoCall = isVideoCall

        sessionScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

        sessionScope?.launch(Dispatchers.IO) {
            launch {
                currentRoom.events.collect { event ->
                    sessionScope?.launch(Dispatchers.Main) {
                        handleEvent(event, currentRoom)
                    }
                }
            }
            currentRoom.connect(url, token)
        }
    }

    fun activateCall() {
        sLogger.info("activateCall - starting audio/video publishing and subscribing")

        if (!isConnected) {
            sLogger.info("Room not connected, cannot activate call")
            listener?.onError("Room not connected")
            return
        }

        if (isCallActive) {
            sLogger.info("Call already active")
            return
        }

        sessionScope?.launch(Dispatchers.IO) {
            val localParticipant = room?.localParticipant
            if (localParticipant != null) {
                // 启用麦克风
                localParticipant.setMicrophoneEnabled(true)
                sLogger.info("Microphone enabled")

                // 如果是视频通话，启用摄像头
                if (currentIsVideoCall) {
                    localParticipant.setCameraEnabled(true)
                    sLogger.info("Camera enabled")
                }
            }
        }

        isCallActive = true
        listener?.onCallActivated()
    }

    fun deactivateCall() {
        sLogger.info("deactivateCall - stopping audio/video publishing and subscribing")

        if (!isCallActive) {
            sLogger.info("Call not active")
            return
        }

        sessionScope?.launch(Dispatchers.IO) {
            try {
                room?.localParticipant?.setCameraEnabled(false)
                room?.localParticipant?.setMicrophoneEnabled(false)
                sLogger.info("Camera and microphone disabled")
            } catch (e: Exception) {
                sLogger.info("Disable media error: ${e.message}")
            }
        }

        try {
            remoteVideoRenderer.clearImage()
            localVideoRenderer.clearImage()
        } catch (e: Exception) {
            sLogger.info("Clear image error: ${e.message}")
        }

        isCallActive = false
        listener?.onCallDeactivated()
    }

    fun disconnectRoom() {
        sLogger.info("disconnectRoom - leaving room and releasing resources")

        // 先暂停通话
        if (isCallActive) {
            deactivateCall()
        }

        if (!isConnected) {
            sLogger.info("Room not connected")
            return
        }

        // 断开房间连接
        try {
            room?.disconnect()
            sLogger.info("Room disconnected")
        } catch (e: Exception) {
            sLogger.info("Disconnect error: ${e.message}")
        }

        // 取消会话协程作用域
        sessionScope?.coroutineContext?.cancelChildren()
        sessionScope = null

        // 清空状态
        isConnected = false
    }

    fun release() {
        sLogger.info("release")
        disconnectRoom()
        try {
            room?.release()
            room = null
        } catch (e: Exception) {
            sLogger.info("Release error: ${e.message}")
        }
    }

    // ==================== 内部方法 ====================

    private fun handleEvent(event: RoomEvent, currentRoom: Room) {
        sLogger.info("Room event: $event")
        when (event) {
            is RoomEvent.Connected -> {
                sLogger.info("Room connected")
                isConnected = true
                listener?.onRoomConnected()
            }
            is RoomEvent.TrackSubscribed -> {
                val track = event.track
                if (event.participant != currentRoom.localParticipant && track is VideoTrack) {
                    sLogger.info("Remote video track subscribed")
                    sessionScope?.launch(Dispatchers.Main){
                        if (isCallActive && currentIsVideoCall) {
                            track.addRenderer(remoteVideoRenderer)
                            listener?.onRemoteVideoReady()
                        }
                    }

                }
            }
            is RoomEvent.LocalTrackSubscribed -> {
                val track = event.publication.track
                if (event.participant == currentRoom.localParticipant && track is VideoTrack) {
                    sLogger.info("Local video track published")
                    sessionScope?.launch(Dispatchers.Main){
                        if (currentIsVideoCall) {
                            track.addRenderer(localVideoRenderer)
                            listener?.onLocalVideoReady()
                        }
                    }

                }
            }
            is RoomEvent.Disconnected -> {
                sLogger.info("Room disconnected")
                isConnected = false
                isCallActive = false
                listener?.onRoomDisconnected()
            }
            else -> {}
        }
    }
}