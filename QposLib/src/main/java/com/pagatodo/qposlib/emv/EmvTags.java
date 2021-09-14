package com.pagatodo.qposlib.emv;

import androidx.annotation.NonNull;
import androidx.core.util.Pair;

public enum EmvTags {
    APPLICATION_CRYPTOGRAM("9F26"),
    APPLICATION_DEDICATED_FILE_NAME("4F"),
    APPLICATION_EXPIRATION_DATE("5F24"),
    APPLICATION_IDENTIFIER("9F06"),
    APPLICATION_INTERCHANGE_PROFILE("82"),
    APPLICATION_LABEL("50"),
    APPLICATION_PREFERRED_NAME("9F12"),
    APPLICATION_PRIORITY_INDICATOR("87"),
    APPLICATION_USAGE_CONTROL("9F07"),
    CARDHOLDER_MOBILE_VERIFICATION_RESULTS("9F71"),
    CARDHOLDER_VERIFICATION_METHOD_LIST("8E"),
    CARDHOLDER_VERIFICATION_RESULTS("9F34"),
    CARD_APPLICATION_VERSION("9F08"),
    CRYPTOGRAM_INFORMATION_DATA("9F27"),
    DEVICE_INFORMATION("9F6E"),
    ISSUER_APPLICATION_DATA("9F10"),
    ISSUER_ACTION_CODE_DEFAULT("9F0D"),
    ISSUER_ACTION_CODE_DENIAL("9F0E"),
    ISSUER_ACTION_CODE_ONLINE("9F0F"),
    ISSUER_COUNTRY_CODE("5F28"),
    TERMINAL_CAPABILITIES("9F33"),
    TERMINAL_VERIFICATION_RESULTS("95"),
    TRACK2_EQUIVALENT_DATA("57"),
    TRANSACTION_STATUS_INDICATOR("9B"),
    MERCHANT_NAME_AND_LOCATION("9F4E"),
    AMOUNT_AUTHORIZED("9F02"),
    AMOUNT_OTHER("9F03"),
    TRANSACTION_CURRENCY_CODE("5F2A"),
    TRANSACTION_CURRENCY_EXPONENT("5F36"),
    TERMINAL_TRANSACTION_QUALIFIERS("9F66"),
    KERNEL_4_READER_CAPABILITIES("9F6D"),
    POS_ENTRY_MODE("9F39")
    ;

    private final String hex;

    EmvTags(String hex) {
        this.hex = hex;
    }

    public final String getHex() {
        return hex;
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
