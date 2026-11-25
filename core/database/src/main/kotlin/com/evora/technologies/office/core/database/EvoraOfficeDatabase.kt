package com.evora.technologies.office.core.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.evora.technologies.office.core.database.dao.DocumentMetaDao
import com.evora.technologies.office.core.database.entity.DocumentMetaEntity
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Database(
    entities = [DocumentMetaEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class EvoraOfficeDatabase : RoomDatabase() {
    abstract fun documentMetaDao(): DocumentMetaDao
}

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideEvoraDatabase(
        @ApplicationContext context: Context,
    ): EvoraOfficeDatabase = Room.databaseBuilder(
        context,
        EvoraOfficeDatabase::class.java,
        "evora_office.db",
    ).build()

    @Provides
    fun provideDocumentMetaDao(database: EvoraOfficeDatabase): DocumentMetaDao =
        database.documentMetaDao()
}
