package com.docstencil.core.integration

import com.docstencil.core.api.OfficeFileTemplate
import com.docstencil.core.api.OfficeTemplateOptions
import com.docstencil.core.config.FileTypeConfig
import com.docstencil.core.helper.DocxComparisonHelper
import com.docstencil.core.render.Globals
import com.docstencil.core.utils.DocUtils
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class IntegrationTest {
    private fun createFileTemplate(
        xmlContent: String,
        options: OfficeTemplateOptions = OfficeTemplateOptions(),
        fileTypeConfig: FileTypeConfig = FileTypeConfig.docx(),
        delimiters: OfficeTemplateOptions.Delimiters = OfficeTemplateOptions.Delimiters("{", "}"),
    ): OfficeFileTemplate {
        return OfficeFileTemplate(
            xmlContent = xmlContent,
            globals = Globals.builder(options, fileTypeConfig, emptyList()).build(),
            fileTypeConfig = fileTypeConfig,
            filePath = "word/document.xml",
            delimiters = delimiters,
            stripInvalidXmlChars = false,
        )
    }

    @Test
    fun `render should replace simple placeholders`() {
        val xml = DocxComparisonHelper.loadStringFixture("integration_simple_placeholders_in.xml")

        val template = createFileTemplate(xml)
        val data = mapOf(
            "firstName" to "John",
            "lastName" to "Doe & Doe",
        )

        val result = template.render(data)

        assertTrue(result.contains("John"), "Output should contain 'John'")
        assertTrue(
            result.contains(DocUtils.escapeXmlString("Doe & Doe")),
            "Output should contain '${DocUtils.escapeXmlString("Doe & Doe")}'",
        )
        assertFalse(
            result.contains("{firstName}"),
            "Output should not contain '{firstName}' placeholder",
        )
        assertFalse(
            result.contains("{lastName}"),
            "Output should not contain '{lastName}' placeholder",
        )
    }

    @Test
    fun `render should handle for loop with dot notation`() {
        val xml = DocxComparisonHelper.loadStringFixture("integration_for_loop_dot_notation_in.xml")

        val template = createFileTemplate(xml)
        val data = mapOf(
            "people" to listOf(
                mapOf("firstName" to "John", "lastName" to "Doe"),
                mapOf("firstName" to "Mary", "lastName" to "Jane"),
            ),
        )

        val result = template.render(data)

        assertTrue(result.contains("John"), "Output should contain 'John'")
        assertTrue(result.contains("Doe"), "Output should contain 'Doe'")
        assertTrue(result.contains("Mary"), "Output should contain 'Mary'")
        assertTrue(result.contains("Jane"), "Output should contain 'Jane'")
        assertFalse(result.contains("{for"), "Output should not contain loop markers")
        assertFalse(result.contains("{end}"), "Output should not contain '{end}' marker")
    }

    @Test
    fun `render should handle case expression with multiple branches`() {
        val xml = """
            <w:document>
                <w:body>
                    <w:p>
                        <w:r>
                            <w:t>Status: {case when status == "error" then "red" when status == "warning" then "yellow" else "green" end}</w:t>
                        </w:r>
                    </w:p>
                </w:body>
            </w:document>
        """.trimIndent()

        val template = createFileTemplate(xml)
        val data = mapOf("status" to "warning")

        val result = template.render(data)

        assertTrue(result.contains("Status: yellow"), "Output should contain 'Status: yellow'")
        assertFalse(result.contains("{case"), "Output should not contain case expression markers")
        assertFalse(result.contains("when"), "Output should not contain 'when' keyword")
        assertFalse(result.contains("end}"), "Output should not contain 'end}' marker")
    }

    @Test
    fun `render should handle case expression with else branch`() {
        val xml = """
            <w:document>
                <w:body>
                    <w:p>
                        <w:r>
                            <w:t>Result: {case when x > 10 then "high" else "low" end}</w:t>
                        </w:r>
                    </w:p>
                </w:body>
            </w:document>
        """.trimIndent()

        val template = createFileTemplate(xml)
        val data = mapOf("x" to 5)

        val result = template.render(data)

        assertTrue(result.contains("Result: low"), "Output should contain 'Result: low'")
        assertFalse(result.contains("{case"), "Output should not contain case expression markers")
    }

    @Test
    fun `render should handle case expression with no else returning null`() {
        val xml = """
            <w:document>
                <w:body>
                    <w:p>
                        <w:r>
                            <w:t>Value: {case when x > 10 then "match" end}</w:t>
                        </w:r>
                    </w:p>
                </w:body>
            </w:document>
        """.trimIndent()

        val template = createFileTemplate(xml)
        val data = mapOf("x" to 5)

        val result = template.render(data)

        assertTrue(result.contains("Value: "), "Output should contain 'Value: ' prefix")
        assertFalse(
            result.contains("match"),
            "Output should not contain 'match' when condition is false",
        )
        assertFalse(result.contains("{case"), "Output should not contain case expression markers")
    }

    @Test
    fun `render should handle type casting functions`() {
        val xml = $$"""
            <w:document>
                <w:body>
                    <w:p>
                        <w:r>
                            <w:t>Without cast: {intVal == doubleVal}</w:t>
                        </w:r>
                    </w:p>
                    <w:p>
                        <w:r>
                            <w:t>With cast: {$asInt(intVal) == $asInt(doubleVal)}</w:t>
                        </w:r>
                    </w:p>
                    <w:p>
                        <w:r>
                            <w:t>Casters: {$asByte(num)},{$asShort(num)},{$asInt(num)},{$asLong(num)},{$asFloat(num)},{$asDouble(num)}</w:t>
                        </w:r>
                    </w:p>
                </w:body>
            </w:document>
        """.trimIndent()

        val template = createFileTemplate(xml)
        val data = mapOf(
            "intVal" to 2,
            "doubleVal" to 2.0,
            "num" to 42.5,
        )

        val result = template.render(data)

        assertTrue(result.contains("Without cast: false"))
        assertTrue(result.contains("With cast: true"))
        assertTrue(result.contains("Casters: 42,42,42,42,42.5,42.5"))
    }

    @Test
    fun `render should handle $enumerate function`() {
        val xml = $$"""
            <w:document>
                <w:body>
                    <w:p>
                        <w:r>
                            <w:t>{for item in $enumerate(items)}{item.index}:{item.value}:{item.isFirst}:{item.isLast}:{item.isEven},{end}</w:t>
                        </w:r>
                    </w:p>
                </w:body>
            </w:document>
        """.trimIndent()

        val template = createFileTemplate(xml)
        val data = mapOf("items" to listOf("a", "b", "c"))

        val result = template.render(data)

        assertTrue(result.contains("0:a:true:false:true,"))
        assertTrue(result.contains("1:b:false:false:false,"))
        assertTrue(result.contains("2:c:false:true:true,"))
    }

    @Test
    fun `render should handle $filterNotNull function`() {
        val xml = $$"""
            <w:document>
                <w:body>
                    <w:p>
                        <w:r>
                            <w:t>{for item in $filterNotNull(items)}{item},{end}</w:t>
                        </w:r>
                    </w:p>
                </w:body>
            </w:document>
        """.trimIndent()

        val template = createFileTemplate(xml)
        val data = mapOf("items" to listOf(1, null, 2, null, 3))

        val result = template.render(data)

        assertTrue(result.contains("1,2,3,"))
        assertFalse(result.contains("null"))
    }

    @Test
    fun `render should handle $formatDate function`() {
        val xml = $$"""
            <w:document>
                <w:body>
                    <w:p>
                        <w:r>
                            <w:t>Date: {$formatDate($now(), "yyyy-MM-dd")}</w:t>
                        </w:r>
                    </w:p>
                </w:body>
            </w:document>
        """.trimIndent()

        val template = createFileTemplate(xml)
        val data = mapOf<String, Any>()

        val result = template.render(data)

        assertTrue(Regex("Date: \\d{4}-\\d{2}-\\d{2}").containsMatchIn(result))
    }

    @Test
    fun `render should handle $formatNumber function`() {
        val xml = $$"""
            <w:document>
                <w:body>
                    <w:p>
                        <w:r>
                            <w:t>Number: {$formatNumber(num, "#,##0.00")}</w:t>
                        </w:r>
                    </w:p>
                </w:body>
            </w:document>
        """.trimIndent()

        val template = createFileTemplate(xml)
        val data = mapOf("num" to 1234.5)

        val result = template.render(data)

        assertTrue(result.contains("Number: 1,234.50") || result.contains("Number: 1.234,50"))
    }

    @Test
    fun `render should handle $join function`() {
        val xml = $$"""
            <w:document>
                <w:body>
                    <w:p>
                        <w:r>
                            <w:t>Fruits: {$join(fruits, ", ")}</w:t>
                        </w:r>
                    </w:p>
                </w:body>
            </w:document>
        """.trimIndent()

        val template = createFileTemplate(xml)
        val data = mapOf("fruits" to listOf("apple", "banana", "cherry"))

        val result = template.render(data)

        assertTrue(result.contains("Fruits: apple, banana, cherry"))
    }

    @Test
    fun `render should handle $range function`() {
        val xml = $$"""
            <w:document>
                <w:body>
                    <w:p>
                        <w:r>
                            <w:t>Range: {for i in $range(1, 4)}{i},{end}</w:t>
                        </w:r>
                    </w:p>
                </w:body>
            </w:document>
        """.trimIndent()

        val template = createFileTemplate(xml)
        val data = mapOf<String, Any>()

        val result = template.render(data)

        assertTrue(result.contains("Range: 1,2,3,"))
    }

    @Test
    fun `render should handle $reverse function`() {
        val xml = $$"""
            <w:document>
                <w:body>
                    <w:p>
                        <w:r>
                            <w:t>Reversed: {for i in $reverse(nums)}{i},{end}</w:t>
                        </w:r>
                    </w:p>
                </w:body>
            </w:document>
        """.trimIndent()

        val template = createFileTemplate(xml)
        val data = mapOf("nums" to listOf(1, 2, 3))

        val result = template.render(data)

        assertTrue(result.contains("Reversed: 3,2,1,"))
    }

    @Test
    fun `render should handle optional chaining with null object`() {
        val xml = """
            <w:document>
                <w:body>
                    <w:p>
                        <w:r>
                            <w:t>Name: {user.?name}</w:t>
                        </w:r>
                    </w:p>
                </w:body>
            </w:document>
        """.trimIndent()

        val template = createFileTemplate(xml)
        val data = mapOf<String, Any?>("user" to null)

        @Suppress("UNCHECKED_CAST")
        val result = template.render(data as Map<String, Any>)

        assertTrue(result.contains("Name: "), "Output should contain 'Name: '")
        assertFalse(result.contains("{user.?name}"), "Output should not contain placeholder")
    }

    @Test
    fun `render should handle optional chaining with non-null object`() {
        val xml = """
            <w:document>
                <w:body>
                    <w:p>
                        <w:r>
                            <w:t>Name: {user.?name}</w:t>
                        </w:r>
                    </w:p>
                </w:body>
            </w:document>
        """.trimIndent()

        val template = createFileTemplate(xml)
        val data = mapOf("user" to mapOf("name" to "Alice"))

        val result = template.render(data)

        assertTrue(result.contains("Name: Alice"), "Output should contain 'Name: Alice'")
    }

    @Test
    fun `render should handle null coalescing with null value`() {
        val xml = """
            <w:document>
                <w:body>
                    <w:p>
                        <w:r>
                            <w:t>Name: {firstName ?? lastName ?? "Unknown"}</w:t>
                        </w:r>
                    </w:p>
                </w:body>
            </w:document>
        """.trimIndent()

        val template = createFileTemplate(xml)
        val data = mapOf<String, Any?>("firstName" to null, "lastName" to null)

        @Suppress("UNCHECKED_CAST")
        val result = template.render(data as Map<String, Any>)

        assertTrue(result.contains("Name: Unknown"), "Output should contain 'Name: Unknown'")
    }

    @Test
    fun `render should handle null coalescing with first non-null value`() {
        val xml = """
            <w:document>
                <w:body>
                    <w:p>
                        <w:r>
                            <w:t>Name: {firstName ?? lastName ?? "Unknown"}</w:t>
                        </w:r>
                    </w:p>
                </w:body>
            </w:document>
        """.trimIndent()

        val template = createFileTemplate(xml)
        val data = mapOf<String, Any?>("firstName" to null, "lastName" to "Smith")

        @Suppress("UNCHECKED_CAST")
        val result = template.render(data as Map<String, Any>)

        assertTrue(result.contains("Name: Smith"), "Output should contain 'Name: Smith'")
    }

    @Test
    fun `render should handle null coalescing with zero value`() {
        val xml = """
            <w:document>
                <w:body>
                    <w:p>
                        <w:r>
                            <w:t>Value: {count ?? 10}</w:t>
                        </w:r>
                    </w:p>
                </w:body>
            </w:document>
        """.trimIndent()

        val template = createFileTemplate(xml)
        val data = mapOf("count" to 0)

        val result = template.render(data)

        assertTrue(
            result.contains("Value: 0"),
            "Output should contain 'Value: 0' (zero is not null)",
        )
    }
}
