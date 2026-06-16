package com.ct.ertclib.dc.core.utils.common

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.ct.ertclib.dc.core.data.common.MediaInfo
import com.ct.ertclib.dc.core.picker.VideoThumbListener
import com.ct.ertclib.dc.core.utils.logger.Logger
import com.luck.picture.lib.basic.PictureSelector
import com.luck.picture.lib.config.SelectMimeType
import com.luck.picture.lib.entity.LocalMedia
import com.luck.picture.lib.interfaces.OnResultCallbackListener
import me.rosuh.filepicker.bean.FileItemBeanImpl
import me.rosuh.filepicker.config.AbstractFileFilter
import me.rosuh.filepicker.config.FilePickerManager
import me.rosuh.filepicker.filetype.RasterImageFileType
import me.rosuh.filepicker.filetype.VideoFileType
import me.rosuh.filepicker.filetype.AudioFileType
import me.rosuh.filepicker.filetype.CompressedFileType
import me.rosuh.filepicker.filetype.DataBaseFileType
import me.rosuh.filepicker.filetype.DataFileType
import me.rosuh.filepicker.filetype.ExecutableFileType
import me.rosuh.filepicker.filetype.FontFileType
import me.rosuh.filepicker.filetype.PageLayoutFileType
import me.rosuh.filepicker.filetype.TextFileType
import me.rosuh.filepicker.filetype.WebFileType
import java.io.File

/**
 * 文件选择辅助类
 * 用于在悬浮窗等非Activity场景中选择文件
 */
object FilePickerHelper {

    private const val TAG = "FilePickerHelper"
    private val logger = Logger.getLogger(TAG)

    private var callback: FilePickCallback? = null

    interface FilePickCallback {
        fun onSuccess(mediaInfoList: List<MediaInfo>)
        fun onFailed()
        fun onCancel()
    }

    /**
     * 选择多个文件
     */
    fun pickFiles(
        context: Context,
        maxSelectable: Int?,
        fileTypes: List<String>?,
        callback: FilePickCallback,
    ) {
        this.callback = callback

        val intent = Intent(context, FilePickerActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            putExtra(FilePickerActivity.EXTRA_PICK_MODE, FilePickerActivity.MODE_MULTIPLE)
            putExtra(FilePickerActivity.EXTRA_MAX_SELECTABLE, maxSelectable)
            putExtra(FilePickerActivity.EXTRA_FILE_TYPES, fileTypes?.let { ArrayList(it) })
        }

        try {
            context.startActivity(intent)
            logger.debug("FilePickerActivity started")
        } catch (e: Exception) {
            logger.error("Failed to start FilePickerActivity", e)
            this.callback?.onFailed()
            this.callback = null
        }
    }

    /**
     * 打开摄像头拍照/录像
     */
    fun openCamera(
        context: Context,
        openCamera: Int,
        isRotateImage: Boolean,
        dirPath: String,
        callback: FilePickCallback
    ) {
        this.callback = callback

        val intent = Intent(context, FilePickerActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            putExtra(FilePickerActivity.EXTRA_PICK_MODE, FilePickerActivity.MODE_CAMERA)
            putExtra(FilePickerActivity.EXTRA_CAMERA_MODE, openCamera)
            putExtra(FilePickerActivity.EXTRA_IS_ROTATE_IMAGE, isRotateImage)
            putExtra(FilePickerActivity.EXTRA_CAMERA_DIR_PATH, dirPath)
        }

        try {
            context.startActivity(intent)
            logger.debug("FilePickerActivity started dirPath:$dirPath")
        } catch (e: Exception) {
            logger.error("Failed to start FilePickerActivity", e)
            this.callback?.onFailed()
            this.callback = null
        }
    }

    internal fun onResult(mediaInfoList: List<MediaInfo>?) {
        if (!mediaInfoList.isNullOrEmpty()) {
            callback?.onSuccess(mediaInfoList)
        } else {
            logger.debug("File pick cancelled or failed")
            callback?.onCancel()
        }
        callback = null
    }

    internal fun onFailed() {
        logger.error("File pick failed")
        callback?.onFailed()
        callback = null
    }

