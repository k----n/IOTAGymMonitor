package com.idevicesinc.sweetblue.ble_util;

import android.app.Activity;
import android.os.Bundle;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import com.idevicesinc.sweetblue.BleDevice;
import com.idevicesinc.sweetblue.BleDevice.BondListener;
import com.idevicesinc.sweetblue.BleDevice.DefaultConnectionFailListener;
import com.idevicesinc.sweetblue.BleDeviceState;
import com.idevicesinc.sweetblue.BleManager;
import com.idevicesinc.sweetblue.BleManager.DiscoveryListener;
import com.idevicesinc.sweetblue.BleManager.NativeStateListener;
import com.idevicesinc.sweetblue.BleManager.StateListener;
import com.idevicesinc.sweetblue.BleManager.UhOhListener;
import com.idevicesinc.sweetblue.BleManagerConfig;
import com.idevicesinc.sweetblue.BleManagerState;
import com.idevicesinc.sweetblue.DeviceStateListener;
import com.idevicesinc.sweetblue.ManagerStateListener;
import com.idevicesinc.sweetblue.utils.BluetoothEnabler;
import com.idevicesinc.sweetblue.utils.Interval;
import com.idevicesinc.sweetblue.utils.Utils_String;
import java.util.ArrayList;
import java.util.List;


// A slightly more in-depth application to show how to do various operations with SweetBlue.
public class MyActivity extends Activity
{
    // We're keeping an instance of BleManager for convenience, but it's not really necessary since it's a singleton. It's helpful so you
    // don't have to keep passing in a Context to retrieve it.
    private BleManager m_bleManager;

    // Here we maintain a list of all discovered BleDevices so that we can display that later on in a RecyclerView.
    private List<BleDevice> m_bleDeviceList = new ArrayList<>();

    private RecyclerViewAdapter m_recyclerViewAdapter = new RecyclerViewAdapter();

    // We arbitrarily create a scan timeout of 5 seconds.
    private static final Interval SCAN_TIMEOUT = Interval.secs(5.0);



