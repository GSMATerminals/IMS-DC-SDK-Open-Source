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

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.ct.ertclib.dc.core.data.model.ConversationEntity
import com.ct.ertclib.dc.core.data.message.ConversationData

@Dao
interface ConversationsDao {

    @Query("SELECT * FROM conversations INNER JOIN contacts on contacts._id = conversations.recipientId ORDER BY date DESC")
    fun getAllConversations(): PagingSource<Int, ConversationData>

    @Query("SELECT * FROM conversations WHERE recipientId = :recipientId")
    fun queryConversation(recipientId: Int): ConversationEntity

    @Insert
    fun insertConversation(conversationEntity: ConversationEntity): Long
}