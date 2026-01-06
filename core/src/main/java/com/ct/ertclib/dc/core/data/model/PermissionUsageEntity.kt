package com.ct.ertclib.dc.core.data.model

import androidx.room.Entity

@Entity(tableName = "permission_usage_table", primaryKeys = ["appId", "permissionUsageTimeStamp"])
data class PermissionUsageEntity(
    var appId: String,
    var permissionName: String,
    var permissionUsageTimeStamp: Long
)
