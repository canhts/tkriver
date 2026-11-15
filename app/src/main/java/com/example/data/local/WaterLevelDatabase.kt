package com.example.data.local

import android.content.Context
import androidx.compose.runtime.Immutable
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Immutable
@Entity(tableName = "station_metadata")
data class StationMetadataEntity(
    @PrimaryKey
    val stationId: String = "71540",
    val stationName: String = "Trà Khúc",
    val riverName: String = "Trà Khúc",
    val provinceName: String = "Quảng Ngãi",
    val source: String = "KTTV",
    val sourceSet: String = "Water",
    val historicalFloodYear: String = "2013",
    val alarmLevel1: Float = 3.5f,
    val alarmLevel2: Float = 5.0f,
    val alarmLevel3: Float = 6.5f,
    val historicalFloodLevel: Float = 8.76f,
    val lastUpdatedMillis: Long = System.currentTimeMillis(),
    val isFallbackData: Boolean = false
)

@Immutable
@Entity(
    tableName = "water_points",
    primaryKeys = ["stationId", "pointIndex"]
)
data class WaterPointEntity(
    val stationId: String = "71540",
    val pointIndex: Int,
    val timeLabel: String,
    val waterLevel: Float,
    val delta: Float,
    val alarmLevel1: Float = 3.5f,
    val alarmLevel2: Float = 5.0f,
    val alarmLevel3: Float = 6.5f,
    val historicalFloodLevel: Float = 8.76f,
    val updatedAtMillis: Long = System.currentTimeMillis()
)

@Immutable
@Entity(tableName = "news_articles")
data class NewsArticleEntity(
    @PrimaryKey
    val url: String,
    val title: String,
    val pubDateStr: String,
    val timestampMillis: Long,
    val isHighlight: Boolean,
    val cachedContent: String? = null
)

@Dao
interface WaterLevelDao {

    @Query("SELECT * FROM station_metadata WHERE stationId = :stationId LIMIT 1")
    fun observeStationMetadata(stationId: String = "71540"): Flow<StationMetadataEntity?>

    @Query("SELECT * FROM station_metadata WHERE stationId = :stationId LIMIT 1")
    suspend fun getStationMetadata(stationId: String = "71540"): StationMetadataEntity?

    @Query("SELECT * FROM water_points WHERE stationId = :stationId ORDER BY pointIndex ASC")
    fun observeWaterPoints(stationId: String = "71540"): Flow<List<WaterPointEntity>>

    @Query("SELECT * FROM water_points WHERE stationId = :stationId ORDER BY pointIndex ASC")
    suspend fun getWaterPoints(stationId: String = "71540"): List<WaterPointEntity>

    @Query("SELECT * FROM water_points WHERE stationId = :stationId ORDER BY pointIndex DESC LIMIT 1")
    suspend fun getLatestWaterPoint(stationId: String = "71540"): WaterPointEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStationMetadata(metadata: StationMetadataEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWaterPoints(points: List<WaterPointEntity>)

    @Query("DELETE FROM water_points WHERE stationId = :stationId")
    suspend fun deleteWaterPointsForStation(stationId: String)

    @Transaction
    suspend fun replaceStationData(
        stationId: String,
        metadata: StationMetadataEntity,
        points: List<WaterPointEntity>
    ) {
        insertStationMetadata(metadata)
        deleteWaterPointsForStation(stationId)
        insertWaterPoints(points)
    }

    // --- Bản tin Thủy văn nguy hiểm (NCHMF) ---

    @Query("SELECT * FROM news_articles ORDER BY timestampMillis DESC LIMIT 30")
    fun observeNewsArticles(): Flow<List<NewsArticleEntity>>

    @Query("SELECT * FROM news_articles ORDER BY timestampMillis DESC LIMIT 30")
    suspend fun getNewsArticles(): List<NewsArticleEntity>

    @Query("SELECT * FROM news_articles WHERE url = :url LIMIT 1")
    suspend fun getNewsArticleByUrl(url: String): NewsArticleEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNewsArticles(articles: List<NewsArticleEntity>)

    @Query("UPDATE news_articles SET cachedContent = :content WHERE url = :url")
    suspend fun updateArticleContent(url: String, content: String)

    @Query("DELETE FROM news_articles")
    suspend fun deleteAllNewsArticles()

    @Transaction
    suspend fun replaceNewsArticlesPreservingCache(newArticles: List<NewsArticleEntity>) {
        val existingMap = getNewsArticles().associateBy { it.url }
        val merged = newArticles.map { incoming ->
            val oldCached = existingMap[incoming.url]?.cachedContent
            if (!incoming.cachedContent.isNullOrBlank()) {
                incoming
            } else {
                incoming.copy(cachedContent = oldCached)
            }
        }
        deleteAllNewsArticles()
        insertNewsArticles(merged)
    }
}

@Database(
    entities = [
        StationMetadataEntity::class,
        WaterPointEntity::class,
        NewsArticleEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class WaterLevelDatabase : RoomDatabase() {

    abstract fun waterLevelDao(): WaterLevelDao

    companion object {
        @Volatile
        private var INSTANCE: WaterLevelDatabase? = null

        fun getInstance(context: Context): WaterLevelDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    WaterLevelDatabase::class.java,
                    "tra_khuc_water_level.db"
                )
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
