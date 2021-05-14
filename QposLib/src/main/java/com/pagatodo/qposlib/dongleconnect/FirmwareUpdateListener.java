package com.pagatodo.qposlib.dongleconnect;

import com.pagatodo.qposlib.QPosManager;

public interface FirmwareUpdateListener {
    void onPosFirmwareUpdateProgress(int percentage);

    void onPosFirmwareUpdateResult(QPosManager.FirmwareStatus firmwareStatus);
}