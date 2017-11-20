package com.example.guiherme.customtileslab

import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.SphericalUtil

fun LatLng.toBounds(radius: Int): LatLngBounds {
    val distanceFromCenterToCorner = radius * Math.sqrt(2.0)
    val southwestCorner = SphericalUtil.computeOffset(this, distanceFromCenterToCorner, 225.0)
    val northeastCorner = SphericalUtil.computeOffset(this, distanceFromCenterToCorner, 45.0)
    return LatLngBounds(southwestCorner, northeastCorner)
}

fun LatLng.toTileCoordinates(zoom: Int): Pair<Int, Int> {
    val radiansLatitude = Math.toRadians(this.latitude)
    var x = Math.floor((this.longitude + 180) / 360 * (1 shl zoom)).toInt()
    var y = Math.floor((1 - Math.log(Math.tan(radiansLatitude) + 1 / Math.cos(radiansLatitude)) / Math.PI) / 2 * (1 shl zoom)).toInt()

    if (x < 0) {
        x = 0
    } else if (x >= 1 shl zoom) {
        x = (1 shl zoom) - 1
    }

    if (y < 0) {
        y = 0
    } else if (y >= 1 shl zoom) {
        y = (1 shl zoom) - 1
    }

    return Pair(x, y)
}