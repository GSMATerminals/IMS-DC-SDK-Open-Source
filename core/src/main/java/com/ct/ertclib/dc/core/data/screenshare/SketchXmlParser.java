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

import android.text.TextUtils;


import com.ct.ertclib.dc.core.data.screenshare.xml.ActionDocument;
import com.ct.ertclib.dc.core.data.screenshare.xml.BoundsInfo;
import com.ct.ertclib.dc.core.data.screenshare.xml.DrawingInfo;
import com.ct.ertclib.dc.core.data.screenshare.xml.MarkerInfo;
import com.ct.ertclib.dc.core.data.screenshare.xml.PointInfo;
import com.ct.ertclib.dc.core.data.screenshare.xml.PointsInfo;
import com.ct.ertclib.dc.core.data.screenshare.xml.RemoveInfo;
import com.ct.ertclib.dc.core.data.screenshare.xml.UserInfo;
import com.ct.ertclib.dc.core.utils.logger.Logger;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.Charset;
import java.util.Stack;


/*
<?xml version='1.0' encoding='UTF-8'?>
<actions>
    <marker>
        <point encoding="Base64">QEvzRyLBk0lAKWWLnsAAAA==</point>
        <id>80ec6d2f-840a-7bcb-1560-73f2c048186a</id>
    </marker>
</actions>

<?xml version='1.0' encoding='UTF-8' ?>
<actions>
    <drawing color="FFFF00FF" width="0.006">
        <points encoding="Base64">QEvdCvCXCWZAKmRCbAAAAEBL3QrwlwlmQCpkRESAAABAS90KrhG8cEAqZEU8AAAAQEvdCos5n/RAKmRFPAAAAEBL3QpItFGmQCpkRihAAABAS90KBJmMvUAqZEYoQAAA</points>
    </drawing>
</actions>

<?xml version='1.0' encoding='UTF-8' ?>
<actions>
    <remove>
        <id>58818160-8b60-4ab9-8ae5-78feaed16343</id>
    </remove>
</actions>
 */

public class SketchXmlParser {
    private static final String TAG = "SketchXmlParser";
    private static final Logger sLogger = Logger.getLogger(TAG);

    private static final String UTF8 = "UTF8";
    private static final String ELEMENT_VERSION = "version";
    private static final String ELEMENT_ACTIONS = "actions";
    private static final String ELEMENT_DRAWING = "drawing";
    private static final String ELEMENT_POINT = "point";
    private static final String ELEMENT_POINTS = "points";
    private static final String ELEMENT_BACKGROUND_COLOR = "background_color";
    private static final String ELEMENT_IMAGE = "image";
    private static final String ELEMENT_USER = "user";
    private static final String ELEMENT_BOUNDS = "bounds";
    private static final String ELEMENT_MARKER = "marker";
    private static final String ELEMENT_REMOVE = "remove";
    private static final String ELEMENT_ID = "id";
    private static final String ELEMENT_TITLE = "title";
    private static final String ELEMENT_SNIPPET = "snippet";
    private static final String ELEMENT_CLOSE = "close";
    private static final String ELEMENT_UNDO = "undo";
    private static final String ATTRIBUTE_ID = "id";
    private static final String ATTRIBUTE_SEQ = "seq";
    private static final String ATTRIBUTE_ENCODING = "encoding";
    private static final String ATTRIBUTE_COLOR = "color";
    private static final String ATTRIBUTE_WIDTH = "width";
    private static final String ATTRIBUTE_ERASE = "erase";

    // private final RcsSettings mRcsSettings;
    private final String mXmlSource;

    private ActionDocument mActionDocument;

    // public SketchXmlParser(byte[] xml, RcsSettings rcsSettings) {
    //     mRcsSettings = rcsSettings;
    //     mXmlSource = new String(xml, Charset.forName(UTF8));
    // }

    public SketchXmlParser(byte[] xml) {
        mXmlSource = new String(xml, Charset.forName(UTF8));
        if (sLogger.isDebugActivated()) sLogger.debug(mXmlSource);
    }

