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

public class DrawingInfo implements XmlInfo {
    private Float      mWidth = 0.006f;
    private String     mColor = "#FFFFFFFF";
    private Boolean    mErase;
    private PointsInfo mPoints;

    public DrawingInfo() {
    }

    public Float getWidth() {
        return mWidth;
    }

    public void setWidth(Float width) {
        mWidth = width;
    }

    public String getColor() {
        return mColor;
    }

    public void setColor(String color) {
        mColor = color;
    }

    public Boolean getErase() {
        return mErase;
    }

    public void setErase(Boolean erase) {
        mErase = erase;
    }

    public PointsInfo getPoints() {
        return mPoints;
    }

    public void setPoints(PointsInfo points) {
        mPoints = points;
    }

    @Override
    public String toXMl() {
        StringBuilder sb = new StringBuilder();
        sb.append("<drawing");
        if (mWidth != null) {
            sb.append(" width=\"").append(mWidth).append("\"");
        }
        if (mColor != null) {
            sb.append(" color=\"").append(mColor).append("\"");
        }
        if (mErase != null) {
            sb.append(" erase=\"").append(mErase).append("\"");
        }
        sb.append(">");
        sb.append(mPoints.toXMl());
        sb.append("</drawing>");
        return sb.toString();
    }
}
