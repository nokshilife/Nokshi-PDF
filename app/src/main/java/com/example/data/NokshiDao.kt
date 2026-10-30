package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkspacePdfDao {
    @Query("SELECT * FROM workspace_pdfs ORDER BY updatedAt DESC")
    fun observeAllPdfs(): Flow<List<WorkspacePdfEntity>>

    @Query("SELECT * FROM workspace_pdfs WHERE filePath = :path LIMIT 1")
    suspend fun findByPath(path: String): WorkspacePdfEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPdf(entity: WorkspacePdfEntity): Long

    @Update
    suspend fun updatePdf(entity: WorkspacePdfEntity)

    @Query("DELETE FROM workspace_pdfs WHERE id = :id")
    suspend fun deleteById(id: Long)
}

@Dao
interface SignatureDao {
    @Query("SELECT * FROM signatures ORDER BY createdAt DESC")
    fun observeAllSignatures(): Flow<List<SignatureEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSignature(signature: SignatureEntity): Long

    @Query("DELETE FROM signatures WHERE id = :id")
    suspend fun deleteSignatureById(id: Long)
}
