package com.docstencil.core.parser

import com.docstencil.core.parser.model.*
import com.docstencil.core.scanner.model.*
import kotlin.test.Test
import kotlin.test.assertEquals

class TemplateParserTest {
    val parser = TemplateParser()

    private fun createToken(
        type: TemplateTokenType,
        lexeme: String,
        literal: Any? = null,
    ): TemplateToken {
        val dummyInputToken = if (type == TemplateTokenType.VERBATIM)
            XmlInputToken.Raw(XmlRawInputToken.Verbatim(0, lexeme))
        else
            null
        val dummyContentIdx = ContentIdx(0)

        return TemplateToken(
            type,
            lexeme,
            dummyInputToken,
            literal,
            dummyContentIdx,
        )
    }

    private fun createRawToken(text: String): XmlInputToken.Raw {
        return XmlInputToken.Raw(XmlRawInputToken.Verbatim(0, text))
    }

    @Test
    fun `parse should handle text and placeholders`() {
        val firstName = createToken(TemplateTokenType.IDENTIFIER, "firstName")
        val lastName = createToken(TemplateTokenType.IDENTIFIER, "lastName")

        val tokens = listOf(
            createToken(TemplateTokenType.VERBATIM, "text1"),
            createToken(TemplateTokenType.VERBATIM, "text2"),
            createToken(TemplateTokenType.DELIMITER_OPEN, "{"),
            firstName,
            createToken(TemplateTokenType.DELIMITER_CLOSE, "}"),
            createToken(TemplateTokenType.DELIMITER_OPEN, "{"),
            lastName,
            createToken(TemplateTokenType.DELIMITER_CLOSE, "}"),
            createToken(TemplateTokenType.VERBATIM, "text3"),
        )

        val result = parser.parse(tokens)

        assertEquals(5, result.size)

        assertEquals(VerbatimStmt(createRawToken("text1")), result[0])
        assertEquals(VerbatimStmt(createRawToken("text2")), result[1])
        assertEquals(ExpressionStmt(VariableExpr(firstName)), result[2])
        assertEquals(ExpressionStmt(VariableExpr(lastName)), result[3])
        assertEquals(VerbatimStmt(createRawToken("text3")), result[4])
    }

    @Test
    fun `parse should handle string literal`() {
        val tokens = listOf(
            createToken(TemplateTokenType.DELIMITER_OPEN, "{"),
            createToken(TemplateTokenType.STRING, "\"hello\"", "hello"),
            createToken(TemplateTokenType.DELIMITER_CLOSE, "}"),
        )

        val result = parser.parse(tokens)

        assertEquals(1, result.size)
        assertEquals(ExpressionStmt(LiteralExpr("hello")), result[0])
    }

    @Test
    fun `parse should handle number literal`() {
        val tokens = listOf(
            createToken(TemplateTokenType.DELIMITER_OPEN, "{"),
            createToken(TemplateTokenType.INTEGER, "42", 42),
            createToken(TemplateTokenType.DELIMITER_CLOSE, "}"),
        )

        val result = parser.parse(tokens)

        assertEquals(1, result.size)
        assertEquals(ExpressionStmt(LiteralExpr(42)), result[0])
    }

    @Test
    fun `parse should handle assignment`() {
        val xToken = createToken(TemplateTokenType.IDENTIFIER, "x")
        val equalToken = createToken(TemplateTokenType.EQUAL, "=")

        val tokens = listOf(
            createToken(TemplateTokenType.DELIMITER_OPEN, "{"),
            xToken,
            equalToken,
            createToken(TemplateTokenType.INTEGER, "5", 5),
            createToken(TemplateTokenType.DELIMITER_CLOSE, "}"),
        )

        val result = parser.parse(tokens)

        assertEquals(1, result.size)
        assertEquals(
            ExpressionStmt(AssignExpr(xToken, LiteralExpr(5))),
            result[0],
        )
    }

    @Test
    fun `parse should handle function call`() {
        val fooToken = createToken(TemplateTokenType.IDENTIFIER, "foo")
        val parenToken = createToken(TemplateTokenType.RIGHT_PAREN, ")")

        val tokens = listOf(
            createToken(TemplateTokenType.DELIMITER_OPEN, "{"),
            fooToken,
            createToken(TemplateTokenType.LEFT_PAREN, "("),
            parenToken,
            createToken(TemplateTokenType.DELIMITER_CLOSE, "}"),
        )

        val result = parser.parse(tokens)

        assertEquals(1, result.size)
        assertEquals(
            ExpressionStmt(CallExpr(VariableExpr(fooToken), parenToken, emptyList())),
            result[0],
        )
    }

