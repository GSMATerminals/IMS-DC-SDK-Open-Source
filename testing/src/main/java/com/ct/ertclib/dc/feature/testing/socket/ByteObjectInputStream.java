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


import android.util.Log;


import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class ByteObjectInputStream {
    byte breaking;
    InputStream inputStream;
    BufferedInputStream bufferedInputStream;

    public ByteObjectInputStream(InputStream i) {
        this(i, (byte) 0x8f);
    }

    public ByteObjectInputStream(InputStream i, byte breaking) {
        this.inputStream = i;
        this.breaking = breaking;
        bufferedInputStream = new BufferedInputStream(inputStream);
    }

    public byte[] readObject() throws IOException {

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

        while (true) {
            byte one = (byte) bufferedInputStream.read();
            if (one == breaking) {
                byte two = (byte) bufferedInputStream.read();
                if (two == breaking) {
                    byteArrayOutputStream.write(breaking);
                } else if (two == 0) {
                    return checkCrc(byteArrayOutputStream.toByteArray());
                } else {
                    Log.d("ByteObjectInputStream", "Other Data" + two);
                }

            } else {
                byteArrayOutputStream.write(one);
            }
        }


    }

    private byte[] checkCrc(byte[] bytes) throws IOException {
        int crcCode = CombineValue.bytesToInt(new byte[]{bytes[0], bytes[1], bytes[2], bytes[3]});
        byte[] tmp = new byte[bytes.length - 4];
        System.arraycopy(bytes, 4, tmp, 0, bytes.length - 4);
        if (CRC.getIntCRC(tmp) == crcCode) {
            return tmp;
        } else {
            throw new IOException("Crc Code Error");
        }
    }

    public void close() throws IOException {
        if (bufferedInputStream != null) {
            bufferedInputStream.close();
            bufferedInputStream = null;
            inputStream = null;
        }
    }

}
