package com.docstencil.core.scanner

import com.docstencil.core.api.OfficeTemplateOptions
import com.docstencil.core.error.TemplaterException
import com.docstencil.core.scanner.model.*

private val KEYWORD_MAP = mapOf(
    "and" to TemplateTokenType.AND,
    "case" to TemplateTokenType.CASE,
    "do" to TemplateTokenType.DO,
    "else" to TemplateTokenType.ELSE,
    "end" to TemplateTokenType.END,
    "false" to TemplateTokenType.FALSE,
    "for" to TemplateTokenType.FOR,
    "if" to TemplateTokenType.IF,
    "in" to TemplateTokenType.IN,
    "insert" to TemplateTokenType.INSERT,
    "null" to TemplateTokenType.NULL,
    "or" to TemplateTokenType.OR,
    "rewrite" to TemplateTokenType.REWRITE,
    "then" to TemplateTokenType.THEN,
    "true" to TemplateTokenType.TRUE,
    "var" to TemplateTokenType.VAR,
    "when" to TemplateTokenType.WHEN,
)

private val DOUBLE_QUOTES = setOf(
    '"',
    '”',
    '„',
    '“',
)

// Taken from section "Escape Sequences": https://docs.oracle.com/javase/tutorial/java/data/characters.html
private val BACKSLASH_ESCAPE_SEQUENCES = mapOf(
    "t" to "\t",
    "b" to "\b",
    "n" to "\n",
    "r" to "\r",
)

