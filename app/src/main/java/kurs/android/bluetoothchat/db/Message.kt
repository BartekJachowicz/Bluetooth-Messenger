package kurs.android.bluetoothchat.db

import android.arch.persistence.room.ColumnInfo
import android.arch.persistence.room.Entity
import android.arch.persistence.room.Index
import android.arch.persistence.room.PrimaryKey
import java.util.*


@Entity(tableName = "messages", indices = [Index("conversation"), Index("timestamp")])
data class Message(
        @PrimaryKey(autoGenerate = true) var id: Long?,
        @ColumnInfo(name = "timestamp") var timestamp: Date,
        @ColumnInfo(name = "conversation") var conversationId: String,
        @ColumnInfo(name = "self") var selfMessage: Boolean,
        @ColumnInfo(name = "message") var message: String
)
