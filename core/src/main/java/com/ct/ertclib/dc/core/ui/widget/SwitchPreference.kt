package com.ct.ertclib.dc.core.ui.widget

import android.content.Context
import android.util.AttributeSet
import android.widget.TextView
import androidx.appcompat.widget.SwitchCompat
import androidx.core.view.isVisible
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder
import com.ct.ertclib.dc.core.R
import com.ct.ertclib.dc.core.utils.common.LogUtils

class SwitchPreference@JvmOverloads constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int = 0): Preference(context, attrs, defStyleAttr) {

    companion object {
        private const val TAG = "SwitchPreference"
    }

    private var settingTitle: TextView? = null
    private var settingSummary: TextView? = null
    private var switchCompat: SwitchCompat? = null
    private var title = ""
    private var summary = ""
    var status: Boolean = false
    private var statusListener : ((Boolean) -> Unit)? = null

    init {
        val typeArray = context.obtainStyledAttributes(attrs, androidx.preference.R.styleable.Preference)

        typeArray.getString(androidx.preference.R.styleable.Preference_title)?.let {
            title = it
        }
        typeArray.getString(androidx.preference.R.styleable.Preference_summary)?.let {
            summary = it
        }
    }

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)

        settingTitle = holder.itemView.findViewById(R.id.setting_preference_title)
        settingSummary = holder.itemView.findViewById(R.id.setting_preference_summary)
        switchCompat = holder.itemView.findViewById(R.id.switch_widget)
        settingTitle?.text = title
        settingSummary?.text = summary
        switchCompat?.isChecked = status
        statusListener?.let { listener ->
            switchCompat?.setOnCheckedChangeListener { _, isChecked ->
                listener.invoke(isChecked)
            }
        }
        adjustPadding()
    }

    fun setSwitchChangeListener(stateListener: (Boolean) -> Unit) {
        this.statusListener = stateListener
        switchCompat?.setOnCheckedChangeListener { _, isChecked ->
            stateListener.invoke(isChecked)
        }
    }

    fun updateSwitchStatus(status: Boolean) {
        LogUtils.debug(TAG, "updateSwitchStatus status: $status")
        switchCompat?.isChecked = status
        this.status = status
    }

    fun updateSummary(summary: String) {
        LogUtils.debug(TAG, "updateSummary summary: $summary")
        this.summary = summary
        settingSummary?.text = summary
        adjustPadding()
    }

    private fun adjustPadding() {
        if (summary.isEmpty()) {
            settingTitle?.let {
                it.setPadding(it.paddingLeft, context.resources.getDimensionPixelSize(R.dimen.preference_padding_top_bottom), it.paddingRight, it.paddingBottom)
            }
            settingSummary?.isVisible = false
        } else {
            settingTitle?.let {
                it.setPadding(it.paddingLeft, 0, it.paddingRight, it.paddingBottom)
            }
            settingSummary?.isVisible = true
        }
    }
}