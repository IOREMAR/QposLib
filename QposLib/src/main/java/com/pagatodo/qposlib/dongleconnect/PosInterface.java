package com.pagatodo.qposlib.dongleconnect;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.core.util.Consumer;

import com.pagatodo.qposlib.pos.QPOSDeviceInfo;
import com.pagatodo.qposlib.pos.QposParameters;
import com.pagatodo.qposlib.pos.dspread.DspreadDevicePOS;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Map;

public interface PosInterface {

    void openCommunication();

    void reopenCommunication();

    void closeCommunication();

    void resetQPOS();

    void getPin(int maxLen, final String maskedPAN);

    void getSessionKeys(final String clavePublicaFile, final Context context);

    void setEmvAidUpdate(ArrayList<String> aidConfigList, Consumer<Boolean> onEmvAidConfigUpdateConsumer);

    void setAidTlvUpdate(@NonNull String[] aidTlvList, Consumer<Boolean> onAidTlvUpdateConsumer);

    void updateDefaultDRL(Consumer<Boolean> onAidTlvUpdateConsumer);

    void doTransaccion(TransactionAmountData transactionAmountData, QposParameters qposParameters);

    void doTransaccionNextOperation(TransactionAmountData transactionAmountData, QposParameters qposParameters);

    void cancelOperacion();

    void operacionFinalizada(final String arpc);

    Map<String, String> getIccTags();

    DspreadDevicePOS getDeviePos();

    void deviceCancel();

    Hashtable<String, String> getQposIdHash();

    @Deprecated
    void setReaderEmvConfig(String emvCfgAppHex, String emvCfgCapkHex, Consumer<Boolean> onReturnCustomConfigConsumer);

    void setReaderEmvConfig(String emvXml, Consumer<Boolean> onReturnCustomConfigConsumer);

    int updateFirmware(@NonNull Context context, final byte[] dataToUpdate, final String file);

    QPOSDeviceInfo getDevicePosInfo();

    void showOnDisplay(@NonNull String message, int seconds);

    /**
     * Tipo de Dispositivo
     */
    enum Tipodongle {
        DSPREAD,
        NODSPREAD,
        SUNMI
    }
}
