package com.pagatodo.qposlib.pos.dspread;

public class HexUtils {
    static final String HEXES = "0123456789ABCDEF";

    public static String byteArray2Hex(byte[] bytes) {
        if (bytes == null) {
            return null;
        } else {
            final StringBuilder hex = new StringBuilder(2 * bytes.length);
            for (final byte raw : bytes) {
                hex.append(HEXES.charAt((raw & 0xF0) >> 4))
                        .append(HEXES.charAt((raw & 0x0F)));
            }
            return hex.toString();
        }
    }
}