    public SketchXmlParser parse() throws SketchXmlParseException {
        try {
            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            factory.setNamespaceAware(false);
            XmlPullParser xpp = factory.newPullParser();
            xpp.setInput(new StringReader(mXmlSource));
            int eventType = xpp.getEventType();
            String text = null;
            while (eventType != XmlPullParser.END_DOCUMENT) {
                String tagName = xpp.getName();
                switch (eventType) {
                    case XmlPullParser.START_TAG:
                        parserStartTag(xpp, tagName);
                        break;
                    case XmlPullParser.TEXT:
                        text = xpp.getText().trim();
                        break;
                    case XmlPullParser.END_TAG:
                        parserEndTag(text, tagName);
                        break;
                    default:
                        break;
                }
                eventType = xpp.next();
            }
            return this;

        } catch (XmlPullParserException | IOException e) {
            throw new SketchXmlParseException("Failed to parse input source!", e);
        } catch (Exception e) {
            throw new SketchXmlParseException("Other parse Exception", e);
        }
    }

    private final Stack<String> mElementStack = new Stack<>();

    private void parserStartTag(XmlPullParser xpp, String tagName) {
        String preTag = "";
        if (mElementStack.size() > 0) {
            preTag = mElementStack.peek();
        }
        if (ELEMENT_ACTIONS.equalsIgnoreCase(tagName)) {
            mActionDocument = new ActionDocument();
            String seq = xpp.getAttributeValue(null, ATTRIBUTE_SEQ);
            if (!TextUtils.isEmpty(seq)) {
                mActionDocument.setSeq(Integer.parseInt(seq));
            }
        } else if (ELEMENT_VERSION.equalsIgnoreCase(tagName)) {
            String id = xpp.getAttributeValue(null, ATTRIBUTE_ID);
            if (!TextUtils.isEmpty(id)) {
                mActionDocument.setVersion(Integer.parseInt(id));
            }
        } else if (ELEMENT_DRAWING.equalsIgnoreCase(tagName)) {
            DrawingInfo drawingInfo = new DrawingInfo();
            String color = xpp.getAttributeValue(null, ATTRIBUTE_COLOR);
            String width = xpp.getAttributeValue(null, ATTRIBUTE_WIDTH);
            String erase = xpp.getAttributeValue(null, ATTRIBUTE_ERASE);
            if (!TextUtils.isEmpty(color)) {
                drawingInfo.setColor(color);
            }
            if (!TextUtils.isEmpty(width)) {
                drawingInfo.setWidth(Float.valueOf(width));
            }
            if (!TextUtils.isEmpty(erase)) {
                drawingInfo.setErase(Boolean.valueOf(erase));
            }
            mActionDocument.setDrawingInfo(drawingInfo);
        } else if (ELEMENT_POINTS.equalsIgnoreCase(tagName)) {
            PointsInfo points = new PointsInfo();
            String encoding = xpp.getAttributeValue(null, ATTRIBUTE_ENCODING);
            if (!TextUtils.isEmpty(encoding)) {
                points.setEncoding(encoding);
            }
            if (ELEMENT_DRAWING.equalsIgnoreCase(preTag)) {
                DrawingInfo drawingInfo = mActionDocument.getDrawingInfo();
                drawingInfo.setPoints(points);
            }
        } else if (ELEMENT_BACKGROUND_COLOR.equalsIgnoreCase(tagName)) {
            String color = xpp.getAttributeValue(null, ATTRIBUTE_COLOR);
            if (!TextUtils.isEmpty(color)) {
                mActionDocument.setBackgroundColor(color);
            }
        } else if (ELEMENT_IMAGE.equalsIgnoreCase(tagName)) {
            String encoding = xpp.getAttributeValue(null, ATTRIBUTE_ENCODING);
            if (!TextUtils.isEmpty(encoding)) {
                mActionDocument.setBackgroundImageEncoding(encoding);
            }
        } else if (ELEMENT_BOUNDS.equalsIgnoreCase(tagName)) {
            String encoding = xpp.getAttributeValue(null, ATTRIBUTE_ENCODING);
            BoundsInfo boundsInfo = new BoundsInfo();
            if (!TextUtils.isEmpty(encoding)) {
                boundsInfo.setEncoding(encoding);
            }
            mActionDocument.setBoundsInfo(boundsInfo);
        } else if (ELEMENT_UNDO.equalsIgnoreCase(tagName)) {
            int undoTimes = mActionDocument.getUndoTimes() + 1;
            mActionDocument.setUndoTimes(undoTimes);
        } else if (ELEMENT_MARKER.equalsIgnoreCase(tagName)) {
            MarkerInfo markerinfo = new MarkerInfo();
            mActionDocument.setMarkerInfo(markerinfo);
        } else if (ELEMENT_POINT.equalsIgnoreCase(tagName)) {
            PointInfo pointInfo = new PointInfo();
            String encoding = xpp.getAttributeValue(null, ATTRIBUTE_ENCODING);
            if (!TextUtils.isEmpty(encoding)) {
                pointInfo.setEncoding(encoding);
            }
            if (ELEMENT_MARKER.equalsIgnoreCase(preTag)) {
                MarkerInfo markerInfo = mActionDocument.getMarkerInfo();
                markerInfo.setPoint(pointInfo);
            } else if (ELEMENT_USER.equalsIgnoreCase(preTag)) {
                UserInfo userInfo = mActionDocument.getUserInfo();
                userInfo.setPointInfo(pointInfo);
            }
        } else if (ELEMENT_USER.equalsIgnoreCase(tagName)) {
            UserInfo userinfo = new UserInfo();
            mActionDocument.setUserInfo(userinfo);
        } else if (ELEMENT_REMOVE.equalsIgnoreCase(tagName)) {
            RemoveInfo removeinfo = new RemoveInfo();
            mActionDocument.setRemoveInfo(removeinfo);
        }
        mElementStack.push(tagName);
    }

