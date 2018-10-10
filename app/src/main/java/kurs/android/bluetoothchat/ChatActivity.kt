package kurs.android.bluetoothchat

import android.arch.lifecycle.ViewModel
import android.arch.lifecycle.ViewModelProviders
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothDevice.EXTRA_DEVICE
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.provider.AlarmClock.EXTRA_MESSAGE
import android.support.v7.app.AppCompatActivity
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.ArrayAdapter
import android.widget.TextView

import kotlinx.android.synthetic.main.activity_chat.*
import kotlinx.android.synthetic.main.content_chat.*
import kurs.android.bluetoothchat.BluetoothService.Companion.ACTION_CONNECTED
import kurs.android.bluetoothchat.BluetoothService.Companion.ACTION_MSG_RECEIVED
import kurs.android.bluetoothchat.db.Message
import kurs.android.bluetoothchat.db.MessageDb
import kurs.android.bluetoothchat.db.MessagesDao
import java.util.*

class ChatActivity : AppCompatActivity() {
    lateinit var viewModel: MessagesViewModel
    lateinit var adapter: MessagesListAdapter
    lateinit var dao : MessagesDao
    lateinit var device : BluetoothDevice
    lateinit var receiver: BroadcastReceiver



    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        BluetoothService.endConnection(this)
        return super.onOptionsItemSelected(item)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val intent = getIntent()
        device = intent.getParcelableExtra<BluetoothDevice>(EXTRA_DEVICE)
        BluetoothService.startConnection(this, device)

        dao = MessageDb.getInstance(this)!!.messagesDao()

        send_button.setOnClickListener {
            sendMessage()
        }
        send_button.isEnabled = false;

        viewModel = ViewModelProviders.of(this).get(MessagesViewModel::class.java)
        viewModel.addMessages(dao.getConversation(device.address))
        viewModel.device = device

        addReceiver()

        adapter = MessagesListAdapter(this, viewModel.Messages)
        messages_list.adapter = adapter
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(receiver)
    }

    private fun addReceiver() {
        val filter = IntentFilter().apply {
            addAction(ACTION_MSG_RECEIVED)
            addAction(ACTION_CONNECTED)
        }
        receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                when (intent?.action) {
                    ACTION_MSG_RECEIVED-> {
                        val message_content = intent!!.getStringExtra(EXTRA_MESSAGE)

                        val message = Message(id = null, timestamp = Calendar.getInstance().time,
                                conversationId = device.address, selfMessage = false, message = message_content)

                        viewModel.addMessage(message)
                        adapter.notifyDataSetChanged()
                    }
                    ACTION_CONNECTED ->{
                        send_button.isEnabled = true
                    }
                }
            }
        }

        registerReceiver(receiver, filter)
    }

    private fun sendMessage(){
        if(editText.text.toString() == "")
            return
        val message_content = editText.text.toString()
        editText.text.clear()

        val message = Message(id = null, timestamp = Calendar.getInstance().time,
                conversationId = device.address, selfMessage = true, message = message_content)

        viewModel.addMessage(message)
        adapter.notifyDataSetChanged()
        dao.insert(message)

        val inputManager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager

        inputManager.hideSoftInputFromWindow(currentFocus!!.windowToken,
                InputMethodManager.HIDE_NOT_ALWAYS)

        messages_list.smoothScrollToPosition(adapter.count - 1)

        BluetoothService.sendMessage(this, message_content)
    }

    inner class MessagesListAdapter(context: Context, list: List<Message>) : ArrayAdapter<Message>(context, android.R.layout.simple_list_item_1, list) {
        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val view = convertView
                    ?: LayoutInflater.from(context).inflate(R.layout.message_content, null)

            val message = getItem(position)

            val send_message = view.findViewById<TextView>(R.id.send_message)
            val recived_message = view.findViewById<TextView>(R.id.recived_message)

            if(message.selfMessage){
                send_message.text = message.message
                send_message.visibility = View.VISIBLE
                recived_message.visibility = View.INVISIBLE
            }
            else{
                recived_message.text = message.message
                recived_message.visibility = View.VISIBLE
                send_message.visibility = View.INVISIBLE
            }

            return view
        }
    }

    class MessagesViewModel : ViewModel() {
        private val messagesArray = ArrayList<Message>()
        lateinit var device: BluetoothDevice

        val Messages: List<Message>
            get() = messagesArray

        fun addMessage(message: Message): Boolean{
            if (!messagesArray.contains(message))
                return messagesArray.add(message)
            return false
        }

        fun addMessages(messages: List<Message>): Boolean{
            for(message: Message in messages) {
                if (!messagesArray.contains(message))
                    messagesArray.add(message)
            }
            return false
        }
    }
}
