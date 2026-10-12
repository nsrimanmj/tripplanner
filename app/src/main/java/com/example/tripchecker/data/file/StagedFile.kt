package com.example.tripchecker.data.file

import java.io.File

/** A PDF copied into app storage during import, before it is assigned a ticket id. */
data class StagedFile(
    val file: File,
    val sha256: String,
    val originalName: String,
    val sizeBytes: Long,
)
