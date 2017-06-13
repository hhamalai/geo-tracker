package net.hhamalai.dao

import kotlinx.coroutines.experimental.runBlocking
import org.junit.Test
import java.util.*

class TestTrackerApplication {


    @Test
    fun testMessageReceivedDispatch()  = runBlocking<Unit> {
        val trackerDatabase = TrackerDatabase("localhost")
        val lUpdate = LocationUpdateT(1, Date(), 10.0f, 10.5f);
        val lUpdate2 = LocationUpdateT(2, Date(), 20.0f, 10.5f);

        trackerDatabase.locationUpdateMapper.save(lUpdate);
        trackerDatabase.locationUpdateMapper.save(lUpdate2)

        println(trackerDatabase.getUpdate(1))
        val resp: LocationUpdateT = trackerDatabase.getUpdate(1)
        println("Single: " + resp)
        val resp2: List<LocationUpdateT> = trackerDatabase.getUpdates(listOf(1, 2))
        println("Multiple: " + resp2)
    }
}