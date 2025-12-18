package com.docstencil.core.scanner

import com.docstencil.core.config.FileTypeConfig
import com.docstencil.core.scanner.model.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith


class XmlInputTokenMergerTest {
    private fun createMerger(): TemplateXmlTokenMerger {
        return TemplateXmlTokenMerger(FileTypeConfig.docx().tagNicknames)
    }

    private fun createContentPart(
        content: String,
        isInsideTextTag: Boolean = true,
    ): XmlRawInputToken.Content {
        return XmlRawInputToken.Content(0, content, isInsideTextTag)
    }

    @Suppress("SameParameterValue")
    private fun createTagPart(
        tagName: String,
        rawText: String,
        type: TagPartType,
        isTextTag: Boolean = false,
    ): XmlRawInputToken.TagPart {
        return XmlRawInputToken.TagPart(tagName, 0, rawText, type, isTextTag)
    }

    @Suppress("SameParameterValue")
    private fun createToken(
        type: TemplateTokenType,
        lexeme: String,
        startIdx: Int,
        literal: Any? = null,
    ): TemplateToken {
        return TemplateToken(type, lexeme, null, literal, ContentIdx(startIdx))
    }

    private fun createTemplateGroup(
        tokens: List<TemplateToken>,
        startInclusive: Int,
        endExclusive: Int,
    ): TemplateTokenGroup {
        return TemplateTokenGroup(
            tokens,
            ContentIdx(startInclusive),
            ContentIdx(endExclusive),
        )
    }

    private fun createSimpleTemplate(name: String, startIdx: Int): TemplateTokenGroup {
        return createTemplateGroup(
            listOf(
                createToken(TemplateTokenType.DELIMITER_OPEN, "{", startIdx),
                createToken(TemplateTokenType.IDENTIFIER, name, startIdx + 1),
                createToken(
                    TemplateTokenType.DELIMITER_CLOSE,
                    "}",
                    startIdx + 1 + name.length,
                ),
            ),
            startIdx,
            startIdx + name.length + 2,
        )
    }

    @Test
    fun `merge should return empty list for empty inputs`() {
        val merger = createMerger()
        val rawTokens = emptyList<XmlRawInputToken>()
        val templateGroups = emptyList<TemplateTokenGroup>()

        val result = merger.merge(rawTokens, templateGroups)

        assertEquals(0, result.size)
    }

    @Test
    fun `merge should handle single RawXml token with no templates`() {
        val merger = createMerger()
        val contentPart = createContentPart("Hello", false)
        val rawTokens = listOf(contentPart)
        val templateGroups = emptyList<TemplateTokenGroup>()

        val result = merger.merge(rawTokens, templateGroups)

        assertEquals(1, result.size)

        assertEquals(XmlInputToken.Raw(contentPart), result[0])
    }

    @Test
    fun `merge should handle single template matching raw content at start`() {
        val merger = createMerger()
        val rawTokens = listOf(
            createContentPart("{", true),
            createContentPart("name", true),
            createContentPart("}", true),
        )
        val templateGroups = listOf(
            createSimpleTemplate("name", 0),
        )

        val result = merger.merge(rawTokens, templateGroups)

        assertEquals(4, result.size)
        assertEquals(
            XmlInputToken.TemplateGroup.create(templateGroups[0]),
            result[0],
        )
        assertEquals(XmlInputToken.Sentinel(), result[1])
        assertEquals(XmlInputToken.Sentinel(), result[2])
        assertEquals(XmlInputToken.Sentinel(), result[3])
    }

    @Test
    fun `merge should emit RawXml for non-text ContentPart`() {
        val merger = createMerger()
        val contentPart = createContentPart("Header", false)
        val rawTokens = listOf(contentPart)
        val templateGroups = emptyList<TemplateTokenGroup>()

        val result = merger.merge(rawTokens, templateGroups)

        assertEquals(1, result.size)
        assertEquals(XmlInputToken.Raw(contentPart), result[0])
    }

    @Test
    fun `merge should emit RawXml for TagPart`() {
        val merger = createMerger()
        val tagPart = createTagPart("w:p", "<w:p>", TagPartType.OPENING, false)
        val rawTokens = listOf(tagPart)
        val templateGroups = emptyList<TemplateTokenGroup>()

        val result = merger.merge(rawTokens, templateGroups)

        assertEquals(1, result.size)
        assertEquals(XmlInputToken.Raw(tagPart), result[0])
    }

