package com.ct.oemec.test

data class OEMECBaseData<T>(
    var module: String,
    var func: String,
    var data: T
)