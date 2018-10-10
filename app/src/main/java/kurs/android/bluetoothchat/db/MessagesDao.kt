package kurs.android.bluetoothchat.db

import android.arch.persistence.room.Dao
import android.arch.persistence.room.Insert
import android.arch.persistence.room.OnConflictStrategy.REPLACE
import android.arch.persistence.room.Query

@Dao
interface MessagesDao {
    @Query("SELECT * from messages")
    fun getAll(): List<Message>

    @Query("SELECT * from messages WHERE conversation = :conversationId ORDER BY timestamp")
    fun getConversation(conversationId: String): List<Message>

    @Query("SELECT DISTINCT conversation from messages")
    fun getConversationList(): List<String>

    @Insert(onConflict = REPLACE)
    fun insert(message: Message)

    @Query("DELETE from messages where conversation = :conversationId")
    fun remove(conversationId: String)
}
