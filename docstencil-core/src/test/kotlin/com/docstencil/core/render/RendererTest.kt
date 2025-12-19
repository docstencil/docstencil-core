package com.docstencil.core.render

import com.docstencil.core.api.OfficeTemplateOptions
import com.docstencil.core.config.FileTypeConfig
import com.docstencil.core.error.TemplaterException
import com.docstencil.core.parser.model.*
import com.docstencil.core.render.model.XmlOutputToken
import com.docstencil.core.scanner.model.*
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.*
import kotlin.test.*

class RendererTest {
    data class Person(var name: String, val age: Int, var salary: Double)

    data class PersonNullable(var name: String?, val age: Int?, var salary: Double?)

    data class NestedObject(var child: Person)

    private fun createGlobals(): Globals {
        return Globals.builder(OfficeTemplateOptions(), FileTypeConfig.docx(), emptyList()).build()
    }

    private fun createToken(
        type: TemplateTokenType,
        lexeme: String,
        literal: Any? = null,
    ): TemplateToken {
        return TemplateToken(type, lexeme, null, literal, ContentIdx(0))
    }

    @Suppress("SameParameterValue")
    private fun createRawToken(text: String): XmlInputToken.Raw {
        return XmlInputToken.Raw(XmlRawInputToken.Verbatim(0, text))
    }

    private data class InterpreterWithEnv(
        val interpreter: Renderer.Interpreter,
        val env: Environment,
    )

    private fun createInterpreterWithEnv(
        program: List<Stmt> = listOf(),
        globalEnv: Environment = Globals.builder(
            OfficeTemplateOptions(),
            FileTypeConfig.docx(),
            emptyList(),
        ).build().env,
    ): InterpreterWithEnv {
        val interpreter = Renderer(globalEnv).createInterpreterForTesting(program)
        return InterpreterWithEnv(interpreter, globalEnv)
    }

    private fun assertTokensEqual(expected: List<XmlOutputToken>, actual: List<XmlOutputToken>) {
        assertEquals(
            expected.joinToString("") { it.xmlString },
            actual.joinToString("") { it.xmlString },
        )
    }

    @Test
    fun `visitLiteralExpr should return integer literal value`() {
        val expr = LiteralExpr(42)
        val (interpreter, env) = createInterpreterWithEnv()
        val result = interpreter.visitLiteralExpr(expr, env)
        assertEquals(42, result)
    }

    @Test
    fun `visitLiteralExpr should return double literal value`() {
        val expr = LiteralExpr(3.14)
        val (interpreter, env) = createInterpreterWithEnv()
        val result = interpreter.visitLiteralExpr(expr, env)
        assertEquals(3.14, result)
    }

    @Test
    fun `visitLiteralExpr should return string literal value`() {
        val expr = LiteralExpr("hello")
        val (interpreter, env) = createInterpreterWithEnv()
        val result = interpreter.visitLiteralExpr(expr, env)
        assertEquals("hello", result)
    }

    @Test
    fun `visitLiteralExpr should return null literal value`() {
        val expr = LiteralExpr(null)
        val (interpreter, env) = createInterpreterWithEnv()
        val result = interpreter.visitLiteralExpr(expr, env)
        assertNull(result)
    }

    @Test
    fun `visitLiteralExpr should return boolean literal value`() {
        val expr = LiteralExpr(true)
        val (interpreter, env) = createInterpreterWithEnv()
        val result = interpreter.visitLiteralExpr(expr, env)
        assertEquals(true, result)
    }

    @Test
    fun `visitVariableExpr should return variable value from environment`() {
        val globalEnv = createGlobals().env
        globalEnv.define("x", 42)
        val token = createToken(TemplateTokenType.IDENTIFIER, "x")
        val expr = VariableExpr(token)
        val (interpreter, env) = createInterpreterWithEnv(globalEnv = globalEnv)
        val result = interpreter.visitVariableExpr(expr, env)
        assertEquals(42, result)
    }

    @Test
    fun `visitVariableExpr should throw for undefined variable`() {
        val token = createToken(TemplateTokenType.IDENTIFIER, "undefined")
        val expr = VariableExpr(token)
        val (interpreter, env) = createInterpreterWithEnv()
        assertFailsWith<TemplaterException.RuntimeError> {
            interpreter.visitVariableExpr(expr, env)
        }
    }

    @Test
    fun `visitVariableExpr should return null for null variable value`() {
        val globalEnv = createGlobals().env
        globalEnv.define("x", null)
        val token = createToken(TemplateTokenType.IDENTIFIER, "x")
        val expr = VariableExpr(token)
        val (interpreter, env) = createInterpreterWithEnv(globalEnv = globalEnv)
        val result = interpreter.visitVariableExpr(expr, env)
        assertNull(result)
    }

    @Test
    fun `visitBinaryExpr should add two integers`() {
        val left = LiteralExpr(5)
        val right = LiteralExpr(3)
        val operator = createToken(TemplateTokenType.PLUS, "+")
        val expr = BinaryExpr(left, operator, right)
        val (interpreter, env) = createInterpreterWithEnv()
        val result = interpreter.visitBinaryExpr(expr, env)
        assertEquals(8, result)
    }

    @Test
    fun `visitBinaryExpr should subtract two integers`() {
        val left = LiteralExpr(10)
        val right = LiteralExpr(4)
        val operator = createToken(TemplateTokenType.MINUS, "-")
        val expr = BinaryExpr(left, operator, right)
        val (interpreter, env) = createInterpreterWithEnv()
        val result = interpreter.visitBinaryExpr(expr, env)
        assertEquals(6, result)
    }

    @Test
    fun `visitBinaryExpr should multiply two integers`() {
        val left = LiteralExpr(6)
        val right = LiteralExpr(7)
        val operator = createToken(TemplateTokenType.STAR, "*")
        val expr = BinaryExpr(left, operator, right)
        val (interpreter, env) = createInterpreterWithEnv()
        val result = interpreter.visitBinaryExpr(expr, env)
        assertEquals(42, result)
    }

    @Test
    fun `visitBinaryExpr should divide two integers returning double`() {
        val left = LiteralExpr(10)
        val right = LiteralExpr(4)
        val operator = createToken(TemplateTokenType.SLASH, "/")
        val expr = BinaryExpr(left, operator, right)
        val (interpreter, env) = createInterpreterWithEnv()
        val result = interpreter.visitBinaryExpr(expr, env)
        assertEquals(2, result)
    }

    @Test
    fun `visitBinaryExpr should calculate modulo`() {
        val left = LiteralExpr(10)
        val right = LiteralExpr(3)
        val operator = createToken(TemplateTokenType.PERCENT, "%")
        val expr = BinaryExpr(left, operator, right)
        val (interpreter, env) = createInterpreterWithEnv()
        val result = interpreter.visitBinaryExpr(expr, env)
        assertEquals(1, result)
    }

    @Test
    fun `visitBinaryExpr should concatenate strings with addition`() {
        val left = LiteralExpr("hello")
        val right = LiteralExpr("world")
        val operator = createToken(TemplateTokenType.PLUS, "+")
        val expr = BinaryExpr(left, operator, right)
        val (interpreter, env) = createInterpreterWithEnv()
        val result = interpreter.visitBinaryExpr(expr, env)
        assertEquals("helloworld", result)
    }

    @Test
    fun `visitBinaryExpr should compare integers with greater than`() {
        val left = LiteralExpr(5)
        val right = LiteralExpr(3)
        val operator = createToken(TemplateTokenType.GREATER, ">")
        val expr = BinaryExpr(left, operator, right)
        val (interpreter, env) = createInterpreterWithEnv()
        val result = interpreter.visitBinaryExpr(expr, env)
        assertEquals(true, result)
    }

    @Test
    fun `visitBinaryExpr should compare integers with less than`() {
        val left = LiteralExpr(3)
        val right = LiteralExpr(5)
        val operator = createToken(TemplateTokenType.LESS, "<")
        val expr = BinaryExpr(left, operator, right)
        val (interpreter, env) = createInterpreterWithEnv()
        val result = interpreter.visitBinaryExpr(expr, env)
        assertEquals(true, result)
    }

    @Test
    fun `visitBinaryExpr should check equality`() {
        val left = LiteralExpr(42)
        val right = LiteralExpr(42)
        val operator = createToken(TemplateTokenType.EQUAL_EQUAL, "==")
        val expr = BinaryExpr(left, operator, right)
        val (interpreter, env) = createInterpreterWithEnv()
        val result = interpreter.visitBinaryExpr(expr, env)
        assertEquals(true, result)
    }

    @Test
    fun `visitBinaryExpr should check inequality`() {
        val left = LiteralExpr(42)
        val right = LiteralExpr(43)
        val operator = createToken(TemplateTokenType.BANG_EQUAL, "!=")
        val expr = BinaryExpr(left, operator, right)
        val (interpreter, env) = createInterpreterWithEnv()
        val result = interpreter.visitBinaryExpr(expr, env)
        assertEquals(true, result)
    }

    @Test
    fun `visitBinaryExpr should throw on division by zero`() {
        val left = LiteralExpr(10)
        val right = LiteralExpr(0)
        val operator = createToken(TemplateTokenType.SLASH, "/")
        val expr = BinaryExpr(left, operator, right)
        val (interpreter, env) = createInterpreterWithEnv()
        assertFailsWith<TemplaterException.RuntimeError> {
            interpreter.visitBinaryExpr(expr, env)
        }
    }

    @Test
    fun `visitUnaryExpr should negate integer`() {
        val right = LiteralExpr(42)
        val operator = createToken(TemplateTokenType.MINUS, "-")
        val expr = UnaryExpr(operator, right)
        val (interpreter, env) = createInterpreterWithEnv()
        val result = interpreter.visitUnaryExpr(expr, env)
        assertEquals(-42, result)
    }

    @Test
    fun `visitUnaryExpr should negate double`() {
        val right = LiteralExpr(3.14)
        val operator = createToken(TemplateTokenType.MINUS, "-")
        val expr = UnaryExpr(operator, right)
        val (interpreter, env) = createInterpreterWithEnv()
        val result = interpreter.visitUnaryExpr(expr, env)
        assertEquals(-3.14, result)
    }

    @Test
    fun `visitUnaryExpr should apply logical not to true`() {
        val right = LiteralExpr(true)
        val operator = createToken(TemplateTokenType.BANG, "!")
        val expr = UnaryExpr(operator, right)
        val (interpreter, env) = createInterpreterWithEnv()
        val result = interpreter.visitUnaryExpr(expr, env)
        assertEquals(false, result)
    }

    @Test
    fun `visitUnaryExpr should apply logical not to false`() {
        val right = LiteralExpr(false)
        val operator = createToken(TemplateTokenType.BANG, "!")
        val expr = UnaryExpr(operator, right)
        val (interpreter, env) = createInterpreterWithEnv()
        val result = interpreter.visitUnaryExpr(expr, env)
        assertEquals(true, result)
    }

