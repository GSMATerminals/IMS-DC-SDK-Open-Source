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

public class FileResponse extends ResponseMessage {

    public final List<FileResponseInfo> responseInfoList = new ArrayList();

    public boolean accept;
    public FileResponse(int code) {
        super(code);
        this.response = "file";
    }

    public static class FileResponseInfo {

        public String name;

        public long start;

        public long end;
    }

    public void addResponseInfo(FileResponseInfo info) {
        this.responseInfoList.add(info);
    }

    public String parseJsonString() {
        JSONObject jsonObject = new JSONObject();

        String uuId = this.uuId;
        if (uuId == null) {
            uuId = "";
        }
        try {
            jsonObject.put(IMessage.KEY_ID, uuId);
            jsonObject.put(IMessage.KEY_TIMESTAMP, timestamp);
            jsonObject.put(IMessage.KEY_TYPE, type);
            jsonObject.put("code", code);
            jsonObject.put("data", getDataJson());

        } catch (Exception e) {
            Log.e("FileResponse", e.getMessage(), e);
        }

        return jsonObject.toString();
    }

    private JSONObject getDataJson() {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("response", response);
            jsonObject.put("accept", accept);
            JSONArray jsonArray = new JSONArray();
            for (int i=0; i<responseInfoList.size(); i++) {
                FileResponseInfo info = responseInfoList.get(i);
                JSONObject infoJson = new JSONObject();
                String name = info.name;
                if (name == null) {
                    name = "";
                }
                infoJson.put("name", name);
                infoJson.put("start",info.start);
                infoJson.put("end", info.end);
                jsonArray.put(infoJson);
            }
            jsonObject.put("list", jsonArray);

        } catch (Exception e) {
            Log.e("FileResponse", e.getMessage(), e);
        }
        return jsonObject;
    }
}
