package com.docstencil.core.integration

import com.docstencil.core.api.OfficeTemplate
import com.docstencil.core.api.OfficeTemplateOptions
import com.docstencil.core.helper.DocxComparisonHelper
import com.docstencil.core.helper.Invoice
import com.docstencil.core.helper.InvoiceAddress
import com.docstencil.core.helper.InvoicePerson
import com.docstencil.core.helper.InvoicePosition
import com.docstencil.core.helper.SectionFactory
import java.time.LocalDate
import java.util.Locale
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Throughput benchmarks for the numbers published in the performance report.
 *
 * Every scenario is capped by both an iteration count and a wall-clock budget, so the
 * whole class stays fast on slow machines while still producing stable statistics on
 * fast ones. Results are printed as `BENCH |`-prefixed summary lines (captured in the
 * Gradle test report XML under system-out).
 *
 * Run with:
 * ./gradlew :docstencil-core:test --tests "*ThroughputBenchmarkTest*"
 */
class ThroughputBenchmarkTest {
    // Consumed sizes accumulate here so the JIT cannot eliminate the rendering work.
    private val blackhole = AtomicLong()

    private val invoiceData = mapOf(
        "invoice" to Invoice(
            id = "20250815",
            serviceName = "Development services",
            freelancer = InvoicePerson(
                name = "Fred Freelancer",
                address = InvoiceAddress("Stubenring", "1", "1234", "Vienna", "Austria"),
                uid = "ATU12345678",
                companyRegistrationNumber = "FN 123456",
                iban = "AT123400000012345678",
            ),
            customer = InvoicePerson(
                name = "Big Corp Ltd.",
                address = InvoiceAddress("Tauentzienstraße", "42", "54321", "Berlin", "Germany"),
                uid = "DEU123456789",
                companyRegistrationNumber = "HR 1234567",
                iban = "DE123400000012345678",
            ),
            positions = listOf(
                InvoicePosition("Consulting", 10, 100),
                InvoicePosition("Development", 50, 120),
            ),
            date = LocalDate.of(2025, 8, 15),
            serviceFrom = LocalDate.of(2025, 7, 19),
            serviceTo = LocalDate.of(2025, 6, 16),
            vatPercent = 20,
        ),
    )

    private val ukLocale = OfficeTemplateOptions(locale = Locale.UK)

    @Test
    fun `minimal template - render only`() {
        val template = OfficeTemplate.fromBytes(DocxComparisonHelper.loadFixture("e2e_hello_in.docx"))
        bench("hello_render_only", maxIterations = 300, budgetSeconds = 10.0, warmup = 50) {
            template.render(mapOf("name" to "world")).bytes
        }
    }

    @Test
    fun `minimal template - full pipeline including template load`() {
        val templateBytes = DocxComparisonHelper.loadFixture("e2e_hello_in.docx")
        bench("hello_full_pipeline", maxIterations = 300, budgetSeconds = 10.0, warmup = 50) {
            OfficeTemplate.fromBytes(templateBytes).render(mapOf("name" to "world")).bytes
        }
    }

    @Test
    fun `invoice template - render only`() {
        val template = OfficeTemplate.fromBytes(
            DocxComparisonHelper.loadFixture("e2e_invoice_in.docx"),
            ukLocale,
        )
        bench("invoice_render_only", maxIterations = 300, budgetSeconds = 15.0, warmup = 50) {
            template.render(invoiceData).bytes
        }
    }

    @Test
    fun `invoice template - full pipeline including template load`() {
        val templateBytes = DocxComparisonHelper.loadFixture("e2e_invoice_in.docx")
        bench("invoice_full_pipeline", maxIterations = 300, budgetSeconds = 15.0, warmup = 50) {
            OfficeTemplate.fromBytes(templateBytes, ukLocale).render(invoiceData).bytes
        }
    }

    @Test
    fun `invoice template - concurrent rendering on all cores`() {
        val templateBytes = DocxComparisonHelper.loadFixture("e2e_invoice_in.docx")
        val threads = Runtime.getRuntime().availableProcessors()
        val documents = 2000

        // Warmup (also exercises the code path once per thread)
        repeat(threads * 2) {
            blackhole.addAndGet(
                OfficeTemplate.fromBytes(templateBytes, ukLocale).render(invoiceData).bytes.size.toLong(),
            )
        }

        val pool = Executors.newFixedThreadPool(threads)
        val start = System.nanoTime()
        repeat(documents) {
            pool.submit {
                blackhole.addAndGet(
                    OfficeTemplate.fromBytes(templateBytes, ukLocale)
                        .render(invoiceData).bytes.size.toLong(),
                )
            }
        }
        pool.shutdown()
        assertTrue(pool.awaitTermination(120, TimeUnit.SECONDS), "Benchmark pool did not finish")
        val elapsedSec = (System.nanoTime() - start) / 1e9

        println(
            "BENCH | scenario=invoice_concurrent | threads=$threads | docs=$documents | " +
                "wall=%.2fs | docs_per_sec=%.1f | docs_per_min=%.0f".format(
                    elapsedSec, documents / elapsedSec, documents / elapsedSec * 60,
                ),
        )
    }

    @Test
    fun `large generated report - render only`() {
        val template = OfficeTemplate.fromBytes(
            DocxComparisonHelper.loadFixture("e2e_performance_text_in.docx"),
            OfficeTemplateOptions(parallelRendering = true),
        )
        val data = mapOf("sections" to SectionFactory.createSections())
        bench("large_report_parallel", maxIterations = 5, budgetSeconds = 90.0, warmup = 1) {
            template.render(data).bytes
        }
    }

    private fun bench(
        scenario: String,
        maxIterations: Int,
        budgetSeconds: Double,
        warmup: Int,
        block: () -> ByteArray,
    ) {
        var outputSize = 0
        repeat(warmup) { outputSize = block().also { blackhole.addAndGet(it.size.toLong()) }.size }

        val timesNanos = ArrayList<Long>(maxIterations)
        val budgetNanos = (budgetSeconds * 1e9).toLong()
        val benchStart = System.nanoTime()
        while (timesNanos.size < maxIterations && System.nanoTime() - benchStart < budgetNanos) {
            val t0 = System.nanoTime()
            val bytes = block()
            timesNanos.add(System.nanoTime() - t0)
            blackhole.addAndGet(bytes.size.toLong())
        }

        assertTrue(timesNanos.isNotEmpty(), "No iterations completed for $scenario")

        val sorted = timesNanos.sorted()
        val meanMs = timesNanos.average() / 1e6
        val medianMs = sorted[sorted.size / 2] / 1e6
        val p95Ms = sorted[(sorted.size * 95 / 100).coerceAtMost(sorted.size - 1)] / 1e6
        val docsPerSec = 1000.0 / meanMs

        println(
            "BENCH | scenario=$scenario | iters=${timesNanos.size} | " +
                "mean=%.2fms | median=%.2fms | p95=%.2fms | ".format(meanMs, medianMs, p95Ms) +
                "docs_per_sec=%.1f | docs_per_min=%.0f | output_bytes=%d".format(
                    docsPerSec, docsPerSec * 60, outputSize,
                ),
        )
    }
}
