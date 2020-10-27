package com.pagatodo.qposlib.dongleconnect;

public interface DongleConnect {

    void onDeviceConnected();

    void ondevicedisconnected();

    void deviceOnTransaction();

    void onRequestNoQposDetected();

    void onSessionKeysObtenidas();
}