    /**
     * 文件选择的透明 Activity
     * 用于接收文件选择结果并回调
     */
    class FilePickerActivity : AppCompatActivity() {

        private val logger = Logger.getLogger("FilePickerActivity")
        private var isResultHandled = false

        var mode = MODE_MULTIPLE

        var maxSelectable: Int? = null
        var fileTypes: List<String>? = null

        override fun onCreate(savedInstanceState: Bundle?) {
            overridePendingTransition(0, 0)
            super.onCreate(savedInstanceState)

            // 设置透明窗口
            window.setBackgroundDrawableResource(android.R.color.transparent)
            window.setLayout(1, 1)

            logger.debug("FilePickerActivity created")

            mode = intent?.getIntExtra(EXTRA_PICK_MODE, MODE_MULTIPLE) ?: MODE_MULTIPLE
            maxSelectable = intent?.getIntExtra(EXTRA_MAX_SELECTABLE, DEFAULT_MAX_SELECTABLE) ?: DEFAULT_MAX_SELECTABLE
            fileTypes = intent?.getStringArrayListExtra(EXTRA_FILE_TYPES)

            when (mode) {
                MODE_MULTIPLE -> {
                    try {
                        val pickerConfig = FilePickerManager
                            .from(this)
                            .maxSelectable(maxSelectable!!)

                        if (!fileTypes.isNullOrEmpty()) {
                            pickerConfig.filter(object : AbstractFileFilter() {
                                override fun doFilter(listData: ArrayList<FileItemBeanImpl>): ArrayList<FileItemBeanImpl> {
                                    return ArrayList(listData.filter { item ->
                                        // 永远保留文件夹以便导航
                                        if (item.isDir) return@filter true

                                        fileTypes!!.any { type ->
                                            when (type) {
                                                "image" -> item.fileType is RasterImageFileType
                                                "video" -> item.fileType is VideoFileType
                                                "audio" -> item.fileType is AudioFileType
                                                "text" -> item.fileType is TextFileType
                                                "compressed" -> item.fileType is CompressedFileType
                                                "pageLayout" -> item.fileType is PageLayoutFileType
                                                "executable" -> item.fileType is ExecutableFileType
                                                "dataBase" -> item.fileType is DataBaseFileType
                                                "data" -> item.fileType is DataFileType
                                                "font" -> item.fileType is FontFileType
                                                "web" -> item.fileType is WebFileType
                                                else -> false
                                            }
                                        }
                                    })
                                }
                            })
                        }

                        // 3. 启动选择器
                        pickerConfig.forResult(FilePickerManager.REQUEST_CODE)
                    } catch (e: Exception){
                        logger.error("Failed to start AndroidFilePicker", e)
                        finishWithResult(null)
                    }
                }
                MODE_CAMERA -> {
                    try {
                        logger.info("PictureSelector Starting camera")
                        val cameraMode = intent.getIntExtra(EXTRA_CAMERA_MODE, SelectMimeType.ofImage())
                        val isRotateImage = intent.getBooleanExtra(EXTRA_IS_ROTATE_IMAGE, false)
                        val dirPath = intent.getStringExtra(EXTRA_CAMERA_DIR_PATH) ?: ""
                        PictureSelector.create(this@FilePickerActivity)
                            .openCamera(cameraMode)
                            .isCameraAroundState(isRotateImage)
                            .setOutputCameraDir(dirPath)
                            .setVideoThumbnailListener(VideoThumbListener(this@FilePickerActivity))
                            .forResult(object : OnResultCallbackListener<LocalMedia> {
                                override fun onResult(result: ArrayList<LocalMedia>) {
                                    // LocalMedia 转 MediaInfo
                                    val mediaInfoList = ArrayList<MediaInfo>()
                                    for (localMedia in result) {
                                        localMedia.let {
                                            val info = MediaInfo().apply {
                                                this.id = it.id
                                                this.path = it.path
                                                this.bucketId = it.bucketId
                                                this.absolutePath = path
                                                this.mimeType = it.mimeType
                                                this.width = it.width
                                                this.height = it.height
                                                this.cropOffsetX = it.cropOffsetX
                                                this.cropOffsetY = it.cropOffsetY
                                                this.duration = it.duration
                                                this.size = it.size
                                                this.sandboxPath = it.sandboxPath
                                                this.originalPath = it.originalPath
                                                this.compressPath = it.compressPath
                                                this.watermarkPath = it.watermarkPath
                                                this.videoThumbnailPath = it.videoThumbnailPath
                                                this.displayName = it.fileName
                                            }
                                            mediaInfoList.add(info)
                                        }
                                    }
                                    finishWithResult(mediaInfoList)
                                }

                                override fun onCancel() {
                                    logger.info("PictureSelector Camera cancelled")
                                    finishWithResult(null)
                                }
                            })
                    } catch (e: Exception){
                        logger.error("Failed to start camera", e)
                        finishWithResult(null)
                    }
                }
            }
        }

