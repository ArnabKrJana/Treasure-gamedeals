package com.example.treasure.domain.uiModels

import com.example.treasure.utils.Maturity

/**
 * Domain representation of an individual game deal match returned from catalog search queries.
 *
 * @property title Primary title of the matched game.
 * @property price Current and baseline [Price] structure of the best matching deal.
 * @property maturity Content age rating and advisory classification ([Maturity]).
 */
data class SearchResult(
    val title: String,
    val price: Price,
    val maturity: Maturity
)
