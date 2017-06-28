@file:Suppress("EXPERIMENTAL_FEATURE_WARNING")

package net.hhamalai

import com.beust.klaxon.*
import com.google.gson.GsonBuilder
import kotlinx.coroutines.experimental.channels.ClosedSendChannelException
import net.hhamalai.Messages.primaryKey
import net.hhamalai.dao.TrackerDatabase
import net.hhamalai.model.Location
import org.jetbrains.ktor.util.buildByteBuffer
import org.jetbrains.ktor.websocket.Frame
import org.jetbrains.ktor.websocket.WebSocket
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SchemaUtils.create

import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.ktor.websocket.CloseReason
import org.jetbrains.ktor.websocket.close


import java.nio.ByteBuffer
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicInteger


object Messages : Table() {
    val id = uuid("id").primaryKey()
    val name = varchar("name", length = 50)
    val time = datetime("datetime")
    val message = text("message")
    val tripId = (integer("city_id") references Trips.id).nullable()
}

object Trips : Table() {
    val id = integer("id").autoIncrement().primaryKey()
}


class TrackerServer {
    val usersCounter = AtomicInteger()
    val memberNames = ConcurrentHashMap<String, String>()
    val trackerDatabase = TrackerDatabase("localhost")
    val members = ConcurrentHashMap<String, MutableList<WebSocket>>()
    val lastMessages = LinkedList<String>()
    val parser: Parser = Parser()
    val gson = GsonBuilder().create()

    suspend fun memberJoin(member: String, socket: WebSocket) {
        val name = memberNames.computeIfAbsent(member) { "user${usersCounter.incrementAndGet()}" }
        val list = members.computeIfAbsent(member) { CopyOnWriteArrayList<WebSocket>() }
        list.add(socket)

        if (list.size == 1) {
            broadcast("{\"type\": \"join\", \"name\": \"$name\"}")
        }

        val currentLocations = trackerDatabase.getMostRecents()
        for (currentLocation in currentLocations) {
            val jsonized = gson.toJson(currentLocation)
            println(jsonized)
            socket.send(Frame.Text(jsonized))
        }
        val messages = synchronized(lastMessages) { lastMessages.toList() }
        for (message in messages) {
            socket.send(Frame.Text(message))
        }
    }

    suspend fun memberRenamed(member: String, to: String) {
        val oldName = memberNames.put(member, to) ?: member
        broadcast("{\"type\": \"rename\", \"oldname\": \"$oldName\"  \"newname\": \"$to\"}")
    }

    suspend fun memberLeft(member: String, socket: WebSocket) {
        val connections = members[member]
        connections?.remove(socket)

        if (connections != null && connections.isEmpty()) {
            val name = memberNames[member] ?: member
            broadcast("{\"type\": \"left\", \"name\": \"$name\"}")
        }
    }

    suspend fun sendTo(receipient: String, sender: String, message: String) {
        members[receipient]?.send(Frame.Text("[$sender] $message"))
    }

    suspend fun updateLocation(sender: String, location: String) {
        val name = memberNames[sender] ?: sender

        val stringBuilder: StringBuilder = StringBuilder(location)
        val json: JsonObject = parser.parse(stringBuilder) as JsonObject
        val latitude = json.double("lat")
        val longitude = json.double("lon")
        val velocity = json.double("vel")
        val tripId = json.int("tripId")
        json.put("name", name)
        trackerDatabase.updateLocation(
                name,
                Location(latitude?.toFloat() ?: 0f, longitude?.toFloat() ?: 0f),
                velocity?.toFloat() ?: 0f)

        val serialized = json.toJsonString()

        broadcast(serialized)

        synchronized(lastMessages) {
            lastMessages.add(serialized)
            if (lastMessages.size > 100) {
                lastMessages.removeFirst()
            }
        }
    }

    suspend fun message(sender: String, msg: String) {
        val senderName = memberNames[sender] ?: sender

        val stringBuilder: StringBuilder = StringBuilder(msg)
        val json: JsonObject = parser.parse(stringBuilder) as JsonObject
        val received_message = json.string("message")
        val received_tripId = json.int("tripId")
        json.put("name", senderName)

        val formatted = "${senderName}: ${received_message}"

        transaction {
            create(Messages, Trips)

            Messages.insert {
                it[tripId] = received_tripId
                it[message] = received_message
                it[name] = senderName
                it[time] = org.joda.time.DateTime()
            }

        }

        broadcast(formatted)

        synchronized(lastMessages) {
            lastMessages.add(formatted)
            if (lastMessages.size > 100) {
                lastMessages.removeFirst()
            }
        }
    }

    suspend fun broadcast(message: String) {
        broadcast(buildByteBuffer {
            putString(message, Charsets.UTF_8)
        })
    }

    suspend fun broadcast(sender: String, message: String) {
        val name = memberNames[sender] ?: sender
        broadcast("[$name] $message")
    }

    suspend fun broadcast(serialized: ByteBuffer) {
        members.values.forEach { socket ->
            socket.send(Frame.Text(true, serialized.duplicate()))
        }
    }

    suspend fun List<WebSocket>.send(frame: Frame) {
        forEach {
            try {
                it.send(frame.copy())
            } catch (t: Throwable) {
                try {
                    it.close(CloseReason(CloseReason.Codes.PROTOCOL_ERROR, ""))
                } catch (ignore: ClosedSendChannelException) {

                }
            }
        }
    }
}