    @Test
    fun `visitUnaryExpr should treat null as falsy`() {
        val right = LiteralExpr(null)
        val operator = createToken(TemplateTokenType.BANG, "!")
        val expr = UnaryExpr(operator, right)
        val (interpreter, env) = createInterpreterWithEnv()
        val result = interpreter.visitUnaryExpr(expr, env)
        assertEquals(true, result)
    }

    @Test
    fun `visitUnaryExpr should throw on negating non-number`() {
        val right = LiteralExpr("string")
        val operator = createToken(TemplateTokenType.MINUS, "-")
        val expr = UnaryExpr(operator, right)
        val (interpreter, env) = createInterpreterWithEnv()
        assertFailsWith<TemplaterException.RuntimeError> {
            interpreter.visitUnaryExpr(expr, env)
        }
    }

    @Test
    fun `visitGroupingExpr should preserve operator precedence`() {
        val three = LiteralExpr(3)
        val four = LiteralExpr(4)
        val plusOp = createToken(TemplateTokenType.PLUS, "+")
        val innerBinary = BinaryExpr(three, plusOp, four)
        val grouped = GroupingExpr(innerBinary)
        val two = LiteralExpr(2)
        val starOp = createToken(TemplateTokenType.STAR, "*")
        val expr = BinaryExpr(two, starOp, grouped)
        val (interpreter, env) = createInterpreterWithEnv()
        val result = interpreter.visitBinaryExpr(expr, env)
        assertEquals(14, result)
    }

    @Test
    fun `visitLogicalExpr should short-circuit OR with truthy left`() {
        val left = LiteralExpr(true)
        val undefinedToken = createToken(TemplateTokenType.IDENTIFIER, "undefined")
        val right = VariableExpr(undefinedToken)
        val operator = createToken(TemplateTokenType.OR, "or")
        val expr = LogicalExpr(left, operator, right)
        val (interpreter, env) = createInterpreterWithEnv()
        val result = interpreter.visitLogicalExpr(expr, env)
        assertEquals(true, result)
    }

    @Test
    fun `visitLogicalExpr should evaluate right side of OR with falsy left`() {
        val left = LiteralExpr(false)
        val right = LiteralExpr(true)
        val operator = createToken(TemplateTokenType.OR, "or")
        val expr = LogicalExpr(left, operator, right)
        val (interpreter, env) = createInterpreterWithEnv()
        val result = interpreter.visitLogicalExpr(expr, env)
        assertEquals(true, result)
    }

    @Test
    fun `visitLogicalExpr should short-circuit AND with falsy left`() {
        val left = LiteralExpr(false)
        val undefinedToken = createToken(TemplateTokenType.IDENTIFIER, "undefined")
        val right = VariableExpr(undefinedToken)
        val operator = createToken(TemplateTokenType.AND, "and")
        val expr = LogicalExpr(left, operator, right)
        val (interpreter, env) = createInterpreterWithEnv()
        val result = interpreter.visitLogicalExpr(expr, env)
        assertEquals(false, result)
    }

    @Test
    fun `visitLogicalExpr should evaluate right side of AND with truthy left`() {
        val left = LiteralExpr(true)
        val right = LiteralExpr(42)
        val operator = createToken(TemplateTokenType.AND, "and")
        val expr = LogicalExpr(left, operator, right)
        val (interpreter, env) = createInterpreterWithEnv()
        val result = interpreter.visitLogicalExpr(expr, env)
        assertEquals(42, result)
    }

    @Test
    fun `visitLogicalExpr should treat empty string as falsy`() {
        val left = LiteralExpr("")
        val right = LiteralExpr("fallback")
        val operator = createToken(TemplateTokenType.OR, "or")
        val expr = LogicalExpr(left, operator, right)
        val (interpreter, env) = createInterpreterWithEnv()
        val result = interpreter.visitLogicalExpr(expr, env)
        assertEquals("fallback", result)
    }

    @Test
    fun `visitCaseExpr should return first matching branch result`() {
        val when1Token = createToken(TemplateTokenType.WHEN, "when")
        val then1Token = createToken(TemplateTokenType.THEN, "then")
        val when2Token = createToken(TemplateTokenType.WHEN, "when")
        val then2Token = createToken(TemplateTokenType.THEN, "then")

        val branch1 = WhenBranch(
            when1Token,
            LiteralExpr(false),
            then1Token,
            LiteralExpr("first"),
        )
        val branch2 = WhenBranch(
            when2Token,
            LiteralExpr(true),
            then2Token,
            LiteralExpr("second"),
        )

        val expr = CaseExpr(listOf(branch1, branch2), null)
        val (interpreter, env) = createInterpreterWithEnv()
        val result = interpreter.visitCaseExpr(expr, env)

        assertEquals("second", result)
    }

    @Test
    fun `visitCaseExpr should not evaluate later branches after match`() {
        val when1Token = createToken(TemplateTokenType.WHEN, "when")
        val then1Token = createToken(TemplateTokenType.THEN, "then")
        val when2Token = createToken(TemplateTokenType.WHEN, "when")
        val then2Token = createToken(TemplateTokenType.THEN, "then")
        val undefinedToken = createToken(TemplateTokenType.IDENTIFIER, "undefined")

        val branch1 = WhenBranch(
            when1Token,
            LiteralExpr(true),
            then1Token,
            LiteralExpr("matched"),
        )
        val branch2 = WhenBranch(
            when2Token,
            VariableExpr(undefinedToken),
            then2Token,
            LiteralExpr("unreachable"),
        )

        val expr = CaseExpr(listOf(branch1, branch2), null)
        val (interpreter, env) = createInterpreterWithEnv()
        val result = interpreter.visitCaseExpr(expr, env)

        assertEquals("matched", result)
    }

    @Test
    fun `visitCaseExpr should return else branch when no match`() {
        val whenToken = createToken(TemplateTokenType.WHEN, "when")
        val thenToken = createToken(TemplateTokenType.THEN, "then")

        val branch = WhenBranch(
            whenToken,
            LiteralExpr(false),
            thenToken,
            LiteralExpr("matched"),
        )

        val expr = CaseExpr(listOf(branch), LiteralExpr("default"))
        val (interpreter, env) = createInterpreterWithEnv()
        val result = interpreter.visitCaseExpr(expr, env)

        assertEquals("default", result)
    }

    @Test
    fun `visitCaseExpr should return null when no match and no else`() {
        val whenToken = createToken(TemplateTokenType.WHEN, "when")
        val thenToken = createToken(TemplateTokenType.THEN, "then")

        val branch = WhenBranch(
            whenToken,
            LiteralExpr(false),
            thenToken,
            LiteralExpr("matched"),
        )

        val expr = CaseExpr(listOf(branch), null)
        val (interpreter, env) = createInterpreterWithEnv()
        val result = interpreter.visitCaseExpr(expr, env)

        assertNull(result)
    }

    @Test
    fun `visitCaseExpr should evaluate expressions with variables`() {
        val globalEnv = createGlobals().env
        globalEnv.define("x", 5)

        val whenToken = createToken(TemplateTokenType.WHEN, "when")
        val xToken = createToken(TemplateTokenType.IDENTIFIER, "x")
        val greaterToken = createToken(TemplateTokenType.GREATER, ">")
        val thenToken = createToken(TemplateTokenType.THEN, "then")
        val plusToken = createToken(TemplateTokenType.PLUS, "+")

        val branch = WhenBranch(
            whenToken,
            BinaryExpr(VariableExpr(xToken), greaterToken, LiteralExpr(3)),
            thenToken,
            BinaryExpr(VariableExpr(xToken), plusToken, LiteralExpr(10)),
        )

        val expr = CaseExpr(listOf(branch), null)
        val (interpreter, env) = createInterpreterWithEnv(globalEnv = globalEnv)
        val result = interpreter.visitCaseExpr(expr, env)

        assertEquals(15, result)
    }

    @Test
    fun `visitCaseExpr should use truthy evaluation for conditions`() {
        val when1Token = createToken(TemplateTokenType.WHEN, "when")
        val then1Token = createToken(TemplateTokenType.THEN, "then")
        val when2Token = createToken(TemplateTokenType.WHEN, "when")
        val then2Token = createToken(TemplateTokenType.THEN, "then")

        val branch1 = WhenBranch(
            when1Token,
            LiteralExpr(0),
            then1Token,
            LiteralExpr("zero"),
        )
        val branch2 = WhenBranch(
            when2Token,
            LiteralExpr("non-empty"),
            then2Token,
            LiteralExpr("string"),
        )

        val expr = CaseExpr(listOf(branch1, branch2), null)
        val (interpreter, env) = createInterpreterWithEnv()
        val result = interpreter.visitCaseExpr(expr, env)

        assertEquals("string", result)
    }

    @Test
    fun `visitCaseExpr should handle multiple branches with complex conditions`() {
        val globalEnv = createGlobals().env
        globalEnv.define("status", "warning")

        val when1Token = createToken(TemplateTokenType.WHEN, "when")
        val statusToken1 = createToken(TemplateTokenType.IDENTIFIER, "status")
        val equal1Token = createToken(TemplateTokenType.EQUAL_EQUAL, "==")
        val then1Token = createToken(TemplateTokenType.THEN, "then")
        val when2Token = createToken(TemplateTokenType.WHEN, "when")
        val statusToken2 = createToken(TemplateTokenType.IDENTIFIER, "status")
        val equal2Token = createToken(TemplateTokenType.EQUAL_EQUAL, "==")
        val then2Token = createToken(TemplateTokenType.THEN, "then")

        val branch1 = WhenBranch(
            when1Token,
            BinaryExpr(VariableExpr(statusToken1), equal1Token, LiteralExpr("error")),
            then1Token,
            LiteralExpr("red"),
        )
        val branch2 = WhenBranch(
            when2Token,
            BinaryExpr(VariableExpr(statusToken2), equal2Token, LiteralExpr("warning")),
            then2Token,
            LiteralExpr("yellow"),
        )

        val expr = CaseExpr(listOf(branch1, branch2), null)
        val (interpreter, env) = createInterpreterWithEnv(globalEnv = globalEnv)
        val result = interpreter.visitCaseExpr(expr, env)

        assertEquals("yellow", result)
    }

    @Test
    fun `visitAssignExpr should assign value to variable`() {
        val globalEnv = createGlobals().env
        globalEnv.define("x", 0)
        val token = createToken(TemplateTokenType.IDENTIFIER, "x")
        val value = LiteralExpr(42)
        val expr = AssignExpr(token, value)

        val (interpreter, env) = createInterpreterWithEnv(globalEnv = globalEnv)

        interpreter.visitAssignExpr(expr, env)
        assertEquals(42, globalEnv.get(token))
    }

