package com.pagatodo.qposlib.pos;

import androidx.annotation.Nullable;

import com.dspread.xpos.QPOSService;

import java.math.BigDecimal;

public final class QposParameters {
    public static final int MODE_ICC = 422;
    public static final int MODE_NFC = 632;
    public static final int MODE_MS = 67;

    private int exponent = 0;
    @Nullable
    private BigDecimal amount = BigDecimal.ZERO;
    @Nullable
    private BigDecimal cashback = BigDecimal.ZERO;
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

    @Nullable
    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    @Nullable
    public BigDecimal getCashback() {
        return cashback;
    }

    public void setCashback(BigDecimal cashback) {
        this.cashback = cashback;
    }

    @Override
    public String toString() {
        return "{" +
                "exponent=" + exponent +
                ", amount=" + amount +
                ", cashback=" + cashback +
                ", cardTradeMode=" + cardTradeMode +
                '}';
    }
}