package com.docstencil.core.integration

import com.docstencil.core.api.OfficeTemplate
import com.docstencil.core.api.OfficeTemplateOptions
import com.docstencil.core.helper.DocxComparisonHelper
import com.docstencil.core.helper.SectionFactory
import kotlin.test.Test
import kotlin.time.measureTimedValue

class PerformanceE2ETest {
    private inline fun <T> timed(label: String, block: () -> T): T {
        val (result, duration) = measureTimedValue(block)
        println("$label: $duration")
        return result
    }

    @Test
    fun `should handle document with large amounts of text`() {
        val testName = "e2e_performance_text"
        val templateBytes = DocxComparisonHelper.loadFixture("${testName}_in.docx")
        val expectedBytes = DocxComparisonHelper.loadFixture("${testName}_out.docx")

        val sections = SectionFactory.createSections()
        val data = mapOf("sections" to sections)

        val options = OfficeTemplateOptions(parallelRendering = true)
        val actualBytes =
            timed(testName) { OfficeTemplate.fromBytes(templateBytes, options).render(data).bytes }

        DocxComparisonHelper.assertDocxEquals(expectedBytes, actualBytes, testName)
    }

    @Test
    fun `should handle document with expensive calculations`() {
        val testName = "e2e_performance_cpu"
        val templateBytes = DocxComparisonHelper.loadFixture("${testName}_in.docx")
        val expectedBytes = DocxComparisonHelper.loadFixture("${testName}_out.docx")

        val options = OfficeTemplateOptions(parallelRendering = true)
        val actualBytes =
            timed(testName) {
                OfficeTemplate.fromBytes(templateBytes, options).render(emptyMap()).bytes
            }

        DocxComparisonHelper.assertDocxEquals(expectedBytes, actualBytes, testName)
    }
}