    @Test
    fun `parse should handle arithmetic expression with grouping`() {
        val starToken = createToken(TemplateTokenType.STAR, "*")
        val plusToken = createToken(TemplateTokenType.PLUS, "+")

        val tokens = listOf(
            createToken(TemplateTokenType.DELIMITER_OPEN, "{"),
            createToken(TemplateTokenType.INTEGER, "4", 4),
            starToken,
            createToken(TemplateTokenType.LEFT_PAREN, "("),
            createToken(TemplateTokenType.INTEGER, "5", 5),
            plusToken,
            createToken(TemplateTokenType.INTEGER, "6", 6),
            createToken(TemplateTokenType.RIGHT_PAREN, ")"),
            createToken(TemplateTokenType.DELIMITER_CLOSE, "}"),
        )

        val result = parser.parse(tokens)

        assertEquals(1, result.size)
        assertEquals(
            ExpressionStmt(
                BinaryExpr(
                    LiteralExpr(4),
                    starToken,
                    GroupingExpr(
                        BinaryExpr(LiteralExpr(5), plusToken, LiteralExpr(6)),
                    ),
                ),
            ),
            result[0],
        )
    }

    @Test
    fun `parse should handle logical expression with grouping`() {
        val aToken = createToken(TemplateTokenType.IDENTIFIER, "a")
        val andToken = createToken(TemplateTokenType.AND, "and")
        val bToken = createToken(TemplateTokenType.IDENTIFIER, "b")
        val orToken = createToken(TemplateTokenType.OR, "or")
        val cToken = createToken(TemplateTokenType.IDENTIFIER, "c")

        val tokens = listOf(
            createToken(TemplateTokenType.DELIMITER_OPEN, "{"),
            aToken,
            andToken,
            createToken(TemplateTokenType.LEFT_PAREN, "("),
            bToken,
            orToken,
            cToken,
            createToken(TemplateTokenType.RIGHT_PAREN, ")"),
            createToken(TemplateTokenType.DELIMITER_CLOSE, "}"),
        )

        val result = parser.parse(tokens)

        assertEquals(1, result.size)
        assertEquals(
            ExpressionStmt(
                LogicalExpr(
                    VariableExpr(aToken),
                    andToken,
                    GroupingExpr(
                        LogicalExpr(VariableExpr(bToken), orToken, VariableExpr(cToken)),
                    ),
                ),
            ),
            result[0],
        )
    }

    @Test
    fun `parse should handle function call with 3 arguments`() {
        val fnToken = createToken(TemplateTokenType.IDENTIFIER, "fn")
        val fooToken = createToken(TemplateTokenType.IDENTIFIER, "foo")
        val barToken = createToken(TemplateTokenType.IDENTIFIER, "bar")
        val bazToken = createToken(TemplateTokenType.IDENTIFIER, "baz")
        val plusToken = createToken(TemplateTokenType.PLUS, "+")
        val parenToken = createToken(TemplateTokenType.RIGHT_PAREN, ")")

        val tokens = listOf(
            createToken(TemplateTokenType.DELIMITER_OPEN, "{"),
            fnToken,
            createToken(TemplateTokenType.LEFT_PAREN, "("),
            fooToken,
            createToken(TemplateTokenType.COMMA, ","),
            barToken,
            createToken(TemplateTokenType.DOT, "."),
            bazToken,
            createToken(TemplateTokenType.COMMA, ","),
            createToken(TemplateTokenType.INTEGER, "4", 4),
            plusToken,
            createToken(TemplateTokenType.INTEGER, "5", 5),
            parenToken,
            createToken(TemplateTokenType.DELIMITER_CLOSE, "}"),
        )

        val result = parser.parse(tokens)

        assertEquals(1, result.size)
        assertEquals(
            ExpressionStmt(
                CallExpr(
                    VariableExpr(fnToken),
                    parenToken,
                    listOf(
                        VariableExpr(fooToken),
                        GetExpr(VariableExpr(barToken), bazToken),
                        BinaryExpr(LiteralExpr(4), plusToken, LiteralExpr(5)),
                    ),
                ),
            ),
            result[0],
        )
    }

    @Test
    fun `parse should handle if statement`() {
        val conditionToken = createToken(TemplateTokenType.IDENTIFIER, "condition")
        val placeholderToken = createToken(TemplateTokenType.IDENTIFIER, "placeholder")

        val tokens = listOf(
            createToken(TemplateTokenType.DELIMITER_OPEN, "{"),
            createToken(TemplateTokenType.IF, "if"),
            conditionToken,
            createToken(TemplateTokenType.DELIMITER_CLOSE, "}"),
            createToken(TemplateTokenType.VERBATIM, "text1"),
            createToken(TemplateTokenType.DELIMITER_OPEN, "{"),
            placeholderToken,
            createToken(TemplateTokenType.DELIMITER_CLOSE, "}"),
            createToken(TemplateTokenType.VERBATIM, "text2"),
            createToken(TemplateTokenType.DELIMITER_OPEN, "{"),
            createToken(TemplateTokenType.END, "end"),
            createToken(TemplateTokenType.DELIMITER_CLOSE, "}"),
            createToken(TemplateTokenType.VERBATIM, "text3"),
        )

        val result = parser.parse(tokens)

        assertEquals(2, result.size)
        assertEquals(
            IfStmt(
                VariableExpr(conditionToken),
                BlockStmt(
                    listOf(
                        VerbatimStmt(createRawToken("text1")),
                        ExpressionStmt(VariableExpr(placeholderToken)),
                        VerbatimStmt(createRawToken("text2")),
                    ),
                ),
            ),
            result[0],
        )
        assertEquals(VerbatimStmt(createRawToken("text3")), result[1])
    }

