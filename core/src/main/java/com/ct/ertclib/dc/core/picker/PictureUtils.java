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

package com.ct.ertclib.dc.core.picker;

import android.content.Context;

import com.ct.ertclib.dc.core.data.common.MediaInfo;
import com.ct.ertclib.dc.core.utils.common.FilePickerHelper;
import com.luck.picture.lib.config.SelectMimeType;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * @Description:
 * @Date: 2022/9/21 21:37
 */
public class PictureUtils {

    /**
     * 打开摄像头 拍照
     *
     * @param isRotateImage                   true 前置 false 后置
     * @param onPictureSelectorResultListener 回调
     */
    public static void openCamera(Context context, boolean isPicture,boolean isRotateImage, String dirPath ,OnPictureSelectorResultListener onPictureSelectorResultListener) {
        if (isPicture){
            openCamera(context, isRotateImage, dirPath, onPictureSelectorResultListener);
        } else {
            openVideo(context, isRotateImage, dirPath, onPictureSelectorResultListener);
        }
    }

    /**
     * 打开摄像头 拍照
     *
     * @param isRotateImage                   true 前置 false 后置
     * @param onPictureSelectorResultListener 回调
     */
    private static void openCamera(Context context, boolean isRotateImage, String dirPath ,OnPictureSelectorResultListener onPictureSelectorResultListener) {
        openCamera(context, SelectMimeType.ofImage(), isRotateImage, dirPath,onPictureSelectorResultListener);
    }

    /**
     * 打开摄像头 录制视频
     *
     * @param isRotateImage                   true 前置 false 后置
     * @param onPictureSelectorResultListener 回调
     */
    private static void openVideo(Context context, boolean isRotateImage, String dirPath, OnPictureSelectorResultListener onPictureSelectorResultListener) {
        openCamera(context, SelectMimeType.ofVideo(), isRotateImage, dirPath, onPictureSelectorResultListener);
    }

    /**
     * 打开摄像头
     *
     * @param context                         上下文
     * @param onPictureSelectorResultListener 回调
     */
    private static void openCamera(Context context, int openCamera, boolean isRotateImage, String dirPath,OnPictureSelectorResultListener onPictureSelectorResultListener) {
        FilePickerHelper.INSTANCE.openCamera(context, openCamera, isRotateImage, dirPath, new FilePickerHelper.FilePickCallback() {
            @Override
            public void onFailed() {

            }

            @Override
            public void onSuccess(@NotNull List<MediaInfo> mediaInfoList) {
                if (onPictureSelectorResultListener != null) {
                    onPictureSelectorResultListener.onResult((ArrayList<MediaInfo>) mediaInfoList);
                }
            }

            @Override
            public void onCancel() {

            }
        });
    }


    public interface OnPictureSelectorResultListener {
        void onResult(ArrayList<MediaInfo> result);
    }
}

