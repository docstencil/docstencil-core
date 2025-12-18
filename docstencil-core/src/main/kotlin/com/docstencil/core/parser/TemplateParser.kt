package com.docstencil.core.parser

import com.docstencil.core.error.TemplaterException
import com.docstencil.core.parser.model.*
import com.docstencil.core.scanner.model.TemplateToken
import com.docstencil.core.scanner.model.TemplateTokenType
import com.docstencil.core.scanner.model.XmlInputToken

class TemplateParser {
    private class Cursor(private val tokens: List<TemplateToken>) {
        var current = 0

        fun parse(): List<Stmt> {
            return program()
        }

        private fun program(): List<Stmt> {
            val stmts = mutableListOf<Stmt>()

            while (!isAtEnd()) {
                stmts.add(stmt())
            }

            return stmts
        }

        private fun varDeclaration(): Stmt {
            val name = consume(TemplateTokenType.IDENTIFIER, "Expect variable name.")
            consume(TemplateTokenType.EQUAL, "Expect initializing statement after variable name.")
            val initializer: Expr = expression()

            consume(
                TemplateTokenType.DELIMITER_CLOSE,
                "Expect closing delimiter after variable declaration.",
            )
            return VarStmt(name, initializer)
        }

        private fun stmt(): Stmt {
            if (match(TemplateTokenType.VERBATIM)) return verbatimStmt()
            if (match(TemplateTokenType.SENTINEL)) return verbatimStmt()

            consume(
                TemplateTokenType.DELIMITER_OPEN,
                "Expect DELIMITER_OPEN at the beginning of a statement.",
            )

            if (match(TemplateTokenType.DELIMITER_CLOSE)) {
                // An empty {} statement becomes null and disappears from the output.
                // This regularly happens with {/* comments */}.
                return ExpressionStmt(LiteralExpr(null))
            }

            // Consume optional @ prefix: {@tag ...}
            if (match(TemplateTokenType.AT) && !match(TemplateTokenType.STRING)) {
                throw TemplaterException.ParseError(
                    "Expected tag name string literal after '@'",
                    peek(),
                )
            }

            if (match(TemplateTokenType.DO)) return doStmt()
            if (match(TemplateTokenType.FOR)) return forStmt()
            if (match(TemplateTokenType.IF)) return ifStmt()
            if (match(TemplateTokenType.INSERT)) return insertStmt()
            if (match(TemplateTokenType.REWRITE)) return rewriteStmt()
            if (match(TemplateTokenType.VAR)) return varDeclaration()

            return expressionStmt()
        }

        private fun verbatimStmt(): VerbatimStmt {
            val xml = previous().xml
                ?: throw TemplaterException.FatalError("VerbatimStmt needs to have input token set.")
            if (xml !is XmlInputToken.Raw && xml !is XmlInputToken.Sentinel) {
                throw TemplaterException.FatalError("VerbatimStmt requires XmlInputToken.Raw or XmlInputToken.Sentinel.")
            }
            return VerbatimStmt(xml)
        }

        private fun expressionStmt(): ExpressionStmt {
            val expr = expression()
            consume(TemplateTokenType.DELIMITER_CLOSE, "Expect closing delimiter after expression.")
            return ExpressionStmt(expr)
        }

        private fun block(): List<Stmt> {
            val stmts = mutableListOf<Stmt>()

            while (
                !(check(TemplateTokenType.DELIMITER_OPEN) &&
                        checkNext(TemplateTokenType.END)) &&
                !isAtEnd()
            ) {
                stmts.add(stmt())
            }

            consume(
                TemplateTokenType.DELIMITER_OPEN,
                "Expect _DELIMITER_OPEN_, END, DELIMITER_CLOSE after block.",
            )
            consume(
                TemplateTokenType.END,
                "Expect DELIMITER_OPEN, _END_, DELIMITER_CLOSE after block.",
            )
            consume(
                TemplateTokenType.DELIMITER_CLOSE,
                "Expect DELIMITER_OPEN, END, _DELIMITER_CLOSE_ after block.",
            )
            return stmts
        }

