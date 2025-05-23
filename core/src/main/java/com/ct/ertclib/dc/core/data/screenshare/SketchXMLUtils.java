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

package com.ct.ertclib.dc.core.data.screenshare;

import android.graphics.Bitmap;


import com.ct.ertclib.dc.core.common.EncoderFactory;
import com.ct.ertclib.dc.core.data.screenshare.xml.PointBean;
import com.ct.ertclib.dc.core.data.screenshare.xml.BoundsInfo;
import com.ct.ertclib.dc.core.data.screenshare.xml.DrawingInfo;
import com.ct.ertclib.dc.core.data.screenshare.xml.MarkerInfo;
import com.ct.ertclib.dc.core.data.screenshare.xml.PointInfo;
import com.ct.ertclib.dc.core.data.screenshare.xml.PointsInfo;
import com.ct.ertclib.dc.core.data.screenshare.xml.RemoveInfo;
import com.ct.ertclib.dc.core.data.screenshare.xml.UserInfo;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class SketchXMLUtils {
    public static String createDrawXml(List<PointBean> points, String encoding, String color, float width, boolean erase) {
        DrawingInfo drawingInfo = new DrawingInfo();
        PointsInfo pointsInfo = new PointsInfo();
        pointsInfo.setEncoding(encoding);
        pointsInfo.setPointS(points);
        drawingInfo.setPoints(pointsInfo);
        drawingInfo.setColor(color);
        drawingInfo.setWidth(width);
        drawingInfo.setErase(erase);
        return wrapActionXml(drawingInfo.toXMl());
    }

    public static String createUndoXml(int times) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < times; i++) {
            sb.append("<undo/>");
        }
        return wrapActionXml(sb.toString());
    }

    public static String createEndSessionXml() {
        return wrapActionXml("<close/>");
    }

    public static String createLocationXml(PointBean point) {
        UserInfo userInfo = new UserInfo();
        PointInfo pointInfo = new PointInfo();
        pointInfo.setPointBean(point);
        userInfo.setPointInfo(pointInfo);
        return wrapActionXml(userInfo.toXMl());
    }

    public static String createBoundsXml(PointBean northeast, PointBean southwest, String encoding) {
        BoundsInfo boundsInfo = new BoundsInfo();
        boundsInfo.setEncodedBounds(encoding);
        ArrayList<PointBean> pointBeans = new ArrayList<>();
        pointBeans.add(northeast);
        pointBeans.add(southwest);
        boundsInfo.setPointS(pointBeans);
        return wrapActionXml(boundsInfo.toXMl());
    }

    public static String createMarkerXml(String title, String snippet, PointBean point, String id) {
        MarkerInfo markerInfo = new MarkerInfo();
        markerInfo.setTitle(title);
        markerInfo.setSnippet(snippet);
        PointInfo pointInfo = new PointInfo();
        pointInfo.setPointBean(point);
        markerInfo.setPoint(pointInfo);
        markerInfo.setUUID(id);
        return wrapActionXml(markerInfo.toXMl());
    }

    public static String createRemoveMarkerXml(String id) {
        RemoveInfo removeInfo = new RemoveInfo();
        removeInfo.setUUID(id);
        return wrapActionXml(removeInfo.toXMl());
    }

    public static String wrapActionXml(String xml) {
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>")
                .append("<actions>")
                .append(xml)
                .append("</actions>");
        return sb.toString();
    }

    public static String createBackgroundColorXml(String color) {
        return "<background_color color=" + color + "/>";
    }

    public static String createBackgroundImageXml(Bitmap bitmap, String encoding) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
        byte[] imageBytes = outputStream.toByteArray();
        byte[] encode = EncoderFactory.getEncoder(encoding).encode(imageBytes);
        return "<image encoding=" + encoding + "/>" + new String(encode, StandardCharsets.UTF_8) + "</image>";
    }

    public static String createBackgroundImageXml(byte[] imageBytes, String encoding) {
        byte[] encode = EncoderFactory.getEncoder(encoding).encode(imageBytes);
        return "<image encoding=" + encoding + "/>" + new String(encode, StandardCharsets.UTF_8) + "</image>";
    }


}
