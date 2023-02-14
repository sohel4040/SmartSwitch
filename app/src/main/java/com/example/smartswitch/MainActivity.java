package com.example.smartswitch;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.app.ActivityCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;
import android.widget.ToggleButton;

import java.util.Set;


public class MainActivity extends AppCompatActivity {

    private boolean connection_toggle;
    private LinearLayout control_panel, manual_view, scheduled_view;
    private BluetoothAdapter bluetoothAdapter;
    private Button connection_button;
    private ToggleButton toggleButton;
    private String MAC_Address;
    private BroadcastReceiver bluetoothBroadcast, progressBroadcast;
    private AlertDialog progressDialog;
    private LayoutInflater progressDialogInflater;
    private View progressDialogView;
    private AlertDialog.Builder progressDialogBuilder;

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);

        initialize();

        initializeBroadcastReceivers();

        restoreLastActivityState();
    }

    public void initialize() {
        control_panel = findViewById(R.id.control_panel);
        manual_view = findViewById(R.id.manual_view);
        scheduled_view = findViewById(R.id.scheduled_view);
        control_panel.setVisibility(View.GONE);
        connection_button = findViewById(R.id.connection_button);
        toggleButton = findViewById(R.id.toggleButton);

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        connection_toggle = true;
        MAC_Address = null;

        progressDialogInflater = LayoutInflater.from(this);
        progressDialogView = progressDialogInflater.inflate(R.layout.custom_progressbar, null);
        progressDialogBuilder = new AlertDialog.Builder(this);
//        progressDialogBuilder.setTitle("Connecting.... Please Wait !");
        progressDialogBuilder.setView(progressDialogView);
        progressDialogBuilder.setCancelable(false);
        progressDialog = progressDialogBuilder.create();
    }

    public void initializeBroadcastReceivers() {
        bluetoothBroadcast = new BroadcastReceiver() {
            @SuppressLint("MissingPermission")
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction().equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                    int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
                    if (state == BluetoothAdapter.STATE_OFF) {
                        control_panel.setVisibility(View.GONE);
                        toggleButton.setChecked(false);
                        toggleButton.setBackgroundResource(R.drawable.off);
                        connection_toggle =  true;
                        connection_button.setText("Connect Device");
                        connection_button.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#26B545")));
                    }
                }
                else if (intent.getAction().equals(BluetoothDevice.ACTION_ACL_DISCONNECTED)) {
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    String deviceName = device.getName();

                    if(deviceName != null && deviceName.equals("HC-05")) {
                        Toast.makeText(context, "Smart Switch Disconnect!", Toast.LENGTH_LONG).show();
                        control_panel.setVisibility(View.GONE);
                        stopBluetoothService(false);
                    }
                }
            }
        };

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        intentFilter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);

        registerReceiver(bluetoothBroadcast, intentFilter);

        progressBroadcast = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String data = intent.getStringExtra("DATA");

                if (data.equals("SOCKET_CONNECTED")) {
                    progressDialog.cancel();
                    control_panel.setVisibility(View.VISIBLE);
                    connection_button.setText("Disconnect Device");
                    connection_button.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#E82222")));
                }
                else if (data.equals("SOCKET_CONNECTION_FAILED")) {
                    progressDialog.cancel();
                    control_panel.setVisibility(View.GONE);
                    Toast.makeText(context, "Connection Failed! Please retry after sometime", Toast.LENGTH_LONG).show();
                    stopBluetoothService(false);
                }
            }
        };

        LocalBroadcastManager.getInstance(getApplicationContext()).registerReceiver(progressBroadcast, new IntentFilter(MyIntentService.MY_INTENT_SERVICE));
    }

    public void restoreLastActivityState() {
        ActivityManager activityManager =  (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for(ActivityManager.RunningServiceInfo service : activityManager.getRunningServices(Integer.MAX_VALUE)) {
            if(BluetoothService.class.getName().equals(service.service.getClassName()) && bluetoothAdapter.isEnabled()) {
                connection_toggle = false;
                connection_button.setText("Disconnect Device");
                connection_button.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#E82222")));

                if(StaticData.SWITCH_STATE.equals("ON")) {
                    toggleButton.setChecked(true);
                    toggleButton.setBackgroundResource(R.drawable.on);
                }
                else {
                    toggleButton.setChecked(false);
                    toggleButton.setBackgroundResource(R.drawable.off);
                }

                control_panel.setVisibility(View.VISIBLE);
            }
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public void connection(View view) {
        if(connection_toggle) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.BLUETOOTH_CONNECT}, 0);
            }

            if (bluetoothAdapter != null) {
                if (!bluetoothAdapter.isEnabled()) {
                    Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(intent, 0);
                }
                else {
                    startBluetoothService();
                }
            }
            else {
                Toast.makeText(this, "Bluetooth not Supported!", Toast.LENGTH_SHORT).show();
            }
        }
        else {
            StaticData.SWITCH_STATE = "OFF";
            control_panel.setVisibility(View.GONE);
            stopBluetoothService(true);
        }
    }

    @SuppressLint("MissingPermission")
    private boolean isSmartSwitchPaired() {
        Set<BluetoothDevice> bt = bluetoothAdapter.getBondedDevices();

        if(bt.size() > 0) {
            for(BluetoothDevice device : bt){
                if(device.getName().equals("HC-05")) {
                    MAC_Address = device.getAddress();
                    return true;
                }
            }
        }
        return false;
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void startBluetoothService() {
        if(!isSmartSwitchPaired()) {
            Toast.makeText(this, "Please pair Smart Switch with this device!", Toast.LENGTH_SHORT).show();
            return;
        }

        progressDialog.show();
        connection_toggle = false;

        if(!isForegroundServiceRunning()) {
            Intent intent = new Intent(this, BluetoothService.class);
            intent.putExtra("MAC_Address", MAC_Address);
            startForegroundService(intent);
        }
    }

    @SuppressLint("MissingPermission")
    private void stopBluetoothService(boolean disableBluetooth) {
        if(bluetoothAdapter.isEnabled() & disableBluetooth)
            bluetoothAdapter.disable();
        Intent intent = new Intent(this, BluetoothService.class);
        stopService(intent);

        toggleButton.setChecked(false);
        toggleButton.setBackgroundResource(R.drawable.off);

        connection_toggle =  true;
        connection_button.setText("Connect Device");
        connection_button.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#26B545")));
    }

    private boolean isForegroundServiceRunning() {
        ActivityManager activityManager =  (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for(ActivityManager.RunningServiceInfo service : activityManager.getRunningServices(Integer.MAX_VALUE)) {
            if(BluetoothService.class.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == 0 && resultCode != 0) {
            startBluetoothService();
            Toast.makeText(this, "Bluetooth Enabled", Toast.LENGTH_SHORT).show();
        }
    }

    public void action(View view) {
        Intent intent = new Intent(this, MyIntentService.class);

        if( toggleButton.isChecked() ) {
            toggleButton.setBackgroundResource(R.drawable.on);
            StaticData.SWITCH_STATE = "ON";
            intent.putExtra("DATA", "SWITCH_ON");
        }
        else {
            toggleButton.setBackgroundResource(R.drawable.off);
            StaticData.SWITCH_STATE = "OFF";
            intent.putExtra("DATA", "SWITCH_OFF");
        }

        startService(intent);
    }

    public void toggle(View view) {
        switch (view.getId()) {
            case R.id.manual:
                scheduled_view.setVisibility(View.GONE);
                manual_view.setVisibility(View.VISIBLE);
                break;
            case R.id.scheduled:
                manual_view.setVisibility(View.GONE);
                scheduled_view.setVisibility(View.VISIBLE);
                break;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(bluetoothBroadcast);
        LocalBroadcastManager.getInstance(getApplicationContext()).unregisterReceiver(progressBroadcast);
    }
}