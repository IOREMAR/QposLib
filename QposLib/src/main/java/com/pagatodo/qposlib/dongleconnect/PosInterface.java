package com.pagatodo.qposlib.dongleconnect;

import android.content.Context;

import com.pagatodo.qposlib.pos.QPOSDeviceInfo;
import com.pagatodo.qposlib.pos.dspread.DspreadDevicePOS;

import java.util.Map;

public interface PosInterface {

    void openCommunication();

    void closeCommunication();

    void resetQPOS();

    String getPosInfo();

    void getPin(final String maskedPAN);

    void getSessionKeys(final String clavePublicaFile, final Context context);

    void doTransaccion(TransactionAmountData transactionAmountData);

    void cancelOperacion();

    void operacionFinalizada(final String ARPC);

    Map<String, String> getIccTags();

    DspreadDevicePOS getDeviePos();

    void deviceCancel();

    int updateFirmware(final byte[] dataToUpdate, final String file);

    int getUpdateProgress();

    QPOSDeviceInfo getDevicePosInfo();

    /**
     * Tipo de Dispositivo
     */
    public enum Tipodongle {
        DSPREAD,
        NODSPREAD

    }

//    void doMifareCard(MifareCommand mifareCommand, int timeout);
}
