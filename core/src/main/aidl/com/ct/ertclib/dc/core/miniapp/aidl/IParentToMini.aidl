package com.ct.ertclib.dc.core.miniapp.aidl;

import com.ct.ertclib.dc.core.miniapp.aidl.IMessageCallback;

interface IParentToMini {

    void finishMiniAppActivity();

    void sendMessageToMini(in String miniAppId, in String message, in IMessageCallback iMessageCallback);
}
