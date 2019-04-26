/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.rtyper;

import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.android.common.logger.Log;

/**
 * This fragment controls Bluetooth to communicate with other devices.
 */
public class BluetoothCommFragment extends Fragment {

    private static final String TAG = "BluetoothCommFragment";

    // Intent request codes

    private int layoutRef;


    public BluetoothBase bluetoothHelper;
    private Handler handler;
    private BluetoothAdapter mBluetoothAdapter;

    BluetoothDirectFragment bdf;

    BluetoothDrawingFragment bdrawingf;
    private int mode = Constants.MODE_DIRECT;


    static BluetoothCommFragment newInstance(int num) {
        BluetoothCommFragment f = new BluetoothCommFragment();

        // Supply num input as an argument.
        Bundle args = new Bundle();
        args.putInt("ref", num);
        f.setArguments(args);

        return f;
    }

    @Override
    public void onResume() {
        super.onResume();

        // Performing this check in onResume() covers the case in which BT was
        // not enabled during onStart(), so we were paused to enable it...
        // onResume() will be called when ACTION_REQUEST_ENABLE activity returns.
        if (bluetoothHelper.mCommService != null) {
            //bluetoothHelper.mCommService.setHandler(bluetoothHelper.mHandler);
            // Only if the state is STATE_NONE, do we know that we haven't started already
            if (bluetoothHelper.mCommService.getState() == BluetoothService.STATE_NONE) {
                // Start the Bluetooth chat services
                bluetoothHelper.mCommService.start();
            }
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        // If BT is not on, request that it be enabled.
        // setupComm() will then be called during onActivityResult
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, BluetoothBase.REQUEST_ENABLE_BT);
            // Otherwise, setup the chat session
        } else if (bluetoothHelper.mCommService == null) {
            bluetoothHelper.setupComm(getView(), getActivity());
        }
    }

    @SuppressLint("HandlerLeak")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        // Get local Bluetooth adapter


        layoutRef = getArguments() != null ? getArguments().getInt("ref") : 1;


        switch (layoutRef){


            case R.layout.fragment_bluetooth_chat:

                bluetoothHelper = new BluetoothDirectFragment();

                bdf = (BluetoothDirectFragment) bluetoothHelper ;

                this.mode = Constants.MODE_DIRECT;

                handler =  new Handler() {
                    @Override
                    public void handleMessage(Message msg) {
                        FragmentActivity activity = getActivity();
                        switch (msg.what) {
                            case Constants.MESSAGE_STATE_CHANGE:
                                switch (msg.arg1) {
                                    case BluetoothService.STATE_CONNECTED:
                                        setStatus(getString(R.string.title_connected_to, bdf.mConnectedDeviceName));
                                        bdf.mConversationArrayAdapter.clear();
                                        break;
                                    case BluetoothService.STATE_CONNECTING:
                                        setStatus(R.string.title_connecting);
                                        break;
                                    case BluetoothService.STATE_LISTEN:
                                    case BluetoothService.STATE_NONE:
                                        setStatus(R.string.title_not_connected);
                                        break;
                                }
                                break;
                            case Constants.MESSAGE_WRITE:
                                byte[] writeBuf = (byte[]) msg.obj;
                                // construct a string from the buffer
                                String writeMessage = new String(writeBuf);

                                if(Constants.MODE_DIRECT == msg.arg2)
                                    bdf.mConversationArrayAdapter.add(writeMessage);
                                break;
                            case Constants.MESSAGE_READ:
                                byte[] readBuf = (byte[]) msg.obj;
                                // construct a string from the valid bytes in the buffer
                                String readMessage = new String(readBuf, 0, msg.arg1);
                                bdf.mConversationArrayAdapter.add(bdf.mConnectedDeviceName + ":  " + readMessage);
                                break;
                            case Constants.MESSAGE_DEVICE_NAME:
                                // save the connected device's name
                                bdf.mConnectedDeviceName = msg.getData().getString(Constants.DEVICE_NAME);
                                if (null != activity) {
                                    Toast.makeText(activity, "Connected to "
                                            + bdf.mConnectedDeviceName, Toast.LENGTH_SHORT).show();
                                }
                                break;
                            case Constants.MESSAGE_TOAST:
                                if (null != activity) {
                                    Toast.makeText(activity, msg.getData().getString(Constants.TOAST),
                                            Toast.LENGTH_SHORT).show();
                                }
                                break;
                        }
                    }
                };

                bdf.setmHandler(handler);



                break;

            case R.layout.fragment_bluetooth_drawing:

                bluetoothHelper = new BluetoothDrawingFragment();

                this.mode = Constants.MODE_DRAWING;

                bdrawingf = (BluetoothDrawingFragment) bluetoothHelper ;

                break;

        }

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // If the adapter is null, then Bluetooth is not supported
        if (mBluetoothAdapter == null) {
            FragmentActivity activity = getActivity();
            Toast.makeText(activity, "Bluetooth is not available", Toast.LENGTH_LONG).show();
            activity.finish();
        }

    }




    @Override
    public void onDestroy() {
        super.onDestroy();
        if (bluetoothHelper.mCommService != null) {
            bluetoothHelper.mCommService.stop();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(layoutRef, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {

        bluetoothHelper.onViewCreated(view);
    }


    /**
     * Updates the status on the action bar.
     *
     * @param resId a string resource ID
     */
    protected void setStatus(int resId) {
        FragmentActivity activity = getActivity();
        if (null == activity) {
            return;
        }
        final ActionBar actionBar = activity.getActionBar();
        if (null == actionBar) {
            return;
        }
        actionBar.setSubtitle(resId);
    }

    /**
     * Updates the status on the action bar.
     *
     * @param subTitle status
     */
    protected void setStatus(CharSequence subTitle) {
        FragmentActivity activity = getActivity();
        if (null == activity) {
            return;
        }
        final ActionBar actionBar = activity.getActionBar();
        if (null == actionBar) {
            return;
        }
        actionBar.setSubtitle(subTitle);
    }


    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case BluetoothBase.REQUEST_CONNECT_DEVICE_SECURE:
                // When DeviceListActivity returns with a device to connect
                if (resultCode == Activity.RESULT_OK) {
                    connectDevice(data, true);
                }
                break;
            case BluetoothBase.REQUEST_CONNECT_DEVICE_INSECURE:
                // When DeviceListActivity returns with a device to connect
                if (resultCode == Activity.RESULT_OK) {
                    connectDevice(data, false);
                }
                break;
            case BluetoothBase.REQUEST_ENABLE_BT:
                // When the request to enable Bluetooth returns
                if (resultCode == Activity.RESULT_OK) {
                    // Bluetooth is now enabled, so set up a chat session

                } else {
                    // User did not enable Bluetooth or an error occurred
                    Log.d(TAG, "BT not enabled");
                    Toast.makeText(getActivity(), R.string.bt_not_enabled_leaving,
                            Toast.LENGTH_SHORT).show();
                    getActivity().finish();
                }
        }
    }

    /**
     * Establish connection with other device
     *
     * @param data   An {@link Intent} with {@link DeviceListActivity#EXTRA_DEVICE_ADDRESS} extra.
     * @param secure Socket Security type - Secure (true) , Insecure (false)
     */
    private void connectDevice(Intent data, boolean secure) {
        // Get the device MAC address
        String address = data.getExtras()
                .getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
        // Get the BluetoothDevice object
        BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        // Attempt to connect to the device
        bluetoothHelper.mCommService.connect(device, secure);
    }




    private void ensureDiscoverable() {
        if (mBluetoothAdapter.getScanMode() !=
                BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
            Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
            startActivity(discoverableIntent);
        }
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.secure_connect_scan: {
                // Launch the DeviceListActivity to see devices and do scan
                Intent serverIntent = new Intent(getActivity(), DeviceListActivity.class);
                startActivityForResult(serverIntent, BluetoothBase.REQUEST_CONNECT_DEVICE_SECURE);
                return true;
            }
            case R.id.insecure_connect_scan: {
                // Launch the DeviceListActivity to see devices and do scan
                Intent serverIntent = new Intent(getActivity(), DeviceListActivity.class);
                startActivityForResult(serverIntent, BluetoothBase.REQUEST_CONNECT_DEVICE_INSECURE);
                return true;
            }
            case R.id.discoverable: {
                // Ensure this device is discoverable by others
                ensureDiscoverable();
                return true;
            }

            case R.id.clear_canvas: {

                if(this.bdrawingf != null)
                    this.bdrawingf.clearCanvas();

                return true;
            }
            case R.id.home: {

                this.bluetoothHelper.sendMessage("3", getActivity());

                return true;
            }
            case R.id.change_key_mode: {


                this.bluetoothHelper.sendKeycode = ! this.bluetoothHelper.sendKeycode;

                Toast.makeText(getActivity(),
                        "send keycode mode: "+ String.valueOf(this.bluetoothHelper.sendKeycode),
                        Toast.LENGTH_SHORT).show();


                return true;
            }
            case R.id.switch_mode: {

                this.bdrawingf.switchMode((MainActivity) getActivity());

                return true;
            }

            case R.id.set_home: {

                this.bluetoothHelper.sendMessage(Constants.INSTRUCTION_SET_HOME, getActivity());

                return true;
            }
        }
        return false;
    }



    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        if (isVisibleToUser) {

            BluetoothService.getInstance().setmMode(this.mode);

            MainActivity activity = (MainActivity) getActivity();

            if(activity != null){
                switch(this.mode){

                    case Constants.MODE_DIRECT:{

                        activity.menuId = R.menu.main_direct;
                        activity.invalidateOptionsMenu();

                        break;
                    }

                    case Constants.MODE_DRAWING:{

                        activity.menuId = R.menu.main_draw;
                        activity.invalidateOptionsMenu();
                        break;
                    }

                    default:
                        break;
                }
            }




            try {
                InputMethodManager mImm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                mImm.hideSoftInputFromWindow(getView().getWindowToken(), 0);
                mImm.hideSoftInputFromWindow(getActivity().getCurrentFocus().getWindowToken(), 0);
            } catch (Exception e) {
                Log.e(TAG, "setUserVisibleHint: ", e);
            }
        }
    }


}
