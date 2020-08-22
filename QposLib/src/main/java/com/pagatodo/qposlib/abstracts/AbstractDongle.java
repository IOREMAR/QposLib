package com.pagatodo.qposlib.abstracts;

import com.pagatodo.qposlib.QPosManager;
import com.pagatodo.qposlib.dongleconnect.DongleConnect;
import com.pagatodo.qposlib.dongleconnect.DongleListener;
import com.pagatodo.qposlib.dongleconnect.FirmwareUpdateListener;
import com.pagatodo.qposlib.dongleconnect.PosInterface;

public abstract class AbstractDongle implements PosInterface {
    protected FirmwareUpdateListener firmwareUpdate;
    protected DongleListener dongleListener;
    protected DongleConnect dongleConnect;

    protected QPosManager qposDspread;

    public AbstractDongle() {
    }

    public AbstractDongle(final DongleConnect listener) {
        dongleConnect = listener;
    }

    public void setDongleListener(final DongleListener dongleListener) {
        this.dongleListener = dongleListener;
    }

    public void setDongleConnect(final DongleConnect connect) {
        dongleConnect = connect;
    }

    public void setFirmwareUpdate(final FirmwareUpdateListener firmwareUpdate) {
        this.firmwareUpdate = firmwareUpdate;
    }

    public AbstractDongle getQpos(final PosInterface.Tipodongle tipodongle) {
        switch (tipodongle) {
            case DSPREAD:
                return qposDspread;
            default:
                return null;
        }
    }

    public void setQposDspread(final QPosManager qposDspread) {
        this.qposDspread = qposDspread;
    }
}