    @Test
    fun `visitAssignExpr should return assigned value`() {
        val globalEnv = createGlobals().env
        globalEnv.define("x", 0)

        val token = createToken(TemplateTokenType.IDENTIFIER, "x")
        val value = LiteralExpr(42)
        val expr = AssignExpr(token, value)

        val (interpreter, env) = createInterpreterWithEnv(globalEnv = globalEnv)
        val result = interpreter.visitAssignExpr(expr, env)

        assertEquals(42, result)
    }

    @Test
    fun `visitAssignExpr should allow chained assignment`() {
        val globalEnv = createGlobals().env
        globalEnv.define("a", 0)
        globalEnv.define("b", 0)

        val tokenA = createToken(TemplateTokenType.IDENTIFIER, "a")
        val tokenB = createToken(TemplateTokenType.IDENTIFIER, "b")
        val innerAssign = AssignExpr(tokenB, LiteralExpr(10))
        val outerAssign = AssignExpr(tokenA, innerAssign)

        val (interpreter, env) = createInterpreterWithEnv(globalEnv = globalEnv)
        interpreter.visitAssignExpr(outerAssign, env)

        assertEquals(10, globalEnv.get(tokenA))
        assertEquals(10, globalEnv.get(tokenB))
    }

    @Test
    fun `visitAssignExpr should throw for undefined variable`() {
        val token = createToken(TemplateTokenType.IDENTIFIER, "undefined")
        val value = LiteralExpr(42)
        val expr = AssignExpr(token, value)
        val (interpreter, env) = createInterpreterWithEnv()
        assertFailsWith<TemplaterException.RuntimeError> {
            interpreter.visitAssignExpr(expr, env)
        }
    }

    @Test
    fun `visitGetExpr should read property from object`() {
        val person = Person("Alice", 30, 50000.0)
        val obj = LiteralExpr(person)
        val nameToken = createToken(TemplateTokenType.IDENTIFIER, "name")
        val expr = GetExpr(obj, nameToken)
        val (interpreter, env) = createInterpreterWithEnv()
        val result = interpreter.visitGetExpr(expr, env)
        assertEquals("Alice", result)
    }

    @Test
    fun `visitGetExpr should read val property`() {
        val person = Person("Bob", 25, 60000.0)
        val obj = LiteralExpr(person)
        val ageToken = createToken(TemplateTokenType.IDENTIFIER, "age")
        val expr = GetExpr(obj, ageToken)
        val (interpreter, env) = createInterpreterWithEnv()
        val result = interpreter.visitGetExpr(expr, env)
        assertEquals(25, result)
    }

    @Test
    fun `visitGetExpr should return null property value`() {
        val personWithNull = PersonNullable(null, 30, 50000.0)
        val obj = LiteralExpr(personWithNull)
        val nameToken = createToken(TemplateTokenType.IDENTIFIER, "name")
        val expr = GetExpr(obj, nameToken)
        val (interpreter, env) = createInterpreterWithEnv()
        val result = interpreter.visitGetExpr(expr, env)
        assertNull(result)
    }

    @Test
    fun `visitGetExpr should throw for null object`() {
        val obj = LiteralExpr(null)
        val nameToken = createToken(TemplateTokenType.IDENTIFIER, "name")
        val expr = GetExpr(obj, nameToken)
        val (interpreter, env) = createInterpreterWithEnv()
        assertFailsWith<TemplaterException.RuntimeError> {
            interpreter.visitGetExpr(expr, env)
        }
    }

    @Test
    fun `visitGetExpr should throw for non-existent property`() {
        val person = Person("Charlie", 35, 70000.0)
        val obj = LiteralExpr(person)
        val badToken = createToken(TemplateTokenType.IDENTIFIER, "nonExistent")
        val expr = GetExpr(obj, badToken)
        val (interpreter, env) = createInterpreterWithEnv()
        assertFailsWith<TemplaterException.RuntimeError> {
            interpreter.visitGetExpr(expr, env)
        }
    }

    @Test
    fun `visitGetExpr should handle chained property access`() {
        val child = Person("David", 10, 0.0)
        val parent = NestedObject(child)
        val objExpr = LiteralExpr(parent)
        val childToken = createToken(TemplateTokenType.IDENTIFIER, "child")
        val nameToken = createToken(TemplateTokenType.IDENTIFIER, "name")
        val innerGet = GetExpr(objExpr, childToken)
        val outerGet = GetExpr(innerGet, nameToken)
        val (interpreter, env) = createInterpreterWithEnv()
        val result = interpreter.visitGetExpr(outerGet, env)
        assertEquals("David", result)
    }

    @Test
    fun `visitGetExpr should work with variable as object`() {
        val person = Person("Eve", 28, 65000.0)
        val globalEnv = createGlobals().env
        globalEnv.define("person", person)

        val varToken = createToken(TemplateTokenType.IDENTIFIER, "person")
        val varExpr = VariableExpr(varToken)
        val nameToken = createToken(TemplateTokenType.IDENTIFIER, "name")
        val expr = GetExpr(varExpr, nameToken)

        val (interpreter, env) = createInterpreterWithEnv(globalEnv = globalEnv)
        val result = interpreter.visitGetExpr(expr, env)

        assertEquals("Eve", result)
    }

    @Test
    fun `visitSetExpr should set mutable property`() {
        val person = Person("Frank", 40, 80000.0)
        val obj = LiteralExpr(person)
        val nameToken = createToken(TemplateTokenType.IDENTIFIER, "name")
        val value = LiteralExpr("NewName")
        val expr = SetExpr(obj, nameToken, value)
        val (interpreter, env) = createInterpreterWithEnv()
        interpreter.visitSetExpr(expr, env)
        assertEquals("NewName", person.name)
    }

    @Test
    fun `visitSetExpr should return assigned value`() {
        val person = Person("Grace", 32, 55000.0)
        val obj = LiteralExpr(person)
        val nameToken = createToken(TemplateTokenType.IDENTIFIER, "name")
        val value = LiteralExpr("Alice")
        val expr = SetExpr(obj, nameToken, value)
        val (interpreter, env) = createInterpreterWithEnv()
        val result = interpreter.visitSetExpr(expr, env)
        assertEquals("Alice", result)
    }

    @Test
    fun `visitSetExpr should apply numeric type conversion`() {
        val person = Person("Henry", 28, 65000.0)
        val obj = LiteralExpr(person)
        val salaryToken = createToken(TemplateTokenType.IDENTIFIER, "salary")
        val value = LiteralExpr(75000)
        val expr = SetExpr(obj, salaryToken, value)
        val (interpreter, env) = createInterpreterWithEnv()
        interpreter.visitSetExpr(expr, env)
        assertEquals(75000.0, person.salary)
    }

    @Test
    fun `visitSetExpr should allow chained assignment`() {
        val person1 = Person("Ivy", 30, 60000.0)
        val person2 = Person("Jack", 35, 70000.0)
        val obj1 = LiteralExpr(person1)
        val obj2 = LiteralExpr(person2)
        val nameToken = createToken(TemplateTokenType.IDENTIFIER, "name")
        val innerSet = SetExpr(obj2, nameToken, LiteralExpr("SameName"))
        val outerSet = SetExpr(obj1, nameToken, innerSet)
        val (interpreter, env) = createInterpreterWithEnv()
        interpreter.visitSetExpr(outerSet, env)
        assertEquals("SameName", person1.name)
        assertEquals("SameName", person2.name)
    }

    @Test
    fun `visitSetExpr should throw for null object`() {
        val obj = LiteralExpr(null)
        val nameToken = createToken(TemplateTokenType.IDENTIFIER, "name")
        val value = LiteralExpr("value")
        val expr = SetExpr(obj, nameToken, value)
        val (interpreter, env) = createInterpreterWithEnv()
        assertFailsWith<TemplaterException.RuntimeError> {
            interpreter.visitSetExpr(expr, env)
        }
    }

    @Test
    fun `visitSetExpr should throw for read-only property`() {
        val person = Person("Karen", 45, 90000.0)
        val obj = LiteralExpr(person)
        val ageToken = createToken(TemplateTokenType.IDENTIFIER, "age")
        val value = LiteralExpr(50)
        val expr = SetExpr(obj, ageToken, value)
        val (interpreter, env) = createInterpreterWithEnv()
        assertFailsWith<TemplaterException.RuntimeError> {
            interpreter.visitSetExpr(expr, env)
        }
    }

    @Test
    fun `visitSetExpr should throw for non-existent property`() {
        val person = Person("Leo", 38, 75000.0)
        val obj = LiteralExpr(person)
        val badToken = createToken(TemplateTokenType.IDENTIFIER, "nonExistent")
        val value = LiteralExpr("value")
        val expr = SetExpr(obj, badToken, value)
        val (interpreter, env) = createInterpreterWithEnv()
        assertFailsWith<TemplaterException.RuntimeError> {
            interpreter.visitSetExpr(expr, env)
        }
    }

    @Test
    fun `visitSetExpr should handle chained property access`() {
        val child = Person("Mike", 12, 0.0)
        val parent = NestedObject(child)
        val objExpr = LiteralExpr(parent)
        val childToken = createToken(TemplateTokenType.IDENTIFIER, "child")
        val nameToken = createToken(TemplateTokenType.IDENTIFIER, "name")
        val getExpr = GetExpr(objExpr, childToken)
        val setExpr = SetExpr(getExpr, nameToken, LiteralExpr("NewName"))
        val (interpreter, env) = createInterpreterWithEnv()
        interpreter.visitSetExpr(setExpr, env)
        assertEquals("NewName", child.name)
    }

    @Test
    fun `visitCallExpr should call no-arg lambda`() {
        val globalEnv = createGlobals().env
        val fn: () -> String = { "result" }
        globalEnv.define("fn", fn)

        val fnToken = createToken(TemplateTokenType.IDENTIFIER, "fn")
        val callee = VariableExpr(fnToken)
        val paren = createToken(TemplateTokenType.RIGHT_PAREN, ")")
        val expr = CallExpr(callee, paren, emptyList())

        val (interpreter, env) = createInterpreterWithEnv(globalEnv = globalEnv)
        val result = interpreter.visitCallExpr(expr, env)

        assertEquals("result", result)
    }

    @Test
    fun `visitCallExpr should call lambda with arguments`() {
        val globalEnv = createGlobals().env
        val add: (Int, Int) -> Int = { a, b -> a + b }
        globalEnv.define("add", add)

        val addToken = createToken(TemplateTokenType.IDENTIFIER, "add")
        val callee = VariableExpr(addToken)
        val paren = createToken(TemplateTokenType.RIGHT_PAREN, ")")
        val args = listOf(LiteralExpr(5), LiteralExpr(3))
        val expr = CallExpr(callee, paren, args)

        val (interpreter, env) = createInterpreterWithEnv(globalEnv = globalEnv)
        val result = interpreter.visitCallExpr(expr, env)

        assertEquals(8, result)
    }

