package org.dhamma.dipi.staff.database

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import net.sqlcipher.database.SupportFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun db(@ApplicationContext context: Context): DipiDb {
        val factory = SupportFactory("dipi-staff-local".toByteArray())
        return Room.databaseBuilder(context, DipiDb::class.java, "dipi-staff.db")
            .openHelperFactory(factory)
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    fun applicants(db: DipiDb): ApplicantDao = db.applicants()

    @Provides
    fun outbox(db: DipiDb): OutboxDao = db.outbox()
}
