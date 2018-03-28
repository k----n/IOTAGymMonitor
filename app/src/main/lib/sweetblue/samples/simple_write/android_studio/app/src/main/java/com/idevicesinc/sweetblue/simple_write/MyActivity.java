package com.idevicesinc.sweetblue.simple_write;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import com.idevicesinc.sweetblue.BleDevice;
import com.idevicesinc.sweetblue.BleDeviceState;
import com.idevicesinc.sweetblue.BleManager;
import com.idevicesinc.sweetblue.BleManagerConfig;
import com.idevicesinc.sweetblue.utils.BluetoothEnabler;
import com.idevicesinc.sweetblue.utils.Interval;
import com.idevicesinc.sweetblue.utils.Uuids;
import java.util.UUID;

/**
 * A very simple example which shows how to scan for BLE devices, connect to the first one seen, and then write to a characteristic.
 */
public class MyActivity extends Activity
{
	// This is the UUID of the characteristic we want to write to (make sure you change it to a valid UUID for the device you want to connect to)
	private static final UUID MY_UUID = Uuids.INVALID;									// NOTE: Replace with your actual UUID.

	// There's really no need to keep this up here, it's just here for convenience
	private static final byte[] MY_DATA = {(byte) 0xC0, (byte) 0xFF, (byte) 0xEE};		//  NOTE: Replace with your actual data, not 0xC0FFEE

	// We're keeping an instance of the BleManager around here for convenience, but it's not too necessary, as it's a singleton. But it's helpful so you
	// don't have to keep passing in a Context to retrieve it.
	private BleManager m_bleManager;

	// The instance of the device we're going to connect and write to
	private BleDevice m_bleDevice;

	private Button m_connect;
	private Button m_disconnect;

	// Textview displaying the device name
	private TextView m_name;

	// Textview displaying the current device states
	private TextView m_state;

	private boolean m_discovered = false;


	@Override protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		setContentView(R.layout.my_activity);

		m_name = findViewById(R.id.name);
		m_state = findViewById(R.id.state);
		m_connect = findViewById(R.id.connect);
		m_disconnect = findViewById(R.id.disconnect);

		m_connect.setOnClickListener(v ->
		{
            // Disable the connect button when we start trying to connect
            m_connect.setEnabled(false);
            connectDevice();
        });

		m_disconnect.setOnClickListener(v -> m_bleDevice.disconnect());

		// This is just a check to make sure you changed the MY_UUID field. You did, right? :)
		if (MY_UUID.equals(Uuids.INVALID))
			throw new RuntimeException("You need to set a valid UUID for MY_UUID!");


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
		m_bleManager.setListener_Discovery(this::processDiscoveryEvent);


		// The BluetoothEnabler will take care of requesting the necessary permissions on Android M and above. It needs A> Bluetooth to be on of course,
		// B> Location permissions, and C> Location services enabled in order for a BLE scan to return any results.
		BluetoothEnabler.start(this, new BluetoothEnabler.DefaultBluetoothEnablerFilter() {
			@Override
			public Please onEvent(BluetoothEnablerEvent e)
			{
				// We call super here to allow the default implementation take care of the details of enabling. We want to make sure
				// it does everything it needs to do before calling any start scan methods
				Please please = super.onEvent(e);

				// If the enabler is now done, we can start scanning.
				if (e.isDone())
					startScanning();

				// Return the Please we got from above now.
				return please;
			}
		});
	}

	private void startScanning()
	{
		// Start the scan
		m_bleManager.startScan();
	}

	private void connectDevice()
	{
		// Connect to the device, and pass in a state listener, so we know when we are connected
		// We also set a ConnectionFailListener here to know if/when the connection failed.
		// For convenience we use the DefaultConnectionFailListener (which will retry twice before giving up). In this instance, we're only worried about
		// when the connection fails, and SweetBlue has given up trying to connect).
		// The interface BleDevice.StateListener is marked as deprecated, because in version 3, it will be moving to it's own class file and getting renamed. Its expected
		// to continue using this "deprecated" interface until then.
		m_bleDevice.connect(stateEvent ->
		{

            // Update the device's state in the TextView
            m_state.setText(stateEvent.device().printState());

            // Check if the device entered the INITIALIZED state (this is the "true" connected state where the device is ready to be operated upon).
            if (stateEvent.didEnter(BleDeviceState.INITIALIZED))
            {
                Log.i("SweetBlueExample", stateEvent.device().getName_debug() + " just initialized!");

                // Now that we're connected, we enable the disconnect button. It can be argued that the disconnect button should stay enabled until
				// the device is disconnected, so that you could cancel a connect in process.
                m_disconnect.setEnabled(true);

                writeChar();
            }
            if (stateEvent.didEnter(BleDeviceState.DISCONNECTED) && !m_bleDevice.is(BleDeviceState.RETRYING_BLE_CONNECTION))
            {
                // If the device got disconnected, and SweetBlue isn't retrying, then we disable the disconnect button, and enable the connect button
                m_connect.setEnabled(true);
                m_disconnect.setEnabled(false);
            }
        }, new BleDevice.DefaultConnectionFailListener()
		{
			@Override
			public Please onEvent(ConnectionFailEvent e)
			{
				// Like in the BluetoothEnabler callback higher up in this class, we want to allow the default implementation do what it needs to do
				// However, in this case, we check the resulting Please that is returned to determine if we need to do anything yet.
				Please please = super.onEvent(e);

				// If the returned please is NOT a retry, then SweetBlue has given up trying to connect, so let's print an error log
				if (!please.isRetry())
				{
					Log.e("SweetBlueExample", e.device().getName_debug() + " failed to connect with a status of " + e.status().name());
				}

				return please;
			}
		} );
	}

	private void writeChar()
	{
		m_bleDevice.write(MY_UUID, MY_DATA, readWriteEvent ->
		{
            if (readWriteEvent.wasSuccess())
            {
                Toast.makeText(MyActivity.this, "Write completed successfully!", Toast.LENGTH_LONG).show();
            }
            else
            {
                Toast.makeText(MyActivity.this, "Write failed with a status of " + readWriteEvent.status().toString(), Toast.LENGTH_LONG).show();
            }
        });
	}

	private void processDiscoveryEvent(BleManager.DiscoveryListener.DiscoveryEvent discoveryEvent)
	{
		// We're only going to connect to the first device we see, so let's stop the scan now. However, it's possible that more devices have already been discovered and will get piped
		// into the discovery listener, hence the need for the discovered boolean here
		if (!m_discovered)
		{
			m_discovered = true;

			// While SweetBlue will automatically stop the scan when you perform any other BLE operation, it's good practice to manually stop the scan here as in this case,
			// we're only concerned with the first device we find. Also, if there is no stopScan method, scanning will resume as soon as all other BLE operations are done.
			m_bleManager.stopAllScanning();

			// We only care about the DISCOVERED event. REDISCOVERED can get posted many times for a single device during a scan.
			if (discoveryEvent.was(BleManager.DiscoveryListener.LifeCycle.DISCOVERED))
			{
				// Grab the device from the DiscoveryEvent instance
				m_bleDevice = discoveryEvent.device();

				m_name.setText(m_bleDevice.getName_debug());
				m_state.setText(m_bleDevice.printState());
				connectDevice();
			}
		}
	}


}
