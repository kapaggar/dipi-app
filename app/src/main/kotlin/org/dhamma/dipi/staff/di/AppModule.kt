package org.dhamma.dipi.staff.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import org.dhamma.dipi.staff.BuildConfig
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @Singleton
    @Named("useMock")
    fun useMock(): Boolean = BuildConfig.USE_MOCK

    @Provides
    @Singleton
    @Named("baseUrl")
    fun baseUrl(): String = BuildConfig.BASE_URL
}
