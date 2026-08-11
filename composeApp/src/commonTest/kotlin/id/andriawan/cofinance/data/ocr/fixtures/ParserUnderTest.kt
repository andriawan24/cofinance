package id.andriawan.cofinance.data.ocr.fixtures

import id.andriawan.cofinance.data.ocr.OcrResult
import id.andriawan.cofinance.data.ocr.parser.ReceiptParser
import id.andriawan.cofinance.domain.model.response.ReceiptScan
import kotlin.time.ExperimentalTime

/**
 * The parse function the accuracy harness measures.
 *
 * Kept as a plain function reference so the harness has exactly one dependency
 * on the parser and no knowledge of how it is constructed.
 */
typealias ParseReceipt = (OcrResult) -> ReceiptScan

/**
 * Wiring point between the accuracy harness and the real parser.
 *
 * The harness measures the parser as the scan flow consumes it — through the
 * domain mapping — so a regression in either the parsing or the mapping shows up
 * as an accuracy drop.
 */
@OptIn(ExperimentalTime::class)
object ParserUnderTest {
    private val parser = ReceiptParser()

    val parse: ParseReceipt? = { ocr -> ReceiptScan.from(parser.parse(ocr)) }
}
