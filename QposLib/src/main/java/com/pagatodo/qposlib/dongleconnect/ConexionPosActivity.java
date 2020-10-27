package com.pagatodo.qposlib.dongleconnect;//NOPMD

import android.app.Activity;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import com.cunoraz.gifview.library.GifView;
import com.pagatodo.qposlib.BuildConfig;
import com.pagatodo.qposlib.PosInstance;
import com.pagatodo.qposlib.R;
import com.pagatodo.qposlib.abstracts.AbstractDongle;
import com.pagatodo.qposlib.broadcasts.BroadcastListener;
import com.pagatodo.qposlib.broadcasts.BroadcastManager;
import com.pagatodo.qposlib.pos.dspread.DSpreadDevicePosFactory;

import java.util.HashMap;

import static com.pagatodo.qposlib.Logger.LOGGER;
import static com.pagatodo.qposlib.broadcasts.BroadcastManager.BLUETHOOTH_DESCONECTADO;

//import com.pagatodoholdings.posandroid.utils.UpdateFirmware;

public class ConexionPosActivity extends Activity implements BroadcastListener, DongleConnect { //NOSONAR

    //----------UI-------------------------------------------------------

    //----- Var ----------------------------------------------------------

    public static final String NAME_RSA_PCI = "PCIEM.PEM";
    private static final int BLUETHOOTH_REQUEST = 10;
    private static final int BLUETHOOTH_DEVICES = 11;
    public static final int QPOS_VENDOR_ID = 0x03EB;
    private static final String  COLOR_TEMA =  PosInstance.getInstance().getColorTema();
    private static final String TAG = ConexionPosActivity.class.getSimpleName();

    protected static final int RC_HANDLE_INTERNET_PERM = 1;
    protected static final int RC_HANDLE_BLUETHOOTH_PERM = 2;
    private final CountDownTimer connect_Time = new CountDownTimer(TIMER_LONG, TIMER_END) {
        @Override
        public void onTick(long millisUntilFinished) {
            if (isConnected) {
//                firebaseAnalytics.logEvent(Event.EVENT_DONGLE_CONNECTED.key, null);
                txtStatus.setText(R.string.Dispositivo_Conectado);
                imgLector.setVisibility(View.GONE);
                PosInstance.getInstance().getDongle().getSessionKeys(NAME_RSA_PCI, PosInstance.getInstance().getAppContext());
                connect_Time.cancel();
            }
        }

        @Override
        public void onFinish() {
            if (!isConnected) {
                imgLector.setGifResource(COLOR_TEMA.equals("AZUL") ? R.raw.totem_not_found_blue  : R.raw.totem_not_found_red );
                txtStatus.setText(R.string.Dispositivo_NoEncontrado);
                btnSearch.setEnabled(true);
                PosInstance.getInstance().getDongle().closeCommunication();
                broadcastManager.btDisconnect();
            } else {
                PosInstance.getInstance().getDongle().getSessionKeys(NAME_RSA_PCI, PosInstance.getInstance().getAppContext());
            }
        }
    };
    private static final Long TIMER_LONG = 25000L;
    private static final Long TIMER_TICK = 1000L;
    private static final Long TIMER_END = 5000L;
    private Boolean isConnected = false;

