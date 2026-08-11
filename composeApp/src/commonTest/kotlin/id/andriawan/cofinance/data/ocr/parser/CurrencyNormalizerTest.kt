package id.andriawan.cofinance.data.ocr.parser

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class CurrencyNormalizerTest {

    @Test
    fun parsesRupiahPrefixWithThousandsSeparator() {
        assertEquals(10_000L, CurrencyNormalizer.parse("Rp10.000"))
        assertEquals(75_000L, CurrencyNormalizer.parse("Rp.75.000"))
        assertEquals(25_000L, CurrencyNormalizer.parse("Rp 25.000"))
    }

    @Test
    fun parsesIdrPrefix() {
        assertEquals(1_234_567L, CurrencyNormalizer.parse("IDR 1.234.567"))
        assertEquals(5_000L, CurrencyNormalizer.parse("idr5.000"))
    }

    @Test
    fun parsesTrailingDecimalComma() {
        assertEquals(25_000L, CurrencyNormalizer.parse("Rp25.000,00"))
        assertEquals(1_500L, CurrencyNormalizer.parse("1.500,50"))
    }

    @Test
    fun parsesTrailingDashSuffix() {
        assertEquals(10_000L, CurrencyNormalizer.parse("Rp10.000,-"))
    }

    @Test
    fun parsesUngroupedDigits() {
        assertEquals(50_000L, CurrencyNormalizer.parse("50000"))
    }

    @Test
    fun returnsNullForMalformedInput() {
        assertNull(CurrencyNormalizer.parse(""))
        assertNull(CurrencyNormalizer.parse("Rp"))
        assertNull(CurrencyNormalizer.parse("abc"))
        assertNull(CurrencyNormalizer.parse("Rp -"))
        assertNull(CurrencyNormalizer.parse("10.00"))
        assertNull(CurrencyNormalizer.parse("1.2.3"))
        assertNull(CurrencyNormalizer.parse("10,000"))
        assertNull(CurrencyNormalizer.parse("-10.000"))
    }

    @Test
    fun collectsCurrencyTokensInReadingOrder() {
        assertEquals(listOf(12_500L), CurrencyNormalizer.tokensIn("TOTAL 12.500"))
        assertEquals(listOf(1_000L, 2_500L), CurrencyNormalizer.tokensIn("Rp1.000 dan Rp2.500"))
    }

    @Test
    fun skipsDatesTimesAndIdentifiers() {
        assertEquals(emptyList<Long>(), CurrencyNormalizer.tokensIn("05/03/2026 13:45:07"))
        assertEquals(emptyList<Long>(), CurrencyNormalizer.tokensIn("No Ref 1234567890123"))
    }
}
