/*
 * Copyright (c) [2019] [lonelyleaf]
 * [docker-registry-gc] is licensed under the Mulan PSL v1.
 * You can use this software according to the terms and conditions of the Mulan PSL v1.
 * You may obtain a copy of Mulan PSL v1 at:
 *    http://license.coscl.org.cn/MulanPSL
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR
 * PURPOSE.
 *
 * See the Mulan PSL v1 for more details.
 */

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
