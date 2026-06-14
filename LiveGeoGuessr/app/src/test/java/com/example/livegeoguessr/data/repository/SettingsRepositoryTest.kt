package com.example.livegeoguessr.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.mutablePreferencesOf
import com.example.livegeoguessr.data.local.datastore.PreferencesKeys
import com.example.livegeoguessr.data.local.datastore.dataStore
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.IOException

class SettingsRepositoryTest {

    private lateinit var context: Context
    private lateinit var dataStore: FakePreferencesDataStore
    private lateinit var repository: SettingsRepository

    @BeforeEach
    fun setUp() {
        context = mockk()
        dataStore = FakePreferencesDataStore()

        /*
         * dataStore jest modułową właściwością rozszerzającą Context.
         * Mockujemy jej statyczny getter przed utworzeniem repozytorium.
         */
        mockkStatic(Context::dataStore)

        every {
            context.dataStore
        } returns dataStore

        repository = SettingsRepository(context)
    }

    @AfterEach
    fun tearDown() {
        unmockkStatic(Context::dataStore)
    }

    // -------------------------------------------------------------------------
    // darkModeFlow
    // -------------------------------------------------------------------------

    @Test
    fun `darkModeFlow returns stored true value`() = runTest {
        val repository = createRepository(
            FakePreferencesDataStore(
                initialPreferences = mutablePreferencesOf(
                    PreferencesKeys.DARK_MODE to true
                )
            )
        )

        val result = repository.darkModeFlow.first()

        assertTrue(result)
    }

    @Test
    fun `darkModeFlow returns stored false value`() = runTest {
        val repository = createRepository(
            FakePreferencesDataStore(
                initialPreferences = mutablePreferencesOf(
                    PreferencesKeys.DARK_MODE to false
                )
            )
        )

        val result = repository.darkModeFlow.first()

        assertFalse(result)
    }

    @Test
    fun `darkModeFlow defaults to false when preference is missing`() =
        runTest {
            val repository = createRepository(
                FakePreferencesDataStore(
                    initialPreferences = emptyPreferences()
                )
            )

            val result = repository.darkModeFlow.first()

            assertFalse(result)
        }

    @Test
    fun `darkModeFlow emits false when DataStore throws IOException`() =
        runTest {
            val repository = createRepository(
                FakePreferencesDataStore(
                    readException = IOException(
                        "Preferences read failed"
                    )
                )
            )

            val result = repository.darkModeFlow.first()

            assertFalse(result)
        }

    @Test
    fun `darkModeFlow rethrows exception other than IOException`() {
        val repository = createRepository(
            FakePreferencesDataStore(
                readException = IllegalStateException(
                    "Unexpected DataStore error"
                )
            )
        )

        val exception = assertThrows(
            IllegalStateException::class.java
        ) {
            runTest {
                repository.darkModeFlow.first()
            }
        }

        assertEquals(
            "Unexpected DataStore error",
            exception.message
        )
    }

    // -------------------------------------------------------------------------
    // useMilesFlow
    // -------------------------------------------------------------------------

    @Test
    fun `useMilesFlow returns stored true value`() = runTest {
        val repository = createRepository(
            FakePreferencesDataStore(
                initialPreferences = mutablePreferencesOf(
                    PreferencesKeys.USE_MILES to true
                )
            )
        )

        val result = repository.useMilesFlow.first()

        assertTrue(result)
    }

    @Test
    fun `useMilesFlow returns stored false value`() = runTest {
        val repository = createRepository(
            FakePreferencesDataStore(
                initialPreferences = mutablePreferencesOf(
                    PreferencesKeys.USE_MILES to false
                )
            )
        )

        val result = repository.useMilesFlow.first()

        assertFalse(result)
    }

    @Test
    fun `useMilesFlow defaults to false when preference is missing`() =
        runTest {
            val repository = createRepository(
                FakePreferencesDataStore(
                    initialPreferences = emptyPreferences()
                )
            )

            val result = repository.useMilesFlow.first()

            assertFalse(result)
        }