    @Test
    fun `merge should handle template with verbatim content`() {
        val merger = createMerger()
        val rawTokens = listOf(
            createContentPart("Hello ", true),
            createContentPart("{", true),
            createContentPart("name", true),
            createContentPart("}", true),
            createContentPart("World ", true),
        )
        val templateGroups = listOf(
            createSimpleTemplate("name", 6),
        )

        val result = merger.merge(rawTokens, templateGroups)

        assertEquals(6, result.size)
        assertEquals(XmlInputToken.Raw(createContentPart("Hello ", true)), result[0])
        assertEquals(XmlInputToken.TemplateGroup.create(templateGroups[0]), result[1])
        assertEquals(XmlInputToken.Sentinel(), result[2])
        assertEquals(XmlInputToken.Sentinel(), result[3])
        assertEquals(XmlInputToken.Sentinel(), result[4])
        assertEquals(XmlInputToken.Raw(createContentPart("World ", true)), result[5])
    }

    @Test
    fun `merge should handle templates at consecutive positions`() {
        val merger = createMerger()
        val rawTokens = listOf(
            createContentPart("{", true),
            createContentPart("first", true),
            createContentPart("}", true),
            createContentPart("{", true),
            createContentPart("second", true),
            createContentPart("}", true),
        )
        val templateGroups = listOf(
            createSimpleTemplate("first", 0),
            createSimpleTemplate("second", 7),
        )

        val result = merger.merge(rawTokens, templateGroups)

        assertEquals(8, result.size)
        assertEquals(XmlInputToken.TemplateGroup.create(templateGroups[0]), result[0])
        assertEquals(XmlInputToken.Sentinel(), result[1])
        assertEquals(XmlInputToken.Sentinel(), result[2])
        assertEquals(XmlInputToken.Sentinel(), result[3])
        assertEquals(XmlInputToken.TemplateGroup.create(templateGroups[1]), result[4])
        assertEquals(XmlInputToken.Sentinel(), result[5])
        assertEquals(XmlInputToken.Sentinel(), result[6])
        assertEquals(XmlInputToken.Sentinel(), result[7])
    }

    @Test
    fun `merge should handle templates separated by tags`() {
        val merger = createMerger()
        val openTag = createTagPart("w:p", "<w:p>", TagPartType.OPENING, false)
        val closeTag = createTagPart("w:p", "</w:p>", TagPartType.CLOSING, false)
        val rawTokens = listOf(
            createContentPart("{", true),
            createContentPart("a", true),
            createContentPart("}", true),
            openTag,
            createContentPart("{", true),
            createContentPart("b", true),
            createContentPart("}", true),
            closeTag,
        )
        val templateGroups = listOf(
            createSimpleTemplate("a", 0),
            createSimpleTemplate("b", 3),
        )

        val result = merger.merge(rawTokens, templateGroups)

        assertEquals(10, result.size)
        assertEquals(XmlInputToken.TemplateGroup.create(templateGroups[0]), result[0])
        assertEquals(XmlInputToken.Sentinel(), result[1])
        assertEquals(XmlInputToken.Sentinel(), result[2])
        assertEquals(XmlInputToken.Sentinel(), result[3])
        assertEquals(XmlInputToken.Raw(openTag), result[4])
        assertEquals(XmlInputToken.TemplateGroup.create(templateGroups[1]), result[5])
        assertEquals(XmlInputToken.Sentinel(), result[6])
        assertEquals(XmlInputToken.Sentinel(), result[7])
        assertEquals(XmlInputToken.Sentinel(), result[8])
        assertEquals(XmlInputToken.Raw(closeTag), result[9])
    }

    @Test
    fun `merge should throw error when raw tokens exhausted before template groups`() {
        val merger = createMerger()
        val rawTokens = listOf(
            createContentPart("a", true),
        )
        val templateGroups = listOf(
            createSimpleTemplate("var", 5),
        )

        assertFailsWith<RuntimeException> {
            merger.merge(rawTokens, templateGroups)
        }
    }

    @Test
    fun `merge should continue with RawXml when template groups exhausted first`() {
        val merger = createMerger()
        val rawTokens = listOf(
            createContentPart("{", true),
            createContentPart("a", true),
            createContentPart("}", true),
            createContentPart("Extra", true),
        )
        val templateGroups = listOf(
            createSimpleTemplate("a", 0),
        )

        val result = merger.merge(rawTokens, templateGroups)

        assertEquals(5, result.size)
        assertEquals(XmlInputToken.TemplateGroup.create(templateGroups[0]), result[0])
        assertEquals(XmlInputToken.Sentinel(), result[1])
        assertEquals(XmlInputToken.Sentinel(), result[2])
        assertEquals(XmlInputToken.Sentinel(), result[3])
        assertEquals(XmlInputToken.Raw(createContentPart("Extra", true)), result[4])
    }

