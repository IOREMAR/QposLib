package com.pagatodo.qposlib;

import android.util.Log;

import static com.pagatodo.qposlib.Logger.Loggers.LOGGER_PRODUCCION;
import static com.pagatodo.qposlib.Logger.Loggers.LOGGER_QA;


public class Logger {

    private Logger(){
    }

    public static privateAppLogger  LOGGER = getInstance();

    public static privateAppLogger getInstance(){
//        final ConfigManager configManager = MposApplication.getInstance().getConfigManager();
        if ("QA".equals("")) {
            return LOGGER_QA;
        } else {
            return LOGGER_PRODUCCION;
        }
    }

    public  enum Loggers implements privateAppLogger {
        LOGGER_QA (){
            @Override
            public void info(String tag, String mensaje) {
                Log.i(tag,mensaje);
            }

            @Override
            public void fine(String tag, String mensaje) {
                Log.i(tag,mensaje);
            }
            @Override
            public void throwing(String tag, int numError, Throwable thrwbl ,String mensaje) {
//                Utilities.appendDeviceInfo();
//                Crashlytics.logException(thrwbl);
                Log.e(tag + " " +numError, mensaje, thrwbl);
            }

        }
        ,
        LOGGER_PRODUCCION(){
            @Override
            public void info(String tag, String mensaje) {
            // NONE
            }

            @Override
            public void fine(String tag, String mensaje) {
                // NONE
            }
            @Override
            public void throwing(String tag, int numError, Throwable thrwbl,String mensaje) {
//                Utilities.appendDeviceInfo();
//                Crashlytics.logException(thrwbl);

            }


        }

    }

  public    interface privateAppLogger {
        void  info(String TAG, String mensaje);
        void  fine(String TAG, String mensaje);
        void throwing(final String TAG, int numError, final Throwable thrwbl, final String mensaje);
    }


}
