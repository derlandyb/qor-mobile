package com.qualorock.android.map

import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.qualorock.shared.map.MapBounds

/** Fixed default camera extent covering Vitória, Vila Velha, Serra, and Cariacica — never device location (MAP-005). */
object GrandeVitoriaBounds {
    private const val MIN_LAT = -20.6
    private const val MAX_LAT = -20.1
    private const val MIN_LNG = -40.5
    private const val MAX_LNG = -40.1

    val shared = MapBounds(minLat = MIN_LAT, maxLat = MAX_LAT, minLng = MIN_LNG, maxLng = MAX_LNG)

    val googleMaps: LatLngBounds =
        LatLngBounds(LatLng(MIN_LAT, MIN_LNG), LatLng(MAX_LAT, MAX_LNG))
}
