package kurs.android.bluetoothchat

import android.arch.lifecycle.ViewModel
import android.arch.lifecycle.ViewModelProviders
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothAdapter.ACTION_DISCOVERY_FINISHED
import android.bluetooth.BluetoothAdapter.ACTION_DISCOVERY_STARTED
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothDevice.ACTION_FOUND
import android.bluetooth.BluetoothDevice.EXTRA_DEVICE
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.*
import android.widget.ArrayAdapter
import android.widget.TextView

import kotlinx.android.synthetic.main.activity_find_device.*
import kotlinx.android.synthetic.main.content_find_device.*
import kotlin.collections.ArrayList

class FindDeviceActivity : AppCompatActivity() {
    lateinit var viewModel: FindDeviceViewModel
    val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
    lateinit var adapter: DeviceListAdapter
    lateinit var receiver: BroadcastReceiver

    private fun addReceiver() {
        val filter = IntentFilter().apply {
            addAction(ACTION_FOUND)
            addAction(ACTION_DISCOVERY_STARTED)
            addAction(ACTION_DISCOVERY_FINISHED)
        }
        receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                when (intent?.action) {
                    ACTION_FOUND -> {
                        val device = intent.getParcelableExtra<BluetoothDevice>(EXTRA_DEVICE)
                        Log.d(TAG, "Found device: ${device.address}")
                        if (viewModel.addDevice(device)) adapter.notifyDataSetChanged()
                    }
                    ACTION_DISCOVERY_STARTED -> {
                        viewModel.clearDevicesList()
                    }
                }
            }
        }

        registerReceiver(receiver, filter)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_find_device)
        setSupportActionBar(toolbar)
        setTitle(R.string.find_device_title)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        viewModel = ViewModelProviders.of(this).get(FindDeviceViewModel::class.java)
        addReceiver()
        adapter = DeviceListAdapter(this, viewModel.foundDevices)
        found_devices.adapter = adapter
        refreshDevices()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_find_device, menu)
        return true
    }

    private fun returnDeviceAsResult(device: BluetoothDevice) {
        val data = Intent()
        data.putExtra(EXTRA_DEVICE, device)
        setResult(RESULT_OK, data)
        finish()
    }

    inner class DeviceListAdapter(context: Context, list: List<BluetoothDevice>) : ArrayAdapter<BluetoothDevice>(context, android.R.layout.simple_list_item_1, list) {
        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val view = convertView
                    ?: LayoutInflater.from(context).inflate(android.R.layout.simple_list_item_1, null)
            val textView = view.findViewById<TextView>(android.R.id.text1)
            val device = getItem(position) ?: return view
            view.setOnClickListener { returnDeviceAsResult(device) }
            textView.text = device.name ?: device.address
            return view
        }
    }

    class FindDeviceViewModel : ViewModel() {
        private val deviceArray = ArrayList<BluetoothDevice>()

        val foundDevices: List<BluetoothDevice>
            get() = deviceArray

        fun addDevice(device: BluetoothDevice): Boolean {
            if (deviceArray.contains(device))
                return false;
            return deviceArray.add(device)
        }

        fun clearDevicesList() {
            deviceArray.clear()
        }

    }

    private fun refreshDevices(): Boolean {
        if (!bluetoothAdapter.isEnabled) {
            Log.d(TAG, "Not able to discover devices if bluetooth is off")
            Snackbar.make(find_devices_layout, "Bluetooth needs to be enabled to refresh", Snackbar.LENGTH_LONG).show()
        }
        if(bluetoothAdapter.isDiscovering) bluetoothAdapter.cancelDiscovery()
        bluetoothAdapter.startDiscovery()
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        return when (item?.itemId) {
            R.id.refresh_find_device -> {
                viewModel.clearDevicesList()
                adapter.notifyDataSetChanged()
                refreshDevices()
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(receiver)
    }

    companion object {
        private val TAG = "alamakota.${FindDeviceActivity::class.java.simpleName}"
    }
}
