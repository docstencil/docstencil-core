package com.docstencil.core.scanner

import com.docstencil.core.config.FileTypeConfig
import com.docstencil.core.scanner.model.*
import kotlin.test.*

class XmlInputTokenExpanderTest {
    private fun createExpander(
        expansionRules: List<FileTypeConfig.ExpansionRule> = FileTypeConfig.docx().expansionRules,
        defaultTagRawXmlExpansion: String = "w:p",
        rewriteFnsDefaultTargets: Map<String, XmlInputToken.ExpansionTarget> = mapOf(),
    ) = TemplateXmlTokenExpander(
        expansionRules,
        defaultTagRawXmlExpansion,
        rewriteFnsDefaultTargets,
    )

    private fun forGroup() = TemplateTokenGroup(
        listOf(
            TemplateToken(TemplateTokenType.DELIMITER_OPEN, "{", null, null, ContentIdx(0)),
            TemplateToken(TemplateTokenType.FOR, "for", null, null, ContentIdx(1)),
            TemplateToken(TemplateTokenType.IDENTIFIER, "item", null, null, ContentIdx(5)),
            TemplateToken(TemplateTokenType.IN, "in", null, null, ContentIdx(10)),
            TemplateToken(TemplateTokenType.IDENTIFIER, "items", null, null, ContentIdx(13)),
            TemplateToken(TemplateTokenType.DELIMITER_CLOSE, "}", null, null, ContentIdx(18)),
        ),
        ContentIdx(0),
        ContentIdx(19),
    )

    private fun ifGroup() = TemplateTokenGroup(
        listOf(
            TemplateToken(TemplateTokenType.DELIMITER_OPEN, "{", null, null, ContentIdx(0)),
            TemplateToken(TemplateTokenType.IF, "if", null, null, ContentIdx(1)),
            TemplateToken(TemplateTokenType.IDENTIFIER, "condition", null, null, ContentIdx(4)),
            TemplateToken(TemplateTokenType.DELIMITER_CLOSE, "}", null, null, ContentIdx(13)),
        ),
        ContentIdx(0),
        ContentIdx(14),
    )

    private fun endGroup() = TemplateTokenGroup(
        listOf(
            TemplateToken(TemplateTokenType.DELIMITER_OPEN, "{", null, null, ContentIdx(0)),
            TemplateToken(TemplateTokenType.END, "end", null, null, ContentIdx(1)),
            TemplateToken(TemplateTokenType.DELIMITER_CLOSE, "}", null, null, ContentIdx(4)),
        ),
        ContentIdx(0),
        ContentIdx(5),
    )

    private fun placeholderGroup() = TemplateTokenGroup(
        listOf(
            TemplateToken(TemplateTokenType.DELIMITER_OPEN, "{", null, null, ContentIdx(0)),
            TemplateToken(TemplateTokenType.IDENTIFIER, "name", null, null, ContentIdx(1)),
            TemplateToken(TemplateTokenType.DELIMITER_CLOSE, "}", null, null, ContentIdx(5)),
        ),
        ContentIdx(0),
        ContentIdx(6),
    )

    private fun doGroup() = TemplateTokenGroup(
        listOf(
            TemplateToken(TemplateTokenType.DELIMITER_OPEN, "{", null, null, ContentIdx(0)),
            TemplateToken(TemplateTokenType.DO, "do", null, null, ContentIdx(1)),
            TemplateToken(TemplateTokenType.IDENTIFIER, "x", null, null, ContentIdx(4)),
            TemplateToken(TemplateTokenType.EQUAL, "=", null, null, ContentIdx(6)),
            TemplateToken(TemplateTokenType.INTEGER, "5", null, 5, ContentIdx(8)),
            TemplateToken(TemplateTokenType.DELIMITER_CLOSE, "}", null, null, ContentIdx(9)),
        ),
        ContentIdx(0),
        ContentIdx(10),
    )

    private fun forToken(expansionTarget: XmlInputToken.ExpansionTarget = XmlInputToken.ExpansionTarget.Auto()): XmlInputToken.TemplateGroup {
        val templateGroup =
            XmlInputToken.TemplateGroup.create(forGroup(), FileTypeConfig.docx().tagNicknames)
        templateGroup.expansionTarget = expansionTarget
        return templateGroup
    }

    private fun ifToken(expansionTarget: XmlInputToken.ExpansionTarget = XmlInputToken.ExpansionTarget.Auto()): XmlInputToken.TemplateGroup {
        val templateGroup =
            XmlInputToken.TemplateGroup.create(ifGroup(), FileTypeConfig.docx().tagNicknames)
        templateGroup.expansionTarget = expansionTarget
        return templateGroup
    }

