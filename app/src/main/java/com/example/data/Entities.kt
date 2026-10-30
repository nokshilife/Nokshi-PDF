package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "workspace_pdfs")
data class WorkspacePdfEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val filePath: String,
    val pageCount: Int,
    val fileSizeBytes: Long,
    val sourceCategory: String, // PDF, DOCUMENT, IMAGE, EXCEL, POWERPOINT, MERGED, SAMPLE
    val isPasswordProtected: Boolean = false,
    val originalUriString: String? = null,
    val downloadUriString: String? = null,
    val savedLocationLabel: String = "",
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "signatures")
data class SignatureEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val label: String,
    val imagePath: String,
    val strokeColorArgb: Int,
    val createdAt: Long = System.currentTimeMillis()
)
