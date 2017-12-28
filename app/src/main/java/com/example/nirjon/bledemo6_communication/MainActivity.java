package com.example.nirjon.bledemo6_communication;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.os.ParcelUuid;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    Switch sw1, sw2, sw3, sw4;
    TextView tv1, tv2, tv3, tv4, tv5, tv6;

    boolean fromcentral, fromperipheral;

    String myServiceUUIDstring = "EC505EFD-75B9-44EB-8F2A-6FE0B41E7264";
    ParcelUuid myServiceUUID = new ParcelUuid(UUID.fromString(myServiceUUIDstring));

    String myCharacteristicUUIDstring = "CAEA7C3A-A09B-4A92-9EA7-2FC6F56DE666";
    ParcelUuid myCharacteristicUUID = new ParcelUuid(UUID.fromString(myCharacteristicUUIDstring));

    String my2ndCharacteristicUUIDstring = "123873DD-D9A3-4F45-AB90-0D372859553A";
    ParcelUuid my2ndCharacteristicUUID = new ParcelUuid(UUID.fromString(my2ndCharacteristicUUIDstring));

    /*Advertisement Broadcast*/
    BluetoothManager myManager;
    BluetoothAdapter myAdapter;
    BluetoothLeAdvertiser myAdvertiser;
    AdvertiseSettings myAdvertiseSettings;
    AdvertiseData myAdvertiseData;
    AdvertiseCallback myAdvertiseCallback = new AdvertiseCallback() {
        @Override
        public void onStartSuccess(AdvertiseSettings settingsInEffect) {
            super.onStartSuccess(settingsInEffect);
            Log.v("Tag","Success to start advertise: " + settingsInEffect.toString());
        }

        @Override
        public void onStartFailure(int errorCode) {
            super.onStartFailure(errorCode);
            tv1.setText("Failed to start advertisement: errorcode = " + errorCode);
            tv1.invalidate();
        }
    };

    /*Scan for Advertisement*/
    BluetoothLeScanner myScanner;
    ScanSettings myScanSettings;
    ScanFilter myScanFilter;
    List<ScanFilter> myScanFilterList;
    ScanCallback myScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
            if(result == null) return;
            if(result.getDevice() == null) return;
            tv2.setText("Found: " + result.getDevice().toString());
            Log.v("Tag", "Found " + result.getDevice().toString());
            myGatt = result.getDevice().connectGatt(getApplicationContext(), false, myGattCallback);
            myScanner.stopScan(this);
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            super.onBatchScanResults(results);
        }

        @Override
        public void onScanFailed(int errorCode) {
            super.onScanFailed(errorCode);
        }
    };

    /*GATT Server*/
    BluetoothDevice myRemoteClientDevice;
    BluetoothGattServer myGattServer;
    BluetoothGattService myGattService;
    BluetoothGattCharacteristic myGattCharateristic, my2ndGattCharacteristic;
    BluetoothGattServerCallback myGattServerCallback = new BluetoothGattServerCallback() {
        @Override
        public void onConnectionStateChange(BluetoothDevice device, int status, int newState) {
            super.onConnectionStateChange(device, status, newState);

            myRemoteClientDevice = device;

            final int ns = newState;
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    tv1.setText("GATT Server connection state changed: " + ns);
                    tv1.invalidate();
                    if(ns == BluetoothGatt.STATE_CONNECTED){
                        sw3.setEnabled(true);
                        tv3.setText("This Peripheral/Server Sent: none");
                        tv4.setText("This Peripheral/Server Recv: none");
                        tv3.invalidate();
                        tv4.invalidate();
                    }
                    else{
                        sw3.setEnabled(false);
                    }
                }
            });
            Log.v("Tag", "GATT Server connection state changed: " + newState);

        }

        @Override
        public void onServiceAdded(int status, BluetoothGattService service) {
            super.onServiceAdded(status, service);
        }

        @Override
        public void onCharacteristicReadRequest(BluetoothDevice device, int requestId, int offset, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicReadRequest(device, requestId, offset, characteristic);
        }

        @Override
        public void onCharacteristicWriteRequest(BluetoothDevice device, int requestId, BluetoothGattCharacteristic characteristic, boolean preparedWrite, boolean responseNeeded, int offset, byte[] value) {
            super.onCharacteristicWriteRequest(device, requestId, characteristic, preparedWrite, responseNeeded, offset, value);

            if(characteristic.getUuid().equals(myCharacteristicUUID.getUuid())){

                myGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, 0, null);
                Log.v("Tag", "Server got write request and sending SUCCESS to client");

                try {
                    final String cmess = new String(value, "UTF-8");
                    final String smess = new String("ACK:" + cmess);
                    byte[] bmess = smess.getBytes();
                    characteristic.setValue(bmess);
                    myGattServer.notifyCharacteristicChanged(device, characteristic, false);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            tv5.setText("This Peripheral Sent: " + smess);
                            tv6.setText("This Peripheral Recv: " + cmess);
                            tv5.invalidate();
                            tv6.invalidate();
                        }
                    });
                }catch (Exception ex){
                    ex.printStackTrace();
                }
            }
            else if(characteristic.getUuid().equals(my2ndCharacteristicUUID.getUuid())){

                myGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, 0, null);
                Log.v("Tag", "Server got write request and sending SUCCESS to client");

                try {
                    final String cmess = new String(value, "UTF-8");
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            tv4.setText("This Peripheral Recv: " + cmess);
                            tv4.invalidate();
                        }
                    });
                }catch (Exception ex){
                    ex.printStackTrace();
                }
            }
        }

        @Override
        public void onDescriptorReadRequest(BluetoothDevice device, int requestId, int offset, BluetoothGattDescriptor descriptor) {
            super.onDescriptorReadRequest(device, requestId, offset, descriptor);
        }

        @Override
        public void onDescriptorWriteRequest(BluetoothDevice device, int requestId, BluetoothGattDescriptor descriptor, boolean preparedWrite, boolean responseNeeded, int offset, byte[] value) {
            super.onDescriptorWriteRequest(device, requestId, descriptor, preparedWrite, responseNeeded, offset, value);
        }

        @Override
        public void onExecuteWrite(BluetoothDevice device, int requestId, boolean execute) {
            super.onExecuteWrite(device, requestId, execute);
        }

        @Override
        public void onNotificationSent(BluetoothDevice device, int status) {
            super.onNotificationSent(device, status);
        }

        @Override
        public void onMtuChanged(BluetoothDevice device, int mtu) {
            super.onMtuChanged(device, mtu);
        }

        @Override
        public void onPhyUpdate(BluetoothDevice device, int txPhy, int rxPhy, int status) {
            super.onPhyUpdate(device, txPhy, rxPhy, status);
        }

        @Override
        public void onPhyRead(BluetoothDevice device, int txPhy, int rxPhy, int status) {
            super.onPhyRead(device, txPhy, rxPhy, status);
        }
    };

    /*GATT Client*/
    BluetoothGatt myGatt;
    BluetoothGattCallback myGattCallback = new BluetoothGattCallback() {
        @Override
        public void onPhyUpdate(BluetoothGatt gatt, int txPhy, int rxPhy, int status) {
            super.onPhyUpdate(gatt, txPhy, rxPhy, status);
        }

        @Override
        public void onPhyRead(BluetoothGatt gatt, int txPhy, int rxPhy, int status) {
            super.onPhyRead(gatt, txPhy, rxPhy, status);
        }

        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);

            Log.v("Tag", "GATT Client connection state changed: " + newState);

            final int ns = newState;
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    tv2.setText("GATT Client connection state changed: " + ns);
                    tv2.invalidate();
                }
            });

            if(newState == BluetoothGatt.STATE_CONNECTED && gatt != null){
                gatt.discoverServices();
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);

            final int ns = status;
            boolean okay = false, okay2 = false;
            Log.v("Tag", "GATT Client discovery status: " + status);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    tv2.setText("GATT Client service discovery status: " + ns);
                    tv2.invalidate();
                }
            });

            if(status == BluetoothGatt.GATT_SUCCESS){
                BluetoothGattService service = gatt.getService(myServiceUUID.getUuid());

                BluetoothGattCharacteristic characteristic = service.getCharacteristic(myCharacteristicUUID.getUuid());
                characteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
                okay = gatt.setCharacteristicNotification(characteristic, true);
                Log.v("Tag", "Client characteristic notification setup: " + okay);

                BluetoothGattCharacteristic characteristic2 = service.getCharacteristic(my2ndCharacteristicUUID.getUuid());
                characteristic2.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
                okay2 = gatt.setCharacteristicNotification(characteristic2, true);
                Log.v("Tag", "Client 2nd characteristic notification setup: " + okay2);
            }

            final boolean okayf = okay;
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if(ns == BluetoothGatt.GATT_SUCCESS && okayf){
                        sw4.setEnabled(true);
                        tv5.setText("This Central/Client Sent: none");
                        tv6.setText("This Central/Client Recv: none");
                        tv5.invalidate();
                        tv6.invalidate();
                    }
                    else{
                        sw4.setEnabled(false);
                    }
                }
            });

        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicRead(gatt, characteristic, status);
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicWrite(gatt, characteristic, status);
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, final BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);
            byte[] val = characteristic.getValue();
            try {
                final String smess = new String(val, "UTF-8");
                Log.v("Tag", "Client received from Server: " + smess);
                if(characteristic.getUuid().equals(myCharacteristicUUID.getUuid())) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            tv6.setText("This Central Recv: " + smess);
                            //this is basically the ACK from server. Do nothing more.
                        }
                    });
                }
                else if(characteristic.getUuid().equals(my2ndCharacteristicUUID.getUuid())){
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            tv4.setText("This Central Recv: " + smess); //this is real data from server. Need to send ACK
                            String cmess = "ACK:"+smess;
                            final BluetoothGattCharacteristic charf = characteristic;
                            try {
                                charf.setValue(cmess.getBytes("UTF-8"));
                            }catch (Exception ex){
                                ex.printStackTrace();
                            }
                            boolean okay = myGatt.writeCharacteristic(characteristic);
                            Log.v("Tag", "Sending ACK from central: " + cmess + ", status = " + okay);
                            if(okay){
                                tv3.setText("This Central Sent: " + cmess);
                            }
                        }
                    });

                }
            }catch (Exception ex){
                ex.printStackTrace();
            }
        }

        @Override
        public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            super.onDescriptorRead(gatt, descriptor, status);
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            super.onDescriptorWrite(gatt, descriptor, status);
        }

        @Override
        public void onReliableWriteCompleted(BluetoothGatt gatt, int status) {
            super.onReliableWriteCompleted(gatt, status);
        }

        @Override
        public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
            super.onReadRemoteRssi(gatt, rssi, status);
        }

        @Override
        public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
            super.onMtuChanged(gatt, mtu, status);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Log.v("Tag", "" + UUID.randomUUID().toString());

        init_uielements();
        init_advertise();
        init_scan();
        init_comm();
    }

    void init_uielements()
    {
        tv1 = (TextView) findViewById(R.id.tv1);
        sw1 = (Switch) findViewById(R.id.switchADV);
        tv2 = (TextView) findViewById(R.id.tv2);
        sw2 = (Switch) findViewById(R.id.switchSC);
        sw3 = (Switch) findViewById(R.id.switchCOM1);
        sw4 = (Switch) findViewById(R.id.switchCOM2);
        tv5 = (TextView) findViewById(R.id.tv5);
        tv6 = (TextView) findViewById(R.id.tv6);
        tv3 = (TextView) findViewById(R.id.tv3);
        tv4 = (TextView) findViewById(R.id.tv4);
    }

    /*Server Side Code: 1) starts advertisement and 2) opens GATT Server.*/
    void init_advertise() {
        myManager = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
        myAdapter = myManager.getAdapter();

        if (!myAdapter.isMultipleAdvertisementSupported()) {
            tv1.setText("Device does not support BLE advertisment.");
            sw1.setEnabled(false);
            return;
        }

        myAdvertiser = myAdapter.getBluetoothLeAdvertiser();
        myAdvertiseSettings = new AdvertiseSettings.Builder()
                .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
                .setConnectable(true)
                .setTimeout(0)
                .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
                .build();
        myAdvertiseData = new AdvertiseData.Builder()
                .setIncludeDeviceName(false)
                .setIncludeTxPowerLevel(false)
                .addServiceUuid(myServiceUUID)
                .build();

        sw1.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if (b) {
                    tv1.setText("Service UUID: " + myServiceUUIDstring);

                    myGattServer = myManager.openGattServer(getApplicationContext(), myGattServerCallback);
                    myGattService = new BluetoothGattService(myServiceUUID.getUuid(), BluetoothGattService.SERVICE_TYPE_PRIMARY);

                    myGattCharateristic = new BluetoothGattCharacteristic(myCharacteristicUUID.getUuid(), BluetoothGattCharacteristic.PROPERTY_WRITE, BluetoothGattCharacteristic.PERMISSION_WRITE);
                    my2ndGattCharacteristic = new BluetoothGattCharacteristic(my2ndCharacteristicUUID.getUuid(), BluetoothGattCharacteristic.PROPERTY_WRITE, BluetoothGattCharacteristic.PERMISSION_WRITE);

                    myGattService.addCharacteristic(myGattCharateristic);
                    myGattService.addCharacteristic(my2ndGattCharacteristic);

                    myGattServer.addService(myGattService);

                    myAdvertiser.startAdvertising(myAdvertiseSettings, myAdvertiseData, myAdvertiseCallback);

                } else {
                    tv1.setText("Advertisement stopped.");
                    try {
                        myAdvertiser.stopAdvertising(myAdvertiseCallback);
                        myGattServer.cancelConnection(myRemoteClientDevice);
                        myGattServer.close();
                    }catch (Exception ex){
                        ex.printStackTrace();
                    }
                }
            }
        });
    }

    /*Client side code: starts scanning (specific UUID). Connection happens at the scan callback */
    void init_scan()
    {
        myScanner = myAdapter.getBluetoothLeScanner();
        myScanSettings = new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .build();

        myScanFilter = new ScanFilter.Builder()
                .setServiceUuid(myServiceUUID)
                .build();

        myScanFilterList = new ArrayList<ScanFilter>();
        myScanFilterList.add(myScanFilter);

        sw2.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if(b){
                    tv2.setText("Scanning for " + myServiceUUIDstring);
                    myScanner.startScan(myScanFilterList, myScanSettings, myScanCallback);
                }
                else {
                    tv2.setText("Scan stopped.");
                    try {
                        myScanner.stopScan(myScanCallback);
                        myGatt.disconnect();
                        myGatt.close();
                    }catch (Exception ex){
                        ex.printStackTrace();
                    }
                }
            }
        });
    }

    void init_comm()
    {

        sw3.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if(b){

                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            fromperipheral = true;
                            for(int j = 0; j < 10000 && fromperipheral; j++){
                                try {
                                    Thread.sleep(1000);
                                    final String smess = String.format("P2C:%05d", j);
                                    my2ndGattCharacteristic.setValue(smess.getBytes("UTF-8"));
                                    myGattServer.notifyCharacteristicChanged(myRemoteClientDevice, my2ndGattCharacteristic, false);
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            tv3.setText("This Peripheral Sent: " + smess);
                                            tv3.invalidate();
                                        }
                                    });
                                }catch (Exception ex){
                                    ex.printStackTrace();
                                }
                            }
                        }
                    }).start();

                }
                else{
                    fromperipheral = false;
                }
            }
        });

        sw4.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if(b){
                    new Thread(new Runnable() {

                        BluetoothGattService service = myGatt.getService(myServiceUUID.getUuid());
                        BluetoothGattCharacteristic characteristic = service.getCharacteristic(myCharacteristicUUID.getUuid());

                        @Override
                        public void run() {
                            fromcentral = true;
                            for(int k = 0; k < 100000 && fromcentral; k++) {
                                try {
                                Thread.sleep(1000);
                                final String cmess = String.format("C2P:%05d", k);

                                    byte[] bm = cmess.getBytes("UTF-8");
                                    characteristic.setValue(cmess);
                                    boolean okay = myGatt.writeCharacteristic(characteristic);
                                    if (okay) {
                                        runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                tv5.setText("This Central Sent: " + cmess);
                                                tv5.invalidate();
                                            }
                                        });
                                    }
                                } catch (Exception ex) {
                                    ex.printStackTrace();
                                }
                            }
                        }
                    }).start();
                }
                else{
                    fromcentral = false;
                }
            }
        });
    }


}
