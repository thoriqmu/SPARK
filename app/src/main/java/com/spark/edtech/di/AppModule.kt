package com.spark.edtech.di

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.spark.edtech.data.repository.AuthRepository
import com.spark.edtech.data.repository.AuthRepositoryImpl
import com.spark.edtech.data.source.FirebaseDataSource
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth {
        return FirebaseAuth.getInstance()
    }

    @Provides
    @Singleton
    fun provideFirebaseDatabase(): FirebaseDatabase {
        return FirebaseDatabase.getInstance()
    }

    @Provides
    @Singleton
    fun provideFirebaseDataSource(
        auth: FirebaseAuth,
        database: FirebaseDatabase
    ): FirebaseDataSource {
        return FirebaseDataSource(auth, database)
    }

    @Provides
    @Singleton
    fun provideAuthRepository(dataSource: FirebaseDataSource): AuthRepository {
        return AuthRepositoryImpl(dataSource)
    }
}