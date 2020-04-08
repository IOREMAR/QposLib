package com.pagatodo.qposlib.dongleconnect;


import com.pagatodo.qposlib.pos.PosResult;


import java.util.Hashtable;
import java.util.List;

public interface DongleListener {

    void onResultData(final Hashtable<String, String> datosOperacion, DongleListener.DoTradeResult entryMode);

    void onRespuestaDongle(final PosResult result);

    void seleccionEmvApp(final List<String> listEMVApps, AplicacionEmv aplicacionEmv);

    void onSearchMifareCardResult(final Hashtable<String, String> hashtable);//NOSONAR

    boolean checkDoTrade();

    void onBatchReadMifareCardResult(String s, Hashtable<String, List<String>> hashtable);//NOSONAR

    void onBatchWriteMifareCardResult(String s, Hashtable<String, List<String>> hashtable);//NOSONAR

    void onVerifyMifareCardResult(final boolean isFailedChip);

    void onOperateMifareCardResult(final Hashtable<String, String> hashtable);//NOSONAR

    void onErrorWriteMifareCard();

    void onFinishMifareCardResult(boolean finish);

    enum DoTradeResult {
        NONE,
        MCR,
        ICC,
        NOT_ICC,
        BAD_SWIPE,
        NO_RESPONSE,
        NO_UPDATE_WORK_KEY,
        NFC_ONLINE,
        NFC_OFFLINE,
        NFC_DECLINED;

        DoTradeResult() {
        }
    }
}