    private BroadcastManager broadcastManager;
    private GifView imgLector;
    private TextView txtStatus;
    private View headerLayout;
    private Button btnSearch;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qpos);
        if(COLOR_TEMA.equals(PosInstance.AZUL)){
            setTheme(R.style.AppThemeBlue);
        }else if(COLOR_TEMA.equals(PosInstance.ROJO)) {
            setTheme(R.style.AppThemeRed);
        }
        initUi();
    }

    protected void initUi() {
        // Inicializa variables yAxis vistas
        broadcastManager = new BroadcastManager(this, this);
        broadcastManager.setDongleListener(this);
        imgLector = findViewById(R.id.img_lector);

        imgLector.setGifResource ( COLOR_TEMA.equals("AZUL") ? R.raw.conectar_totem_blue : R.raw.conectar_totem_red );
        txtStatus = findViewById(R.id.txt_status);
        headerLayout = findViewById(R.id.header_qpos_activity);
        btnSearch = findViewById(R.id.btn_search);
        btnSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View view) {
                broadcastManager.validateBluethoothPermisiion();
                btnSearch.setEnabled(false);
            }
        });

        setReceivers();

        if (checkUSBConnected()) {
            onCheckForUsbDevices();
        }
    }

    private void setReceivers() {
        try {
            final IntentFilter filter = new IntentFilter();
            filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
            filter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
            filter.addAction(UsbManager.EXTRA_PERMISSION_GRANTED);
            filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
            filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
            registerReceiver(broadcastManager, filter);
        } catch (Exception ex) {
            LOGGER.throwing(TAG, 1, ex, "Error al ingresar los Brodcast");
            alertBuilder(true, "Error", ex.getMessage());
        }
    }

    @SuppressWarnings("ConstantConditions")
    protected void onSuccessBluethoothPermissions() {
        final BluetoothAdapter btAdapter = ((BluetoothManager) getSystemService(BLUETOOTH_SERVICE)).getAdapter();
        if (btAdapter != null) {
            if (btAdapter.getState() == BluetoothAdapter.STATE_OFF) {
                final Intent enabler = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enabler, BLUETHOOTH_REQUEST);
            }
            if (btAdapter.getState() == BluetoothAdapter.STATE_ON) {
                final Intent serverIntent = new Intent(this, ListaDispositivos.class);
                startActivityForResult(serverIntent, BLUETHOOTH_DEVICES);
            }
        }
    }

    //----------Override Methods-------------------------------------------------------

    @SuppressWarnings("ConstantConditions")
    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == BLUETHOOTH_REQUEST) {
            onSuccessBluethoothPermissions();
        } else if (requestCode == BLUETHOOTH_DEVICES && data != null) {


            final BluetoothDevice device = data.getParcelableExtra(ListaDispositivos.EXTRA_DEVICE);

            if (device != null) {

                //Pintado Mensaje
                txtStatus.setText(R.string.Conectando_Dispositivo);
                ///
                final AbstractDongle qpos = new DSpreadDevicePosFactory().getDongleDevice(device, PosInterface.Tipodongle.DSPREAD, this, BuildConfig.DEBUG);
                PosInstance.getInstance().setDongle(qpos);
                imgLector.setGifResource(COLOR_TEMA.equals("AZUL") ? R.raw.totem_connecting_blue : R.raw.totem_connecting_red );
                broadcastManager.realizarConexion(qpos);
                startTimer();
            } else {
                btnSearch.setEnabled(true);
            }
        } else {
            btnSearch.setEnabled(true);
        }
    }

    public void onCheckForUsbDevices() {

        final UsbManager usbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
        final HashMap<String, UsbDevice> deviceList;
        final PendingIntent permissionIntent = PendingIntent.getBroadcast(this, 0, new Intent(
                "com.android.example.USB_PERMISSION"), 0);
        final IntentFilter filter = new IntentFilter(BroadcastManager.ACTION_USB_PERMISSION);
        registerReceiver(broadcastManager, filter);

        if (usbManager != null) {

            deviceList = usbManager.getDeviceList();

            final DSpreadDevicePosFactory dSpreadDevicePosFactory = new DSpreadDevicePosFactory();

            for (final UsbDevice device : deviceList.values()) {

                if (device.getVendorId() == QPOS_VENDOR_ID && !usbManager.hasPermission(device)) {

                    usbManager.requestPermission(device, permissionIntent);
                } else if (device.getVendorId() == QPOS_VENDOR_ID) {

                    final AbstractDongle qpos = dSpreadDevicePosFactory.getDongleDevice(device, PosInterface.Tipodongle.DSPREAD, this, BuildConfig.DEBUG);
                    broadcastManager.realizarConexion(qpos);
                    break;
                }
            }
        }
    }

    @Override
    public void onRecive(final String bundle) {

        switch (bundle) {

            case BroadcastManager.BLUETHOOTH_PERMISOS:
                onSuccessBluethoothPermissions();
                break;
            case BroadcastManager.USB_CONECTADO:
                onCheckForUsbDevices();
                break;
            case BroadcastManager.USB_ERROR_CONECTAR:
                txtStatus.setText(R.string.Conectando_Dispositivo);
//                imgLector.setGifResource(R.attr.totem_not_found_gif);
                break;
            case BroadcastManager.CONECTANDO_DISPOSITIVO:
                txtStatus.setText(R.string.Conectando_Dispositivo);
                imgLector.setVisibility(View.GONE);
                connect_Time.start();
                break;
            case BLUETHOOTH_DESCONECTADO:
                if (PosInstance.getInstance().getDongle() != null) {
                    PosInstance.getInstance().getDongle().closeCommunication();
                    broadcastManager.btDisconnect();
                    finishApp();
                }
            default:
                break;
        }
    }

    private void finishApp() {
        PosInstance.getInstance().getDongle().closeCommunication();
        finishAffinity();
        restaurarEnLugarDeNada();
    }

    @Override
    public void ondevicedisconnected() {

    }

    @Override
    public void onDeviceConnected() {
        isConnected = true;
    }

    @Override
    public void deviceOnTransaction() {

    }

    @Override
    public void onSessionKeysObtenidas() {
        Intent data = new Intent();
        setResult(RESULT_OK, data);
        finish();
//        cambiaDeActividad(LoginPCIActivity.class);
    }

    public void restaurarEnLugarDeNada() {
        Intent data = new Intent();
        setResult(RESULT_CANCELED, data);
        finishAffinity();
        android.os.Process.killProcess(android.os.Process.myPid());
        System.exit(0);
    }

    @Override
    public void onRequestNoQposDetected() {
        //  isConnected = false;

    }

    private boolean checkUSBConnected() {
        final Intent intent = registerReceiver(null, new IntentFilter("android.hardware.usb.action.USB_STATE"));
        if (intent != null) {
            return intent.getExtras().getBoolean("connected");
        } else {
            return false;
        }
    }

    private void alertBuilder(final boolean esDeError, final String cabecera, final String cuerpo) {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(
                this);

        // set title
        alertDialogBuilder.setTitle(cabecera);

        if (!esDeError) {
            // set dialog message
            alertDialogBuilder
                    .setMessage(cuerpo)
                    .setCancelable(false)
                    .setPositiveButton("Aceptar", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            // if this button is clicked, close
                            // current activity

                            ConexionPosActivity.this.finish();
                        }
                    });
        } else {

            alertDialogBuilder
                    .setMessage(cuerpo)
                    .setCancelable(false)
                    .setPositiveButton("Aceptar", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            // if this button is clicked, close
                            // current activity

                            ConexionPosActivity.this.finish();
                        }
                    })
                    .setNegativeButton("No", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            // if this button is clicked, just close
                            // the dialog box and do nothing
                            dialog.cancel();
                        }
                    });
        }

        // create alert dialog
        AlertDialog alertDialog = alertDialogBuilder.create();

        // show it
        alertDialog.show();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        Intent data = new Intent();
        setResult(RESULT_CANCELED, data);
        finish();
    }

    private void startTimer() {
        connect_Time.start();
    }
}
