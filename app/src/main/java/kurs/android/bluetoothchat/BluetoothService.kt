package kurs.android.bluetoothchat

import android.app.Service
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothDevice.EXTRA_DEVICE
import android.bluetooth.BluetoothSocket
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.*
import android.os.Process.THREAD_PRIORITY_BACKGROUND
import android.util.Log
import kurs.android.bluetoothchat.db.MessageDb
import kurs.android.bluetoothchat.db.MessagesDao
import java.io.BufferedInputStream
import java.io.IOException
import java.lang.Exception
import java.util.*

class BluetoothService : Service() {
    private val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
    private var serverThread: ServerThread? = null
    lateinit var clientThread: ClientThread
    lateinit var inputThread: InputThread
    private var outputHandler: OutputHandler? = null
    private lateinit var messegesDao: MessagesDao
    private lateinit var receiver: BroadcastReceiver

    private fun logError(error: Exception) {
        Log.e(TAG, error.message)
        Log.e(TAG, error.stackTrace.joinToString(separator = "\n"))
    }


    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    private fun startServerThread() {
        try {
            if (serverThread == null || serverThread!!.isAlive) {
                serverThread = ServerThread()
                serverThread?.start()
            }
        }
        catch (error: IOException) {
            logError(error)
            broadcastError(error.toString())
        }
    }

    private fun connectTo(device: BluetoothDevice) {
        clientThread = ClientThread(device)
        serverThread!!.interrupt()
        clientThread.start()
    }

    private fun endConnection() {
        try {
            inputThread.closeInput()
            outputHandler!!.closeOutput()
        }
        catch (error: Exception) {
            logError(error)
        }

    }

    private fun sendMessage(data: String) {
        if (outputHandler == null) {
            Handler().postDelayed({
                val message = outputHandler!!.obtainMessage()
                message.data.putString(EXTRA_MSG, data)
                outputHandler!!.sendMessage(message)
            }, 10000)
        }
    }

