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

package com.ct.ertclib.dc.core.common;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;

import com.ct.ertclib.dc.core.ui.activity.BaseFragmentActivity;
import com.ct.ertclib.dc.core.utils.logger.Logger;
import com.ct.ertclib.dc.core.R;

public class ConfirmActivity extends BaseFragmentActivity {

    private static final String TAG = "ConfirmActivity";
    private static final Logger sLogger = Logger.getLogger(TAG);

    private static ConfirmCallback mConfirmCallback;

    private ConfirmDialog confirmDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_confirm);

        Intent intent = getIntent();
        if (intent != null) {
            String message = intent.getStringExtra("message");
            String accept = intent.getStringExtra("accept");
            String cancel = intent.getStringExtra("cancel");

            confirmDialog = new ConfirmDialog(message, mConfirmCallback);
            if (!TextUtils.isEmpty(accept)) confirmDialog.setAcceptText(accept);
            if (!TextUtils.isEmpty(cancel)) confirmDialog.setCancelText(cancel);
            confirmDialog.show(getSupportFragmentManager(), ConfirmDialog.class.getSimpleName());
            confirmDialog.setCallback(new ConfirmDialog.Callback() {
                @Override
                public void onDismiss() {
                    finish();
                }
            });
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mConfirmCallback = null;
    }

    public interface ConfirmCallback {
        void onAccept();

        void onCancel();
    }

    public static void startConfirm(Context context, String message, ConfirmCallback callback, String accept, String cancel) {
        // 先简单处理，只允许一个弹窗
        if (mConfirmCallback != null){
            return;
        }
        mConfirmCallback = callback;
        Intent intent = new Intent(context, ConfirmActivity.class);
        intent.setFlags(Intent.FLAG_RECEIVER_FOREGROUND);
        intent.putExtra("message", message);
        if (!TextUtils.isEmpty(accept)) intent.putExtra("accept", accept);
        if (!TextUtils.isEmpty(cancel)) intent.putExtra("cancel", cancel);
        context.startActivity(intent);
    }
}