/*
 *   Copyright 2025-China Telecom Research Institute.
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package com.ct.ertclib.dc.feature.testing.socket

import android.content.Context
import android.text.TextUtils
import androidx.lifecycle.MutableLiveData
import com.blankj.utilcode.util.FileUtils
import com.blankj.utilcode.util.SPUtils
import com.blankj.utilcode.util.ThreadUtils
import com.blankj.utilcode.util.Utils
import com.ct.ertclib.dc.core.utils.logger.Logger
import com.ct.ertclib.dc.core.manager.call.NewCallsManager
import com.ct.ertclib.dc.core.data.common.ContentType
import com.ct.ertclib.dc.core.common.CompressManager
import com.ct.ertclib.dc.core.common.MessageStatus
import com.ct.ertclib.dc.core.utils.common.MimeUtils
import com.ct.ertclib.dc.core.common.PathManager
import com.ct.ertclib.dc.core.utils.common.UriUtils
import com.ct.ertclib.dc.core.common.NewCallDatabase
import com.ct.ertclib.dc.core.data.model.ConversationEntity
import com.ct.ertclib.dc.core.data.model.MessageEntity
import com.ct.ertclib.dc.core.data.event.DBEvent
import com.ct.ertclib.dc.core.common.EventDispatcher
import com.ct.ertclib.dc.core.data.event.UpdateDownloadProgressEvent
import com.ct.ertclib.dc.core.common.ConfirmActivity
import com.ct.ertclib.dc.core.manager.common.StateFlowManager
import com.ct.ertclib.dc.core.miniapp.MiniAppStartManager
import com.ct.ertclib.dc.core.miniapp.aidl.IMessageCallback
import com.ct.ertclib.dc.core.data.screenshare.SketchXMLUtils
import com.ct.ertclib.dc.core.data.screenshare.xml.DrawingInfo
import com.ct.ertclib.dc.core.utils.common.ScreenUtils
import com.ct.ertclib.dc.feature.testing.DBUriUtil
import com.ct.ertclib.dc.feature.testing.INetworkCallback
import com.ct.ertclib.dc.feature.testing.INetworkManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.OutputStream

class SocketNetworkManager private constructor(
    val isServer: Boolean,
    val ip: String = "127.0.0.1",
    val port: Int = 8888,
    val connected: MutableLiveData<Boolean>,
    val callsManager: NewCallsManager
) : INetworkManager, KoinComponent {
    private val sLogger: Logger = Logger.getLogger(TAG)
    private var mCallback: INetworkCallback? = null

    companion object {
        private var INSTANCE: SocketNetworkManager? = null
        private const val TAG = "SocketNetworkManager"

        fun init(
            isServer: Boolean,
            ip: String = "127.0.0.1",
            port: Int = 8888,
            connected: MutableLiveData<Boolean>,
            callsManager: NewCallsManager
        ) {
            INSTANCE =
                SocketNetworkManager(isServer, ip, port, connected, callsManager)
        }

        /**
         * 需要先执行init
         */
        fun getINSTANCE(): SocketNetworkManager? {
            return INSTANCE
        }


        private const val CMD_SKETCH_REQUEST = 1000
        private const val CMD_SKETCH_AGREE = 1099
        private const val CMD_SKETCH_DISAGREE = 1098
        private const val CMD_SKETCH_OPEN = 1001
        private const val CMD_SKETCH_CLOSE = 1002

        private const val CMD_SENDFILE_REQUEST = 1010
        private const val CMD_SENDFILE_AGREE = 1109
        private const val CMD_SENDFILE_DISAGREE = 1108

        private const val CMD_SCREEN_REQUEST = 1020
        private const val CMD_SCREEN_AGREE = 1209
        private const val CMD_SCREEN_DISAGREE = 1208
        private const val CMD_SCREEN_OPEN = 1021
        private const val CMD_SCREEN_CLOSE = 1022
    }

    var server: ServerThread? = null
    var clinet: ClientThread? = null
    var watcher: Watcher? = null
    var rawWatcher: RawPackWatcher? = null
    var sendFileTask: SendFileTask? = null

    val inDataPackList = DataPackList(120)
    val outDataPackList = DataPackList(100)
    private val applicationContext: Context by inject()

    suspend fun start() = withContext(Dispatchers.Default) {
        sLogger.info("start isServer:$isServer ip:$ip port:$port")
        try {
            if (isServer) {
                server = ServerThread(port, inDataPackList, outDataPackList)
            } else {
                clinet = ClientThread(ip, port, inDataPackList, outDataPackList)
            }
            server?.start()
            clinet?.connect()
            clinet?.start()
            watcher = Watcher()
            watcher?.start()
            rawWatcher = RawPackWatcher()
            rawWatcher?.start()
            connected.postValue(true)
        } catch (e: Exception) {
            sLogger.warn("start error $e")
            server?.onError(e)
            clinet?.onError(e)
            watcher?.closeReceiving()
            rawWatcher?.closeReceiving()
        }
    }

    suspend fun stop() = withContext(Dispatchers.Default) {
        try {
            server?.exit()
            clinet?.exit()
            watcher?.closeReceiving()
            connected.postValue(false)
        } catch (e: Exception) {
            sLogger.warn("close error $e")
        }
    }

    override fun sendByte(bytes: ByteArray) {
        val rawBytePack = RawBytePack.buildRawPack(bytes)
        outDataPackList.putDataPack(rawBytePack)
    }

    override fun sendCMD(cmd: String) {
        val ctrlPack = CtrlPack(cmd)
        outDataPackList.putDataPack(ctrlPack)
//        sendMessage(cmd)
    }

    override fun sendMessage(msg: String) {
        val rawBytePack = RawBytePack.buildRawPack(msg.toByteArray())
        outDataPackList.putDataPack(rawBytePack)
    }

    override fun sendFile(filename: String, path: String, length: Long, mimetype: String) {
        if (!FileUtils.isFileExists(path)) {
            sLogger.info("File not exist")
            return
        }
        sLogger.info("filename:$filename path:$path \n length:$length mimetype:$mimetype")
        sendCMD("cmd:sendfile:request")
        sendFileTask = SendFileTask(filename, path, length, mimetype)
    }


    override fun sendSketch(drawingInfo: DrawingInfo) {
        val wrapActionXml = SketchXMLUtils.wrapActionXml(drawingInfo.toXMl())
        val sketchPack = SketchPack(wrapActionXml)
        outDataPackList.putDataPack(sketchPack)
    }

    override fun setCallback(callback: INetworkCallback) {
        this.mCallback = callback
    }

    inner class ServerThread(port: Int, dataIn: DataPackList, dataOut: DataPackList) :
        SocketServerThread(
            port, dataIn,
            dataOut
        ) {
        override fun onError(t: Throwable) {
            sLogger.warn("ServerThread" + t.message)
            connected.postValue(false)
        }

    }

    inner class ClientThread(ip: String, port: Int, dataIn: DataPackList, dataOut: DataPackList) :
        SocketClientThread(
            ip, port,
            dataIn,
            dataOut
        ) {
        override fun onError(t: Throwable) {
            sLogger.warn("ClientThread" + t.message)
            connected.postValue(false)
        }

    }

    inner class RawPackWatcher : Thread("Thread-RawWatcher") {
        var receiving = true
        override fun run() {
            while (receiving) {
                inDataPackList.getRawPack(true)?.let {
                    val rawPack = WriteableUtil.parse(it.toByteArray(), RawBytePack::class.java)
                    val string = String(rawPack.byteArray, Charsets.UTF_8)
                    sLogger.info("getRawPack:${string}")
                    mCallback?.onMessage(string)
                    val appId = SPUtils.getInstance().getString("appId", "")
                    if (!TextUtils.isEmpty(appId)) {
                        MiniAppStartManager.sendMessageToMiniApp(
                            "TC@1",
                            appId,
                            string,
                            object : IMessageCallback.Stub() {
                                override fun reply(message: String?) {
                                    if (sLogger.isDebugActivated) {
                                        sLogger.debug("AppService MessageCallBackImpl reply message:$message")
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }

        fun closeReceiving() {
            receiving = false
        }
    }

    inner class Watcher : Thread("Thread-Watcher") {
        var receiving = true
        override fun run() {
            while (receiving) {
                inDataPackList.getCRTPack(true)?.let {
                    val ctrl = WriteableUtil.parse(it.toByteArray(), CtrlPack::class.java)
                    dispatchCmd(ctrl.getRawCMD())
                }
            }
        }

        fun closeReceiving() {
            receiving = false
        }

        private fun dispatchCmd(cmd: String) {
            when (getCMDType(cmd)) {
                CMD_SKETCH_REQUEST -> {
                    ThreadUtils.getMainHandler().post {
                        ConfirmActivity.startConfirm(
                            Utils.getApp(),
                            "对方请求开启屏幕标记",
                            object : ConfirmActivity.ConfirmCallback {
                                override fun onAccept() {
                                    sendCMD("cmd:sketch:agree")
                                    val screenWidth = ScreenUtils.getScreenWidth(applicationContext)
                                    val screenHeight = ScreenUtils.getScreenHeight(applicationContext)
                                    sLogger.info("open sketch share ScreenSize=${screenWidth}x${screenHeight}")
                                    sendCMD("data:sketch:size=${screenWidth}x${screenHeight}")
                                }

                                override fun onCancel() {
                                    sendCMD("cmd:sketch:disagree")
                                }
                            }, "同意", "拒绝"
                        )
                    }
                }

                CMD_SKETCH_AGREE -> {

                }

                CMD_SKETCH_DISAGREE -> {

                }

                CMD_SENDFILE_REQUEST -> {
                    ThreadUtils.getMainHandler().post {
                        ConfirmActivity.startConfirm(
                            Utils.getApp(),
                            "对方请求发送文件",
                            object : ConfirmActivity.ConfirmCallback {
                                override fun onAccept() {
                                    sendCMD("cmd:sendfile:agree")
                                    ThreadUtils.getCpuPool().submit(ReceiveFileThread())
                                }

                                override fun onCancel() {
                                    sendCMD("cmd:sendfile:disagree")
                                }

                            }, "同意", "拒绝"
                        )
                    }
                }

                CMD_SENDFILE_AGREE -> {
                    sendFileTask?.start()
                }

                CMD_SENDFILE_DISAGREE -> {

                }

                CMD_SCREEN_REQUEST -> {
                    ThreadUtils.getMainHandler().post {
                        ConfirmActivity.startConfirm(
                            Utils.getApp(),
                            "对方请求开启屏幕共享",
                            object : ConfirmActivity.ConfirmCallback {
                                override fun onAccept() {
                                    sendCMD("cmd:screen:agree")
                                    val screenWidth = ScreenUtils.getScreenWidth(applicationContext)
                                    val screenHeight = ScreenUtils.getScreenHeight(applicationContext)
                                    sLogger.info("open screen share ScreenSize=${screenWidth}x${screenHeight}")
                                    sendCMD("data:screen:size=${screenWidth}x${screenHeight}")
                                }

                                override fun onCancel() {
                                    sendCMD("cmd:screen:disagree")
                                }
                            }, "同意", "拒绝"
                        )
                    }
                }

                CMD_SCREEN_AGREE -> {
                }

                CMD_SCREEN_DISAGREE -> {

                }

                else -> {}
            }
        }

        private fun getCMDType(string: String): Int {
            when (string) {
                "cmd:sketch:request" -> {
                    return CMD_SKETCH_REQUEST
                }

                "cmd:sketch:agree" -> {
                    return CMD_SKETCH_AGREE
                }

                "cmd:sketch:disagree" -> {
                    return CMD_SKETCH_DISAGREE
                }

                "cmd:sketch:open" -> {
                    return CMD_SKETCH_OPEN
                }

                "cmd:sketch:close" -> {
                    return CMD_SKETCH_CLOSE
                }

                "cmd:sendfile:request" -> {
                    return CMD_SENDFILE_REQUEST
                }

                "cmd:sendfile:agree" -> {
                    return CMD_SENDFILE_AGREE
                }

                "cmd:sendfile:disagree" -> {
                    return CMD_SENDFILE_DISAGREE
                }

                "cmd:screen:request" -> {
                    return CMD_SCREEN_REQUEST
                }

                "cmd:screen:agree" -> {
                    return CMD_SCREEN_AGREE
                }

                "cmd:screen:disagree" -> {
                    return CMD_SCREEN_DISAGREE
                }

                "cmd:screen:open" -> {
                    return CMD_SCREEN_OPEN
                }

                "cmd:screen:close" -> {
                    return CMD_SCREEN_CLOSE
                }
            }
            return -1
        }
    }

    /**
     *
     * @property filename String 文件名称
     * @property path String 文件路径
     * @property length Long 文件长度
     * @property mimetype String 文件类型
     * @constructor
     */
    //qys
    inner class SendFileTask(
        val filename: String,
        val path: String,
        val length: Long,
        val mimetype: String
    ) : Thread("Thread-SendFileTask") {
        override fun run() {
            var fileInputStream: FileInputStream? = null
            try {
                fileInputStream = FileInputStream(path)
                val byteArray = ByteArray(1024 * 8)
                var len: Long = 0
                var packsNum = 0

                val callInfo = callsManager.getCallInfo("")?: return
                val querySelfId =
                    Utils.getApp().contentResolver.query(
                        DBUriUtil.getContactUri(),
                        null,
                        "phoneNumber=?",
                        arrayOf(callInfo.myNumber),
                        null
                    )
                val querySendId =
                    Utils.getApp().contentResolver.query(
                        DBUriUtil.getContactUri(),
                        null,
                        "phoneNumber=?",
                        arrayOf(callInfo.remoteNumber),
                        null
                    )

                val selfid = if (querySelfId?.moveToFirst() == true) {
                    querySelfId.getInt(0)
                } else {
                    if (isServer) 1 else 2
                }
                val sendid = if (querySendId?.moveToFirst() == true) {
                    querySendId.getInt(0)
                } else {
                    if (isServer) 1 else 2
                }
                val conversationDao =
                    NewCallDatabase.getInstance().conversationDao()

                var conversationEntity = conversationDao.queryConversation(sendid)
                val conversationId: Int = if (conversationEntity != null) {
                    conversationEntity._id
                } else {
                    conversationEntity =
                        ConversationEntity(0, null, null, 0, null, sendid, 0)
                    conversationDao.insertConversation(conversationEntity).toInt()
                }

                val sendTime = System.currentTimeMillis()
                val uri = UriUtils.file2Uri(Utils.getApp(), File(path))
                val extension = filename.substring(filename.lastIndexOf(".") + 1)
                val mimetype = MimeUtils.guessMimeTypeFromExtension(extension)
                var messageId = 0;
                val messageEntity = MessageEntity(
                    messageId,
                    conversationId,
                    selfid,
                    MessageStatus.OUTGOING_SENDING.value,
                    0,
                    mimetype ?: "application/*",
                    filename,
                    uri.toString(),
                    length,
                    sendTime,
                    sendTime,
                    selfid,
                    null,
                    0,
                    "ddddd"
                )

                messageEntity?.let { itMessage ->
                    if (ContentType.isImage(itMessage.type)) {
                        val filename = itMessage.message
                        val extension = filename.substring(filename.lastIndexOf("."))
                        itMessage.thumbnailUri =
                            PathManager().getInternalCacheDirPath(Utils.getApp()) + System.currentTimeMillis() + extension
                        val compressManager = CompressManager(Utils.getApp())
                        compressManager.setCompressTask(mutableListOf(itMessage))
                        compressManager.startCompress()
                    }
                }

//                var intent = Intent(ACTION_MESSAGE_NOTIFY).apply {
//                    component = ComponentName(
//                        Utils.getApp().packageName,
//                        ChatMessageReceiver::class.java.name
//                    )
//                    putExtra("data", messageEntity)
//                }
                val observer = object : EventDispatcher.EventObserver<DBEvent> {
                    override fun onEvent(t: DBEvent) {
                        messageId = t.message
                    }
                }
//                Utils.getApp().sendBroadcast(intent)
                EventDispatcher.registerEventObserver(DBEvent::class.java, observer)

                val startPack = FilePack(filename.toByteArray(), 0, length, 0)
                outDataPackList.putDataPack(startPack)

                len = fileInputStream.read(byteArray).toLong()
                var nowTime: Long = System.currentTimeMillis()
                var nowLength: Long = 0
                while (len != -1L) {
                    try {
                        packsNum++
                        nowLength += len
                        val filePack = FilePack(byteArray, 1, len, packsNum)
                        outDataPackList.putDataPack(filePack)
                    } catch (e: Exception) {
                        sLogger.warn("SendFileTask " + e.message)
                    }
                    len = fileInputStream.read(byteArray).toLong()
                    if (System.currentTimeMillis() - nowTime > 1000) {
//                        intent =
//                            Intent(ACTION_MESSAGE_FILE_TRANSFER_PROGRESS).apply {
//                                component = ComponentName(
//                                    Utils.getApp().packageName,
//                                    ChatMessageReceiver::class.java.name
//                                )
//                                putExtra("id", messageId)
//                                val progress = 1.0f * nowLength / length * 100.0f
//                                putExtra("progress", progress)
//                            }
//                        Utils.getApp().sendBroadcast(intent)
                        nowTime = System.currentTimeMillis()
                    }
                }

                packsNum++
                val endPack = FilePack(filename.toByteArray(), 2, length, packsNum)
                outDataPackList.putDataPack(endPack)
                sLogger.info("Send $filename finished $packsNum")

                messageEntity?.apply {
                    _id = messageId
                    status = MessageStatus.OUTGOING_COMPLETE.value
                }
//                intent = Intent(ACTION_MESSAGE_STATUS_CHANGED).apply {
//                    component = ComponentName(
//                        Utils.getApp().packageName,
//                        ChatMessageReceiver::class.java.name
//                    )
//                    putExtra("data", messageEntity)
//                }
//                Utils.getApp().sendBroadcast(intent)
                EventDispatcher.unregisterEventObserver(DBEvent::class.java, observer)
            } catch (e: Exception) {
                sLogger.warn(e.message, e)
            } finally {
                try {
                    fileInputStream?.close()
                } catch (e: Exception) {
                    sLogger.warn(e.message, e)
                }
            }
        }
    }

    inner class ReceiveFileThread : Thread("Thread-ReceiveFileThread") {
        override fun run() {
            inDataPackList.let {
                var receiving = true
                var fileLength: Long = 0
                var fileName = ""
                var currentLength: Long = 0
                var file: File? = null
                var outputStream: OutputStream? = null
                var messageEntity: MessageEntity? = null
                var noPackTime: Long = 0
                var packnum = 0
                var nowTime: Long = System.currentTimeMillis()
                var messageId = 0

                val observer = object : EventDispatcher.EventObserver<DBEvent> {
                    override fun onEvent(t: DBEvent) {
                        messageId = t.message
                    }
                }
                EventDispatcher.registerEventObserver(DBEvent::class.java, observer)

                while (receiving) {
                    it.getFilePack()?.let { filepack ->
                        val tmp =
                            WriteableUtil.parse(filepack.toByteArray(), FilePack::class.java)
                        if (tmp.isStart()) {
                            fileLength = tmp.fileLength
                            fileName = String(tmp.byteArray)
                            sLogger.info("get package $fileName start")
                            file =
                                PathManager().createCacheFile(Utils.getApp(),fileName = fileName)
                            outputStream = FileOutputStream(file)

                            val callInfo = callsManager.getCallInfo("")?: return
                            val querySelfId =
                                Utils.getApp().contentResolver.query(
                                    DBUriUtil.getContactUri(),
                                    null,
                                    "phoneNumber=?",
                                    arrayOf(callInfo.myNumber),
                                    null
                                )
                            val querySendId =
                                Utils.getApp().contentResolver.query(
                                    DBUriUtil.getContactUri(),
                                    null,
                                    "phoneNumber=?",
                                    arrayOf(callInfo.remoteNumber),
                                    null
                                )

                            val selfid = if (querySelfId?.moveToFirst() == true) {
                                querySelfId.getInt(0)
                            } else {
                                if (isServer) 1 else 2
                            }
                            val sendid = if (querySendId?.moveToFirst() == true) {
                                querySendId.getInt(0)
                            } else {
                                if (isServer) 1 else 2
                            }
                            val sendTime = System.currentTimeMillis()
                            val uri = UriUtils.file2Uri(Utils.getApp(), file!!)
                            val extension = fileName.substring(fileName.lastIndexOf(".") + 1)
                            val mimetype = MimeUtils.guessMimeTypeFromExtension(extension)
                            val conversationDao =
                                NewCallDatabase.getInstance().conversationDao()

                            var conversationEntity = conversationDao.queryConversation(sendid)
                            val conversationId: Int = if (conversationEntity != null) {
                                conversationEntity._id
                            } else {
                                conversationEntity =
                                    ConversationEntity(
                                        0,
                                        System.currentTimeMillis().toString(),
                                        null,
                                        0,
                                        null,
                                        sendid,
                                        0
                                    )

                                conversationDao.insertConversation(conversationEntity).toInt()
                            }

                            messageEntity = MessageEntity(
                                0,
                                conversationId,
                                sendid,
                                MessageStatus.INCOMING_AUTO_DOWNLOADING.value,
                                0,
                                mimetype ?: "application/*",
                                fileName,
                                uri.toString(),
                                fileLength,
                                sendTime,
                                sendTime,
                                selfid,
                                null,
                                0,
                                "eeee"
                            )
//                            val intent = Intent(ACTION_MESSAGE_NOTIFY).apply {
//                                component = ComponentName(
//                                    Utils.getApp().packageName,
//                                    ChatMessageReceiver::class.java.name
//                                )
//                                putExtra("data", messageEntity)
//                            }
//                            Utils.getApp().sendBroadcast(intent)
                        } else if (tmp.isEnd()) {
                            receiving = false
                            outputStream?.let { outputStream ->
                                outputStream.flush()
                                outputStream.close()
                            }
                            sLogger.info("get package $fileName end $currentLength:$fileLength")
                        } else {
                            outputStream?.write(
                                tmp.byteArray,
                                0,
                                tmp.fileLength.toInt()
                            )
                            currentLength += tmp.fileLength.toInt()
                            if (System.currentTimeMillis() - nowTime > 1000) {
                                val progress = 1.0f * currentLength / fileLength * 100.0f
                                val updateDownloadProgressEvent =
                                    UpdateDownloadProgressEvent(messageId, progress)
                                StateFlowManager.emitUpdateDownloadProgress(updateDownloadProgressEvent)
                                nowTime = System.currentTimeMillis()
                            }
                        }
                        it.getFilePack(true)
                        packnum++
                    }
                    if (it.getFilePack() == null) {
                        val currentTimeMillis = System.currentTimeMillis()
                        if (noPackTime == 0L) {
                            noPackTime = currentTimeMillis
                        }
                        if (currentTimeMillis - noPackTime > 30000) {
                            receiving = false
                            sLogger.info("timeout to receive file")
                        }
                        noPackTime = System.currentTimeMillis()
                        sleep(1000)
                    }
                }
                messageEntity?.apply {
                    status = MessageStatus.INCOMING_COMPLETE.value
                }
                messageEntity?.let { itMessage ->
                    if (ContentType.isImage(itMessage.type)) {
                        val filename = itMessage.message
                        val extension = filename.substring(filename.lastIndexOf("."))
                        itMessage.thumbnailUri =
                            PathManager().getInternalCacheDirPath(Utils.getApp()) + System.currentTimeMillis() + extension
                        val compressManager = CompressManager(Utils.getApp())
                        compressManager.setCompressTask(mutableListOf(itMessage))
                        compressManager.startCompress()
                    }
                }

//                val intent = Intent(ACTION_MESSAGE_STATUS_CHANGED).apply {
//                    component = ComponentName(
//                        Utils.getApp().packageName,
//                        ChatMessageReceiver::class.java.name
//                    )
//                    putExtra("data", messageEntity)
//                }
//                Utils.getApp().sendBroadcast(intent)
                EventDispatcher.unregisterEventObserver(DBEvent::class.java, observer)
            }
        }
    }
}