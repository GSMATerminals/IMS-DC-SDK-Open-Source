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

package com.ct.ertclib.dc.core.picker;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.luck.lib.camerax.SimpleCameraX;
import com.luck.picture.lib.basic.PictureSelector;
import com.luck.picture.lib.config.PictureMimeType;
import com.luck.picture.lib.config.SelectMimeType;
import com.luck.picture.lib.engine.CompressFileEngine;
import com.luck.picture.lib.engine.CropFileEngine;
import com.luck.picture.lib.engine.VideoPlayerEngine;
import com.luck.picture.lib.entity.LocalMedia;
import com.luck.picture.lib.entity.MediaExtraInfo;
import com.luck.picture.lib.interfaces.OnCameraInterceptListener;
import com.luck.picture.lib.interfaces.OnExternalPreviewEventListener;
import com.luck.picture.lib.interfaces.OnRecordAudioInterceptListener;
import com.luck.picture.lib.interfaces.OnResultCallbackListener;
import com.luck.picture.lib.permissions.PermissionChecker;
import com.luck.picture.lib.permissions.PermissionResultCallback;
import com.luck.picture.lib.utils.MediaUtils;
import com.luck.picture.lib.utils.ToastUtils;

import java.io.File;
import java.util.ArrayList;

/**
 * @author: zzw
 * @Description:
 * @Date: 2022/9/21 21:37
 */
public class PictureUtils {

    /**
     * 打开摄像头 拍照
     *
     * @param context
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
     * @param context
     * @param isRotateImage                   true 前置 false 后置
     * @param onPictureSelectorResultListener 回调
     */
    private static void openCamera(Context context, boolean isRotateImage, String dirPath ,OnPictureSelectorResultListener onPictureSelectorResultListener) {
        openCamera(context, SelectMimeType.ofImage(), isRotateImage, dirPath,onPictureSelectorResultListener);
    }

    /**
     * 打开摄像头 录制视频
     *
     * @param context
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
        PictureSelector.create(context)
                .openCamera(openCamera)
                .isCameraAroundState(isRotateImage)
                .setOutputCameraDir(dirPath)
                .setVideoThumbnailListener(new VideoThumbListener(context))
                .forResult(new OnResultCallbackListener<>() {
                    @Override
                    public void onResult(ArrayList<LocalMedia> result) {
                        onPictureSelectorResultListener.onResult(result);
                    }

                    @Override
                    public void onCancel() {
                    }
                });
    }

    /**
     * 设置头像
     *
     * @param mContext
     * @param selectResult                    结果
     * @param onPictureSelectorResultListener 结果回调
     */
    public static void createAvatar(Context mContext, ArrayList<LocalMedia> selectResult, OnPictureSelectorResultListener onPictureSelectorResultListener) {
        create(mContext, SelectMimeType.ofImage(), selectResult, 1, 1, true, onPictureSelectorResultListener);
    }

    /**
     * 选择单张图片
     *
     * @param mContext
     * @param selectResult                    结果
     * @param onPictureSelectorResultListener 结果回调
     */
    public static void createImageMin(Context mContext, ArrayList<LocalMedia> selectResult, OnPictureSelectorResultListener onPictureSelectorResultListener) {
        create(mContext, SelectMimeType.ofImage(), selectResult, 1, 1, false, onPictureSelectorResultListener);
    }


    /**
     * 选择多张图片
     *
     * @param mContext
     * @param selectResult                    结果
     * @param selectMax                       最多选择
     * @param onPictureSelectorResultListener 结果回调
     */
    public static void createImageMax(Context mContext, int selectMax, ArrayList<LocalMedia> selectResult, OnPictureSelectorResultListener onPictureSelectorResultListener) {
        create(mContext, SelectMimeType.ofImage(), selectResult, 1, selectMax, false, onPictureSelectorResultListener);
    }

    /**
     * 选择单个视频
     *
     * @param mContext
     * @param selectResult                    结果
     * @param onPictureSelectorResultListener 结果回调
     */
    public static void createVideo(Context mContext, ArrayList<LocalMedia> selectResult, OnPictureSelectorResultListener onPictureSelectorResultListener) {
        create(mContext, SelectMimeType.ofVideo(), selectResult, 1, 1, false, onPictureSelectorResultListener);
    }

    /**
     * 选择单个音频
     *
     * @param mContext
     * @param selectResult                    结果
     * @param onPictureSelectorResultListener 结果回调
     */
    public static void createAudio(Context mContext, ArrayList<LocalMedia> selectResult, OnPictureSelectorResultListener onPictureSelectorResultListener) {
        create(mContext, SelectMimeType.ofAudio(), selectResult, 1, 1, false, onPictureSelectorResultListener);
    }

