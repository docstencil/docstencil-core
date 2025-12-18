package com.docstencil.core.scanner

import com.docstencil.core.api.OfficeTemplateOptions
import com.docstencil.core.scanner.model.TagPartType
import com.docstencil.core.scanner.model.XmlRawInputToken
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RawXmlScannerTest {
    private fun createScanner(
        templateTagNames: Set<String> = setOf("w:t"),
        delimiters: OfficeTemplateOptions.Delimiters = OfficeTemplateOptions.Delimiters("{", "}"),
    ): RawXmlScanner {
        return RawXmlScanner(delimiters, templateTagNames)
    }

    private fun createTagPart(
        tagName: String,
        position: Int,
        rawText: String,
        tagPartType: TagPartType,
        isTextTag: Boolean,
    ): XmlRawInputToken.TagPart {
        return XmlRawInputToken.TagPart(tagName, position, rawText, tagPartType, isTextTag)
    }

    private fun createContentPart(
        position: Int,
        rawText: String,
        isInsideTextTag: Boolean,
    ): XmlRawInputToken.Content {
        return XmlRawInputToken.Content(position, rawText, isInsideTextTag)
    }

    @Suppress("SameParameterValue")
    private fun createPreamble(position: Int, rawText: String): XmlRawInputToken.Verbatim {
        return XmlRawInputToken.Verbatim(position, rawText)
    }

    @Test
    fun `scan should extract content between tags`() {
        val content = "<w:t>Hello World</w:t>"
        val scanner = createScanner()

        val tokens = scanner.scan(content)

        assertEquals(3, tokens.size)

        assertEquals(createTagPart("w:t", 0, "<w:t>", TagPartType.OPENING, true), tokens[0])
        assertEquals(createContentPart(5, "Hello World", true), tokens[1])
        assertEquals(createTagPart("w:t", 16, "</w:t>", TagPartType.CLOSING, true), tokens[2])
    }

    @Test
    fun `scan should handle multiple tags`() {
        val content = "<w:p><w:r><w:t>Text</w:t></w:r></w:p>"
        val scanner = createScanner()

        val tokens = scanner.scan(content)

        assertEquals(7, tokens.size)

        assertEquals(createTagPart("w:p", 0, "<w:p>", TagPartType.OPENING, false), tokens[0])
        assertEquals(createTagPart("w:r", 5, "<w:r>", TagPartType.OPENING, false), tokens[1])
        assertEquals(createTagPart("w:t", 10, "<w:t>", TagPartType.OPENING, true), tokens[2])
        assertEquals(createContentPart(15, "Text", true), tokens[3])
        assertEquals(createTagPart("w:t", 19, "</w:t>", TagPartType.CLOSING, true), tokens[4])
        assertEquals(createTagPart("w:r", 25, "</w:r>", TagPartType.CLOSING, false), tokens[5])
        assertEquals(createTagPart("w:p", 31, "</w:p>", TagPartType.CLOSING, false), tokens[6])
    }

    @Test
    fun `scan should mark content inside text tags`() {
        val content = "Header<w:t>Inside</w:t>Footer"
        val scanner = createScanner()

        val tokens = scanner.scan(content)

        assertEquals(5, tokens.size)

        assertEquals(createContentPart(0, "Header", false), tokens[0])
        assertEquals(createTagPart("w:t", 6, "<w:t>", TagPartType.OPENING, true), tokens[1])
        assertEquals(createContentPart(11, "Inside", true), tokens[2])
        assertEquals(createTagPart("w:t", 17, "</w:t>", TagPartType.CLOSING, true), tokens[3])
        assertEquals(createContentPart(23, "Footer", false), tokens[4])
    }

    @Test
    fun `scan should detect tag part types`() {
        val content = "<w:p><w:t>Text</w:t><w:br/></w:p>"
        val scanner = createScanner()

        val tokens = scanner.scan(content)

        assertEquals(6, tokens.size)

        assertEquals(createTagPart("w:p", 0, "<w:p>", TagPartType.OPENING, false), tokens[0])
        assertEquals(createTagPart("w:t", 5, "<w:t>", TagPartType.OPENING, true), tokens[1])
        assertEquals(createContentPart(10, "Text", true), tokens[2])
        assertEquals(createTagPart("w:t", 14, "</w:t>", TagPartType.CLOSING, true), tokens[3])
        assertEquals(
            createTagPart("w:br", 20, "<w:br/>", TagPartType.SELF_CLOSING, false),
            tokens[4],
        )
        assertEquals(createTagPart("w:p", 27, "</w:p>", TagPartType.CLOSING, false), tokens[5])
    }

    @Test
    fun `scan should handle XML preamble`() {
        val content = "<?xml version=\"1.0\"?><w:t>Text</w:t>"
        val scanner = createScanner()

        val tokens = scanner.scan(content)

        assertEquals(4, tokens.size)

        assertEquals(createPreamble(0, "<?xml version=\"1.0\"?>"), tokens[0])
        assertEquals(createTagPart("w:t", 21, "<w:t>", TagPartType.OPENING, true), tokens[1])
        assertEquals(createContentPart(26, "Text", true), tokens[2])
        assertEquals(createTagPart("w:t", 30, "</w:t>", TagPartType.CLOSING, true), tokens[3])
    }

    @Test
    fun `scan should split content at delimiter positions`() {
        val content = "<w:t>{firstName}</w:t>"
        val scanner = createScanner()

        val tokens = scanner.scan(content)

        assertEquals(5, tokens.size)

        assertEquals(createTagPart("w:t", 0, "<w:t>", TagPartType.OPENING, true), tokens[0])
        assertEquals(createContentPart(0, "{", true), tokens[1])
        assertEquals(createContentPart(1, "firstName", true), tokens[2])
        assertEquals(createContentPart(10, "}", true), tokens[3])
        assertEquals(createTagPart("w:t", 16, "</w:t>", TagPartType.CLOSING, true), tokens[4])
    }

    @Test
    fun `scan should split multiple placeholders`() {
        val content = "<w:t>{firstName} {lastName}</w:t>"
        val scanner = createScanner()

        val tokens = scanner.scan(content)

        assertEquals(9, tokens.size)

        assertEquals(createTagPart("w:t", 0, "<w:t>", TagPartType.OPENING, true), tokens[0])
        assertEquals(createContentPart(0, "{", true), tokens[1])
        assertEquals(createContentPart(1, "firstName", true), tokens[2])
        assertEquals(createContentPart(10, "}", true), tokens[3])
        assertEquals(createContentPart(11, " ", true), tokens[4])
        assertEquals(createContentPart(12, "{", true), tokens[5])
        assertEquals(createContentPart(13, "lastName", true), tokens[6])
        assertEquals(createContentPart(21, "}", true), tokens[7])
        assertEquals(createTagPart("w:t", 27, "</w:t>", TagPartType.CLOSING, true), tokens[8])
    }

    @Test
    fun `scan should only split content inside text tags`() {
        val content = "Header{notSplit}<w:t>{split}</w:t>Footer{alsoNotSplit}"
        val scanner = createScanner()

        val tokens = scanner.scan(content)

        assertEquals(7, tokens.size)

        assertEquals(createContentPart(0, "Header{notSplit}", false), tokens[0])
        assertEquals(createTagPart("w:t", 16, "<w:t>", TagPartType.OPENING, true), tokens[1])
        assertEquals(createContentPart(0, "{", true), tokens[2])
        assertEquals(createContentPart(1, "split", true), tokens[3])
        assertEquals(createContentPart(6, "}", true), tokens[4])
        assertEquals(createTagPart("w:t", 28, "</w:t>", TagPartType.CLOSING, true), tokens[5])
        assertEquals(createContentPart(34, "Footer{alsoNotSplit}", false), tokens[6])
    }

    @Test
    fun `scan should handle loop syntax`() {
        val content = "<w:t>{for item in items}Item: {item.name}{end}</w:t>"
        val scanner = createScanner()

        val tokens = scanner.scan(content)

        assertEquals(12, tokens.size)

        assertEquals(createTagPart("w:t", 0, "<w:t>", TagPartType.OPENING, true), tokens[0])
        assertEquals(createContentPart(0, "{", true), tokens[1])
        assertEquals(createContentPart(1, "for item in items", true), tokens[2])
        assertEquals(createContentPart(18, "}", true), tokens[3])
        assertEquals(createContentPart(19, "Item: ", true), tokens[4])
        assertEquals(createContentPart(25, "{", true), tokens[5])
        assertEquals(createContentPart(26, "item.name", true), tokens[6])
        assertEquals(createContentPart(35, "}", true), tokens[7])
        assertEquals(createContentPart(36, "{", true), tokens[8])
        assertEquals(createContentPart(37, "end", true), tokens[9])
        assertEquals(createContentPart(40, "}", true), tokens[10])
        assertEquals(createTagPart("w:t", 46, "</w:t>", TagPartType.CLOSING, true), tokens[11])
    }

    @Test
    fun `scan should handle fragmented placeholders across runs`() {
        val content = "<w:r><w:t>{first</w:t></w:r><w:r><w:t>Name}</w:t></w:r>"
        val scanner = createScanner()

        val tokens = scanner.scan(content)

        assertEquals(12, tokens.size)

        assertEquals(createTagPart("w:r", 0, "<w:r>", TagPartType.OPENING, false), tokens[0])
        assertEquals(createTagPart("w:t", 5, "<w:t>", TagPartType.OPENING, true), tokens[1])
        assertEquals(createContentPart(0, "{", true), tokens[2])
        assertEquals(createContentPart(1, "first", true), tokens[3])
        assertEquals(createTagPart("w:t", 16, "</w:t>", TagPartType.CLOSING, true), tokens[4])
        assertEquals(createTagPart("w:r", 22, "</w:r>", TagPartType.CLOSING, false), tokens[5])
        assertEquals(createTagPart("w:r", 28, "<w:r>", TagPartType.OPENING, false), tokens[6])
        assertEquals(createTagPart("w:t", 33, "<w:t>", TagPartType.OPENING, true), tokens[7])
        assertEquals(createContentPart(6, "Name", true), tokens[8])
        assertEquals(createContentPart(10, "}", true), tokens[9])
        assertEquals(createTagPart("w:t", 43, "</w:t>", TagPartType.CLOSING, true), tokens[10])
        assertEquals(createTagPart("w:r", 49, "</w:r>", TagPartType.CLOSING, false), tokens[11])
    }

    @Test
    fun `scan should handle multiple fragmented placeholders`() {
        val content = "<w:r><w:t>{</w:t></w:r><w:r><w:t>first</w:t></w:r><w:r><w:t>}</w:t></w:r>" +
                "<w:r><w:t> </w:t></w:r>" +
                "<w:r><w:t>{</w:t></w:r><w:r><w:t>second</w:t></w:r><w:r><w:t>}</w:t></w:r>"
        val scanner = createScanner()

        val tokens = scanner.scan(content)

        assertEquals(35, tokens.size)

        assertEquals(createTagPart("w:r", 0, "<w:r>", TagPartType.OPENING, false), tokens[0])
        assertEquals(createTagPart("w:t", 5, "<w:t>", TagPartType.OPENING, true), tokens[1])
        assertEquals(createContentPart(10, "{", true), tokens[2])
        assertEquals(createTagPart("w:t", 11, "</w:t>", TagPartType.CLOSING, true), tokens[3])
        assertEquals(createTagPart("w:r", 17, "</w:r>", TagPartType.CLOSING, false), tokens[4])
        assertEquals(createTagPart("w:r", 23, "<w:r>", TagPartType.OPENING, false), tokens[5])
        assertEquals(createTagPart("w:t", 28, "<w:t>", TagPartType.OPENING, true), tokens[6])
        assertEquals(createContentPart(33, "first", true), tokens[7])
        assertEquals(createTagPart("w:t", 38, "</w:t>", TagPartType.CLOSING, true), tokens[8])
        assertEquals(createTagPart("w:r", 44, "</w:r>", TagPartType.CLOSING, false), tokens[9])
        assertEquals(createTagPart("w:r", 50, "<w:r>", TagPartType.OPENING, false), tokens[10])
        assertEquals(createTagPart("w:t", 55, "<w:t>", TagPartType.OPENING, true), tokens[11])
        assertEquals(createContentPart(60, "}", true), tokens[12])
        assertEquals(createTagPart("w:t", 61, "</w:t>", TagPartType.CLOSING, true), tokens[13])
        assertEquals(createTagPart("w:r", 67, "</w:r>", TagPartType.CLOSING, false), tokens[14])
        assertEquals(createTagPart("w:r", 73, "<w:r>", TagPartType.OPENING, false), tokens[15])
        assertEquals(createTagPart("w:t", 78, "<w:t>", TagPartType.OPENING, true), tokens[16])
        assertEquals(createContentPart(83, " ", true), tokens[17])
        assertEquals(createTagPart("w:t", 84, "</w:t>", TagPartType.CLOSING, true), tokens[18])
        assertEquals(createTagPart("w:r", 90, "</w:r>", TagPartType.CLOSING, false), tokens[19])
        assertEquals(createTagPart("w:r", 96, "<w:r>", TagPartType.OPENING, false), tokens[20])
        assertEquals(createTagPart("w:t", 101, "<w:t>", TagPartType.OPENING, true), tokens[21])
        assertEquals(createContentPart(106, "{", true), tokens[22])
        assertEquals(createTagPart("w:t", 107, "</w:t>", TagPartType.CLOSING, true), tokens[23])
        assertEquals(createTagPart("w:r", 113, "</w:r>", TagPartType.CLOSING, false), tokens[24])
        assertEquals(createTagPart("w:r", 119, "<w:r>", TagPartType.OPENING, false), tokens[25])
        assertEquals(createTagPart("w:t", 124, "<w:t>", TagPartType.OPENING, true), tokens[26])
        assertEquals(createContentPart(129, "second", true), tokens[27])
        assertEquals(createTagPart("w:t", 135, "</w:t>", TagPartType.CLOSING, true), tokens[28])
        assertEquals(createTagPart("w:r", 141, "</w:r>", TagPartType.CLOSING, false), tokens[29])
        assertEquals(createTagPart("w:r", 147, "<w:r>", TagPartType.OPENING, false), tokens[30])
        assertEquals(createTagPart("w:t", 152, "<w:t>", TagPartType.OPENING, true), tokens[31])
        assertEquals(createContentPart(157, "}", true), tokens[32])
        assertEquals(createTagPart("w:t", 158, "</w:t>", TagPartType.CLOSING, true), tokens[33])
        assertEquals(createTagPart("w:r", 164, "</w:r>", TagPartType.CLOSING, false), tokens[34])
    }

    @Test
    fun `scan should handle nested loops with fragmented closing tag`() {
        val content = "<w:r><w:t>{for out in outer}{for inn in inner}" +
                "Content" +
                "{end}{:</w:t></w:r><w:r><w:t>end</w:t></w:r><w:r><w:t>}</w:t></w:r>"
        val scanner = createScanner()

        val tokens = scanner.scan(content)

        assertEquals(26, tokens.size)

        assertEquals(createTagPart("w:r", 0, "<w:r>", TagPartType.OPENING, false), tokens[0])
        assertEquals(createTagPart("w:t", 5, "<w:t>", TagPartType.OPENING, true), tokens[1])
        assertEquals(createContentPart(0, "{", true), tokens[2])
        assertEquals(createContentPart(1, "for out in outer", true), tokens[3])
        assertEquals(createContentPart(17, "}", true), tokens[4])
        assertEquals(createContentPart(18, "{", true), tokens[5])
        assertEquals(createContentPart(19, "for inn in inner", true), tokens[6])
        assertEquals(createContentPart(35, "}", true), tokens[7])
        assertEquals(createContentPart(36, "Content", true), tokens[8])
        assertEquals(createContentPart(43, "{", true), tokens[9])
        assertEquals(createContentPart(44, "end", true), tokens[10])
        assertEquals(createContentPart(47, "}", true), tokens[11])
        assertEquals(createContentPart(48, "{", true), tokens[12])
        assertEquals(createContentPart(49, ":", true), tokens[13])
        assertEquals(createTagPart("w:t", 60, "</w:t>", TagPartType.CLOSING, true), tokens[14])
        assertEquals(createTagPart("w:r", 66, "</w:r>", TagPartType.CLOSING, false), tokens[15])
        assertEquals(createTagPart("w:r", 72, "<w:r>", TagPartType.OPENING, false), tokens[16])
        assertEquals(createTagPart("w:t", 77, "<w:t>", TagPartType.OPENING, true), tokens[17])
        assertEquals(createContentPart(82, "end", true), tokens[18])
        assertEquals(createTagPart("w:t", 85, "</w:t>", TagPartType.CLOSING, true), tokens[19])
        assertEquals(createTagPart("w:r", 91, "</w:r>", TagPartType.CLOSING, false), tokens[20])
        assertEquals(createTagPart("w:r", 97, "<w:r>", TagPartType.OPENING, false), tokens[21])
        assertEquals(createTagPart("w:t", 102, "<w:t>", TagPartType.OPENING, true), tokens[22])
        assertEquals(createContentPart(107, "}", true), tokens[23])
        assertEquals(createTagPart("w:t", 108, "</w:t>", TagPartType.CLOSING, true), tokens[24])
        assertEquals(createTagPart("w:r", 114, "</w:r>", TagPartType.CLOSING, false), tokens[25])
    }

    @Test
    fun `scan should handle custom delimiters`() {
        val content = "<w:t>{{firstName}}</w:t>"
        val scanner = createScanner(
            delimiters = OfficeTemplateOptions.Delimiters("{{", "}}"),
        )

        val tokens = scanner.scan(content)

        assertEquals(5, tokens.size)

        assertEquals(createTagPart("w:t", 0, "<w:t>", TagPartType.OPENING, true), tokens[0])
        assertEquals(createContentPart(0, "{{", true), tokens[1])
        assertEquals(createContentPart(2, "firstName", true), tokens[2])
        assertEquals(createContentPart(11, "}}", true), tokens[3])
        assertEquals(createTagPart("w:t", 18, "</w:t>", TagPartType.CLOSING, true), tokens[4])
    }

    @Test
    fun `scan should handle content at end of file`() {
        val content = "<w:t>Text</w:t>Trailing content"
        val scanner = createScanner()

        val tokens = scanner.scan(content)

        assertEquals(4, tokens.size)

        assertEquals(createTagPart("w:t", 0, "<w:t>", TagPartType.OPENING, true), tokens[0])
        assertEquals(createContentPart(5, "Text", true), tokens[1])
        assertEquals(createTagPart("w:t", 9, "</w:t>", TagPartType.CLOSING, true), tokens[2])
        assertEquals(createContentPart(15, "Trailing content", false), tokens[3])
    }

    @Test
    fun `scan should handle empty content`() {
        val content = "<w:t></w:t>"
        val scanner = createScanner()

        val tokens = scanner.scan(content)

        assertEquals(2, tokens.size)

        assertEquals(createTagPart("w:t", 0, "<w:t>", TagPartType.OPENING, true), tokens[0])
        assertEquals(createTagPart("w:t", 5, "</w:t>", TagPartType.CLOSING, true), tokens[1])
    }

    @Test
    fun `scan should handle multiple text tags`() {
        val content = "<w:t>{first}</w:t><w:t>{second}</w:t>"
        val scanner = createScanner()

        val tokens = scanner.scan(content)

        assertEquals(10, tokens.size)

        assertEquals(createTagPart("w:t", 0, "<w:t>", TagPartType.OPENING, true), tokens[0])
        assertEquals(createContentPart(0, "{", true), tokens[1])
        assertEquals(createContentPart(1, "first", true), tokens[2])
        assertEquals(createContentPart(6, "}", true), tokens[3])
        assertEquals(createTagPart("w:t", 12, "</w:t>", TagPartType.CLOSING, true), tokens[4])
        assertEquals(createTagPart("w:t", 18, "<w:t>", TagPartType.OPENING, true), tokens[5])
        assertEquals(createContentPart(7, "{", true), tokens[6])
        assertEquals(createContentPart(8, "second", true), tokens[7])
        assertEquals(createContentPart(14, "}", true), tokens[8])
        assertEquals(createTagPart("w:t", 31, "</w:t>", TagPartType.CLOSING, true), tokens[9])
    }

    @Test
    fun `scan should preserve rawText in TagPart`() {
        val content = "<w:p xml:space=\"preserve\">Content</w:p>"
        val scanner = createScanner()

        val tokens = scanner.scan(content)

        assertEquals(3, tokens.size)

        val openingTag = tokens[0] as XmlRawInputToken.TagPart
        assertEquals("w:p", openingTag.tagName)
        assertEquals(TagPartType.OPENING, openingTag.tagPartType)
        assertTrue(openingTag.rawText.contains("xml:space=\"preserve\""))
        assertEquals(createContentPart(26, "Content", false), tokens[1])
        assertEquals(createTagPart("w:p", 33, "</w:p>", TagPartType.CLOSING, false), tokens[2])
    }

    @Test
    fun `scan should unescape XML entities in content`() {
        val content = "<w:t>Hello &amp; world &lt;tag&gt; &quot;test&quot; &apos;test&apos;</w:t>"
        val scanner = createScanner()

        val tokens = scanner.scan(content)

        assertEquals(3, tokens.size)

        assertEquals(createTagPart("w:t", 0, "<w:t>", TagPartType.OPENING, true), tokens[0])
        assertEquals(createContentPart(5, "Hello & world <tag> \"test\" 'test'", true), tokens[1])
        assertEquals(createTagPart("w:t", 68, "</w:t>", TagPartType.CLOSING, true), tokens[2])
    }
}
