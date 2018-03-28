package com.idevicesinc.sweetblue.simple_ota;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import com.idevicesinc.sweetblue.BleDevice;
import com.idevicesinc.sweetblue.BleDeviceState;
import com.idevicesinc.sweetblue.BleManager;
import com.idevicesinc.sweetblue.BleManagerConfig;
import com.idevicesinc.sweetblue.BleTransaction;
import com.idevicesinc.sweetblue.utils.BluetoothEnabler;
import com.idevicesinc.sweetblue.utils.Interval;
import com.idevicesinc.sweetblue.utils.Uuids;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * A very simple example which shows how to scan for BLE devices, connect to the first one seen, and then perform an over-the-air (OTA) firmware update.
 */
public class MyActivity extends Activity
{
    // This is the UUID of the characteristic we want to write to (make sure you change it to a valid UUID for the device you want to connect to)
    private static final UUID MY_UUID = Uuids.INVALID;  // NOTE: Replace with your actual UUID.

    // There's really no need to keep this up here, it's just here for convenience.
    private static final byte[] MY_DATA = {(byte) 0xC0, (byte) 0xFF, (byte) 0xEE};//  NOTE: Replace with your actual data, not 0xC0FFEE.

    // We're keeping an instance of BleManager for convenience, but it's not really necessary since it's a singleton. It's helpful so you
    // don't have to keep passing in a Context to retrieve it.
    private BleManager m_bleManager;

    // The instance of the device we're going to connect to and write to.
    private BleDevice m_bleDevice;

    private boolean m_discovered = false;

    @Override protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        // Check that required variables were initialized
        if (MY_UUID.equals(Uuids.INVALID))
            throw new RuntimeException("You need to set a valid UUID for MY_UUID!");

        startScan();
    }

    private void startScan()
    {
        BleManagerConfig config = new BleManagerConfig();

        // Run SweetBlue's update loop in a background thread. The default is to run on the UI thread, which is a legacy option. The default
        // will be false in version 3.
        config.runOnMainThread = false;

        // Only enable logging in debug builds
        config.loggingEnabled = BuildConfig.DEBUG;

        // The scan report delay doesn't work properly on the Pixel, so we're disabling it here. This is another legacy option which will default
        // to disabled in version 3. If this isn't disabled, then the result will be that on the Pixel, it will take about 5 seconds to find the
        // first device.
        config.scanReportDelay = Interval.DISABLED;

        // Get the instance of the BleManager, and pass in the config.
        m_bleManager = BleManager.get(this, config);

        // Set the discovery listener. You can pass in a listener when calling startScan() if you want, but in most cases, this is preferred so you don't
        // have to keep passing in the listener when calling any of the startScan methods.
        m_bleManager.setListener_Discovery(this::onDeviceDiscovered);

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

    private void onDeviceDiscovered(BleManager.DiscoveryListener.DiscoveryEvent discoveryEvent)
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
            if(discoveryEvent.was(BleManager.DiscoveryListener.LifeCycle.DISCOVERED))
            {
                // Grab the device from the DiscoveryEvent instance
                m_bleDevice = discoveryEvent.device();

                connectToDevice();
            }
        }
    }

    private void connectToDevice()
    {
        // Connect to the device, and pass in a state listener, so we know when we are connected We also set a ConnectionFailListener here
        // to know if/when the connection failed. For convenience we use the DefaultConnectionFailListener (which will retry twice before
        // giving up). In this instance, we're only worried about when the connection fails, and SweetBlue has given up trying to connect.
        // The interface BleDevice.StateListener is marked as deprecated, because in version 3, it will be moving to it's own class file
        // and getting renamed. Its expected to continue using this "deprecated" interface until then.
        m_bleDevice.connect(stateEvent ->
        {
            // Check if the device entered the INITIALIZED state (this is the "true" connected state where the device is ready to be operated upon).
            if(stateEvent.didEnter(BleDeviceState.INITIALIZED))
            {
                Log.i("SweetBlueExample", stateEvent.device().getName_debug() + " just initialized!");

                final ArrayList<byte[]> writeQueue = new ArrayList<>();

                writeQueue.add(MY_DATA);

                writeQueue.add(MY_DATA);

                writeQueue.add(MY_DATA);

                writeQueue.add(MY_DATA);

                stateEvent.device().performOta(new SimpleOtaTransaction(writeQueue));
            }
        }, new BleDevice.DefaultConnectionFailListener()
        {
            @Override public Please onEvent(ConnectionFailEvent connectionFailEvent)
            {
                // Like in the BluetoothEnabler callback higher up in this class, we want to allow the default implementation do what it needs to do
                // However, in this case, we check the resulting Please that is returned to determine if we need to do anything yet.
                Please please = super.onEvent(connectionFailEvent);

                // If the returned please is NOT a retry, then SweetBlue has given up trying to connect, so let's print an error log
                if(!please.isRetry())
                {
                    Log.e("SweetBlueExample", connectionFailEvent.device().getName_debug() + " failed to connect with a status of " + connectionFailEvent.status().name());
                }

                return please;
            }
        });
    }

    // A simple implementation of an OTA transaction class. This simply holds a list of byte arrays. Each array will be sent in it's own
    // write operation.
    private static class SimpleOtaTransaction extends BleTransaction.Ota
    {
        // Our list of byte arrays to be sent to the device
        private final List<byte[]> m_dataQueue;

        // The current index we're on in the list
        private int m_currentIndex = 0;

        // A ReadWriteListener for listening to the result of each write.
        private final BleDevice.ReadWriteListener m_readWriteListener = readWriteEvent ->
        {
            // If the last write was a success, go ahead and move on to the next one
            if(readWriteEvent.wasSuccess())
            {
                doNextWrite();
            }
            else
            {
                // When running a transaction, you must remember to call succeed(), or fail() to release the queue for other operations to be
                // performed.
                fail();
            }
        };

        public SimpleOtaTransaction(final List<byte[]> dataQueue)
        {
            m_dataQueue = dataQueue;
        }

        @Override protected void start(BleDevice device)
        {
            doNextWrite();
        }

        private void doNextWrite()
        {
            if(m_currentIndex == m_dataQueue.size())
            {
                // Now that we've sent all data, we succeed the transaction, so that other operations may be performed on the device.
                succeed();
            }
            else
            {
                final byte[] nextData = m_dataQueue.get(m_currentIndex);

                getDevice().write(MY_UUID, nextData, m_readWriteListener);

                m_currentIndex++;
            }
        }
    }
}