    private fun endToken() =
        XmlInputToken.TemplateGroup.create(endGroup(), FileTypeConfig.docx().tagNicknames)

    private fun placeholderToken() =
        XmlInputToken.TemplateGroup.create(
            placeholderGroup(),
            FileTypeConfig.docx().tagNicknames,
        )

    private fun doToken() =
        XmlInputToken.TemplateGroup.create(doGroup(), FileTypeConfig.docx().tagNicknames)

    private fun tag(name: String, type: TagPartType, textTag: Boolean = false) =
        XmlInputToken.Raw(
            XmlRawInputToken.TagPart(
                name,
                0,
                when (type) {
                    TagPartType.OPENING -> "<$name>"
                    TagPartType.CLOSING -> "</$name>"
                    TagPartType.SELF_CLOSING -> "<$name/>"
                },
                type,
                textTag,
            ),
        )

    private fun content(text: String, insideTextTag: Boolean = true) =
        XmlInputToken.Raw(XmlRawInputToken.Content(0, text, insideTextTag))

    private fun sentinel() = XmlInputToken.Sentinel()

    private fun assertPaired(
        left: XmlInputToken.TemplateGroup,
        right: XmlInputToken.TemplateGroup,
    ) {
        assertSame(left.partner, right, "Left token partner should be right token")
        assertSame(right.partner, left, "Right token partner should be left token")
    }

    private fun assertRawXmlTag(
        token: XmlInputToken,
        tagName: String,
        tagPartType: TagPartType,
    ) {
        assertTrue(token is XmlInputToken.Raw, "Expected RawXml token")
        assertTrue(token.xml is XmlRawInputToken.TagPart, "Expected TagPart")
        assertEquals(tagName, token.xml.tagName, "Tag name mismatch")
        assertEquals(tagPartType, token.xml.tagPartType, "Tag type mismatch")
    }

    private fun assertRawXmlContent(
        token: XmlInputToken,
        expectedText: String,
    ) {
        assertTrue(token is XmlInputToken.Raw, "Expected RawXml token")
        assertTrue(token.xml is XmlRawInputToken.Content, "Expected ContentPart")
        assertEquals(
            expectedText,
            (token.xml as XmlRawInputToken.Content).text,
            "Content text mismatch",
        )
    }

    @Test
    fun `expand should pair single for loop`() {
        val forToken = forToken()
        val endToken = endToken()
        val tokens = listOf(
            tag("w:p", TagPartType.OPENING),
            forToken,
            content("Content"),
            endToken,
            tag("w:p", TagPartType.CLOSING),
        )

        val result = createExpander().expand(tokens)

        assertEquals(5, result.size)

        assertRawXmlTag(result[0], "w:p", TagPartType.OPENING)
        assertSame(forToken, result[1])
        assertRawXmlContent(result[2], "Content")
        assertSame(endToken, result[3])
        assertRawXmlTag(result[4], "w:p", TagPartType.CLOSING)

        assertTrue(forToken.expansionTarget is XmlInputToken.ExpansionTarget.None)
        assertTrue(endToken.expansionTarget is XmlInputToken.ExpansionTarget.None)

        assertPaired(forToken, endToken)
    }

    @Test
    fun `expand should expand single for loop`() {
        val forToken = forToken(XmlInputToken.ExpansionTarget.Tag("w:p"))
        val endToken = endToken()
        val tokens = listOf(
            tag("w:p", TagPartType.OPENING),
            forToken,
            content("Content"),
            endToken,
            tag("w:p", TagPartType.CLOSING),
        )

        val result = createExpander().expand(tokens)

        assertEquals(5, result.size)

        assertSame(forToken, result[0])
        assertRawXmlTag(result[1], "w:p", TagPartType.OPENING)
        assertRawXmlContent(result[2], "Content")
        assertRawXmlTag(result[3], "w:p", TagPartType.CLOSING)
        assertSame(endToken, result[4])

        assertEquals(XmlInputToken.ExpansionTarget.Tag("w:p"), forToken.expansionTarget)
        assertEquals(XmlInputToken.ExpansionTarget.Tag("w:p"), endToken.expansionTarget)

        assertPaired(forToken, endToken)
    }

