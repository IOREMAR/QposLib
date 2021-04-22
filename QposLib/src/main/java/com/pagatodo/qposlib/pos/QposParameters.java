package com.pagatodo.qposlib.pos;

import com.dspread.xpos.QPOSService;

public final class QposParameters {
    public static final int MODE_ICC = 422;
    public static final int MODE_NFC = 632;
    public static final int MODE_MS = 67;
    private int exponent = 0;
    private QPOSService.CardTradeMode cardTradeMode;

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

    public int getExponent() {
        return exponent;
    }

    public void setExponent(int exponent) {
        this.exponent = exponent;
    }

    @Override
    public String toString() {
        return "{" +
                "exponent=" + exponent +
                ", cardTradeMode=" + cardTradeMode +
                '}';
    }
}