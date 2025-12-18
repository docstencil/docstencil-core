package com.docstencil.core.scanner

import com.docstencil.core.api.OfficeTemplateOptions
import com.docstencil.core.error.TemplaterException
import com.docstencil.core.scanner.model.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class TemplateTokenTemplateGroupScannerTest {
    private fun createScanner(
        delimiters: OfficeTemplateOptions.Delimiters = OfficeTemplateOptions.Delimiters("{", "}"),
    ): TemplateTokenGroupScanner {
        return TemplateTokenGroupScanner(delimiters)
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

    private fun createToken(
        type: TemplateTokenType,
        lexeme: String,
        startIdx: Int,
        literal: Any? = null,
    ): TemplateToken {
        return TemplateToken(type, lexeme, null, literal, ContentIdx(startIdx))
    }

    private fun createGroup(
        tokens: List<TemplateToken>,
        startIdx: Int,
        endIdx: Int,
    ): TemplateTokenGroup {
        return TemplateTokenGroup(tokens, ContentIdx(startIdx), ContentIdx(endIdx))
    }

    @Test
    fun `scan should detect closing delimiter within group`() {
        val input = listOf(createContentPart("{firstName}"))
        val scanner = createScanner()

        val groups = scanner.scan(input)

        assertEquals(1, groups.size)
        assertEquals(
            createGroup(
                listOf(
                    createToken(TemplateTokenType.DELIMITER_OPEN, "{", 0),
                    createToken(TemplateTokenType.IDENTIFIER, "firstName", 1),
                    createToken(TemplateTokenType.DELIMITER_CLOSE, "}", 10),
                ),
                0, 11,
            ),
            groups[0],
        )
    }

    @Test
    fun `scan should handle empty template expression`() {
        val input = listOf(createContentPart("{}"))
        val scanner = createScanner()

        val groups = scanner.scan(input)

        assertEquals(1, groups.size)
        assertEquals(
            createGroup(
                listOf(
                    createToken(TemplateTokenType.DELIMITER_OPEN, "{", 0),
                    createToken(TemplateTokenType.DELIMITER_CLOSE, "}", 1),
                ),
                0, 2,
            ),
            groups[0],
        )
    }

    @Test
    fun `scan should create multiple groups for multiple expressions`() {
        val input = listOf(createContentPart("{firstName}{lastName}"))
        val scanner = createScanner()

        val groups = scanner.scan(input)

        assertEquals(2, groups.size)
        assertEquals(
            createGroup(
                listOf(
                    createToken(TemplateTokenType.DELIMITER_OPEN, "{", 0),
                    createToken(TemplateTokenType.IDENTIFIER, "firstName", 1),
                    createToken(TemplateTokenType.DELIMITER_CLOSE, "}", 10),
                ),
                0, 11,
            ),
            groups[0],
        )
        assertEquals(
            createGroup(
                listOf(
                    createToken(TemplateTokenType.DELIMITER_OPEN, "{", 11),
                    createToken(TemplateTokenType.IDENTIFIER, "lastName", 12),
                    createToken(TemplateTokenType.DELIMITER_CLOSE, "}", 20),
                ),
                11, 21,
            ),
            groups[1],
        )
    }

    @Test
    fun `scan should handle custom delimiters`() {
        val input = listOf(createContentPart("{{firstName}}"))
        val scanner = createScanner(
            delimiters = OfficeTemplateOptions.Delimiters("{{", "}}"),
        )

        val groups = scanner.scan(input)

        assertEquals(1, groups.size)
        assertEquals(
            createGroup(
                listOf(
                    createToken(TemplateTokenType.DELIMITER_OPEN, "{{", 0),
                    createToken(TemplateTokenType.IDENTIFIER, "firstName", 2),
                    createToken(TemplateTokenType.DELIMITER_CLOSE, "}}", 11),
                ),
                0, 13,
            ),
            groups[0],
        )
    }

    @Test
    fun `scan should recognize parentheses within group`() {
        val input = listOf(createContentPart("{(firstName)}"))
        val scanner = createScanner()

        val groups = scanner.scan(input)

        assertEquals(1, groups.size)
        assertEquals(
            createGroup(
                listOf(
                    createToken(TemplateTokenType.DELIMITER_OPEN, "{", 0),
                    createToken(TemplateTokenType.LEFT_PAREN, "(", 1),
                    createToken(TemplateTokenType.IDENTIFIER, "firstName", 2),
                    createToken(TemplateTokenType.RIGHT_PAREN, ")", 11),
                    createToken(TemplateTokenType.DELIMITER_CLOSE, "}", 12),
                ),
                0, 13,
            ),
            groups[0],
        )
    }

    @Test
    fun `scan should recognize arithmetic operators`() {
        val input = listOf(createContentPart("{a + b - c * d / e % f}"))
        val scanner = createScanner()

        val groups = scanner.scan(input)

        assertEquals(1, groups.size)
        assertEquals(
            createGroup(
                listOf(
                    createToken(TemplateTokenType.DELIMITER_OPEN, "{", 0),
                    createToken(TemplateTokenType.IDENTIFIER, "a", 1),
                    createToken(TemplateTokenType.PLUS, "+", 3),
                    createToken(TemplateTokenType.IDENTIFIER, "b", 5),
                    createToken(TemplateTokenType.MINUS, "-", 7),
                    createToken(TemplateTokenType.IDENTIFIER, "c", 9),
                    createToken(TemplateTokenType.STAR, "*", 11),
                    createToken(TemplateTokenType.IDENTIFIER, "d", 13),
                    createToken(TemplateTokenType.SLASH, "/", 15),
                    createToken(TemplateTokenType.IDENTIFIER, "e", 17),
                    createToken(TemplateTokenType.PERCENT, "%", 19),
                    createToken(TemplateTokenType.IDENTIFIER, "f", 21),
                    createToken(TemplateTokenType.DELIMITER_CLOSE, "}", 22),
                ),
                0, 23,
            ),
            groups[0],
        )
    }

    @Test
    fun `scan should recognize dot operator for property access`() {
        val input = listOf(createContentPart("{user.name}"))
        val scanner = createScanner()

        val groups = scanner.scan(input)

        assertEquals(1, groups.size)
        assertEquals(
            createGroup(
                listOf(
                    createToken(TemplateTokenType.DELIMITER_OPEN, "{", 0),
                    createToken(TemplateTokenType.IDENTIFIER, "user", 1),
                    createToken(TemplateTokenType.DOT, ".", 5),
                    createToken(TemplateTokenType.IDENTIFIER, "name", 6),
                    createToken(TemplateTokenType.DELIMITER_CLOSE, "}", 10),
                ),
                0, 11,
            ),
            groups[0],
        )
    }

    @Test
    fun `scan should recognize comma in function calls`() {
        val input = listOf(createContentPart("{func(a, b)}"))
        val scanner = createScanner()

        val groups = scanner.scan(input)

        assertEquals(1, groups.size)
        assertEquals(
            createGroup(
                listOf(
                    createToken(TemplateTokenType.DELIMITER_OPEN, "{", 0),
                    createToken(TemplateTokenType.IDENTIFIER, "func", 1),
                    createToken(TemplateTokenType.LEFT_PAREN, "(", 5),
                    createToken(TemplateTokenType.IDENTIFIER, "a", 6),
                    createToken(TemplateTokenType.COMMA, ",", 7),
                    createToken(TemplateTokenType.IDENTIFIER, "b", 9),
                    createToken(TemplateTokenType.RIGHT_PAREN, ")", 10),
                    createToken(TemplateTokenType.DELIMITER_CLOSE, "}", 11),
                ),
                0, 12,
            ),
            groups[0],
        )
    }

    @Test
    fun `scan should recognize equality operator`() {
        val input = listOf(createContentPart("{a == b}"))
        val scanner = createScanner()

        val groups = scanner.scan(input)

        assertEquals(1, groups.size)
        assertEquals(
            createGroup(
                listOf(
                    createToken(TemplateTokenType.DELIMITER_OPEN, "{", 0),
                    createToken(TemplateTokenType.IDENTIFIER, "a", 1),
                    createToken(TemplateTokenType.EQUAL_EQUAL, "==", 3),
                    createToken(TemplateTokenType.IDENTIFIER, "b", 6),
                    createToken(TemplateTokenType.DELIMITER_CLOSE, "}", 7),
                ),
                0, 8,
            ),
            groups[0],
        )
    }

    @Test
    fun `scan should recognize boolean keywords`() {
        val input = listOf(createContentPart("{true and false or null}"))
        val scanner = createScanner()

        val groups = scanner.scan(input)

        assertEquals(1, groups.size)
        assertEquals(
            createGroup(
                listOf(
                    createToken(TemplateTokenType.DELIMITER_OPEN, "{", 0),
                    createToken(TemplateTokenType.TRUE, "true", 1),
                    createToken(TemplateTokenType.AND, "and", 6),
                    createToken(TemplateTokenType.FALSE, "false", 10),
                    createToken(TemplateTokenType.OR, "or", 16),
                    createToken(TemplateTokenType.NULL, "null", 19),
                    createToken(TemplateTokenType.DELIMITER_CLOSE, "}", 23),
                ),
                0, 24,
            ),
            groups[0],
        )
    }

    @Test
    fun `scan should recognize loop keywords`() {
        val input = listOf(createContentPart("{for item in items}"))
        val scanner = createScanner()

        val groups = scanner.scan(input)

        assertEquals(1, groups.size)
        assertEquals(
            createGroup(
                listOf(
                    createToken(TemplateTokenType.DELIMITER_OPEN, "{", 0),
                    createToken(TemplateTokenType.FOR, "for", 1),
                    createToken(TemplateTokenType.IDENTIFIER, "item", 5),
                    createToken(TemplateTokenType.IN, "in", 10),
                    createToken(TemplateTokenType.IDENTIFIER, "items", 13),
                    createToken(TemplateTokenType.DELIMITER_CLOSE, "}", 18),
                ),
                0, 19,
            ),
            groups[0],
        )
    }

    @Test
    fun `scan should recognize end keyword`() {
        val input = listOf(createContentPart("{end}"))
        val scanner = createScanner()

        val groups = scanner.scan(input)

        assertEquals(1, groups.size)
        assertEquals(
            createGroup(
                listOf(
                    createToken(TemplateTokenType.DELIMITER_OPEN, "{", 0),
                    createToken(TemplateTokenType.END, "end", 1),
                    createToken(TemplateTokenType.DELIMITER_CLOSE, "}", 4),
                ),
                0, 5,
            ),
            groups[0],
        )
    }

    @Test
    fun `scan should recognize end keyword case-insensitively`() {
        val scanner = createScanner()

        val testCases = listOf("end", "END", "End", "eNd", "EnD")
        testCases.forEach { keyword ->
            val input = listOf(createContentPart("{$keyword}"))
            val groups = scanner.scan(input)

            assertEquals(1, groups.size)
            assertEquals(
                createGroup(
                    listOf(
                        createToken(TemplateTokenType.DELIMITER_OPEN, "{", 0),
                        createToken(TemplateTokenType.END, keyword, 1),
                        createToken(TemplateTokenType.DELIMITER_CLOSE, "}", 1 + keyword.length),
                    ),
                    0, 2 + keyword.length,
                ),
                groups[0],
            )
        }
    }

    @Test
    fun `scan should parse string literals`() {
        val input = listOf(createContentPart("{\"hello world\"}"))
        val scanner = createScanner()

        val groups = scanner.scan(input)

        assertEquals(1, groups.size)
        assertEquals(
            createGroup(
                listOf(
                    createToken(TemplateTokenType.DELIMITER_OPEN, "{", 0),
                    createToken(TemplateTokenType.STRING, "\"hello world\"", 1, "hello world"),
                    createToken(TemplateTokenType.DELIMITER_CLOSE, "}", 14),
                ),
                0, 15,
            ),
            groups[0],
        )
    }

    @Test
    fun `scan should parse integer literals`() {
        val input = listOf(createContentPart("{123}"))
        val scanner = createScanner()

        val groups = scanner.scan(input)

        assertEquals(1, groups.size)
        assertEquals(
            createGroup(
                listOf(
                    createToken(TemplateTokenType.DELIMITER_OPEN, "{", 0),
                    createToken(TemplateTokenType.INTEGER, "123", 1, 123),
                    createToken(TemplateTokenType.DELIMITER_CLOSE, "}", 4),
                ),
                0, 5,
            ),
            groups[0],
        )
    }

    @Test
    fun `scan should parse double literals`() {
        val input = listOf(createContentPart("{123.45}"))
        val scanner = createScanner()

        val groups = scanner.scan(input)

        assertEquals(1, groups.size)
        assertEquals(
            createGroup(
                listOf(
                    createToken(TemplateTokenType.DELIMITER_OPEN, "{", 0),
                    createToken(TemplateTokenType.DOUBLE, "123.45", 1, 123.45),
                    createToken(TemplateTokenType.DELIMITER_CLOSE, "}", 7),
                ),
                0, 8,
            ),
            groups[0],
        )
    }

    @Test
    fun `scan should skip spaces within template expressions`() {
        val input = listOf(createContentPart("{ firstName }"))
        val scanner = createScanner()

        val groups = scanner.scan(input)

        assertEquals(1, groups.size)
        assertEquals(
            createGroup(
                listOf(
                    createToken(TemplateTokenType.DELIMITER_OPEN, "{", 0),
                    createToken(TemplateTokenType.IDENTIFIER, "firstName", 2),
                    createToken(TemplateTokenType.DELIMITER_CLOSE, "}", 12),
                ),
                0, 13,
            ),
            groups[0],
        )
    }

    @Test
    fun `scan should only process content inside text tags`() {
        val input = listOf(
            createContentPart("{outside}", false),
            createContentPart("{inside}", true),
        )
        val scanner = createScanner()

        val groups = scanner.scan(input)

        assertEquals(1, groups.size)
        assertEquals(
            createGroup(
                listOf(
                    createToken(TemplateTokenType.DELIMITER_OPEN, "{", 0),
                    createToken(TemplateTokenType.IDENTIFIER, "inside", 1),
                    createToken(TemplateTokenType.DELIMITER_CLOSE, "}", 7),
                ),
                0, 8,
            ),
            groups[0],
        )
    }

    @Test
    fun `scan should ignore TagPart tokens`() {
        val input = listOf(
            createTagPart("w:t", "<w:t>", TagPartType.OPENING, true),
            createContentPart("{name}", true),
            createTagPart("w:t", "</w:t>", TagPartType.CLOSING, true),
        )
        val scanner = createScanner()

        val groups = scanner.scan(input)

        assertEquals(1, groups.size)
        assertEquals(
            createGroup(
                listOf(
                    createToken(TemplateTokenType.DELIMITER_OPEN, "{", 0),
                    createToken(TemplateTokenType.IDENTIFIER, "name", 1),
                    createToken(TemplateTokenType.DELIMITER_CLOSE, "}", 5),
                ),
                0, 6,
            ),
            groups[0],
        )
    }

    @Test
    fun `scan should concatenate multiple ContentPart tokens`() {
        val input = listOf(
            createContentPart("{", true),
            createContentPart("first", true),
            createContentPart("Name}", true),
        )
        val scanner = createScanner()

        val groups = scanner.scan(input)

        assertEquals(1, groups.size)
        assertEquals(
            createGroup(
                listOf(
                    createToken(TemplateTokenType.DELIMITER_OPEN, "{", 0),
                    createToken(TemplateTokenType.IDENTIFIER, "firstName", 1),
                    createToken(TemplateTokenType.DELIMITER_CLOSE, "}", 10),
                ),
                0, 11,
            ),
            groups[0],
        )
    }

    @Test
    fun `scan should handle fragmented delimiter across content parts`() {
        val input = listOf(
            createContentPart("{", true),
            createContentPart("name", true),
            createContentPart("}", true),
        )
        val scanner = createScanner()

        val groups = scanner.scan(input)

        assertEquals(1, groups.size)
        assertEquals(
            createGroup(
                listOf(
                    createToken(TemplateTokenType.DELIMITER_OPEN, "{", 0),
                    createToken(TemplateTokenType.IDENTIFIER, "name", 1),
                    createToken(TemplateTokenType.DELIMITER_CLOSE, "}", 5),
                ),
                0, 6,
            ),
            groups[0],
        )
    }

    @Test
    fun `scan should handle conditional expression`() {
        val input = listOf(createContentPart("{if condition == true}"))
        val scanner = createScanner()

        val groups = scanner.scan(input)

        assertEquals(1, groups.size)
        assertEquals(
            createGroup(
                listOf(
                    createToken(TemplateTokenType.DELIMITER_OPEN, "{", 0),
                    createToken(TemplateTokenType.IF, "if", 1),
                    createToken(TemplateTokenType.IDENTIFIER, "condition", 4),
                    createToken(TemplateTokenType.EQUAL_EQUAL, "==", 14),
                    createToken(TemplateTokenType.TRUE, "true", 17),
                    createToken(TemplateTokenType.DELIMITER_CLOSE, "}", 21),
                ),
                0, 22,
            ),
            groups[0],
        )
    }

    @Test
    fun `scan should handle content with no templates`() {
        val input = listOf(createContentPart("plain text"))
        val scanner = createScanner()

        val groups = scanner.scan(input)

        assertEquals(0, groups.size)
    }

    @Test
    fun `scan should throw error on unterminated string`() {
        val input = listOf(createContentPart("{\"unterminated}"))
        val scanner = createScanner()

        assertFailsWith<RuntimeException> {
            scanner.scan(input)
        }
    }

    @Test
    fun `scan should throw error on unexpected character`() {
        val input = listOf(createContentPart("{#invalid}"))
        val scanner = createScanner()

        assertFailsWith<RuntimeException> {
            scanner.scan(input)
        }
    }

    @Test
    fun `scan should handle expression without closing delimiter`() {
        val input = listOf(createContentPart("{name"))
        val scanner = createScanner()

        val groups = scanner.scan(input)

        assertEquals(1, groups.size)
        assertEquals(
            createGroup(
                listOf(
                    createToken(TemplateTokenType.DELIMITER_OPEN, "{", 0),
                    createToken(TemplateTokenType.IDENTIFIER, "name", 1),
                ),
                0, 5,
            ),
            groups[0],
        )
    }

    @Test
    fun `scan should tokenize optional chaining operator`() {
        val input = listOf(createContentPart("{a.?b}"))
        val scanner = createScanner()

        val groups = scanner.scan(input)

        assertEquals(1, groups.size)
        assertEquals(
            createGroup(
                listOf(
                    createToken(TemplateTokenType.DELIMITER_OPEN, "{", 0),
                    createToken(TemplateTokenType.IDENTIFIER, "a", 1),
                    createToken(TemplateTokenType.DOT_QUESTION, ".?", 2),
                    createToken(TemplateTokenType.IDENTIFIER, "b", 4),
                    createToken(TemplateTokenType.DELIMITER_CLOSE, "}", 5),
                ),
                0, 6,
            ),
            groups[0],
        )
    }

    @Test
    fun `scan should tokenize null coalescing operator`() {
        val input = listOf(createContentPart("{a ?? b}"))
        val scanner = createScanner()

        val groups = scanner.scan(input)

        assertEquals(1, groups.size)
        assertEquals(
            createGroup(
                listOf(
                    createToken(TemplateTokenType.DELIMITER_OPEN, "{", 0),
                    createToken(TemplateTokenType.IDENTIFIER, "a", 1),
                    createToken(TemplateTokenType.QUESTION_QUESTION, "??", 3),
                    createToken(TemplateTokenType.IDENTIFIER, "b", 6),
                    createToken(TemplateTokenType.DELIMITER_CLOSE, "}", 7),
                ),
                0, 8,
            ),
            groups[0],
        )
    }

    @Test
    fun `scan should throw error for single question mark`() {
        val input = listOf(createContentPart("{a ? b}"))
        val scanner = createScanner()

        assertFailsWith<TemplaterException.ScanError> {
            scanner.scan(input)
        }
    }

    @Test
    fun `scan should skip empty block comment`() {
        val input = listOf(createContentPart("{/**/}"))
        val scanner = createScanner()

        val groups = scanner.scan(input)

        assertEquals(1, groups.size)
        assertEquals(
            createGroup(
                listOf(
                    createToken(TemplateTokenType.DELIMITER_OPEN, "{", 0),
                    createToken(TemplateTokenType.DELIMITER_CLOSE, "}", 5),
                ),
                0, 6,
            ),
            groups[0],
        )
    }

    @Test
    fun `scan should skip block comment with content`() {
        val input = listOf(createContentPart("{/* This is a comment */}"))
        val scanner = createScanner()

        val groups = scanner.scan(input)

        assertEquals(1, groups.size)
        assertEquals(
            createGroup(
                listOf(
                    createToken(TemplateTokenType.DELIMITER_OPEN, "{", 0),
                    createToken(TemplateTokenType.DELIMITER_CLOSE, "}", 24),
                ),
                0, 25,
            ),
            groups[0],
        )
    }

    @Test
    fun `scan should skip block comment with spaces`() {
        val input = listOf(createContentPart("{/*   */}"))
        val scanner = createScanner()

        val groups = scanner.scan(input)

        assertEquals(1, groups.size)
        assertEquals(
            createGroup(
                listOf(
                    createToken(TemplateTokenType.DELIMITER_OPEN, "{", 0),
                    createToken(TemplateTokenType.DELIMITER_CLOSE, "}", 8),
                ),
                0, 9,
            ),
            groups[0],
        )
    }

    @Test
    fun `scan should skip multiline block comment`() {
        val input = listOf(createContentPart("{/* line 1\nline 2\nline 3 */}"))
        val scanner = createScanner()

        val groups = scanner.scan(input)

        assertEquals(1, groups.size)
        assertEquals(
            createGroup(
                listOf(
                    createToken(TemplateTokenType.DELIMITER_OPEN, "{", 0),
                    createToken(TemplateTokenType.DELIMITER_CLOSE, "}", 27),
                ),
                0, 28,
            ),
            groups[0],
        )
    }

    @Test
    fun `scan should skip comment before expression`() {
        val input = listOf(createContentPart("{/* comment */ firstName}"))
        val scanner = createScanner()

        val groups = scanner.scan(input)

        assertEquals(1, groups.size)
        assertEquals(
            createGroup(
                listOf(
                    createToken(TemplateTokenType.DELIMITER_OPEN, "{", 0),
                    createToken(TemplateTokenType.IDENTIFIER, "firstName", 15),
                    createToken(TemplateTokenType.DELIMITER_CLOSE, "}", 24),
                ),
                0, 25,
            ),
            groups[0],
        )
    }

    @Test
    fun `scan should skip comment after expression`() {
        val input = listOf(createContentPart("{firstName /* comment */}"))
        val scanner = createScanner()

        val groups = scanner.scan(input)

        assertEquals(1, groups.size)
        assertEquals(
            createGroup(
                listOf(
                    createToken(TemplateTokenType.DELIMITER_OPEN, "{", 0),
                    createToken(TemplateTokenType.IDENTIFIER, "firstName", 1),
                    createToken(TemplateTokenType.DELIMITER_CLOSE, "}", 24),
                ),
                0, 25,
            ),
            groups[0],
        )
    }

    @Test
    fun `scan should skip multiple comments in expression`() {
        val input = listOf(createContentPart("{/* c1 */ a + /* c2 */ b /* c3 */}"))
        val scanner = createScanner()

        val groups = scanner.scan(input)

        assertEquals(1, groups.size)
        assertEquals(
            createGroup(
                listOf(
                    createToken(TemplateTokenType.DELIMITER_OPEN, "{", 0),
                    createToken(TemplateTokenType.IDENTIFIER, "a", 10),
                    createToken(TemplateTokenType.PLUS, "+", 12),
                    createToken(TemplateTokenType.IDENTIFIER, "b", 23),
                    createToken(TemplateTokenType.DELIMITER_CLOSE, "}", 33),
                ),
                0, 34,
            ),
            groups[0],
        )
    }

    @Test
    fun `scan should distinguish division from comment start`() {
        val input = listOf(createContentPart("{a / b}"))
        val scanner = createScanner()

        val groups = scanner.scan(input)

        assertEquals(1, groups.size)
        assertEquals(
            createGroup(
                listOf(
                    createToken(TemplateTokenType.DELIMITER_OPEN, "{", 0),
                    createToken(TemplateTokenType.IDENTIFIER, "a", 1),
                    createToken(TemplateTokenType.SLASH, "/", 3),
                    createToken(TemplateTokenType.IDENTIFIER, "b", 5),
                    createToken(TemplateTokenType.DELIMITER_CLOSE, "}", 6),
                ),
                0, 7,
            ),
            groups[0],
        )
    }

    @Test
    fun `scan should handle division followed by multiply`() {
        val input = listOf(createContentPart("{a / * b}"))
        val scanner = createScanner()

        val groups = scanner.scan(input)

        assertEquals(1, groups.size)
        assertEquals(
            createGroup(
                listOf(
                    createToken(TemplateTokenType.DELIMITER_OPEN, "{", 0),
                    createToken(TemplateTokenType.IDENTIFIER, "a", 1),
                    createToken(TemplateTokenType.SLASH, "/", 3),
                    createToken(TemplateTokenType.STAR, "*", 5),
                    createToken(TemplateTokenType.IDENTIFIER, "b", 7),
                    createToken(TemplateTokenType.DELIMITER_CLOSE, "}", 8),
                ),
                0, 9,
            ),
            groups[0],
        )
    }

    @Test
    fun `scan should handle comment with asterisks in content`() {
        val input = listOf(createContentPart("{/* comment with * asterisks ** here */}"))
        val scanner = createScanner()

        val groups = scanner.scan(input)

        assertEquals(1, groups.size)
        assertEquals(
            createGroup(
                listOf(
                    createToken(TemplateTokenType.DELIMITER_OPEN, "{", 0),
                    createToken(TemplateTokenType.DELIMITER_CLOSE, "}", 39),
                ),
                0, 40,
            ),
            groups[0],
        )
    }

    @Test
    fun `scan should throw error on unterminated comment`() {
        val input = listOf(createContentPart("{/* unterminated}"))
        val scanner = createScanner()

        val exception = assertFailsWith<TemplaterException.ScanError> {
            scanner.scan(input)
        }
        assertEquals("Unterminated block comment.", exception.message)
        assertEquals(1, exception.contentIdx.value)
    }

    @Test
    fun `scan should throw error on unterminated comment at end of expression`() {
        val input = listOf(createContentPart("{a /* comment"))
        val scanner = createScanner()

        val exception = assertFailsWith<TemplaterException.ScanError> {
            scanner.scan(input)
        }
        assertEquals("Unterminated block comment.", exception.message)
        assertEquals(3, exception.contentIdx.value)
    }

    @Test
    fun `scan should handle comment spanning multiple content parts`() {
        val input = listOf(
            createContentPart("{/* com", true),
            createContentPart("ment */}", true),
        )
        val scanner = createScanner()

        val groups = scanner.scan(input)

        assertEquals(1, groups.size)
        assertEquals(
            createGroup(
                listOf(
                    createToken(TemplateTokenType.DELIMITER_OPEN, "{", 0),
                    createToken(TemplateTokenType.DELIMITER_CLOSE, "}", 14),
                ),
                0, 15,
            ),
            groups[0],
        )
    }

    @Test
    fun `scan should handle comment with special characters`() {
        val input = listOf(createContentPart("{/* !@#$%^&()_+=[]{}|;:,.<>? */}"))
        val scanner = createScanner()

        val groups = scanner.scan(input)

        assertEquals(1, groups.size)
        assertEquals(
            createGroup(
                listOf(
                    createToken(TemplateTokenType.DELIMITER_OPEN, "{", 0),
                    createToken(TemplateTokenType.DELIMITER_CLOSE, "}", 31),
                ),
                0, 32,
            ),
            groups[0],
        )
    }
}
