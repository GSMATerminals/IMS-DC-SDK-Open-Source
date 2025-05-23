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

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.ct.ertclib.dc.core.data.model.ContactEntity

@Dao
interface ContactDao {

    @Query("SELECT * FROM contacts WHERE _id = :id")
    fun queryContactById(id: Int): ContactEntity

    @Query("SELECT * FROM contacts WHERE phoneNumber = :phoneNumber")
    fun queryContact(phoneNumber: String): ContactEntity

    @Insert
    fun insertContact(contactEntity: ContactEntity): Long

    @Update
    fun updateContact(contactEntity: ContactEntity): Int
}