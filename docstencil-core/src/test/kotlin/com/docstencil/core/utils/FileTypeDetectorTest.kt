package com.docstencil.core.utils

import com.docstencil.core.api.OfficeFileType
import com.docstencil.core.config.FileTypeConfig
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class FileTypeDetectorTest {
    @Test
    fun `should detect DOCX with main document`() {
        val contentTypesXml = """
            <?xml version="1.0"?>
            <Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
                <Override PartName="/word/document.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml"/>
            </Types>
        """.trimIndent()

        val files = mapOf(
            "[Content_Types].xml" to contentTypesXml.toByteArray(),
            "word/document.xml" to ByteArray(0),
        )

        val result = FileTypeDetector.detect(files, FileTypeConfig.docx().fallbackPaths)

        assertEquals(OfficeFileType.DOCX, result.fileType)
        assertTrue(result.targetFiles.contains("word/document.xml"))
    }

    @Test
    fun `should include auxiliary files like properties and settings`() {
        val contentTypesXml = """
            <?xml version="1.0"?>
            <Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
                <Override PartName="/word/document.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml"/>
                <Override PartName="/docProps/core.xml" ContentType="application/vnd.openxmlformats-package.core-properties+xml"/>
                <Override PartName="/docProps/app.xml" ContentType="application/vnd.openxmlformats-officedocument.extended-properties+xml"/>
                <Override PartName="/word/settings.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.settings+xml"/>
            </Types>
        """.trimIndent()

        val files = mapOf(
            "[Content_Types].xml" to contentTypesXml.toByteArray(),
            "word/document.xml" to ByteArray(0),
            "docProps/core.xml" to ByteArray(0),
            "docProps/app.xml" to ByteArray(0),
            "word/settings.xml" to ByteArray(0),
        )

        val result = FileTypeDetector.detect(files, FileTypeConfig.docx().fallbackPaths)

        assertTrue(result.targetFiles.contains("word/document.xml"))
        assertTrue(result.targetFiles.contains("docProps/core.xml"))
        assertTrue(result.targetFiles.contains("docProps/app.xml"))
        assertTrue(result.targetFiles.contains("word/settings.xml"))
    }

    @Test
    fun `should validate relationship types and skip files with wrong type`() {
        val contentTypesXml = """
            <?xml version="1.0"?>
            <Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
                <Override PartName="/word/document.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml"/>
                <Override PartName="/customXml/item1.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml"/>
            </Types>
        """.trimIndent()

        val relsXml = """
            <?xml version="1.0"?>
            <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
                <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="word/document.xml"/>
                <Relationship Id="rId2" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/customXml" Target="customXml/item1.xml"/>
            </Relationships>
        """.trimIndent()

        val files = mapOf(
            "[Content_Types].xml" to contentTypesXml.toByteArray(),
            "_rels/.rels" to relsXml.toByteArray(),
            "word/document.xml" to ByteArray(0),
            "customXml/item1.xml" to ByteArray(0),
        )

        val result = FileTypeDetector.detect(files, FileTypeConfig.docx().fallbackPaths)

        assertTrue(result.targetFiles.contains("word/document.xml"))
        assertTrue(!result.targetFiles.contains("customXml/item1.xml"))
    }

    @Test
    fun `should fall back to hardcoded patterns when content types missing`() {
        val files = mapOf(
            "word/document.xml" to ByteArray(0),
            "word/header1.xml" to ByteArray(0),
            "word/footer1.xml" to ByteArray(0),
            "docProps/core.xml" to ByteArray(0),
        )

        val result = FileTypeDetector.detect(files, FileTypeConfig.docx().fallbackPaths)

        assertEquals(OfficeFileType.DOCX, result.fileType)
        assertTrue(result.targetFiles.contains("word/document.xml"))
        assertTrue(result.targetFiles.contains("word/header1.xml"))
        assertTrue(result.targetFiles.contains("word/footer1.xml"))
        assertTrue(result.targetFiles.contains("docProps/core.xml"))
    }

    @Test
    fun `should handle missing Content_Types xml gracefully`() {
        val files = mapOf(
            "word/document.xml" to ByteArray(0),
        )

        val result = FileTypeDetector.detect(files, FileTypeConfig.docx().fallbackPaths)

        assertNotNull(result)
        assertEquals(OfficeFileType.DOCX, result.fileType)
        assertTrue(result.targetFiles.isNotEmpty())
    }

    @Test
    fun `should handle missing _rels rels gracefully`() {
        val contentTypesXml = """
            <?xml version="1.0"?>
            <Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
                <Override PartName="/word/document.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml"/>
            </Types>
        """.trimIndent()

        val files = mapOf(
            "[Content_Types].xml" to contentTypesXml.toByteArray(),
            "word/document.xml" to ByteArray(0),
        )

        // Use new overloaded detect() with config
        val result = FileTypeDetector.detect(files, FileTypeConfig.docx().fallbackPaths)

        assertNotNull(result)
        assertEquals(OfficeFileType.DOCX, result.fileType)
        assertEquals("word/document.xml", result.mainFile)  // Should use fallback
        assertTrue(result.targetFiles.contains("word/document.xml"))
    }

    @Test
    fun `detect with config should use rels when present`() {
        val contentTypesXml = """
            <?xml version="1.0"?>
            <Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
                <Override PartName="/word/document.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml"/>
            </Types>
        """.trimIndent()

        val relsXml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
                <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officedocument/2006/relationships/metadata/core-properties" Target="docProps/core.xml"/>
                <Relationship Id="rId2" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/extended-properties" Target="docProps/app.xml"/>
                <Relationship Id="rId3" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/custom-properties" Target="docProps/custom.xml"/>
                <Relationship Id="rId4" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="word/document.xml"/>
            </Relationships>
        """.trimIndent()

        val files = mapOf(
            "[Content_Types].xml" to contentTypesXml.toByteArray(),
            "_rels/.rels" to relsXml.toByteArray(),
            "word/document.xml" to ByteArray(0),
            "docProps/core.xml" to ByteArray(0),
            "docProps/app.xml" to ByteArray(0),
            "docProps/custom.xml" to ByteArray(0),
        )

        // Use new overloaded detect() with config
        val result = FileTypeDetector.detect(files, FileTypeConfig.docx().fallbackPaths)

        assertNotNull(result)
        assertEquals(OfficeFileType.DOCX, result.fileType)
        assertEquals("word/document.xml", result.mainFile)  // Should use rels
    }

    @Test
    fun `should handle macro-enabled documents`() {
        val contentTypesXml = """
            <?xml version="1.0"?>
            <Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
                <Override PartName="/word/document.xml" ContentType="application/vnd.ms-word.document.macroEnabled.main+xml"/>
            </Types>
        """.trimIndent()

        val files = mapOf(
            "[Content_Types].xml" to contentTypesXml.toByteArray(),
            "word/document.xml" to ByteArray(0),
        )

        val result = FileTypeDetector.detect(files, FileTypeConfig.docx().fallbackPaths)

        assertEquals(OfficeFileType.DOCX, result.fileType)
        assertTrue(result.targetFiles.contains("word/document.xml"))
    }

    @Test
    fun `should handle template documents`() {
        val dotxContentTypesXml = """
            <?xml version="1.0"?>
            <Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
                <Override PartName="/word/document.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.template.main+xml"/>
            </Types>
        """.trimIndent()

        val dotxFiles = mapOf(
            "[Content_Types].xml" to dotxContentTypesXml.toByteArray(),
            "word/document.xml" to ByteArray(0),
        )

        val dotxResult = FileTypeDetector.detect(dotxFiles, FileTypeConfig.docx().fallbackPaths)

        assertEquals(OfficeFileType.DOCX, dotxResult.fileType)

        val dotmContentTypesXml = """
            <?xml version="1.0"?>
            <Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
                <Override PartName="/word/document.xml" ContentType="application/vnd.ms-word.template.macroEnabledTemplate.main+xml"/>
            </Types>
        """.trimIndent()

        val dotmFiles = mapOf(
            "[Content_Types].xml" to dotmContentTypesXml.toByteArray(),
            "word/document.xml" to ByteArray(0),
        )

        val dotmResult = FileTypeDetector.detect(dotmFiles, FileTypeConfig.docx().fallbackPaths)

        assertEquals(OfficeFileType.DOCX, dotmResult.fileType)
    }

    @Test
    fun `should handle documents with headers, footers, footnotes and comments`() {
        val contentTypesXml = """
            <?xml version="1.0"?>
            <Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
                <Override PartName="/word/document.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml"/>
                <Override PartName="/word/header1.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.header+xml"/>
                <Override PartName="/word/header2.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.header+xml"/>
                <Override PartName="/word/footer1.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.footer+xml"/>
                <Override PartName="/word/footnotes.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.footnotes+xml"/>
                <Override PartName="/word/comments.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.comments+xml"/>
            </Types>
        """.trimIndent()

        val files = mapOf(
            "[Content_Types].xml" to contentTypesXml.toByteArray(),
            "word/document.xml" to ByteArray(0),
            "word/header1.xml" to ByteArray(0),
            "word/header2.xml" to ByteArray(0),
            "word/footer1.xml" to ByteArray(0),
            "word/footnotes.xml" to ByteArray(0),
            "word/comments.xml" to ByteArray(0),
        )

        val result = FileTypeDetector.detect(files, FileTypeConfig.docx().fallbackPaths)

        assertEquals(OfficeFileType.DOCX, result.fileType)
        assertTrue(result.targetFiles.contains("word/document.xml"))
        assertTrue(result.targetFiles.contains("word/header1.xml"))
        assertTrue(result.targetFiles.contains("word/header2.xml"))
        assertTrue(result.targetFiles.contains("word/footer1.xml"))
        assertTrue(result.targetFiles.contains("word/footnotes.xml"))
        assertTrue(result.targetFiles.contains("word/comments.xml"))
    }

    @Test
    fun `should not contain duplicate targets`() {
        val contentTypesXml = """
            <?xml version="1.0"?>
            <Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
                <Override PartName="/word/document.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml"/>
                <Override PartName="/docProps/core.xml" ContentType="application/vnd.openxmlformats-package.core-properties+xml"/>
            </Types>
        """.trimIndent()

        val files = mapOf(
            "[Content_Types].xml" to contentTypesXml.toByteArray(),
            "word/document.xml" to ByteArray(0),
            "docProps/core.xml" to ByteArray(0),
        )

        val result = FileTypeDetector.detect(files, FileTypeConfig.docx().fallbackPaths)

        assertEquals(result.targetFiles.size, result.targetFiles.distinct().size)
    }
}
