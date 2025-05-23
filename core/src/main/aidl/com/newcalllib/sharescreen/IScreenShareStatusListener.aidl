package com.newcalllib.sharescreen;

import com.newcalllib.sharescreen.ScreenShareStatus;

interface IScreenShareStatusListener {
    void onScreenShareStatus(in ScreenShareStatus status);
}