    @Test
    fun `visitCallExpr should call Java Supplier`() {
        val globalEnv = createGlobals().env
        val supplier = java.util.function.Supplier { "value" }
        globalEnv.define("supplier", supplier)

        val supplierToken = createToken(TemplateTokenType.IDENTIFIER, "supplier")
        val callee = VariableExpr(supplierToken)
        val paren = createToken(TemplateTokenType.RIGHT_PAREN, ")")
        val expr = CallExpr(callee, paren, emptyList())

        val (interpreter, env) = createInterpreterWithEnv(globalEnv = globalEnv)
        val result = interpreter.visitCallExpr(expr, env)

        assertEquals("value", result)
    }

    @Test
    fun `visitCallExpr should return null from void function`() {
        val globalEnv = createGlobals().env
        val consumer = java.util.function.Consumer<String> { }
        globalEnv.define("consumer", consumer)

        val consumerToken = createToken(TemplateTokenType.IDENTIFIER, "consumer")
        val callee = VariableExpr(consumerToken)
        val paren = createToken(TemplateTokenType.RIGHT_PAREN, ")")
        val args = listOf(LiteralExpr("test"))
        val expr = CallExpr(callee, paren, args)

        val (interpreter, env) = createInterpreterWithEnv(globalEnv = globalEnv)
        val result = interpreter.visitCallExpr(expr, env)

        assertNull(result)
    }

    @Test
    fun `visitCallExpr should handle chained calls`() {
        val globalEnv = createGlobals().env
        val innerFn: () -> String = { "inner" }
        val outerFn: () -> (() -> String) = { innerFn }
        globalEnv.define("outerFn", outerFn)

        val outerToken = createToken(TemplateTokenType.IDENTIFIER, "outerFn")
        val outerCallee = VariableExpr(outerToken)
        val paren = createToken(TemplateTokenType.RIGHT_PAREN, ")")
        val innerCall = CallExpr(outerCallee, paren, emptyList())
        val outerCall = CallExpr(innerCall, paren, emptyList())

        val (interpreter, env) = createInterpreterWithEnv(globalEnv = globalEnv)
        val result = interpreter.visitCallExpr(outerCall, env)

        assertEquals("inner", result)
    }

    @Test
    fun `visitCallExpr should throw for non-callable object`() {
        val notCallable = LiteralExpr(42)
        val paren = createToken(TemplateTokenType.RIGHT_PAREN, ")")
        val expr = CallExpr(notCallable, paren, emptyList())
        val (interpreter, env) = createInterpreterWithEnv()
        assertFailsWith<TemplaterException.RuntimeError> {
            interpreter.visitCallExpr(expr, env)
        }
    }

    @Test
    fun `visitCallExpr should throw for null callee`() {
        val nullCallee = LiteralExpr(null)
        val paren = createToken(TemplateTokenType.RIGHT_PAREN, ")")
        val expr = CallExpr(nullCallee, paren, emptyList())
        val (interpreter, env) = createInterpreterWithEnv()
        assertFailsWith<TemplaterException.RuntimeError> {
            interpreter.visitCallExpr(expr, env)
        }
    }

    @Test
    fun `visitCallExpr should throw for wrong argument count`() {
        val globalEnv = createGlobals().env
        val twoArg: (Int, Int) -> Int = { a, b -> a + b }
        globalEnv.define("twoArg", twoArg)

        val twoArgToken = createToken(TemplateTokenType.IDENTIFIER, "twoArg")
        val callee = VariableExpr(twoArgToken)
        val paren = createToken(TemplateTokenType.RIGHT_PAREN, ")")
        val args = listOf(LiteralExpr(5))
        val expr = CallExpr(callee, paren, args)

        val (interpreter, env) = createInterpreterWithEnv(globalEnv = globalEnv)
        assertFailsWith<TemplaterException.RuntimeError> {
            interpreter.visitCallExpr(expr, env)
        }
    }

    @Test
    fun `visitCallExpr should throw for too many arguments`() {
        val globalEnv = createGlobals().env
        val oneArg: (Int) -> Int = { it * 2 }
        globalEnv.define("oneArg", oneArg)

        val oneArgToken = createToken(TemplateTokenType.IDENTIFIER, "oneArg")
        val callee = VariableExpr(oneArgToken)
        val paren = createToken(TemplateTokenType.RIGHT_PAREN, ")")
        val args = listOf(LiteralExpr(5), LiteralExpr(10), LiteralExpr(15))
        val expr = CallExpr(callee, paren, args)

        val (interpreter, env) = createInterpreterWithEnv(globalEnv = globalEnv)
        assertFailsWith<TemplaterException.RuntimeError> {
            interpreter.visitCallExpr(expr, env)
        }
    }

    @Test
    fun `visitCallExpr should pass evaluated arguments`() {
        val globalEnv = createGlobals().env
        val identity: (Int) -> Int = { it }
        globalEnv.define("identity", identity)

        val identityToken = createToken(TemplateTokenType.IDENTIFIER, "identity")
        val callee = VariableExpr(identityToken)
        val paren = createToken(TemplateTokenType.RIGHT_PAREN, ")")
        val two = LiteralExpr(2)
        val three = LiteralExpr(3)
        val plusOp = createToken(TemplateTokenType.PLUS, "+")
        val argExpr = BinaryExpr(two, plusOp, three)
        val args = listOf(argExpr)
        val expr = CallExpr(callee, paren, args)

        val (interpreter, env) = createInterpreterWithEnv(globalEnv = globalEnv)
        val result = interpreter.visitCallExpr(expr, env)

        assertEquals(5, result)
    }

    @Test
    fun `interpreter should handle nested arithmetic with variables`() {
        val globalEnv = createGlobals().env
        globalEnv.define("x", 5)
        globalEnv.define("y", 3)

        val xToken = createToken(TemplateTokenType.IDENTIFIER, "x")
        val yToken = createToken(TemplateTokenType.IDENTIFIER, "y")
        val xExpr = VariableExpr(xToken)
        val yExpr = VariableExpr(yToken)
        val plusOp = createToken(TemplateTokenType.PLUS, "+")
        val sum = BinaryExpr(xExpr, plusOp, yExpr)
        val grouped = GroupingExpr(sum)
        val two = LiteralExpr(2)
        val starOp = createToken(TemplateTokenType.STAR, "*")
        val expr = BinaryExpr(grouped, starOp, two)

        val (interpreter, env) = createInterpreterWithEnv(globalEnv = globalEnv)
        val result = interpreter.visitBinaryExpr(expr, env)

        assertEquals(16, result)
    }

    @Test
    fun `interpreter should handle property access on function result`() {
        val globalEnv = createGlobals().env
        val factory: () -> Person = { Person("Nina", 29, 62000.0) }
        globalEnv.define("factory", factory)

        val factoryToken = createToken(TemplateTokenType.IDENTIFIER, "factory")
        val callee = VariableExpr(factoryToken)
        val paren = createToken(TemplateTokenType.RIGHT_PAREN, ")")
        val callExpr = CallExpr(callee, paren, emptyList())
        val nameToken = createToken(TemplateTokenType.IDENTIFIER, "name")
        val getExpr = GetExpr(callExpr, nameToken)

        val (interpreter, env) = createInterpreterWithEnv(globalEnv = globalEnv)
        val result = interpreter.visitGetExpr(getExpr, env)

        assertEquals("Nina", result)
    }

    @Test
    fun `interpreter should handle function call with property argument`() {
        val globalEnv = createGlobals().env
        val double: (Int) -> Int = { it * 2 }
        val person = Person("Oscar", 50, 100000.0)
        globalEnv.define("double", double)
        globalEnv.define("person", person)

        val doubleToken = createToken(TemplateTokenType.IDENTIFIER, "double")
        val personToken = createToken(TemplateTokenType.IDENTIFIER, "person")
        val personExpr = VariableExpr(personToken)
        val ageToken = createToken(TemplateTokenType.IDENTIFIER, "age")
        val getExpr = GetExpr(personExpr, ageToken)
        val callee = VariableExpr(doubleToken)
        val paren = createToken(TemplateTokenType.RIGHT_PAREN, ")")
        val callExpr = CallExpr(callee, paren, listOf(getExpr))

        val (interpreter, env) = createInterpreterWithEnv(globalEnv = globalEnv)
        val result = interpreter.visitCallExpr(callExpr, env)

        assertEquals(100, result)
    }

    @Test
    fun `interpreter should handle complex chained operations`() {
        val globalEnv = createGlobals().env
        val child = Person("Paul", 8, 0.0)
        val parent = NestedObject(child)
        val fn: () -> String = { "FunctionResult" }
        globalEnv.define("obj", parent)
        globalEnv.define("fn", fn)

        val objToken = createToken(TemplateTokenType.IDENTIFIER, "obj")
        val fnToken = createToken(TemplateTokenType.IDENTIFIER, "fn")
        val objExpr = VariableExpr(objToken)
        val childToken = createToken(TemplateTokenType.IDENTIFIER, "child")
        val getExpr = GetExpr(objExpr, childToken)
        val fnExpr = VariableExpr(fnToken)
        val paren = createToken(TemplateTokenType.RIGHT_PAREN, ")")
        val callExpr = CallExpr(fnExpr, paren, emptyList())
        val nameToken = createToken(TemplateTokenType.IDENTIFIER, "name")
        val setExpr = SetExpr(getExpr, nameToken, callExpr)

        val (interpreter, env) = createInterpreterWithEnv(globalEnv = globalEnv)
        interpreter.visitSetExpr(setExpr, env)

        assertEquals("FunctionResult", child.name)
    }

    @Test
    fun `interpret should execute expression statement and append to output`() {
        val expr = LiteralExpr("hello")
        val stmt = ExpressionStmt(expr)
        val (interpreter, _) = createInterpreterWithEnv(listOf(stmt))
        val result = interpreter.run().joinToString("") { it.xmlString }
        assertEquals("hello", result)
    }

    @Test
    fun `interpret should handle var statement`() {
        val globalEnv = createGlobals().env

        val token = createToken(TemplateTokenType.IDENTIFIER, "x")
        val initializer = LiteralExpr(42)
        val stmt = VarStmt(token, initializer)

        val (interpreter, _) = createInterpreterWithEnv(listOf(stmt), globalEnv)
        interpreter.run()

        assertEquals(42, globalEnv.get(token))
    }

    @Test
    fun `interpret should handle block statement with new scope`() {
        val globalEnv = createGlobals().env
        globalEnv.define("outer", 1)

        val outerToken = createToken(TemplateTokenType.IDENTIFIER, "outer")
        val innerToken = createToken(TemplateTokenType.IDENTIFIER, "inner")
        val innerVar = VarStmt(innerToken, LiteralExpr(2))
        val block = BlockStmt(listOf(innerVar))

        val (interpreter, _) = createInterpreterWithEnv(listOf(block), globalEnv)
        interpreter.run()

        assertEquals(1, globalEnv.get(outerToken))
        assertFailsWith<TemplaterException.RuntimeError> {
            globalEnv.get(innerToken)
        }
    }