    private void parserEndTag(String text, String tagName) throws SketchXmlParseException {
        if (mActionDocument == null) {
            return;
        }
        if (text == null) {
            throw new SketchXmlParseException("Bad HTTP file transfer information " + mXmlSource);
        }
        String preTag = "";
        mElementStack.pop();
        if (mElementStack.size() > 0) {
            preTag = mElementStack.peek();
        }
        if (ELEMENT_CLOSE.equalsIgnoreCase(tagName)) {
            mActionDocument.setCloseSession(true);
        } else if (ELEMENT_POINTS.equalsIgnoreCase(tagName)) {
            if (ELEMENT_DRAWING.equalsIgnoreCase(preTag)) {
                DrawingInfo drawingInfo = mActionDocument.getDrawingInfo();
                PointsInfo points = drawingInfo.getPoints();
                points.setEncodedData(text);
            }
        } else if (ELEMENT_IMAGE.equalsIgnoreCase(tagName)) {
            mActionDocument.setBackgroundImage(text);
        } else if (ELEMENT_BOUNDS.equalsIgnoreCase(tagName)) {
            BoundsInfo boundsInfo = mActionDocument.getBoundsInfo();
            boundsInfo.setEncodedBounds(text);
        }
        // pretag
        else if (ELEMENT_MARKER.equalsIgnoreCase(preTag)) {
            if (ELEMENT_ID.equalsIgnoreCase(tagName)) {
                MarkerInfo markerInfo = mActionDocument.getMarkerInfo();
                markerInfo.setUUID(text);
            } else if (ELEMENT_TITLE.equalsIgnoreCase(tagName)) {
                MarkerInfo markerInfo = mActionDocument.getMarkerInfo();
                markerInfo.setTitle(text);
            } else if (ELEMENT_SNIPPET.equalsIgnoreCase(tagName)) {
                MarkerInfo markerInfo = mActionDocument.getMarkerInfo();
                markerInfo.setSnippet(text);
            } else if (ELEMENT_POINT.equalsIgnoreCase(tagName)) {
                MarkerInfo markerInfo = mActionDocument.getMarkerInfo();
                PointInfo point = markerInfo.getPoint();
                point.setEncodedData(text);
            }
        } else if (ELEMENT_USER.equalsIgnoreCase(preTag)
                && ELEMENT_POINT.equalsIgnoreCase(tagName)) {
            UserInfo userInfo = mActionDocument.getUserInfo();
            PointInfo pointInfo = userInfo.getPointInfo();
            pointInfo.setEncodedData(text);
        } else if (ELEMENT_REMOVE.equalsIgnoreCase(preTag)
                && ELEMENT_ID.equalsIgnoreCase(tagName)) {
            RemoveInfo removeInfo = mActionDocument.getRemoveInfo();
            removeInfo.setUUID(text);
        }
    }

    public ActionDocument getActionDocument() {
        return mActionDocument;
    }
}
