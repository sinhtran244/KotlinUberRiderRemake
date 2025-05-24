package com.example.kotlinuberriderremake.Model

import com.firebase.geofire.GeoLocation

class DriverGeoModel {
    var key: String? = null
    var geolocation: GeoLocation? = null
    var driverInfoModel: DriverInfoModel? = null

    constructor(key: String?, geolocation: GeoLocation?) {
        this.key = key
        this.geolocation = geolocation!!
    }
}