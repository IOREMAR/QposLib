package com.pagatodo.qposlib.dongleconnect;

public interface FirmwareUpdateListener {
    void onPosFirmwareUpdated();

    void onPosFirmwareUpdatedFailed(String updateResult);
}
