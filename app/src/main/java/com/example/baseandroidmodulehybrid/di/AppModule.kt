package com.example.baseandroidmodulehybrid.di

import android.content.Context
import android.util.Log
import androidx.room.Room
import com.example.baseandroidmodulehybrid.core.model.AppDatabase
import com.example.baseandroidmodulehybrid.core.model.WidgetDataDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val loggingInterceptor = Interceptor { chain ->
            val request = chain.request()
            Log.d("OkHttp", "--> SENDING REQUEST: ${request.url}")
            
            try {
                val response = chain.proceed(request)
                Log.d("OkHttp", "<-- RECEIVED RESPONSE: ${response.code} for ${request.url}")
                response
            } catch (e: Exception) {
                Log.e("OkHttp", "xxx REQUEST FAILED: ${request.url}", e)
                throw e
            }
        }

        val userAgentInterceptor = Interceptor { chain ->
            val request = chain.request().newBuilder()
                .header("User-Agent", "BaseAndroidModuleHybrid-App")
                .build()
            chain.proceed(request)
        }

        return OkHttpClient.Builder()
            .addInterceptor(userAgentInterceptor)
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "hybrid_db"
        )
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    @Singleton
    fun provideWidgetDataDao(db: AppDatabase): WidgetDataDao = db.widgetDataDao()
}
