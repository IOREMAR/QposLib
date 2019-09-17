package com.pagatodo.qposlib.pos;

public enum ICCDecodeData {
    ENC_PAN("5A"),
    ENC_TRACK_1("9F1F"),
    ENC_TRACK_2("57"),
    ENC_TRACK_3("58"),
    PIN_BLOCK("99"),
    TLV("tlv"),
    ICC_DATA("iccdata"),
    EXPIRE_DATE("5F24"),
    CARDHOLDER_NAME("5F20"),
    SERVICE_CODE("5F30");

    private String label;

    ICCDecodeData(final String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}