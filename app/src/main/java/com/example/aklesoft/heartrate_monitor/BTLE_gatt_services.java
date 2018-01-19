//package com.example.aklesoft.heartrate_monitor;
//
//import android.app.Activity;
//import android.app.ListActivity;
//import android.bluetooth.BluetoothDevice;
//import android.content.Intent;
//import android.os.Handler;
//import android.view.LayoutInflater;
//import android.view.View;
//import android.view.ViewGroup;
//import android.widget.BaseAdapter;
//import android.widget.Button;
//import android.widget.TextView;
//
//import java.util.ArrayList;
//
///**
// * Created by Thunder on 22.11.2017.
// */
//
//public class BTLE_gatt_services  extends ListActivity{
//
//    public LeDeviceListAdapter mLeDeviceListAdapter;
////    private BluetoothAdapter mBluetoothAdapter;
//    private boolean mScanning;
//    private Handler mHandler;
//
//    private static final int REQUEST_ENABLE_BT = 1;
//    // Stops scanning after 10 seconds.
//    private static final long SCAN_PERIOD = 10000;
//
////    @Override
////    public void onCreate(Bundle savedInstanceState) {
////        super.onCreate(savedInstanceState);
//////        getActionBar().setTitle(R.string.title_devices);
////        mHandler = new Handler();
////
////        // Use this check to determine whether BLE is supported on the device.  Then you can
////        // selectively disable BLE-related features.
////
////
////        // Initializes a Bluetooth adapter.  For API level 18 and above, get a reference to
////        // BluetoothAdapter through BluetoothManager.
////        final BluetoothManager bluetoothManager =
////                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
////        mBluetoothAdapter = bluetoothManager.getAdapter();
////
////        // Checks if Bluetooth is supported on the device.
////        if (mBluetoothAdapter == null) {
////            Toast.makeText(this, R.string.error_bluetooth_not_supported, Toast.LENGTH_SHORT).show();
////            finish();
////            return;
////        }
////    }
//
////    @Override
////    public boolean onCreateOptionsMenu(Menu menu) {
////        getMenuInflater().inflate(R.menu.main, menu);
////        if (!mScanning) {
////            menu.findItem(R.id.menu_stop).setVisible(false);
////            menu.findItem(R.id.menu_scan).setVisible(true);
////            menu.findItem(R.id.menu_refresh).setActionView(null);
////        } else {
////            menu.findItem(R.id.menu_stop).setVisible(true);
////            menu.findItem(R.id.menu_scan).setVisible(false);
////            menu.findItem(R.id.menu_refresh).setActionView(
////                    R.layout.actionbar_indeterminate_progress);
////        }
////        return true;
////    }
////
////    @Override
////    public boolean onOptionsItemSelected(MenuItem item) {
////        switch (item.getItemId()) {
////            case R.id.menu_scan:
////                mLeDeviceListAdapter.clear();
////                scanLeDevice(true, mBluetoothAdapter);
////                break;
////            case R.id.menu_stop:
////                scanLeDevice(false, mBluetoothAdapter);
////                break;
////        }
////        return true;
////    }
////    @Override
////    protected void onResume() {
////        super.onResume();
////
////        // Ensures Bluetooth is enabled on the device.  If Bluetooth is not currently enabled,
////        // fire an intent to display a dialog asking the user to grant permission to enable it.
////        if (!mBluetoothAdapter.isEnabled()) {
////            if (!mBluetoothAdapter.isEnabled()) {
////                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
////                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
////            }
////        }
////
////        // Initializes list view adapter.
////        mLeDeviceListAdapter = new LeDeviceListAdapter();
////        setListAdapter(mLeDeviceListAdapter);
////        scanLeDevice(true);
////    }
//
//
//
//    @Override
//    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
//        // User chose not to enable Bluetooth.
//        if (requestCode == REQUEST_ENABLE_BT && resultCode == Activity.RESULT_CANCELED) {
//            finish();
//            return;
//        }
//        super.onActivityResult(requestCode, resultCode, data);
//    }
//
//    @Override
//    protected void onPause() {
//        super.onPause();
////        scanLeDevice(false, mBluetoothAdapter);
//        mLeDeviceListAdapter.clear();
//    }
//
////    @Override
////    protected void onListItemClick(ListView l, View v, int position, long id) {
////        final BluetoothDevice device = mLeDeviceListAdapter.getDevice(position);
////        if (device == null) return;
//////        final Intent intent = new Intent(this, BTLE_DeviceControl.class);
//////        intent.putExtra(BTLE_DeviceControl.EXTRAS_DEVICE_NAME, device.getName());
//////        intent.putExtra(BTLE_DeviceControl.EXTRAS_DEVICE_ADDRESS, device.getAddress());
////        if (mScanning) {
////            mBluetoothAdapter.stopLeScan(mLeScanCallback);
////            mScanning = false;
////        }
//////        startActivity(intent);
////    }
//
//    public void GoToBlackMode(View view) {
//
//        Button b1 = (Button) findViewById(R.id.button_blackmode);
//
//        b1.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                Intent myIntent = new Intent(v.getContext(), BlackMode.class);
//                startActivityForResult(myIntent, 0);
//            }
//        });
//    }
//
//////////////////////////////////////////
//// Adapter for holding devices found through scanning.
//    public static class LeDeviceListAdapter extends BaseAdapter {
//        private ArrayList<BluetoothDevice> mLeDevices;
//        private LayoutInflater mInflator;
//
//        public LeDeviceListAdapter() {
//            super();
//            mLeDevices = new ArrayList<BluetoothDevice>();
////            mInflator = MainActivity.this.getLayoutInflater();
//        }
//
//        public void addDevice(BluetoothDevice device) {
//            if(!mLeDevices.contains(device)) {
//                mLeDevices.add(device);
//            }
//        }
//
//        public BluetoothDevice getDevice(int position) {
//            return mLeDevices.get(position);
//        }
//
//        public void clear() {
//            mLeDevices.clear();
//        }
//
//        @Override
//        public int getCount() {
//            return mLeDevices.size();
//        }
//
//        @Override
//        public Object getItem(int i) {
//            return mLeDevices.get(i);
//        }
//
//        @Override
//        public long getItemId(int i) {
//            return i;
//        }
//
//        @Override
//        public View getView(int i, View view, ViewGroup viewGroup) {
////            ViewHolder viewHolder;
////            // General ListView optimization code.
////            if (view == null) {
////                view = mInflator.inflate(R.layout.activity_main, null);
////                viewHolder = new ViewHolder();
////                viewHolder.deviceName = view.findViewById(R.id.HR_Device);
////                viewHolder.bt_status = view.findViewById(R.id.BT_Status);
////                view.setTag(viewHolder);
////            } else {
////                viewHolder = (ViewHolder) view.getTag();
////            }
////
////            BluetoothDevice device = mLeDevices.get(i);
////            final String deviceName = device.getName();
////            if (deviceName != null && deviceName.length() > 0)
////                viewHolder.deviceName.setText("HR Device: "+deviceName);
////            else
////                viewHolder.deviceName.setText("HR Device: "+device.getAddress());
////
////            return view;
//            return null;
//        }
//    }
//
////    // Device scan callback.
////    public BluetoothAdapter.LeScanCallback mLeScanCallback =
////            new BluetoothAdapter.LeScanCallback() {
////
////                @Override
////                public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {
////                    runOnUiThread(new Runnable() {
////                        @Override
////                        public void run() {
////                            mLeDeviceListAdapter.addDevice(device);
////                            mLeDeviceListAdapter.notifyDataSetChanged();
////                        }
////                    });
////                }
////            };
//
//    static class ViewHolder {
//        TextView deviceName;
//        TextView bt_status;
//    }
//
//}
