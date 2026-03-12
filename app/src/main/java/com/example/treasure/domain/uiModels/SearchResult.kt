package com.example.treasure.domain.uiModels

import com.example.treasure.utils.Maturity

data class SearchResult(
    val title:String,
    val price: Price,
    val maturity: Maturity
)
