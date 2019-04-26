package com.example.android.rtyper;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.os.Handler;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

/**
 * Created by ReDDarK on 17-Apr-2018.
 */

public abstract class BluetoothBase implements  IBluetooth {


    // Intent request codes
    public static final int REQUEST_CONNECT_DEVICE_SECURE = 1;
    public static final int REQUEST_CONNECT_DEVICE_INSECURE = 2;
    public static final int REQUEST_ENABLE_BT = 3;

    // Layout Views


    /**
     * Name of the connected device
     */
    public String mConnectedDeviceName = null;

    /**
     * Array adapter for the conversation thread
     */


    /**
     * String buffer for outgoing messages
     */
    public StringBuffer mOutStringBuffer;

    /**
     * Local Bluetooth adapter
     */


    /**
     * Member object for the chat services
     */
    public BluetoothService mCommService;

    public Handler mHandler;


    public Boolean sendKeycode = false;

    public BluetoothBase() {

    }

    public void setmHandler(Handler mHandler) {
        this.mHandler = mHandler;
        BluetoothService.getInstance().setHandler(mHandler);
    }

    @Override
    public void sendMessage(String message, Activity activity) {
        // Check that we're actually connected before trying anything
        if (mCommService.getState() != BluetoothService.STATE_CONNECTED) {
            Toast.makeText(activity, R.string.not_connected, Toast.LENGTH_SHORT).show();
            return;
        }

        // Check that there's actually something to send
        if (message.length() > 0) {
            // Get the message bytes and tell the BluetoothService to write
            byte[] send = message.getBytes();
            mCommService.write(send);
        }
    }



    public abstract void onViewCreated(View view);
}
