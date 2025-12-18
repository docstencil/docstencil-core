package com.docstencil.core.integration

import com.docstencil.core.api.OfficeTemplate
import com.docstencil.core.utils.XmlHelper
import com.docstencil.core.utils.ZipUtils
import org.junit.jupiter.api.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class NamespaceInjectionIntegrationTest {

    @Test
    fun `should inject default namespaces into rendered document`() {
        // Create a minimal DOCX with simple template
        val documentXml = """
            <w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">
                <w:body>
                    <w:p>
                        <w:r>
                            <w:t>{name}</w:t>
                        </w:r>
                    </w:p>
                </w:body>
            </w:document>
        """.trimIndent()

        val contentTypesXml = """
            <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
                <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
                <Default Extension="xml" ContentType="application/xml"/>
                <Override PartName="/word/document.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml"/>
            </Types>
        """.trimIndent()

        val relsXml = """
            <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
                <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="word/document.xml"/>
            </Relationships>
        """.trimIndent()

        val files = mapOf(
            "word/document.xml" to documentXml.toByteArray(Charsets.UTF_8),
            "[Content_Types].xml" to contentTypesXml.toByteArray(Charsets.UTF_8),
            "_rels/.rels" to relsXml.toByteArray(Charsets.UTF_8),
        )

        val zipBytes = ZipUtils.writeFile(files)

        // Render the template
        val template = OfficeTemplate.fromBytes(zipBytes)
        val result = template.render(mapOf("name" to "John Doe"))

        // Extract and verify the rendered document
        val renderedFiles = ZipUtils.readFile(result.bytes)
        val renderedDocXml = renderedFiles["word/document.xml"]?.toString(Charsets.UTF_8)

        assertNotNull(renderedDocXml, "Rendered document XML should not be null")

        // Parse the XML to verify namespaces were added
        val doc = XmlHelper.parseXml(renderedDocXml)
        val root = doc.documentElement

        // Verify essential namespaces are present
        assertTrue(root.hasAttribute("xmlns:w"), "Should have w namespace")
        assertTrue(root.hasAttribute("xmlns:a"), "Should have a namespace")
        assertTrue(root.hasAttribute("xmlns:pic"), "Should have pic namespace")
        assertTrue(root.hasAttribute("xmlns:wp"), "Should have wp namespace")

        // Verify namespace URIs
        assertTrue(
            root.getAttribute("xmlns:w") == "http://schemas.openxmlformats.org/wordprocessingml/2006/main",
            "w namespace should have correct URI",
        )
        assertTrue(
            root.getAttribute("xmlns:a") == "http://schemas.openxmlformats.org/drawingml/2006/main",
            "a namespace should have correct URI",
        )
        assertTrue(
            root.getAttribute("xmlns:pic") == "http://schemas.openxmlformats.org/drawingml/2006/picture",
            "pic namespace should have correct URI",
        )

        // Verify content was rendered correctly
        assertTrue(renderedDocXml.contains("John Doe"), "Should contain rendered template value")
    }

    @Test
    fun `should preserve existing namespaces in template`() {
        // Create a DOCX with some existing namespaces
        val documentXml = """
            <w:document
                xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main"
                xmlns:custom="http://example.com/custom">
                <w:body>
                    <w:p>
                        <w:r>
                            <w:t>Test</w:t>
                        </w:r>
                    </w:p>
                </w:body>
            </w:document>
        """.trimIndent()

        val contentTypesXml = """
            <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
                <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
                <Default Extension="xml" ContentType="application/xml"/>
                <Override PartName="/word/document.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml"/>
            </Types>
        """.trimIndent()

        val relsXml = """
            <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
                <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="word/document.xml"/>
            </Relationships>
        """.trimIndent()

        val files = mapOf(
            "word/document.xml" to documentXml.toByteArray(Charsets.UTF_8),
            "[Content_Types].xml" to contentTypesXml.toByteArray(Charsets.UTF_8),
            "_rels/.rels" to relsXml.toByteArray(Charsets.UTF_8),
        )

        val zipBytes = ZipUtils.writeFile(files)

        // Render the template
        val template = OfficeTemplate.fromBytes(zipBytes)
        val result = template.render(emptyMap())

        // Extract and verify the rendered document
        val renderedFiles = ZipUtils.readFile(result.bytes)
        val renderedDocXml = renderedFiles["word/document.xml"]?.toString(Charsets.UTF_8)

        assertNotNull(renderedDocXml, "Rendered document XML should not be null")

        val doc = XmlHelper.parseXml(renderedDocXml)
        val root = doc.documentElement

        // Verify custom namespace was preserved
        assertTrue(root.hasAttribute("xmlns:custom"), "Should preserve existing custom namespace")
        assertTrue(
            root.getAttribute("xmlns:custom") == "http://example.com/custom",
            "Custom namespace should have correct URI",
        )

        // Verify new namespaces were added
        assertTrue(root.hasAttribute("xmlns:a"), "Should add new a namespace")
        assertTrue(root.hasAttribute("xmlns:pic"), "Should add new pic namespace")
    }
}
