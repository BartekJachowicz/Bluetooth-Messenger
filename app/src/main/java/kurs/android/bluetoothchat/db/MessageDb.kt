package kurs.android.bluetoothchat.db

import android.arch.persistence.room.Database
import android.arch.persistence.room.Room
import android.arch.persistence.room.RoomDatabase
import android.arch.persistence.room.TypeConverters
import android.content.Context

@Database(entities = [Message::class], version = 1)
@TypeConverters(Converters::class)
abstract class MessageDb : RoomDatabase() {
    abstract fun messagesDao(): MessagesDao

    companion object {
        private var INSTANCE: MessageDb? = null

        fun getInstance(context: Context): MessageDb? {
            if (INSTANCE == null) {
                synchronized(MessageDb::class) {
                    INSTANCE = Room.databaseBuilder(context.applicationContext,
                            MessageDb::class.java, "messages.db").allowMainThreadQueries()
                            .build()
                }
            }
            return INSTANCE
        }

        fun destroyInstance() {
            INSTANCE = null
        }
    }
}
