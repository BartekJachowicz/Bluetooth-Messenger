package kurs.android.bluetoothchat

import android.Manifest.permission.*
import android.app.AlertDialog
import android.arch.lifecycle.ViewModel
import android.arch.lifecycle.ViewModelProviders
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothAdapter.*
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothDevice.ACTION_FOUND
import android.bluetooth.BluetoothDevice.EXTRA_DEVICE
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.*
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.TextView

import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.content_main.*
import kurs.android.bluetoothchat.BluetoothService.Companion.ACTION_MSG_RECEIVED
import kurs.android.bluetoothchat.db.Message
import kurs.android.bluetoothchat.db.MessageDb
import kurs.android.bluetoothchat.db.MessagesDao
import kurs.android.bluetoothchat.db.RetrieveConversationListTask
import java.util.*




private const val PERMISSION_REQUEST = 241
private const val DEVICE_REQUEST = 101


class MainActivity : AppCompatActivity() {
    private val permissions = arrayOf(BLUETOOTH_ADMIN, BLUETOOTH, ACCESS_COARSE_LOCATION)
    private val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
    lateinit var dao : MessagesDao
    lateinit var adapter: DeviceListAdapter
    lateinit var viewModel: DeviceViewModel
    lateinit var notifyManager: MyNotificationManager
    lateinit var receiver: BroadcastReceiver


    private fun ensurePermissionsAreGranted() {
        val notGranted = permissions.filter { ContextCompat.checkSelfPermission(this, it) != PERMISSION_GRANTED }
        if (notGranted.isNotEmpty()) {
            Log.d(TAG, "Asking for permissions: $notGranted")
            ActivityCompat.requestPermissions(this, notGranted.toTypedArray(), PERMISSION_REQUEST)
        }
        else {
            Log.d(TAG, "All permissions are granted")
        }
    }

    private fun addReceiver() {
        val filter = IntentFilter().apply {
            addAction(ACTION_MSG_RECEIVED)
        }
        receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                when(intent?.action) {
                    ACTION_MSG_RECEIVED -> {
                        viewModel.addDevices(dao.getConversationList())
                        adapter.notifyDataSetChanged()
                        notifyManager.sendNotification("New Message!")
                    }
                }
            }
        }

        registerReceiver(receiver, filter)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)
        setTitle(R.string.main_title)
        sendButton.setOnClickListener { _ ->
            Log.d(TAG, "Starting FindDeviceActivity activity")
            val intent = Intent(this, FindDeviceActivity::class.java)
            startActivityForResult(intent, DEVICE_REQUEST)
        }
        ensurePermissionsAreGranted()
        enableBluetooth()
        makeDeviceDiscoverable()
        notifyManager = MyNotificationManager(this)
        addReceiver()
        BluetoothService.startService(this)

        dao = MessageDb.getInstance(this)!!.messagesDao()

        viewModel = ViewModelProviders.of(this).get(DeviceViewModel::class.java)
        addViewModelListener()
        adapter = DeviceListAdapter(this, viewModel.Devices)
        chats.adapter = adapter
    }

    private fun addViewModelListener(){
        val devices = RetrieveConversationListTask(this).execute().get()
        viewModel.addDevices(devices)

    }


    private fun changeNameAction(): Boolean {
        val builder = AlertDialog.Builder(this)
        val view = layoutInflater.inflate(R.layout.dialog_change_name, null)
        val inputText = view.findViewById<EditText>(R.id.change_input)
        inputText.setText(bluetoothAdapter.name)
        builder.setView(view)
                .setPositiveButton(R.string.change_name_button) { _, _ ->
                    val name = inputText.text.toString()
                    when {
                        !bluetoothAdapter.isEnabled -> {
                            Log.d(TAG, "Changing name is not possible, bluetooth must be enabled");
                            Snackbar.make(main_layout, "Bluetooth needs to be enabled to change name", Snackbar.LENGTH_LONG).show()
                        }
                        name.isEmpty() -> {
                            Log.d(TAG, "Not able to set empty name")
                            Snackbar.make(main_layout, "Device name must be nonempty", Snackbar.LENGTH_LONG).show()
                        }
                        else -> {
                            Log.d(TAG, "Changing device name from ${bluetoothAdapter.name} to $name")
                            bluetoothAdapter.name = name
                        }
                    }
                }
                .show()
        return true
    }

    private fun enableBluetooth() {
        if(!bluetoothAdapter.isEnabled) {
            val intent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivity(intent)
        }
    }

    private fun makeDeviceDiscoverable() {
        if(bluetoothAdapter.scanMode != SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
            val intent = Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE).apply { putExtra(EXTRA_DISCOVERABLE_DURATION, 300) }
            startActivity(intent)
        }
    }

    private fun refreshDevices(): Boolean {
        if (!bluetoothAdapter.isEnabled) {
            Log.d(TAG, "Not able to discover devices if bluetooth is off")
            Snackbar.make(main_layout, "Bluetooth needs to be enabled to refresh", Snackbar.LENGTH_LONG).show()
        }
        if (bluetoothAdapter.isDiscovering) bluetoothAdapter.cancelDiscovery()
        bluetoothAdapter.startDiscovery()
        return true
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            DEVICE_REQUEST -> {
                if (data == null)
                    return
                val device = data.getParcelableExtra<BluetoothDevice>(EXTRA_DEVICE)
                startNewChatWith(device)
            }

        }
    }

    private fun startNewChatWith(device: BluetoothDevice) {
        Log.d(TAG, "Starting chat with $device")
        val intent = Intent(this, ChatActivity::class.java).apply { putExtra(EXTRA_DEVICE, device) }
        startActivity(intent)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        return when (item?.itemId) {
            R.id.change_name -> changeNameAction()
            R.id.refresh_main -> refreshDevices()
            else -> super.onOptionsItemSelected(item)
        }
    }

    companion object {
        private val TAG = "alamakota.${MainActivity::class.java.simpleName}"
    }

    inner class DeviceListAdapter(context: Context, list: List<String>) : ArrayAdapter<String>(context, android.R.layout.simple_list_item_1, list) {
        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val view = convertView
                    ?: LayoutInflater.from(context).inflate(android.R.layout.simple_list_item_1, null)
            val textView = view.findViewById<TextView>(android.R.id.text1)
            val device = getItem(position) ?: return view
            view.setOnClickListener {
                startNewChatWith(bluetoothAdapter.getRemoteDevice(device))
            }
            view.setOnLongClickListener {
                showRemoveDialog(device)
                return@setOnLongClickListener true
            }
            textView.text = device
            return view
        }
    }

    private fun showRemoveDialog(device: String){
        val builder: AlertDialog.Builder = AlertDialog.Builder(this)

        builder.setTitle("Remove")
                .setMessage("Are you sure you want to delete this conversation?")
                .setPositiveButton("OK") { dialog, which ->
                    dao.remove(device)
                    viewModel.removeDevice(device)
                    adapter.notifyDataSetChanged()
                }
                .setNegativeButton("Cancel") { dialog, which ->
                    // do nothing
                }
                .show()
    }

    class DeviceViewModel : ViewModel() {
        private val deviceArray = ArrayList<String>()

        val Devices: List<String>
            get() = deviceArray

        fun addDevices(devices: List<String>): Boolean{
            for (device: String in devices){
                if (!deviceArray.contains(device))
                    deviceArray.add(device)
            }
            return true
        }

        fun removeDevice(device: String): Boolean{
            if(deviceArray.contains(device))
                return deviceArray.remove(device)
            return false
        }
    }
}
