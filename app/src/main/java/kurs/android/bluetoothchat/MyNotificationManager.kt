package kurs.android.bluetoothchat

import android.app.NotificationManager
import android.content.Context
import android.support.v4.app.NotificationCompat



class MyNotificationManager(private val context: Context) {

    fun sendNotification(message: String) {
        val mNotific = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val ncode = 101
        val name = "Ragav"
        val ChannelID = "bluetoothChat"

        val builder = NotificationCompat.Builder(context, ChannelID)
                .setContentTitle(context.packageName)
                .setContentText(message)
                .setSmallIcon(R.mipmap.ic_launcher_round)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)

        mNotific.notify(ncode, builder.build())
    }
}