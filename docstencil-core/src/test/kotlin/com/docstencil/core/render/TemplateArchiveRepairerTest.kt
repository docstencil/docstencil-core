package com.docstencil.core.render

import com.docstencil.core.utils.ZipUtils
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TemplateArchiveRepairerTest {
    private fun createZipWithDrawings(files: Map<String, String>): ByteArray {
        val bytesMap = files.mapValues { it.value.toByteArray(Charsets.UTF_8) }
        return ZipUtils.writeFile(bytesMap)
    }

    @Test
    fun `fixDrawingIds basic single drawing renumbered`() {
        val documentXml = """
            <w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main"
                        xmlns:wp="http://schemas.openxmlformats.org/drawingml/2006/wordprocessingDrawing">
                <w:body>
                    <w:p>
                        <w:r>
                            <w:drawing>
                                <wp:inline>
                                    <wp:docPr id="5" name="Picture 1"/>
                                </wp:inline>
                            </w:drawing>
                        </w:r>
                    </w:p>
                </w:body>
            </w:document>
        """.trimIndent()

        val inputZip = createZipWithDrawings(mapOf("word/document.xml" to documentXml))
        val outputZip = TemplateArchiveRepairer().repair(inputZip)

        val outputFiles = ZipUtils.readFile(outputZip)
        val outputXml = outputFiles["word/document.xml"]!!.toString(Charsets.UTF_8)

        assertTrue(outputXml.contains("id=\"1\""))
        assertFalse(outputXml.contains("id=\"5\""))
    }

    @Test
    fun `fixDrawingIds multiple drawings in same file get sequential IDs`() {
        val documentXml = """
            <w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main"
                        xmlns:wp="http://schemas.openxmlformats.org/drawingml/2006/wordprocessingDrawing">
                <w:body>
                    <w:p>
                        <w:r>
                            <w:drawing>
                                <wp:inline>
                                    <wp:docPr id="1" name="Picture 1"/>
                                </wp:inline>
                            </w:drawing>
                        </w:r>
                        <w:r>
                            <w:drawing>
                                <wp:inline>
                                    <wp:docPr id="1" name="Picture 2"/>
                                </wp:inline>
                            </w:drawing>
                        </w:r>
                        <w:r>
                            <w:drawing>
                                <wp:inline>
                                    <wp:docPr id="1" name="Picture 3"/>
                                </wp:inline>
                            </w:drawing>
                        </w:r>
                    </w:p>
                </w:body>
            </w:document>
        """.trimIndent()

        val inputZip = createZipWithDrawings(mapOf("word/document.xml" to documentXml))
        val outputZip = TemplateArchiveRepairer().repair(inputZip)

        val outputFiles = ZipUtils.readFile(outputZip)
        val outputXml = outputFiles["word/document.xml"]!!.toString(Charsets.UTF_8)

        val idPattern = Regex("""<wp:docPr id="(\d+)"""")
        val ids = idPattern.findAll(outputXml).map { it.groupValues[1] }.toList()
        assertEquals(listOf("1", "2", "3"), ids)
    }

    @Test
    fun `fixDrawingIds across multiple files maintains global counter`() {
        val documentXml = """
            <w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main"
                        xmlns:wp="http://schemas.openxmlformats.org/drawingml/2006/wordprocessingDrawing">
                <w:body>
                    <w:p>
                        <w:r>
                            <w:drawing>
                                <wp:inline>
                                    <wp:docPr id="10" name="Picture 1"/>
                                </wp:inline>
                            </w:drawing>
                        </w:r>
                        <w:r>
                            <w:drawing>
                                <wp:inline>
                                    <wp:docPr id="11" name="Picture 2"/>
                                </wp:inline>
                            </w:drawing>
                        </w:r>
                    </w:p>
                </w:body>
            </w:document>
        """.trimIndent()

        val headerXml = """
            <w:hdr xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main"
                   xmlns:wp="http://schemas.openxmlformats.org/drawingml/2006/wordprocessingDrawing">
                <w:p>
                    <w:r>
                        <w:drawing>
                            <wp:inline>
                                <wp:docPr id="20" name="Header Picture"/>
                            </wp:inline>
                        </w:drawing>
                    </w:r>
                </w:p>
            </w:hdr>
        """.trimIndent()

        val footerXml = """
            <w:ftr xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main"
                   xmlns:wp="http://schemas.openxmlformats.org/drawingml/2006/wordprocessingDrawing">
                <w:p>
                    <w:r>
                        <w:drawing>
                            <wp:inline>
                                <wp:docPr id="30" name="Footer Picture"/>
                            </wp:inline>
                        </w:drawing>
                    </w:r>
                </w:p>
            </w:ftr>
        """.trimIndent()

        val inputZip = createZipWithDrawings(
            mapOf(
                "word/document.xml" to documentXml,
                "word/header1.xml" to headerXml,
                "word/footer1.xml" to footerXml,
            ),
        )
        val outputZip = TemplateArchiveRepairer().repair(inputZip)

        val outputFiles = ZipUtils.readFile(outputZip)

        val allIds = mutableListOf<String>()
        val idPattern = Regex("""<wp:docPr id="(\d+)"""")

        for (fileName in outputFiles.keys.sorted().filter { it.endsWith(".xml") }) {
            val xml = outputFiles[fileName]!!.toString(Charsets.UTF_8)
            val ids = idPattern.findAll(xml).map { it.groupValues[1] }.toList()
            allIds.addAll(ids)
        }

        assertEquals(4, allIds.size)
        assertEquals(listOf("1", "2", "3", "4"), allIds.sorted())
    }

    @Test
    fun `fixDrawingIds loop-generated duplicate drawings`() {
        val documentXml = """
            <w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main"
                        xmlns:wp="http://schemas.openxmlformats.org/drawingml/2006/wordprocessingDrawing">
                <w:body>
                    <w:p><w:r><w:drawing><wp:inline><wp:docPr id="1" name="Pic"/></wp:inline></w:drawing></w:r></w:p>
                    <w:p><w:r><w:drawing><wp:inline><wp:docPr id="1" name="Pic"/></wp:inline></w:drawing></w:r></w:p>
                    <w:p><w:r><w:drawing><wp:inline><wp:docPr id="1" name="Pic"/></wp:inline></w:drawing></w:r></w:p>
                    <w:p><w:r><w:drawing><wp:inline><wp:docPr id="1" name="Pic"/></wp:inline></w:drawing></w:r></w:p>
                    <w:p><w:r><w:drawing><wp:inline><wp:docPr id="1" name="Pic"/></wp:inline></w:drawing></w:r></w:p>
                </w:body>
            </w:document>
        """.trimIndent()

        val inputZip = createZipWithDrawings(mapOf("word/document.xml" to documentXml))
        val outputZip = TemplateArchiveRepairer().repair(inputZip)

        val outputFiles = ZipUtils.readFile(outputZip)
        val outputXml = outputFiles["word/document.xml"]!!.toString(Charsets.UTF_8)

        val idPattern = Regex("""<wp:docPr id="(\d+)"""")
        val ids = idPattern.findAll(outputXml).map { it.groupValues[1] }.toList()
        assertEquals(listOf("1", "2", "3", "4", "5"), ids)
    }

    @Test
    fun `fixDrawingIds mixed XML and non-XML files`() {
        val documentXml = """
            <w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main"
                        xmlns:wp="http://schemas.openxmlformats.org/drawingml/2006/wordprocessingDrawing">
                <w:body>
                    <w:p>
                        <w:r>
                            <w:drawing>
                                <wp:inline>
                                    <wp:docPr id="100" name="Picture"/>
                                </wp:inline>
                            </w:drawing>
                        </w:r>
                    </w:p>
                </w:body>
            </w:document>
        """.trimIndent()

        val pngBytes = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)
        val inputZip = ZipUtils.writeFile(
            mapOf(
                "word/document.xml" to documentXml.toByteArray(Charsets.UTF_8),
                "word/media/image1.png" to pngBytes,
                "word/media/image2.jpeg" to byteArrayOf(0xFF.toByte(), 0xD8.toByte()),
            ),
        )
        val outputZip = TemplateArchiveRepairer().repair(inputZip)

        val outputFiles = ZipUtils.readFile(outputZip)

        val outputXml = outputFiles["word/document.xml"]!!.toString(Charsets.UTF_8)
        assertTrue(outputXml.contains("id=\"1\""))

        assertTrue(outputFiles["word/media/image1.png"]!!.contentEquals(pngBytes))
        assertEquals(outputFiles["word/media/image2.jpeg"]!![0], 0xFF.toByte())
    }

    @Test
    fun `fixDrawingIds malformed XML returns original bytes`() {
        val malformedXml = "<w:document><unclosed>"
        val inputZip = createZipWithDrawings(mapOf("word/document.xml" to malformedXml))
        val outputZip = TemplateArchiveRepairer().repair(inputZip)

        val outputFiles = ZipUtils.readFile(outputZip)
        val outputXml = outputFiles["word/document.xml"]!!.toString(Charsets.UTF_8)

        assertEquals(malformedXml, outputXml)
    }

    @Test
    fun `fixDrawingIds no drawings present document unchanged`() {
        val documentXml = """
            <w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">
                <w:body>
                    <w:p>
                        <w:r>
                            <w:t>Just text, no drawings</w:t>
                        </w:r>
                    </w:p>
                </w:body>
            </w:document>
        """.trimIndent()

        val inputZip = createZipWithDrawings(mapOf("word/document.xml" to documentXml))
        val outputZip = TemplateArchiveRepairer().repair(inputZip)

        val outputFiles = ZipUtils.readFile(outputZip)
        val outputXml = outputFiles["word/document.xml"]!!.toString(Charsets.UTF_8)

        assertTrue(outputXml.contains("Just text, no drawings"))
    }
}
