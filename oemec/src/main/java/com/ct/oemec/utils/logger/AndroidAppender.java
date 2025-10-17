/*
 *   Copyright 2025-China Telecom Research Institute.
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *        https://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package com.ct.oemec.utils.logger;

import android.util.Log;

import com.ct.oemec.utils.logger.LogConfig;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * Android appender
 *
 * @author jexa7410
 */
public class AndroidAppender extends Appender {

    /**
     * Constructor
     */
    public AndroidAppender() {
        super();
    }

    /**
     * Print a trace
     *
     * @param classname Classname
     * @param level Trace level
     * @param trace Trace
     */
    public synchronized void printTrace(String classname, int level, String trace) {
        classname = "[SDKLog][OEM][" + classname + "]";

        if (!LogConfig.INSTANCE.isLogEnabled()){
            return;
        }

        if (level == Logger.INFO_LEVEL) {
            Log.i(classname, trace);
        } else if (level == Logger.WARN_LEVEL) {
            Log.w(classname, trace);
        } else if (level == Logger.ERROR_LEVEL) {
            Log.e(classname, trace);
        } else if (level == Logger.FATAL_LEVEL) {
            Log.e(classname, trace);
        } else {
            largeLog(classname, trace);
        }
    }

    private static void largeLog(String tag, String content) {
        int maxLength = 4000;
        int length = content.length();

        for (int i = 0; i < length; i += maxLength) {
            int end = Math.min(length, i + maxLength);
            String part = content.substring(i, end);
            Log.v(tag, part);
        }
    }
}