    /**
     * 选择 媒体 图片 视频 音频
     *
     * @param mContext
     * @param selectResult                    结果
     * @param onPictureSelectorResultListener 结果回调
     */
    public static void createPicture(Context mContext, ArrayList<LocalMedia> selectResult, OnPictureSelectorResultListener onPictureSelectorResultListener) {
        create(mContext, SelectMimeType.ofAll(), selectResult, 1, 1, false, onPictureSelectorResultListener);
    }

    /**
     * 默认设置
     *
     * @param mContext
     * @param selectMimeType                  结果
     * @param selectResult                    结果
     * @param selectMin                       最少选择
     * @param selectMax                       最多选择
     * @param isCrop                          是否剪裁
     * @param onPictureSelectorResultListener 结果回到
     */
    public static void create(Context mContext, int selectMimeType, ArrayList<LocalMedia> selectResult, int selectMin, int selectMax, boolean isCrop,
                              OnPictureSelectorResultListener onPictureSelectorResultListener) {
        PictureSelector.create(mContext)
                .openGallery(selectMimeType)
                .setMaxSelectNum(selectMax)
                .setMinSelectNum(selectMin)
                .setFilterVideoMaxSecond(selectMimeType == SelectMimeType.ofVideo() ? 60 : 60 * 10)
                .setFilterVideoMinSecond(5)
                .setRecordVideoMaxSecond(selectMimeType == SelectMimeType.ofVideo() ? 60 : 60 * 10)
                .setRecordVideoMinSecond(5)
                .setFilterMaxFileSize(100 * 1024 * 1024)
                .setCameraInterceptListener(new MeOnCameraInterceptListener())
                .isFilterSizeDuration(true)
                .setSelectedData(selectResult)
                .setImageEngine(GlideEngine.INSTANCE)
                .setRecordAudioInterceptListener(new MeOnRecordAudioInterceptListener())
                .forResult(new OnResultCallbackListener<LocalMedia>() {
                    @Override
                    public void onResult(ArrayList<LocalMedia> result) {
                        for (LocalMedia media : result) {
                            if (media.getWidth() == 0 || media.getHeight() == 0) {
                                if (PictureMimeType.isHasImage(media.getMimeType())) {
                                    MediaExtraInfo imageExtraInfo = MediaUtils.getImageSize(mContext, media.getPath());
                                    media.setWidth(imageExtraInfo.getWidth());
                                    media.setHeight(imageExtraInfo.getHeight());
                                } else if (PictureMimeType.isHasVideo(media.getMimeType())) {
                                    MediaExtraInfo videoExtraInfo = MediaUtils.getVideoSize(mContext, media.getPath());
                                    media.setWidth(videoExtraInfo.getWidth());
                                    media.setHeight(videoExtraInfo.getHeight());
                                }
                            }
//                            LogUtils.e("文件名: " + media.getFileName() + "\n" +
//                                    "是否压缩:" + media.isCompressed() + "\n" +
//                                    "压缩路径:" + media.getCompressPath() + "\n" +
//                                    "初始路径:" + media.getPath() + "\n" +
//                                    "绝对路径:" + media.getRealPath() + "\n" +
//                                    "是否裁剪:" + media.isCut() + "\n" +
//                                    "裁剪路径:" + media.getCutPath() + "\n" +
//                                    "是否开启原图:" + media.isOriginal() + "\n" +
//                                    "原图路径:" + media.getOriginalPath() + "\n" +
//                                    "沙盒路径:" + media.getSandboxPath() + "\n" +
//                                    "水印路径:" + media.getWatermarkPath() + "\n" +
//                                    "视频缩略图:" + media.getVideoThumbnailPath() + "\n" +
//                                    "原始宽高: " + media.getWidth() + "x" + media.getHeight() + "\n" +
//                                    "裁剪宽高: " + media.getCropImageWidth() + "x" + media.getCropImageHeight() + "\n" +
//                                    "文件大小: " + PictureFileUtils.formatAccurateUnitFileSize(media.getSize()) + "\n" +
//                                    "文件大小: " + media.getSize() + "\n" +
//                                    "文件时长: " + media.getDuration()
//                            );
                        }
                        onPictureSelectorResultListener.onResult(result);
                    }

                    @Override
                    public void onCancel() {

                    }
                });
    }

