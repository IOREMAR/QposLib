package com.pagatodo.qposlib.dongleconnect;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.pagatodo.qposlib.PosInstance;
import com.pagatodo.qposlib.R;



import java.util.LinkedHashMap;

import java.util.Map;
import java.util.Set;

public class ListaDispositivos extends Activity { //NOSONAR

    public static final String EXTRA_DEVICE = "EXTRA_DEVICE";
    private static final String MPOS_NAME = "MPOS";
    private BluetoothAdapter mBtAdapter;
    private Map<String, BluetoothDevice> mDevicesList;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if( PosInstance.getInstance().getColorTema().equals(PosInstance.AZUL)){
            setTheme(R.style.AppThemeBlue);
        }else if(PosInstance.getInstance().getColorTema().equals(PosInstance.ROJO)) {
            setTheme(R.style.AppThemeRed);
        }

        setContentView(R.layout.device_list);
        iniciaUI();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (mBtAdapter != null) {
            mBtAdapter.cancelDiscovery();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        iniciaUI();
    }

    protected void iniciaUI() {

        final ArrayAdapter<String> mPairedDevicesArrayAdapter = new ArrayAdapter<>(this, R.layout.device_name);
        mDevicesList = new LinkedHashMap<>();
        final ListView pairedListView = findViewById(R.id.paired_devices);
        pairedListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(final AdapterView<?> parent, final View view, final int position, final long idItem) {

                mBtAdapter.cancelDiscovery();
                final String info = ((TextView) view).getText().toString();
                final BluetoothDevice abstractDevicePOS = mDevicesList.get(info);

                if (abstractDevicePOS != null) {

                    final Intent intent = new Intent();
                    intent.putExtra(EXTRA_DEVICE, abstractDevicePOS);
                    setResult(Activity.RESULT_OK, intent);
                    finish();
                } else {

                    final Intent settintIntent = new Intent(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS);
                    startActivity(settintIntent);
                }
            }
        });

        mBtAdapter = BluetoothAdapter.getDefaultAdapter();
        final Set<BluetoothDevice> pairedDevices = mBtAdapter.getBondedDevices();

        if (!pairedDevices.isEmpty()) {

            for (final BluetoothDevice device : pairedDevices) {

                if (device.getName().contains(MPOS_NAME)) {

                    mDevicesList.put(device.getName() + "\n" + device.getAddress(), device);
                }
            }
        }

        mDevicesList.put(getResources().getString(R.string.scan_bt_device), null);
        mPairedDevicesArrayAdapter.addAll(mDevicesList.keySet());
        pairedListView.setAdapter(mPairedDevicesArrayAdapter);
    }


    @Override
    public void onBackPressed() {
        super.onBackPressed();
        realizaAlPresionarBack();
    }

    protected void realizaAlPresionarBack() {
        finish();
    }


}
