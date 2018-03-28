package com.idevicesinc.sweetblue.hello_ble;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import com.idevicesinc.sweetblue.BleDevice;
import com.idevicesinc.sweetblue.BleDeviceState;
import com.idevicesinc.sweetblue.BleManager;
import com.idevicesinc.sweetblue.BleManagerConfig;
import com.idevicesinc.sweetblue.utils.BluetoothEnabler;
import com.idevicesinc.sweetblue.utils.Interval;
import com.idevicesinc.sweetblue.utils.Uuids;

/**
 * A very simple example which shows how to scan for BLE devices, connect to the first one seen, and then read it's battery level.
 */
public class MyActivity extends Activity
{
    // We're keeping an instance of BleManager for convenience, but it's not really necessary since it's a singleton. It's helpful so you
    // don't have to keep passing in a Context to retrieve it.
    private BleManager m_bleManager;

    // The instance of the device we're going to connect to, and read it's battery characteristic, if it exists.
    private BleDevice m_device;

    // Button for connecting to the first discovered device.
    private Button m_connect;

    // Button for disconnecting from the currently connected device.
    private Button m_disconnect;

    // TextView for displaying the device name.
    private TextView m_name;

    // TextView for displaying the current device states.
    private TextView m_state;

    // TextView for displaying the battery level, once it has been read.
    private TextView m_battery_level;

