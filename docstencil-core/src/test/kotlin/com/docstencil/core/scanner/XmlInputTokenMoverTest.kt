package com.docstencil.core.scanner

import com.docstencil.core.config.FileTypeConfig
import com.docstencil.core.scanner.model.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

class XmlInputTokenMoverTest {
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

    private fun endGroup() = TemplateTokenGroup(
        listOf(
            TemplateToken(TemplateTokenType.DELIMITER_OPEN, "{", null, null, ContentIdx(0)),
            TemplateToken(TemplateTokenType.END, "end", null, null, ContentIdx(1)),
            TemplateToken(TemplateTokenType.DELIMITER_CLOSE, "}", null, null, ContentIdx(4)),
        ),
        ContentIdx(0),
        ContentIdx(5),
    )

    private fun forToken(): XmlInputToken.TemplateGroup {
        return XmlInputToken.TemplateGroup.create(forGroup(), FileTypeConfig.docx().tagNicknames)
    }

    private fun endToken(): XmlInputToken.TemplateGroup {
        return XmlInputToken.TemplateGroup.create(endGroup(), FileTypeConfig.docx().tagNicknames)
    }

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
                type, textTag,
            ),
        )

    private fun content(text: String, insideTextTag: Boolean = true) =
        XmlInputToken.Raw(XmlRawInputToken.Content(0, text, insideTextTag))

    @Test
    fun `moveTokenToTarget should move token left to target position`() {
        val mover = TemplateXmlTokenMover()
        val openingTag = tag("w:p", TagPartType.OPENING)
        val forToken = forToken()
        val contentToken = content("text")
        val closingTag = tag("w:p", TagPartType.CLOSING)
        val tokens: MutableList<XmlInputToken> = mutableListOf(
            openingTag,
            forToken,
            contentToken,
            closingTag,
        )

        mover.moveTokenToTarget(tokens, 1, "w:p", TemplateXmlTokenMover.ExpansionConfig.forLeft())

        assertEquals(4, tokens.size)

        assertSame(forToken, tokens[0])
        assertSame(openingTag, tokens[1])
        assertSame(contentToken, tokens[2])
        assertSame(closingTag, tokens[3])
    }

    @Test
    fun `moveTokenToTarget should move token right to target position at end of list`() {
        val mover = TemplateXmlTokenMover()
        val openingTag = tag("w:p", TagPartType.OPENING)
        val contentToken = content("text")
        val endToken = endToken()
        val closingTag = tag("w:p", TagPartType.CLOSING)
        val tokens: MutableList<XmlInputToken> = mutableListOf(
            openingTag,
            contentToken,
            endToken,
            closingTag,
        )

        mover.moveTokenToTarget(tokens, 2, "w:p", TemplateXmlTokenMover.ExpansionConfig.forRight())

        assertEquals(4, tokens.size)

        assertSame(openingTag, tokens[0])
        assertSame(contentToken, tokens[1])
        assertSame(closingTag, tokens[2])
        assertSame(endToken, tokens[3])
    }
}
