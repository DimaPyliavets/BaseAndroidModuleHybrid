package com.example.baseandroidmodulehybrid.core.model

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

/**
 * WidgetDataEntity — таблиця для збереження даних що відображає Glance Widget.
 *
 * ⚠️ ЗМІНИТИ:
 *  - Додай потрібні поля (наприклад: title, subtitle, imageUrl, badgeCount тощо)
 *  - При зміні схеми — збільш версію БД та додай Migration в AppDatabase
 */
@Entity(tableName = "widget_data")
data class WidgetDataEntity(
    @PrimaryKey val id: Int = 1, // Singleton-рядок (тільки один запис)

    // ⚠️ ЗМІНИТИ: замінити на реальні поля для твого віджета
    val title: String = "",
    val subtitle: String = "",
    val lastUpdated: Long = System.currentTimeMillis()
)

/**
 * WidgetDataDao — Data Access Object для widget_data таблиці.
 */
@Dao
interface WidgetDataDao {

    /** Спостерігати за змінами даних (для Glance Widget). */
    @Query("SELECT * FROM widget_data WHERE id = 1")
    fun observe(): Flow<WidgetDataEntity?>

    /** Отримати поточні дані синхронно. */
    @Query("SELECT * FROM widget_data WHERE id = 1")
    suspend fun get(): WidgetDataEntity?

    /** Зберегти або оновити дані. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(data: WidgetDataEntity)
}

/**
 * AppDatabase — головна Room база даних.
 *
 * ⚠️ ЗМІНИТИ:
 *  - version: збільшуй при зміні схеми таблиць
 *  - Додай нові entities та DAO якщо потрібні нові таблиці
 *  - Додай Migration об'єкти для оновлення схеми без втрати даних
 */
@Database(
    entities = [WidgetDataEntity::class],
    version = 1,          // ⚠️ ЗМІНИТИ: збільшуй при зміні схеми
    exportSchema = false   // ⚠️ ЗМІНИТИ на true в production та зберігай schemas/
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun widgetDataDao(): WidgetDataDao
}
