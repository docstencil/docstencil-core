package com.docstencil.core.integration

import com.docstencil.core.api.OfficeTemplate
import com.docstencil.core.api.OfficeTemplateOptions
import com.docstencil.core.modules.hyperlink.HyperlinkModule
import com.docstencil.core.utils.ZipUtils
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue


class HyperlinkModuleIntegrationTest {
    private val options = OfficeTemplateOptions().addModule(HyperlinkModule())

    @Test
    fun `should insert single hyperlink`() {
        val templateXml =
            $$"""<w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
  <w:body>
    <w:p>
      <w:r>
        <w:t>Click {rewrite $link(url)}here{end} for more info.</w:t>
      </w:r>
    </w:p>
  </w:body>
</w:document>"""

        val templateBytes = createMinimalDocx(templateXml)
        val data = mapOf("url" to "https://example.com")

        val result = OfficeTemplate.fromBytes(templateBytes, options).render(data).bytes

        val docXml = extractDocumentXml(result)
        assertTrue(
            docXml.contains("<w:hyperlink"),
            "Document should contain hyperlink element",
        )
        assertTrue(
            docXml.contains("r:id=\"rId"),
            "Hyperlink should have relationship ID",
        )
        assertTrue(
            docXml.contains("<w:rStyle w:val=\"Hyperlink\"/>"),
            "Hyperlink should have Hyperlink style",
        )

        val relsXml = extractRelsXml(result)
        assertTrue(
            relsXml.contains("https://example.com"),
            "Relationships should contain the URL",
        )
        assertTrue(
            relsXml.contains("TargetMode=\"External\""),
            "Relationship should have TargetMode=External",
        )
        assertTrue(
            relsXml.contains("Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/hyperlink\""),
            "Relationship should have hyperlink type",
        )
    }

    @Test
    fun `should deduplicate same URL`() {
        val templateXml =
            $$"""<w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
  <w:body>
    <w:p>
      <w:r>
        <w:t>{rewrite $link(url)}First{end} and {rewrite $link(url)}Second{end}.</w:t>
      </w:r>
    </w:p>
  </w:body>
</w:document>"""

        val templateBytes = createMinimalDocx(templateXml)
        val data = mapOf("url" to "https://example.com")

        val result = OfficeTemplate.fromBytes(templateBytes, options).render(data).bytes

        val relsXml = extractRelsXml(result)
        val urlCount = relsXml.split("https://example.com").size - 1
        assertEquals(
            1,
            urlCount,
            "Same URL should only appear once in relationships (deduplication)",
        )

        val docXml = extractDocumentXml(result)
        val hyperlinkCount = docXml.split("<w:hyperlink").size - 1
        assertEquals(
            2,
            hyperlinkCount,
            "Should have 2 hyperlink elements in document",
        )
    }

    @Test
    fun `should handle multiple different URLs`() {
        val templateXml =
            $$"""<w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
  <w:body>
    <w:p>
      <w:r>
        <w:t>{rewrite $link(url1)}First{end} and {rewrite $link(url2)}Second{end}.</w:t>
      </w:r>
    </w:p>
  </w:body>
</w:document>"""

        val templateBytes = createMinimalDocx(templateXml)
        val data = mapOf(
            "url1" to "https://example1.com",
            "url2" to "https://example2.com",
        )

        val result = OfficeTemplate.fromBytes(templateBytes, options).render(data).bytes

        val relsXml = extractRelsXml(result)
        assertTrue(
            relsXml.contains("https://example1.com"),
            "Relationships should contain first URL",
        )
        assertTrue(
            relsXml.contains("https://example2.com"),
            "Relationships should contain second URL",
        )
    }

    @Test
    fun `should insert hyperlinks in loops`() {
        val templateXml =
            $$"""<w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
  <w:body>
    <w:p>
      <w:r>
        <w:t>{for link in links}{rewrite $link(link.url)}{link.text}{end} {end}</w:t>
      </w:r>
    </w:p>
  </w:body>
</w:document>"""

        val templateBytes = createMinimalDocx(templateXml)
        val data = mapOf(
            "links" to listOf(
                mapOf("text" to "Google", "url" to "https://google.com"),
                mapOf("text" to "GitHub", "url" to "https://github.com"),
                mapOf("text" to "Example", "url" to "https://example.com"),
            ),
        )

        val result = OfficeTemplate.fromBytes(templateBytes, options).render(data).bytes

        val docXml = extractDocumentXml(result)
        val hyperlinkCount = docXml.split("<w:hyperlink").size - 1
        assertEquals(
            3,
            hyperlinkCount,
            "Should have 3 hyperlink elements",
        )

        assertTrue(docXml.contains("Google"), "Should contain Google text")
        assertTrue(docXml.contains("GitHub"), "Should contain GitHub text")
        assertTrue(docXml.contains("Example"), "Should contain Example text")

        val relsXml = extractRelsXml(result)
        assertTrue(relsXml.contains("https://google.com"), "Should contain Google URL")
        assertTrue(relsXml.contains("https://github.com"), "Should contain GitHub URL")
        assertTrue(relsXml.contains("https://example.com"), "Should contain Example URL")
    }

    @Test
    fun `should not add relationships when no hyperlinks used`() {
        val templateXml =
            """<w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">
  <w:body>
    <w:p>
      <w:r>
        <w:t>Document without hyperlinks.</w:t>
      </w:r>
    </w:p>
  </w:body>
</w:document>"""

        val templateBytes = createMinimalDocx(templateXml)
        val data = emptyMap<String, Any>()

        val result = OfficeTemplate.fromBytes(templateBytes, options).render(data).bytes

        val relsXml = extractRelsXml(result)
        assertTrue(
            !relsXml.contains("hyperlink"),
            "No hyperlink relationships should be added when no hyperlinks are used",
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

    private fun extractRelsXml(zipBytes: ByteArray): String {
        val files = ZipUtils.readFile(zipBytes)
        return files["word/_rels/document.xml.rels"]?.toString(Charsets.UTF_8) ?: ""
    }
}