    @Test
    fun `interpret should handle if statement with truthy condition`() {
        val condition = LiteralExpr(true)
        val body = BlockStmt(listOf(ExpressionStmt(LiteralExpr("yes"))))
        val ifStmt = IfStmt(condition, body)
        val (interpreter, _) = createInterpreterWithEnv(listOf(ifStmt))
        val result = interpreter.run().joinToString("") { it.xmlString }
        assertEquals("yes", result)
    }

    @Test
    fun `interpret should skip if statement with falsy condition`() {
        val condition = LiteralExpr(false)
        val body = BlockStmt(listOf(ExpressionStmt(LiteralExpr("no"))))
        val ifStmt = IfStmt(condition, body)
        val (interpreter, _) = createInterpreterWithEnv(listOf(ifStmt))
        val result = interpreter.run().joinToString("") { it.xmlString }
        assertEquals("", result)
    }

    @Test
    fun `interpret should handle identity rewrite statement`() {
        val rewriteToken = createToken(TemplateTokenType.REWRITE, "rewrite")
        val expression = CallExpr(
            VariableExpr(createToken(TemplateTokenType.IDENTIFIER, $$"$identityRewriter")),
            createToken(TemplateTokenType.RIGHT_PAREN, ")"),
            listOf(),
        )
        val body = BlockStmt(listOf(ExpressionStmt(LiteralExpr("output"))))
        val rewriteStmt = RewriteStmt(rewriteToken, expression, body)

        val (interpreter, _) = createInterpreterWithEnv(listOf(rewriteStmt))
        val result = interpreter.run().joinToString("") { it.xmlString }

        assertEquals("output", result)
    }

    @Test
    fun `interpret should append verbatim text`() {
        val stmt = VerbatimStmt(createRawToken("text"))
        val (interpreter, _) = createInterpreterWithEnv(listOf(stmt))
        val result = interpreter.run().joinToString("") { it.xmlString }
        assertEquals("text", result)
    }

    @Test
    fun `interpret should stringify null as empty string`() {
        val expr = LiteralExpr(null)
        val stmt = ExpressionStmt(expr)
        val (interpreter, _) = createInterpreterWithEnv(listOf(stmt))
        val result = interpreter.run().joinToString("") { it.xmlString }
        assertEquals("", result)
    }

    @Test
    fun `interpret should wrap RuntimeError in RuntimeException`() {
        val left = LiteralExpr(10)
        val right = LiteralExpr(0)
        val operator = createToken(TemplateTokenType.SLASH, "/")
        val expr = BinaryExpr(left, operator, right)
        val stmt = ExpressionStmt(expr)
        val (interpreter, _) = createInterpreterWithEnv(listOf(stmt))
        assertFailsWith<RuntimeException> {
            interpreter.run()
        }
    }

    @Test
    fun `RuntimeError should include token information`() {
        val token = createToken(TemplateTokenType.IDENTIFIER, "test")
        val error = TemplaterException.RuntimeError("Test message", token)
        assertEquals(token, error.token)
        assertEquals("test", error.token.lexeme)
    }

    @Test
    fun `RuntimeError should include descriptive message`() {
        val token = createToken(TemplateTokenType.IDENTIFIER, "test")
        val message = "This is a descriptive error message"
        val error = TemplaterException.RuntimeError(message, token)
        assertEquals(message, error.message)
    }

    @Test
    fun `interpreter should handle complete template with all features`() {
        val globalEnv = createGlobals().env

        val person = Person("Quinn", 40, 85000.0)
        globalEnv.define("person", person)

        val double: (Int) -> Int = { it * 2 }
        globalEnv.define("double", double)

        val personToken = createToken(TemplateTokenType.IDENTIFIER, "person")
        val doubleToken = createToken(TemplateTokenType.IDENTIFIER, "double")
        val ageToken = createToken(TemplateTokenType.IDENTIFIER, "age")

        val personVar = VariableExpr(personToken)
        val getAge = GetExpr(personVar, ageToken)
        val doubleVar = VariableExpr(doubleToken)
        val paren = createToken(TemplateTokenType.RIGHT_PAREN, ")")
        val callDouble = CallExpr(doubleVar, paren, listOf(getAge))

        val ten = LiteralExpr(10)
        val plusOp = createToken(TemplateTokenType.PLUS, "+")
        val stmt = ExpressionStmt(BinaryExpr(callDouble, plusOp, ten))

        val (interpreter, _) = createInterpreterWithEnv(listOf(stmt), globalEnv)
        val output = interpreter.run().joinToString("") { it.xmlString }

        assertEquals("90", output)
    }

    @Test
    fun `interpreter should maintain environment state across statements`() {
        val xToken = createToken(TemplateTokenType.IDENTIFIER, "x")
        val defineX = VarStmt(xToken, LiteralExpr(42))
        val useX = ExpressionStmt(VariableExpr(xToken))

        val (interpreter, _) = createInterpreterWithEnv(listOf(defineX, useX))
        val output = interpreter.run().joinToString("") { it.xmlString }

        assertEquals("42", output)
    }

    @Test
    fun `interpreter should handle globalEnv correctly`() {
        val nowToken = createToken(TemplateTokenType.IDENTIFIER, $$"$now")
        val nowVar = VariableExpr(nowToken)
        val paren = createToken(TemplateTokenType.RIGHT_PAREN, ")")
        val callNow = CallExpr(nowVar, paren, emptyList())

        val (interpreter, env) = createInterpreterWithEnv()
        val result = interpreter.visitCallExpr(callNow, env)

        assertNotNull(result)
        assertTrue(result is LocalDateTime)
    }

    @Test
    fun `interpreter should handle scoped environments`() {
        val xToken = createToken(TemplateTokenType.IDENTIFIER, "x")
        val defineX = VarStmt(xToken, LiteralExpr(1))
        val innerX = VarStmt(xToken, LiteralExpr(2))
        val innerBlock = BlockStmt(listOf(innerX))
        val useX = ExpressionStmt(VariableExpr(xToken))

        val (interpreter, _) = createInterpreterWithEnv(listOf(defineX, innerBlock, useX))
        val output = interpreter.run().joinToString("") { it.xmlString }

        assertEquals("1", output)
    }

    @Test
    fun `interpret should escape XML special characters in string literals`() {
        val expr = LiteralExpr("Hello & goodbye <world> \"test\" 'test'")
        val stmt = ExpressionStmt(expr)

        val (interpreter, _) = createInterpreterWithEnv(listOf(stmt))
        val result = interpreter.run().joinToString("") { it.xmlString }

        assertEquals("Hello &amp; goodbye &lt;world&gt; &quot;test&quot; &apos;test&apos;", result)
    }

    @Test
    fun `interpret should escape XML special characters in variable values`() {
        val globalEnv = createGlobals().env
        globalEnv.define("greeting", "A & B < C > D \"quoted\" 'apostrophe'")

        val token = createToken(TemplateTokenType.IDENTIFIER, "greeting")
        val expr = VariableExpr(token)
        val stmt = ExpressionStmt(expr)

        val (interpreter, _) = createInterpreterWithEnv(listOf(stmt), globalEnv)
        val result = interpreter.run().joinToString("") { it.xmlString }

        assertEquals("A &amp; B &lt; C &gt; D &quot;quoted&quot; &apos;apostrophe&apos;", result)
    }

    @Test
    fun `interpret should evaluate do statement without output`() {
        val xToken = createToken(TemplateTokenType.IDENTIFIER, "x")
        val declareX = VarStmt(xToken, LiteralExpr(0))
        val expr = AssignExpr(xToken, LiteralExpr(5))
        val stmt = DoStmt(expr)

        val (interpreter, _) = createInterpreterWithEnv(listOf(declareX, stmt))
        val result = interpreter.run().joinToString("") { it.xmlString }

        assertEquals("", result)
    }

    @Test
    fun `interpret should handle multiple do statements with side effects`() {
        val xToken = createToken(TemplateTokenType.IDENTIFIER, "x")
        val yToken = createToken(TemplateTokenType.IDENTIFIER, "y")
        val plusToken = createToken(TemplateTokenType.PLUS, "+")

        val declareX = VarStmt(xToken, LiteralExpr(0))
        val declareY = VarStmt(yToken, LiteralExpr(0))
        val assignX = DoStmt(AssignExpr(xToken, LiteralExpr(5)))
        val assignY = DoStmt(AssignExpr(yToken, LiteralExpr(3)))
        val useSum =
            ExpressionStmt(BinaryExpr(VariableExpr(xToken), plusToken, VariableExpr(yToken)))

        val (interpreter, _) = createInterpreterWithEnv(
            listOf(
                declareX,
                declareY,
                assignX,
                assignY,
                useSum,
            ),
        )
        val result = interpreter.run().joinToString("") { it.xmlString }

        assertEquals("8", result)
    }

    @Test
    fun `visitOptionalGetExpr should return null for null object`() {
        val objToken = createToken(TemplateTokenType.IDENTIFIER, "obj")
        val nameToken = createToken(TemplateTokenType.IDENTIFIER, "name")

        val globalEnv = createGlobals().env
        globalEnv.define("obj", null)

        val expr = OptionalGetExpr(VariableExpr(objToken), nameToken)
        val (interpreter, env) = createInterpreterWithEnv(globalEnv = globalEnv)
        val result = interpreter.visitOptionalGetExpr(expr, env)

        assertEquals(null, result)
    }

    @Test
    fun `visitOptionalGetExpr should return property for non-null object`() {
        val objToken = createToken(TemplateTokenType.IDENTIFIER, "obj")
        val nameToken = createToken(TemplateTokenType.IDENTIFIER, "name")

        val globalEnv = createGlobals().env
        globalEnv.define("obj", Person("John", 30, 50000.0))

        val expr = OptionalGetExpr(VariableExpr(objToken), nameToken)
        val (interpreter, env) = createInterpreterWithEnv(globalEnv = globalEnv)
        val result = interpreter.visitOptionalGetExpr(expr, env)

        assertEquals("John", result)
    }

    @Test
    fun `visitOptionalGetExpr should chain correctly`() {
        val objToken = createToken(TemplateTokenType.IDENTIFIER, "obj")
        val childToken = createToken(TemplateTokenType.IDENTIFIER, "child")
        val nameToken = createToken(TemplateTokenType.IDENTIFIER, "name")

        val globalEnv = createGlobals().env
        globalEnv.define("obj", NestedObject(Person("Jane", 25, 60000.0)))

        val expr = OptionalGetExpr(
            OptionalGetExpr(VariableExpr(objToken), childToken),
            nameToken,
        )
        val (interpreter, env) = createInterpreterWithEnv(globalEnv = globalEnv)
        val result = interpreter.visitOptionalGetExpr(expr, env)

        assertEquals("Jane", result)
    }