    @Test
    fun `merge should only process text-tag content for templates`() {
        val merger = createMerger()
        val rawTokens = listOf(
            createContentPart("{name}", false),
            createContentPart("{", true),
            createContentPart("var", true),
            createContentPart("}", true),
        )
        val templateGroups = listOf(
            createSimpleTemplate("var", 0),
        )

        val result = merger.merge(rawTokens, templateGroups)

        assertEquals(5, result.size)
        assertEquals(XmlInputToken.Raw(createContentPart("{name}", false)), result[0])
        assertEquals(XmlInputToken.TemplateGroup.create(templateGroups[0]), result[1])
        assertEquals(XmlInputToken.Sentinel(), result[2])
        assertEquals(XmlInputToken.Sentinel(), result[3])
        assertEquals(XmlInputToken.Sentinel(), result[4])
    }

    @Test
    fun `merge should handle fragmented template across multiple runs`() {
        val merger = createMerger()
        val rawTokens = listOf(
            createContentPart("{", true),
            createContentPart("first", true),
            createContentPart("Na", true),
            createContentPart("me", true),
            createContentPart("}", true),
        )
        val templateGroups = listOf(
            createTemplateGroup(
                listOf(
                    createToken(TemplateTokenType.DELIMITER_OPEN, "{", 0),
                    createToken(TemplateTokenType.IDENTIFIER, "firstName", 1),
                    createToken(TemplateTokenType.DELIMITER_CLOSE, "}", 10),
                ),
                0,
                11,
            ),
        )

        val result = merger.merge(rawTokens, templateGroups)

        assertEquals(6, result.size)
        assertEquals(XmlInputToken.TemplateGroup.create(templateGroups[0]), result[0])
        assertEquals(XmlInputToken.Sentinel(), result[1])
        assertEquals(XmlInputToken.Sentinel(), result[2])
        assertEquals(XmlInputToken.Sentinel(), result[3])
        assertEquals(XmlInputToken.Sentinel(), result[4])
        assertEquals(XmlInputToken.Sentinel(), result[5])
    }

    @Test
    fun `merge should handle complex FOR loop template`() {
        val merger = createMerger()
        val rawTokens = listOf(
            createContentPart("{", true),
            createContentPart("for item in items", true),
            createContentPart("}", true),
            createContentPart("foo", true),
            createContentPart("{", true),
            createContentPart("end", true),
            createContentPart("}", true),
        )
        val templateGroups = listOf(
            createTemplateGroup(
                listOf(
                    createToken(TemplateTokenType.DELIMITER_OPEN, "{", 0),
                    createToken(TemplateTokenType.FOR, "for", 1),
                    createToken(TemplateTokenType.IDENTIFIER, "item", 5),
                    createToken(TemplateTokenType.IN, "in", 10),
                    createToken(TemplateTokenType.IDENTIFIER, "items", 13),
                    createToken(TemplateTokenType.DELIMITER_CLOSE, "}", 18),
                ),
                0,
                19,
            ),
            createTemplateGroup(
                listOf(
                    createToken(TemplateTokenType.DELIMITER_OPEN, "{", 22),
                    createToken(TemplateTokenType.END, "end", 23),
                    createToken(TemplateTokenType.DELIMITER_CLOSE, "}", 26),
                ),
                22,
                27,
            ),
        )

        val result = merger.merge(rawTokens, templateGroups)

        assertEquals(9, result.size)
        assertEquals(XmlInputToken.TemplateGroup.create(templateGroups[0]), result[0])
        assertEquals(XmlInputToken.Sentinel(), result[1])
        assertEquals(XmlInputToken.Sentinel(), result[2])
        assertEquals(XmlInputToken.Sentinel(), result[3])
        assertEquals(XmlInputToken.Raw(createContentPart("foo", true)), result[4])
        assertEquals(XmlInputToken.TemplateGroup.create(templateGroups[1]), result[5])
        assertEquals(XmlInputToken.Sentinel(), result[6])
        assertEquals(XmlInputToken.Sentinel(), result[7])
        assertEquals(XmlInputToken.Sentinel(), result[8])
    }
}
