package com.example.android.rtyper;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Paint;
import android.support.v4.app.FragmentActivity;
import android.view.MenuItem;
import android.view.View;

import com.android.graphics.CanvasView;

/**
 * Created by ReDDarK on 17-Apr-2018.
 */

public class BluetoothDrawingFragment extends BluetoothBase {


    private CanvasView canvasView;

    private String bluetoothPayload = "";

    private final int PAYLOAD_SIZE = 500;

    private float typerWidth = 480;
    private float typerHeight = 180;

    private float norm(float v, float min, float max){
        return (v-min)/(max-min);
    }

    private float lerp(float norm, float min, float max){
        return (max-min)*norm+min;
    }

    private float map(float val, float smin, float smax, float dmin, float dmax){
        return this.lerp(this.norm(val, smin,smax), dmin,dmax);
    }


    private void addInstrctionToPayload(String instruction, View view, boolean send){

        if(bluetoothPayload == ""){

            bluetoothPayload += instruction;

        }else if(bluetoothPayload.length() + instruction.length() + 1 < PAYLOAD_SIZE){

            bluetoothPayload += ("," + instruction);
        }else{
            send = true;
        }

        if(send){
            sendMessage(bluetoothPayload, (Activity) view.getContext());

            bluetoothPayload = "";
        }
    }

    private void instructionToString(float x, float y, View view){

        float valX =
                Math.max(0, Math.min(typerWidth, map(x, 0, this.canvasView.drawZoneWidth, 0, typerWidth)));
        float valY =
                Math.max(0, Math.min(typerHeight, typerHeight - map(y, 0, this.canvasView.drawZoneHeight, 0, typerHeight)));



        addInstrctionToPayload(Float.toString(valX)+" "+Float.toString(valY), view, false);

    }

    private void instructionToString(boolean down, View view){
        addInstrctionToPayload(Integer.toString(down ? 1 : 0), view, !down);
    }


    public void clearCanvas(){

        this.canvasView.clear();

    }

    @Override
    public void setupComm(View view, Activity activity) {

        // Initialize the BluetoothService to perform bluetooth connections
        mCommService = BluetoothService.getInstance();

        mCommService.start();



    }

    public void switchMode(MainActivity activity) {


        activity.menuId = this.canvasView.isMoveMode()? R.menu.main_draw: R.menu.main_draw_move;
        activity.invalidateOptionsMenu();

        this.canvasView.setMoveMode(!this.canvasView.isMoveMode());

    }




    @Override
    public void onViewCreated(View view) {
        this.canvasView = (CanvasView)view.findViewById(R.id.canvas);
        this.canvasView.setMode(CanvasView.Mode.DRAW);
        this.canvasView.setDrawer(CanvasView.Drawer.PEN);
        this.canvasView.setPaintStyle(Paint.Style.STROKE);
        this.canvasView.setPaintStrokeWidth(2F);

        this.canvasView.onMovePath((x,y)->{instructionToString(x,y,view);});
        this.canvasView.onNewPath((down)->{instructionToString(down,view);});
    }

    public BluetoothDrawingFragment() {
        super();
    }



}
