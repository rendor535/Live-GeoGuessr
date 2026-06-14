package com.example.livegeoguessr.data.remote.firebase

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.storage.FirebaseStorage

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import com.example.livegeoguessr.BuildConfig
@Module
@InstallIn(SingletonComponent::class)
object FirebaseModule {

    val auth: FirebaseAuth by lazy {
        FirebaseAuth.getInstance().apply {
            if (BuildConfig.USE_FIREBASE_EMULATORS) {
                useEmulator(
                    BuildConfig.FIREBASE_EMULATOR_HOST,
                    9099
                )
            }
        }
    }

    val firestore: FirebaseFirestore by lazy {
        FirebaseFirestore.getInstance().apply {
            if (BuildConfig.USE_FIREBASE_EMULATORS) {
                useEmulator(
                    BuildConfig.FIREBASE_EMULATOR_HOST,
                    8080
                )
            }
        }
    }

    val storage: FirebaseStorage by lazy {
        FirebaseStorage.getInstance()
    }

    val functions: FirebaseFunctions by lazy {
        FirebaseFunctions.getInstance("us-central1")
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

    @Provides
    @Singleton
    fun provideFirebaseFunctions(): FirebaseFunctions {
        return functions
    }
}