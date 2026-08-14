package org.dhamma.dipi.staff.database

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "applicants")
data class ApplicantEntity(
    @PrimaryKey val id: Int,
    val courseId: Int,
    val payload: String,
)

@Entity(tableName = "outbox")
data class OutboxEntity(
    @PrimaryKey(autoGenerate = true) val rowId: Long = 0,
    val applicantId: Int,
    val status: String,
    val letterId: Int,
    val comment: String,
    val state: String,
    val message: String?,
)

@Dao
interface ApplicantDao {
    @Query("SELECT * FROM applicants WHERE courseId = :courseId")
    fun observe(courseId: Int): Flow<List<ApplicantEntity>>

    @Query("SELECT * FROM applicants WHERE courseId = :courseId")
    suspend fun list(courseId: Int): List<ApplicantEntity>

    @Query("SELECT * FROM applicants WHERE id = :id")
    suspend fun get(id: Int): ApplicantEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(rows: List<ApplicantEntity>)

    @Query("DELETE FROM applicants")
    suspend fun clear()
}

@Dao
interface OutboxDao {
    @Query("SELECT * FROM outbox WHERE state != 'Synced' ORDER BY rowId")
    fun observePending(): Flow<List<OutboxEntity>>

    @Query("SELECT * FROM outbox WHERE state != 'Synced' ORDER BY rowId")
    suspend fun pending(): List<OutboxEntity>

    @Insert
    suspend fun insert(row: OutboxEntity): Long

    @Query("UPDATE outbox SET state = :state, message = :message WHERE rowId = :id")
    suspend fun updateState(id: Long, state: String, message: String?)

    @Query("DELETE FROM outbox")
    suspend fun clear()
}

@Database(entities = [ApplicantEntity::class, OutboxEntity::class], version = 1, exportSchema = false)
abstract class DipiDb : RoomDatabase() {
    abstract fun applicants(): ApplicantDao
    abstract fun outbox(): OutboxDao
}
