package com.example.android.rtyper;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.android.common.logger.Log;
import com.google.common.base.Joiner;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by ReDDarK on 17-Apr-2018.
 */

public class BluetoothDirectFragment extends BluetoothBase{


    public ListView mConversationView;
    public EditText mOutEditText;
    public Button mSendButton;
    public ArrayAdapter<String> mConversationArrayAdapter;
    public  HashMap<String, String> keyCodes;
    /**
     * The action listener for the EditText widget, to listen for the return key
     */




    @Override
    public void sendMessage(String message, Activity activity) {
        // Check that we're actually connected before trying anything
        if (mCommService.getState() != BluetoothService.STATE_CONNECTED) {
            Toast.makeText(activity, R.string.not_connected, Toast.LENGTH_SHORT).show();
            return;
        }

        // Check that there's actually something to send
        if (message.length() > 0) {



            if(sendKeycode){

                List<String> stringList = new ArrayList<>();

                for(String str : message.split(";")){

                    if(keyCodes.containsKey(str.toUpperCase()))
                    stringList.add(keyCodes.get(str.toUpperCase()));
                    stringList.add("1");
                }

                message = Joiner.on(",").join(stringList);

            }

            // Get the message bytes and tell the BluetoothService to write
            byte[] send = message.getBytes();
            mCommService.write(send);

            // Reset out string buffer to zero and clear the edit text field
            mOutStringBuffer.setLength(0);
            mOutEditText.setText(mOutStringBuffer);
        }
    }

    public void setupComm(final View view, final Activity activity) {


        // Initialize the array adapter for the conversation thread
        mConversationArrayAdapter = new ArrayAdapter<String>(activity, R.layout.message);

        mConversationView.setAdapter(mConversationArrayAdapter);

        // Initialize the compose field with a listener for the return key
        mOutEditText.setOnEditorActionListener((TextView tView, int actionId, KeyEvent event) -> {


            if (actionId == EditorInfo.IME_NULL
                    && event.getAction() == KeyEvent.ACTION_DOWN) {
                String message = tView.getText().toString();
                sendMessage(message, activity);
            }
            return true;
        });


        mSendButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Send a message using content of the edit text widget

                if (null != view) {
                    TextView textView = (TextView) view.findViewById(R.id.edit_text_out);
                    String message = textView.getText().toString();
                    sendMessage(message, activity);
                }
            }
        });


        // Initialize the send button with a listener that for click events


        // Initialize the BluetoothService to perform bluetooth connections
        mCommService = BluetoothService.getInstance();

        mCommService.start();


        // Initialize the buffer for outgoing messages
        mOutStringBuffer = new StringBuffer("");



    }


    public BluetoothDirectFragment() {
        super();
    }

    @Override
    public void onViewCreated(View view) {
        mConversationView = (ListView) view.findViewById(R.id.in);
        mOutEditText = (EditText) view.findViewById(R.id.edit_text_out);
        mSendButton = (Button) view.findViewById(R.id.button_send);

        String codes = "{'0':'k11','1':'k2','2':'k3','3':'k4','4':'k5','5':'k6','6':'k7','7':'k8','8':'k9','9':'k10','ESC':'k1','-':'k74','=':'k13','BS':'k14','TAB':'k15','Q':'k16','W':'k17','E':'k18','R':'k19','T':'k20','Y':'k21','U':'k22','I':'k23','O':'k24','P':'k25','[':'k26',']':'k27','ENTER':'k28','L CTRL':'k29','A':'k30','S':'k31','D':'k32','F':'k33','G':'k34','H':'k35','J':'k36','K':'k37','L':'k38',';':'k39','`':'k41','L SHIFT':'k42','Z':'k44','X':'k45','C':'k46','V':'k47','B':'k48','N':'k49','M':'k50',',':'k51','.':'k52','/':'k98','R SHIFT':'k54','*':'k55','L ALT':'k56','SPACE':'k57','CAPS LOCK':'k58','F1':'k59','F2':'k60','F3':'k61','F4':'k62','F5':'k63','F6':'k64','F7':'k65','F8':'k66','F9':'k67','F10':'k68','NUM LOCK':'k69','SCROLL LOCK':'k70','HOME 7':'k71','UP 8':'k72','PGUP 9':'k73','LEFT 4':'k75','RT ARROW 6':'k77','+':'k78','END 1':'k79','DOWN 2':'k80','PGDN 3':'k81','INS':'k82','DEL':'k83','F11':'k87','F12':'k88','R ENTER':'k96','R CTRL':'k97','PRT SCR':'k99','R ALT':'k100','Home':'k102','Up':'k103','PgUp':'k104','Left':'k105','Right':'k106','End':'k107','Down':'k108','PgDn':'k109','Insert':'k110','Del':'k111','Pause':'k119'}";
        Gson gson = new Gson();

        Type type = new TypeToken<HashMap<String, String>>(){}.getType();
        keyCodes = gson.fromJson(codes, type);

    }



}
