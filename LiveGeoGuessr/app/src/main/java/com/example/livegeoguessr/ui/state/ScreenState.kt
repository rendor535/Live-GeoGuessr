package com.example.livegeoguessr.ui.state

sealed interface ScreenState<out T> {
    data object Loading : ScreenState<Nothing>

    data class Content<T>(val data: T) : ScreenState<T>

    data class Error(
        val message: String? = null,
        val errorResId: Int? = null,
        val throwable: Throwable? = null
    ) : ScreenState<Nothing>

    data class Empty(val message: String? = null) : ScreenState<Nothing>
}
