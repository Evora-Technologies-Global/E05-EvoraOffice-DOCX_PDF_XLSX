package com.evora.technologies.office.core.data.di

import com.evora.technologies.office.core.data.monetization.NoOpAdPlacementHook
import com.evora.technologies.office.core.data.repository.DocumentRepositoryImpl
import com.evora.technologies.office.core.data.repository.DraftRecoveryRepositoryImpl
import com.evora.technologies.office.core.data.repository.LocalDocumentScanner
import com.evora.technologies.office.core.data.repository.MediaStoreDocumentScanner
import com.evora.technologies.office.core.data.repository.PreferencesRepositoryImpl
import com.evora.technologies.office.core.data.repository.SignatureRepositoryImpl
import com.evora.technologies.office.core.data.repository.StorageBrowserRepositoryImpl
import com.evora.technologies.office.core.data.repository.TemplateRepositoryImpl
import com.evora.technologies.office.core.domain.port.AdPlacementHook
import com.evora.technologies.office.core.domain.port.DocumentRepository
import com.evora.technologies.office.core.domain.port.DraftRecoveryRepository
import com.evora.technologies.office.core.domain.port.PreferencesRepository
import com.evora.technologies.office.core.domain.port.SignatureRepository
import com.evora.technologies.office.core.domain.port.StorageBrowserRepository
import com.evora.technologies.office.core.domain.port.TemplateRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class IoDispatcher

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class DefaultDispatcher

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class MainDispatcher

@Module
@InstallIn(SingletonComponent::class)
object DispatchersModule {
    @Provides
    @IoDispatcher
    fun provideIoDispatcher(): CoroutineDispatcher = Dispatchers.IO

    @Provides
    @DefaultDispatcher
    fun provideDefaultDispatcher(): CoroutineDispatcher = Dispatchers.Default

    @Provides
    @MainDispatcher
    fun provideMainDispatcher(): CoroutineDispatcher = Dispatchers.Main
}

@Module
@InstallIn(SingletonComponent::class)
abstract class DataModule {
    @Binds
    @Singleton
    abstract fun bindAdPlacementHook(impl: NoOpAdPlacementHook): AdPlacementHook

    @Binds
    @Singleton
    abstract fun bindPreferencesRepository(impl: PreferencesRepositoryImpl): PreferencesRepository

    @Binds
    @Singleton
    abstract fun bindLocalDocumentScanner(impl: MediaStoreDocumentScanner): LocalDocumentScanner

    @Binds
    @Singleton
    abstract fun bindDocumentRepository(impl: DocumentRepositoryImpl): DocumentRepository

    @Binds
    @Singleton
    abstract fun bindSignatureRepository(impl: SignatureRepositoryImpl): SignatureRepository

    @Binds
    @Singleton
    abstract fun bindDraftRecoveryRepository(impl: DraftRecoveryRepositoryImpl): DraftRecoveryRepository

    @Binds
    @Singleton
    abstract fun bindTemplateRepository(impl: TemplateRepositoryImpl): TemplateRepository

    @Binds
    @Singleton
    abstract fun bindStorageBrowserRepository(impl: StorageBrowserRepositoryImpl): StorageBrowserRepository
}
