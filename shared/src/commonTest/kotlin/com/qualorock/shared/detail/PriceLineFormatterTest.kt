package com.qualorock.shared.detail

import com.qualorock.shared.domain.Price
import kotlin.test.Test
import kotlin.test.assertEquals

class PriceLineFormatterTest {
    @Test
    fun `given a null price when format is called then it returns preco nao informado`() {
        assertEquals("Preço não informado", PriceLineFormatter.format(null))
    }

    @Test
    fun `given a free price when format is called then it returns gratuito`() {
        val price = Price(isFree = true, min = null, max = null, currency = "BRL")
        assertEquals("Gratuito", PriceLineFormatter.format(price))
    }

    @Test
    fun `given a single tier price when format is called then it returns a partir de formatted brl`() {
        val price = Price(isFree = false, min = 60.0, max = 60.0, currency = "BRL")
        assertEquals("A partir de R$ 60,00", PriceLineFormatter.format(price))
    }

    @Test
    fun `given a multi tier price when format is called then it returns a partir de the lowest tier`() {
        val price = Price(isFree = false, min = 1234.5, max = 2000.0, currency = "BRL")
        assertEquals("A partir de R$ 1.234,50", PriceLineFormatter.format(price))
    }
}
