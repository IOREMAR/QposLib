package com.pagatodo.qposlib.pos.dspread;

import android.hardware.usb.UsbDevice;

import com.dspread.xpos.QPOSService;

public class POSUsbDevice extends DspreadDevicePOS<UsbDevice> {

    public POSUsbDevice(final UsbDevice usbDevice) {
        super(usbDevice, QPOSService.CommunicationMode.USB);
    }
}

