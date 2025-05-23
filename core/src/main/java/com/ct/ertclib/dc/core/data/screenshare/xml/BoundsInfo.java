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

public class BoundsInfo implements XmlInfo {
    private String       mEncoding = EncoderFactory.BASE64;
    private String       mEncodedBounds;
    private List<PointBean> mPointS;

    public BoundsInfo() {
    }

    public String getEncoding() {
        return mEncoding;
    }

    public void setEncoding(String encoding) {
        mEncoding = encoding;
    }

    public String getEncodedBounds() {
        return mEncodedBounds;
    }

    public void setEncodedBounds(String encodedBounds) {
        mEncodedBounds = encodedBounds;
        mPointS = SketchEncoder.decode(mEncoding, encodedBounds.getBytes());
    }

    public List<PointBean> getPointS() {
        return mPointS;
    }

    public void setPointS(List<PointBean> pointS) {
        mPointS = pointS;
    }

    @Override
    public String toXMl() {
        if (TextUtils.isEmpty(mEncodedBounds) && mPointS != null && mPointS.size() > 0) {
            mEncodedBounds = new String(SketchEncoder.encode(mEncoding, mPointS));
        }
        return "<bounds encoding=\"" + mEncodedBounds + "\">" + mEncodedBounds + "</bounds>";
    }
}