    @Test
    fun `parse should handle rewrite statement`() {
        val rewriteToken = createToken(TemplateTokenType.REWRITE, "rewrite")
        val exprToken = createToken(TemplateTokenType.IDENTIFIER, "someVar")
        val placeholderToken = createToken(TemplateTokenType.IDENTIFIER, "placeholder")

        val tokens = listOf(
            createToken(TemplateTokenType.DELIMITER_OPEN, "{"),
            rewriteToken,
            exprToken,
            createToken(TemplateTokenType.DELIMITER_CLOSE, "}"),
            createToken(TemplateTokenType.VERBATIM, "content"),
            createToken(TemplateTokenType.DELIMITER_OPEN, "{"),
            placeholderToken,
            createToken(TemplateTokenType.DELIMITER_CLOSE, "}"),
            createToken(TemplateTokenType.DELIMITER_OPEN, "{"),
            createToken(TemplateTokenType.END, "end"),
            createToken(TemplateTokenType.DELIMITER_CLOSE, "}"),
        )

        val result = parser.parse(tokens)

        assertEquals(1, result.size)
        assertEquals(
            RewriteStmt(
                rewriteToken,
                VariableExpr(exprToken),
                BlockStmt(
                    listOf(
                        VerbatimStmt(createRawToken("content")),
                        ExpressionStmt(VariableExpr(placeholderToken)),
                    ),
                ),
            ),
            result[0],
        )
    }

    @Test
    fun `parse should handle for loop with text and placeholder`() {
        val itemToken = createToken(TemplateTokenType.IDENTIFIER, "item")
        val itemsToken = createToken(TemplateTokenType.IDENTIFIER, "items")
        val placeholderToken = createToken(TemplateTokenType.IDENTIFIER, "item")

        val tokens = listOf(
            createToken(TemplateTokenType.DELIMITER_OPEN, "{"),
            createToken(TemplateTokenType.FOR, "for"),
            itemToken,
            createToken(TemplateTokenType.IN, "in"),
            itemsToken,
            createToken(TemplateTokenType.DELIMITER_CLOSE, "}"),
            createToken(TemplateTokenType.VERBATIM, "text"),
            createToken(TemplateTokenType.DELIMITER_OPEN, "{"),
            placeholderToken,
            createToken(TemplateTokenType.DELIMITER_CLOSE, "}"),
            createToken(TemplateTokenType.DELIMITER_OPEN, "{"),
            createToken(TemplateTokenType.END, "end"),
            createToken(TemplateTokenType.DELIMITER_CLOSE, "}"),
        )

        val result = parser.parse(tokens)

        assertEquals(1, result.size)
        assertEquals(
            ForStmt(
                itemToken,
                VariableExpr(itemsToken),
                BlockStmt(
                    listOf(
                        VerbatimStmt(createRawToken("text")),
                        ExpressionStmt(VariableExpr(placeholderToken)),
                    ),
                ),
            ),
            result[0],
        )
    }

