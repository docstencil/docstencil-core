package com.docstencil.core.utils

import com.docstencil.core.config.FileTypeConfig
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RelationshipParserTest {
    private val parser = RelationshipParser()

    @Test
    fun `should parse Relationship elements`() {
        val xml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
                <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="word/document.xml"/>
                <Relationship Id="rId2" Type="http://schemas.openxmlformats.org/package/2006/relationships/metadata/core-properties" Target="docProps/core.xml"/>
            </Relationships>
        """.trimIndent()

        val relationships = parser.parse(xml)

        assertEquals(2, relationships.size)
        assertEquals("rId1", relationships[0].id)
        assertEquals("word/document.xml", relationships[0].target)
        assertTrue(relationships[0].type.contains("officeDocument"))
    }

    @Test
    fun `should handle empty relationships`() {
        val xml = """
            <Relationships>
            </Relationships>
        """.trimIndent()

        val relationships = parser.parse(xml)

        assertEquals(0, relationships.size)
    }

    @Test
    fun `getMainDocumentPath with config should use fallback when rels missing`() {
        val files = mapOf<String, ByteArray>(
            "word/document.xml" to ByteArray(0),
        )
        val config = FileTypeConfig.docx().fallbackPaths

        val parser = RelationshipParser()
        val result = parser.getMainDocumentPath(files, config)

        assertEquals("word/document.xml", result)
    }

    @Test
    fun `getMainDocumentPath with config should prefer rels over fallback`() {
        val relsXml = """
            <?xml version="1.0"?>
            <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
                <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="custom/main.xml"/>
            </Relationships>
        """.trimIndent()

        val files = mapOf(
            "_rels/.rels" to relsXml.toByteArray(),
            "word/document.xml" to ByteArray(0),
            "custom/main.xml" to ByteArray(0),
        )
        val config = FileTypeConfig.docx().fallbackPaths

        val parser = RelationshipParser()
        val result = parser.getMainDocumentPath(files, config)

        assertEquals("custom/main.xml", result)  // Should use rels, not fallback
    }
}
