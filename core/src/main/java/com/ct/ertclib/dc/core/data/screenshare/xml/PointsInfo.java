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

package com.ct.ertclib.dc.core.data.screenshare.xml;


import android.text.TextUtils;

import com.ct.ertclib.dc.core.common.EncoderFactory;
import com.ct.ertclib.dc.core.common.SketchEncoder;

import java.util.List;

public class PointsInfo implements XmlInfo {
    private String mEncoding = EncoderFactory.BASE64;
    private String mEncodedData;
    private List<PointBean> mPointS;

    public PointsInfo() {
    }

    public String getEncoding() {
        return mEncoding;
    }

    public void setEncoding(String encoding) {
        mEncoding = encoding;
    }

    public String getEncodedData() {
        return mEncodedData;
    }

    public void setEncodedData(String encodedData) {
        mEncodedData = encodedData;
        mPointS = SketchEncoder.decode(mEncoding, encodedData.getBytes());
    }

    public List<PointBean> getPointS() {
        return mPointS;
    }

    public void setPointS(List<PointBean> pointS) {
        mPointS = pointS;
    }

    @Override
    public String toXMl() {
        if (TextUtils.isEmpty(mEncodedData) && mPointS != null) {
            mEncodedData = new String(SketchEncoder.encode(mEncoding, mPointS));
        }
        return "<points encoding=\"" + mEncoding + "\">" +
                mEncodedData +
                "</points>";
    }
}
