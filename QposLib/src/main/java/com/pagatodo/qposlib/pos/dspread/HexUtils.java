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

    /*
     * Convert hex value to ascii code
     **/
    public static String convertHexToString(String hex) {
        StringBuilder sb = new StringBuilder();
        StringBuilder temp = new StringBuilder();

        //49204c6f7665204a617661 split into two characters 49, 20, 4c...
        for (int i = 0; i < hex.length() - 1; i += 2) {
            //grab the hex in pairs
            String output = hex.substring(i, (i + 2));
            //convert hex to decimal
            int decimal = Integer.parseInt(output, 16);
            //convert the decimal to character
            sb.append((char) decimal);

            temp.append(decimal);
        }

        return sb.toString();
    }

    /**
     * int 转换为hex的string
     *
     * @param i
     * @return
     */
    public static String intToHexStr(int i) {
        String string = null;
        if (i >= 0 && i < 10) {
            string = "0" + i;
        } else {
            string = Integer.toHexString(i);
        }
        return string;
    }
}
