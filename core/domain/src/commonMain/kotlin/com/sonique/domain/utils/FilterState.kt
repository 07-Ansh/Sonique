package com.sonique.domain.utils

sealed class FilterState {
    data object CustomOrder : FilterState()

    data object OlderFirst : FilterState()

    data object NewerFirst : FilterState()

    data object Title : FilterState()
}