    private fun broadcastError(error: String) {
        val intent = Intent(ACTION_ERROR).apply { putExtra(EXTRA_MSG, error) }
        sendBroadcast(intent)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startServerThread()
            ACTION_CONNECT -> {
                val device = intent.getParcelableExtra<BluetoothDevice>(EXTRA_DEVICE)
                connectTo(device)
            }
            ACTION_END_CONNECTION -> endConnection()
            ACTION_SEND_MSG -> {
                val message = intent.getStringExtra(EXTRA_MSG)
                sendMessage(message)

            }
        }
        return START_NOT_STICKY
    }

    override fun onCreate() {
        super.onCreate()
        messegesDao = MessageDb.getInstance(this)!!.messagesDao()
        val filter = IntentFilter(ACTION_MSG_RECEIVED)
        receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                when (intent?.action) {
                    ACTION_MSG_RECEIVED -> {
                        val message = intent.getStringExtra(EXTRA_MSG)
                        val device = intent.getParcelableExtra<BluetoothDevice>(EXTRA_DEVICE)
                        addMessageToDB(message, device)
                    }
                }
            }

        }

        registerReceiver(receiver, filter)
    }


    private fun addMessageToDB(message: String, device: BluetoothDevice) {
        messegesDao.insert(kurs.android.bluetoothchat.db.Message(id = null, timestamp = Calendar.getInstance().time,
                conversationId = device.address, selfMessage = false, message = message))
    }

    fun createInputThread(socket: BluetoothSocket) {
        inputThread = InputThread(socket)
        inputThread.start()
    }

    fun createOutputHandler(socket: BluetoothSocket) {
        val handlerThread = HandlerThread("kurs.android", THREAD_PRIORITY_BACKGROUND)
        handlerThread.start()
        outputHandler = OutputHandler(handlerThread.looper, socket)
    }


    private fun broadcastConnected() {
        val intent = Intent(ACTION_CONNECTED)
        sendBroadcast(intent)
    }

    inner class ServerThread : Thread() {
        private val serverSocket = bluetoothAdapter.listenUsingRfcommWithServiceRecord("kurs.android", uuid)!!

        override fun run() {

            while (true) {
                try {
                    val socket = serverSocket.accept()!!
                    createInputThread(socket)
                    createOutputHandler(socket)
                    broadcastConnected()
                    break
                }
                catch (error: IOException) {
                    logError(error)
                }
                catch (error: Exception) {
                    logError(error)
                    break
                }
            }
        }
    }

    inner class ClientThread(device: BluetoothDevice) : Thread() {
        private val socket = device.createRfcommSocketToServiceRecord(uuid)!!

        override fun run() {
            bluetoothAdapter.cancelDiscovery()

            try {
                socket.connect()
                createInputThread(socket)
                createOutputHandler(socket)
                broadcastConnected()
            }
            catch (error: IOException) {
                logError(error)
            }

        }
    }

    inner class OutputHandler(looper: Looper, private val socket: BluetoothSocket) : Handler(looper) {
        private val stream = socket.outputStream

        override fun handleMessage(message: Message?) {
            if (message == null) return
            try {
                val data = message.data.getString(EXTRA_MSG)!!
                stream.write(data.toByteArray())
            }
            catch (error: IOException) {
                logError(error)
            }
            catch (error: Exception) {
                Log.e(TAG, error.message)
            }
        }

        fun closeOutput() {
            socket.use { _ ->
                stream.close()
                stopSelf()
            }
        }
    }

    inner class InputThread(private val socket: BluetoothSocket) : Thread() {
        private val stream = socket.inputStream

        override fun run() {
            val buffer = ByteArray(1024 * 16)
            while (true) {
                try {
                    val read = stream.read(buffer)
                    val message = String(buffer, 0, read)
                    Log.d(TAG, "Message was received: $message")
                    broadcastMessage(message)
                }
                catch (error: IOException) {
                    logError(error)
                    break
                }
                catch (error: Exception) {
                    Log.e(TAG, error.message)
                    break
                }
            }
        }

        private fun broadcastMessage(message: String) {
            val intent = Intent(ACTION_MSG_RECEIVED).apply {
                putExtra(EXTRA_MSG, message)
                putExtra(EXTRA_DEVICE, socket.remoteDevice)
            }
            sendBroadcast(intent)
        }


        fun closeInput() {
            socket.use {
                stream.close()
                interrupt()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(receiver)
    }

    companion object {
        val uuid = UUID.fromString("fb36491d-7c21-40ef-9f67-a63237b5bbea")!!
        val TAG = "alamakota.${BluetoothService::class.java.simpleName}"
        const val ACTION_START = "android.kurs.ACTION.START"
        const val ACTION_CONNECT = "android.kurs.ACTION.CONNECT"
        const val ACTION_MSG_RECEIVED = "android.kurs.ACTION.MSG_RECEIVED"
        const val ACTION_SEND_MSG = "android.kurs.ACTION.SEND_MSG"
        const val ACTION_END_CONNECTION = "android.kurs.ACTION.END_CONNECTION"
        const val ACTION_ERROR = "android.kurs.ACTION.ERROR"
        const val ACTION_CONNECTED = "android.kurs.ACTION.CONNECTED"
        const val EXTRA_MSG = "android.kurs.EXTRA.msg"


        @JvmStatic
        fun startService(context: Context) {
            val intent = Intent(context, BluetoothService::class.java).apply { action = ACTION_START }
            context.startService(intent)
        }

        @JvmStatic
        fun startConnection(context: Context, device: BluetoothDevice) {
            val intent = Intent(context, BluetoothService::class.java).apply {
                action = ACTION_CONNECT
                putExtra(EXTRA_DEVICE, device)
            }
            context.startService(intent)
        }

        @JvmStatic
        fun sendMessage(context: Context, message: String) {
            val intent = Intent(context, BluetoothService::class.java).apply {
                action = ACTION_SEND_MSG
                putExtra(EXTRA_MSG, message)
            }
            context.startService(intent)
        }

        @JvmStatic
        fun endConnection(context: Context) {
            val intent = Intent(context, BluetoothService::class.java).apply {
                action = ACTION_END_CONNECTION
            }
            context.stopService(intent)
        }
    }
}
