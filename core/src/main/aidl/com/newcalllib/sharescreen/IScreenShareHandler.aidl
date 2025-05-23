package com.newcalllib.sharescreen;

import com.newcalllib.sharescreen.IScreenShareStatusListener;

interface IScreenShareHandler {

//    void nativeStartScreenShare(in IScreenShareStatusListener l);
    void startNativeScreenShare(in IScreenShareStatusListener l);

//    void nativeStopScreenShare();
    void stopNativeScreenShare();

//    boolean nativeRequestScreenShareAbility();
    boolean requestNativeScreenShareAbility();
}
