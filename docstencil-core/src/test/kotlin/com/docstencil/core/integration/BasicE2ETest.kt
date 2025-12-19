package com.docstencil.core.integration

import com.docstencil.core.api.OfficeTemplate
import com.docstencil.core.api.OfficeTemplateOptions
import com.docstencil.core.helper.*
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.test.Test

class BasicE2ETest {
    private val options = OfficeTemplateOptions()

    @Test
    fun `should render formatted placeholders`() {
        val testName = "e2e_formatted_placeholders"
        val templateBytes = DocxComparisonHelper.loadFixture("${testName}_in.docx")
        val expectedBytes = DocxComparisonHelper.loadFixture("${testName}_out.docx")

        val data = mapOf("firstName" to "John", "lastName" to "Doe")

        val actualBytes = OfficeTemplate.fromBytes(templateBytes, options).render(data).bytes

        DocxComparisonHelper.assertDocxEquals(expectedBytes, actualBytes, testName)
    }

    @Test
    fun `should render nested loops with fragmented placeholders`() {
        val testName = "e2e_fragmented_nested_loops"
        val templateBytes = DocxComparisonHelper.loadFixture("${testName}_in.docx")
        val expectedBytes = DocxComparisonHelper.loadFixture("${testName}_out.docx")

        val data = mapOf(
            "companies" to listOf(
                mapOf(
                    "name" to "Foo",
                    "people" to listOf(
                        mapOf("firstName" to "John", "lastName" to "Doe"),
                        mapOf("firstName" to "Mary", "lastName" to "Jane"),
                    ),
                ),
                mapOf(
                    "name" to "Bar",
                    "people" to listOf(
                        mapOf("firstName" to "John2", "lastName" to "Doe2"),
                        mapOf("firstName" to "Mary2", "lastName" to "Jane2"),
                    ),
                ),
            ),
        )

        val actualBytes = OfficeTemplate.fromBytes(templateBytes, options).render(data).bytes

        DocxComparisonHelper.assertDocxEquals(expectedBytes, actualBytes, testName)
    }

    @Test
    fun `should render table loop with dot notation`() {
        val testName = "e2e_dot_notation"
        val templateBytes = DocxComparisonHelper.loadFixture("${testName}_in.docx")
        val expectedBytes = DocxComparisonHelper.loadFixture("${testName}_out.docx")

        val data = mapOf(
            "people" to listOf(
                mapOf("data" to mapOf("firstName" to "John", "lastName" to "Doe")),
                mapOf("data" to mapOf("firstName" to "Mary", "lastName" to "Jane")),
            ),
        )

        val actualBytes = OfficeTemplate.fromBytes(templateBytes, options).render(data).bytes

        DocxComparisonHelper.assertDocxEquals(expectedBytes, actualBytes, testName)
    }

    @Test
    fun `should render nested table loop`() {
        val testName = "e2e_nested_table_loops"
        val templateBytes = DocxComparisonHelper.loadFixture("${testName}_in.docx")
        val expectedBytes = DocxComparisonHelper.loadFixture("${testName}_out.docx")

        val data = mapOf(
            "companies" to listOf(
                Company("A Corp", mutableListOf(Person("John", "Doe"), Person("Mary", "Jane"))),
                Company("B Corp", mutableListOf(Person("Max", "Power"))),
                Company("X", mutableListOf(Person("A", "B"), Person("C", "D"), Person("E", "F"))),
            ),
        )

        val actualBytes = OfficeTemplate.fromBytes(templateBytes, options).render(data).bytes

        DocxComparisonHelper.assertDocxEquals(expectedBytes, actualBytes, testName)
    }

    @Test
    fun `should render table cell loop`() {
        val testName = "e2e_table_cell_loops"
        val templateBytes = DocxComparisonHelper.loadFixture("${testName}_in.docx")
        val expectedBytes = DocxComparisonHelper.loadFixture("${testName}_out.docx")

        val data = mapOf(
            "rows" to listOf(
                listOf(1, 2, 3, 4),
                listOf(5, 6, 7, 8),
                listOf(9, 10, 11, 12),
            ),
        )

        val actualBytes = OfficeTemplate.fromBytes(templateBytes, options).render(data).bytes

        DocxComparisonHelper.assertDocxEquals(expectedBytes, actualBytes, testName)
    }

