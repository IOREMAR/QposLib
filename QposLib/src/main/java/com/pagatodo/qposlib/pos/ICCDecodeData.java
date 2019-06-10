package com.pagatodo.qposlib.pos;

public enum ICCDecodeData {
    ENC_PAN("encPAN"),
    ENC_TRACK_1("encTrack1"),
    ENC_TRACK_2("encTrack2"),
    ENC_TRACK_3("encTrack3"),
    PIN_BLOCK("pinBlock"),
    TLV("tlv"),
    ICC_DATA("iccdata"),
    EXPIRE_DATE("expiryDate");

    private String label;

    ICCDecodeData(final String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}