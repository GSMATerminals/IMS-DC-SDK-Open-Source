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

package com.ct.ertclib.dc.core.utils.common;

import android.annotation.TargetApi;
import android.app.ActivityManager;
import android.content.Context;
import android.os.Build;
import android.text.TextUtils;
import android.webkit.WebView;

import java.io.File;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.util.List;

public class WebViewUtil {

    public static void handleWebViewDir(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            return;
        }
        try {
            String suffix = "";
            String processName = getCurProcessName(context);
            if (!TextUtils.equals(context.getPackageName(), processName)) {//判断不等于默认进程名称
                suffix = TextUtils.isEmpty(processName) ? context.getPackageName() : processName;
                WebView.setDataDirectorySuffix(suffix);
                suffix = "_" + suffix;
            }
            tryLockOrRecreateFile(context, suffix);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @TargetApi(Build.VERSION_CODES.P)
    private static void tryLockOrRecreateFile(Context context, String suffix) {
        String sb = context.getDataDir().getAbsolutePath() +
                "/app_webview" + suffix + "/webview_data.lock";
        File file = new File(sb);
        FileLock tryLock = null;
        RandomAccessFile randomAccessFile = null;
        FileChannel fileChannel = null;
        if (file.exists()) {
            try {
                randomAccessFile = new RandomAccessFile(file, "rw");
                fileChannel = randomAccessFile.getChannel();
                tryLock = fileChannel.tryLock();
                if (tryLock != null) {
                    tryLock.close();
                } else {
                    createFile(file, file.delete());
                }
            } catch (Exception e) {
                e.printStackTrace();
                boolean deleted = false;
                if (file.exists()) {
                    deleted = file.delete();
                }
                createFile(file, deleted);
            } finally {
                try {
                    if (randomAccessFile!=null){
                        randomAccessFile.close();
                    }
                    if (fileChannel!=null){
                        fileChannel.close();
                    }
                    if (tryLock!=null){
                        tryLock.close();
                    }
                } catch (Exception e){
                    e.printStackTrace();
                }
            }
        }
    }

    private static void createFile(File file, boolean deleted) {
        try {
            if (deleted && !file.exists()) {
                file.createNewFile();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String getCurProcessName(Context context) {
        int pid = android.os.Process.myPid();
        ActivityManager activityManager = (ActivityManager) context
                .getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningAppProcessInfo> appProcesses = activityManager
                .getRunningAppProcesses();
        if (appProcesses == null) {
            return null;
        }
        for (ActivityManager.RunningAppProcessInfo appProcess : appProcesses) {
            if (appProcess == null) {
                continue;
            }
            if (appProcess.pid == pid) {
                return appProcess.processName;
            }
        }
        return null;
    }

}