    @Test
    fun `should render list items with for loop`() {
        val testName = "e2e_for_loop_with_lists"
        val templateBytes = DocxComparisonHelper.loadFixture("${testName}_in.docx")
        val expectedBytes = DocxComparisonHelper.loadFixture("${testName}_out.docx")

        val data = mapOf(
            "items" to listOf("5 apples", "3 oranges", "8 pears"),
        )

        val actualBytes = OfficeTemplate.fromBytes(templateBytes, options).render(data).bytes

        DocxComparisonHelper.assertDocxEquals(expectedBytes, actualBytes, testName)
    }

    @Test
    fun `should render list items with nested for loops`() {
        val testName = "e2e_nested_for_loop_with_lists"
        val templateBytes = DocxComparisonHelper.loadFixture("${testName}_in.docx")
        val expectedBytes = DocxComparisonHelper.loadFixture("${testName}_out.docx")

        val data = mapOf(
            "items" to listOf(
                Item(
                    "A",
                    mutableListOf(
                        Item(
                            "A 1",
                            mutableListOf(
                                Item("A 1 I"),
                                Item("A 1 II"),
                            ),
                        ),
                        Item(
                            "A 2",
                            mutableListOf(
                                Item("A 2 I"),
                                Item("A 2 II"),
                                Item("A 2 III"),
                            ),
                        ),
                    ),
                ),
                Item(
                    "B",
                    mutableListOf(
                        Item("B 1"),
                        Item(
                            "B 2",
                            mutableListOf(
                                Item("B 2 I"),
                                Item("B 2 II"),
                                Item("B 2 III"),
                            ),
                        ),
                        Item("B 3"),
                    ),
                ),
                Item("C"),
            ),
        )

        val actualBytes = OfficeTemplate.fromBytes(templateBytes, options).render(data).bytes

        DocxComparisonHelper.assertDocxEquals(expectedBytes, actualBytes, testName)
    }

    @Test
    fun `should render inline for loop`() {
        val testName = "e2e_inline_for_loop"
        val templateBytes = DocxComparisonHelper.loadFixture("${testName}_in.docx")
        val expectedBytes = DocxComparisonHelper.loadFixture("${testName}_out.docx")

        val data = mapOf(
            "items" to listOf("X", "O", "X", "O", "X", "O", "X", "O", "X", "O"),
        )

        val actualBytes = OfficeTemplate.fromBytes(templateBytes, options).render(data).bytes

        DocxComparisonHelper.assertDocxEquals(expectedBytes, actualBytes, testName)
    }

    @Test
    fun `should call built-in functions`() {
        val testName = "e2e_built_in_functions"
        val templateBytes = DocxComparisonHelper.loadFixture("${testName}_in.docx")
        val expectedBytes = DocxComparisonHelper.loadFixture("${testName}_out.docx")

        val data = mapOf(
            "numVar" to 123.456,
            "dateVar" to LocalDate.of(2020, 8, 20),
            "dateTimeVar" to LocalDateTime.of(2020, 8, 20, 14, 42, 5),
        )
        val actualBytes = OfficeTemplate.fromBytes(templateBytes, options).render(data).bytes

        DocxComparisonHelper.assertDocxEquals(expectedBytes, actualBytes, testName)
    }

    @Test
    fun `should unescape string literals`() {
        val testName = "e2e_string_unescaping"
        val templateBytes = DocxComparisonHelper.loadFixture("${testName}_in.docx")
        val expectedBytes = DocxComparisonHelper.loadFixture("${testName}_out.docx")

        val data = mapOf<String, Any>(
            "foobarVar" to "foo&bar",
        )

        val actualBytes = OfficeTemplate.fromBytes(templateBytes, options).render(data).bytes

        DocxComparisonHelper.assertDocxEquals(expectedBytes, actualBytes, testName)
    }

    @Test
    fun `should compare integer literals`() {
        val testName = "e2e_comparisons"
        val templateBytes = DocxComparisonHelper.loadFixture("${testName}_in.docx")
        val expectedBytes = DocxComparisonHelper.loadFixture("${testName}_out.docx")

        val actualBytes = OfficeTemplate.fromBytes(templateBytes, options).render(emptyMap()).bytes

        DocxComparisonHelper.assertDocxEquals(expectedBytes, actualBytes, testName)
    }

    @Test
    fun `should handle case statements`() {
        val testName = "e2e_case"
        val templateBytes = DocxComparisonHelper.loadFixture("${testName}_in.docx")
        val expectedBytes = DocxComparisonHelper.loadFixture("${testName}_out.docx")

        val data = mapOf<String, Any>(
            "x" to 25,
            "y" to -5,
            "z" to -10,
        )

        val actualBytes = OfficeTemplate.fromBytes(templateBytes, options).render(data).bytes

        DocxComparisonHelper.assertDocxEquals(expectedBytes, actualBytes, testName)
    }

