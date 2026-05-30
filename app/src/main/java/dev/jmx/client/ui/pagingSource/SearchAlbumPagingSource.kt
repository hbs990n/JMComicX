package dev.jmx.client.ui.pagingSource

import androidx.paging.PagingSource
import androidx.paging.PagingState
import dev.jmx.client.data.models.Album
import dev.jmx.client.data.models.AlbumSearchOrderFilter
import dev.jmx.client.data.models.SearchTagFilter
import dev.jmx.client.data.models.SearchTagFilterMode
import dev.jmx.client.repository.AlbumRepository
import dev.jmx.client.data.remote.model.AlbumListResponse
import dev.jmx.client.data.remote.model.NetworkResult

data class SearchAlbumFilter(
    val order: AlbumSearchOrderFilter = AlbumSearchOrderFilter.NEWEST,
    val searchContent: String = "",
    val tagFilter: SearchTagFilter = SearchTagFilter(),
)

class SearchAlbumPagingSource(
    private val albumRepository: AlbumRepository,
    private val filter: SearchAlbumFilter,
    private val onFindSingleAlbumId: (id: Int?) -> Unit = {}
) : PagingSource<Int, Album>() {
    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Album> {
        val currentPage = params.key ?: 1
        return when (val data =
            albumRepository.getAlbumList(currentPage, filter.order, filter.searchContent)) {
            is NetworkResult.Error -> {
                LoadResult.Error(Exception(data.message))
            }

            is NetworkResult.Success<AlbumListResponse> -> {
                if (data.data.redirect_aid != null) {
                    onFindSingleAlbumId(data.data.redirect_aid.toInt())
                    LoadResult.Page(
                        data = listOf(),
                        prevKey = null,
                        nextKey = null
                    )
                } else {
                    onFindSingleAlbumId(null)
                    val list = filterAlbumsByTags(data.data.toAlbumList())
                    val total = data.data.total.toInt()
                    val isLastPage = currentPage >= (total + params.loadSize - 1) / params.loadSize
                    LoadResult.Page(
                        data = list,
                        prevKey = if (currentPage == 1) null else currentPage - 1,
                        nextKey = if (isLastPage) null else currentPage + 1,
                        itemsAfter = if (filter.tagFilter.enabled && !isLastPage) 1 else LoadResult.Page.COUNT_UNDEFINED
                    )
                }
            }
        }
    }

    override fun getRefreshKey(state: PagingState<Int, Album>): Int? = null

    private suspend fun filterAlbumsByTags(list: List<Album>): List<Album> {
        val tagFilter = filter.tagFilter
        if (!tagFilter.enabled) return list
        return list.mapNotNull { album ->
            when (val detail = albumRepository.getAlbumDetail(album.id)) {
                is NetworkResult.Error -> null
                is NetworkResult.Success -> {
                    val detailedAlbum = detail.data.toAlbum()
                    if (matchesTagFilter(detailedAlbum.tagList, tagFilter)) {
                        detailedAlbum
                    } else {
                        null
                    }
                }
            }
        }
    }

    private fun matchesTagFilter(albumTags: List<String>, filter: SearchTagFilter): Boolean {
        val normalizedAlbumTags = albumTags.map { it.normalizeTag() }
        val targetTags = filter.tags.map { it.normalizeTag() }.filter { it.isNotBlank() }
        if (targetTags.isEmpty()) return true
        return when (filter.mode) {
            SearchTagFilterMode.INCLUDE -> targetTags.all { target ->
                normalizedAlbumTags.any { albumTag ->
                    albumTag == target || albumTag.contains(target) || target.contains(albumTag)
                }
            }

            SearchTagFilterMode.EXCLUDE -> targetTags.none { target ->
                normalizedAlbumTags.any { albumTag ->
                    albumTag == target || albumTag.contains(target) || target.contains(albumTag)
                }
            }
        }
    }

    private fun String.normalizeTag(): String {
        return trim().lowercase()
    }
}
