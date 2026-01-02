package com.sonique.domain.data.model.searchResult

import com.sonique.domain.data.type.SearchResultType

data class SearchSuggestions(
    val queries: List<String>,
    val recommendedItems: List<SearchResultType>,
)

