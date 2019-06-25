package com.pagatodo.qposlib;

import android.content.Context;

import com.pagatodo.qposlib.abstracts.AbstractDongle;

import java.util.Hashtable;

public class PosInstance {

    private static PosInstance instance;
    public static String AZUL = "AZUL";
    public static String  ROJO = "ROJO";

    private AbstractDongle Dongle;

    private Context AppContext;

    private Hashtable<String, String> sessionKeys;

    private String ColorTema ;

    public String getColorTema() {
        return ColorTema;
    }

    public void setColorTema(String colorTema) {
        ColorTema = colorTema;
    }

    public static PosInstance getInstance() {
        synchronized (PosInstance.class) {
            if (instance == null) {
                instance = new PosInstance();
            }
        }
        return instance;
    }

    public AbstractDongle getDongle() {
        return Dongle;
    }

    public void setDongle(AbstractDongle dongle) {
        Dongle = dongle;
    }

    public Context getAppContext() {
        return AppContext;
    }

    public void setAppContext(Context appContext) {
        AppContext = appContext;
    }

    public Hashtable<String, String> getSessionKeys() {
        return sessionKeys;
    }

    public void setSessionKeys(Hashtable<String, String> sessionKeys) {
        this.sessionKeys = sessionKeys;
    }
}
