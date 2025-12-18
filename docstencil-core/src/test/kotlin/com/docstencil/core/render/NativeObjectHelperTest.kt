package com.docstencil.core.render

import com.docstencil.core.error.TemplaterException
import com.docstencil.core.scanner.model.ContentIdx
import com.docstencil.core.scanner.model.TemplateToken
import com.docstencil.core.scanner.model.TemplateTokenType
import kotlin.test.*

class NativeObjectHelperTest {
    private val helper = NativeObjectHelper()

    data class Person(var name: String, val age: Int, var salary: Double)

    private fun createToken(lexeme: String): TemplateToken {
        return TemplateToken(
            TemplateTokenType.IDENTIFIER,
            lexeme,
            null,
            null,
            ContentIdx(0),
        )
    }

    @Test
    fun `isCallable should return true for Kotlin lambda`() {
        val lambda: () -> String = { "hello" }
        assertTrue(helper.isCallable(lambda))
    }

    @Test
    fun `isCallable should return true for Java Supplier`() {
        val supplier = java.util.function.Supplier { "hello" }
        assertTrue(helper.isCallable(supplier))
    }

    @Test
    fun `isCallable should return false for regular object`() {
        val person = Person("John", 30, 50000.0)
        assertFalse(helper.isCallable(person))
    }

    @Test
    fun `isCallable should return false for null`() {
        assertFalse(helper.isCallable(null))
    }

    @Test
    fun `getArity should return 0 for no-arg lambda`() {
        val lambda: () -> String = { "hello" }
        assertEquals(0, helper.getArity(lambda))
    }

    @Test
    fun `getArity should return 2 for two-arg lambda`() {
        val lambda: (Int, Int) -> Int = { a, b -> a + b }
        assertEquals(2, helper.getArity(lambda))
    }

    @Test
    fun `getArity should return 1 for Java Function`() {
        val func = java.util.function.Function<String, Int> { it.length }
        assertEquals(1, helper.getArity(func))
    }

    @Test
    fun `call should invoke Kotlin lambda with correct args`() {
        val lambda: (Int, Int) -> Int = { a, b -> a + b }
        val result = helper.call(lambda, listOf(5, 3))
        assertEquals(8, result)
    }

    @Test
    fun `call should invoke Java Supplier`() {
        val supplier = java.util.function.Supplier { "result" }
        val result = helper.call(supplier, emptyList())
        assertEquals("result", result)
    }

    @Test
    fun `call should invoke Java BiFunction`() {
        val biFunc = java.util.function.BiFunction<Int, Int, String> { a, b -> "${a + b}" }
        val result = helper.call(biFunc, listOf(10, 20))
        assertEquals("30", result)
    }

    @Test
    fun `getProperty should read property from data class`() {
        val person = Person("Alice", 25, 60000.0)
        val nameToken = createToken("name")

        val result = helper.getProperty(person, nameToken)
        assertEquals("Alice", result)
    }

    @Test
    fun `getProperty should read val property`() {
        val person = Person("Bob", 30, 55000.0)
        val ageToken = createToken("age")

        val result = helper.getProperty(person, ageToken)
        assertEquals(30, result)
    }

    @Test
    fun `getProperty should throw for non-existent property`() {
        val person = Person("Charlie", 35, 70000.0)
        val token = createToken("nonExistent")

        assertFailsWith<TemplaterException.RuntimeError> {
            helper.getProperty(person, token)
        }
    }

    @Test
    fun `setProperty should update mutable property`() {
        val person = Person("David", 40, 80000.0)
        val nameToken = createToken("name")

        helper.setProperty(person, nameToken, "NewName")
        assertEquals("NewName", person.name)
    }

    @Test
    fun `setProperty should apply numeric type conversion`() {
        val person = Person("Eve", 28, 65000.0)
        val salaryToken = createToken("salary")

        helper.setProperty(person, salaryToken, 75000)
        assertEquals(75000.0, person.salary)
    }

    @Test
    fun `setProperty should throw for read-only property`() {
        val person = Person("Frank", 45, 90000.0)
        val ageToken = createToken("age")

        assertFailsWith<TemplaterException.RuntimeError> {
            helper.setProperty(person, ageToken, 50)
        }
    }

    @Test
    fun `hasProperty should return false for non-existent property`() {
        val person = Person("Grace", 33, 62000.0)
        assertFalse(helper.hasProperty(person, "notAProperty"))
    }

    @Test
    fun `hasProperty should return true for existing property`() {
        val person = Person("Helen", 29, 58000.0)
        assertTrue(helper.hasProperty(person, "name"))
    }
}