    @Test
    fun `parse should handle nested for loops`() {
        val itemToken = createToken(TemplateTokenType.IDENTIFIER, "item")
        val itemsToken = createToken(TemplateTokenType.IDENTIFIER, "items")
        val item2Token = createToken(TemplateTokenType.IDENTIFIER, "item2")
        val fnToken = createToken(TemplateTokenType.IDENTIFIER, "fn")
        val parenToken = createToken(TemplateTokenType.RIGHT_PAREN, ")")
        val placeholderToken = createToken(TemplateTokenType.IDENTIFIER, "placeholder")

        val tokens = listOf(
            createToken(TemplateTokenType.DELIMITER_OPEN, "{"),
            createToken(TemplateTokenType.FOR, "for"),
            itemToken,
            createToken(TemplateTokenType.IN, "in"),
            itemsToken,
            createToken(TemplateTokenType.DELIMITER_CLOSE, "}"),
            createToken(TemplateTokenType.DELIMITER_OPEN, "{"),
            createToken(TemplateTokenType.IDENTIFIER, "item"),
            createToken(TemplateTokenType.DELIMITER_CLOSE, "}"),
            createToken(TemplateTokenType.DELIMITER_OPEN, "{"),
            createToken(TemplateTokenType.FOR, "for"),
            item2Token,
            createToken(TemplateTokenType.IN, "in"),
            fnToken,
            createToken(TemplateTokenType.LEFT_PAREN, "("),
            parenToken,
            createToken(TemplateTokenType.DELIMITER_CLOSE, "}"),
            createToken(TemplateTokenType.DELIMITER_OPEN, "{"),
            placeholderToken,
            createToken(TemplateTokenType.DELIMITER_CLOSE, "}"),
            createToken(TemplateTokenType.DELIMITER_OPEN, "{"),
            createToken(TemplateTokenType.END, "end"),
            createToken(TemplateTokenType.DELIMITER_CLOSE, "}"),
            createToken(TemplateTokenType.DELIMITER_OPEN, "{"),
            createToken(TemplateTokenType.END, "end"),
            createToken(TemplateTokenType.DELIMITER_CLOSE, "}"),
        )

        val result = parser.parse(tokens)

        assertEquals(1, result.size)
        assertEquals(
            ForStmt(
                itemToken,
                VariableExpr(itemsToken),
                BlockStmt(
                    listOf(
                        ExpressionStmt(
                            VariableExpr(
                                createToken(
                                    TemplateTokenType.IDENTIFIER,
                                    "item",
                                ),
                            ),
                        ),
                        ForStmt(
                            item2Token,
                            CallExpr(VariableExpr(fnToken), parenToken, emptyList()),
                            BlockStmt(
                                listOf(
                                    ExpressionStmt(VariableExpr(placeholderToken)),
                                ),
                            ),
                        ),
                    ),
                ),
            ),
            result[0],
        )
    }

    @Test
    fun `parse should handle for loop with if statement`() {
        val itemToken = createToken(TemplateTokenType.IDENTIFIER, "item")
        val itemsToken = createToken(TemplateTokenType.IDENTIFIER, "items")
        val conditionToken = createToken(TemplateTokenType.IDENTIFIER, "condition")
        val placeholderToken = createToken(TemplateTokenType.IDENTIFIER, "placeholder")

        val tokens = listOf(
            createToken(TemplateTokenType.DELIMITER_OPEN, "{"),
            createToken(TemplateTokenType.FOR, "for"),
            itemToken,
            createToken(TemplateTokenType.IN, "in"),
            itemsToken,
            createToken(TemplateTokenType.DELIMITER_CLOSE, "}"),
            createToken(TemplateTokenType.DELIMITER_OPEN, "{"),
            createToken(TemplateTokenType.IF, "if"),
            conditionToken,
            createToken(TemplateTokenType.DELIMITER_CLOSE, "}"),
            createToken(TemplateTokenType.DELIMITER_OPEN, "{"),
            placeholderToken,
            createToken(TemplateTokenType.DELIMITER_CLOSE, "}"),
            createToken(TemplateTokenType.VERBATIM, "text"),
            createToken(TemplateTokenType.DELIMITER_OPEN, "{"),
            createToken(TemplateTokenType.END, "end"),
            createToken(TemplateTokenType.DELIMITER_CLOSE, "}"),
            createToken(TemplateTokenType.DELIMITER_OPEN, "{"),
            createToken(TemplateTokenType.END, "end"),
            createToken(TemplateTokenType.DELIMITER_CLOSE, "}"),
        )

        val result = parser.parse(tokens)

        assertEquals(1, result.size)
        assertEquals(
            ForStmt(
                itemToken,
                VariableExpr(itemsToken),
                BlockStmt(
                    listOf(
                        IfStmt(
                            VariableExpr(conditionToken),
                            BlockStmt(
                                listOf(
                                    ExpressionStmt(VariableExpr(placeholderToken)),
                                    VerbatimStmt(createRawToken("text")),
                                ),
                            ),
                        ),
                    ),
                ),
            ),
            result[0],
        )
    }

    @Test
    fun `parse should handle case expression with single when-then`() {
        val caseToken = createToken(TemplateTokenType.CASE, "case")
        val whenToken = createToken(TemplateTokenType.WHEN, "when")
        val xToken = createToken(TemplateTokenType.IDENTIFIER, "x")
        val thenToken = createToken(TemplateTokenType.THEN, "then")
        val endToken = createToken(TemplateTokenType.END, "end")

        val tokens = listOf(
            createToken(TemplateTokenType.DELIMITER_OPEN, "{"),
            caseToken,
            whenToken,
            xToken,
            thenToken,
            createToken(TemplateTokenType.INTEGER, "42", 42),
            endToken,
            createToken(TemplateTokenType.DELIMITER_CLOSE, "}"),
        )

        val result = parser.parse(tokens)

        assertEquals(1, result.size)
        assertEquals(
            ExpressionStmt(
                CaseExpr(
                    listOf(
                        WhenBranch(whenToken, VariableExpr(xToken), thenToken, LiteralExpr(42)),
                    ),
                    null,
                ),
            ),
            result[0],
        )
    }