        private fun ifStmt(): IfStmt {
            val condition = expression()

            consume(
                TemplateTokenType.DELIMITER_CLOSE,
                "Expect closing delimiter after 'if' condition.",
            )

            val thenBranch = block()

            return IfStmt(condition, BlockStmt(thenBranch))
        }

        private fun insertStmt(): InsertStmt {
            val insertKeyword = previous()
            val expression = expression()

            consume(
                TemplateTokenType.DELIMITER_CLOSE,
                "Expect closing delimiter after 'insert' expression.",
            )

            val body = block()

            return InsertStmt(insertKeyword, expression, BlockStmt(body))
        }

        private fun rewriteStmt(): RewriteStmt {
            val rewriteKeyword = previous()
            val expression = expression()

            consume(
                TemplateTokenType.DELIMITER_CLOSE,
                "Expect closing delimiter after 'rewrite' expression.",
            )

            val body = block()

            return RewriteStmt(rewriteKeyword, expression, BlockStmt(body))
        }

        private fun forStmt(): Stmt {
            val loopVar =
                consume(TemplateTokenType.IDENTIFIER, "Expect identifier at start of 'for' loop.")
            consume(TemplateTokenType.IN, "Expect 'in' after loop variable identifier.")
            val iterable = expression()

            consume(
                TemplateTokenType.DELIMITER_CLOSE,
                "Expect closing delimiter after 'for' clause.",
            )
            val body = block()

            return ForStmt(loopVar, iterable, BlockStmt(body))
        }

        private fun doStmt(): DoStmt {
            val expr = expression()
            consume(
                TemplateTokenType.DELIMITER_CLOSE,
                "Expect closing delimiter after 'do' expression.",
            )
            return DoStmt(expr)
        }

        private fun expression(): Expr {
            return assignment()
        }

        private fun assignment(): Expr {
            val expr = nullCoalescing()

            if (match(TemplateTokenType.EQUAL)) {
                val value = expression()
                if (expr is VariableExpr) {
                    return AssignExpr(expr.name, value)
                } else if (expr is GetExpr) {
                    return SetExpr(expr.obj, expr.name, value)
                }

                throw TemplaterException.ParseError("Invalid assignment target.", previous())
            }

            return expr
        }

        private fun nullCoalescing(): Expr {
            val expr = pipe()

            if (match(TemplateTokenType.QUESTION_QUESTION)) {
                val operator = previous()
                val right = nullCoalescing()
                return LogicalExpr(expr, operator, right)
            }

            return expr
        }

        private fun pipe(): Expr {
            var expr = or()

            while (match(TemplateTokenType.PIPE)) {
                val operator = previous()
                val right = or()
                expr = PipeExpr(expr, operator, right)
            }

            return expr
        }

        private fun or(): Expr {
            val expr = and()

            if (match(TemplateTokenType.OR)) {
                val operator = previous()
                val right = expression()
                return LogicalExpr(expr, operator, right)
            }

            return expr
        }

        private fun and(): Expr {
            val expr = equality()

            if (match(TemplateTokenType.AND)) {
                val operator = previous()
                val right = expression()
                return LogicalExpr(expr, operator, right)
            }

            return expr
        }

        private fun equality(): Expr {
            var expr = comparison()

            while (match(TemplateTokenType.BANG_EQUAL, TemplateTokenType.EQUAL_EQUAL)) {
                val operator = previous()
                val right = comparison()
                expr = BinaryExpr(expr, operator, right)
            }

            return expr
        }

