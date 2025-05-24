package com.example.kotlinuberriderremake.Callback

import com.example.kotlinuberriderremake.Model.DriverGeoModel
interface FirebaseDriverInfoListener {
    fun onDriverInfoLoadSuccess(driverGeoModel: DriverGeoModel?)
}