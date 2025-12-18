package com.docstencil.core.scanner.model

enum class TemplateTokenType {
    // Delimiter tokens and expansion specifier.
    DELIMITER_OPEN, DELIMITER_CLOSE, AT,

    // Single-character tokens.
    LEFT_PAREN, RIGHT_PAREN,
    COMMA, DOT, MINUS, PERCENT, PLUS, SLASH, STAR, PIPE,

    // One or two character tokens.
    BANG, BANG_EQUAL,
    EQUAL, EQUAL_EQUAL, EQUAL_GREATER,
    GREATER, GREATER_EQUAL,
    LESS, LESS_EQUAL,
    DOT_QUESTION,
    QUESTION_QUESTION,

    // Literals.
    IDENTIFIER, STRING, INTEGER, DOUBLE, VERBATIM, SENTINEL,

    // Simple keywords.
    AND, FALSE, IN, NULL, OR, TRUE, VAR,

    // Structural keywords.
    CASE, DO, ELSE, END, FOR, IF, INSERT, REWRITE, THEN, WHEN
}
