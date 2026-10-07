package com.uniflow.app.di

import android.content.Context
import android.content.SharedPreferences
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.uniflow.app.data.repository.AuthRepositoryImpl
import com.uniflow.app.data.repository.UniversityRepositoryImpl
import com.uniflow.app.domain.repository.AuthRepository
import com.uniflow.app.domain.repository.UniversityRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
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
    fun provideFirebaseFirestore(): FirebaseFirestore {
        return FirebaseFirestore.getInstance()
    }

    @Provides
    @Singleton
    fun provideFirebaseStorage(): FirebaseStorage {
        return FirebaseStorage.getInstance()
    }

    @Provides
    @Singleton
    fun provideSharedPreferences(
        @ApplicationContext context: Context
    ): SharedPreferences {
        return context.getSharedPreferences("uniflow_prefs", Context.MODE_PRIVATE)
    }

    @Provides
    @Singleton
    fun provideUniversityRepository(
        firestore: FirebaseFirestore,
        auth: FirebaseAuth
    ): UniversityRepository {
        return UniversityRepositoryImpl(firestore, auth)
    }

    @Provides
    @Singleton
    fun provideAuthRepository(
        auth: FirebaseAuth,
        firestore: FirebaseFirestore,
        storage: FirebaseStorage,
        sharedPreferences: SharedPreferences,
        @ApplicationContext context: Context
    ): AuthRepository {
        return AuthRepositoryImpl(auth, firestore, storage, sharedPreferences, context)
    }
}