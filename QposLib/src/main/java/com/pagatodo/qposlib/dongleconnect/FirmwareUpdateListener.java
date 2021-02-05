package com.pagatodo.qposlib.dongleconnect;

import androidx.annotation.Nullable;

public interface FirmwareUpdateListener {
    void onPosFirmwareUpdateProgress(int percentage);

    void onPosFirmwareUpdateResult(boolean wasSuccessful, @Nullable String error);
}