        private fun createDefaultPickIntent(): Intent {
            return Intent(Intent.ACTION_GET_CONTENT).apply {
                type = "*/*"
                addCategory(Intent.CATEGORY_OPENABLE)
            }
        }

        override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
            super.onActivityResult(requestCode, resultCode, data)
            if (requestCode == FilePickerManager.REQUEST_CODE && !isResultHandled){
                isResultHandled = true
                when (resultCode) {
                    RESULT_OK -> {
                        val list = FilePickerManager.obtainData(release = true)
                        logger.debug("FileList (${list.size}): ${list.joinToString(", ")}")
                        val mediaInfoList = ArrayList<MediaInfo>()
                        for (absPath in list) {
                            createMediaInfoFromPath(absPath)?.let {
                                mediaInfoList.add(it)
                            }
                        }
                        finishWithResult(mediaInfoList)
                    }
                    RESULT_CANCELED -> {
                        logger.debug("Files pick cancelled")
                        finishWithResult(null)
                    }
                    else -> {
                        logger.warn("Unknown result code: $resultCode")
                        finishWithResult(null)
                    }
                 }

            } else {
                if (mode != MODE_CAMERA){ // 拍照的库会触发这个，屏蔽一下
                    finishWithResult(null)
                }
            }
        }

        private fun createMediaInfoFromUri(context: Context,uri: Uri): MediaInfo? {
            val displayName = FileUtils.getFileNameFromUri(this@FilePickerActivity, uri)
            val cacheFile = FileUtils.copyUriToCache(context,uri, displayName)
            if (cacheFile == null) {
                logger.warn("Failed to copy file to cache")
                return null
            }
            return MediaInfo().apply {
                path = cacheFile.absolutePath
                this.absolutePath = cacheFile.absolutePath
                this.displayName = FileUtils.getFileNameFromUri(this@FilePickerActivity, uri)
                size = FileUtils.getFileSizeFromUri(this@FilePickerActivity, uri)
                lastModified = FileUtils.getFileLastModifiedFromUri(this@FilePickerActivity, uri)
                isDirectory = false
            }
        }

        private fun createMediaInfoFromPath(absolutePath: String): MediaInfo? {
            return MediaInfo().apply {
                val file = File(absolutePath)
                if (!file.exists()) return null

                path = absolutePath
                this.absolutePath = absolutePath
                displayName = file.name
                size = file.length()
                lastModified = file.lastModified()
                isDirectory = false
            }
        }

        private fun finishWithResult(mediaInfoList: List<MediaInfo>?) {
            onResult(mediaInfoList)
            window.decorView.post({
                if (!isFinishing && !isDestroyed) {
                    finish()
                    overridePendingTransition(0, 0)
                }
            })
        }

        override fun onBackPressed() {
            logger.debug("Back pressed, cancelling file pick")
            finishWithResult(null)
            super.onBackPressed()
        }

        companion object {
            const val EXTRA_PICK_MODE = "extra_pick_mode"
            const val EXTRA_MAX_SELECTABLE = "extra_max_selectable"
            const val DEFAULT_MAX_SELECTABLE = 9
            const val EXTRA_FILE_TYPES = "extra_file_types"
            const val EXTRA_CAMERA_MODE = "extra_camera_mode"
            const val EXTRA_IS_ROTATE_IMAGE = "extra_is_rotate_image"
            const val EXTRA_CAMERA_DIR_PATH = "extra_camera_dir_path"
            const val MODE_MULTIPLE = 2
            const val MODE_CAMERA = 3
        }
    }
}