package com.example.tripchecker.data.file

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class StagingCleanupTest {
    @Test
    fun `clears cache staging and legacy ticket staging files`() {
        val root = createTempDir()
        val cacheStaging = File(root, "cache/staging").also { it.mkdirs() }
        val tickets = File(root, "tickets").also { it.mkdirs() }
        File(cacheStaging, "orphan.pdf").writeText("x")
        File(tickets, "staging-123.pdf").writeText("y")
        File(tickets, "real-id.pdf").writeText("keep")

        StagingCleanup.clean(cacheStaging, tickets)

        assertFalse(File(cacheStaging, "orphan.pdf").exists())
        assertFalse(File(tickets, "staging-123.pdf").exists())
        assertTrue(File(tickets, "real-id.pdf").exists())
    }
}
