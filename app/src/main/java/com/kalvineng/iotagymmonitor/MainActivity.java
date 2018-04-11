package com.kalvineng.iotagymmonitor;

import android.app.Fragment;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.idevicesinc.sweetblue.BleDevice;
import com.idevicesinc.sweetblue.BleDeviceState;
import com.idevicesinc.sweetblue.BleManager;
import com.idevicesinc.sweetblue.BleManagerConfig;
import com.idevicesinc.sweetblue.BleScanMode;
import com.idevicesinc.sweetblue.utils.BluetoothEnabler;
import com.idevicesinc.sweetblue.utils.Interval;
import com.idevicesinc.sweetblue.utils.Uuids;

import java.util.Arrays;
import java.util.Locale;

import jota.IotaAPI;
import jota.dto.response.GetNodeInfoResponse;

public class MainActivity extends AppCompatActivity {

    private TextView mTextMessage;
    private FragmentManager manager = getSupportFragmentManager();
    private enum fragments { HOME, DEVICES, IOTA};
    private fragments current = fragments.HOME;
    private Boolean HRMconnected = false;

    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = new BottomNavigationView.OnNavigationItemSelectedListener() {

        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            switch (item.getItemId()) {
                case R.id.navigation_home:
                    manager.beginTransaction().replace(R.id.fragment_container, new Home(), "HOME").commit();
                    current = fragments.HOME;
                    HRMconnected = false;
                    return true;
                case R.id.navigation_iota:
                    manager.beginTransaction().replace(R.id.fragment_container, new IOTAFragment(), "IOTA").commit();
                    current = fragments.IOTA;
                    HRMconnected = false;
                    return true;
            }
            return false;
        }
    };

    void BLEdevices() {

        final BleManager.DiscoveryListener discoveryListener = new BleManager.DiscoveryListener() {
            @Override
            public void onEvent(DiscoveryEvent e) {
                if (current.equals(fragments.HOME)) {
                    TextView tDistance;
                    TextView tQuality;
                    TextView tRSSI;

                    Log.d("MAC",e.macAddress());

                    switch (e.macAddress()) {
                        case "DF:10:46:09:32:A8":
                            if (!HRMconnected) {
                                    e.device().connect(new BleDevice.StateListener() {
                                        @Override
                                        public void onEvent(StateEvent e) {
                                            if (e.didEnter(BleDeviceState.INITIALIZED)) {
                                                e.device().enableNotify(Uuids.HEART_RATE_MEASUREMENT, new BleDevice.ReadWriteListener() {
                                                    @Override
                                                    public void onEvent(ReadWriteEvent e) {
                                                        if (e.wasSuccess() && !e.isNull()) {
                                                            try {
                                                                /* Heart rate data has been retrieved */
                                                                Log.d("CURRENT", current.toString());
                                                                if (current.equals(fragments.HOME)) {
                                                                    Log.d("BPM", Byte.toString(e.data()[1]));
                                                                    TextView t = (TextView) findViewById(R.id.heartRate);
                                                                    t.setText(String.format(Locale.US, "%d", e.data()[1]));
                                                                }
                                                            } catch (Exception err) {
                                                                Log.i("hrdebug", "Caught exception: " + err.toString());
                                                            }
                                                        }
                                                    }
                                                });
                                            }
                                        }
                                    });
                                    HRMconnected = true;
                                }
                            break;
                        case "C7:ED:53:AD:0B:DB":
                            tDistance = (TextView) findViewById(R.id.BeaconADistanceValue);
                            tDistance.setText(String.format(Locale.US, "%.2fm", e.device().getDistance().meters()));

                            tQuality = (TextView) findViewById(R.id.BeaconAQualityeValue);
                            tQuality.setText(e.device().getRssiPercent().toString());

                            tRSSI = (TextView) findViewById(R.id.BeaconARSSIValue);
                            tRSSI.setText(String.format(Locale.US,"%ddBm",e.device().getRssi()));

                            break;
                        case "C2:06:E6:D9:FD:2A":
                            tDistance = (TextView) findViewById(R.id.BeaconBDistanceValue);
                            tDistance.setText(String.format(Locale.US, "%.2fm", e.device().getDistance().meters()));

                            tQuality = (TextView) findViewById(R.id.BeaconBQualityeValue);
                            tQuality.setText(e.device().getRssiPercent().toString());

                            tRSSI = (TextView) findViewById(R.id.BeaconBRSSIValue);
                            tRSSI.setText(String.format(Locale.US,"%ddBm",e.device().getRssi()));                            break;
                        case "C1:E0:C2:99:BC:DE":
                            tDistance = (TextView) findViewById(R.id.BeaconCDistanceValue);
                            tDistance.setText(String.format(Locale.US, "%.2fm", e.device().getDistance().meters()));

                            tQuality = (TextView) findViewById(R.id.BeaconCQualityeValue);
                            tQuality.setText(e.device().getRssiPercent().toString());

                            tRSSI = (TextView) findViewById(R.id.BeaconCRSSIValue);
                            tRSSI.setText(String.format(Locale.US,"%ddBm",e.device().getRssi()));
                            break;
                        default:
                            break;

                    }
                }
            }
        };

        BleManager.get(this.getApplicationContext()).startScan(
                new BleManagerConfig.ScanFilter()
        {
            @Override public Please onEvent(ScanEvent e)
            {
                return Please.acknowledgeIf(e.macAddress().equals("C7:ED:53:AD:0B:DB") ||
                        e.macAddress().equals("C2:06:E6:D9:FD:2A") ||
                        e.macAddress().equals("C1:E0:C2:99:BC:DE") ||
                        e.macAddress().equals("DF:10:46:09:32:A8"));
            }
        }, discoveryListener);

    }

    @Override
    public void onResume() {
        super.onResume();



        HRMconnected = false;

        BLEdevices();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Helps you navigate the treacherous waters of Android M Location requirements for scanning.
        BluetoothEnabler.start(this, new BluetoothEnabler.DefaultBluetoothEnablerFilter()
        {
            @Override public Please onEvent(BluetoothEnablerEvent e)
            {
                if( e.isDone())
                {
                    HRMconnected = false;
                    BLEdevices();
                }

                return super.onEvent(e);
            }
        });

        manager = getSupportFragmentManager();
        manager.beginTransaction().replace(R.id.fragment_container, new Home()).commit();

        BottomNavigationView navigation = (BottomNavigationView) findViewById(R.id.navigation);
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);
    }

}
