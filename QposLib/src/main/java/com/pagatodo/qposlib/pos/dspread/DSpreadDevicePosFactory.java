package com.pagatodo.qposlib.pos.dspread;

import android.bluetooth.BluetoothDevice;
import android.hardware.usb.UsbDevice;
import android.os.Parcelable;

import com.pagatodo.qposlib.QPosManager;
import com.pagatodo.qposlib.abstracts.AbstractDongle;
import com.pagatodo.qposlib.dongleconnect.DongleConnect;
import com.pagatodo.qposlib.dongleconnect.PosInterface;


public class DSpreadDevicePosFactory {

    public AbstractDongle getDongleDevice(final Parcelable device, final PosInterface.Tipodongle tipodongle, final DongleConnect dongleConnect) {

        if (tipodongle.equals(PosInterface.Tipodongle.DSPREAD)) {

            if (device instanceof BluetoothDevice) {

                return new QPosManager( new POSBluetoothDevice((BluetoothDevice) device),dongleConnect);
            } else if (device instanceof UsbDevice) {
                return new QPosManager( new POSUsbOTGDevice((UsbDevice) device),dongleConnect);
            }
        }

        return null;
    }
}
