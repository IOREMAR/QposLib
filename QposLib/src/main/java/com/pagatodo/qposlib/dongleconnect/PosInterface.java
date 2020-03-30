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

    boolean requestQuotas();

    void setFallBack(boolean isFallback);

    void getPin(final String maskedPAN);

    void getSessionKeys(final String clavePublicaFile, final Context context);

    void doTransaccion(TransactionAmountData transactionAmountData);

    void cancelOperacion();

    void operacionFinalizada(final String arpc, final int status);

    Map<String, String> getIccTags();

    DspreadDevicePOS getDeviePos();

    void deviceCancel();

    Hashtable<String, String> getQposIdHash();

    int updateFirmware(final byte[] dataToUpdate, final String file);

    int getUpdateProgress();


    QPOSDeviceInfo getDevicePosInfo();

    byte[] onEncryptData(final byte[] bytes, final PosInterface.EncrypType type);

    /**
     * Tipo de Dispositivo
     */
    public enum Tipodongle {
        DSPREAD,
        NODSPREAD,
        SUNMI
    }

    enum EncrypType {
        TAGENCRYPT,
        ICCENCRYPT,
        PINENCRYPT
    }

//    void doMifareCard(MifareCommand mifareCommand, int timeout);
}
