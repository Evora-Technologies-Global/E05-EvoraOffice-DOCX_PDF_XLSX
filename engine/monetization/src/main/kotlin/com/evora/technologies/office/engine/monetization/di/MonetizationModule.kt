package com.evora.technologies.office.engine.monetization.di

import com.evora.technologies.office.core.domain.port.AnalyticsPort
import com.evora.technologies.office.core.domain.port.ProEntitlementPort
import com.evora.technologies.office.core.domain.port.RemoteConfigPort
import com.evora.technologies.office.engine.monetization.AndroidKeystoreHmacSigner
import com.evora.technologies.office.engine.monetization.BillingGateway
import com.evora.technologies.office.engine.monetization.BuildConfig
import com.evora.technologies.office.engine.monetization.EntitlementSigner
import com.evora.technologies.office.engine.monetization.FirebaseAnalyticsSink
import com.evora.technologies.office.engine.monetization.FirebaseRemoteConfigManager
import com.evora.technologies.office.engine.monetization.FirebaseTelemetry
import com.evora.technologies.office.engine.monetization.IsDebugBuild
import com.evora.technologies.office.engine.monetization.PlayBillingClientGateway
import com.evora.technologies.office.engine.monetization.PlayBillingEntitlementRepository
import com.evora.technologies.office.engine.monetization.TelemetrySink
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class MonetizationBindingsModule {

    @Binds
    @Singleton
    abstract fun bindEntitlementSigner(impl: AndroidKeystoreHmacSigner): EntitlementSigner

    @Binds
    @Singleton
    abstract fun bindBillingGateway(impl: PlayBillingClientGateway): BillingGateway

    @Binds
    @Singleton
    abstract fun bindProEntitlementPort(impl: PlayBillingEntitlementRepository): ProEntitlementPort

    @Binds
    @Singleton
    abstract fun bindRemoteConfigPort(impl: FirebaseRemoteConfigManager): RemoteConfigPort

    @Binds
    @Singleton
    abstract fun bindTelemetrySink(impl: FirebaseAnalyticsSink): TelemetrySink

    @Binds
    @Singleton
    abstract fun bindAnalyticsPort(impl: FirebaseTelemetry): AnalyticsPort
}

@Module
@InstallIn(SingletonComponent::class)
object MonetizationConfigModule {

    @Provides
    @IsDebugBuild
    fun provideIsDebugBuild(): Boolean = BuildConfig.DEBUG
}