        private fun comparison(): Expr {
            var expr = term()

            while (match(
                    TemplateTokenType.GREATER,
                    TemplateTokenType.GREATER_EQUAL,
                    TemplateTokenType.LESS,
                    TemplateTokenType.LESS_EQUAL,
                )
            ) {
                val operator = previous()
                val right = term()
                expr = BinaryExpr(expr, operator, right)
            }

            return expr
        }

        private fun term(): Expr {
            var expr = factor()

            while (match(TemplateTokenType.PLUS, TemplateTokenType.MINUS)) {
                val operator = previous()
                val right = factor()
                expr = BinaryExpr(expr, operator, right)
            }

            return expr
        }

        private fun factor(): Expr {
            var expr = unary()

            while (match(TemplateTokenType.SLASH, TemplateTokenType.STAR)) {
                val operator = previous()
                val right = unary()
                expr = BinaryExpr(expr, operator, right)
            }

            return expr
        }

        private fun unary(): Expr {
            if (match(TemplateTokenType.BANG, TemplateTokenType.MINUS)) {
                val operator = previous()
                val right = unary()
                return UnaryExpr(operator, right)
            }

            return call()
        }

        private fun call(): Expr {
            var expr = primary()

            while (true) {
                if (match(TemplateTokenType.LEFT_PAREN)) {
                    expr = finishCall(expr)
                } else if (match(TemplateTokenType.DOT)) {
                    val name =
                        consume(TemplateTokenType.IDENTIFIER, "Expect property name after '.'.")
                    expr = GetExpr(expr, name)
                } else if (match(TemplateTokenType.DOT_QUESTION)) {
                    val name =
                        consume(TemplateTokenType.IDENTIFIER, "Expect property name after '.?'.")
                    expr = OptionalGetExpr(expr, name)
                } else {
                    break
                }
            }

            return expr
        }

        private fun finishCall(callee: Expr): Expr {
            val args = mutableListOf<Expr>()
            if (!check(TemplateTokenType.RIGHT_PAREN)) {
                args.add(expression())
                while (match(TemplateTokenType.COMMA)) {
                    if (args.size >= 255) {
                        throw TemplaterException.ParseError(
                            "Can't have more than 255 arguments.",
                            peek(),
                        )
                    }
                    args.add(expression())
                }
            }

            val paren = consume(TemplateTokenType.RIGHT_PAREN, "Expect ')' after arguments.")

            return CallExpr(callee, paren, args)
        }

        private fun primary(): Expr {
            if (match(TemplateTokenType.TRUE)) return LiteralExpr(true)
            if (match(TemplateTokenType.FALSE)) return LiteralExpr(false)
            if (match(TemplateTokenType.NULL)) return LiteralExpr(null)

            if (match(TemplateTokenType.CASE)) return caseExpr()

            if (match(
                    TemplateTokenType.INTEGER,
                    TemplateTokenType.DOUBLE,
                    TemplateTokenType.STRING,
                )
            ) return LiteralExpr(previous().literal)
            if (match(TemplateTokenType.IDENTIFIER)) return VariableExpr(previous())

            if (match(TemplateTokenType.LEFT_PAREN)) {
                return lambdaOrGrouping()
            }

            throw TemplaterException.ParseError("Expected expression.", peek())
        }

        private fun lambdaOrGrouping(): Expr {
            if (isLambda()) {
                return lambda()
            }

            // Check for (id) grouping (not a lambda)
            if (check(TemplateTokenType.IDENTIFIER) && checkNext(TemplateTokenType.RIGHT_PAREN)) {
                val param = advance()
                advance() // consume RIGHT_PAREN
                return GroupingExpr(VariableExpr(param))
            }

            // Not a lambda pattern, parse as regular grouping expression
            val expr = expression()
            consume(TemplateTokenType.RIGHT_PAREN, "Expected ')' after expression.")
            return GroupingExpr(expr)
        }

        private fun isLambda(): Boolean {
            return isLambdaWithZeroArgs() || isLambdaWithOneArg() || isLambdaWithMultipleArgs()
        }