class TemplateTokenGroupScanner(private val delimiters: OfficeTemplateOptions.Delimiters) {
    private class Cursor(
        private val rawContentText: String,
        private val delimiters: OfficeTemplateOptions.Delimiters,
        private var currIdx: ContentIdx = ContentIdx(0),
        private var startIdx: ContentIdx = ContentIdx(0),
    ) {
        companion object {
            fun create(
                rawContentText: String,
                delimiters: OfficeTemplateOptions.Delimiters,
            ): Cursor {
                return Cursor(rawContentText, delimiters)
            }
        }

        fun scanGroup(): TemplateTokenGroup? {
            if (!isAtStartOfTemplateExpression()) {
                forwardToStartOfTemplateExpression()
            }
            if (isAtEnd()) {
                return null
            }

            val tokens = mutableListOf<TemplateToken>()
            val groupStart = currIdx
            while (!isAtEnd()) {
                startIdx = currIdx
                val token = scanToken()
                if (token != null) {
                    tokens.add(token)
                }
                if (token?.type == TemplateTokenType.DELIMITER_CLOSE) {
                    break
                }
            }

            return TemplateTokenGroup(tokens, groupStart, currIdx)
        }

        fun isAtStartOfTemplateExpression(): Boolean {
            return rawContentText.startsWith(delimiters.open, currIdx.value)
        }

        fun forwardToStartOfTemplateExpression() {
            val nextTemplateDelimiterOpen = rawContentText.indexOf(delimiters.open, currIdx.value)
            currIdx = ContentIdx(
                if (nextTemplateDelimiterOpen == -1) {
                    rawContentText.length
                } else {
                    nextTemplateDelimiterOpen
                },
            )
        }

        fun isAtBeginning(): Boolean {
            return currIdx.value <= 0
        }

        fun isAtEnd(): Boolean {
            return currIdx.value >= rawContentText.length
        }

        private fun scanToken(): TemplateToken? {
            if (match(delimiters.open)) {
                return createToken(TemplateTokenType.DELIMITER_OPEN)
            }
            if (match(delimiters.close)) {
                return createToken(TemplateTokenType.DELIMITER_CLOSE)
            }

            return when (val c = advance()) {
                '(' -> createToken(TemplateTokenType.LEFT_PAREN)
                ')' -> createToken(TemplateTokenType.RIGHT_PAREN)
                ',' -> createToken(TemplateTokenType.COMMA)
                '.' -> createToken(if (match('?')) TemplateTokenType.DOT_QUESTION else TemplateTokenType.DOT)
                '-' -> createToken(TemplateTokenType.MINUS)
                '%' -> createToken(TemplateTokenType.PERCENT)
                '+' -> createToken(TemplateTokenType.PLUS)
                '*' -> createToken(TemplateTokenType.STAR)
                '@' -> createToken(TemplateTokenType.AT)
                '!' -> createToken(if (match('=')) TemplateTokenType.BANG_EQUAL else TemplateTokenType.BANG)
                '=' -> createToken(
                    when {
                        match('=') -> TemplateTokenType.EQUAL_EQUAL
                        match('>') -> TemplateTokenType.EQUAL_GREATER
                        else -> TemplateTokenType.EQUAL
                    },
                )

                '<' -> createToken(if (match('=')) TemplateTokenType.LESS_EQUAL else TemplateTokenType.LESS)
                '>' -> createToken(if (match('=')) TemplateTokenType.GREATER_EQUAL else TemplateTokenType.GREATER)
                '?' -> createToken(
                    if (match('?')) TemplateTokenType.QUESTION_QUESTION else throw TemplaterException.ScanError(
                        "Unexpected character: '?'",
                        currIdx.decrement(),
                    ),
                )

                '/' -> {
                    if (peek() == '*') {
                        skipBlockComment()
                        null
                    } else {
                        createToken(TemplateTokenType.SLASH)
                    }
                }

                '|' -> createToken(TemplateTokenType.PIPE)
                ' ' -> null
                '\n' -> null
                '\r' -> null
                '\t' -> null
                '0', '1', '2', '3', '4', '5', '6', '7', '8', '9' -> number()
                else -> {
                    if (DOUBLE_QUOTES.contains(c)) {
                        string()
                    } else if (isAlpha(c)) {
                        identifier()
                    } else {
                        throw TemplaterException.ScanError(
                            "Unexpected character: '$c'",
                            currIdx.decrement(),
                        )
                    }
                }
            }
        }

        private fun advance(): Char {
            val currChar = rawContentText[currIdx.value]
            currIdx = currIdx.increment()
            return currChar
        }

        private fun peek(): Char {
            if (isAtEnd()) {
                return '\u0000'
            }
            return rawContentText[currIdx.value]
        }

        private fun previous(): Char {
            if (isAtBeginning()) {
                return '\u0000'
            }
            return rawContentText[currIdx.value - 1]
        }

        private fun peekNext(): Char {
            if (isAtEnd() || (currIdx.value + 1 >= rawContentText.length)) {
                return '\u0000'
            }
            return rawContentText[currIdx.value + 1]
        }

        private fun createToken(type: TemplateTokenType): TemplateToken {
            return createToken(type, null)
        }

        private fun createToken(type: TemplateTokenType, literal: Any?): TemplateToken {
            val lexeme = rawContentText.substring(startIdx.value, currIdx.value)
            return TemplateToken(type, lexeme, null, literal, startIdx)
        }

        @Suppress("SameParameterValue")
        private fun match(char: Char): Boolean {
            if (isAtEnd()) {
                return false
            }
            if (rawContentText[currIdx.value] != char) {
                return false
            }

            currIdx = currIdx.increment()
            return true
        }

        private fun match(str: String): Boolean {
            if (isAtEnd()) {
                return false
            }
            if (!rawContentText.startsWith(str, currIdx.value)) {
                return false
            }

            currIdx = currIdx.add(str.length)
            return true
        }

        private fun string(): TemplateToken {
            while (!DOUBLE_QUOTES.contains(peek()) && !isAtEnd()) {
                advance()
                // Backslash escapes the next character.
                if (previous() == '\\') {
                    advance()
                }
            }

            if (isAtEnd()) {
                throw TemplaterException.ScanError(
                    "Unterminated string.",
                    startIdx.decrement(),
                )
            }

            // Consume the closing double quote character.
            advance()

            // Trim the surrounding quotes.
            val rawValue = rawContentText.substring(startIdx.value + 1, currIdx.value - 1)
            // Remove backslashes used for escaping the characters after them.
            // 4 backslashes because we need to escape the string and the regex engine.
            val value = rawValue.replace(
                Regex("\\\\(.)"),
            ) { mr ->
                val escapedChar =
                    mr.groups[1]?.value ?: throw TemplaterException.FatalError(
                        "Escape regex must have a bug.",
                        contentIdx = startIdx,
                    )
                BACKSLASH_ESCAPE_SEQUENCES.getOrDefault(escapedChar, escapedChar)
            }
            return createToken(TemplateTokenType.STRING, value as Any)
        }

        private fun number(): TemplateToken {
            while (peek().isDigit()) advance()

            // Look for a fractional part.
            if (peek() == '.' && peekNext().isDigit()) {
                // Consume the "."
                advance()

                while (peek().isDigit()) advance()
            }

            val strValue = rawContentText.substring(startIdx.value, currIdx.value)
            return if (strValue.contains(".")) {
                createToken(TemplateTokenType.DOUBLE, strValue.toDouble())
            } else {
                createToken(TemplateTokenType.INTEGER, strValue.toInt())
            }
        }

        private fun identifier(): TemplateToken {
            while (isAlphaNum(peek())) advance()

            val value = rawContentText.substring(startIdx.value, currIdx.value)
            val keyword = KEYWORD_MAP[value.lowercase()]

            return if (keyword != null) {
                createToken(keyword)
            } else {
                createToken(TemplateTokenType.IDENTIFIER)
            }
        }

        private fun isAlpha(c: Char): Boolean {
            return (c.code >= 'a'.code && c.code <= 'z'.code) ||
                    (c.code >= 'A'.code && c.code <= 'Z'.code) ||
                    c == '_' ||
                    c == '$'
        }

        private fun isAlphaNum(c: Char): Boolean {
            return isAlpha(c) || c.isDigit()
        }

        private fun skipBlockComment() {
            if (!match('*')) {
                throw TemplaterException.FatalError(
                    "skipBlockComment called but '*' not found",
                    contentIdx = currIdx,
                )
            }

            val commentStartIdx = startIdx

            while (!isAtEnd()) {
                if (peek() == '*' && peekNext() == '/') {
                    advance()
                    advance()
                    return
                }
                advance()
            }

            throw TemplaterException.ScanError(
                "Unterminated block comment.",
                commentStartIdx,
            )
        }
    }

    /**
     * Extracts lexed template token groups from the `RawXmlToken`s.
     *
     * This is done in a separate step, because string literals in XML as well as the templating
     * language mean that we need to scan two different languages with string literals sequentially
     * to correctly identify template expressions.
     */
    fun scan(rawTokens: List<XmlRawInputToken>): List<TemplateTokenGroup> {
        val rawContentText = XmlRawInputToken.extractTextContent(rawTokens)

        val cursor = Cursor.create(rawContentText, delimiters)
        val tokenGroups = mutableListOf<TemplateTokenGroup>()

        while (!cursor.isAtEnd()) {
            val group = cursor.scanGroup()
            if (group != null) {
                tokenGroups.add(group)
            }
        }

        return tokenGroups
    }
}