    @Test
    fun `expand should pair multiple consecutive pairs`() {
        val for1 = forToken(XmlInputToken.ExpansionTarget.Tag("w:p"))
        val end1 = endToken()
        val for2 = forToken()
        val end2 = endToken()
        val tokens = listOf(
            tag("w:p", TagPartType.OPENING),
            for1,
            content("A"),
            end1,
            tag("w:p", TagPartType.CLOSING),
            tag("w:p", TagPartType.OPENING),
            for2,
            content("B"),
            end2,
            tag("w:p", TagPartType.CLOSING),
        )

        val result = createExpander().expand(tokens)

        assertEquals(10, result.size)

        assertSame(for1, result[0])
        assertRawXmlTag(result[1], "w:p", TagPartType.OPENING)
        assertRawXmlContent(result[2], "A")
        assertRawXmlTag(result[3], "w:p", TagPartType.CLOSING)
        assertSame(end1, result[4])
        assertRawXmlTag(result[5], "w:p", TagPartType.OPENING)
        assertSame(for2, result[6])
        assertRawXmlContent(result[7], "B")
        assertSame(end2, result[8])
        assertRawXmlTag(result[9], "w:p", TagPartType.CLOSING)

        assertPaired(for1, end1)
        assertPaired(for2, end2)
    }

    @Test
    fun `expand handle nested for loops`() {
        val outerFor = forToken(XmlInputToken.ExpansionTarget.Tag("w:p"))
        val innerFor = forToken(XmlInputToken.ExpansionTarget.Tag("w:tc"))
        val innerEnd = endToken()
        val outerEnd = endToken()
        val tokens = listOf(
            tag("w:p", TagPartType.OPENING),
            outerFor,
            innerFor,
            content("Content"),
            innerEnd,
            outerEnd,
            tag("w:p", TagPartType.CLOSING),
        )

        val result = createExpander().expand(tokens)

        assertEquals(7, result.size)

        assertSame(outerFor, result[0])
        assertSame(innerFor, result[1])
        assertRawXmlTag(result[2], "w:p", TagPartType.OPENING)
        assertRawXmlContent(result[3], "Content")
        assertRawXmlTag(result[4], "w:p", TagPartType.CLOSING)
        assertSame(innerEnd, result[5])
        assertSame(outerEnd, result[6])

        assertPaired(outerFor, outerEnd)
        assertPaired(innerFor, innerEnd)
    }

    @Test
    fun `expand should throw error for unpaired opening tag`() {
        val forToken = forToken()
        val tokens = listOf(
            tag("w:p", TagPartType.OPENING),
            forToken,
            content("Content"),
            tag("w:p", TagPartType.CLOSING),
        )

        assertFailsWith<RuntimeException> {
            createExpander().expand(tokens)
        }
    }

    @Test
    fun `expand should throw error for unpaired closing tag`() {
        val endToken = endToken()
        val tokens = listOf(
            tag("w:p", TagPartType.OPENING),
            content("Content"),
            endToken,
            tag("w:p", TagPartType.CLOSING),
        )

        assertFailsWith<RuntimeException> {
            createExpander().expand(tokens)
        }
    }

    @Test
    fun `expand should set expansion target with single rule match`() {
        val ifToken = ifToken()
        val endToken = endToken()
        val tokens = listOf(
            tag("w:p", TagPartType.OPENING),
            ifToken,
            tag("w:p", TagPartType.CLOSING),
            tag("w:p", TagPartType.OPENING),
            content("Content"),
            endToken,
            tag("w:p", TagPartType.CLOSING),
        )

        val result = createExpander().expand(tokens)

        assertEquals(7, result.size)

        assertSame(ifToken, result[0])
        assertRawXmlTag(result[1], "w:p", TagPartType.OPENING)
        assertRawXmlTag(result[2], "w:p", TagPartType.CLOSING)
        assertRawXmlTag(result[3], "w:p", TagPartType.OPENING)
        assertRawXmlContent(result[4], "Content")
        assertRawXmlTag(result[5], "w:p", TagPartType.CLOSING)
        assertSame(endToken, result[6])

        assertEquals(ifToken.expansionTarget, XmlInputToken.ExpansionTarget.Tag("w:p"))
        assertEquals(endToken.expansionTarget, XmlInputToken.ExpansionTarget.Tag("w:p"))

        assertPaired(ifToken, endToken)
    }

