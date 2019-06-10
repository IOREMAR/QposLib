package com.pagatodo.qposlib.pos.dspread;

import android.hardware.usb.UsbDevice;

import com.dspread.xpos.QPOSService;

public class POSUsbOTGDevice extends DspreadDevicePOS<UsbDevice> {
    private String name;

    public POSUsbOTGDevice(final UsbDevice mDevicePos) {

        super(mDevicePos, QPOSService.CommunicationMode.USB_OTG_CDC_ACM);
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }
}
