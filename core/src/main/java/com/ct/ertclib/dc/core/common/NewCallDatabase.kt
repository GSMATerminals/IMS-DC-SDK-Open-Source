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

package com.ct.ertclib.dc.core.common

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.blankj.utilcode.util.LogUtils
import com.blankj.utilcode.util.Utils
import com.ct.ertclib.dc.core.port.dao.ContactDao
import com.ct.ertclib.dc.core.port.dao.ConversationsDao
import com.ct.ertclib.dc.core.port.dao.DcPropertiesDao
import com.ct.ertclib.dc.core.port.dao.MessageDao
import com.ct.ertclib.dc.core.port.dao.MiniAppInfoDao
import com.ct.ertclib.dc.core.data.model.ContactEntity
import com.ct.ertclib.dc.core.data.model.ConversationEntity
import com.ct.ertclib.dc.core.data.model.DataChannelPropertyEntity
import com.ct.ertclib.dc.core.data.model.MessageEntity
import com.ct.ertclib.dc.core.data.model.MiniAppInfo
import com.ct.ertclib.dc.core.data.model.PermissionModel
import com.ct.ertclib.dc.core.port.dao.PermissionDao

@Database(
    entities = [MiniAppInfo::class, MessageEntity::class, ContactEntity::class, ConversationEntity::class, DataChannelPropertyEntity::class, PermissionModel::class],
    version = 10,
    exportSchema = false
)
abstract class NewCallDatabase : RoomDatabase() {

    abstract fun miniAppDao(): MiniAppInfoDao
    abstract fun messageDao(): MessageDao
    abstract fun contactDao(): ContactDao

    abstract fun conversationDao(): ConversationsDao

    abstract fun dcPropertiesDao(): DcPropertiesDao

    abstract fun permissionDao(): PermissionDao