    @Test
    fun `useMilesFlow emits false when DataStore throws IOException`() =
        runTest {
            val repository = createRepository(
                FakePreferencesDataStore(
                    readException = IOException(
                        "Preferences read failed"
                    )
                )
            )

            val result = repository.useMilesFlow.first()

            assertFalse(result)
        }

    @Test
    fun `useMilesFlow rethrows exception other than IOException`() {
        val repository = createRepository(
            FakePreferencesDataStore(
                readException = IllegalArgumentException(
                    "Unexpected DataStore error"
                )
            )
        )

        val exception = assertThrows(
            IllegalArgumentException::class.java
        ) {
            runTest {
                repository.useMilesFlow.first()
            }
        }

        assertEquals(
            "Unexpected DataStore error",
            exception.message
        )
    }

    // -------------------------------------------------------------------------
    // isLoggedInFlow
    // -------------------------------------------------------------------------

    @Test
    fun `isLoggedInFlow returns stored true value`() = runTest {
        val repository = createRepository(
            FakePreferencesDataStore(
                initialPreferences = mutablePreferencesOf(
                    PreferencesKeys.isLoggedIn to true
                )
            )
        )

        val result = repository.isLoggedInFlow.first()

        assertTrue(result)
    }

    @Test
    fun `isLoggedInFlow returns stored false value`() = runTest {
        val repository = createRepository(
            FakePreferencesDataStore(
                initialPreferences = mutablePreferencesOf(
                    PreferencesKeys.isLoggedIn to false
                )
            )
        )

        val result = repository.isLoggedInFlow.first()

        assertFalse(result)
    }

    @Test
    fun `isLoggedInFlow defaults to false when preference is missing`() =
        runTest {
            val repository = createRepository(
                FakePreferencesDataStore(
                    initialPreferences = emptyPreferences()
                )
            )

            val result = repository.isLoggedInFlow.first()

            assertFalse(result)
        }

    @Test
    fun `isLoggedInFlow emits false when DataStore throws IOException`() =
        runTest {
            val repository = createRepository(
                FakePreferencesDataStore(
                    readException = IOException(
                        "Preferences read failed"
                    )
                )
            )

            val result = repository.isLoggedInFlow.first()

            assertFalse(result)
        }

    @Test
    fun `isLoggedInFlow rethrows exception other than IOException`() {
        val repository = createRepository(
            FakePreferencesDataStore(
                readException = RuntimeException(
                    "Unexpected DataStore error"
                )
            )
        )

        val exception = assertThrows(
            RuntimeException::class.java
        ) {
            runTest {
                repository.isLoggedInFlow.first()
            }
        }

        assertEquals(
            "Unexpected DataStore error",
            exception.message
        )
    }

    // -------------------------------------------------------------------------
    // setDarkMode
    // -------------------------------------------------------------------------

    @Test
    fun `setDarkMode stores true value`() = runTest {
        repository.setDarkMode(true)

        assertEquals(
            true,
            dataStore.currentPreferences()[
                PreferencesKeys.DARK_MODE
            ]
        )
    }

    @Test
    fun `setDarkMode stores false value`() = runTest {
        repository.setDarkMode(true)
        repository.setDarkMode(false)

        assertEquals(
            false,
            dataStore.currentPreferences()[
                PreferencesKeys.DARK_MODE
            ]
        )
    }

    @Test
    fun `setDarkMode propagates DataStore update error`() {
        val repository = createRepository(
            FakePreferencesDataStore(
                updateException = IOException(
                    "Write failed"
                )
            )
        )

        val exception = assertThrows(
            IOException::class.java
        ) {
            runTest {
                repository.setDarkMode(true)
            }
        }

        assertEquals("Write failed", exception.message)
    }

    // -------------------------------------------------------------------------
    // setUseMiles
    // -------------------------------------------------------------------------

    @Test
    fun `setUseMiles stores true value`() = runTest {
        repository.setUseMiles(true)

        assertEquals(
            true,
            dataStore.currentPreferences()[
                PreferencesKeys.USE_MILES
            ]
        )
    }

