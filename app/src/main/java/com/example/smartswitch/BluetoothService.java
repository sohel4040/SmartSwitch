package com.example.smartswitch;

import android.annotation.SuppressLint;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.io.IOException;
import java.util.UUID;

public class BluetoothService extends Service {

    private BluetoothAdapter bluetoothAdapter;
    BluetoothSocket socket = null;
    BluetoothDevice device = null;
    private boolean isConnected = false;
    public String MAC_Address;
    static final UUID myUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private BroadcastReceiver bluetoothBroadcast, writeBroadcast;


    @SuppressLint("MissingPermission")
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        MAC_Address = intent.getStringExtra("MAC_Address");

        showNotification();
        initializeBroadcastReceivers();

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        Thread thread = new Thread() {
            @Override
            public void run() {
                device = bluetoothAdapter.getRemoteDevice(MAC_Address);
                try {
                    socket = device.createInsecureRfcommSocketToServiceRecord(myUUID);
                    bluetoothAdapter.cancelDiscovery();

                    try {
                        socket.connect();
                        System.out.println(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>> Socket Connect!");
                        Intent intent = new Intent(getApplicationContext(), MyIntentService.class);
                        intent.putExtra("DATA", "SOCKET_CONNECTED");
                        startService(intent);

                    } catch (IOException e) {
                        socket.close();
                        Intent intent = new Intent(getApplicationContext(), MyIntentService.class);
                        intent.putExtra("DATA", "SOCKET_CONNECTION_FAILED");
                        startService(intent);
                        System.out.println(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>> Socket Connection Failed!");
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        };

        thread.start();

        return super.onStartCommand(intent, flags, startId);
    }

    public void initializeBroadcastReceivers() {
        bluetoothBroadcast  = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction().equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                    int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
                    if (state == BluetoothAdapter.STATE_OFF) {
                        write("OFF");
                        stopForeground(true);
                    }
                }
                else if (intent.getAction().equals(BluetoothDevice.ACTION_ACL_DISCONNECTED)) {
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    @SuppressLint("MissingPermission") String deviceName = device.getName();
                    if(deviceName != null && deviceName.equals("HC-05")) {
                        stopForeground(true);
                    }
                }
            }
        };

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        intentFilter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        registerReceiver(bluetoothBroadcast, intentFilter);

        writeBroadcast = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String data = intent.getStringExtra("DATA");
                if (data.equals("SWITCH_ON"))
                    write("ON");
                else if (data.equals("SWITCH_OFF"))
                    write("OFF");
            }
        };

        LocalBroadcastManager.getInstance(getApplicationContext()).registerReceiver(writeBroadcast, new IntentFilter(MyIntentService.MY_INTENT_SERVICE));
    }

    public void write(String msg) {
        if (socket != null && socket.isConnected()) {
            try {
                socket.getOutputStream().write(msg.getBytes());
                System.out.println(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>> Sent Successfully!");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void showNotification() {

        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this,"channelID")
                .setSmallIcon(R.drawable.icon)
                .setContentTitle("Smart Switch")
                .setContentText("Device Connected")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        int notificationId = 1;
        createChannel(notificationManager);
        notificationManager.notify(notificationId, notificationBuilder.build());

        startForeground(1, notificationBuilder.build());
    }

    public void createChannel(NotificationManager notificationManager){
        if (Build.VERSION.SDK_INT < 26) {
            return;
        }
        NotificationChannel channel = new NotificationChannel("channelID","name", NotificationManager.IMPORTANCE_DEFAULT);
        channel.setDescription("Smart Switch Notification.");
        notificationManager.createNotificationChannel(channel);
    }


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        write("OFF");
        unregisterReceiver(bluetoothBroadcast);
        LocalBroadcastManager.getInstance(getApplicationContext()).unregisterReceiver(writeBroadcast);
    }
}
