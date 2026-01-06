package com.ct.ertclib.dc.core.data.miniapp

data class PermissionUsageData(
    var permissionTitle: String,
    var permissionUsageTime: String
)

fun PermissionUsageData.getCombineKey(): String {
    return "${permissionTitle}_$permissionUsageTime"
}