    companion object {

        private const val updateCount = " UPDATE conversations SET count = " +
                "(SELECT COUNT(messages._id) " +
                "FROM messages " +
                "LEFT JOIN " +
                "conversations ON conversations._id = conversationId " +
                "WHERE conversations._id = new.conversationId) ;"
        private const val updateRead = " UPDATE conversations SET read = CASE ( " +
                "SELECT COUNT( * ) " +
                " FROM messages " +
                "WHERE read = 0 AND conversationId = conversations._id ) " +
                "WHEN 0 THEN 1 ELSE 0 END " +
                "WHERE conversations._id = new.conversationId;"

        private const val updateConversationByMessages =
            "CREATE TRIGGER IF NOT EXISTS message_update_conversation_on_insert AFTER INSERT ON messages " +
                    "BEGIN" +
                    " UPDATE conversations SET date=(strftime('%s','now') * 1000), snippet = new.text, type = new.type" +
                    " WHERE  conversations._id = new.conversationId ;" +
                    updateCount +
                    updateRead +
                    " END; "

        private fun initialize(context: Context): NewCallDatabase {
            LogUtils.i("NewCallDatabase initialize")
            val db = Room.databaseBuilder(context, NewCallDatabase::class.java, "newcall_db")
                .addMigrations(object : Migration(1, 2) {
                    override fun migrate(database: SupportSQLiteDatabase) {
                        LogUtils.i("NewCallDatabase update 1-2")
                        val addConversationDbSql =
                            "CREATE TABLE conversations ( _id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT," +
                                    " date TEXT, snippet TEXT, read INTEGER NOT NULL DEFAULT 0, type TEXT, recipientId INTEGER NOT NULL DEFAULT 0, count INTEGER NOT NULL DEFAULT 0 )"
                        database.execSQL(addConversationDbSql)

                        val defaultInt = 0
                        // 更新Message表
                        val addText = "ALTER TABLE messages ADD COLUMN text TEXT"
                        val addDirection =
                            "ALTER TABLE messages ADD COLUMN direction INTEGER NOT NULL DEFAULT $defaultInt"
                        database.execSQL(addText)
                        database.execSQL(addDirection)

                        //创建更新的触发器, 更新message消息时，更新Conversaiton
                        database.execSQL(updateConversationByMessages)
                    }
                })
                .addMigrations(object : Migration(2, 3) {
                    override fun migrate(database: SupportSQLiteDatabase) {
                        LogUtils.i("NewCallDatabase update 2-3")
                        val defaultString = "0"
                        val addMessageId =
                            "ALTER TABLE messages ADD COLUMN messageId TEXT NOT NULL DEFAULT $defaultString"
                        database.execSQL(addMessageId)

                        database.execSQL(
                            "CREATE TABLE IF NOT EXISTS contacts (" +
                                    "_id INTEGER PRIMARY KEY," +
                                    "phoneNumber TEXT," +
                                    "avatarUri TEXT" +
                                    ")"
                        )
                    }
                })
                .addMigrations(object : Migration(3, 4) {
                    override fun migrate(database: SupportSQLiteDatabase) {
                        LogUtils.i("NewCallDatabase update 3-4")
                        database.execSQL("ALTER TABLE messages ADD COLUMN thumbnailUri TEXT")
                    }
                })
                .addMigrations(object : Migration(4, 5) {
                    override fun migrate(database: SupportSQLiteDatabase) {
                        database.execSQL(
                            "CREATE TABLE IF NOT EXISTS dc_properties (" +
                                    "_id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL," +
                                    "appId TEXT NOT NULL," +
                                    "dcId TEXT," +
                                    "streamId TEXT," +
                                    "dcLabel TEXT," +
                                    "useCase TEXT," +
                                    "subprotocol TEXT," +
                                    "ordered TEXT," +
                                    "maxRetr TEXT," +
                                    "maxTime TEXT," +
                                    "priority TEXT," +
                                    "autoAcceptDcSetup TEXT," +
                                    "bandwidth TEXT," +
                                    "qosHint TEXT)"
                        )
                    }
                })
                .addMigrations(object : Migration(5, 6) {
                    override fun migrate(database: SupportSQLiteDatabase) {
                        database.execSQL(
                            "CREATE TABLE IF NOT EXISTS mini_app_info (" +
                                    "appId TEXT PRIMARY KEY NOT NULL," +
                                    "appName TEXT NOT NULL," +
                                    "appIcon TEXT," +
                                    "autoLaunch BOOLEAN NOT NULL," +
                                    "autoLoad BOOLEAN NOT NULL," +
                                    "callId TEXT NOT NULL," +
                                    "eTag TEXT NOT NULL," +
                                    "ifWorkWithoutPeerDc BOOLEAN NOT NULL," +
                                    "isOutgoingCall BOOLEAN NOT NULL," +
                                    "myNumber TEXT," +
                                    "path TEXT," +
                                    "phase TEXT NOT NULL," +
                                    "qosHint TEXT NOT NULL," +
                                    "remoteNumber TEXT," +
                                    "slotId INTEGER NOT NULL," +
                                    "supportScene INTEGER NOT NULL," +
                                    "isStartAfterInstalled BOOLEAN NOT NULL," +
                                    "lastUseTime INTEGER NOT NULL)"
                        )
                    }
                })
                .addMigrations(object : Migration(6, 7) {
                    override fun migrate(database: SupportSQLiteDatabase) {
                        LogUtils.i("NewCallDatabase update 6-7")
                        val addIsFromRemote =
                            "ALTER TABLE mini_app_info ADD COLUMN `isFromRemote` INTEGER NOT NULL DEFAULT 0"
                        database.execSQL(addIsFromRemote)
                    }
                })
                .addMigrations(object : Migration(7, 8) {
                    override fun migrate(database: SupportSQLiteDatabase) {
                        LogUtils.i("NewCallDatabase update 7-8")
                        val addIsFromRemote =
                            "ALTER TABLE mini_app_info RENAME COLUMN isFromRemote TO isFromBDC100"
                        database.execSQL(addIsFromRemote)
                    }
                })
                .addMigrations(object : Migration(8, 9) {
                    override fun migrate(database: SupportSQLiteDatabase) {
                        LogUtils.i("NewCallDatabase update 8-9")
                        database.execSQL(
                            "CREATE TABLE IF NOT EXISTS permission_table (" +
                                    "appId TEXT PRIMARY KEY NOT NULL," +
                                    "permissionMapString TEXT NOT NULL)"
                        )
                    }
                })
                .addMigrations(object : Migration(9, 10) {
                    override fun migrate(database: SupportSQLiteDatabase) {
                        LogUtils.i("NewCallDatabase update 9-10")
                        val addIsActiveStart =
                            "ALTER TABLE mini_app_info ADD COLUMN `isActiveStart` INTEGER NOT NULL DEFAULT 1"
                        database.execSQL(addIsActiveStart)
                    }
                })
                .addCallback(object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        LogUtils.i("NewCallDatabase onCreate create trigger")
                        db.execSQL(updateConversationByMessages)
                    }
                })
                .build()
            return db
        }

        private lateinit var newCallDb: NewCallDatabase
        private val sLock: ByteArray = ByteArray(1)

        fun getInstance(): NewCallDatabase {
            synchronized(sLock) {
                if (!this::newCallDb.isInitialized) {
                    newCallDb = initialize(Utils.getApp())
                }
            }
            return newCallDb
        }
    }
}