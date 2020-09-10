package com.pagatodo.qposlib.emv;

import androidx.annotation.NonNull;
import androidx.core.util.Pair;

public enum EmvTags {
    APPLICATION_CRYPTOGRAM("9F26"),
    APPLICATION_DEDICATED_FILE_NAME("4F"),
    APPLICATION_IDENTIFIER("9F06"),
    APPLICATION_PREFERRED_NAME("9F12"),
    APPLICATION_PRIMARY_ACCOUNT_NUMBER("5A"),
    APPLICATION_USAGE_CONTROL("9F07"),
    CARDHOLDER_NAME("5F20"),
    CARDHOLDER_VERIFICATION_METHOD_LIST("8E"),
    CARDHOLDER_VERIFICATION_RESULTS("9F34"),
    CRYPTOGRAM_INFORMATION_DATA("9F27"),
    ISSUER_APPLICATION_DATA("9F10"),
    ISSUER_ACTION_CODE_DEFAULT("9F0D"),
    ISSUER_ACTION_CODE_DENIAL("9F0E"),
    ISSUER_ACTION_CODE_ONLINE("9F0F"),
    ISSUER_COUNTRY_CODE("5F28"),
    TERMINAL_VERIFICATION_RESULTS("95"),
    TRANSACTION_CURRENCY_EXPONENT("5F36"),
    TRANSACTION_STATUS_INDICATOR("9B");

    private final String hex;

    EmvTags(String hex) {
        this.hex = hex;
    }

    @NonNull
    public static Pair<String, Integer> getAsString(@NonNull EmvTags... emvTags) {
        StringBuilder stringBuilder = new StringBuilder();
        for (EmvTags tag : emvTags) {
            stringBuilder.append(tag.hex);
        }
        return new Pair<>(stringBuilder.toString(), emvTags.length);
    }
}
