package net.hhamalai.model

data class Location(val latitude: Float, val longitude: Float)

data class MapConfiguration(val defaultMapPosition: Location, val defaultZoomLevel: Int)
data class AppConfiguration(val mapConfiguration: MapConfiguration)
