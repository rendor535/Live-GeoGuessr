package com.example.livegeoguessr.data.remote.firebase

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.functions.FirebaseFunctions

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object FirebaseModule {

    val auth: FirebaseAuth by lazy {
        FirebaseAuth.getInstance()
    }

    val firestore: FirebaseFirestore by lazy {
        FirebaseFirestore.getInstance()
    }

    val storage: FirebaseStorage by lazy {
        FirebaseStorage.getInstance()
    }

    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth {
        return auth
    }

    @Provides
    @Singleton
    fun provideFirebaseFirestore(): FirebaseFirestore {
        return firestore
    }

    @Provides
    @Singleton
    fun provideFirebaseStorage(): FirebaseStorage {
        return storage
    }
    val functions: FirebaseFunctions by lazy {
        FirebaseFunctions.getInstance("us-central1")
    }
}