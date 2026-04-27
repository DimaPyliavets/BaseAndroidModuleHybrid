package com.example.baseandroidmodulehybrid

import android.content.Context
import androidx.room.Room
import com.example.baseandroidmodulehybrid.core.model.AppDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

/**
 * AppModule — головний DI модуль Hilt.
 *
 * ⚠️ ЗМІНИТИ:
 *  - Таймаути OkHttpClient відповідно до швидкості твого сервера
 *  - Назву Room БД ("hybrid_db") якщо хочеш іншу
 *  - Додай Interceptors до OkHttpClient (логування, auth headers тощо)
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    /**
     * OkHttpClient для мережевих запитів (version.json + bundle download).
     *
     * ⚠️ ЗМІНИТИ:
     *  - connectTimeout / readTimeout: збільш для повільних з'єднань
     *  - Додай HttpLoggingInterceptor для debug builds:
     *    .addInterceptor(HttpLoggingInterceptor().apply { level = Level.BASIC })
     */
    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS) // ⚠️ ЗМІНИТИ за потреби
            .readTimeout(120, TimeUnit.SECONDS)   // ⚠️ Довгий таймаут для великих бандлів
            .writeTimeout(30, TimeUnit.SECONDS)
            // ⚠️ ДОДАТИ у debug builds:
            // .addInterceptor(HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BASIC))
            .build()
    }

    /**
     * Room Database — єдиний екземпляр для всього додатка.
     *
     * ⚠️ ЗМІНИТИ:
     *  - "hybrid_db": ім'я файлу бази даних на пристрої
     *  - При зміні схеми (AppDatabase.version) — додай Migration замість fallbackToDestructiveMigration
     */
    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "hybrid_db" // ⚠️ ЗМІНИТИ: назву файлу БД
        )
            .fallbackToDestructiveMigration() // ⚠️ ЗМІНИТИ: на явні Migration у production!
            .build()
    }

    /**
     * WidgetDataDao — надається через DB singleton.
     */
    @Provides
    @Singleton
    fun provideWidgetDataDao(db: AppDatabase) = db.widgetDataDao()
}
