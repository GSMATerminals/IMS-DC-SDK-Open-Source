/*
 *   Copyright 2025-China Telecom Research Institute.
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package com.ct.ertclib.dc.core.port.dao

import androidx.lifecycle.LiveData
import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.ct.ertclib.dc.core.data.model.MessageEntity

@Dao
interface MessageDao {
    @Query("SELECT COUNT(*) FROM MESSAGES")
    fun queryMessageCount(): Long

    @Query("SELECT * FROM messages WHERE conversationId = :conversationId")
    fun queryAllMessage(conversationId: Int): PagingSource<Int, MessageEntity>

    @Query("SELECT * FROM messages WHERE conversationId = :conversationId ORDER BY _id desc LIMIT :pageSize OFFSET :offset")
    fun queryAllMessageList(conversationId: Int, pageSize: Int, offset: Int): List<MessageEntity>

    @Query("SELECT _id FROM messages WHERE conversationId = :conversationId ORDER BY _id desc LIMIT 1")
    fun queryLatestMessageIDByConversationId(conversationId: Int): Int

    @Query("SELECT * FROM messages WHERE _id = :id")
    fun queryMessage(id: Int): MessageEntity

    /**
     *
     * @param senderId Int 目前只支持单一对话，senderId对应conversationId
     * @param selfId Int 发送方id
     * @return PagingSource<Int, MessageData>
     */
    @Query("SELECT * FROM messages WHERE conversationId = :senderId AND selfId=:selfId")
    fun getMessageByContactId(senderId: Int, selfId: Int): PagingSource<Int, MessageEntity>

    @Query("SELECT COUNT(*) FROM messages WHERE conversationId = :senderId AND selfId = :selfId AND receiveTime <= :timestamp")
    fun queryMessageCount(senderId: Int, selfId: Int, timestamp: Long): Int

    @Query("SELECT COUNT(*) FROM messages WHERE conversationId = :senderId AND selfId = :selfId AND read = 0 AND receiveTime <= :timestamp")
    fun queryUnreadCount(senderId: Int, selfId: Int, timestamp: Long): Int

    @Query("SELECT * FROM messages WHERE message LIKE '%' || :keyword || '%'")
    fun queryMessage(keyword: String): LiveData<List<MessageEntity>>

    @Insert
    fun insertMessage(message: MessageEntity): Long

    @Update
    fun updateMessage(message: MessageEntity): Int

    @Query("DELETE FROM messages WHERE _id = :id")
    fun deleteMessageById(id: Int)

    @Query("DELETE FROM messages WHERE conversationId = :senderId AND selfId = :selfId AND receiveTime <= :timestamp")
    fun deleteMessage(senderId: Int, selfId: Int, timestamp: Long)

    @Query("SELECT * FROM messages WHERE messageId = :msgId")
    fun getMessageByMessageId(msgId: String): MessageEntity

}