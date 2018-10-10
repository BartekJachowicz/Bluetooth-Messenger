package kurs.android.bluetoothchat.db

import android.content.Context
import android.os.AsyncTask
import java.lang.ref.WeakReference
import java.util.*

class AddMessageTask(
        context: Context,
        private val timestamp: Date,
        private val conversationId: String,
        private val selfMessage: Boolean,
        private val message: String
        ) : AsyncTask<Unit, Unit, Unit>() {
    private var context: WeakReference<Context> = WeakReference(context)

    override fun doInBackground(vararg p0: Unit?) {
        val ctx = context.get() ?: return

        val dao = MessageDb.getInstance(ctx)!!.messagesDao()
        dao.insert(Message(
                id = null, timestamp = timestamp, conversationId = conversationId,
                selfMessage = selfMessage, message = message))
    }
}
