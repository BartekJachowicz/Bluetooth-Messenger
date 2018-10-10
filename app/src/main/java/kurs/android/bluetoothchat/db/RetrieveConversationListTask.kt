package kurs.android.bluetoothchat.db

import android.content.Context
import android.os.AsyncTask
import java.lang.ref.WeakReference

class RetrieveConversationListTask(context: Context) :
        AsyncTask<Unit, Unit, List<String>>() {
    private var context: WeakReference<Context> = WeakReference(context)

    override fun doInBackground(vararg p0: Unit?): List<String> {
        val ctx = context.get() ?: return listOf()

        val dao = MessageDb.getInstance(ctx)!!.messagesDao()
        return dao.getConversationList()
    }
}
