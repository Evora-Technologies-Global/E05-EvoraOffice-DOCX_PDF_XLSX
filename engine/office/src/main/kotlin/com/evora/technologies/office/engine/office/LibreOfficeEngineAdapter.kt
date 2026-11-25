package com.evora.technologies.office.engine.office

import com.evora.technologies.office.core.domain.port.OfficeEnginePort
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Office engine boundary awaiting LibreOfficeKit 16 KB native artifact from R-01 CI (T-002 / T-015).
 */
@Singleton
class LibreOfficeEngineAdapter @Inject constructor() : OfficeEnginePort {
    override fun isNativeEngineAvailable(): Boolean = runCatching {
        System.loadLibrary("lo-native-code")
        true
    }.getOrDefault(false)
}

@Module
@InstallIn(SingletonComponent::class)
abstract class OfficeEngineModule {
    @Binds
    @Singleton
    abstract fun bindOfficeEnginePort(impl: LibreOfficeEngineAdapter): OfficeEnginePort
}