    @Override protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.my_activity);

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

        // Disabling undiscovery so the list doesn't jump around...ultimately a UI problem so should be fixed there eventually.
        config.undiscoveryKeepAlive = Interval.DISABLED;

        // Get the instance of the BleManager, and pass in the config.
        m_bleManager = BleManager.get(this, config);

        // Set a listener for when we get an error, otherwise known as an "UhOh". These UhOhEvents also come with some best-guess suggestions for remedies.
        m_bleManager.setListener_UhOh(this::onUhOh);

        // You must cast this method reference, as there are 2 setListener_State() methods in BleManager (this will change in v3 to only be one)
        m_bleManager.setListener_State((ManagerStateListener) this::onManagerStateEvent);

        // This listener listens to the native state of the android BluetoothManager (as opposed to SweetBlue's managed state above)
        m_bleManager.setListener_NativeState(this::onNativeStateEvent);

        // Set the discovery listener. You can pass in a listener when calling startScan() if you want, but in most cases, this is preferred so you don't
        // have to keep passing in the listener when calling any of the startScan methods.
        m_bleManager.setListener_Discovery(this::onDiscovery);

        // The BluetoothEnabler will take care of requesting the necessary permissions on Android M and above. It needs A> Bluetooth to be on of course,
        // B> Location permissions, and C> Location services enabled in order for a BLE scan to return any results.
        BluetoothEnabler.start(this, new BluetoothEnabler.DefaultBluetoothEnablerFilter()
        {
            @Override public Please onEvent(BluetoothEnablerEvent bluetoothEnablerEvent)
            {

                // We call super here to allow the default implementation take care of the details of enabling. We want to make sure
                // it does everything it needs to do before calling any start scan methods
                Please please = super.onEvent(bluetoothEnablerEvent);

                // If the enabler is now done, we can proceed.
                if(bluetoothEnablerEvent.isDone())
                {
                    setEnableButton();

                    setDisableButton();

                    setUnbondAllButton();

                    setNukeButton();

                    setScanInfinitelyButton();

                    setStopScanButton();

                    setScanForFiveSecondsButton();

                    setScanPeriodicallyButton();

                    setAbstractedStatesTextView();

                    setNativeStatusTextView();

                    setRecyclerView();
                }

                // Return the Please we got from above now.
                return please;
            }
        });
    }

    private void setEnableButton()
    {
        final Button enableButton = findViewById(R.id.enableButton);

        enableButton.setOnClickListener(view -> m_bleManager.turnOn());
    }

    private void setDisableButton()
    {
        final Button disableButton = findViewById(R.id.disableButton);

        disableButton.setOnClickListener(view -> m_bleManager.turnOff());
    }

    private void setUnbondAllButton()
    {
        final Button unbondAllButton = findViewById(R.id.unbondAllButton);

        unbondAllButton.setOnClickListener(view -> m_bleManager.unbondAll());
    }

    private void setNukeButton()
    {
        final Button nukeButton = findViewById(R.id.nukeButton);

        nukeButton.setOnClickListener(view -> m_bleManager.reset());
    }

    private void setScanInfinitelyButton()
    {
        final Button scanInfinitelyButton = findViewById(R.id.scanInfinitelyButton);

        scanInfinitelyButton.setOnClickListener(view -> m_bleManager.startScan());
    }

    private void setStopScanButton()
    {
        final Button stopScanButton = findViewById(R.id.stopScanButton);

        stopScanButton.setOnClickListener(view ->
        {
            // Use stopAllScanning here so that if you decide to switch to/from periodic scan to an infinite one, you don't have to change
            // the stop method to call.
            m_bleManager.stopAllScanning();
        });
    }


    private void setScanForFiveSecondsButton()
    {
        final Button scanForFiveSecondsButton = findViewById(R.id.scanForFiveSecondsButton);

        int timeout = (int) SCAN_TIMEOUT.secs();

        scanForFiveSecondsButton.setText(getString(R.string.scan_for_x_sec).replace("{{seconds}}", timeout + ""));

        scanForFiveSecondsButton.setOnClickListener(view -> m_bleManager.startScan(SCAN_TIMEOUT));
    }

    private void setScanPeriodicallyButton()
    {
        final Button scanPeriodicallyButton = findViewById(R.id.scanPeriodicallyButton);

        int timeout = (int) SCAN_TIMEOUT.secs();

        scanPeriodicallyButton.setText(getString(R.string.scan_for_x_sec_repeated).replace("{{seconds}}", timeout + ""));

        scanPeriodicallyButton.setOnClickListener(view -> m_bleManager.startPeriodicScan(SCAN_TIMEOUT, SCAN_TIMEOUT));
    }

    private void setAbstractedStatesTextView()
    {
        final TextView abstractedStatesTextView = findViewById(R.id.abstractedStatesTextView);

        abstractedStatesTextView.setText(Utils_String.makeStateString(BleManagerState.values(), m_bleManager.getStateMask()));
    }

    private void setNativeStatusTextView()
    {
        final TextView nativeStatusTextView = findViewById(R.id.nativeStatusTextView);

        nativeStatusTextView.setText(Utils_String.makeStateString(BleManagerState.values(), m_bleManager.getNativeStateMask()));
    }

    private void setRecyclerView()
    {
        final RecyclerView recyclerView = findViewById(R.id.recyclerView);

        recyclerView.setAdapter(m_recyclerViewAdapter);

        recyclerView.getItemAnimator().setChangeDuration(0);
    }


    // Adapter for our recyclerview. This hooks up the layout for each device, and sets the click listeners for the relevant buttons.
    private class RecyclerViewAdapter extends RecyclerView.Adapter<ViewHolder>
    {
        @Override public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType)
        {
            final View view = LayoutInflater.from(MyActivity.this).inflate(R.layout.device_cell, parent, false);

            return new ViewHolder(view);
        }

        @Override public void onBindViewHolder(final ViewHolder viewHolder, final int position)
        {
            final BleDevice device = m_bleDeviceList.get(position);

            // Background color.
            viewHolder.contentLinearLayout.setBackgroundColor(getResources().getColor(position % 2 == 0 ? R.color.light_blue : R.color.dark_blue));

            // Device name.
            String name = device.getName_normalized();

            if(name.length() == 0)
            {
                name = device.getMacAddress();
            }
            else
            {
                name += "(" + device.getMacAddress() + ")";
            }

            viewHolder.nameTextView.setText(name);

            // Connect button.
            viewHolder.connectButton.setOnClickListener(view -> device.connect());

            // Disconnect button.
            viewHolder.disconnectButton.setOnClickListener(view -> device.disconnect());

            // Bond button
            viewHolder.bondButton.setOnClickListener(view -> device.bond());

            // Unbond button.
            viewHolder.unbondButton.setOnClickListener(view -> device.unbond());

            // Status.
            viewHolder.statusTextView.setText(Utils_String.makeStateString(BleDeviceState.values(), device.getStateMask()));
        }

        @Override public int getItemCount()
        {
            return m_bleDeviceList.size();
        }
    }

    private class ViewHolder extends RecyclerView.ViewHolder
    {
        private LinearLayout contentLinearLayout;

        private TextView nameTextView;

        private Button connectButton;

        private Button disconnectButton;

        private Button bondButton;

        private Button unbondButton;

        private TextView statusTextView;

        private ViewHolder(View view)
        {
            super(view);

            contentLinearLayout = view.findViewById(R.id.contentLinearLayout);

            nameTextView = view.findViewById(R.id.nameTextView);

            connectButton = view.findViewById(R.id.connectButton);

            disconnectButton = view.findViewById(R.id.disconnectButton);

            bondButton = view.findViewById(R.id.bondButton);

            unbondButton = view.findViewById(R.id.unbondButton);

            statusTextView = view.findViewById(R.id.statusTextView);
        }
    }

    private void onUhOh(UhOhListener.UhOhEvent uhOhEvent)
    {
        AlertManager.onEvent(this, uhOhEvent);
    }

    private void onManagerStateEvent(StateListener.StateEvent stateEvent)
    {
        setAbstractedStatesTextView();

        setNativeStatusTextView();
    }

    private void onNativeStateEvent(NativeStateListener.NativeStateEvent nativeStateEvent)
    {
        setAbstractedStatesTextView();

        setNativeStatusTextView();
    }

    private void onDiscovery(DiscoveryListener.DiscoveryEvent discoveryEvent)
    {
        if(discoveryEvent.was(DiscoveryListener.LifeCycle.DISCOVERED))
        {
            BleDevice device = discoveryEvent.device();

            m_bleDeviceList.add(device);

            m_recyclerViewAdapter.notifyItemInserted(m_bleDeviceList.size() - 1);

            // You must cast this method reference, as there are 2 setListener_State() methods in BleDevice (this will change in v3 to only be one)
            device.setListener_State((DeviceStateListener) this::onDeviceStateEvent);

            device.setListener_ConnectionFail(new SimpleConnectionFailListener());

            device.setListener_Bond(this::onBondEvent);
        }
        else if(discoveryEvent.was(DiscoveryListener.LifeCycle.UNDISCOVERED))
        {
            BleDevice device = discoveryEvent.device();

            int position = m_bleDeviceList.indexOf(device);

            m_bleDeviceList.remove(device);

            m_recyclerViewAdapter.notifyItemRemoved(position);

            device.setListener_State((BleDevice.StateListener) null);

            device.setListener_ConnectionFail(null);

            device.setListener_Bond(null);
        }
    }

    private void onDeviceStateEvent(BleDevice.StateListener.StateEvent stateEvent)
    {
        BleDevice device = stateEvent.device();

        int position = m_bleDeviceList.indexOf(device);

        m_recyclerViewAdapter.notifyItemChanged(position);
    }

    private void onBondEvent(BondListener.BondEvent bondEvent)
    {
        final String message = bondEvent.device().getName_debug() + " bond attempt finished with status " + bondEvent.status();

        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }



    private class SimpleConnectionFailListener extends DefaultConnectionFailListener
    {
        @Override public Please onEvent(final ConnectionFailEvent connectionFailEvent)
        {
            // Like in the BluetoothEnabler callback higher up in this class, we want to allow the default implementation do what it needs to do
            // However, in this case, we check the resulting Please that is returned to determine if we need to do anything yet.
            Please please = super.onEvent(connectionFailEvent);

            // If the returned please is NOT a retry, then SweetBlue has given up trying to connect, so let's show an error.
            if(!please.isRetry())
            {
                // As the ConnectionFailListener returns a value, it cannot be automatically posted to the main thread. So, we post to the UI thread
                // here to avoid any crashes.
                runOnUiThread(() ->
                {
                    final String message = connectionFailEvent.device().getName_debug() + " connection failed with " + connectionFailEvent.failureCountSoFar() + " retries - " + connectionFailEvent.status();

                    Toast.makeText(MyActivity.this, message, Toast.LENGTH_LONG).show();
                });
            }

            // Return the please from the default listener implementation.
            return please;
        }
    }

}