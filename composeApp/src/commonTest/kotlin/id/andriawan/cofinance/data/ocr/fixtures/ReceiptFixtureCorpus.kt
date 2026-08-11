package id.andriawan.cofinance.data.ocr.fixtures

import kotlinx.serialization.json.Json

/**
 * Loads every checked-in receipt fixture.
 *
 * Fixtures are stored as JSON. Because `commonTest` has no portable filesystem
 * or resource-loading API across Android, iOS and the JVM host, each JSON
 * document is held verbatim in a Kotlin raw string in its own file under this
 * directory and registered in [ReceiptFixtureRegistry]. The JSON is the format
 * of record: it is what a capture tool emits and what a reviewer reads.
 *
 * See `README.md` in this directory for how to capture and add a fixture.
 */
object ReceiptFixtureCorpus {

    private val json = Json {
        ignoreUnknownKeys = false
        isLenient = false
    }

    /** Every registered fixture, synthetic ones included. */
    val all: List<ReceiptFixture> by lazy {
        ReceiptFixtureRegistry.documents.map(::parse)
    }

    /**
     * The accuracy corpus: real captured receipts only.
     *
     * Synthetic harness fixtures are deliberately excluded — measuring accuracy
     * against hand-written input would make the thresholds meaningless.
     */
    val accuracyCorpus: List<ReceiptFixture> by lazy {
        all.filterNot(ReceiptFixture::synthetic)
    }

    /** Fixtures that exist only to exercise this harness. Never measured. */
    val syntheticHarnessFixtures: List<ReceiptFixture> by lazy {
        all.filter(ReceiptFixture::synthetic)
    }

    /** Source types with no real fixture backing them. */
    fun missingSourceTypes(corpus: List<ReceiptFixture> = accuracyCorpus): List<ReceiptSourceType> {
        val covered = corpus.map(ReceiptFixture::sourceType).toSet()
        return ReceiptSourceType.entries.filterNot(covered::contains)
    }

    /** Parses one fixture document. Exposed so the harness self-test can round-trip JSON. */
    fun parse(document: String): ReceiptFixture = json.decodeFromString(document)
}

/**
 * The registered fixture documents.
 *
 * To add a real fixture: create `<id>.kt` next to this file holding the JSON in
 * a raw string constant, then add that constant to [documents].
 */
object ReceiptFixtureRegistry {

    /**
     * Real captured receipt fixtures. **Currently empty.**
     *
     * This list is empty because no real Indonesian receipts were available when
     * the harness was built. `ReceiptCorpusAccuracyTest` fails while it stays
     * empty — that failure is the reminder, not an oversight.
     */
    val realFixtures: List<String> = emptyList()

    /** Synthetic fixtures used only to test this harness. Never part of the corpus. */
    val syntheticFixtures: List<String> = listOf(
        SYNTHETIC_HARNESS_FIXTURE_CLEAN,
        SYNTHETIC_HARNESS_FIXTURE_FEE_BEARING,
        SYNTHETIC_HARNESS_FIXTURE_NO_CATEGORY
    )

    val documents: List<String> = realFixtures + syntheticFixtures
}
