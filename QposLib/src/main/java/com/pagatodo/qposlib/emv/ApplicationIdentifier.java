package com.pagatodo.qposlib.emv;

import androidx.annotation.NonNull;

import java.util.Locale;

public enum ApplicationIdentifier {
    MASTERCARD_GLOBAL("A0000000041010"),
    VISA_CLASSIC("A0000000031010");

    private final String value;

    ApplicationIdentifier(String value) {
        this.value = value;
    }

    public static String getTlv(@NonNull ApplicationIdentifier aid) {
        String length = String.format(Locale.getDefault(), "%02d", aid.value.length() / 2);
        return "9F06" + length + aid.value;
    }
}