    @Test
    fun `parse should handle case expression with multiple when-then branches`() {
        val caseToken = createToken(TemplateTokenType.CASE, "case")
        val when1Token = createToken(TemplateTokenType.WHEN, "when")
        val xToken = createToken(TemplateTokenType.IDENTIFIER, "x")
        val equalToken = createToken(TemplateTokenType.EQUAL_EQUAL, "==")
        val then1Token = createToken(TemplateTokenType.THEN, "then")
        val when2Token = createToken(TemplateTokenType.WHEN, "when")
        val yToken = createToken(TemplateTokenType.IDENTIFIER, "y")
        val then2Token = createToken(TemplateTokenType.THEN, "then")
        val endToken = createToken(TemplateTokenType.END, "end")

        val tokens = listOf(
            createToken(TemplateTokenType.DELIMITER_OPEN, "{"),
            caseToken,
            when1Token,
            xToken,
            equalToken,
            createToken(TemplateTokenType.INTEGER, "1", 1),
            then1Token,
            createToken(TemplateTokenType.STRING, "\"one\"", "one"),
            when2Token,
            yToken,
            equalToken,
            createToken(TemplateTokenType.INTEGER, "2", 2),
            then2Token,
            createToken(TemplateTokenType.STRING, "\"two\"", "two"),
            endToken,
            createToken(TemplateTokenType.DELIMITER_CLOSE, "}"),
        )

        val result = parser.parse(tokens)

        assertEquals(1, result.size)
        val caseExpr = (result[0] as ExpressionStmt).expr as CaseExpr
        assertEquals(2, caseExpr.branches.size)
        assertEquals(
            WhenBranch(
                when1Token,
                BinaryExpr(VariableExpr(xToken), equalToken, LiteralExpr(1)),
                then1Token,
                LiteralExpr("one"),
            ),
            caseExpr.branches[0],
        )
        assertEquals(
            WhenBranch(
                when2Token,
                BinaryExpr(VariableExpr(yToken), equalToken, LiteralExpr(2)),
                then2Token,
                LiteralExpr("two"),
            ),
            caseExpr.branches[1],
        )
    }

    @Test
    fun `parse should handle case expression with else branch`() {
        val caseToken = createToken(TemplateTokenType.CASE, "case")
        val whenToken = createToken(TemplateTokenType.WHEN, "when")
        val xToken = createToken(TemplateTokenType.IDENTIFIER, "x")
        val thenToken = createToken(TemplateTokenType.THEN, "then")
        val elseToken = createToken(TemplateTokenType.ELSE, "else")
        val endToken = createToken(TemplateTokenType.END, "end")

        val tokens = listOf(
            createToken(TemplateTokenType.DELIMITER_OPEN, "{"),
            caseToken,
            whenToken,
            xToken,
            thenToken,
            createToken(TemplateTokenType.STRING, "\"matched\"", "matched"),
            elseToken,
            createToken(TemplateTokenType.STRING, "\"default\"", "default"),
            endToken,
            createToken(TemplateTokenType.DELIMITER_CLOSE, "}"),
        )

        val result = parser.parse(tokens)

        assertEquals(1, result.size)
        val caseExpr = (result[0] as ExpressionStmt).expr as CaseExpr
        assertEquals(LiteralExpr("default"), caseExpr.elseBranch)
    }

    @Test
    fun `parse should handle case expression with complex expressions`() {
        val caseToken = createToken(TemplateTokenType.CASE, "case")
        val whenToken = createToken(TemplateTokenType.WHEN, "when")
        val xToken = createToken(TemplateTokenType.IDENTIFIER, "x")
        val greaterToken = createToken(TemplateTokenType.GREATER, ">")
        val thenToken = createToken(TemplateTokenType.THEN, "then")
        val plusToken = createToken(TemplateTokenType.PLUS, "+")
        val endToken = createToken(TemplateTokenType.END, "end")

        val tokens = listOf(
            createToken(TemplateTokenType.DELIMITER_OPEN, "{"),
            caseToken,
            whenToken,
            xToken,
            greaterToken,
            createToken(TemplateTokenType.INTEGER, "10", 10),
            thenToken,
            xToken,
            plusToken,
            createToken(TemplateTokenType.INTEGER, "5", 5),
            endToken,
            createToken(TemplateTokenType.DELIMITER_CLOSE, "}"),
        )

        val result = parser.parse(tokens)

        assertEquals(1, result.size)
        val caseExpr = (result[0] as ExpressionStmt).expr as CaseExpr
        assertEquals(1, caseExpr.branches.size)
        assertEquals(
            BinaryExpr(VariableExpr(xToken), greaterToken, LiteralExpr(10)),
            caseExpr.branches[0].condition,
        )
        assertEquals(
            BinaryExpr(VariableExpr(xToken), plusToken, LiteralExpr(5)),
            caseExpr.branches[0].branch,
        )
    }

