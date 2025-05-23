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


public class ActionDocument {

    private int version = 1;
    private int seq     = 0;

    private DrawingInfo mDrawingInfo;
    private Integer     mUndoTimes;
    private Boolean     mCloseSession;

    // map only start
    private BoundsInfo mBoundsInfo;
    private MarkerInfo mMarkerInfo;
    private RemoveInfo mRemoveInfo;
    private UserInfo   mUserInfo;
    // map only end

    // sketch only start
    private String mBackgroundColor;
    private String mBackgroundImageEncoding;
    private String mBackgroundImage;
    // sketch only end


    public ActionDocument() {
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public int getSeq() {
        return seq;
    }

    public void setSeq(int seq) {
        this.seq = seq;
    }

    public DrawingInfo getDrawingInfo() {
        return mDrawingInfo;
    }

    public void setDrawingInfo(DrawingInfo drawingInfo) {
        mDrawingInfo = drawingInfo;
    }

    public int getUndoTimes() {
        return mUndoTimes;
    }

    public void addUndoTimes(int undoTimes) {
        this.mUndoTimes = undoTimes;
    }

    public boolean isCloseSession() {
        return mCloseSession;
    }

    public void setCloseSession(boolean closeSession) {
        mCloseSession = closeSession;
    }

    public BoundsInfo getBoundsInfo() {
        return mBoundsInfo;
    }

    public void setBoundsInfo(BoundsInfo boundsInfo) {
        mBoundsInfo = boundsInfo;
    }

    public MarkerInfo getMarkerInfo() {
        return mMarkerInfo;
    }

    public void setMarkerInfo(MarkerInfo markerInfo) {
        mMarkerInfo = markerInfo;
    }

    public RemoveInfo getRemoveInfo() {
        return mRemoveInfo;
    }

    public void setRemoveInfo(RemoveInfo removeInfo) {
        mRemoveInfo = removeInfo;
    }

    public UserInfo getUserInfo() {
        return mUserInfo;
    }

    public void setUserInfo(UserInfo userInfo) {
        mUserInfo = userInfo;
    }

    public String getBackgroundColor() {
        return mBackgroundColor;
    }

    public void setBackgroundColor(String backgroundColor) {
        mBackgroundColor = backgroundColor;
    }

    public void setUndoTimes(int undoTimes) {
        mUndoTimes = undoTimes;
    }

    public String getBackgroundImageEncoding() {
        return mBackgroundImageEncoding;
    }

    public void setBackgroundImageEncoding(String backgroundImageEncoding) {
        mBackgroundImageEncoding = backgroundImageEncoding;
    }

    public String getBackgroundImage() {
        return mBackgroundImage;
    }

    public void setBackgroundImage(String backgroundImage) {
        mBackgroundImage = backgroundImage;
    }

    @Override
    public String toString() {
        return "ActionDocument{" +
                "version=" + version +
                ", seq=" + seq +
                ", mDrawingInfo=" + mDrawingInfo +
                ", mUndoTimes=" + mUndoTimes +
                ", mCloseSession=" + mCloseSession +
                ", mBoundsInfo=" + mBoundsInfo +
                ", mMarkerInfo=" + mMarkerInfo +
                ", mRemoveInfo=" + mRemoveInfo +
                ", mUserInfo=" + mUserInfo +
                ", mBackgroundColor='" + mBackgroundColor + '\'' +
                ", mBackgroundImageEncoding='" + mBackgroundImageEncoding + '\'' +
                ", mBackgroundImage='" + mBackgroundImage + '\'' +
                '}';
    }
}