    @Test
    fun `should handle do statements`() {
        val testName = "e2e_do"
        val templateBytes = DocxComparisonHelper.loadFixture("${testName}_in.docx")
        val expectedBytes = DocxComparisonHelper.loadFixture("${testName}_out.docx")

        val actualBytes = OfficeTemplate.fromBytes(templateBytes, options).render(emptyMap()).bytes

        DocxComparisonHelper.assertDocxEquals(expectedBytes, actualBytes, testName)
    }

    @Test
    fun `should insert raw XML`() {
        val testName = "e2e_raw_xml"
        val templateBytes = DocxComparisonHelper.loadFixture("${testName}_in.docx")
        val expectedBytes = DocxComparisonHelper.loadFixture("${testName}_out.docx")

        val actualBytes = OfficeTemplate.fromBytes(templateBytes, options).render(emptyMap()).bytes

        DocxComparisonHelper.assertDocxEquals(expectedBytes, actualBytes, testName)
    }

    @Test
    fun `should call $lineBreaksToTags`() {
        val testName = "e2e_line_breaks_as_tags"
        val templateBytes = DocxComparisonHelper.loadFixture("${testName}_in.docx")
        val expectedBytes = DocxComparisonHelper.loadFixture("${testName}_out.docx")

        val actualBytes = OfficeTemplate.fromBytes(templateBytes, options).render(emptyMap()).bytes

        DocxComparisonHelper.assertDocxEquals(expectedBytes, actualBytes, testName)
    }

    @Test
    fun `should add comment to error location`() {
        val testName = "e2e_error_comments"
        val templateBytes = DocxComparisonHelper.loadFixture("${testName}_in.docx")
        val expectedBytes = DocxComparisonHelper.loadFixture("${testName}_out.docx")

        val actualBytes = OfficeTemplate.fromBytes(templateBytes, options).render(emptyMap()).bytes

        DocxComparisonHelper.assertDocxEquals(expectedBytes, actualBytes, testName)
    }

    @Test
    fun `should add comment to error location with escape sequences`() {
        val testName = "e2e_error_comments_escape_sequences"
        val templateBytes = DocxComparisonHelper.loadFixture("${testName}_in.docx")
        val expectedBytes = DocxComparisonHelper.loadFixture("${testName}_out.docx")

        val actualBytes = OfficeTemplate.fromBytes(templateBytes, options).render(emptyMap()).bytes

        DocxComparisonHelper.assertDocxEquals(expectedBytes, actualBytes, testName)
    }

    @Test
    fun `should insert table of contents`() {
        val testName = "e2e_toc"
        val templateBytes = DocxComparisonHelper.loadFixture("${testName}_in.docx")
        val expectedBytes = DocxComparisonHelper.loadFixture("${testName}_out.docx")

        val actualBytes = OfficeTemplate.fromBytes(templateBytes, options).render(emptyMap()).bytes

        DocxComparisonHelper.assertDocxEquals(expectedBytes, actualBytes, testName)
    }

    @Test
    fun `should handle null coalescing`() {
        val testName = "e2e_null_coalescing"
        val templateBytes = DocxComparisonHelper.loadFixture("${testName}_in.docx")
        val expectedBytes = DocxComparisonHelper.loadFixture("${testName}_out.docx")

        val data = mapOf<String, Any>(
            "data" to mapOf(
                "a" to null,
                "b" to null,
                "c" to 42,
                "d" to null,
                "e" to 1,
            ),
        )
        val actualBytes = OfficeTemplate.fromBytes(templateBytes, options).render(data).bytes

        DocxComparisonHelper.assertDocxEquals(expectedBytes, actualBytes, testName)
    }

    @Test
    fun `should handle optional get`() {
        val testName = "e2e_optional_get"
        val templateBytes = DocxComparisonHelper.loadFixture("${testName}_in.docx")
        val expectedBytes = DocxComparisonHelper.loadFixture("${testName}_out.docx")

        val data = mapOf<String, Any>(
            "x" to Chain("1", Chain("2", Chain("3"))),
        )
        val actualBytes = OfficeTemplate.fromBytes(templateBytes, options).render(data).bytes

        DocxComparisonHelper.assertDocxEquals(expectedBytes, actualBytes, testName)
    }

