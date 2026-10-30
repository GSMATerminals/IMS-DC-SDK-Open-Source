package com.ct.ertclib.dc.net.data

data class RecentCall(
    val name: String,
    val phoneNumber: String,
    val time: String,
    val isOutgoing: Boolean,
    val isVideoCall: Boolean = false
)

data class ADCState(
    val label: String,
    val state: String
)

enum class PageState(val value: String) {
    INIT("init"),
    OUT_CALLING("outCalling"),
    ACTIVE("active"),
    RINGING("ringing")
}