    @Test
    fun `parse should handle do statement with simple expression`() {
        val doToken = createToken(TemplateTokenType.DO, "do")
        val xToken = createToken(TemplateTokenType.IDENTIFIER, "x")

        val tokens = listOf(
            createToken(TemplateTokenType.DELIMITER_OPEN, "{"),
            doToken,
            xToken,
            createToken(TemplateTokenType.DELIMITER_CLOSE, "}"),
        )

        val result = parser.parse(tokens)

        assertEquals(1, result.size)
        assertEquals(DoStmt(VariableExpr(xToken)), result[0])
    }

    @Test
    fun `parse should handle do statement with assignment`() {
        val doToken = createToken(TemplateTokenType.DO, "do")
        val xToken = createToken(TemplateTokenType.IDENTIFIER, "x")

        val tokens = listOf(
            createToken(TemplateTokenType.DELIMITER_OPEN, "{"),
            doToken,
            xToken,
            createToken(TemplateTokenType.EQUAL, "="),
            createToken(TemplateTokenType.INTEGER, "5", 5),
            createToken(TemplateTokenType.DELIMITER_CLOSE, "}"),
        )

        val result = parser.parse(tokens)

        assertEquals(1, result.size)
        assertEquals(DoStmt(AssignExpr(xToken, LiteralExpr(5))), result[0])
    }

    @Test
    fun `parse should handle do statement with complex expression`() {
        val doToken = createToken(TemplateTokenType.DO, "do")
        val xToken = createToken(TemplateTokenType.IDENTIFIER, "x")
        val plusToken = createToken(TemplateTokenType.PLUS, "+")

        val tokens = listOf(
            createToken(TemplateTokenType.DELIMITER_OPEN, "{"),
            doToken,
            xToken,
            createToken(TemplateTokenType.EQUAL, "="),
            xToken,
            plusToken,
            createToken(TemplateTokenType.INTEGER, "1", 1),
            createToken(TemplateTokenType.DELIMITER_CLOSE, "}"),
        )

        val result = parser.parse(tokens)

        assertEquals(1, result.size)
        val expectedExpr = AssignExpr(
            xToken,
            BinaryExpr(VariableExpr(xToken), plusToken, LiteralExpr(1)),
        )
        assertEquals(DoStmt(expectedExpr), result[0])
    }

    @Test
    fun `parse should handle multiple do statements`() {
        val doToken = createToken(TemplateTokenType.DO, "do")
        val xToken = createToken(TemplateTokenType.IDENTIFIER, "x")
        val yToken = createToken(TemplateTokenType.IDENTIFIER, "y")

        val tokens = listOf(
            createToken(TemplateTokenType.DELIMITER_OPEN, "{"),
            doToken,
            xToken,
            createToken(TemplateTokenType.EQUAL, "="),
            createToken(TemplateTokenType.INTEGER, "1", 1),
            createToken(TemplateTokenType.DELIMITER_CLOSE, "}"),
            createToken(TemplateTokenType.DELIMITER_OPEN, "{"),
            doToken,
            yToken,
            createToken(TemplateTokenType.EQUAL, "="),
            createToken(TemplateTokenType.INTEGER, "2", 2),
            createToken(TemplateTokenType.DELIMITER_CLOSE, "}"),
        )

        val result = parser.parse(tokens)

        assertEquals(2, result.size)
        assertEquals(DoStmt(AssignExpr(xToken, LiteralExpr(1))), result[0])
        assertEquals(DoStmt(AssignExpr(yToken, LiteralExpr(2))), result[1])
    }

    @Test
    fun `parse should handle optional chaining operator`() {
        val aToken = createToken(TemplateTokenType.IDENTIFIER, "a")
        val bToken = createToken(TemplateTokenType.IDENTIFIER, "b")

        val tokens = listOf(
            createToken(TemplateTokenType.DELIMITER_OPEN, "{"),
            aToken,
            createToken(TemplateTokenType.DOT_QUESTION, ".?"),
            bToken,
            createToken(TemplateTokenType.DELIMITER_CLOSE, "}"),
        )

        val result = parser.parse(tokens)

        assertEquals(1, result.size)
        assertEquals(
            ExpressionStmt(
                OptionalGetExpr(VariableExpr(aToken), bToken),
            ),
            result[0],
        )
    }

    @Test
    fun `parse should handle null coalescing operator`() {
        val aToken = createToken(TemplateTokenType.IDENTIFIER, "a")
        val bToken = createToken(TemplateTokenType.IDENTIFIER, "b")
        val opToken = createToken(TemplateTokenType.QUESTION_QUESTION, "??")

        val tokens = listOf(
            createToken(TemplateTokenType.DELIMITER_OPEN, "{"),
            aToken,
            opToken,
            bToken,
            createToken(TemplateTokenType.DELIMITER_CLOSE, "}"),
        )

        val result = parser.parse(tokens)

        assertEquals(1, result.size)
        assertEquals(
            ExpressionStmt(
                LogicalExpr(VariableExpr(aToken), opToken, VariableExpr(bToken)),
            ),
            result[0],
        )
    }

