package com.example.android.rtyper;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.os.Handler;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;

/**
 * Created by ReDDarK on 17-Apr-2018.
 */

public interface IBluetooth {


    int REQUEST_CONNECT_DEVICE_SECURE = 1;
    int REQUEST_CONNECT_DEVICE_INSECURE = 2;
    int REQUEST_ENABLE_BT = 3;


    void sendMessage(String message, Activity activity);

    void setupComm(final View view, Activity activity);



}
