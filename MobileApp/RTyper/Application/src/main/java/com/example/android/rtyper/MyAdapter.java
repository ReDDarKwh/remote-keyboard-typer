package com.example.android.rtyper;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentPagerAdapter;

public class MyAdapter extends FragmentPagerAdapter {

    public MyAdapter(android.support.v4.app.FragmentManager supportFragmentManager) {
        super(supportFragmentManager);
    }

    @Override
    public int getCount() {
        return 2;
    }

    @Override
    public Fragment getItem(int position) {
        switch (position){

            case 0:
                return BluetoothCommFragment.newInstance(R.layout.fragment_bluetooth_chat);
            case 1:
                return BluetoothCommFragment.newInstance(R.layout.fragment_bluetooth_drawing);
            default:

                return null;

        }
    }
}
