package xyz.lonelyleaf.docker.registry.gc

import org.junit.Assert
import org.junit.Test
import java.time.Duration

class TimeTest {

    @Test
    fun testDurationParse() {
        val day = Duration.parse("P5D")
        Assert.assertEquals(5, day.toDays())

        val hour = Duration.parse("PT36H")
        Assert.assertEquals(36,hour.toHours())

        val dayWithHour = Duration.parse("P2DT12H")
        Assert.assertEquals(60,dayWithHour.toHours())
    }


}
