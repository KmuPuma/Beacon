package com.example.youngseok.foscar;

import android.app.Activity;
import android.os.Bundle;

import com.perples.recosdk.RECOBeaconManager;
import com.perples.recosdk.RECOBeaconRegion;
import com.perples.recosdk.RECOServiceConnectListener;

import java.util.ArrayList;

public abstract class RecoActivity extends Activity implements RECOServiceConnectListener {
    protected RECOBeaconManager mRecoManager;
    protected ArrayList<RECOBeaconRegion> mRegions;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        /**
         * Create an instance of RECOBeaconManager (to set scanning target and ranging timeout in the background.)
         * If you want to scan RECOs only and not to set ranging timeout in the backgournd, create an instance:
         * 		mRecoManager = RECOBeaconManager.getInstance(getApplicationContext(), true, false);
         * WARNING: False enableRangingTimeout will affect the battery consumption.
         *
         * RECOBeaconManager 인스턴스틀 생성합니다. (스캔 대상 및 백그라운드 ranging timeout 설정)
         * RECO만을 스캔하고, 백그라운드 ranging timeout을 설정하고 싶지 않으시다면, 다음과 같이 생성하시기 바랍니다.
         * 		mRecoManager = RECOBeaconManager.getInstance(getApplicationContext(), true, false);
         * 주의: enableRangingTimeout을 false로 설정 시, 배터리 소모량이 증가합니다.
         */

        mRecoManager = RECOBeaconManager.getInstance(getApplicationContext(), MainActivity.SCAN_RECO_ONLY, MainActivity.ENABLE_BACKGROUND_RANGING_TIMEOUT);
        mRegions = this.generateBeaconRegion();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    private ArrayList<RECOBeaconRegion> generateBeaconRegion() {
        ArrayList<RECOBeaconRegion> regions = new ArrayList<RECOBeaconRegion>();

        RECOBeaconRegion recoRegion;
        recoRegion = new RECOBeaconRegion(MainActivity.RECO_UUID, "RECO Sample Region");
        regions.add(recoRegion);

        return regions;
    }

    protected abstract void start(ArrayList<RECOBeaconRegion> regions);
    protected abstract void stop(ArrayList<RECOBeaconRegion> regions);
}
