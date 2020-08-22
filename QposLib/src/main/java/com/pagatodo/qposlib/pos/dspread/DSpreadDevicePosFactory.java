package com.pagatodo.qposlib.pos.dspread;

import android.bluetooth.BluetoothDevice;
import android.hardware.usb.UsbDevice;
import android.os.Parcelable;

import com.pagatodo.qposlib.QPosManager;
import com.pagatodo.qposlib.abstracts.AbstractDongle;
import com.pagatodo.qposlib.dongleconnect.DongleConnect;
import com.pagatodo.qposlib.dongleconnect.PosInterface;

import java.util.Hashtable;

public class DSpreadDevicePosFactory {

    public AbstractDongle getDongleDevice(final Parcelable device, final PosInterface.Tipodongle tipodongle, final DongleConnect dongleConnect, boolean enableLog) {
        if (tipodongle.equals(PosInterface.Tipodongle.DSPREAD)) {
            if (device instanceof BluetoothDevice) {
                return new QPosManager(new POSBluetoothDevice((BluetoothDevice) device), dongleConnect, enableLog);
            } else if (device instanceof UsbDevice) {
                return new QPosManager(new POSUsbOTGDevice((UsbDevice) device), dongleConnect, enableLog);
            }
        }

        return null;
    }

    public AbstractDongle getDongleDevice(final PosInterface.Tipodongle tipodongle, final DongleConnect dongleConnect, Hashtable<String, String> hashtable) {
        if (tipodongle.equals(PosInterface.Tipodongle.SUNMI)) {
            return null;
        }
        return null;
    }
}