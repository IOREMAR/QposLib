package com.pagatodo.qposlib.abstracts;

import com.pagatodo.qposlib.QPosManager;
import com.pagatodo.qposlib.dongleconnect.DongleConnect;

import com.pagatodo.qposlib.dongleconnect.DongleListener;
import com.pagatodo.qposlib.dongleconnect.PosInterface;


import static com.pagatodo.qposlib.dongleconnect.PosInterface.Tipodongle.DSPREAD;


public abstract class AbstractDongle implements PosInterface {

    protected DongleListener dongleListener;
    protected DongleConnect dongleConnect;

    protected QPosManager qposDspread;

    public AbstractDongle(final DongleConnect listener) {
        dongleConnect = listener;
    }

    public void setDongleListener(final DongleListener dongleListener) {
        this.dongleListener = dongleListener;
    }


    public void setDongleConnect (final DongleConnect connect){
        dongleConnect = connect;
    }

    public AbstractDongle getQpos(final PosInterface.Tipodongle tipodongle) {
        if (tipodongle.equals(DSPREAD)) {
            return qposDspread;
        } else {
            return null;
        }
    }

    public void setQposDspread(final QPosManager qposDspread) {
        this.qposDspread = qposDspread;
    }

}