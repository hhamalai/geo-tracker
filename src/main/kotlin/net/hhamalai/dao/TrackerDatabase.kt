package net.hhamalai.dao

import java.io.*

import com.datastax.driver.core.Cluster
import com.datastax.driver.mapping.MappingManager
import com.datastax.driver.mapping.annotations.ClusteringColumn
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

// Null default values required for constructor
data class LocationUpdateT(
        @PartitionKey(0) @Column(name="object_id") val objectId: String? = null,
        @ClusteringColumn(0) @Column(name="event_time") val eventTime: Date? = null,
        val latitude: Float? = null,
        val longitude: Float? = null,
        val velocity: Float? = null
)

@Table(keyspace = "tracker",
        name = "chat",
        readConsistency = "ONE",
        writeConsistency = "ANY",
        caseSensitiveKeyspace = false,
        caseSensitiveTable = false)
data class ChatMessageT(
        @PartitionKey(0) @Column(name="message_id") val messageId: String? = null,
        @ClusteringColumn(0) @Column(name="event_time") val eventTime: Date? = null,
        val message: String? = null
)


interface TrackerStorage : Closeable {
    fun getMostRecents(): List<LocationUpdateT>
    fun getUpdates(objectIds: List<String>): List<LocationUpdateT>
    fun updateLocation(user: String, location: Location, velocity: Float)
    fun getMessages(): List<ChatMessageT>
    fun addMessage(user: String, message: String)
}


class TrackerDatabase(val dbHost: String) : TrackerStorage {
    val cluster = Cluster.builder().addContactPoint(dbHost).build()
    val session = cluster.connect()
    val manager = MappingManager(session)
    val locationUpdateMapper = manager.mapper(LocationUpdateT::class.java)
    val chatMessageMapper = manager.mapper(ChatMessageT::class.java)

    fun getUpdatesForObjectId(objectId: String): List<LocationUpdateT> {
        val results = session.execute("SELECT * FROM tracker.location WHERE object_id = ?", objectId)
        return locationUpdateMapper.map(results).toList()
    }

    override fun getMostRecents(): List<LocationUpdateT> {
        val results = session.execute("select * from tracker.location group by object_id")
        return locationUpdateMapper.map(results).toList()
    }

    override fun addMessage(user: String, message: String) {
        val message = ChatMessageT(user, Date(), message)
        chatMessageMapper.save(message)
    }

    override fun getMessages(): List<ChatMessageT> {
        val results = session.execute("SELECT * FROM tracker.chat")
        return chatMessageMapper.map(results).toList()
    }


    override fun getUpdates(objectIds: List<String>): List<LocationUpdateT> {
        val results = session.execute("SELECT * FROM tracker.location where object_id in ?", objectIds)
        return locationUpdateMapper.map(results).toList()
    }

    override fun updateLocation(user: String, location: Location, velocity: Float): Unit  {
        val locUpdate = LocationUpdateT(user, Date(), location.latitude, location.longitude, velocity)
        locationUpdateMapper.save(locUpdate)
    }



    override fun close() {
        cluster.close()
    }
}