    @Test
    fun `setUseMiles stores false value`() = runTest {
        repository.setUseMiles(true)
        repository.setUseMiles(false)

        assertEquals(
            false,
            dataStore.currentPreferences()[
                PreferencesKeys.USE_MILES
            ]
        )
    }

    @Test
    fun `setUseMiles propagates DataStore update error`() {
        val repository = createRepository(
            FakePreferencesDataStore(
                updateException = IOException(
                    "Write failed"
                )
            )
        )

        val exception = assertThrows(
            IOException::class.java
        ) {
            runTest {
                repository.setUseMiles(true)
            }
        }

        assertEquals("Write failed", exception.message)
    }

    // -------------------------------------------------------------------------
    // setLoggedIn
    // -------------------------------------------------------------------------

    @Test
    fun `setLoggedIn stores true value`() = runTest {
        repository.setLoggedIn(true)

        assertEquals(
            true,
            dataStore.currentPreferences()[
                PreferencesKeys.isLoggedIn
            ]
        )
    }

    @Test
    fun `setLoggedIn stores false value`() = runTest {
        repository.setLoggedIn(true)
        repository.setLoggedIn(false)

        assertEquals(
            false,
            dataStore.currentPreferences()[
                PreferencesKeys.isLoggedIn
            ]
        )
    }

    @Test
    fun `setLoggedIn propagates DataStore update error`() {
        val repository = createRepository(
            FakePreferencesDataStore(
                updateException = IOException(
                    "Write failed"
                )
            )
        )

        val exception = assertThrows(
            IOException::class.java
        ) {
            runTest {
                repository.setLoggedIn(true)
            }
        }

        assertEquals("Write failed", exception.message)
    }

    // -------------------------------------------------------------------------
    // Combined behavior
    // -------------------------------------------------------------------------

    @Test
    fun `all preferences are stored independently`() = runTest {
        repository.setDarkMode(true)
        repository.setUseMiles(false)
        repository.setLoggedIn(true)

        val preferences = dataStore.currentPreferences()

        assertEquals(
            true,
            preferences[PreferencesKeys.DARK_MODE]
        )

        assertEquals(
            false,
            preferences[PreferencesKeys.USE_MILES]
        )

        assertEquals(
            true,
            preferences[PreferencesKeys.isLoggedIn]
        )
    }

    @Test
    fun `flows emit values written by repository`() = runTest {
        repository.setDarkMode(true)
        repository.setUseMiles(true)
        repository.setLoggedIn(true)

        assertTrue(repository.darkModeFlow.first())
        assertTrue(repository.useMilesFlow.first())
        assertTrue(repository.isLoggedInFlow.first())
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private fun createRepository(
        testDataStore: DataStore<Preferences>
    ): SettingsRepository {
        every {
            context.dataStore
        } returns testDataStore

        return SettingsRepository(context)
    }

    /**
     * Prosta implementacja DataStore przeznaczona wyłącznie
     * do testów jednostkowych.
     *
     * Pozwala:
     * - przechowywać Preferences w pamięci,
     * - symulować IOException podczas odczytu,
     * - symulować inne wyjątki podczas odczytu,
     * - symulować błąd zapisu.
     */
    private class FakePreferencesDataStore(
        initialPreferences: Preferences = emptyPreferences(),
        private val readException: Throwable? = null,
        private val updateException: Throwable? = null
    ) : DataStore<Preferences> {

        private val state = MutableStateFlow(
            initialPreferences
        )

        override val data: Flow<Preferences> =
            if (readException == null) {
                state
            } else {
                flow {
                    throw readException
                }
            }

        override suspend fun updateData(
            transform: suspend (Preferences) -> Preferences
        ): Preferences {
            updateException?.let { exception ->
                throw exception
            }

            val updatedPreferences = transform(
                state.value
            )

            state.value = updatedPreferences

            return updatedPreferences
        }

        fun currentPreferences(): Preferences {
            return state.value
        }
    }
}