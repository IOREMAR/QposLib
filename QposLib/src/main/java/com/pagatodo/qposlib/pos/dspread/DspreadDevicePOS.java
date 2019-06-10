package com.pagatodo.qposlib.pos.dspread;

import android.os.Parcelable;

import com.dspread.xpos.QPOSService;

public class DspreadDevicePOS<T extends Parcelable> {

    protected final T mDevicePos;
    private final QPOSService.CommunicationMode communicationMode;

    protected DspreadDevicePOS(final T mDevicePos, final QPOSService.CommunicationMode communicationMode) {

        this.mDevicePos = mDevicePos;
        this.communicationMode = communicationMode;
    }

    public T getmDevicePos() {
        return mDevicePos;
    }

    public QPOSService.CommunicationMode getCommunicationMode() {
        return communicationMode;
    }
}