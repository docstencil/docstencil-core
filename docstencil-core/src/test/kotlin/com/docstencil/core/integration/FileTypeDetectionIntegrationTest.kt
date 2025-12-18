package com.docstencil.core.integration

import com.docstencil.core.api.OfficeTemplate
import com.docstencil.core.utils.ZipUtils
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Verifies that:
 * - Auto-discovery of target files works correctly.
 * - Relationship validation is applied.
 * - Auxiliary files (properties, settings) are included.
 */
class FileTypeDetectionIntegrationTest {
    private fun createDocx(
        includeHeaders: Boolean = false,
        includeFooters: Boolean = false,
        includeProperties: Boolean = false,
        includeFootnotes: Boolean = false,
        includeComments: Boolean = false,
    ): ByteArray {
        val documentXml = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">
  <w:body>
    <w:p>
      <w:r>
        <w:t>Test {placeholder}</w:t>
      </w:r>
    </w:p>
  </w:body>
</w:document>"""

        val files = mutableMapOf<String, ByteArray>()

        // Core files
        files["word/document.xml"] = documentXml.toByteArray(Charsets.UTF_8)

        // Build Content_Types.xml
        val contentTypeOverrides = mutableListOf(
            """<Override PartName="/word/document.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml"/>""",
        )

        if (includeHeaders) {
            files["word/header1.xml"] = """<?xml version="1.0"?>
<w:hdr xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">
  <w:p><w:r><w:t>Header {title}</w:t></w:r></w:p>
</w:hdr>""".toByteArray(Charsets.UTF_8)
            contentTypeOverrides.add(
                """<Override PartName="/word/header1.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.header+xml"/>""",
            )
        }

        if (includeFooters) {
            files["word/footer1.xml"] = """<?xml version="1.0"?>
<w:ftr xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">
  <w:p><w:r><w:t>Footer {page}</w:t></w:r></w:p>
</w:ftr>""".toByteArray(Charsets.UTF_8)
            contentTypeOverrides.add(
                """<Override PartName="/word/footer1.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.footer+xml"/>""",
            )
        }

        if (includeProperties) {
            files["docProps/core.xml"] = """<?xml version="1.0"?>
<cp:coreProperties xmlns:cp="http://schemas.openxmlformats.org/package/2006/metadata/core-properties">
  <dc:title xmlns:dc="http://purl.org/dc/elements/1.1/">{docTitle}</dc:title>
  <dc:creator xmlns:dc="http://purl.org/dc/elements/1.1/">{author}</dc:creator>
</cp:coreProperties>""".toByteArray(Charsets.UTF_8)
            contentTypeOverrides.add(
                """<Override PartName="/docProps/core.xml" ContentType="application/vnd.openxmlformats-package.core-properties+xml"/>""",
            )

            files["docProps/app.xml"] = """<?xml version="1.0"?>
<Properties xmlns="http://schemas.openxmlformats.org/officeDocument/2006/extended-properties">
  <Application>Microsoft Word</Application>
  <Company>{company}</Company>
</Properties>""".toByteArray(Charsets.UTF_8)
            contentTypeOverrides.add(
                """<Override PartName="/docProps/app.xml" ContentType="application/vnd.openxmlformats-officedocument.extended-properties+xml"/>""",
            )
        }

        if (includeFootnotes) {
            files["word/footnotes.xml"] = """<?xml version="1.0"?>
<w:footnotes xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">
  <w:footnote w:id="1">
    <w:p><w:r><w:t>Footnote {noteText}</w:t></w:r></w:p>
  </w:footnote>
</w:footnotes>""".toByteArray(Charsets.UTF_8)
            contentTypeOverrides.add(
                """<Override PartName="/word/footnotes.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.footnotes+xml"/>""",
            )
        }

        if (includeComments) {
            files["word/comments.xml"] = """<?xml version="1.0"?>
<w:comments xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">
  <w:comment w:id="1">
    <w:p><w:r><w:t>Comment {commentText}</w:t></w:r></w:p>
  </w:comment>
</w:comments>""".toByteArray(Charsets.UTF_8)
            contentTypeOverrides.add(
                """<Override PartName="/word/comments.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.comments+xml"/>""",
            )
        }

        val contentTypesXml = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
  <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
  <Default Extension="xml" ContentType="application/xml"/>
  ${contentTypeOverrides.joinToString("\n  ")}
</Types>"""

        files["[Content_Types].xml"] = contentTypesXml.toByteArray(Charsets.UTF_8)

        val relsXml = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="word/document.xml"/>
</Relationships>"""

        files["_rels/.rels"] = relsXml.toByteArray(Charsets.UTF_8)

        return ZipUtils.writeFile(files)
    }

    @Test
    fun `should process basic document with main file only`() {
        val docxBytes = createDocx()
        val template = OfficeTemplate.fromBytes(docxBytes)

        val result = template.render(mapOf("placeholder" to "Hello World")).bytes

        val outputFiles = ZipUtils.readFile(result)
        val documentXml = outputFiles["word/document.xml"]?.toString(Charsets.UTF_8)

        assertNotNull(documentXml)
        assertTrue(documentXml.contains("Hello World"))
    }

    @Test
    fun `should process document with headers and footers`() {
        val docxBytes = createDocx(includeHeaders = true, includeFooters = true)
        val template = OfficeTemplate.fromBytes(docxBytes)

        val data = mapOf(
            "placeholder" to "Main Content",
            "title" to "Document Title",
            "page" to "1",
        )

        val result = template.render(data).bytes
        val outputFiles = ZipUtils.readFile(result)

        val documentXml = outputFiles["word/document.xml"]?.toString(Charsets.UTF_8)
        val headerXml = outputFiles["word/header1.xml"]?.toString(Charsets.UTF_8)
        val footerXml = outputFiles["word/footer1.xml"]?.toString(Charsets.UTF_8)

        assertNotNull(documentXml)
        assertNotNull(headerXml)
        assertNotNull(footerXml)

        assertTrue(documentXml.contains("Main Content"))
        assertTrue(headerXml.contains("Document Title"))
        assertTrue(footerXml.contains("1"))
    }

    @Test
    fun `should process document properties (auxiliary files)`() {
        val docxBytes = createDocx(includeProperties = true)
        val template = OfficeTemplate.fromBytes(docxBytes)

        val data = mapOf(
            "placeholder" to "Content",
            "docTitle" to "My Document",
            "author" to "John Doe",
            "company" to "ACME Corp",
        )

        val result = template.render(data).bytes
        val outputFiles = ZipUtils.readFile(result)

        val coreXml = outputFiles["docProps/core.xml"]?.toString(Charsets.UTF_8)
        val appXml = outputFiles["docProps/app.xml"]?.toString(Charsets.UTF_8)

        assertNotNull(coreXml)
        assertNotNull(appXml)

        assertTrue(coreXml.contains("My Document"))
        assertTrue(coreXml.contains("John Doe"))
        assertTrue(appXml.contains("ACME Corp"))
    }

    @Test
    fun `should process footnotes and comments`() {
        val docxBytes = createDocx(includeFootnotes = true, includeComments = true)
        val template = OfficeTemplate.fromBytes(docxBytes)

        val data = mapOf(
            "placeholder" to "Main",
            "noteText" to "This is a footnote",
            "commentText" to "This is a comment",
        )

        val result = template.render(data).bytes
        val outputFiles = ZipUtils.readFile(result)

        val footnotesXml = outputFiles["word/footnotes.xml"]?.toString(Charsets.UTF_8)
        val commentsXml = outputFiles["word/comments.xml"]?.toString(Charsets.UTF_8)

        assertNotNull(footnotesXml)
        assertNotNull(commentsXml)

        assertTrue(footnotesXml.contains("This is a footnote"))
        assertTrue(commentsXml.contains("This is a comment"))
    }

    @Test
    fun `should process all file types in complex document`() {
        val docxBytes = createDocx(
            includeHeaders = true,
            includeFooters = true,
            includeProperties = true,
            includeFootnotes = true,
            includeComments = true,
        )

        val template = OfficeTemplate.fromBytes(docxBytes)

        val data = mapOf(
            "placeholder" to "Main Content",
            "title" to "Header Text",
            "page" to "1",
            "docTitle" to "Complex Document",
            "author" to "Test Author",
            "company" to "Test Company",
            "noteText" to "Footnote Content",
            "commentText" to "Comment Content",
        )

        val result = template.render(data).bytes
        val outputFiles = ZipUtils.readFile(result)

        assertNotNull(outputFiles["word/document.xml"])
        assertNotNull(outputFiles["word/header1.xml"])
        assertNotNull(outputFiles["word/footer1.xml"])
        assertNotNull(outputFiles["docProps/core.xml"])
        assertNotNull(outputFiles["docProps/app.xml"])
        assertNotNull(outputFiles["word/footnotes.xml"])
        assertNotNull(outputFiles["word/comments.xml"])

        val documentXml = outputFiles["word/document.xml"]!!.toString(Charsets.UTF_8)
        assertTrue(documentXml.contains("Main Content"))
        assertTrue(!documentXml.contains("{placeholder}"))
    }
}
