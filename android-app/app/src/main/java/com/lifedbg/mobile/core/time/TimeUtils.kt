package com.lifedbg.mobile.core.time

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

object TimeUtils {
    fun startOfTodayMillis(zoneId: ZoneId = ZoneId.systemDefault()): Long {
        return LocalDate.now(zoneId).atStartOfDay(zoneId).toInstant().toEpochMilli()
    }

    fun nowMillis(): Long = Instant.now().toEpochMilli()
}
