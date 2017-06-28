package net.hhamalai.dao

import kotlinx.coroutines.experimental.runBlocking
import org.junit.Test
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TestTrackerApplication {


    @Test
    fun testMessageReceivedDispatch()  = runBlocking<Unit> {
        val trackerDatabase = TrackerDatabase("localhost")
        val user1 = "user1"
        val user2 = "user2"
        val dateNow = Date()
        val lUpdate = LocationUpdateT(user1, dateNow, 10.0f, 10.5f, 5f);
        val lUpdate2 = LocationUpdateT(user2, dateNow, 20.0f, 10.5f, 6f);

        trackerDatabase.locationUpdateMapper.save(lUpdate);
        trackerDatabase.locationUpdateMapper.save(lUpdate2)

        val resp = trackerDatabase.getUpdatesForObjectId(user1).filter {
            it.latitude == 10.0f && it.longitude == 10.5f
        }
        assertEquals(resp.first().objectId, "user1")
        val resp2: List<LocationUpdateT> = trackerDatabase.getUpdates(listOf(user1, user2))
        assertTrue(resp2.contains(lUpdate))
        assertTrue(resp2.contains(lUpdate2))
    }

    @Test
    fun testMostRecentLocations() = runBlocking<Unit> {
        val trackerDatabase = TrackerDatabase("localhost")
        val user1 = "user1"
        val user2 = "user2"

        trackerDatabase.locationUpdateMapper.save(LocationUpdateT(user1, Date(), 10.0f, 10.5f, 5f))
        trackerDatabase.locationUpdateMapper.save(LocationUpdateT(user2, Date(), 20.0f, 10.5f, 6f))

        trackerDatabase.locationUpdateMapper.save(LocationUpdateT(user1, Date(), 20.0f, 20.5f, 7f))
        trackerDatabase.locationUpdateMapper.save(LocationUpdateT(user2, Date(), 40.0f, 20.5f, 8f))

        trackerDatabase.locationUpdateMapper.save(LocationUpdateT(user1, Date(), 30.0f, 30.5f, 9f))
        trackerDatabase.locationUpdateMapper.save(LocationUpdateT(user2, Date(), 60.0f, 30.5f, 10f))

        trackerDatabase.locationUpdateMapper.save(LocationUpdateT(user1, Date(0), 100.0f, 0.5f, 0f))
        trackerDatabase.locationUpdateMapper.save(LocationUpdateT(user2, Date(0), 100.0f, 0.5f, 0f))

        val mostRecents = trackerDatabase.getMostRecents().map{
            listOf(it.latitude, it.longitude, it.velocity)
        }
        assertTrue { mostRecents.contains(listOf(30.0f, 30.5f, 9f)) }
        assertTrue { mostRecents.contains(listOf(60.0f, 30.5f, 10f))}

    }
}