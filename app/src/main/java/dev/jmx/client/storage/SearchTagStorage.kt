package dev.jmx.client.storage

import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class SearchTagStorage(
    private val secureStorage: SecureStorage
) {
    companion object {
        private const val STORAGE_KEY = "searchTag"
    }

    private var _state = MutableStateFlow<List<String>?>(null)
    val state = _state.asStateFlow()

    fun set(list: List<String>) {
        _state.update {
            list
        }
        secureStorage.set(STORAGE_KEY, state.value)
    }

    fun get(): List<String> {
        if (_state.value == null) {
            _state.update {
                secureStorage.get(STORAGE_KEY, object : TypeToken<List<String>>() {}.type)
                    ?: emptyList()
            }
        }
        return _state.value!!
    }
}
