package com.ct.ertclib.dc.feature.testing.socket

class SocketData{
    var type: Int = 0 // 1:创建ADC,2:普通数据,3:呼叫,4:挂断,5:接听
    var createLabels: ArrayList<String> ?= null // 要创建的ADC的labels
    var dataLabel: String ?= null // 收发数据ADC的label
    var data: String ?= null // 数据,byteArray的base64编码
}
