/*
* Copyright 2013 The Android Open Source Project
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*     http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

package com.example.android.common.activities;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.KeyEvent;

import com.example.android.common.logger.Log;
import com.example.android.common.logger.LogWrapper;
import com.example.android.rtyper.BluetoothCommFragment;
import com.example.android.rtyper.BluetoothDirectFragment;

/**
 * Base launcher activity, to handle most of the common plumbing for samples.
 */
public class SampleActivityBase extends FragmentActivity {

    public static final String TAG = "SampleActivityBase";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    protected  void onStart() {
        super.onStart();
        initializeLogging();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {

//
//        if(pos == 0){
//
//            BluetoothCommFragment base = (BluetoothCommFragment)pagerAdapter.getItem(pos);
//
//            BluetoothDirectFragment direct =  (BluetoothDirectFragment) base.bluetoothHelper;
//
//            if(direct != null){
//
//                if(direct.sendKeycode){
//
//                    direct.mOutEditText.setText(  direct.mOutEditText.getText() + "k" + String.valueOf(event.getScanCode()));
//
//                    return true;
//                }
//            }
//
//
//        }



        return super.onKeyDown(keyCode, event);
    }

    /** Set up targets to receive log data */
    public void initializeLogging() {
        // Using Log, front-end to the logging chain, emulates android.util.log method signatures.
        // Wraps Android's native log framework
        LogWrapper logWrapper = new LogWrapper();
        Log.setLogNode(logWrapper);

        Log.i(TAG, "Ready");
    }
}