    @Test
    fun `visitOptionalGetExpr should return null when chaining with null`() {
        val objToken = createToken(TemplateTokenType.IDENTIFIER, "obj")
        val childToken = createToken(TemplateTokenType.IDENTIFIER, "child")
        val nameToken = createToken(TemplateTokenType.IDENTIFIER, "name")

        val globalEnv = createGlobals().env
        globalEnv.define("obj", null)

        val expr = OptionalGetExpr(
            OptionalGetExpr(VariableExpr(objToken), childToken),
            nameToken,
        )
        val (interpreter, env) = createInterpreterWithEnv(globalEnv = globalEnv)
        val result = interpreter.visitOptionalGetExpr(expr, env)

        assertEquals(null, result)
    }

    @Test
    fun `visitLogicalExpr should return left value for non-null left with null coalescing`() {
        val aToken = createToken(TemplateTokenType.IDENTIFIER, "a")
        val bToken = createToken(TemplateTokenType.IDENTIFIER, "b")
        val opToken = createToken(TemplateTokenType.QUESTION_QUESTION, "??")

        val globalEnv = createGlobals().env
        globalEnv.define("a", 5)
        globalEnv.define("b", 10)

        val expr = LogicalExpr(VariableExpr(aToken), opToken, VariableExpr(bToken))
        val (interpreter, env) = createInterpreterWithEnv(globalEnv = globalEnv)
        val result = interpreter.visitLogicalExpr(expr, env)

        assertEquals(5, result)
    }

    @Test
    fun `visitLogicalExpr should return right value for null left with null coalescing`() {
        val aToken = createToken(TemplateTokenType.IDENTIFIER, "a")
        val bToken = createToken(TemplateTokenType.IDENTIFIER, "b")
        val opToken = createToken(TemplateTokenType.QUESTION_QUESTION, "??")

        val globalEnv = createGlobals().env
        globalEnv.define("a", null)
        globalEnv.define("b", 10)

        val expr = LogicalExpr(VariableExpr(aToken), opToken, VariableExpr(bToken))
        val (interpreter, env) = createInterpreterWithEnv(globalEnv = globalEnv)
        val result = interpreter.visitLogicalExpr(expr, env)

        assertEquals(10, result)
    }

    @Test
    fun `visitLogicalExpr should handle chained null coalescing`() {
        val aToken = createToken(TemplateTokenType.IDENTIFIER, "a")
        val bToken = createToken(TemplateTokenType.IDENTIFIER, "b")
        val cToken = createToken(TemplateTokenType.IDENTIFIER, "c")
        val op1Token = createToken(TemplateTokenType.QUESTION_QUESTION, "??")
        val op2Token = createToken(TemplateTokenType.QUESTION_QUESTION, "??")

        val globalEnv = createGlobals().env
        globalEnv.define("a", null)
        globalEnv.define("b", null)
        globalEnv.define("c", 3)

        val expr = LogicalExpr(
            VariableExpr(aToken),
            op1Token,
            LogicalExpr(VariableExpr(bToken), op2Token, VariableExpr(cToken)),
        )
        val (interpreter, env) = createInterpreterWithEnv(globalEnv = globalEnv)
        val result = interpreter.visitLogicalExpr(expr, env)

        assertEquals(3, result)
    }

    @Test
    fun `visitLogicalExpr should return zero for zero left with null coalescing`() {
        val aToken = createToken(TemplateTokenType.IDENTIFIER, "a")
        val bToken = createToken(TemplateTokenType.IDENTIFIER, "b")
        val opToken = createToken(TemplateTokenType.QUESTION_QUESTION, "??")

        val globalEnv = createGlobals().env
        globalEnv.define("a", 0)
        globalEnv.define("b", 10)

        val expr = LogicalExpr(VariableExpr(aToken), opToken, VariableExpr(bToken))
        val (interpreter, env) = createInterpreterWithEnv(globalEnv = globalEnv)
        val result = interpreter.visitLogicalExpr(expr, env)

        assertEquals(0, result)
    }

    @Test
    fun `visitLogicalExpr should return false for false left with null coalescing`() {
        val aToken = createToken(TemplateTokenType.IDENTIFIER, "a")
        val bToken = createToken(TemplateTokenType.IDENTIFIER, "b")
        val opToken = createToken(TemplateTokenType.QUESTION_QUESTION, "??")

        val globalEnv = createGlobals().env
        globalEnv.define("a", false)
        globalEnv.define("b", true)

        val expr = LogicalExpr(VariableExpr(aToken), opToken, VariableExpr(bToken))
        val (interpreter, env) = createInterpreterWithEnv(globalEnv = globalEnv)
        val result = interpreter.visitLogicalExpr(expr, env)

        assertEquals(false, result)
    }

    @Test
    fun `visitLambdaExpr should create TemplateFunction with closure`() {
        val xToken = createToken(TemplateTokenType.IDENTIFIER, "x")
        val lambda = LambdaExpr(listOf(xToken), LiteralExpr(42))

        val (interpreter, env) = createInterpreterWithEnv()
        val result = interpreter.visitLambdaExpr(lambda, env)

        assertTrue(result is TemplateFunction)
        assertEquals(1, result.arity)
        assertEquals("x", result.params[0].lexeme)
    }

    @Test
    fun `lambda should be callable`() {
        val xToken = createToken(TemplateTokenType.IDENTIFIER, "x")
        val yToken = createToken(TemplateTokenType.IDENTIFIER, "y")
        val plusToken = createToken(TemplateTokenType.PLUS, "+")

        val lambda = LambdaExpr(
            listOf(xToken, yToken),
            BinaryExpr(
                VariableExpr(xToken),
                plusToken,
                VariableExpr(yToken),
            ),
        )

        val callExpr = CallExpr(
            lambda,
            createToken(TemplateTokenType.RIGHT_PAREN, ")"),
            listOf(LiteralExpr(3), LiteralExpr(4)),
        )

        val program = listOf(ExpressionStmt(callExpr))

        val renderer = Renderer(createGlobals().env)
        val result = renderer.render(program, emptyMap())

        assertTokensEqual(XmlOutputToken.fromString("7"), result)
    }

    @Test
    fun `lambda should capture closure variables`() {
        val baseToken = createToken(TemplateTokenType.IDENTIFIER, "base")
        val xToken = createToken(TemplateTokenType.IDENTIFIER, "x")
        val plusToken = createToken(TemplateTokenType.PLUS, "+")

        val baseVarStmt = VarStmt(baseToken, LiteralExpr(10))

        val lambda = LambdaExpr(
            listOf(xToken),
            BinaryExpr(
                VariableExpr(xToken),
                plusToken,
                VariableExpr(baseToken),
            ),
        )

        val callExpr = CallExpr(
            lambda,
            createToken(TemplateTokenType.RIGHT_PAREN, ")"),
            listOf(LiteralExpr(5)),
        )

        val program = listOf(baseVarStmt, ExpressionStmt(callExpr))

        val renderer = Renderer(createGlobals().env)
        val result = renderer.render(program, emptyMap())

        assertTokensEqual(XmlOutputToken.fromString("15"), result)
    }

    @Test
    fun `lambda should be callable multiple times`() {
        val xToken = createToken(TemplateTokenType.IDENTIFIER, "x")
        val fnToken = createToken(TemplateTokenType.IDENTIFIER, "fn")
        val starToken = createToken(TemplateTokenType.STAR, "*")

        val lambda = LambdaExpr(
            listOf(xToken),
            BinaryExpr(
                VariableExpr(xToken),
                starToken,
                LiteralExpr(2),
            ),
        )
        val varStmt = VarStmt(fnToken, lambda)

        val call1 = CallExpr(
            VariableExpr(fnToken),
            createToken(TemplateTokenType.RIGHT_PAREN, ")"),
            listOf(LiteralExpr(5)),
        )

        val call2 = CallExpr(
            VariableExpr(fnToken),
            createToken(TemplateTokenType.RIGHT_PAREN, ")"),
            listOf(LiteralExpr(10)),
        )

        val program = listOf(varStmt, ExpressionStmt(call1), ExpressionStmt(call2))

        val renderer = Renderer(createGlobals().env)
        val result = renderer.render(program, emptyMap())

        assertTokensEqual(XmlOutputToken.fromString("10") + XmlOutputToken.fromString("20"), result)
    }

    @Test
    fun `lambda call should throw on arity mismatch`() {
        val xToken = createToken(TemplateTokenType.IDENTIFIER, "x")

        val lambda = LambdaExpr(listOf(xToken), VariableExpr(xToken))

        val callExpr = CallExpr(
            lambda,
            createToken(TemplateTokenType.RIGHT_PAREN, ")"),
            listOf(LiteralExpr(1), LiteralExpr(2)),
        )

        val (interpreter, env) = createInterpreterWithEnv()
        assertFailsWith<TemplaterException.RuntimeError> {
            interpreter.visitCallExpr(callExpr, env)
        }
    }

    @Test
    fun `lambda should be assignable to variable`() {
        val xToken = createToken(TemplateTokenType.IDENTIFIER, "x")
        val fnToken = createToken(TemplateTokenType.IDENTIFIER, "fn")
        val starToken = createToken(TemplateTokenType.STAR, "*")

        val lambda = LambdaExpr(
            listOf(xToken),
            BinaryExpr(
                VariableExpr(xToken),
                starToken,
                LiteralExpr(2),
            ),
        )
        val varStmt = VarStmt(fnToken, lambda)

        val callExpr = CallExpr(
            VariableExpr(fnToken),
            createToken(TemplateTokenType.RIGHT_PAREN, ")"),
            listOf(LiteralExpr(5)),
        )

        val program = listOf(varStmt, ExpressionStmt(callExpr))

        val renderer = Renderer(createGlobals().env)
        val result = renderer.render(program, emptyMap())

        assertTokensEqual(XmlOutputToken.fromString("10"), result)
    }

    @Test
    fun `lambda should be passable as argument to another lambda`() {
        val fnToken = createToken(TemplateTokenType.IDENTIFIER, "fn")
        val xToken = createToken(TemplateTokenType.IDENTIFIER, "x")
        val starToken = createToken(TemplateTokenType.STAR, "*")

        val applyLambda = LambdaExpr(
            listOf(fnToken, xToken),
            CallExpr(
                VariableExpr(fnToken),
                createToken(TemplateTokenType.RIGHT_PAREN, ")"),
                listOf(VariableExpr(xToken)),
            ),
        )

        val doubleLambda = LambdaExpr(
            listOf(xToken),
            BinaryExpr(
                VariableExpr(xToken),
                starToken,
                LiteralExpr(2),
            ),
        )

        val callExpr = CallExpr(
            applyLambda,
            createToken(TemplateTokenType.RIGHT_PAREN, ")"),
            listOf(doubleLambda, LiteralExpr(5)),
        )

        val program = listOf(ExpressionStmt(callExpr))

        val renderer = Renderer(createGlobals().env)
        val result = renderer.render(program, emptyMap())

        assertTokensEqual(XmlOutputToken.fromString("10"), result)
    }

