package com.pagatodo.qposlib.pos;

public class POSConnectionState {

    private STATE_POS mStateQPOS;

    public enum STATE_POS {
        NONE, OPENING, CONNECTED, DISCONECTED, CLOSE, NOT_DETECTED, OCCUPIED, PROCESSING_TRANSACTION, WAITING
    }

    public void updateState(final STATE_POS state) {
        this.mStateQPOS = state;
    }

    public STATE_POS getState() {
        return mStateQPOS;
    }

    public boolean isConnected() {
        return mStateQPOS == STATE_POS.CONNECTED;
    }
}