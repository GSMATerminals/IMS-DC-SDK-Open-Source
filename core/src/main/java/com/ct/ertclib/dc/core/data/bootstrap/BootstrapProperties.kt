package com.ct.ertclib.dc.core.data.bootstrap

data class BootstrapProperties(
    val appId: List<String>?,
    val supportInnerADCDevices: List<String>?,
    val version: String?,
    val windowStyle: WindowStyle?
)
