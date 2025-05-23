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

package com.ct.ertclib.dc.feature.testing.socket;


import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class ByteObjectOutputStream {
    byte breaking;
    OutputStream outputStream;
    BufferedOutputStream bufferedOutputStream;

    public ByteObjectOutputStream(OutputStream o) {
        this(o, (byte) 0x8f);
    }

    public ByteObjectOutputStream(OutputStream outputStream, byte breaking) {
        this.breaking = breaking;
        this.outputStream = outputStream;
        bufferedOutputStream = new BufferedOutputStream(outputStream);
    }

    public void writeObject(byte[] bytes) throws IOException {
        writeObject(bytes, 0, bytes.length);
    }

    public void writeObject(byte[] bytes, int offset, int len) throws IOException {
        byte[] crctmp = new byte[len];
        System.arraycopy(bytes, offset, crctmp, 0, len);

        int crcCode = CRC.getIntCRC(crctmp);
        crctmp = new byte[len + 4];
        System.arraycopy(CombineValue.intToByte(crcCode), 0, crctmp, 0, 4);
        System.arraycopy(bytes, offset, crctmp, 4, len);
        for (int i = 0; i < len + 4; i++) {
            if (crctmp[i] == breaking) {
                bufferedOutputStream.write(breaking);
                bufferedOutputStream.write(breaking);
            } else {
                bufferedOutputStream.write(crctmp[i]);
            }
        }
        bufferedOutputStream.write(breaking);
        bufferedOutputStream.write(0);
        bufferedOutputStream.flush();
    }

    public void close() throws IOException {
        if (bufferedOutputStream != null) {
            bufferedOutputStream.close();
            bufferedOutputStream = null;
            outputStream = null;
        }
    }
}
