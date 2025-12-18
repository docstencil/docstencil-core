package com.docstencil.core.utils

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class XmlNamespaceEnsurerTest {

    @Test
    fun `should add missing namespaces to root element`() {
        val xml = "<document><body>test</body></document>"
        val namespaces = mapOf(
            "w" to "http://schemas.openxmlformats.org/wordprocessingml/2006/main",
            "a" to "http://schemas.openxmlformats.org/drawingml/2006/main",
        )

        val result = XmlNamespaceEnsurer.ensure(xml, namespaces)

        assertTrue(result.contains("xmlns:w=\"http://schemas.openxmlformats.org/wordprocessingml/2006/main\""))
        assertTrue(result.contains("xmlns:a=\"http://schemas.openxmlformats.org/drawingml/2006/main\""))
        assertTrue(result.contains("<body>test</body>"))
    }

    @Test
    fun `should preserve existing namespaces`() {
        val xml =
            "<w:document xmlns:w=\"http://schemas.openxmlformats.org/wordprocessingml/2006/main\">" +
                    "<w:body>test</w:body></w:document>"
        val namespaces = mapOf(
            "w" to "http://schemas.openxmlformats.org/wordprocessingml/2006/main",
            "a" to "http://schemas.openxmlformats.org/drawingml/2006/main",
        )

        val result = XmlNamespaceEnsurer.ensure(xml, namespaces)

        // Should keep existing w namespace
        assertTrue(result.contains("xmlns:w=\"http://schemas.openxmlformats.org/wordprocessingml/2006/main\""))
        // Should add missing a namespace
        assertTrue(result.contains("xmlns:a=\"http://schemas.openxmlformats.org/drawingml/2006/main\""))
        // Count occurrences of xmlns:w - should only appear once
        val wCount = result.split("xmlns:w=").size - 1
        assertEquals(1, wCount, "xmlns:w should appear exactly once")
    }

    @Test
    fun `should handle empty namespace map`() {
        val xml = "<w:document><w:body>test</w:body></w:document>"
        val namespaces = emptyMap<String, String>()

        val result = XmlNamespaceEnsurer.ensure(xml, namespaces)

        // Should return unchanged XML
        assertEquals(xml, result)
    }

    @Test
    fun `should handle XML with no existing namespaces`() {
        val xml = "<root><child>content</child></root>"
        val namespaces = mapOf(
            "ns1" to "http://example.com/ns1",
            "ns2" to "http://example.com/ns2",
        )

        val result = XmlNamespaceEnsurer.ensure(xml, namespaces)

        assertTrue(result.contains("xmlns:ns1=\"http://example.com/ns1\""))
        assertTrue(result.contains("xmlns:ns2=\"http://example.com/ns2\""))
        assertTrue(result.contains("<child>content</child>"))
    }

    @Test
    fun `should handle XML with some existing namespaces`() {
        val xml =
            "<w:document xmlns:w=\"http://schemas.openxmlformats.org/wordprocessingml/2006/main\" " +
                    "xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\">" +
                    "<w:body>test</w:body></w:document>"
        val namespaces = mapOf(
            "w" to "http://schemas.openxmlformats.org/wordprocessingml/2006/main",
            "r" to "http://schemas.openxmlformats.org/officeDocument/2006/relationships",
            "a" to "http://schemas.openxmlformats.org/drawingml/2006/main",
            "pic" to "http://schemas.openxmlformats.org/drawingml/2006/picture",
        )

        val result = XmlNamespaceEnsurer.ensure(xml, namespaces)

        // Should preserve existing namespaces
        assertTrue(result.contains("xmlns:w=\"http://schemas.openxmlformats.org/wordprocessingml/2006/main\""))
        assertTrue(result.contains("xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\""))
        // Should add missing namespaces
        assertTrue(result.contains("xmlns:a=\"http://schemas.openxmlformats.org/drawingml/2006/main\""))
        assertTrue(result.contains("xmlns:pic=\"http://schemas.openxmlformats.org/drawingml/2006/picture\""))
        // Verify namespace declarations appear only once each
        assertEquals(1, result.split("xmlns:w=").size - 1)
        assertEquals(1, result.split("xmlns:r=").size - 1)
        assertEquals(1, result.split("xmlns:a=").size - 1)
        assertEquals(1, result.split("xmlns:pic=").size - 1)
    }

    @Test
    fun `should not add XML declaration`() {
        val xml = "<document><body>test</body></document>"
        val namespaces =
            mapOf("w" to "http://schemas.openxmlformats.org/wordprocessingml/2006/main")

        val result = XmlNamespaceEnsurer.ensure(xml, namespaces)

        // Should not start with XML declaration
        assertTrue(!result.startsWith("<?xml"), "Result should not contain XML declaration")
    }
}
