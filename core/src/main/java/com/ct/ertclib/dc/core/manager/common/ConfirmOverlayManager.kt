package com.ct.ertclib.dc.core.manager.common

import android.content.Context
import com.ct.ertclib.dc.core.ui.widget.ConfirmOverlayWindow
import java.lang.ref.WeakReference

object ConfirmOverlayManager {
    private var currentWindow: WeakReference<ConfirmOverlayWindow>? = null

    fun startConfirm(
        context: Context,
        message: String,
        callback: ConfirmOverlayWindow.ConfirmCallback?,
        accept: String? = null,
        cancel: String? = null
    ) {
        // 检查是否已有显示的窗口
        if (isShowing()) {
            return
        }

        val window = ConfirmOverlayWindow(context.applicationContext) // 使用ApplicationContext避免泄漏
        window.show(message, callback, accept, cancel)
        currentWindow = WeakReference(window)
    }

    fun dismiss() {
        currentWindow?.get()?.dismiss()
        currentWindow = null
    }

    fun isShowing(): Boolean {
        return currentWindow?.get()?.isShowing == true
    }
}