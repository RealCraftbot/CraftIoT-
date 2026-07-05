package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "firmwares")
data class FirmwareRelease(
    @PrimaryKey val id: String,
    val version: String,
    val deviceType: String,
    val releaseNotes: String,
    val fileSizeMb: Float,
    val releaseDate: Long = System.currentTimeMillis(),
    val isDownloaded: Boolean = false
)
