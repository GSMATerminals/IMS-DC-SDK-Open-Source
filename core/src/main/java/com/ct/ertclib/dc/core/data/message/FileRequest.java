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

package com.ct.ertclib.dc.core.data.message;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class FileRequest extends RequestMessage {

    public final List<FileRequestInfo> fileInfoList = new ArrayList<>();

    public static class FileRequestInfo {
        public String name;
        public String md5;
        public long size;
    }

    public FileRequest() {
        this.request = "file";
    }

    public void addFileRequestInfo(FileRequestInfo info) {
        fileInfoList.add(info);
    }

    public String getJsonString() {
        JSONObject jSONObject = new JSONObject();
        try {
            String uuId = this.uuId;
            if (uuId == null) {
                uuId = "";
            }
            jSONObject.put(IMessage.KEY_ID, uuId);
            jSONObject.put(IMessage.KEY_TIMESTAMP, this.timestamp);
            jSONObject.put(IMessage.KEY_TYPE, this.type);
            jSONObject.put("from", from);
            jSONObject.put("to", to);
            jSONObject.put("data", getDataJson());
        } catch (Exception e) {
            Log.e("FileRequest", e.getMessage(), e);
        }
        return jSONObject.toString();
    }

    private JSONObject getDataJson() {
        JSONObject jSONObject = new JSONObject();
        try {
            jSONObject.put("request", this.request);
            List<FileRequestInfo> list = this.fileInfoList;
            if (list != null && list.size() > 0) {
                JSONArray jSONArray = new JSONArray();
                for (int i = 0; i < this.fileInfoList.size(); i++) {
                    FileRequestInfo requestInfo = this.fileInfoList.get(i);
                    JSONObject requestInfoObject = new JSONObject();
                    String name = requestInfo.name;
                    if (name == null) {
                        name = "";
                    }
                    requestInfoObject.put("name", name);
                    String md5 = requestInfo.md5;
                    if (md5 == null) {
                        md5 = "";
                    }
                    requestInfoObject.put("md5", md5);
                    requestInfoObject.put("size", requestInfo.size);
                    jSONArray.put(requestInfoObject);
                }
                jSONObject.put("list", jSONArray);
            }
        } catch (Exception e) {
            Log.e("FileRequest", e.getMessage(), e);
        }
        return jSONObject;
    }
}
