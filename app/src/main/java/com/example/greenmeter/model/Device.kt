package com.example.greenmeter.model

class Device
{
    var deviceId: String = ""
    var deviceName: String = ""
    var deviceLogo: String = ""

    constructor() {}

    constructor(
        deviceId: String,
        deviceName: String,
        deviceLogo: String = ""
    ) {
        this.deviceId = deviceId
        this.deviceName = deviceName
        this.deviceLogo = deviceLogo
    }
}