package com.example.tripchecker.data.file

import java.io.File

/** Removes orphan import staging files after process death or failed imports. */
object StagingCleanup {
    const val STAGING_DIR_NAME = "staging"
    private const val LEGACY_STAGING_PREFIX = "staging-"

    /** Deletes every file under [cacheStagingDir] and legacy `staging-*` files in [ticketsDir]. */
    fun clean(cacheStagingDir: File, ticketsDir: File?) {
        cacheStagingDir.mkdirs()
        cacheStagingDir.listFiles()?.forEach { file ->
            if (file.isFile) file.delete()
        }
        ticketsDir?.listFiles()?.forEach { file ->
            if (file.isFile && file.name.startsWith(LEGACY_STAGING_PREFIX)) {
                file.delete()
            }
        }
    }
}
