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

public class MarkerInfo implements XmlInfo {
    private String    mTitle;
    private String    mSnippet;
    private PointInfo mPoint;
    private String    mUUID;

    public MarkerInfo() {
    }

    public String getTitle() {
        return mTitle;
    }

    public void setTitle(String title) {
        mTitle = title;
    }

    public String getSnippet() {
        return mSnippet;
    }

    public void setSnippet(String snippet) {
        mSnippet = snippet;
    }

    public PointInfo getPoint() {
        return mPoint;
    }

    public void setPoint(PointInfo point) {
        mPoint = point;
    }

    public String getUUID() {
        return mUUID;
    }

    public void setUUID(String UUID) {
        mUUID = UUID;
    }

    @Override
    public String toXMl() {
        StringBuilder sb = new StringBuilder();
        sb.append("<marker>");
        if (!TextUtils.isEmpty(mTitle)) {
            sb.append("<title>").append(mTitle).append("</title>");
        }
        if (!TextUtils.isEmpty(mSnippet)) {
            sb.append("<snippet>").append(mSnippet).append("</snippet>");
        }
        sb.append(mPoint.toXMl());
        sb.append("<id>").append(mUUID).append("</id>");
        sb.append("</marker>");
        return sb.toString();
    }
}
