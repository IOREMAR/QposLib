package com.pagatodo.qposlib.emv;

import androidx.annotation.NonNull;

import java.util.Locale;

public enum ApplicationIdentifier {
    AMERICAN_EXPRESS("A00000002501"),
    DISCOVER("A0000001523010"),
    MAESTRO_DEBIT("A0000000043060"),
    MASTERCARD_GLOBAL("A0000000041010"),
    MASTERCARD_MAESTRO("A0000000042203"),
    UNION_PAY("A0000003330101"),
    VISA_CLASSIC("A0000000031010"),
    VISA_ELECTRON("A0000000032010"),
    VISA_INTERLINK("A0000000033010");

    private final String value;

    ApplicationIdentifier(String value) {
        this.value = value;
    }

    public static String getTlv(@NonNull ApplicationIdentifier aid) {
        String length = String.format(Locale.getDefault(), "%02d", aid.value.length() / 2);
        return "9F06" + length + aid.value;
    }
}