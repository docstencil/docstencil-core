package com.docstencil.core.render

import com.docstencil.core.api.OfficeFileType
import com.docstencil.core.config.FileTypeConfig
import com.docstencil.core.render.model.XmlOutputToken
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TemplateFileRepairerTest {
    private val config = FileTypeConfig.docx()
    private val repairer = TemplateFileRepairer(OfficeFileType.DOCX, config.repairRules)

    private fun repair(xml: String): String {
        val tokens = XmlOutputToken.fromString(xml)
        val repaired = repairer.repair(tokens)
        return repaired.joinToString("") { it.xmlString }
    }

    @Test
    fun `empty cell inserts paragraph`() {
        val result = repair("<w:tr><w:tc></w:tc></w:tr>")
        assertTrue(result.contains("<w:p></w:p>"))
        assertTrue(result.contains("<w:tc><w:p></w:p></w:tc>"))
    }

    @Test
    fun `empty cell with properties inserts paragraph and preserves properties`() {
        val result = repair("<w:tr><w:tc><w:tcPr><w:tcW w:w=\"2000\"/></w:tcPr></w:tc></w:tr>")
        assertTrue(result.contains("<w:tcPr><w:tcW w:w=\"2000\"/></w:tcPr>"))
        assertTrue(result.contains("<w:p></w:p>"))
        assertTrue(result.indexOf("<w:tcPr>") < result.indexOf("<w:p></w:p>"))
    }

    @Test
    fun `cell with content is unchanged`() {
        val input = "<w:tc><w:p><w:r><w:t>Content</w:t></w:r></w:p></w:tc>"
        val result = repair(input)
        assertEquals(input, result)
    }

    @Test
    fun `self-closing empty cell is fixed`() {
        val result = repair("<w:tr><w:tc/></w:tr>")
        assertTrue(result.contains("<w:tc><w:p></w:p></w:tc>"))
        assertFalse(result.contains("<w:tc/>"))
    }

    @Test
    fun `multiple empty cells should all get paragraphs inserted`() {
        val result =
            repair("<w:tr><w:tc><w:tcPr></w:tcPr></w:tc><w:tc><w:tcPr></w:tcPr></w:tc></w:tr>")
        assertEquals(
            "<w:tr><w:tc><w:tcPr></w:tcPr><w:p></w:p></w:tc><w:tc><w:tcPr></w:tcPr><w:p></w:p></w:tc></w:tr>",
            result,
        )
    }

    @Test
    fun `empty row is dropped`() {
        val result = repair("<w:tbl><w:tr></w:tr></w:tbl>")
        assertFalse(result.contains("<w:tr>"))
        assertFalse(result.contains("</w:tr>"))
    }

    @Test
    fun `row with cells is kept`() {
        val result = repair("<w:tr><w:tc><w:p></w:p></w:tc></w:tr>")
        assertTrue(result.contains("<w:tr>"))
        assertTrue(result.contains("<w:tc>"))
    }

    @Test
    fun `empty table is dropped`() {
        val result = repair("<w:body><w:tbl></w:tbl></w:body>")
        assertFalse(result.contains("<w:tbl>"))
        assertFalse(result.contains("</w:tbl>"))
        assertTrue(result.contains("<w:body>"))
    }

    @Test
    fun `table with rows is kept`() {
        val result = repair("<w:tbl><w:tr><w:tc><w:p></w:p></w:tc></w:tr></w:tbl>")
        assertTrue(result.contains("<w:tbl>"))
        assertTrue(result.contains("<w:tr>"))
    }

    @Test
    fun `table followed by run gets paragraph inserted`() {
        val result =
            repair("<w:tbl><w:tr><w:tc><w:p></w:p></w:tc></w:tr></w:tbl><w:r><w:t>Text</w:t></w:r>")
        assertTrue(result.contains("</w:tbl><w:p/><w:r>"))
    }

    @Test
    fun `table followed by paragraph unchanged`() {
        val result =
            repair("<w:tbl><w:tr><w:tc><w:p></w:p></w:tc></w:tr></w:tbl><w:p><w:r><w:t>Text</w:t></w:r></w:p>")
        assertFalse(result.contains("</w:tbl><w:p/>"))
        assertTrue(result.contains("</w:tbl><w:p>"))
    }

    @Test
    fun `table followed by sectPr unchanged`() {
        val result =
            repair("<w:tbl><w:tr><w:tc><w:p></w:p></w:tc></w:tr></w:tbl><w:sectPr><w:pgSz w:w=\"12240\"/></w:sectPr>")
        assertFalse(result.contains("</w:tbl><w:p/>"))
        assertTrue(result.contains("</w:tbl><w:sectPr>"))
    }

    @Test
    fun `table at end of document unchanged`() {
        val result =
            repair("<w:body><w:tbl><w:tr><w:tc><w:p></w:p></w:tc></w:tr></w:tbl></w:body>")
        assertFalse(result.contains("</w:tbl><w:p/>"))
        assertTrue(result.contains("</w:tbl></w:body>"))
    }

    @Test
    fun `table followed by another table unchanged`() {
        val result = repair(
            "<w:tbl><w:tr><w:tc><w:p></w:p></w:tc></w:tr></w:tbl>" +
                    "<w:tbl><w:tr><w:tc><w:p></w:p></w:tc></w:tr></w:tbl>",
        )
        assertFalse(result.contains("</w:tbl><w:p/>"))
        assertTrue(result.contains("</w:tbl><w:tbl>"))
    }

    @Test
    fun `empty space preserve tag converted to self-closing`() {
        val result = repair("<w:r><w:t xml:space=\"preserve\"></w:t></w:r>")
        assertTrue(result.contains("<w:t/>"))
        assertFalse(result.contains("xml:space=\"preserve\""))
    }

    @Test
    fun `space preserve with content unchanged`() {
        val result = repair("<w:r><w:t xml:space=\"preserve\"> Content </w:t></w:r>")
        assertTrue(result.contains("<w:t xml:space=\"preserve\">"))
        assertTrue(result.contains(" Content "))
        assertTrue(result.contains("</w:t>"))
    }

    @Test
    fun `empty content control inserts paragraph`() {
        val result = repair("<w:sdt><w:sdtContent></w:sdtContent></w:sdt>")
        assertTrue(result.contains("<w:sdtContent><w:p></w:p></w:sdtContent>"))
    }

    @Test
    fun `content control with run is kept unchanged`() {
        val input = "<w:sdt><w:sdtContent><w:r><w:t>Text</w:t></w:r></w:sdtContent></w:sdt>"
        val result = repair(input)
        assertEquals(input, result)
    }

    @Test
    fun `complete document cleanup pipeline`() {
        val result = repair(
            "<w:body>" +
                    "<w:tbl><w:tr><w:tc></w:tc></w:tr></w:tbl>" +
                    "<w:r><w:t xml:space=\"preserve\"></w:t></w:r>" +
                    "</w:body>",
        )
        assertTrue(result.contains("<w:tc><w:p></w:p></w:tc>"))
        assertTrue(result.contains("</w:tbl><w:p/>"))
        assertTrue(result.contains("<w:t/>"))
    }
}
