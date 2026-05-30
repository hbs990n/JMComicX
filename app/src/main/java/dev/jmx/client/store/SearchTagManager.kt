package dev.jmx.client.store

import dev.jmx.client.storage.SearchTagStorage
import dev.jmx.client.task.AppInitTask
import dev.jmx.client.task.AppTaskInfo
import dev.jmx.client.utils.log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class SearchTagManager(
    private val searchTagStorage: SearchTagStorage
) : AppInitTask {

    private val _searchTagState = MutableStateFlow(emptyList<String>())
    val searchTagState = _searchTagState.asStateFlow()

    fun addTag(tag: String) {
        val value = normalizeTag(tag) ?: return
        val list = listOf(value) + _searchTagState.value.filterNot { it.equals(value, ignoreCase = true) }
        _searchTagState.update { list }
        searchTagStorage.set(list)
        JmxDiagnostics.d(
            "Storage",
            "Search tag persisted",
            metadata = mapOf(
                "operation" to "write",
                "target" to "search_tag",
                "tag_length" to value.length,
                "total_count" to list.size
            )
        )
    }

    fun removeTag(tag: String) {
        val list = _searchTagState.value.filterNot { it.equals(tag, ignoreCase = true) }
        _searchTagState.update { list }
        searchTagStorage.set(list)
        JmxDiagnostics.d(
            "Storage",
            "Search tag removed",
            metadata = mapOf(
                "operation" to "delete",
                "target" to "search_tag",
                "total_count" to list.size
            )
        )
    }

    fun clear() {
        _searchTagState.update { emptyList() }
        searchTagStorage.set(emptyList())
        JmxDiagnostics.d(
            "Storage",
            "Search tags cleared",
            metadata = mapOf(
                "operation" to "delete",
                "target" to "search_tag"
            )
        )
    }

    override suspend fun init() {
        log("加载搜索标签数据")
        _searchTagState.update {
            searchTagStorage.get().mapNotNull(::normalizeTag).distinctBy { it.lowercase() }
        }
        JmxDiagnostics.d(
            "Storage",
            "Search tags loaded",
            metadata = mapOf(
                "operation" to "read",
                "target" to "search_tag",
                "total_count" to _searchTagState.value.size
            )
        )
        log("已加载搜索标签数据")
    }

    private val appTaskInfo = AppTaskInfo(
        taskName = "加载搜索标签数据",
        sort = 5,
    )

    override fun getAppTaskInfo(): AppTaskInfo = appTaskInfo

    private fun normalizeTag(tag: String): String? {
        return tag.trim()
            .trim(',', '，', '、', '#', ' ')
            .takeIf { it.isNotBlank() }
    }
}
