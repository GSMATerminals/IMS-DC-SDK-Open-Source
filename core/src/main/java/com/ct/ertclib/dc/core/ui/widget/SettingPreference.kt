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

package com.ct.ertclib.dc.core.ui.widget

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.widget.TextView
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder
import com.ct.ertclib.dc.core.R

@SuppressLint("PrivateResource", "Recycle")
class SettingPreference @JvmOverloads constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int = 0): Preference(context, attrs, defStyleAttr) {

    private var settingTitle: TextView? = null
    private var title = ""

    init {
        val typeArray = context.obtainStyledAttributes(attrs, androidx.preference.R.styleable.Preference)

        typeArray.getString(androidx.preference.R.styleable.Preference_title)?.let {
            title = it
        }
    }

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)
        settingTitle = holder.itemView.findViewById(R.id.setting_preference_title)
        settingTitle?.text = title
    }
}