    @Test
    fun `zero-parameter lambda should work`() {
        val lambda = LambdaExpr(emptyList(), LiteralExpr(42))

        val callExpr = CallExpr(
            lambda,
            createToken(TemplateTokenType.RIGHT_PAREN, ")"),
            emptyList(),
        )

        val (interpreter, env) = createInterpreterWithEnv()
        val result = interpreter.visitCallExpr(callExpr, env)
        assertEquals(42, result)
    }

    @Test
    fun `pipe operator should pass value to lambda`() {
        val lambda = LambdaExpr(
            listOf(createToken(TemplateTokenType.IDENTIFIER, "x")),
            BinaryExpr(
                VariableExpr(createToken(TemplateTokenType.IDENTIFIER, "x")),
                createToken(TemplateTokenType.STAR, "*"),
                LiteralExpr(2),
            ),
        )

        val pipeExpr = PipeExpr(
            LiteralExpr(5),
            createToken(TemplateTokenType.PIPE, "|"),
            lambda,
        )

        val program = listOf(ExpressionStmt(pipeExpr))
        val renderer = Renderer(createGlobals().env)
        val result = renderer.render(program, emptyMap())

        assertTokensEqual(XmlOutputToken.fromString("10"), result)
    }

    @Test
    fun `pipe operator should work with native function`() {
        val pipeExpr = PipeExpr(
            LiteralExpr(42),
            createToken(TemplateTokenType.PIPE, "|"),
            VariableExpr(createToken(TemplateTokenType.IDENTIFIER, $$"$notNull")),
        )

        val program = listOf(ExpressionStmt(pipeExpr))
        val renderer = Renderer(createGlobals().env)
        val result = renderer.render(program, emptyMap())

        assertTokensEqual(XmlOutputToken.fromString("true"), result)
    }

    @Test
    fun `pipe operator should chain operations`() {
        val lambda1 = LambdaExpr(
            listOf(createToken(TemplateTokenType.IDENTIFIER, "x")),
            BinaryExpr(
                VariableExpr(createToken(TemplateTokenType.IDENTIFIER, "x")),
                createToken(TemplateTokenType.PLUS, "+"),
                LiteralExpr(1),
            ),
        )

        val lambda2 = LambdaExpr(
            listOf(createToken(TemplateTokenType.IDENTIFIER, "x")),
            BinaryExpr(
                VariableExpr(createToken(TemplateTokenType.IDENTIFIER, "x")),
                createToken(TemplateTokenType.STAR, "*"),
                LiteralExpr(2),
            ),
        )

        val pipeExpr = PipeExpr(
            PipeExpr(
                LiteralExpr(5),
                createToken(TemplateTokenType.PIPE, "|"),
                lambda1,
            ),
            createToken(TemplateTokenType.PIPE, "|"),
            lambda2,
        )

        val program = listOf(ExpressionStmt(pipeExpr))
        val renderer = Renderer(createGlobals().env)
        val result = renderer.render(program, emptyMap())

        assertTokensEqual(XmlOutputToken.fromString("12"), result)
    }

    @Test
    fun `filter should work with notNull predicate`() {
        val items = listOf(1, null, 2, null, 3)

        val filterCall = CallExpr(
            VariableExpr(createToken(TemplateTokenType.IDENTIFIER, $$"$filter")),
            createToken(TemplateTokenType.RIGHT_PAREN, ")"),
            listOf(
                LiteralExpr(items),
                VariableExpr(createToken(TemplateTokenType.IDENTIFIER, $$"$notNull")),
            ),
        )

        val program = listOf(ExpressionStmt(filterCall))
        val renderer = Renderer(createGlobals().env)
        val result = renderer.render(program, emptyMap())

        assertTokensEqual(XmlOutputToken.fromString("[1, 2, 3]"), result)
    }

    @Test
    fun `filter should work with lambda predicate`() {
        val items = listOf(1, 2, 3, 4, 5)

        val predicate = LambdaExpr(
            listOf(createToken(TemplateTokenType.IDENTIFIER, "x")),
            BinaryExpr(
                VariableExpr(createToken(TemplateTokenType.IDENTIFIER, "x")),
                createToken(TemplateTokenType.GREATER, ">"),
                LiteralExpr(2),
            ),
        )

        val filterCall = CallExpr(
            VariableExpr(createToken(TemplateTokenType.IDENTIFIER, $$"$filter")),
            createToken(TemplateTokenType.RIGHT_PAREN, ")"),
            listOf(LiteralExpr(items), predicate),
        )

        val program = listOf(ExpressionStmt(filterCall))
        val renderer = Renderer(createGlobals().env)
        val result = renderer.render(program, emptyMap())

        assertTokensEqual(XmlOutputToken.fromString("[3, 4, 5]"), result)
    }

    @Test
    fun `map should work with lambda mapper`() {
        val items = listOf(1, 2, 3)

        val mapper = LambdaExpr(
            listOf(createToken(TemplateTokenType.IDENTIFIER, "x")),
            BinaryExpr(
                VariableExpr(createToken(TemplateTokenType.IDENTIFIER, "x")),
                createToken(TemplateTokenType.STAR, "*"),
                LiteralExpr(2),
            ),
        )

        val mapCall = CallExpr(
            VariableExpr(createToken(TemplateTokenType.IDENTIFIER, $$"$map")),
            createToken(TemplateTokenType.RIGHT_PAREN, ")"),
            listOf(LiteralExpr(items), mapper),
        )

        val program = listOf(ExpressionStmt(mapCall))
        val renderer = Renderer(createGlobals().env)
        val result = renderer.render(program, emptyMap())

        assertTokensEqual(XmlOutputToken.fromString("[2, 4, 6]"), result)
    }

    @Test
    fun `reduce should work with lambda reducer and initial value`() {
        val items = listOf(1, 2, 3, 4)

        val reducer = LambdaExpr(
            listOf(
                createToken(TemplateTokenType.IDENTIFIER, "acc"),
                createToken(TemplateTokenType.IDENTIFIER, "x"),
            ),
            BinaryExpr(
                VariableExpr(createToken(TemplateTokenType.IDENTIFIER, "acc")),
                createToken(TemplateTokenType.PLUS, "+"),
                VariableExpr(createToken(TemplateTokenType.IDENTIFIER, "x")),
            ),
        )

        val reduceCall = CallExpr(
            VariableExpr(createToken(TemplateTokenType.IDENTIFIER, $$"$reduce")),
            createToken(TemplateTokenType.RIGHT_PAREN, ")"),
            listOf(LiteralExpr(items), reducer, LiteralExpr(0)),
        )

        val program = listOf(ExpressionStmt(reduceCall))
        val renderer = Renderer(createGlobals().env)
        val result = renderer.render(program, emptyMap())

        assertTokensEqual(XmlOutputToken.fromString("10"), result)
    }

    @Test
    fun `reduce should work without initial value`() {
        val items = listOf(1, 2, 3, 4)

        val reducer = LambdaExpr(
            listOf(
                createToken(TemplateTokenType.IDENTIFIER, "acc"),
                createToken(TemplateTokenType.IDENTIFIER, "x"),
            ),
            BinaryExpr(
                VariableExpr(createToken(TemplateTokenType.IDENTIFIER, "acc")),
                createToken(TemplateTokenType.PLUS, "+"),
                VariableExpr(createToken(TemplateTokenType.IDENTIFIER, "x")),
            ),
        )

        val reduceCall = CallExpr(
            VariableExpr(createToken(TemplateTokenType.IDENTIFIER, $$"$reduce")),
            createToken(TemplateTokenType.RIGHT_PAREN, ")"),
            listOf(LiteralExpr(items), reducer),
        )

        val program = listOf(ExpressionStmt(reduceCall))
        val renderer = Renderer(createGlobals().env)
        val result = renderer.render(program, emptyMap())

        assertTokensEqual(XmlOutputToken.fromString("10"), result)
    }

    @Test
    fun `reduce should return null for empty list without initial value`() {
        val items = emptyList<Int>()

        val reducer = LambdaExpr(
            listOf(
                createToken(TemplateTokenType.IDENTIFIER, "acc"),
                createToken(TemplateTokenType.IDENTIFIER, "x"),
            ),
            BinaryExpr(
                VariableExpr(createToken(TemplateTokenType.IDENTIFIER, "acc")),
                createToken(TemplateTokenType.PLUS, "+"),
                VariableExpr(createToken(TemplateTokenType.IDENTIFIER, "x")),
            ),
        )

        val reduceCall = CallExpr(
            VariableExpr(createToken(TemplateTokenType.IDENTIFIER, $$"$reduce")),
            createToken(TemplateTokenType.RIGHT_PAREN, ")"),
            listOf(LiteralExpr(items), reducer),
        )

        val program = listOf(ExpressionStmt(reduceCall))
        val renderer = Renderer(createGlobals().env)
        val result = renderer.render(program, emptyMap())

        assertTokensEqual(XmlOutputToken.fromString(""), result)
    }

    @Test
    fun `chained pipe with filter map and reduce`() {
        val items = listOf(1, 2, 3, 4, 5)

        val filterPredicate = LambdaExpr(
            listOf(createToken(TemplateTokenType.IDENTIFIER, "x")),
            BinaryExpr(
                VariableExpr(createToken(TemplateTokenType.IDENTIFIER, "x")),
                createToken(TemplateTokenType.GREATER, ">"),
                LiteralExpr(2),
            ),
        )
        val filterCall = CallExpr(
            VariableExpr(createToken(TemplateTokenType.IDENTIFIER, $$"$filter")),
            createToken(TemplateTokenType.RIGHT_PAREN, ")"),
            listOf(filterPredicate),
        )

        val mapper = LambdaExpr(
            listOf(createToken(TemplateTokenType.IDENTIFIER, "x")),
            BinaryExpr(
                VariableExpr(createToken(TemplateTokenType.IDENTIFIER, "x")),
                createToken(TemplateTokenType.STAR, "*"),
                LiteralExpr(2),
            ),
        )
        val mapCall = CallExpr(
            VariableExpr(createToken(TemplateTokenType.IDENTIFIER, $$"$map")),
            createToken(TemplateTokenType.RIGHT_PAREN, ")"),
            listOf(mapper),
        )

        val filterPipe = PipeExpr(
            LiteralExpr(items),
            createToken(TemplateTokenType.PIPE, "|"),
            filterCall,
        )

        val mapPipe = PipeExpr(
            filterPipe,
            createToken(TemplateTokenType.PIPE, "|"),
            mapCall,
        )

        val program = listOf(ExpressionStmt(mapPipe))
        val renderer = Renderer(createGlobals().env)
        val result = renderer.render(program, emptyMap())

        assertTokensEqual(XmlOutputToken.fromString("[6, 8, 10]"), result)
    }

