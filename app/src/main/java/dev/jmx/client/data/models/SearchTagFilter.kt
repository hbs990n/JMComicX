package dev.jmx.client.data.models

enum class SearchTagFilterMode {
    INCLUDE,
    EXCLUDE,
}

data class SearchTagFilter(
    val mode: SearchTagFilterMode = SearchTagFilterMode.INCLUDE,
    val tags: List<String> = emptyList(),
) {
    val enabled: Boolean get() = tags.isNotEmpty()
}
