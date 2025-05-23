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

package com.ct.ertclib.dc.core.common;


import com.ct.ertclib.dc.core.data.screenshare.xml.PointBean;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

public class SketchEncoder {
    public static byte[] encode(String encoding, PointBean point) {
        final ArrayList<PointBean> PointBeanS = new ArrayList<>();
        PointBeanS.add(point);
        return encode(encoding, PointBeanS);
    }


    public static byte[] encode(String encoding, List<PointBean> points) {
        if (points == null || points.size() == 0) {
            return null;
        }
        BaseEncoder encoder = EncoderFactory.getEncoder(encoding);
        return encoder.encode(points2ByteArray(points));
    }

    public static List<PointBean> decode(String encoding, byte[] data) {
        if (data == null || data.length == 0) {
            return null;
        }
        BaseEncoder encoder = EncoderFactory.getEncoder(encoding);
        return byteArray2points(encoder.decode(data));
    }


    private static byte[] points2ByteArray(List<PointBean> data) {
        ByteBuffer buffer = ByteBuffer.allocate(data.size() * Double.BYTES * 2);
        buffer.order(ByteOrder.BIG_ENDIAN);
        data.forEach(item -> {
            buffer.put(item.getByteArray());
        });
        return buffer.array();
    }

    private static List<PointBean> byteArray2points(byte[] data) {
        List<PointBean> result = new ArrayList<>();

        ByteBuffer buffer = ByteBuffer.allocate(data.length);
        buffer.order(ByteOrder.BIG_ENDIAN);
        buffer.put(data);
        int count = buffer.position() / 8;
        buffer.rewind();
        for (int i = 0; i < count; i += 2) {
            PointBean point = new PointBean(0.0f, 0.0f);
            point.setX(buffer.getFloat());
            point.setY(buffer.getFloat());
            result.add(point);
        }

        return result;
    }
}