    @Test
    fun `parse should handle null coalescing with correct precedence`() {
        val aToken = createToken(TemplateTokenType.IDENTIFIER, "a")
        val bToken = createToken(TemplateTokenType.IDENTIFIER, "b")
        val cToken = createToken(TemplateTokenType.IDENTIFIER, "c")
        val opToken = createToken(TemplateTokenType.QUESTION_QUESTION, "??")

        val tokens = listOf(
            createToken(TemplateTokenType.DELIMITER_OPEN, "{"),
            aToken,
            opToken,
            bToken,
            createToken(TemplateTokenType.DOT_QUESTION, ".?"),
            cToken,
            createToken(TemplateTokenType.DELIMITER_CLOSE, "}"),
        )

        val result = parser.parse(tokens)

        assertEquals(1, result.size)
        assertEquals(
            ExpressionStmt(
                LogicalExpr(
                    VariableExpr(aToken),
                    opToken,
                    OptionalGetExpr(VariableExpr(bToken), cToken),
                ),
            ),
            result[0],
        )
    }

    @Test
    fun `parse should handle null coalescing with right associativity`() {
        val aToken = createToken(TemplateTokenType.IDENTIFIER, "a")
        val bToken = createToken(TemplateTokenType.IDENTIFIER, "b")
        val cToken = createToken(TemplateTokenType.IDENTIFIER, "c")
        val op1Token = createToken(TemplateTokenType.QUESTION_QUESTION, "??")
        val op2Token = createToken(TemplateTokenType.QUESTION_QUESTION, "??")

        val tokens = listOf(
            createToken(TemplateTokenType.DELIMITER_OPEN, "{"),
            aToken,
            op1Token,
            bToken,
            op2Token,
            cToken,
            createToken(TemplateTokenType.DELIMITER_CLOSE, "}"),
        )

        val result = parser.parse(tokens)

        assertEquals(1, result.size)
        assertEquals(
            ExpressionStmt(
                LogicalExpr(
                    VariableExpr(aToken),
                    op1Token,
                    LogicalExpr(VariableExpr(bToken), op2Token, VariableExpr(cToken)),
                ),
            ),
            result[0],
        )
    }

    @Test
    fun `parse should handle zero-parameter lambda`() {
        val tokens = listOf(
            createToken(TemplateTokenType.DELIMITER_OPEN, "{"),
            createToken(TemplateTokenType.VAR, "var"),
            createToken(TemplateTokenType.IDENTIFIER, "fn"),
            createToken(TemplateTokenType.EQUAL, "="),
            createToken(TemplateTokenType.LEFT_PAREN, "("),
            createToken(TemplateTokenType.RIGHT_PAREN, ")"),
            createToken(TemplateTokenType.EQUAL_GREATER, "=>"),
            createToken(TemplateTokenType.INTEGER, "42", 42),
            createToken(TemplateTokenType.DELIMITER_CLOSE, "}"),
        )

        val result = parser.parse(tokens)
        assertEquals(1, result.size)

        val stmt = result[0] as VarStmt
        assertEquals("fn", stmt.name.lexeme)

        val lambda = stmt.initializer as LambdaExpr
        assertEquals(0, lambda.params.size)
        assertEquals(LiteralExpr(42), lambda.body)
    }

    @Test
    fun `parse should handle single-parameter lambda`() {
        val xToken = createToken(TemplateTokenType.IDENTIFIER, "x")
        val starToken = createToken(TemplateTokenType.STAR, "*")

        val tokens = listOf(
            createToken(TemplateTokenType.DELIMITER_OPEN, "{"),
            createToken(TemplateTokenType.VAR, "var"),
            createToken(TemplateTokenType.IDENTIFIER, "double"),
            createToken(TemplateTokenType.EQUAL, "="),
            createToken(TemplateTokenType.LEFT_PAREN, "("),
            xToken,
            createToken(TemplateTokenType.RIGHT_PAREN, ")"),
            createToken(TemplateTokenType.EQUAL_GREATER, "=>"),
            xToken,
            starToken,
            createToken(TemplateTokenType.INTEGER, "2", 2),
            createToken(TemplateTokenType.DELIMITER_CLOSE, "}"),
        )

        val result = parser.parse(tokens)
        assertEquals(1, result.size)

        val stmt = result[0] as VarStmt
        val lambda = stmt.initializer as LambdaExpr
        assertEquals(1, lambda.params.size)
        assertEquals("x", lambda.params[0].lexeme)

        val expectedBody = BinaryExpr(
            VariableExpr(xToken),
            starToken,
            LiteralExpr(2),
        )
        assertEquals(expectedBody, lambda.body)
    }

