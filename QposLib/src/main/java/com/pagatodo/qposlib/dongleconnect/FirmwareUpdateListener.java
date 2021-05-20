package com.pagatodo.qposlib.dongleconnect;

import com.pagatodo.qposlib.enums.FirmwareStatus;

public interface FirmwareUpdateListener {
    void onPosFirmwareUpdateProgress(int percentage);

    void onPosFirmwareUpdateResult(FirmwareStatus firmwareStatus, boolean requiresReconnect);
}