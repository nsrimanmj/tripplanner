package com.example.tripchecker.domain

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.time.Clock
import java.time.Instant

/** Ticks [Instant] for recomputing journey status while a screen is open. */
fun journeyNowTicker(clock: Clock, intervalMs: Long = 60_000L): Flow<Instant> =
    flow {
        emit(clock.instant())
        while (true) {
            delay(intervalMs)
            emit(clock.instant())
        }
    }