    @Test
    fun `expand should use last matching rule when multiple rules match`() {
        val expansionRules = listOf(
            FileTypeConfig.ExpansionRule("w:tc", null, "w:tr"),
            FileTypeConfig.ExpansionRule("w:p", null, "w:p"),
        )

        val forToken = forToken()
        val endToken = endToken()
        val tokens = listOf(
            tag("w:tr", TagPartType.OPENING),
            tag("w:tc", TagPartType.OPENING),
            tag("w:p", TagPartType.OPENING),
            forToken,
            tag("w:tr", TagPartType.OPENING),
            tag("w:tc", TagPartType.OPENING),
            tag("w:p", TagPartType.OPENING),
            content("Content"),
            tag("w:p", TagPartType.CLOSING),
            tag("w:tc", TagPartType.CLOSING),
            tag("w:tr", TagPartType.CLOSING),
            endToken,
            tag("w:p", TagPartType.CLOSING),
            tag("w:tc", TagPartType.CLOSING),
            tag("w:tr", TagPartType.CLOSING),
        )

        val result = createExpander(expansionRules = expansionRules).expand(tokens)

        assertEquals(15, result.size)

        assertRawXmlTag(result[0], "w:tr", TagPartType.OPENING)
        assertRawXmlTag(result[1], "w:tc", TagPartType.OPENING)
        assertSame(forToken, result[2])
        assertRawXmlTag(result[3], "w:p", TagPartType.OPENING)
        assertRawXmlTag(result[4], "w:tr", TagPartType.OPENING)
        assertRawXmlTag(result[5], "w:tc", TagPartType.OPENING)
        assertRawXmlTag(result[6], "w:p", TagPartType.OPENING)
        assertRawXmlContent(result[7], "Content")
        assertRawXmlTag(result[8], "w:p", TagPartType.CLOSING)
        assertRawXmlTag(result[9], "w:tc", TagPartType.CLOSING)
        assertRawXmlTag(result[10], "w:tr", TagPartType.CLOSING)
        assertRawXmlTag(result[11], "w:p", TagPartType.CLOSING)
        assertSame(endToken, result[12])
        assertRawXmlTag(result[13], "w:tc", TagPartType.CLOSING)
        assertRawXmlTag(result[14], "w:tr", TagPartType.CLOSING)

        assertEquals(forToken.expansionTarget, XmlInputToken.ExpansionTarget.Tag("w:p"))
        assertEquals(endToken.expansionTarget, XmlInputToken.ExpansionTarget.Tag("w:p"))

        assertPaired(forToken, endToken)
    }

    @Test
    fun `expand should respect explicit expansion target from expand syntax`() {
        val forToken = forToken().apply {
            expansionTarget = XmlInputToken.ExpansionTarget.Tag("w:tr")
        }
        val endToken = endToken()
        val tokens = listOf(
            tag("w:tr", TagPartType.OPENING),
            tag("w:tc", TagPartType.OPENING),
            tag("w:p", TagPartType.OPENING),
            forToken,
            content("Content"),
            endToken,
            tag("w:p", TagPartType.CLOSING),
            tag("w:tc", TagPartType.CLOSING),
            tag("w:tr", TagPartType.CLOSING),
        )

        val result = createExpander().expand(tokens)

        assertEquals(9, result.size)

        assertSame(forToken, result[0])
        assertRawXmlTag(result[1], "w:tr", TagPartType.OPENING)
        assertRawXmlTag(result[2], "w:tc", TagPartType.OPENING)
        assertRawXmlTag(result[3], "w:p", TagPartType.OPENING)
        assertRawXmlContent(result[4], "Content")
        assertRawXmlTag(result[5], "w:p", TagPartType.CLOSING)
        assertRawXmlTag(result[6], "w:tc", TagPartType.CLOSING)
        assertRawXmlTag(result[7], "w:tr", TagPartType.CLOSING)
        assertSame(endToken, result[8])

        assertEquals(XmlInputToken.ExpansionTarget.Tag("w:tr"), forToken.expansionTarget)
        assertEquals(XmlInputToken.ExpansionTarget.Tag("w:tr"), endToken.expansionTarget)

        assertPaired(forToken, endToken)
    }

    @Test
    fun `expand should throw error when cannot expand further left`() {
        val forToken = forToken().apply {
            expansionTarget = XmlInputToken.ExpansionTarget.Tag("w:p")
        }
        val endToken = endToken()
        val tokens = listOf(content("Text"), forToken, content("More"), endToken)

        assertFailsWith<RuntimeException> {
            createExpander().expand(tokens)
        }
    }

