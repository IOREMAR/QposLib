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
    private String ctlsTransactionAboveCvmLimitCapabilities;
    private String ctlsTransactionUnderCvmLimitCapabilities;
    private String defaultTransactionTerminalQualifiers;
    private String ics;

    public String getIcs() {
        return ics;
    }

    /**
     * @param ics TagValue length: 16
     */
    public void setIcs(String ics) {
        this.ics = ics;
    }

    public String getCtlsTransactionAboveCvmLimitCapabilities() {
        return ctlsTransactionAboveCvmLimitCapabilities;
    }

    /**
     * @param ctlsTransactionAboveCvmLimitCapabilities TagValue length: 1
     */
    public void setCtlsTransactionAboveCvmLimitCapabilities(String ctlsTransactionAboveCvmLimitCapabilities) {
        this.ctlsTransactionAboveCvmLimitCapabilities = ctlsTransactionAboveCvmLimitCapabilities;
    }

    public String getCtlsTransactionUnderCvmLimitCapabilities() {
        return ctlsTransactionUnderCvmLimitCapabilities;
    }

    /**
     * @param ctlsTransactionUnderCvmLimitCapabilities TagValue length: 1
     */
    public void setCtlsTransactionUnderCvmLimitCapabilities(String ctlsTransactionUnderCvmLimitCapabilities) {
        this.ctlsTransactionUnderCvmLimitCapabilities = ctlsTransactionUnderCvmLimitCapabilities;
    }

    public String getTransactionTerminalDefaultQualifiers() {
        return defaultTransactionTerminalQualifiers;
    }

    /**
     * @param transactionTerminalDefaultQualifiers TagValue length: 4
     */
    public void setDefaultTransactionTerminalQualifiers(String transactionTerminalDefaultQualifiers) {
        this.defaultTransactionTerminalQualifiers = transactionTerminalDefaultQualifiers;
    }

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

    /**
     * @param ctlsTransactionLimitValue TagValue length: 6
     */
    public void setCtlsTransactionLimitValue(String ctlsTransactionLimitValue) {
        this.ctlsTransactionLimitValue = ctlsTransactionLimitValue;
    }

    public String getCtlsTransactionCvmLimitValue() {
        return ctlsTransactionCvmLimitValue;
    }

    /**
     * @param ctlsTransactionCvmLimitValue TagValue length: 6
     */
    public void setCtlsTransactionCvmLimitValue(String ctlsTransactionCvmLimitValue) {
        this.ctlsTransactionCvmLimitValue = ctlsTransactionCvmLimitValue;
    }

    public String getCtlsTransactionFloorLimitValue() {
        return ctlsTransactionFloorLimitValue;
    }

    /**
     * @param ctlsTransactionFloorLimitValue TagValue length: 6
     */
    public void setCtlsTransactionFloorLimitValue(String ctlsTransactionFloorLimitValue) {
        this.ctlsTransactionFloorLimitValue = ctlsTransactionFloorLimitValue;
    }

    @Override
    public String toString() {
        return "{\"cardTradeMode\" : " + (cardTradeMode)
                + ",\"ctlsTransactionLimitValue\" : " + (ctlsTransactionLimitValue == null ? null : "\"" + ctlsTransactionLimitValue + "\"")
                + ",\"ctlsTransactionCvmLimitValue\" : " + (ctlsTransactionCvmLimitValue == null ? null : "\"" + ctlsTransactionCvmLimitValue + "\"")
                + ",\"ctlsTransactionFloorLimitValue\" : " + (ctlsTransactionFloorLimitValue == null ? null : "\"" + ctlsTransactionFloorLimitValue + "\"")
                + ",\"ctlsTransactionAboveCvmLimitCapabilities\" : " + (ctlsTransactionAboveCvmLimitCapabilities == null ? null : "\"" + ctlsTransactionAboveCvmLimitCapabilities + "\"")
                + ",\"ctlsTransactionUnderCvmLimitCapabilities\" : " + (ctlsTransactionUnderCvmLimitCapabilities == null ? null : "\"" + ctlsTransactionUnderCvmLimitCapabilities + "\"")
                + ",\"defaultTransactionTerminalQualifiers\" : " + (defaultTransactionTerminalQualifiers == null ? null : "\"" + defaultTransactionTerminalQualifiers + "\"")
                + ",\"ics\" : " + (ics == null ? null : "\"" + ics + "\"") + "}";
    }
}