    @Override protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.my_activity);

        m_connect = findViewById(R.id.connect);

        m_disconnect = findViewById(R.id.disconnect);

        m_name = findViewById(R.id.name);

        m_state = findViewById(R.id.state);

        m_battery_level = findViewById(R.id.battery_level);

        setConnectButton();

        setDisconnectButton();

        startScan();
    }

    private void setConnectButton()
    {
        m_connect.setOnClickListener(new View.OnClickListener()
        {
            @Override public void onClick(View view)
            {
                // Disable the connect button when we start trying to connect.
                m_connect.setEnabled(false);

                connectToDevice();
            }
        });
    }

    private void setDisconnectButton()
    {
        m_disconnect.setOnClickListener(new View.OnClickListener()
        {
            @Override public void onClick(View view)
            {
                m_device.disconnect();
            }
        });
    }

    private void startScan()
    {
        BleManagerConfig config = new BleManagerConfig();

        // Only enable logging in debug builds.
        config.loggingEnabled = BuildConfig.DEBUG;

        // Run SweetBlue's update loop in a background thread. The default is to run on the UI thread, which is a legacy option. The default
        // will be false in version 3.
        config.runOnMainThread = false;

        // The scan report delay doesn't work properly on the Pixel, so we're disabling it here. This is another legacy option which will default
        // to disabled in version 3. If this isn't disabled, then the result will be that on the Pixel, it will take about 5 seconds to find the
        // first device.
        config.scanReportDelay = Interval.DISABLED;

        // Get the instance of the BleManager, and pass in the config.
        m_bleManager = BleManager.get(this, config);

        // Set the discovery listener. You can pass in a listener when calling startScan() if you want, but in most cases, this is preferred so you don't
        // have to keep passing in the listener when calling any of the startScan methods.
        m_bleManager.setListener_Discovery(new SimpleDiscoveryListener());

        // The BluetoothEnabler will take care of requesting the necessary permissions on Android M and above. It needs A> Bluetooth to be on of course,
        // B> Location permissions, and C> Location services enabled in order for a BLE scan to return any results.
        BluetoothEnabler.start(this, new BluetoothEnabler.DefaultBluetoothEnablerFilter()
        {
            @Override public Please onEvent(BluetoothEnablerEvent bluetoothEnablerEvent)
            {
                // We call super here to allow the default implementation take care of the details of enabling. We want to make sure
                // it does everything it needs to do before calling any start scan methods
                Please please = super.onEvent(bluetoothEnablerEvent);

                // If the enabler is now done, we can start scanning.
                if(bluetoothEnablerEvent.isDone())
                {
                    // Start the scan.
                    m_bleManager.startScan();
                }

                // Return the Please we got from above now.
                return please;
            }
        });
    }

    // Our simple discovery listener implementation.
    private final class SimpleDiscoveryListener implements BleManager.DiscoveryListener
    {
        private boolean m_discovered = false;

        @Override public void onEvent(DiscoveryEvent discoveryEvent)
        {
            // We're only going to connect to the first device we see, so let's stop the scan now. However, it's possible that more devices have already been discovered and will get piped
            // into the discovery listener, hence the need for the discovered boolean here
            if(!m_discovered)
            {
                m_discovered = true;

                // While SweetBlue will automatically stop the scan when you perform any other BLE operation, it's good practice to manually stop the scan here as in this case,
                // we're only concerned with the first device we find. Also, if there is no stopScan method, scanning will resume as soon as all other BLE operations are done.
                m_bleManager.stopScan();

                // We only care about the DISCOVERED event. REDISCOVERED can get posted many times for a single device during a scan.
                if(discoveryEvent.was(LifeCycle.DISCOVERED))
                {
                    // Grab the device from the DiscoveryEvent instance.
                    m_device = discoveryEvent.device();

                    m_name.setText(m_device.getName_debug());

                    m_state.setText(m_device.printState());

                    connectToDevice();
                }
            }
        }
    }

    private void connectToDevice()
    {
        // Connect to the device, and pass in a state listener, so we know when we are connected
        // We also set a ConnectionFailListener here to know if/when the connection failed.
        // For convenience we use the DefaultConnectionFailListener (which will retry twice before giving up). In this instance, we're only worried about
        // when the connection fails, and SweetBlue has given up trying to connect.
        // The interface BleDevice.StateListener is marked as deprecated, because in version 3, it will be moving to it's own class file and getting renamed. Its expected
        // to continue using this "deprecated" interface until then.
        m_device.connect(new BleDevice.StateListener()
        {
            @Override public void onEvent(StateEvent stateEvent)
            {

                // Update the device's state in the TextView
                m_state.setText(stateEvent.device().printState());

                // Check if the device entered the INITIALIZED state (this is the "true" connected state where the device is ready to be operated upon).
                if(stateEvent.didEnter(BleDeviceState.INITIALIZED))
                {
                    Log.i("SweetBlueExample", stateEvent.device().getName_debug() + " just initialized!");

                    // Now that we're connected, we enable the disconnect button.
                    m_disconnect.setEnabled(true);

                    // Now that we're connected, we can read the battery level characteristic of the device.
                    readBatteryLevelCharacteristic(stateEvent.device());
                }
                if(stateEvent.didEnter(BleDeviceState.DISCONNECTED) && !m_device.is(BleDeviceState.RETRYING_BLE_CONNECTION))
                {
                    // If the device got disconnected, and SweetBlue isn't retrying, then we disable the disconnect button, and enable the connect button.
                    m_connect.setEnabled(true);

                    m_disconnect.setEnabled(false);
                }
            }
        }, new BleDevice.DefaultConnectionFailListener()
        {
            @Override public Please onEvent(ConnectionFailEvent connectionFailEvent)
            {
                // Like in the BluetoothEnabler callback higher up in this class, we want to allow the default implementation do what it needs to do
                // However, in this case, we check the resulting Please that is returned to determine if we need to do anything yet.
                Please please = super.onEvent(connectionFailEvent);

                // If the returned please is NOT a retry, then SweetBlue has given up trying to connect, so let's print an error log.
                if(!please.isRetry())
                {
                    Log.e("SweetBlueExample", connectionFailEvent.device().getName_debug() + " failed to connect with a status of " + connectionFailEvent.status().name());
                }

                return please;
            }
        });
    }

    private void readBatteryLevelCharacteristic(BleDevice device)
    {
        // Read the battery level of the device. You don't necessarily have to pass in the service UUID here, as SweetBlue will scan the service database
        // for the characteristic you're looking for, however, it's most efficient to be this explicit so it can avoid having to iterate over everything.
        // The interface BleDevice.ReadWriteListener is marked as deprecated, because in version 3, it will be moving to it's own class file. Its expected
        // to continue using this "deprecated" interface until then.
        device.read(Uuids.BATTERY_SERVICE_UUID, Uuids.BATTERY_LEVEL, new BleDevice.ReadWriteListener()
        {
            @Override public void onEvent(ReadWriteEvent readWriteEvent)
            {
                if(readWriteEvent.wasSuccess())
                {
                    m_battery_level.setText(String.format("%s%%", readWriteEvent.data_byte()));

                    Log.i("SweetBlueExample", "Battery level is " + readWriteEvent.data_byte() + "%");
                }
                else
                {
                    // If SweetBlue couldn't find the battery service and characteristic, then the device must not have it, or it's in a custom
                    // characteristic
                    if(readWriteEvent.status() == Status.NO_MATCHING_TARGET)
                    {
                        m_battery_level.setText("[No battery characteristic]");
                    }

                    // There are several possible failures here. This will still get called if the battery service/characteristic is not found.
                    Log.e("SweetBlueExample", "Reading battery level failed with status " + readWriteEvent.status().name());
                }
            }
        });
    }
}