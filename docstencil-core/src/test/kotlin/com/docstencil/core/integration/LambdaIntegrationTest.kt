package com.docstencil.core.integration

import com.docstencil.core.api.OfficeTemplate
import com.docstencil.core.utils.ZipUtils
import kotlin.test.Test
import kotlin.test.assertTrue

class LambdaIntegrationTest {
    @Test
    fun `should render simple lambda with arithmetic`() {
        val templateXml =
            """<w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">
  <w:body>
    <w:p>
      <w:r>
        <w:t>{var add = (x, y) => x + y}{add(3, 4)}</w:t>
      </w:r>
    </w:p>
  </w:body>
</w:document>"""

        val templateBytes = createMinimalDocx(templateXml)
        val result = OfficeTemplate.fromBytes(templateBytes).render(emptyMap()).bytes

        val documentXml = extractDocumentXml(result)
        assertTrue(
            documentXml.contains(">7<") || documentXml.contains("7"),
            "Document should contain the result of add(3, 4) = 7",
        )
    }

    @Test
    fun `should support lambda with closure`() {
        val templateXml =
            """<w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">
  <w:body>
    <w:p>
      <w:r>
        <w:t>{var base = 100}{var adder = (x) => x + base}{adder(23)}</w:t>
      </w:r>
    </w:p>
  </w:body>
</w:document>"""

        val templateBytes = createMinimalDocx(templateXml)
        val result = OfficeTemplate.fromBytes(templateBytes).render(emptyMap()).bytes

        val documentXml = extractDocumentXml(result)
        assertTrue(
            documentXml.contains("123"),
            "Document should contain the result of adder(23) = 123 (closure captured base=100)",
        )
    }

    @Test
    fun `should support lambda callable multiple times`() {
        val templateXml =
            """<w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">
  <w:body>
    <w:p>
      <w:r>
        <w:t>{var double = (x) => x * 2}{double(5)} and {double(10)}</w:t>
      </w:r>
    </w:p>
  </w:body>
</w:document>"""

        val templateBytes = createMinimalDocx(templateXml)
        val result = OfficeTemplate.fromBytes(templateBytes).render(emptyMap()).bytes

        val documentXml = extractDocumentXml(result)
        assertTrue(
            documentXml.contains("10 and 20"),
            "Document should contain '10 and 20'",
        )
    }

    @Test
    fun `should support lambda in loop`() {
        val templateXml =
            """<w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">
  <w:body>
    <w:p>
      <w:r>
        <w:t>{var triple = (x) => x * 3}{for num in numbers}{triple(num)} {end}</w:t>
      </w:r>
    </w:p>
  </w:body>
</w:document>"""

        val templateBytes = createMinimalDocx(templateXml)
        val data = mapOf("numbers" to listOf(1, 2, 3, 4))
        val result = OfficeTemplate.fromBytes(templateBytes).render(data).bytes

        val documentXml = extractDocumentXml(result)
        assertTrue(
            documentXml.contains("3 6 9 12") || (documentXml.contains("3") && documentXml.contains("6") && documentXml.contains(
                "9",
            ) && documentXml.contains("12")),
            "Document should contain triple results: 3, 6, 9, 12",
        )
    }

    @Test
    fun `should support zero-parameter lambda`() {
        val templateXml =
            """<w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">
  <w:body>
    <w:p>
      <w:r>
        <w:t>{var getAnswer = () => 42}The answer is {getAnswer()}</w:t>
      </w:r>
    </w:p>
  </w:body>
</w:document>"""

        val templateBytes = createMinimalDocx(templateXml)
        val result = OfficeTemplate.fromBytes(templateBytes).render(emptyMap()).bytes

        val documentXml = extractDocumentXml(result)
        assertTrue(
            documentXml.contains("The answer is") && documentXml.contains("42"),
            "Document should contain 'The answer is 42'",
        )
    }

    @Test
    fun `should support lambda passed as argument`() {
        val templateXml =
            """<w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">
  <w:body>
    <w:p>
      <w:r>
        <w:t>{var apply = (fn, x) => fn(x)}{var double = (x) => x * 2}{apply(double, 7)}</w:t>
      </w:r>
    </w:p>
  </w:body>
</w:document>"""

        val templateBytes = createMinimalDocx(templateXml)
        val result = OfficeTemplate.fromBytes(templateBytes).render(emptyMap()).bytes

        val documentXml = extractDocumentXml(result)
        assertTrue(
            documentXml.contains("14"),
            "Document should contain apply(double, 7) = 14",
        )
    }

    @Test
    fun `should support lambda with data from template`() {
        val templateXml =
            """<w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">
  <w:body>
    <w:p>
      <w:r>
        <w:t>{var addTax = (price) => price + price * taxRate}Price with tax: {addTax(100)}</w:t>
      </w:r>
    </w:p>
  </w:body>
</w:document>"""

        val templateBytes = createMinimalDocx(templateXml)
        val data = mapOf("taxRate" to 0.2) // 20% tax
        val result = OfficeTemplate.fromBytes(templateBytes).render(data).bytes

        val documentXml = extractDocumentXml(result)
        assertTrue(
            documentXml.contains("Price with tax:") && (documentXml.contains("120") || documentXml.contains(
                "120.0",
            )),
            "Document should contain 'Price with tax: 120'",
        )
    }

    // Helper methods

    private fun createMinimalDocx(documentXml: String): ByteArray {
        val relsXml = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
</Relationships>"""

        val contentTypesXml = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
  <Override PartName="/word/document.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml"/>
</Types>"""

        val files = mapOf(
            "word/document.xml" to documentXml.toByteArray(),
            "word/_rels/document.xml.rels" to relsXml.toByteArray(),
            "[Content_Types].xml" to contentTypesXml.toByteArray(),
        )
        return ZipUtils.writeFile(files)
    }

    private fun extractDocumentXml(zipBytes: ByteArray): String {
        val files = ZipUtils.readFile(zipBytes)
        return files["word/document.xml"]!!.toString(Charsets.UTF_8)
    }
}
