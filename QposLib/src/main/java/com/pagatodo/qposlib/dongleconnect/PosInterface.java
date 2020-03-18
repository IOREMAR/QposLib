package com.pagatodo.qposlib.dongleconnect;

import android.content.Context;

import com.pagatodo.qposlib.pos.QPOSDeviceInfo;
import com.pagatodo.qposlib.pos.dspread.DspreadDevicePOS;

import java.util.Hashtable;
import java.util.Map;

public interface PosInterface {

    void openCommunication();

    void closeCommunication();

    void resetQPOS();

    String getPosInfo();

    void getPin(final String maskedPAN);

    void getSessionKeys(final String clavePublicaFile, final Context context);

    @Deprecated
    void doTransaccion(TransactionAmountData transactionAmountData);

    void doTransaccion(TransactionAmountData transactionAmountData, int tradeMode);

    void cancelOperacion();

    void operacionFinalizada(final String ARPC);

    Map<String, String> getIccTags();

    DspreadDevicePOS getDeviePos();

    void deviceCancel();

    Hashtable<String, String> getQposIdHash();

    int updateFirmware(final byte[] dataToUpdate, final String file);

    int getUpdateProgress();

    QPOSDeviceInfo getDevicePosInfo();

    /**
     * Tipo de Dispositivo
     */
    public enum Tipodongle {
        DSPREAD,
        NODSPREAD,
        SUNMI
    }

//    void doMifareCard(MifareCommand mifareCommand, int timeout);
}
