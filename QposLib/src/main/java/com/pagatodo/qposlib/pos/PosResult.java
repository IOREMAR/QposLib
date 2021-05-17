package com.pagatodo.qposlib.pos;

public class PosResult {

    private PosTransactionResult responce;
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

    public enum PosTransactionResult {
        FAILKEYS,
        APROBADO,
        TERMINADO,
        DECLINADO,
        CANCELADO,
        CAPK_FAIL,
        NO_CHIP,
        NO_CHIP_FALLBACK,
        SELECT_APP_FAIL,
        ERROR_DISPOSITIVO,
        CARD_NOT_SUPPORTED,
        MISSING_MANDATORY_DATA,
        CARD_BLOCKED_OR_NO_EMV_APPS,
        INVALID_ICC_DATA,
        FALLBACK,
        NFC_TERMINATED,
        NFC_DECLINED,
        CARD_REMOVED,
        TRADE_LOG_FULL,
        TIMEOUT,
        MAC_ERROR,
        CMD_TIEMPOFINALIZADO,
        CMD_NOT_AVAILABLE,
        DEVICE_RESET,
        UNKNOWN,
        DEVICE_BUSY,
        INPUT_OUT_OF_RANGE,
        INPUT_INVALID_FORMAT,
        INPUT_ZERO_VALUES,
        INPUT_INVALID,
        CASHBACK_NOT_SUPPORTED,
        CRC_ERROR,
        COMM_ERROR,
        WR_DATA_ERROR,
        EMV_APP_CFG_ERROR,
        EMV_CAPK_CFG_ERROR,
        APDU_ERROR,
        ICC_ONLINE_TIMEOUT,
        AMOUNT_OUT_OF_LIMIT,
        TRY_ANOTHER_INTERFACE,
        AID_BLOCKED,
        SEE_PHONE;

        PosTransactionResult() {
        }
    }

    public PosTransactionResult getResponce() {
        return responce;
    }

    public String getMessage() {
        return message;
    }

    public boolean isCorrect() {
        return correct;
    }

    @Override
    public String toString() {
        return "{" +
                "response=" + responce +
                ", correct=" + correct +
                ", message='" + message + '\'' +
                '}';
    }
}
