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

package com.ct.ertclib.dc.core.common

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.text.TextUtils
import android.view.View
import android.view.View.OnClickListener
import android.view.WindowManager
import androidx.fragment.app.DialogFragment
import com.ct.ertclib.dc.core.utils.logger.Logger
import com.ct.ertclib.dc.core.R
import com.ct.ertclib.dc.core.databinding.DialogConfirmBinding
import com.ct.ertclib.dc.core.common.ConfirmActivity.ConfirmCallback

class ConfirmDialog(val messagae: String, val confirmCallback: ConfirmCallback) : DialogFragment(), OnClickListener {

    companion object {
        private const val TAG = "ConfirmDialog"
    }

    private val sLogger = Logger.getLogger(TAG)

    private var callback: Callback? = null
    private var acceptText: String = ""
    private var cancelText: String = ""
    private var otherText: String = ""

    private lateinit var viewBinding: DialogConfirmBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.ConfirmActivity)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        viewBinding = DialogConfirmBinding.inflate(layoutInflater)
        dialog.setContentView(viewBinding.root)
        val attributes = dialog.window!!.attributes
        attributes.width = WindowManager.LayoutParams.MATCH_PARENT
        attributes.height = WindowManager.LayoutParams.WRAP_CONTENT
        dialog.window!!.attributes = attributes
        initView()
        return dialog
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        callback?.onDismiss()
    }

    private fun initView() {
        viewBinding.tvMessage.visibility = View.VISIBLE
        viewBinding.tvMessage.text = messagae
        viewBinding.btnCancel.setOnClickListener(this)
        viewBinding.btnDone.setOnClickListener(this)

        if (!TextUtils.isEmpty(acceptText)) {
            viewBinding.btnDone.text = acceptText
        }

        if (!TextUtils.isEmpty(cancelText)) {
            viewBinding.btnCancel.text = cancelText
        }
    }

    fun setCallback(callback: Callback) {
        this.callback = callback
    }

    fun setAcceptText(accept: String) {
        acceptText = accept
    }

    fun setCancelText(cancel: String) {
        cancelText = cancel
    }

    interface Callback {
        fun onDismiss()
    }

    override fun onClick(v: View?) {
        v?.let {
            if (it.id == R.id.btn_cancel) {
                confirmCallback.onCancel()
                sLogger.info("confirm dialog onCancel")
                dismiss()
            } else if (it.id == R.id.btn_done) {
                confirmCallback.onAccept()
                sLogger.info("confirm dialog accept")
                dismiss()
            }
        }
    }

}