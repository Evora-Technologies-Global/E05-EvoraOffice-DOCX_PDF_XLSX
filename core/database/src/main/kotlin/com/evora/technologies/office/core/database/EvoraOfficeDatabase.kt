package com.evora.technologies.office.core.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.evora.technologies.office.core.database.dao.CloudAccountDao
import com.evora.technologies.office.core.database.dao.DocumentMetaDao
import com.evora.technologies.office.core.database.dao.DraftRecoveryDao
import com.evora.technologies.office.core.database.dao.SignatureDao
import com.evora.technologies.office.core.database.dao.TrashDao
import com.evora.technologies.office.core.database.entity.CloudAccountEntity
import com.evora.technologies.office.core.database.entity.DocumentMetaEntity
import com.evora.technologies.office.core.database.entity.DraftRecoveryEntity
import com.evora.technologies.office.core.database.entity.SignatureEntity
import com.evora.technologies.office.core.database.entity.TrashEntity
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Database(
    entities = [
        DocumentMetaEntity::class,
        TrashEntity::class,
        DraftRecoveryEntity::class,
        SignatureEntity::class,
        CloudAccountEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class EvoraOfficeDatabase : RoomDatabase() {
    abstract fun documentMetaDao(): DocumentMetaDao
    abstract fun trashDao(): TrashDao
    abstract fun draftRecoveryDao(): DraftRecoveryDao
    abstract fun signatureDao(): SignatureDao
    abstract fun cloudAccountDao(): CloudAccountDao
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

    @Provides
    fun provideTrashDao(database: EvoraOfficeDatabase): TrashDao =
        database.trashDao()

    @Provides
    fun provideDraftRecoveryDao(database: EvoraOfficeDatabase): DraftRecoveryDao =
        database.draftRecoveryDao()

    @Provides
    fun provideSignatureDao(database: EvoraOfficeDatabase): SignatureDao =
        database.signatureDao()

    @Provides
    fun provideCloudAccountDao(database: EvoraOfficeDatabase): CloudAccountDao =
        database.cloudAccountDao()
}