    /**
     * 查看图片大图
     *
     * @param mContext
     * @param position
     * @param localMedia
     */
    public static void openImage(Context mContext, int position, ArrayList<LocalMedia> localMedia) {
        PictureSelector.create(mContext)
                .openPreview()
                .isHidePreviewDownload(true)
                .setImageEngine(GlideEngine.INSTANCE)
                .setExternalPreviewEventListener(new OnExternalPreviewEventListener() {
                    @Override
                    public void onPreviewDelete(int position) {

                    }

                    @Override
                    public boolean onLongPressDownload(Context context, LocalMedia media) {
                        return false;
                    }

                }).startActivityPreview(position, false, localMedia);
    }

    public static void openImage(Context mContext, int position, String imageUrl) {
        ArrayList<LocalMedia> localMedia = new ArrayList<>();
        for (String url : imageUrl.split(",")) {
            localMedia.add(LocalMedia.generateHttpAsLocalMedia(url));
        }
        PictureSelector.create(mContext)
                .openPreview()
                .setImageEngine(GlideEngine.INSTANCE)
                .setExternalPreviewEventListener(new OnExternalPreviewEventListener() {
                    @Override
                    public void onPreviewDelete(int position) {

                    }

                    @Override
                    public boolean onLongPressDownload(Context context, LocalMedia media) {
                        return false;
                    }

                }).startActivityPreview(position, false, localMedia);
    }

    /**
     * 预览视频
     *
     * @param mContext
     * @param position
     * @param localMedia
     */
    public static void openVideo(Context mContext, int position, ArrayList<LocalMedia> localMedia) {
        openVideo(mContext, position, localMedia, null);
    }

    /**
     * 预览视频
     *
     * @param mContext
     * @param position
     * @param localMedia
     */
    public static void openVideo(Context mContext, int position, ArrayList<LocalMedia> localMedia, VideoPlayerEngine videoPlayerEngine) {
        PictureSelector.create(mContext)
                .openPreview()
                .setImageEngine(GlideEngine.INSTANCE)
                .setVideoPlayerEngine(videoPlayerEngine)
                .isAutoVideoPlay(true)
                .setExternalPreviewEventListener(new OnExternalPreviewEventListener() {
                    @Override
                    public void onPreviewDelete(int position) {

                    }

                    @Override
                    public boolean onLongPressDownload(Context context, LocalMedia media) {
                        return false;
                    }

                }).startActivityPreview(position, false, localMedia);
    }

    public interface OnPictureSelectorResultListener {
        void onResult(ArrayList<LocalMedia> result);
    }

    /**
     * 自定义拍照
     */
    private static class MeOnCameraInterceptListener implements OnCameraInterceptListener {

        @Override
        public void openCamera(Fragment fragment, int cameraMode, int requestCode) {
            SimpleCameraX camera = SimpleCameraX.of();
            camera.isAutoRotation(true);
            camera.setCameraMode(cameraMode);
            camera.setVideoFrameRate(50);
            camera.setVideoBitRate(5 * 1024 * 1024);
            camera.isDisplayRecordChangeTime(true);
            camera.isManualFocusCameraPreview(true);
            camera.isZoomCameraPreview(true);
            camera.setImageEngine((context, url, imageView) -> Glide.with(context).load(url).into(imageView));
            camera.start(fragment.requireActivity(), fragment, requestCode);
        }
    }

    /**
     * 录音回调事件
     */
    private static class MeOnRecordAudioInterceptListener implements OnRecordAudioInterceptListener {

        @Override
        public void onRecordAudio(Fragment fragment, int requestCode) {
            String[] recordAudio = {Manifest.permission.RECORD_AUDIO};
            if (PermissionChecker.isCheckSelfPermission(fragment.getContext(), recordAudio)) {
                startRecordSoundAction(fragment, requestCode);
            } else {
                PermissionChecker.getInstance().requestPermissions(fragment,
                        new String[]{Manifest.permission.RECORD_AUDIO}, new PermissionResultCallback() {
                            @Override
                            public void onGranted() {
                                startRecordSoundAction(fragment, requestCode);
                            }

                            @Override
                            public void onDenied() {
                            }
                        });
            }
        }
    }

    /**
     * 启动录音意图
     *
     * @param fragment
     * @param requestCode
     */
    private static void startRecordSoundAction(Fragment fragment, int requestCode) {
        Intent recordAudioIntent = new Intent(MediaStore.Audio.Media.RECORD_SOUND_ACTION);
        if (recordAudioIntent.resolveActivity(fragment.requireActivity().getPackageManager()) != null) {
            fragment.startActivityForResult(recordAudioIntent, requestCode);
        } else {
            ToastUtils.showToast(fragment.getContext(), "The system is missing a recording component");
        }
    }
}

