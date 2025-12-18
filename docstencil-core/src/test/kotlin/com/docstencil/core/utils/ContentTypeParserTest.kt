package com.docstencil.core.utils

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ContentTypeParserTest {
    @Test
    fun `should parse Override elements`() {
        val xml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
                <Override PartName="/word/document.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml"/>
                <Override PartName="/word/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.styles+xml"/>
            </Types>
        """.trimIndent()

        val result = ContentTypeParser.parse(xml)

        assertEquals(2, result.overrides.size)
        assertEquals("/word/document.xml", result.overrides[0].partName)
        assertTrue(result.overrides[0].contentType.contains("wordprocessingml.document.main"))
    }

    @Test
    fun `should parse Default elements`() {
        val xml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
                <Default Extension="xml" ContentType="application/xml"/>
                <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
            </Types>
        """.trimIndent()

        val result = ContentTypeParser.parse(xml)

        assertEquals(2, result.defaults.size)
        assertEquals("xml", result.defaults[0].extension)
        assertEquals("application/xml", result.defaults[0].contentType)
    }
}
