package com.docstencil.core.scanner

import com.docstencil.core.error.TemplaterException
import com.docstencil.core.scanner.model.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class XmlInputTokenTest {
    @Test
    fun `create should resolve nickname to actual tag name`() {
        val nicknames = mapOf(
            "inline" to "w:r",
            "block" to "w:p",
        )

        val group = TemplateTokenGroup(
            listOf(
                TemplateToken(TemplateTokenType.DELIMITER_OPEN, "{", null, null, ContentIdx(0)),
                TemplateToken(TemplateTokenType.AT, "@", null, null, ContentIdx(1)),
                TemplateToken(
                    TemplateTokenType.STRING,
                    "\"inline\"",
                    null,
                    "inline",
                    ContentIdx(2),
                ),
                TemplateToken(TemplateTokenType.FOR, "for", null, null, ContentIdx(11)),
                TemplateToken(TemplateTokenType.IDENTIFIER, "item", null, null, ContentIdx(15)),
                TemplateToken(TemplateTokenType.IN, "in", null, null, ContentIdx(20)),
                TemplateToken(TemplateTokenType.IDENTIFIER, "items", null, null, ContentIdx(23)),
                TemplateToken(TemplateTokenType.DELIMITER_CLOSE, "}", null, null, ContentIdx(28)),
            ),
            ContentIdx(0),
            ContentIdx(29),
        )

        val result = XmlInputToken.TemplateGroup.create(group, nicknames)

        assertEquals(XmlInputToken.ExpansionTarget.Tag("w:r"), result.expansionTarget)
    }

    @Test
    fun `create should treat unknown nickname as literal tag name`() {
        val nicknames = mapOf(
            "inline" to "w:r",
            "block" to "w:p",
        )

        val group = TemplateTokenGroup(
            listOf(
                TemplateToken(TemplateTokenType.DELIMITER_OPEN, "{", null, null, ContentIdx(0)),
                TemplateToken(TemplateTokenType.AT, "@", null, null, ContentIdx(1)),
                TemplateToken(
                    TemplateTokenType.STRING,
                    "\"customTag\"",
                    null,
                    "customTag",
                    ContentIdx(2),
                ),
                TemplateToken(TemplateTokenType.FOR, "for", null, null, ContentIdx(13)),
                TemplateToken(TemplateTokenType.IDENTIFIER, "item", null, null, ContentIdx(17)),
                TemplateToken(TemplateTokenType.IN, "in", null, null, ContentIdx(22)),
                TemplateToken(TemplateTokenType.IDENTIFIER, "items", null, null, ContentIdx(25)),
                TemplateToken(TemplateTokenType.DELIMITER_CLOSE, "}", null, null, ContentIdx(30)),
            ),
            ContentIdx(0),
            ContentIdx(31),
        )

        val result = XmlInputToken.TemplateGroup.create(group, nicknames)

        assertEquals(XmlInputToken.ExpansionTarget.Tag("customTag"), result.expansionTarget)
    }

    @Test
    fun `create should be case sensitive for nicknames`() {
        val nicknames = mapOf(
            "inline" to "w:r",
            "block" to "w:p",
        )

        val group = TemplateTokenGroup(
            listOf(
                TemplateToken(TemplateTokenType.DELIMITER_OPEN, "{", null, null, ContentIdx(0)),
                TemplateToken(TemplateTokenType.AT, "@", null, null, ContentIdx(1)),
                TemplateToken(
                    TemplateTokenType.STRING,
                    "\"Inline\"",
                    null,
                    "Inline",
                    ContentIdx(2),
                ),
                TemplateToken(TemplateTokenType.FOR, "for", null, null, ContentIdx(11)),
                TemplateToken(TemplateTokenType.IDENTIFIER, "item", null, null, ContentIdx(15)),
                TemplateToken(TemplateTokenType.IN, "in", null, null, ContentIdx(20)),
                TemplateToken(TemplateTokenType.IDENTIFIER, "items", null, null, ContentIdx(23)),
                TemplateToken(TemplateTokenType.DELIMITER_CLOSE, "}", null, null, ContentIdx(28)),
            ),
            ContentIdx(0),
            ContentIdx(29),
        )

        val result = XmlInputToken.TemplateGroup.create(group, nicknames)

        assertEquals(XmlInputToken.ExpansionTarget.Tag("Inline"), result.expansionTarget)
    }

    @Test
    fun `create should work with empty nicknames map`() {
        val nicknames = mapOf<String, String>()

        val group = TemplateTokenGroup(
            listOf(
                TemplateToken(TemplateTokenType.DELIMITER_OPEN, "{", null, null, ContentIdx(0)),
                TemplateToken(TemplateTokenType.AT, "@", null, null, ContentIdx(1)),
                TemplateToken(TemplateTokenType.STRING, "\"w:p\"", null, "w:p", ContentIdx(2)),
                TemplateToken(TemplateTokenType.FOR, "for", null, null, ContentIdx(7)),
                TemplateToken(TemplateTokenType.IDENTIFIER, "item", null, null, ContentIdx(11)),
                TemplateToken(TemplateTokenType.IN, "in", null, null, ContentIdx(16)),
                TemplateToken(TemplateTokenType.IDENTIFIER, "items", null, null, ContentIdx(19)),
                TemplateToken(TemplateTokenType.DELIMITER_CLOSE, "}", null, null, ContentIdx(24)),
            ),
            ContentIdx(0),
            ContentIdx(25),
        )

        val result = XmlInputToken.TemplateGroup.create(group, nicknames)

        assertEquals(XmlInputToken.ExpansionTarget.Tag("w:p"), result.expansionTarget)
    }

    @Test
    fun `create should throw error when @ has no tag name`() {
        val group = TemplateTokenGroup(
            listOf(
                TemplateToken(TemplateTokenType.DELIMITER_OPEN, "{", null, null, ContentIdx(0)),
                TemplateToken(TemplateTokenType.AT, "@", null, null, ContentIdx(1)),
                TemplateToken(TemplateTokenType.FOR, "for", null, null, ContentIdx(2)),
                TemplateToken(TemplateTokenType.IDENTIFIER, "item", null, null, ContentIdx(6)),
                TemplateToken(TemplateTokenType.IN, "in", null, null, ContentIdx(11)),
                TemplateToken(TemplateTokenType.IDENTIFIER, "items", null, null, ContentIdx(14)),
                TemplateToken(TemplateTokenType.DELIMITER_CLOSE, "}", null, null, ContentIdx(19)),
            ),
            ContentIdx(0),
            ContentIdx(20),
        )

        assertFailsWith<TemplaterException.ParseError> {
            XmlInputToken.TemplateGroup.create(group, mapOf())
        }
    }
}