    @Test
    fun `sum should sum list of integers`() {
        val items = listOf(1, 2, 3, 4)
        val sumCall = CallExpr(
            VariableExpr(createToken(TemplateTokenType.IDENTIFIER, $$"$sum")),
            createToken(TemplateTokenType.RIGHT_PAREN, ")"),
            listOf(LiteralExpr(items)),
        )

        val program = listOf(ExpressionStmt(sumCall))
        val renderer = Renderer(createGlobals().env)
        val result = renderer.render(program, emptyMap())

        assertTokensEqual(XmlOutputToken.fromString("10.0"), result)
    }

    @Test
    fun `sum should sum list of mixed numbers`() {
        val items = listOf(1, 2.5, 3)
        val sumCall = CallExpr(
            VariableExpr(createToken(TemplateTokenType.IDENTIFIER, $$"$sum")),
            createToken(TemplateTokenType.RIGHT_PAREN, ")"),
            listOf(LiteralExpr(items)),
        )

        val program = listOf(ExpressionStmt(sumCall))
        val renderer = Renderer(createGlobals().env)
        val result = renderer.render(program, emptyMap())

        assertTokensEqual(XmlOutputToken.fromString("6.5"), result)
    }

    @Test
    fun `sum should return 0 for empty list`() {
        val items = emptyList<Int>()
        val sumCall = CallExpr(
            VariableExpr(createToken(TemplateTokenType.IDENTIFIER, $$"$sum")),
            createToken(TemplateTokenType.RIGHT_PAREN, ")"),
            listOf(LiteralExpr(items)),
        )

        val program = listOf(ExpressionStmt(sumCall))
        val renderer = Renderer(createGlobals().env)
        val result = renderer.render(program, emptyMap())

        assertTokensEqual(XmlOutputToken.fromString("0.0"), result)
    }

    @Test
    fun `sum should return 0 for null input`() {
        val sumCall = CallExpr(
            VariableExpr(createToken(TemplateTokenType.IDENTIFIER, $$"$sum")),
            createToken(TemplateTokenType.RIGHT_PAREN, ")"),
            listOf(LiteralExpr(null)),
        )

        val program = listOf(ExpressionStmt(sumCall))
        val renderer = Renderer(createGlobals().env)
        val result = renderer.render(program, emptyMap())

        assertTokensEqual(XmlOutputToken.fromString("0"), result)
    }

    @Test
    fun `sum should work with pipe`() {
        val items = listOf(1, 2, 3)
        val sumCall = CallExpr(
            VariableExpr(createToken(TemplateTokenType.IDENTIFIER, $$"$sum")),
            createToken(TemplateTokenType.RIGHT_PAREN, ")"),
            emptyList(),
        )
        val pipeExpr = PipeExpr(
            LiteralExpr(items),
            createToken(TemplateTokenType.PIPE, "|"),
            sumCall,
        )

        val program = listOf(ExpressionStmt(pipeExpr))
        val renderer = Renderer(createGlobals().env)
        val result = renderer.render(program, emptyMap())

        assertTokensEqual(XmlOutputToken.fromString("6.0"), result)
    }

    @Test
    fun `$format should format LocalDate with default pattern`() {
        val date = LocalDate.of(2020, 8, 25)
        val formatCall = CallExpr(
            VariableExpr(createToken(TemplateTokenType.IDENTIFIER, $$"$format")),
            createToken(TemplateTokenType.RIGHT_PAREN, ")"),
            listOf(LiteralExpr(date)),
        )

        val program = listOf(ExpressionStmt(formatCall))
        val renderer = Renderer(createGlobals().env)
        val result = renderer.render(program, emptyMap())

        assertTokensEqual(XmlOutputToken.fromString("2020/08/25"), result)
    }

    @Test
    fun `$format should format LocalDateTime with default pattern`() {
        val dateTime = LocalDateTime.of(2020, 8, 24, 14, 42)
        val formatCall = CallExpr(
            VariableExpr(createToken(TemplateTokenType.IDENTIFIER, $$"$format")),
            createToken(TemplateTokenType.RIGHT_PAREN, ")"),
            listOf(LiteralExpr(dateTime)),
        )

        val program = listOf(ExpressionStmt(formatCall))
        val renderer = Renderer(createGlobals().env)
        val result = renderer.render(program, emptyMap())

        assertTokensEqual(XmlOutputToken.fromString("2020/08/24 14:42"), result)
    }

    @Test
    fun `$format should format ZonedDateTime with default pattern`() {
        val zonedDateTime = ZonedDateTime.of(2020, 8, 24, 14, 42, 0, 0, ZoneId.of("UTC"))
        val formatCall = CallExpr(
            VariableExpr(createToken(TemplateTokenType.IDENTIFIER, $$"$format")),
            createToken(TemplateTokenType.RIGHT_PAREN, ")"),
            listOf(LiteralExpr(zonedDateTime)),
        )

        val program = listOf(ExpressionStmt(formatCall))
        val renderer = Renderer(createGlobals().env)
        val result = renderer.render(program, emptyMap())

        assertTokensEqual(XmlOutputToken.fromString("2020/08/24 14:42"), result)
    }

    @Test
    fun `$format should format java Date with default pattern`() {
        val dateTime = LocalDateTime.of(2020, 8, 24, 14, 42)
        val date = Date.from(dateTime.atZone(ZoneId.systemDefault()).toInstant())
        val formatCall = CallExpr(
            VariableExpr(createToken(TemplateTokenType.IDENTIFIER, $$"$format")),
            createToken(TemplateTokenType.RIGHT_PAREN, ")"),
            listOf(LiteralExpr(date)),
        )

        val program = listOf(ExpressionStmt(formatCall))
        val renderer = Renderer(createGlobals().env)
        val result = renderer.render(program, emptyMap())

        assertTokensEqual(XmlOutputToken.fromString("2020/08/24 14:42"), result)
    }

    @Test
    fun `$format should format integer with thousand separators`() {
        val formatCall = CallExpr(
            VariableExpr(createToken(TemplateTokenType.IDENTIFIER, $$"$format")),
            createToken(TemplateTokenType.RIGHT_PAREN, ")"),
            listOf(LiteralExpr(1234567)),
        )

        val program = listOf(ExpressionStmt(formatCall))
        val renderer = Renderer(createGlobals().env)
        val result = renderer.render(program, emptyMap())

        assertTokensEqual(XmlOutputToken.fromString("1,234,567"), result)
    }

    @Test
    fun `$format should format Long with thousand separators`() {
        val formatCall = CallExpr(
            VariableExpr(createToken(TemplateTokenType.IDENTIFIER, $$"$format")),
            createToken(TemplateTokenType.RIGHT_PAREN, ")"),
            listOf(LiteralExpr(1234567890123L)),
        )

        val program = listOf(ExpressionStmt(formatCall))
        val renderer = Renderer(createGlobals().env)
        val result = renderer.render(program, emptyMap())

        assertTokensEqual(XmlOutputToken.fromString("1,234,567,890,123"), result)
    }

    @Test
    fun `$format should format Double with thousand separators and two decimals`() {
        val formatCall = CallExpr(
            VariableExpr(createToken(TemplateTokenType.IDENTIFIER, $$"$format")),
            createToken(TemplateTokenType.RIGHT_PAREN, ")"),
            listOf(LiteralExpr(1234567.891)),
        )

        val program = listOf(ExpressionStmt(formatCall))
        val renderer = Renderer(createGlobals().env)
        val result = renderer.render(program, emptyMap())

        assertTokensEqual(XmlOutputToken.fromString("1,234,567.89"), result)
    }

    @Test
    fun `$format should format Float with thousand separators and two decimals`() {
        val formatCall = CallExpr(
            VariableExpr(createToken(TemplateTokenType.IDENTIFIER, $$"$format")),
            createToken(TemplateTokenType.RIGHT_PAREN, ")"),
            listOf(LiteralExpr(1234.56f)),
        )

        val program = listOf(ExpressionStmt(formatCall))
        val renderer = Renderer(createGlobals().env)
        val result = renderer.render(program, emptyMap())

        assertTokensEqual(XmlOutputToken.fromString("1,234.56"), result)
    }

    @Test
    fun `$format should accept custom pattern for dates`() {
        val date = LocalDate.of(2020, 8, 25)
        val formatCall = CallExpr(
            VariableExpr(createToken(TemplateTokenType.IDENTIFIER, $$"$format")),
            createToken(TemplateTokenType.RIGHT_PAREN, ")"),
            listOf(LiteralExpr(date), LiteralExpr("dd/MM/yyyy")),
        )

        val program = listOf(ExpressionStmt(formatCall))
        val renderer = Renderer(createGlobals().env)
        val result = renderer.render(program, emptyMap())

        assertTokensEqual(XmlOutputToken.fromString("25/08/2020"), result)
    }

    @Test
    fun `$format should accept custom pattern for numbers`() {
        val formatCall = CallExpr(
            VariableExpr(createToken(TemplateTokenType.IDENTIFIER, $$"$format")),
            createToken(TemplateTokenType.RIGHT_PAREN, ")"),
            listOf(LiteralExpr(1234567.89), LiteralExpr("0.000")),
        )

        val program = listOf(ExpressionStmt(formatCall))
        val renderer = Renderer(createGlobals().env)
        val result = renderer.render(program, emptyMap())

        assertTokensEqual(XmlOutputToken.fromString("1234567.890"), result)
    }

    @Test
    fun `$format should return null for null input`() {
        val formatCall = CallExpr(
            VariableExpr(createToken(TemplateTokenType.IDENTIFIER, $$"$format")),
            createToken(TemplateTokenType.RIGHT_PAREN, ")"),
            listOf(LiteralExpr(null)),
        )

        val program = listOf(ExpressionStmt(formatCall))
        val renderer = Renderer(createGlobals().env)
        val result = renderer.render(program, emptyMap())

        assertTokensEqual(XmlOutputToken.fromString(""), result)
    }

    @Test
    fun `$format should throw error for unsupported type`() {
        val formatCall = CallExpr(
            VariableExpr(createToken(TemplateTokenType.IDENTIFIER, $$"$format")),
            createToken(TemplateTokenType.RIGHT_PAREN, ")"),
            listOf(LiteralExpr("not a date or number")),
        )

        val program = listOf(ExpressionStmt(formatCall))
        val renderer = Renderer(createGlobals().env)

        val exception = assertFailsWith<TemplaterException.RuntimeError> {
            renderer.render(program, emptyMap())
        }
        assertTrue(exception.message!!.contains($$"$format: unsupported type"))
    }
}