    @Test
    fun `expand should handle nested loops each expanding correctly`() {
        val outerFor = forToken()
        val innerFor = forToken()
        val innerEnd = endToken()
        val outerEnd = endToken()
        val tokens = listOf(
            tag("w:tbl", TagPartType.OPENING),
            tag("w:tr", TagPartType.OPENING),
            tag("w:tc", TagPartType.OPENING),
            tag("w:p", TagPartType.OPENING),
            outerFor,
            innerFor,
            content("Content"),
            innerEnd,
            outerEnd,
            tag("w:p", TagPartType.CLOSING),
            tag("w:tc", TagPartType.CLOSING),
            tag("w:tr", TagPartType.CLOSING),
            tag("w:tbl", TagPartType.CLOSING),
        )

        val result = createExpander().expand(tokens)

        assertEquals(13, result.size)
        assertRawXmlTag(result[0], "w:tbl", TagPartType.OPENING)
        assertRawXmlTag(result[1], "w:tr", TagPartType.OPENING)
        assertRawXmlTag(result[2], "w:tc", TagPartType.OPENING)
        assertRawXmlTag(result[3], "w:p", TagPartType.OPENING)
        assertSame(outerFor, result[4])
        assertSame(innerFor, result[5])
        assertRawXmlContent(result[6], "Content")
        assertSame(innerEnd, result[7])
        assertSame(outerEnd, result[8])
        assertRawXmlTag(result[9], "w:p", TagPartType.CLOSING)
        assertRawXmlTag(result[10], "w:tc", TagPartType.CLOSING)
        assertRawXmlTag(result[11], "w:tr", TagPartType.CLOSING)
        assertRawXmlTag(result[12], "w:tbl", TagPartType.CLOSING)
        assertPaired(outerFor, outerEnd)
        assertPaired(innerFor, innerEnd)
    }

    @Test
    fun `expand should handle nested for loops`() {
        val expansionRules = listOf(FileTypeConfig.ExpansionRule("w:tc", null, "w:tr"))

        val forToken1 = forToken()
        val endToken1 = endToken()
        val forToken2 = forToken()
        val endToken2 = endToken()
        val tokens = listOf(
            tag("w:tbl", TagPartType.OPENING),
            tag("w:tr", TagPartType.OPENING),
            tag("w:tc", TagPartType.OPENING),
            forToken1,
            forToken2,
            content("Content1"),
            tag("w:tc", TagPartType.CLOSING),
            tag("w:tc", TagPartType.OPENING),
            content("Content2"),
            endToken2,
            endToken1,
            tag("w:tc", TagPartType.CLOSING),
            tag("w:tr", TagPartType.CLOSING),
            tag("w:tbl", TagPartType.CLOSING),
        )

        val result = createExpander(expansionRules = expansionRules).expand(tokens)

        assertEquals(14, result.size)

        assertRawXmlTag(result[0], "w:tbl", TagPartType.OPENING)
        assertSame(forToken1, result[1])
        assertSame(forToken2, result[2])
        assertRawXmlTag(result[3], "w:tr", TagPartType.OPENING)
        assertRawXmlTag(result[4], "w:tc", TagPartType.OPENING)
        assertRawXmlContent(result[5], "Content1")
        assertRawXmlTag(result[6], "w:tc", TagPartType.CLOSING)
        assertRawXmlTag(result[7], "w:tc", TagPartType.OPENING)
        assertRawXmlContent(result[8], "Content2")
        assertRawXmlTag(result[9], "w:tc", TagPartType.CLOSING)
        assertRawXmlTag(result[10], "w:tr", TagPartType.CLOSING)
        assertSame(endToken2, result[11])
        assertSame(endToken1, result[12])
        assertRawXmlTag(result[13], "w:tbl", TagPartType.CLOSING)

        assertEquals(forToken1.expansionTarget, XmlInputToken.ExpansionTarget.Tag("w:tr"))
        assertEquals(endToken1.expansionTarget, XmlInputToken.ExpansionTarget.Tag("w:tr"))
        assertEquals(forToken2.expansionTarget, XmlInputToken.ExpansionTarget.Tag("w:tr"))
        assertEquals(endToken2.expansionTarget, XmlInputToken.ExpansionTarget.Tag("w:tr"))

        assertPaired(forToken1, endToken1)
        assertPaired(forToken2, endToken2)
    }

    @Test
    fun `create should detect do statement and set Leftmost target`() {
        val doToken = doToken()

        assertEquals(XmlInputToken.ExpansionDirection.LEFT, doToken.expansionDirection)
        assertEquals(XmlInputToken.ExpansionTarget.Outermost(), doToken.expansionTarget)
        assertFalse(doToken.requiresPartner)
    }

