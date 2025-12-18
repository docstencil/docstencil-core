package com.docstencil.core.scanner

import com.docstencil.core.scanner.model.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class TemplateTokenTemplateGroupFixerTest {
    private val fixer = TemplateTokenGroupFixer()

    @Test
    fun `fix should add end token after basic insert statement`() {
        val insertTokens = listOf(
            TemplateToken(TemplateTokenType.DELIMITER_OPEN, "{", null, null, ContentIdx(0)),
            TemplateToken(TemplateTokenType.INSERT, "insert", null, null, ContentIdx(1)),
            TemplateToken(TemplateTokenType.IDENTIFIER, "foo", null, null, ContentIdx(8)),
            TemplateToken(TemplateTokenType.DELIMITER_CLOSE, "}", null, null, ContentIdx(11)),
        )
        val insertGroup = TemplateTokenGroup(insertTokens, ContentIdx(0), ContentIdx(12))
        val insertTemplateTemplateGroup = XmlInputToken.TemplateGroup.create(insertGroup)

        val tokens = listOf(insertTemplateTemplateGroup)

        val result = fixer.fix(tokens)

        assertEquals(2, result.size, "Should have 2 tokens: insert and fake end")

        val firstToken = result[0] as XmlInputToken.TemplateGroup
        assertEquals(
            insertTemplateTemplateGroup,
            firstToken,
            "First token should be the original insert",
        )

        val secondToken = result[1] as XmlInputToken.TemplateGroup
        val endTokens = secondToken.tokens.tokens
        assertEquals(3, endTokens.size, "End group should have 3 tokens: { end }")
        assertEquals(TemplateTokenType.DELIMITER_OPEN, endTokens[0].type)
        assertEquals(TemplateTokenType.END, endTokens[1].type)
        assertEquals(TemplateTokenType.DELIMITER_CLOSE, endTokens[2].type)
    }

    @Test
    fun `fix should add end token after insert with target`() {
        val insertTokens = listOf(
            TemplateToken(TemplateTokenType.DELIMITER_OPEN, "{", null, null, ContentIdx(0)),
            TemplateToken(TemplateTokenType.AT, "@", null, null, ContentIdx(1)),
            TemplateToken(TemplateTokenType.STRING, "\"w:p\"", null, "w:p", ContentIdx(2)),
            TemplateToken(TemplateTokenType.INSERT, "insert", null, null, ContentIdx(8)),
            TemplateToken(TemplateTokenType.IDENTIFIER, "foo", null, null, ContentIdx(15)),
            TemplateToken(TemplateTokenType.DELIMITER_CLOSE, "}", null, null, ContentIdx(18)),
        )
        val insertGroup = TemplateTokenGroup(insertTokens, ContentIdx(0), ContentIdx(19))
        val insertTemplateTemplateGroup = XmlInputToken.TemplateGroup.create(insertGroup)

        val tokens = listOf(insertTemplateTemplateGroup)

        val result = fixer.fix(tokens)

        assertEquals(2, result.size, "Should have 2 tokens: insert and fake end")

        val secondToken = result[1] as XmlInputToken.TemplateGroup
        val endTokens = secondToken.tokens.tokens
        assertEquals(TemplateTokenType.END, endTokens[1].type, "Second token should be END")
    }

    @Test
    fun `fix should not add end token after non-insert statements`() {
        val rewriteTokens = listOf(
            TemplateToken(TemplateTokenType.DELIMITER_OPEN, "{", null, null, ContentIdx(0)),
            TemplateToken(TemplateTokenType.REWRITE, "rewrite", null, null, ContentIdx(1)),
            TemplateToken(TemplateTokenType.IDENTIFIER, "foo", null, null, ContentIdx(9)),
            TemplateToken(TemplateTokenType.DELIMITER_CLOSE, "}", null, null, ContentIdx(12)),
        )
        val rewriteGroup = TemplateTokenGroup(rewriteTokens, ContentIdx(0), ContentIdx(13))
        val rewriteTemplateTemplateGroup = XmlInputToken.TemplateGroup.create(rewriteGroup)

        val tokens = listOf(rewriteTemplateTemplateGroup)

        val result = fixer.fix(tokens)

        assertEquals(1, result.size, "Should have only 1 token: the original rewrite")
        assertEquals(rewriteTemplateTemplateGroup, result[0])
    }

    @Test
    fun `fix should handle multiple insert statements`() {
        val insert1Tokens = listOf(
            TemplateToken(TemplateTokenType.DELIMITER_OPEN, "{", null, null, ContentIdx(0)),
            TemplateToken(TemplateTokenType.INSERT, "insert", null, null, ContentIdx(1)),
            TemplateToken(TemplateTokenType.IDENTIFIER, "foo", null, null, ContentIdx(8)),
            TemplateToken(TemplateTokenType.DELIMITER_CLOSE, "}", null, null, ContentIdx(11)),
        )
        val insert1Group = TemplateTokenGroup(insert1Tokens, ContentIdx(0), ContentIdx(12))
        val insert1TemplateTemplateGroup = XmlInputToken.TemplateGroup.create(insert1Group)

        val insert2Tokens = listOf(
            TemplateToken(TemplateTokenType.DELIMITER_OPEN, "{", null, null, ContentIdx(12)),
            TemplateToken(TemplateTokenType.INSERT, "insert", null, null, ContentIdx(13)),
            TemplateToken(TemplateTokenType.IDENTIFIER, "bar", null, null, ContentIdx(20)),
            TemplateToken(TemplateTokenType.DELIMITER_CLOSE, "}", null, null, ContentIdx(23)),
        )
        val insert2Group = TemplateTokenGroup(insert2Tokens, ContentIdx(12), ContentIdx(24))
        val insert2TemplateTemplateGroup = XmlInputToken.TemplateGroup.create(insert2Group)

        val tokens = listOf(insert1TemplateTemplateGroup, insert2TemplateTemplateGroup)

        val result = fixer.fix(tokens)

        assertEquals(4, result.size, "Should have 4 tokens: insert1, end1, insert2, end2")
        assertTrue(result[0] is XmlInputToken.TemplateGroup)
        assertTrue(result[1] is XmlInputToken.TemplateGroup)
        assertTrue(result[2] is XmlInputToken.TemplateGroup)
        assertTrue(result[3] is XmlInputToken.TemplateGroup)

        val end1Tokens = (result[1] as XmlInputToken.TemplateGroup).tokens.tokens
        assertEquals(TemplateTokenType.END, end1Tokens[1].type)

        val end2Tokens = (result[3] as XmlInputToken.TemplateGroup).tokens.tokens
        assertEquals(TemplateTokenType.END, end2Tokens[1].type)
    }
}
