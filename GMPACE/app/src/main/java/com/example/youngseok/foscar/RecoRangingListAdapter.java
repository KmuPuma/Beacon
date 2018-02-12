package com.example.youngseok.foscar;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.perples.recosdk.RECOBeacon;

import java.util.ArrayList;
import java.util.Collection;

public class RecoRangingListAdapter extends BaseAdapter{
    private ArrayList<RECOBeacon> mRangedBeacons;
    private LayoutInflater mLayoutInflater;

    public static int beaconFront;
    public static int beaconLeft;
    public static int beaconRight;

    public RecoRangingListAdapter(Context context) {
        super();
        mRangedBeacons = new ArrayList<RECOBeacon>();
        mLayoutInflater = LayoutInflater.from(context);
    }

    public void updateAllBeacons(Collection<RECOBeacon> beacons) {
        synchronized (beacons) {
            mRangedBeacons = new ArrayList<RECOBeacon>(beacons);

//            if(mRangedBeacons.size() == 3) {
                for (int i = 0; i < mRangedBeacons.size(); i++) {
                    if (mRangedBeacons.get(i).getMinor() == 1115) {
                        beaconFront = mRangedBeacons.get(i).getRssi();
                    } else if (mRangedBeacons.get(i).getMinor() == 1109) {
                        beaconLeft = mRangedBeacons.get(i).getRssi();
                    } else if (mRangedBeacons.get(i).getMinor() == 1110) {
                        beaconRight = mRangedBeacons.get(i).getRssi();
                    }
                }
//            }

            Log.e("Rssi", "Beacon Front : " + beaconFront + " Beacon Left : " + beaconLeft + " Beacon Right : " + beaconRight);
        }
    }

    public void clear() {
        mRangedBeacons.clear();
    }

    @Override
    public int getCount() {
        return mRangedBeacons.size();
    }

    @Override
    public Object getItem(int position) {
        return mRangedBeacons.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder viewHolder;

        if(convertView == null) {
            convertView = mLayoutInflater.inflate(R.layout.item_ranging_beacon, parent, false);
            viewHolder = new ViewHolder();
            viewHolder.recoMinor = (TextView)convertView.findViewById(R.id.recoMinor);
            viewHolder.recoTxPower = (TextView)convertView.findViewById(R.id.recoTxPower);
            viewHolder.recoRssi = (TextView)convertView.findViewById(R.id.recoRssi);
            viewHolder.recoBattery = (TextView)convertView.findViewById(R.id.recoBattery);
            viewHolder.recoProximity = (TextView)convertView.findViewById(R.id.recoProximity);
            viewHolder.recoAccuracy = (TextView)convertView.findViewById(R.id.recoAccuracy);
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder)convertView.getTag();
        }

        RECOBeacon recoBeacon = mRangedBeacons.get(position);

        viewHolder.recoMinor.setText(recoBeacon.getMinor() + "");
        viewHolder.recoTxPower.setText(recoBeacon.getTxPower() + "");
        viewHolder.recoRssi.setText(recoBeacon.getRssi() + "");
        viewHolder.recoBattery.setText(recoBeacon.getBattery() + "");
        viewHolder.recoProximity.setText(recoBeacon.getProximity() + "");
        viewHolder.recoAccuracy.setText(String.format("%.2f", recoBeacon.getAccuracy()));

        return convertView;
    }

    static class ViewHolder {
        TextView recoMinor;
        TextView recoTxPower;
        TextView recoRssi;
        TextView recoBattery;
        TextView recoProximity;
        TextView recoAccuracy;
    }
}