    @Test
    fun `expand should move do statement to leftmost position`() {
        val doToken = doToken()
        val tokens = listOf(
            tag("w:p", TagPartType.OPENING),
            content("Text"),
            doToken,
            tag("w:p", TagPartType.CLOSING),
        )

        val result = createExpander().expand(tokens)

        assertEquals(4, result.size)

        assertSame(doToken, result[0])
        assertRawXmlTag(result[1], "w:p", TagPartType.OPENING)
        assertRawXmlContent(result[2], "Text")
        assertRawXmlTag(result[3], "w:p", TagPartType.CLOSING)
    }

    @Test
    fun `expand should stop do statement at blocking template group`() {
        val placeholder = placeholderToken()
        val doToken = doToken()
        val tokens = listOf(
            tag("w:p", TagPartType.OPENING),
            placeholder,
            content("Text"),
            doToken,
            tag("w:p", TagPartType.CLOSING),
        )

        val result = createExpander().expand(tokens)

        assertEquals(5, result.size)

        assertRawXmlTag(result[0], "w:p", TagPartType.OPENING)
        assertSame(placeholder, result[1])
        assertSame(doToken, result[2])
        assertRawXmlContent(result[3], "Text")
        assertRawXmlTag(result[4], "w:p", TagPartType.CLOSING)
    }

    @Test
    fun `expand should stop do statement at document beginning`() {
        val doToken = doToken()
        val tokens = listOf(
            doToken,
            tag("w:p", TagPartType.OPENING),
            content("Text"),
            tag("w:p", TagPartType.CLOSING),
        )

        val result = createExpander().expand(tokens)

        assertEquals(4, result.size)

        assertSame(doToken, result[0])
        assertRawXmlTag(result[1], "w:p", TagPartType.OPENING)
        assertRawXmlContent(result[2], "Text")
        assertRawXmlTag(result[3], "w:p", TagPartType.CLOSING)
    }

    @Test
    fun `expand should handle do statement moving to document start`() {
        val doToken = doToken()
        val tokens = listOf(
            tag("w:p", TagPartType.OPENING),
            content("Text"),
            doToken,
            tag("w:p", TagPartType.CLOSING),
        )

        val result = createExpander().expand(tokens)

        assertEquals(4, result.size)

        assertSame(doToken, result[0])
        assertRawXmlTag(result[1], "w:p", TagPartType.OPENING)
        assertRawXmlContent(result[2], "Text")
        assertRawXmlTag(result[3], "w:p", TagPartType.CLOSING)
    }

    @Test
    fun `expand should handle for loop with target and do statement`() {
        val forToken = forToken(XmlInputToken.ExpansionTarget.Tag("w:p"))
        val doToken = doToken()
        val endToken = endToken()
        val tokens = listOf(
            tag("w:p", TagPartType.OPENING),
            forToken,
            content("some text 1"),
            doToken,
            content("some text 2"),
            endToken,
            tag("w:p", TagPartType.CLOSING),
        )

        val result = createExpander().expand(tokens)

        assertEquals(7, result.size)

        assertSame(forToken, result[0])
        assertSame(doToken, result[1])
        assertRawXmlTag(result[2], "w:p", TagPartType.OPENING)
        assertRawXmlContent(result[3], "some text 1")
        assertRawXmlContent(result[4], "some text 2")
        assertRawXmlTag(result[5], "w:p", TagPartType.CLOSING)
        assertSame(endToken, result[6])

        assertPaired(forToken, endToken)
    }

    @Test
    fun `expand should handle if with at-target`() {
        val ifToken = ifToken(XmlInputToken.ExpansionTarget.Tag("w:p"))
        val endToken = endToken()
        val tokens = listOf(
            tag("w:p", TagPartType.OPENING),
            ifToken,
            content("Content"),
            endToken,
            tag("w:p", TagPartType.CLOSING),
        )

        val result = createExpander().expand(tokens)

        assertEquals(5, result.size)

        assertSame(ifToken, result[0])
        assertRawXmlTag(result[1], "w:p", TagPartType.OPENING)
        assertRawXmlContent(result[2], "Content")
        assertRawXmlTag(result[3], "w:p", TagPartType.CLOSING)
        assertSame(endToken, result[4])

        assertEquals(XmlInputToken.ExpansionTarget.Tag("w:p"), ifToken.expansionTarget)
        assertEquals(XmlInputToken.ExpansionTarget.Tag("w:p"), endToken.expansionTarget)

        assertPaired(ifToken, endToken)
    }
}