        private fun isLambdaWithZeroArgs(): Boolean {
            return check(TemplateTokenType.RIGHT_PAREN) &&
                    checkNext(TemplateTokenType.EQUAL_GREATER)
        }

        private fun isLambdaWithOneArg(): Boolean {
            return check(TemplateTokenType.IDENTIFIER) &&
                    checkNext(TemplateTokenType.RIGHT_PAREN) &&
                    checkVeryNext(TemplateTokenType.EQUAL_GREATER)
        }

        private fun isLambdaWithMultipleArgs(): Boolean {
            return check(TemplateTokenType.IDENTIFIER) &&
                    checkNext(TemplateTokenType.COMMA)
        }

        private fun lambda(): Expr {
            val params = mutableListOf<TemplateToken>()

            // Parse parameters
            if (!check(TemplateTokenType.RIGHT_PAREN)) {
                // At least one parameter
                params.add(
                    consume(
                        TemplateTokenType.IDENTIFIER,
                        "Expect parameter name.",
                    ),
                )

                while (match(TemplateTokenType.COMMA)) {
                    params.add(
                        consume(
                            TemplateTokenType.IDENTIFIER,
                            "Expect parameter name.",
                        ),
                    )
                }
            }

            consume(TemplateTokenType.RIGHT_PAREN, "Expect ')' after parameters.")
            consume(TemplateTokenType.EQUAL_GREATER, "Expect '=>' after parameters.")

            val body = expression()

            return LambdaExpr(params, body)
        }

        private fun caseExpr(): Expr {
            val branches = mutableListOf<WhenBranch>()

            if (!check(TemplateTokenType.WHEN)) {
                throw TemplaterException.ParseError("Expect 'when' after 'case'.", peek())
            }

            while (match(TemplateTokenType.WHEN)) {
                val whenKeyword = previous()
                val condition = expression()

                val thenKeyword = consume(
                    TemplateTokenType.THEN,
                    "Expect 'then' after when condition.",
                )

                val result = expression()

                branches.add(WhenBranch(whenKeyword, condition, thenKeyword, result))
            }

            var elseBranch: Expr? = null
            if (match(TemplateTokenType.ELSE)) {
                elseBranch = expression()
            }

            consume(TemplateTokenType.END, "Expect 'end' to close case expression.")

            return CaseExpr(branches, elseBranch)
        }

        private fun isAtEnd(): Boolean {
            return current >= tokens.size
        }

        private fun match(vararg tokenTypes: TemplateTokenType): Boolean {
            if (isAtEnd()) {
                return false
            }
            for (tokenType in tokenTypes) {
                if (check(tokenType)) {
                    advance()
                    return true
                }
            }

            return false
        }

        private fun check(tokenType: TemplateTokenType): Boolean {
            if (isAtEnd()) return false
            return tokens[current].type == tokenType
        }

        @Suppress("SameParameterValue")
        private fun checkNext(tokenType: TemplateTokenType): Boolean {
            if (current >= tokens.size - 1) return false
            return tokens[current + 1].type == tokenType
        }

        @Suppress("SameParameterValue")
        private fun checkVeryNext(tokenType: TemplateTokenType): Boolean {
            if (current >= tokens.size - 2) return false
            return tokens[current + 2].type == tokenType
        }

        private fun advance(): TemplateToken {
            if (!isAtEnd()) ++current
            return previous()
        }

        private fun peek(): TemplateToken {
            return tokens[current]
        }

        private fun previous(): TemplateToken {
            return tokens[current - 1]
        }

        private fun consume(
            expectedTokenType: TemplateTokenType,
            errorMessage: String,
        ): TemplateToken {
            if (check(expectedTokenType)) {
                return advance()
            }
            throw TemplaterException.ParseError(errorMessage, peek())
        }
    }

    fun parse(tokens: List<TemplateToken>): List<Stmt> {
        return Cursor(tokens).parse()
    }
}
