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

import com.ct.ertclib.dc.core.utils.logger.Logger
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.Socket
import java.net.SocketAddress

abstract class SocketClientThread(
    ip: String,
    port: Int,
    val dataIn: DataPackList,
    val dataOut: DataPackList
) : Thread(TAG) {
    private val sLogger = Logger.getLogger(TAG)

    companion object {
        val TAG = "SocketNetworkManager" + SocketClientThread::class.java.name
    }

    private val client: Socket = Socket()
    private var socketAddress: SocketAddress = InetSocketAddress(ip, port)
    private var exit = false

    private var socketin: InputStream? = null
    private var dataInput: ByteObjectInputStream? = null
    private var socketout: OutputStream? = null
    private var dataOutput: ByteObjectOutputStream? = null

    @Throws(IOException::class)
    fun connect() {
        sLogger.info("try to connect to server")
        client.connect(socketAddress)
        sLogger.info("success to connect")
    }

    override fun run() {
        try {
            socketin = client.getInputStream()
            dataInput = ByteObjectInputStream(socketin)

            socketout = client.getOutputStream()
            dataOutput = ByteObjectOutputStream(socketout)

            Thread({
                var toSleep = true
                while (!exit && !client.isClosed) {
                    dataOut.getCRTPack()?.let {
                        val tmp = WriteableUtil.buildCtrlPack(it)
                        dataOutput?.let { output ->
                            output.writeObject(tmp)
                            dataOut.getCRTPack(true)
                            if (sLogger.isDebugActivated) sLogger.debug("client has send ctr pack")
                        }
                        toSleep = false
                    }

                    dataOut.getVideoPack()?.let {
                        val tmp = WriteableUtil.buildVideoPack(it)
                        dataOutput?.let { output ->
                            output.writeObject(tmp)
                            dataOut.getVideoPack(true)
                            if (sLogger.isDebugActivated) sLogger.debug("client has send video pack")
                        }
                        toSleep = false
                    }

                    dataOut.getVoicePack()?.let {
                        val tmp = WriteableUtil.buildVoicePack(it)
                        dataOutput?.let { output ->
                            output.writeObject(tmp)
                            dataOut.getVoicePack(true)
                            if (sLogger.isDebugActivated) sLogger.debug("client has send voice pack")
                        }
                        toSleep = false
                    }

                    dataOut.getFilePack()?.let {
                        val tmp = WriteableUtil.buildFilePack(it)
                        val filePack = it as FilePack
                        dataOutput?.let { output ->
                            output.writeObject(tmp)
                            dataOut.getFilePack(true)
                            if (sLogger.isDebugActivated) sLogger.debug("client has send file pack type:${filePack.type} seq:${filePack.seq}")
                        }
                        toSleep = false
                    }

                    dataOut.getSketchPack()?.let {
                        val tmp = WriteableUtil.buildSketchPack(it)
                        dataOutput?.let { output ->
                            output.writeObject(tmp)
                            dataOut.getSketchPack(true)
                            if (sLogger.isDebugActivated) sLogger.debug("client has send sketch pack")
                        }
                        toSleep = false
                    }

                    dataOut.getRawPack()?.let {
                        val tmp = WriteableUtil.buildRawPack(it)
                        dataOutput?.let { output ->
                            output.writeObject(tmp)
                            dataOut.getRawPack(true)
                            if (sLogger.isDebugActivated) sLogger.debug("server has send raw pack")
                        }
                    }

                    if (toSleep) {
                        sleep(100)
                    }
                }
            }, "Thread-SocketClientThread-child").start()

            while (!exit) {
                val bytes = dataInput?.readObject()
                bytes?.let {
                    dataIn.putDataPack(it)
                }
            }
        } catch (e: Throwable) {
            e.printStackTrace()
            onError(e)
        } finally {
            dataInput?.close()
            dataOutput?.close()
            socketin?.close()
            socketout?.close()
            client.close()
        }
        if (sLogger.isDebugActivated) sLogger.debug("SocketClient end")
    }

    fun exit() {
        if (sLogger.isDebugActivated) sLogger.debug("SocketClient stopping")
        exit = true
        dataInput?.close()
        interrupt()
    }

    abstract fun onError(t: Throwable)

}