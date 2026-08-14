package org.dhamma.dipi.staff.datastore

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import org.dhamma.dipi.staff.network.TokenStore
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DatastoreModule {
    @Binds
    @Singleton
    abstract fun tokens(impl: SessionStore): TokenStore
}
