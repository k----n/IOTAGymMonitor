package com.kalvineng.iotagymmonitor;

import android.app.Fragment;
import android.content.res.AssetManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.ConsoleMessage;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.TextView;

import com.idevicesinc.sweetblue.BleDevice;
import com.idevicesinc.sweetblue.BleDeviceState;
import com.idevicesinc.sweetblue.BleManager;
import com.idevicesinc.sweetblue.BleManagerConfig;
import com.idevicesinc.sweetblue.BleScanMode;
import com.idevicesinc.sweetblue.utils.BluetoothEnabler;
import com.idevicesinc.sweetblue.utils.Interval;
import com.idevicesinc.sweetblue.utils.Uuids;


import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringEscapeUtils;
import org.codehaus.jackson.map.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private TextView mTextMessage;
    private FragmentManager manager = getSupportFragmentManager();
    private enum fragments { HOME, DEVICES, IOTA};
    private fragments current = fragments.HOME;
    private Boolean HRMconnected = false;
    private ArrayList<String> states = new ArrayList<String>();
    private ArrayList<String> packets = new ArrayList<String>();
    private ArrayList<String> roots = new ArrayList<String>();
    private Integer stateIndex = 0;
    private Integer packetIndex = 0;
    private WebView mWebView;
    private ObjectMapper mapperObj = new ObjectMapper();
    private String baseRoot = "";

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
                            tRSSI.setText(String.format(Locale.US,"%ddBm",e.device().getRssi()));
                            break;
                        case "F7:6A:3D:FF:5E:7A":
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
                        e.macAddress().equals("F7:6A:3D:FF:5E:7A") ||
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

        final Handler handler = new Handler();
        final Handler IOTAhandler = new Handler();

        handler.postDelayed(new Runnable(){
            public void run(){
                handler.postDelayed(this, 10000);
                if (current.equals(fragments.IOTA)) {
                    TextView t = (TextView) findViewById(R.id.rootIOTA);
                    t.setText(baseRoot);
                } else {
                    try {
                        TextView t = (TextView) findViewById(R.id.heartRate);
                        TextView t1 = (TextView) findViewById(R.id.BeaconADistanceValue);
                        TextView t2 = (TextView) findViewById(R.id.BeaconBDistanceValue);
                        TextView t3 = (TextView) findViewById(R.id.BeaconCDistanceValue);
                        String f = mapperObj.writeValueAsString(new Packet(t.getText().toString(), t1.getText().toString(), t2.getText().toString(), t3.getText().toString()));
                        Log.d("FFFF", f);
                        packets.add(f);
                    } catch (Exception err) {
                        Log.i("FFFF", "Caught exception: " + err.toString());
                    }
                }
            }
        }, 10000);

//        mWebView = new WebView(this);
//        WebSettings webSettings = mWebView.getSettings();
//        webSettings.setJavaScriptEnabled(true);
//        mWebView.setWebChromeClient(new WebChromeClient() {
//            @Override
//            public boolean onConsoleMessage(ConsoleMessage consoleMessage) {
//                if(consoleMessage.message().startsWith("###")){
//                    states.add(consoleMessage.message().substring(3));
//                }
//                else if (consoleMessage.message().startsWith("##")){
//                    roots.add(consoleMessage.message().substring(2));
//                } else {
//                    baseRoot = consoleMessage.message();
//                }
//                android.util.Log.d("WebView", consoleMessage.message());
//                return true;
//            }
//        });
//
//        mWebView.loadUrl("file:///android_asset/index.html");
//
//        IOTAhandler.post(new Runnable() {
//            @Override
//            public void run() {
//                if (current.equals(fragments.HOME)) {
//                    Iterator<String> stateIterator = states.listIterator(stateIndex);
//                    Iterator<String> packetIterator = packets.listIterator(packetIndex);
//                    Iterator<String> rootIterator = roots.listIterator(stateIndex);
//                    if (stateIterator.hasNext()) {
//                        if (stateIterator.hasNext() && rootIterator.hasNext()) {
//                            if (packetIterator.hasNext()) {
//                                stateIndex += 1;
//                                packetIndex += 1;
//                                String packet1 = StringEscapeUtils.escapeJava(stateIterator.next());
//                                String packet2 = StringEscapeUtils.escapeJava(packetIterator.next());
//                                String packet3 = StringEscapeUtils.escapeJava(rootIterator.next());
//                                String u = "file:///android_asset/index.html?" + "state=\"" + packet1 + "\"&packet=\"" + packet2 + "\"&root=\"" + packet3 + "\"";
//                                mWebView.loadUrl(u);
//                                Log.d("FUN", u);
//                            }
//                        }
//                    }
//                    handler.postDelayed(this, 15000);
//                }
//            }
//        });



        manager = getSupportFragmentManager();
        manager.beginTransaction().replace(R.id.fragment_container, new Home()).commit();

        BottomNavigationView navigation = (BottomNavigationView) findViewById(R.id.navigation);
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);
    }

}
