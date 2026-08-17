package com.qualorock.shared.detail

import com.qualorock.shared.domain.Price
import kotlin.math.roundToLong

/** TICKET-002 — one shared implementation of the price-line copy rules, used by Compose and SwiftUI. */
object PriceLineFormatter {
    fun format(price: Price?): String =
        when {
            price == null -> "Preço não informado"
            price.isFree -> "Gratuito"
            price.min == null && price.max == null -> "Preço não informado"
            else -> "A partir de R$ ${formatBrl(price.min ?: price.max!!)}"
        }

    private fun formatBrl(value: Double): String {
        val totalCents = (value * 100).roundToLong()
        val intPart = totalCents / 100
        val fracPart = (totalCents % 100).toString().padStart(2, '0')
        return "${groupThousands(intPart.toString())},$fracPart"
    }

    private fun groupThousands(digits: String): String = digits.reversed().chunked(3).joinToString(".").reversed()
}