    @Test
    fun `should render header and footer`() {
        val testName = "e2e_header_footer"
        val templateBytes = DocxComparisonHelper.loadFixture("${testName}_in.docx")
        val expectedBytes = DocxComparisonHelper.loadFixture("${testName}_out.docx")

        val data = mapOf<String, Any>(
            "header" to "The Header",
            "footer" to "The Footer",
        )
        val actualBytes = OfficeTemplate.fromBytes(templateBytes, options).render(data).bytes

        DocxComparisonHelper.assertDocxEquals(expectedBytes, actualBytes, testName)
    }

    @Test
    fun `should handle lambdas`() {
        val testName = "e2e_lambdas"
        val templateBytes = DocxComparisonHelper.loadFixture("${testName}_in.docx")
        val expectedBytes = DocxComparisonHelper.loadFixture("${testName}_out.docx")

        val data = mapOf<String, Any>(
            "items" to listOf(1, null, 2, null, 3, null, 4, null),
        )

        val actualBytes = OfficeTemplate.fromBytes(templateBytes, options).render(data).bytes

        DocxComparisonHelper.assertDocxEquals(expectedBytes, actualBytes, testName)
    }

    @Test
    fun `should handle comments`() {
        val testName = "e2e_comments"
        val templateBytes = DocxComparisonHelper.loadFixture("${testName}_in.docx")
        val expectedBytes = DocxComparisonHelper.loadFixture("${testName}_out.docx")

        val actualBytes = OfficeTemplate.fromBytes(templateBytes, options).render(emptyMap()).bytes

        DocxComparisonHelper.assertDocxEquals(expectedBytes, actualBytes, testName)
    }

    @Test
    fun `should handle links`() {
        val testName = "e2e_links"
        val templateBytes = DocxComparisonHelper.loadFixture("${testName}_in.docx")
        val expectedBytes = DocxComparisonHelper.loadFixture("${testName}_out.docx")

        val actualBytes = OfficeTemplate.fromBytes(templateBytes, options).render(emptyMap()).bytes

        DocxComparisonHelper.assertDocxEquals(expectedBytes, actualBytes, testName)
    }

    @Test
    fun `should handle invoice`() {
        val testName = "e2e_invoice"
        val templateBytes = DocxComparisonHelper.loadFixture("${testName}_in.docx")
        val expectedBytes = DocxComparisonHelper.loadFixture("${testName}_out.docx")

        val data = mapOf(
            "invoice" to Invoice(
                id = "20250815",
                serviceName = "Development services",
                freelancer = InvoicePerson(
                    name = "Fred Freelancer",
                    address = InvoiceAddress(
                        street = "Stubenring",
                        streetNumber = "1",
                        zipCode = "1234",
                        city = "Vienna",
                        country = "Austria",
                    ),
                    uid = "ATU12345678",
                    companyRegistrationNumber = "FN 123456",
                    iban = "AT123400000012345678",
                ),
                customer = InvoicePerson(
                    name = "Big Corp Ltd.",
                    address = InvoiceAddress(
                        street = "Tauentzienstraße",
                        streetNumber = "42",
                        zipCode = "54321",
                        city = "Berlin",
                        country = "Germany",
                    ),
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
        val actualBytes = OfficeTemplate.fromBytes(templateBytes, options).render(data).bytes

        DocxComparisonHelper.assertDocxEquals(expectedBytes, actualBytes, testName)
    }

    @Test
    fun `should handle hello world example`() {
        val testName = "e2e_hello"
        val templateBytes = DocxComparisonHelper.loadFixture("${testName}_in.docx")
        val expectedBytes = DocxComparisonHelper.loadFixture("${testName}_out.docx")

        val data = mapOf("name" to "world")

        val actualBytes = OfficeTemplate.fromBytes(templateBytes, options).render(data).bytes

        DocxComparisonHelper.assertDocxEquals(expectedBytes, actualBytes, testName)
    }

    @Test
    fun `should handle truthiness`() {
        val testName = "e2e_truthiness"
        val templateBytes = DocxComparisonHelper.loadFixture("${testName}_in.docx")
        val expectedBytes = DocxComparisonHelper.loadFixture("${testName}_out.docx")

        val data = mapOf(
            "list" to listOf("hello"),
            "emptyList" to emptyList(),
        )

        val actualBytes = OfficeTemplate.fromBytes(templateBytes, options).render(data).bytes

        DocxComparisonHelper.assertDocxEquals(expectedBytes, actualBytes, testName)
    }
}
