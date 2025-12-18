package com.docstencil.core.modules

import com.docstencil.core.api.OfficeFileType
import com.docstencil.core.api.model.OfficeTemplateData
import com.docstencil.core.config.FileTypeConfig
import com.docstencil.core.utils.FileTypeDetector
import org.junit.jupiter.api.Test
import kotlin.test.assertTrue

class NamespaceEnsurerModuleTest {

    private fun createDetectionResult(targetFiles: List<String>): FileTypeDetector.DetectionResult {
        return FileTypeDetector.DetectionResult(
            fileType = OfficeFileType.DOCX,
            mainFile = "word/document.xml",
            targetFiles = targetFiles,
        )
    }

    @Test
    fun `should ensure namespaces in postRender hook`() {
        val xml = "<document><body>test</body></document>"
        val files = mapOf(
            "word/document.xml" to xml.toByteArray(Charsets.UTF_8),
        )
        val config = FileTypeConfig.docx()
        val targetFiles = listOf("word/document.xml")

        val module = NamespaceEnsurerModule(config, targetFiles)
        val templateData = OfficeTemplateData(files, createDetectionResult(targetFiles))

        module.postRender(templateData)

        val result = templateData.getData()["word/document.xml"]?.toString(Charsets.UTF_8)
        assertTrue(result != null, "Result should not be null")
        assertTrue(result.contains("xmlns:w=\"http://schemas.openxmlformats.org/wordprocessingml/2006/main\""))
        assertTrue(result.contains("xmlns:a=\"http://schemas.openxmlformats.org/drawingml/2006/main\""))
    }

    @Test
    fun `should process all target files`() {
        val xml1 = "<document><body>test1</body></document>"
        val xml2 = "<document><body>test2</body></document>"
        val files = mapOf(
            "word/document.xml" to xml1.toByteArray(Charsets.UTF_8),
            "word/header1.xml" to xml2.toByteArray(Charsets.UTF_8),
        )
        val config = FileTypeConfig.docx()
        val targetFiles = listOf("word/document.xml", "word/header1.xml")

        val module = NamespaceEnsurerModule(config, targetFiles)
        val templateData = OfficeTemplateData(files, createDetectionResult(targetFiles))

        module.postRender(templateData)

        val result = templateData.getData()
        val doc = result["word/document.xml"]?.toString(Charsets.UTF_8)
        val header = result["word/header1.xml"]?.toString(Charsets.UTF_8)

        assertTrue(doc != null && doc.contains("xmlns:w="))
        assertTrue(header != null && header.contains("xmlns:w="))
    }

    @Test
    fun `should skip processing when defaultNamespaces is empty`() {
        val xml = "<document><body>test</body></document>"
        val files = mapOf(
            "word/document.xml" to xml.toByteArray(Charsets.UTF_8),
        )
        val config = FileTypeConfig(
            fileType = OfficeFileType.DOCX,
            tagsText = emptyList(),
            tagsStructure = emptyList(),
            lineBreaks = FileTypeConfig.LineBreakConfig("w:br", emptyList()),
            defaultNamespaces = emptyMap(),
            fallbackPaths = FileTypeConfig.docx().fallbackPaths,
        )
        val targetFiles = listOf("word/document.xml")

        val module = NamespaceEnsurerModule(config, targetFiles)
        val templateData = OfficeTemplateData(files, createDetectionResult(targetFiles))

        module.postRender(templateData)

        val result = templateData.getData()["word/document.xml"]?.toString(Charsets.UTF_8)
        // Should return unchanged XML when namespaces are empty
        assertTrue(result == xml, "XML should be unchanged when defaultNamespaces is empty")
    }
}
