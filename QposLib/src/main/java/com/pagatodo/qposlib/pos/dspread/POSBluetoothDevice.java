package com.pagatodo.qposlib.pos.dspread;

import android.bluetooth.BluetoothDevice;

import com.dspread.xpos.QPOSService;

public class POSBluetoothDevice extends DspreadDevicePOS<BluetoothDevice> {

    public POSBluetoothDevice(final BluetoothDevice mDevicePos) {
        super(mDevicePos, QPOSService.CommunicationMode.BLUETOOTH);
    }

    public String getName() {
        return mDevicePos.getName();
    }

    public String getAddress() {
        return mDevicePos.getAddress();
    }
}
