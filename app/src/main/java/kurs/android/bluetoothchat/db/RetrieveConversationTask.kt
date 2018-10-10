package kurs.android.bluetoothchat.db

import android.content.Context
import android.os.AsyncTask
import java.lang.ref.WeakReference

class RetrieveConversationTask(context: Context, private val conversationId: String) :
        AsyncTask<Unit, Unit, List<Message>>() {
    private var context: WeakReference<Context> = WeakReference(context)

    override fun doInBackground(vararg p0: Unit?): List<Message> {
        val ctx = context.get() ?: return listOf()

        val dao = MessageDb.getInstance(ctx)!!.messagesDao()
        return dao.getConversation(conversationId)
    }
}