    @Test
    fun `parse should handle multi-parameter lambda`() {
        val xToken = createToken(TemplateTokenType.IDENTIFIER, "x")
        val yToken = createToken(TemplateTokenType.IDENTIFIER, "y")
        val plusToken = createToken(TemplateTokenType.PLUS, "+")

        val tokens = listOf(
            createToken(TemplateTokenType.DELIMITER_OPEN, "{"),
            createToken(TemplateTokenType.VAR, "var"),
            createToken(TemplateTokenType.IDENTIFIER, "add"),
            createToken(TemplateTokenType.EQUAL, "="),
            createToken(TemplateTokenType.LEFT_PAREN, "("),
            xToken,
            createToken(TemplateTokenType.COMMA, ","),
            yToken,
            createToken(TemplateTokenType.RIGHT_PAREN, ")"),
            createToken(TemplateTokenType.EQUAL_GREATER, "=>"),
            xToken,
            plusToken,
            yToken,
            createToken(TemplateTokenType.DELIMITER_CLOSE, "}"),
        )

        val result = parser.parse(tokens)
        val stmt = result[0] as VarStmt
        val lambda = stmt.initializer as LambdaExpr
        assertEquals(2, lambda.params.size)
        assertEquals("x", lambda.params[0].lexeme)
        assertEquals("y", lambda.params[1].lexeme)

        val expectedBody = BinaryExpr(
            VariableExpr(xToken),
            plusToken,
            VariableExpr(yToken),
        )
        assertEquals(expectedBody, lambda.body)
    }

    @Test
    fun `parse should distinguish lambda from grouping expression`() {
        val xToken = createToken(TemplateTokenType.IDENTIFIER, "x")

        val tokensGrouping = listOf(
            createToken(TemplateTokenType.DELIMITER_OPEN, "{"),
            createToken(TemplateTokenType.LEFT_PAREN, "("),
            xToken,
            createToken(TemplateTokenType.RIGHT_PAREN, ")"),
            createToken(TemplateTokenType.DELIMITER_CLOSE, "}"),
        )

        val resultGrouping = parser.parse(tokensGrouping)
        val exprStmt = resultGrouping[0] as ExpressionStmt
        assertEquals(GroupingExpr(VariableExpr(xToken)), exprStmt.expr)

        val tokensLambda = listOf(
            createToken(TemplateTokenType.DELIMITER_OPEN, "{"),
            createToken(TemplateTokenType.LEFT_PAREN, "("),
            xToken,
            createToken(TemplateTokenType.RIGHT_PAREN, ")"),
            createToken(TemplateTokenType.EQUAL_GREATER, "=>"),
            xToken,
            createToken(TemplateTokenType.DELIMITER_CLOSE, "}"),
        )

        val resultLambda = parser.parse(tokensLambda)
        val exprStmt2 = resultLambda[0] as ExpressionStmt
        val lambda = exprStmt2.expr as LambdaExpr
        assertEquals(1, lambda.params.size)
        assertEquals("x", lambda.params[0].lexeme)
        assertEquals(VariableExpr(xToken), lambda.body)
    }

    @Test
    fun `parse should handle lambda as function call argument`() {
        val xToken = createToken(TemplateTokenType.IDENTIFIER, "x")
        val applyToken = createToken(TemplateTokenType.IDENTIFIER, "apply")
        val plusToken = createToken(TemplateTokenType.PLUS, "+")

        val tokens = listOf(
            createToken(TemplateTokenType.DELIMITER_OPEN, "{"),
            applyToken,
            createToken(TemplateTokenType.LEFT_PAREN, "("),
            createToken(TemplateTokenType.LEFT_PAREN, "("),
            xToken,
            createToken(TemplateTokenType.RIGHT_PAREN, ")"),
            createToken(TemplateTokenType.EQUAL_GREATER, "=>"),
            xToken,
            plusToken,
            createToken(TemplateTokenType.INTEGER, "1", 1),
            createToken(TemplateTokenType.COMMA, ","),
            createToken(TemplateTokenType.INTEGER, "5", 5),
            createToken(TemplateTokenType.RIGHT_PAREN, ")"),
            createToken(TemplateTokenType.DELIMITER_CLOSE, "}"),
        )

        val result = parser.parse(tokens)
        val exprStmt = result[0] as ExpressionStmt
        val callExpr = exprStmt.expr as CallExpr

        assertEquals(VariableExpr(applyToken), callExpr.callee)
        assertEquals(2, callExpr.args.size)

        val lambdaArg = callExpr.args[0] as LambdaExpr
        assertEquals(1, lambdaArg.params.size)
        assertEquals("x", lambdaArg.params[0].lexeme)
    }
}


