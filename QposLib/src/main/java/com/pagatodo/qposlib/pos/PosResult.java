package com.pagatodo.qposlib.pos;

public class PosResult {

    private PosTransactionResult responce;
    private int response;
    private boolean correct;
    private String message;

    public PosResult() {
        //Nothing to do
    }

    public PosResult(final PosTransactionResult enumResponse, final String message, final Boolean isCorrecta) {
        responce = enumResponse;
        this.message = message;
        this.correct = isCorrecta;
    }

    public PosResult(final int response, final String message) {
        this.response = response;
        this.message = message;
    }

    public enum PosTransactionResult {
        FAILKEYS(1),
        APROBADO(0),
        TERMINADO(-4107),
        DECLINADO(-50024),
        CANCELADO(7),
        CAPK_FAIL(1),
        NO_CHIP(3),
        NO_CHIP_FALLBACK(2),
        SELECT_APP_FAIL(8),
        ERROR_DISPOSITIVO(9),
        CARD_NOT_SUPPORTED(-4125),
        MISSING_MANDATORY_DATA(11),
        CARD_BLOCKED_OR_NO_EMV_APPS(12),
        INVALID_ICC_DATA(13),
        FALLBACK(14),
        NFC_TERMINATED(15),
        CARD_REMOVED(16),
        TRADE_LOG_FULL(17),
        TIMEOUT(-50021),
        MAC_ERROR(19),
        CMD_TIEMPOFINALIZADO(20),
        CMD_NOT_AVAILABLE(21),
        DEVICE_RESET(22),
        UNKNOWN(-1),
        DEVICE_BUSY(23),
        INPUT_OUT_OF_RANGE(24),
        INPUT_INVALID_FORMAT(25),
        INPUT_ZERO_VALUES(26),
        INPUT_INVALID(27),
        CASHBACK_NOT_SUPPORTED(28),
        CRC_ERROR(29),
        COMM_ERROR(30),
        WR_DATA_ERROR(31),
        EMV_APP_CFG_ERROR(-50009),
        EMV_CAPK_CFG_ERROR(33),
        APDU_ERROR(34),
        ICC_ONLINE_TIMEOUT(-2801),
        AMOUNT_OUT_OF_LIMIT(36),
        PIN_CANCEL(-50020),
        TRANS_REFUSED(-4000),
        SYNCOPERATION(-4115);

        public final int result;

        PosTransactionResult(int result) {
            this.result = result;
        }
    }

    public PosTransactionResult getResponce() {
        return responce;
    }


    public int getResponse() {
        return response;
    }

    public String getMessage() {
        return message;
    }

    public boolean isCorrect() {
        return correct;
    }
}
