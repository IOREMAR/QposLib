package com.pagatodo.qposlib.pos;

import com.dspread.xpos.QPOSService;

public final class QposParameters {
    public static final int MODE_ICC = 422;
    public static final int MODE_NFC = 632;
    public static final int MODE_MS = 67;

    private QPOSService.CardTradeMode cardTradeMode;
    private String ctlsTransactionLimitValue;
    private String ctlsTransactionCvmLimitValue;
    private String ctlsTransactionFloorLimitValue;

    public void setTradeMode(int tradeMode) {
        if (tradeMode == (QposParameters.MODE_ICC | QposParameters.MODE_NFC)) {
            cardTradeMode = QPOSService.CardTradeMode.TAP_INSERT_CARD;
        } else if (tradeMode == (QposParameters.MODE_ICC | QposParameters.MODE_MS)) {
            cardTradeMode = QPOSService.CardTradeMode.SWIPE_INSERT_CARD;
        } else if (tradeMode == (QposParameters.MODE_ICC)) {
            cardTradeMode = QPOSService.CardTradeMode.ONLY_INSERT_CARD;
        } else if (tradeMode == (QposParameters.MODE_NFC)) {
            cardTradeMode = QPOSService.CardTradeMode.ONLY_TAP_CARD;
        } else if (tradeMode == (QposParameters.MODE_MS)) {
            cardTradeMode = QPOSService.CardTradeMode.ONLY_SWIPE_CARD;
        } else {
            // (MODE_NFC | MODE_MS) is not supported
            cardTradeMode = QPOSService.CardTradeMode.SWIPE_TAP_INSERT_CARD;
        }
    }

    public QPOSService.CardTradeMode getCardTradeMode() {
        return cardTradeMode;
    }

    public String getCtlsTransactionLimitValue() {
        return ctlsTransactionLimitValue;
    }

    public void setCtlsTransactionLimitValue(String ctlsTransactionLimitValue) {
        this.ctlsTransactionLimitValue = ctlsTransactionLimitValue;
    }

    public String getCtlsTransactionCvmLimitValue() {
        return ctlsTransactionCvmLimitValue;
    }

    public void setCtlsTransactionCvmLimitValue(String ctlsTransactionCvmLimitValue) {
        this.ctlsTransactionCvmLimitValue = ctlsTransactionCvmLimitValue;
    }

    public String getCtlsTransactionFloorLimitValue() {
        return ctlsTransactionFloorLimitValue;
    }

    public void setCtlsTransactionFloorLimitValue(String ctlsTransactionFloorLimitValue) {
        this.ctlsTransactionFloorLimitValue = ctlsTransactionFloorLimitValue;
    }


    @Override
    public String toString() {
        return "{\"cardTradeMode\" : " + (cardTradeMode)
                + ",\"ctlsTransactionLimitValue\" : " + (ctlsTransactionLimitValue == null ? null : "\"" + ctlsTransactionLimitValue + "\"")
                + ",\"ctlsTransactionCvmLimitValue\" : " + (ctlsTransactionCvmLimitValue == null ? null : "\"" + ctlsTransactionCvmLimitValue + "\"")
                + ",\"ctlsTransactionFloorLimitValue\" : " + (ctlsTransactionFloorLimitValue == null ? null : "\"" + ctlsTransactionFloorLimitValue + "\"")
                + "}";
    }
}
