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

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import com.ct.ertclib.dc.core.databinding.ActivityWebBinding
import com.ct.ertclib.dc.core.ui.activity.BaseAppCompatActivity
import com.ct.ertclib.dc.core.ui.activity.BaseToolBarActivity

class WebActivity: BaseAppCompatActivity() {
    companion object{
        const val PARAMS_WEB_URL = "params_web_url"
        const val PARAMS_WEB_TITLE= "params_web_title"
        const val PARAMS_CALL_ID= "params_web_call_id"
        fun startActivity(context:Context,url:String,title:String,callId:String?){
            val intent = Intent(context,WebActivity::class.java)
            intent.putExtra(PARAMS_WEB_URL,url)
            intent.putExtra(PARAMS_WEB_TITLE,title)
            if (!TextUtils.isEmpty(callId)){
                intent.putExtra(PARAMS_CALL_ID,callId)
            }
            context.startActivity(intent)
        }
    }
    private lateinit var mBinding: ActivityWebBinding
    private var url = ""
    private var title = ""
    private var callId = ""
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mBinding = ActivityWebBinding.inflate(layoutInflater)
        setContentView(mBinding.root)
        intent.getStringExtra(PARAMS_WEB_URL)?.let {
            url = it
        }
        intent.getStringExtra(PARAMS_WEB_TITLE)?.let {
            title = it
        }
        intent.getStringExtra(PARAMS_CALL_ID)?.let {
            callId = it
        }
        mBinding.webView.loadUrl(url)
        mBinding.title.text = title
        mBinding.backIcon.setOnClickListener {
            finish()
        }
    }
}