package com.evora.technologies.office.engine.monetization.di

import com.evora.technologies.office.core.designsystem.component.InlineNativeAdSlotRenderer
import com.evora.technologies.office.core.domain.port.AdPlacementHook
import com.evora.technologies.office.core.domain.port.AnalyticsPort
import com.evora.technologies.office.core.domain.port.ConsentPort
import com.evora.technologies.office.core.domain.port.ProEntitlementPort
import com.evora.technologies.office.core.domain.port.RemoteConfigPort
import com.evora.technologies.office.engine.monetization.AdMobInlineNativeRenderer
import com.evora.technologies.office.engine.monetization.AndroidKeystoreHmacSigner
import com.evora.technologies.office.engine.monetization.BillingGateway
import com.evora.technologies.office.engine.monetization.BuildConfig
import com.evora.technologies.office.engine.monetization.EntitlementSigner
import com.evora.technologies.office.engine.monetization.FirebaseAndAdjustSink
import com.evora.technologies.office.engine.monetization.FirebaseAndAdjustTelemetry
import com.evora.technologies.office.engine.monetization.FirebaseRemoteConfigManager
import com.evora.technologies.office.engine.monetization.IsDebugBuild
import com.evora.technologies.office.engine.monetization.MediatedAdPlacementManager
import com.evora.technologies.office.engine.monetization.PlayBillingClientGateway
import com.evora.technologies.office.engine.monetization.PlayBillingEntitlementRepository
import com.evora.technologies.office.engine.monetization.TelemetrySink
import com.evora.technologies.office.engine.monetization.UmpConsentManager
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
    abstract fun bindConsentPort(impl: UmpConsentManager): ConsentPort

    @Binds
    @Singleton
    abstract fun bindRemoteConfigPort(impl: FirebaseRemoteConfigManager): RemoteConfigPort

    @Binds
    @Singleton
    abstract fun bindTelemetrySink(impl: FirebaseAndAdjustSink): TelemetrySink

    @Binds
    @Singleton
    abstract fun bindAnalyticsPort(impl: FirebaseAndAdjustTelemetry): AnalyticsPort

    @Binds
    @Singleton
    abstract fun bindAdPlacementHook(impl: MediatedAdPlacementManager): AdPlacementHook

    @Binds
    @Singleton
    abstract fun bindInlineNativeAdSlotRenderer(impl: AdMobInlineNativeRenderer): InlineNativeAdSlotRenderer
}

@Module
@InstallIn(SingletonComponent::class)
object MonetizationConfigModule {

    @Provides
    @IsDebugBuild
    fun provideIsDebugBuild(): Boolean = BuildConfig.DEBUG
}
