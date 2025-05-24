package com.example.kotlinuberriderremake.Common

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.widget.TextView
import androidx.core.app.NotificationCompat
import com.example.kotlinuberriderremake.Model.AnimationMode
import com.example.kotlinuberriderremake.Model.DriverGeoModel
import com.example.kotlinuberriderremake.Model.RiderModel
import com.example.kotlinuberriderremake.R
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import kotlin.math.abs
import kotlin.math.atan

import java.util.Calendar


object Common {
    val driversSubscribe: MutableMap<String, AnimationMode> = HashMap<String, AnimationMode>()
    val markerList: MutableMap<String, Marker?> = HashMap<String, Marker?>()
    val DRIVER_INFO_REFERENCE: String = "DriverInfo"
    val driversFound: MutableSet<DriverGeoModel> = HashSet<DriverGeoModel>()
    val DRIVERS_LOCATION_REFERENCES: String = "DriverLocation" //Same as Server app
    val TOKEN_REFERENCE: String = "Token"
    var currentRider: RiderModel? = null
    val RIDER_INFO_REFERENCE: String = "Riders"

    fun buildName(firstName: String?, lastName: String?): String {
        return java.lang.StringBuilder(firstName!!).append(" ").append(lastName).toString()
    }
    fun buildWelcomeMessage(): String {
        return StringBuilder("Welcome, ")
            .append(currentRider!!.firstName)
            .append(" ")
            .append(currentRider!!.lastName)
            .toString()
    }
    fun showNotification(
        context: Context,
        id: Int,
        title: String?,
        body: String?,
        intent: Intent?
    ) {
        var pendingIntent: PendingIntent? = null
        if (intent != null) {
            pendingIntent = PendingIntent.getActivity(
                context,
                id,
                intent!!,
                PendingIntent.FLAG_UPDATE_CURRENT
            )
        }

        val NOTIFICATION_CHANNEL_ID = "edt_dev_uber_remake"
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationChannel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "Uber Remake",
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationChannel.description = "Uber Remake"
            notificationChannel.enableLights(true)
            notificationChannel.lightColor = Color.RED
            notificationChannel.vibrationPattern = longArrayOf(0, 1000, 500, 1000)
            notificationChannel.enableVibration(true)
            notificationManager.createNotificationChannel(notificationChannel)
        }

        val builder = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground) // Replace with your actual icon
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)

        if (pendingIntent != null) {
            builder.setContentIntent(pendingIntent!!)
        }
        val notification = builder.build()
        notificationManager.notify(id, notification)
    }
    val NOTI_TITLE: String = "title"
    val NOTI_BODY: String = "body"

    //GET BEARING
    fun getBearing(begin: LatLng, end: LatLng): Float {
        //You can copy this function by link at description
        val lat = abs(begin.latitude - end.latitude)
        val lng = abs(begin.longitude - end.longitude)

        if (begin.latitude < end.latitude && begin.longitude < end.longitude) return (Math.toDegrees(
            atan(lng / lat)
        )).toFloat()
        else if (begin.latitude >= end.latitude && begin.longitude < end.longitude) return ((90 - Math.toDegrees(
            atan(lng / lat)
        )) + 90).toFloat()
        else if (begin.latitude >= end.latitude && begin.longitude >= end.longitude) return (Math.toDegrees(
            atan(lng / lat)
        ) + 180).toFloat()
        else if (begin.latitude < end.latitude && begin.longitude >= end.longitude) return ((90 - Math.toDegrees(
            atan(lng / lat)
        )) + 270).toFloat()
        return -1f
    }

    //DECODE POLY
    fun decodePoly(encoded: String): ArrayList<LatLng> {
        val poly= ArrayList<LatLng?>()
        var index = 0
        val len = encoded.length
        var lat = 0
        var lng = 0
        while (index < len) {
            var b: Int
            var shift = 0
            var result = 0
            do {
                b = encoded.get(index++).code - 63
                result = result or ((b and 0x1f) shl shift)
                shift += 5
            } while (b >= 0x20)
            val dlat = (if ((result and 1) != 0) (result shr 1).inv() else (result shr 1))
            lat += dlat

            shift = 0
            result = 0
            do {
                b = encoded.get(index++).code - 63
                result = result or ((b and 0x1f) shl shift)
                shift += 5
            } while (b >= 0x20)
            val dlng = (if ((result and 1) != 0) (result shr 1).inv() else (result shr 1))
            lng += dlng

            val p = LatLng(
                ((lat.toDouble() / 1E5)),
                ((lng.toDouble() / 1E5))
            )
            poly.add(p)
        }
        return poly as ArrayList<LatLng>



    }


    fun setWelcomeMessage(txtWelcome: TextView) {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)

        if (hour in 1..12) {
            txtWelcome.text = "Good morning."
        } else if (hour in 13..17) {
            txtWelcome.text = "Good afternoon."
        } else {
            txtWelcome.text = "Good evening."
        }
    }
}

