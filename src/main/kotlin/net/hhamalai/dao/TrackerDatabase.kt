package net.hhamalai.dao

import java.io.*

import com.datastax.driver.core.Cluster
import com.datastax.driver.mapping.MappingManager
import com.datastax.driver.mapping.annotations.Table
import com.datastax.driver.mapping.annotations.PartitionKey
import com.datastax.driver.mapping.annotations.Column
import net.hhamalai.model.Location


import java.util.*

@Table(keyspace = "tracker",
        name = "location",
        readConsistency = "ONE",
        writeConsistency = "ANY",
        caseSensitiveKeyspace = false,
        caseSensitiveTable = false)
data class LocationUpdateT(
        @PartitionKey(0) @Column(name="object_id") val objectId: Int? = null,
        val event_time: Date? = null,
        val latitude: Float? = null,
        val longitude: Float? = null
)

interface TrackerStorage : Closeable {
    fun getUpdates(objectIds: List<Int>): List<LocationUpdateT>
    fun updateLocation(user: Int, location: Location)
}


class TrackerDatabase(val dbHost: String) : TrackerStorage {
    val cluster = Cluster.builder().addContactPoint(dbHost).build()
    val session = cluster.connect()
    val manager = MappingManager(session)
    val locationUpdateMapper = manager.mapper(LocationUpdateT::class.java)

    fun getUpdate(objectId: Int): LocationUpdateT {
        return locationUpdateMapper.get(objectId)
    }

    override fun getUpdates(objectIds: List<Int>): List<LocationUpdateT> {
        val results = session.execute("SELECT * FROM tracker.location")
        return locationUpdateMapper.map(results).toList()
    }

    override fun updateLocation(objectId: Int, location: Location): Unit  {
        val locUpdate = LocationUpdateT(objectId, Date(), location.latitude, location.longitude)
        locationUpdateMapper.save(locUpdate)
    }

    override fun close() {
        cluster.close()
    }
}