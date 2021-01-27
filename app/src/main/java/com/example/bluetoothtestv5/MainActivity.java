package com.example.bluetoothtestv5;

import androidx.appcompat.app.AppCompatActivity;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {
    private Button btnServerOn;
    private Button btnConnectToServer;
    private Button btnDisconnect;
    private Button btnPairedDevices;
    private ListView listPairedDevices;

    private AcceptThread acceptThread;
    private ConnectThread connectThread;

    private static final UUID MY_UUID_SECURE =
            UUID.fromString("fa87c0d0-afac-11de-8a39-0800200c9a66");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            Toast.makeText(getApplicationContext(),"不支援藍芽",Toast.LENGTH_LONG).show();
        }

        if (!bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent,1);
        }

        btnServerOn = (Button)findViewById(R.id.btnServerOn);
        btnConnectToServer = (Button)findViewById(R.id.btnConnectOn);
        btnDisconnect = (Button)findViewById(R.id.btnDisconnect);
        btnPairedDevices =  (Button)findViewById(R.id.btnPairedDevice);

        btnDisconnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                connectThread.cancel();
            }
        });

        btnServerOn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                acceptThread = new AcceptThread();
                acceptThread.start();
            }
        });

        btnPairedDevices.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
                ArrayList<String>  deviceList = new ArrayList<String>();
                ArrayList<String> addressList = new ArrayList<String>();
                if (pairedDevices.size() > 0) {
                    // There are paired devices. Get the name and address of each paired device.
                    for (BluetoothDevice device : pairedDevices) {
                        String deviceName = device.getName();
                        String deviceHardwareAddress = device.getAddress(); // MAC address
                        deviceList.add(deviceName + " " + deviceHardwareAddress);
                        addressList.add(deviceHardwareAddress);
                    }
                }
                ArrayAdapter adapter = new ArrayAdapter<String>(getApplicationContext(), android.R.layout.simple_list_item_1,deviceList);
                listPairedDevices = (ListView)findViewById(R.id.listPairedDevices);
                listPairedDevices.setAdapter(adapter);
                listPairedDevices.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                        String address = addressList.get(position);
                        connectThread = new ConnectThread(address);
                        connectThread.start();
                    }
                });
            }
        });




    }

    private class AcceptThread extends Thread {
        private final BluetoothServerSocket mmServerSocket;
        private BluetoothAdapter bluetoothAdapter;
        public AcceptThread() {
            BluetoothServerSocket tmp = null;
            bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            try {
                tmp = bluetoothAdapter.listenUsingRfcommWithServiceRecord("john's device", MY_UUID_SECURE);
            } catch (IOException e) {
                Log.e("AcceptThread", "Socket listen() 方法 失敗", e);
            }

            mmServerSocket = tmp;

        }

        public void run() {
            BluetoothSocket socket = null;
            while (true) {
                try {
                    socket = mmServerSocket.accept();

                } catch(IOException e) {
                    Log.e("AcceptThread", "Socket's accept() 方法 失敗", e);
                    break;
                }

                if (socket != null) {
                    try {
                        mmServerSocket.close();
                    } catch (IOException e) {
                        Log.e("AcceptThread", "Socket's close() 方法 失敗", e);
                    }
                    break;
                }
            }
            Looper.prepare();
            Toast.makeText(getBaseContext(),"藍芽連接已建立成功", Toast.LENGTH_LONG).show();
            Looper.loop();
            Log.d("john", "AcceptThread: 藍芽連接已建立成功");
        }
    }

    private class ConnectThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;
        private BluetoothAdapter mmbluetoothAdapter;

        public ConnectThread(String macAddress) {
            mmbluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            BluetoothSocket tmp = null;
            mmDevice = mmbluetoothAdapter.getRemoteDevice(macAddress);
            try {
                tmp = mmDevice.createInsecureRfcommSocketToServiceRecord(MY_UUID_SECURE);
            } catch (IOException e) {
                Log.e("ConnectThread", "Socket's create() 方法成功", e);
            }
            mmSocket = tmp;

        }

        public void run() {
            mmbluetoothAdapter.cancelDiscovery();

            try {
                mmSocket.connect();
                Looper.prepare();
                Toast.makeText(getBaseContext(),"連接至" + mmDevice, Toast.LENGTH_LONG).show();
                Looper.loop();

            } catch (IOException eConnect) {
                Log.e("ConnectThread", "連接失敗", eConnect);
                try {
                    mmSocket.close();
                } catch (IOException eClose) {
                    Log.e("ConnectThread", "無法關閉 client socket", eClose);
                }
            }
        }

        public void cancel() {
            try {
                mmSocket.close();
                Toast.makeText(getBaseContext(),"藍芽連線關閉" , Toast.LENGTH_LONG).show();
            } catch (IOException e) {
                Log.e("ConnectThread", "無法關閉 client socket", e);
            }

        }
    }

}