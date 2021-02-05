package com.pagatodo.qposlib.broadcasts;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import com.pagatodo.qposlib.BuildConfig;
import com.pagatodo.qposlib.PosInstance;
import com.pagatodo.qposlib.abstracts.AbstractDongle;
import com.pagatodo.qposlib.dongleconnect.DongleConnect;
import com.pagatodo.qposlib.dongleconnect.PosInterface;
import com.pagatodo.qposlib.pos.dspread.DSpreadDevicePosFactory;

import static com.pagatodo.qposlib.Logger.LOGGER;
import static com.pagatodo.qposlib.dongleconnect.ConexionPosActivity.QPOS_VENDOR_ID;

public class BroadcastManager extends BroadcastReceiver {

    private static final String BLUETOOTH_CLASSDONGLE_GENERIC = "40424";
    private BroadcastListener activityCallback;
    private Activity activityCaller;
    private DongleConnect dongleListener;
    protected static final int RC_HANDLE_BLUETHOOTH_PERM = 2;

    // BUNDLE PARAMS
    public static final String USB_CONECTADO = "USB_CONECTADO";
    public static final String BLUETHOOTH_CONNECTADO = "BLUETHOOTH_CONNECTADO";
    public static final String BLUETHOOTH_PERMISOS = "BLUETHOOTH_CONNECTADO";
    public static final String USB_DESCONECTADO = "USB_DESCONECTADO";
    public static final String BLUETHOOTH_DESCONECTADO = "BLUETHOOTH_DESCONECTADO";
    public static final String USB_ERROR_CONECTAR = "USB_ERROR_CONECTAR";
    public static final String BLUETHOOTH_ERRRO_CONECTAR = "BLUETHOOTH_ERRRO_CONECTAR";
    public static final String ERROR_BROADCAST = "ERROR_BROADCAST";
    public static final String ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION";
    public static final String CONECTANDO_DISPOSITIVO = "CONECTANDO_DISPOSITIVO";
    public static final String USB_DEVICE = "USB_DEVICE";
    public static final String BLUETOOTH_DEVICE = "BLUETOOTH_DEVICE";
    private static final String TAG = BroadcastManager.class.getSimpleName();

    private AbstractDongle qpos;

    public BroadcastManager() {
        //none
    }

    public BroadcastManager(final Activity activitycontext, final BroadcastListener activityListener) {
        activityCaller = activitycontext;
        activityCallback = activityListener;
    }
    // SpecificBluethooth

    public DongleConnect getDongleListener() {
        return dongleListener;
    }

    public void setDongleListener(final DongleConnect dongleListener) {
        this.dongleListener = dongleListener;
    }

    @Override
    public void onReceive(final Context context, @NonNull final Intent intent) {

        switch (intent.getAction()) {

            case ACTION_USB_PERMISSION:
                usbPermission(intent);
                break;

            case UsbManager.ACTION_USB_DEVICE_DETACHED:
                usbDeviceDisconnect(intent);
                break;

            case UsbManager.ACTION_USB_DEVICE_ATTACHED:
                connectUSB();
                break;

            case BluetoothAdapter.ACTION_STATE_CHANGED:
                bluetoothChange(intent);
                break;

            case BluetoothDevice.ACTION_ACL_CONNECTED:
                activityCallback.onRecive(BroadcastManager.CONECTANDO_DISPOSITIVO);
                break;

            case BluetoothDevice.ACTION_ACL_DISCONNECTED:
                final BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

                //BLUETOOTH_CLASSDONGLE_GENERIC es una variable hexadecimal que utiliza la tecnologia Bluetooth para identificar
                //diferentes tipos de Dispositivos y agruparlos en base a un codigo.
                if (device.getBluetoothClass().toString().equals(BLUETOOTH_CLASSDONGLE_GENERIC) && device.getName().contains("MPOS")) {

                    aclDisconnected();
                }
                break;

            default:
                LOGGER.info(TAG, intent.getAction());
                break;
        }
    }

    private void aclDisconnected() {
        // ImplPOs
        if (PosInstance.getInstance().getDongle() != null) {
            PosInstance.getInstance().getDongle().closeCommunication();
//            dongleListener.ondevicedisconnected();
            activityCallback.onRecive(BLUETHOOTH_DESCONECTADO);
        }

        LOGGER.throwing(TAG, 1, new Throwable("Bluetooth STATE_OFF"), "Bluetooth STATE_OFF");
    }

    private void bluetoothChange(@NonNull final Intent intent) {
        final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);

        if (state == BluetoothAdapter.STATE_OFF) {
            if (qpos != null) {
                qpos.closeCommunication();
            }
            LOGGER.throwing(TAG, 1, new Throwable("Bluetooth STATE_OFF"), "Bluetooth STATE_OFF");
        }
    }

    private void connectUSB() {
        synchronized (this) {
            activityCallback.onRecive(USB_CONECTADO);
        }
    }

    private void usbDeviceDisconnect(final Intent extraIntent) {

        final UsbDevice device = extraIntent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
        if (device.getVendorId() == QPOS_VENDOR_ID && PosInstance.getInstance().getDongle() != null) {
            PosInstance.getInstance().getDongle().closeCommunication();
            dongleListener.ondevicedisconnected();
        }
    }

    private void usbPermission(@NonNull final Intent intent) {
        synchronized (this) {
            final UsbDevice usbDevice = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);

            if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                if (usbDevice != null) {
                    qpos = new DSpreadDevicePosFactory().getDongleDevice(usbDevice, PosInterface.Tipodongle.DSPREAD, dongleListener, BuildConfig.DEBUG);
                    PosInstance.getInstance().setDongle(qpos);
                    realizarConexion(PosInstance.getInstance().getDongle());
                } else {
                    activityCallback.onRecive(USB_ERROR_CONECTAR);
                    LOGGER.throwing(TAG, 1, new Throwable(), "Es necesario ");
                }
            }
        }
    }

    public void validateBluethoothPermisiion() {
        final int rcBluethooth = ActivityCompat.checkSelfPermission(activityCaller, Manifest.permission.BLUETOOTH);
        if (rcBluethooth == PackageManager.PERMISSION_GRANTED) {
            activityCallback.onRecive(BLUETHOOTH_PERMISOS);
        } else {
            requestBluethoothPermissions();
        }
    }

    private void requestBluethoothPermissions() {
        final String[] permissions = new String[]{Manifest.permission.BLUETOOTH};
        if (!ActivityCompat.shouldShowRequestPermissionRationale(activityCaller, Manifest.permission.BLUETOOTH)) {
            ActivityCompat.requestPermissions(activityCaller, permissions, RC_HANDLE_BLUETHOOTH_PERM);
        }
    }

    public void realizarConexion(final AbstractDongle device) {
        if (device != null) {
            device.openCommunication();
        } else {
            activityCallback.onRecive(USB_ERROR_CONECTAR);
        }
    }

    public void btDisconnect() {
        try {
            BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            bluetoothAdapter.disable();
        } catch (Throwable exe) {
            LOGGER.throwing(TAG, 1, exe, exe.getCause().toString());
        }
    }
}