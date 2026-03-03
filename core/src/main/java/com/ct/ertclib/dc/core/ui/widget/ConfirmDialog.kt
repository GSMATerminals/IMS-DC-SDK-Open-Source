package com.ct.ertclib.dc.core.ui.widget

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.text.TextUtils
import android.view.View
import android.view.WindowManager
import androidx.fragment.app.DialogFragment
import com.ct.ertclib.dc.core.R
import com.ct.ertclib.dc.core.databinding.DialogConfirmBinding
import com.ct.ertclib.dc.core.ui.activity.ConfirmActivity
import com.ct.ertclib.dc.core.utils.logger.Logger

class ConfirmDialog(val messagae: String, val confirmCallback: ConfirmActivity.ConfirmCallback) : DialogFragment(),
    View.OnClickListener {

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