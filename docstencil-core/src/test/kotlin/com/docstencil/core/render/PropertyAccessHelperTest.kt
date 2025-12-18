package com.docstencil.core.render

import com.docstencil.core.error.TemplaterException
import com.docstencil.core.helper.JavaPerson
import com.docstencil.core.scanner.model.ContentIdx
import com.docstencil.core.scanner.model.TemplateToken
import com.docstencil.core.scanner.model.TemplateTokenType
import kotlin.test.*

class PropertyAccessHelperTest {
    private val helper = PropertyAccessHelper()

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
    fun `getProperty should retrieve property from object`() {
        val person = Person("Alice", 30, 75000.0)
        val nameToken = createToken("name")

        val result = helper.getProperty(person, nameToken)

        assertEquals("Alice", result)
    }

    @Test
    fun `setProperty should update mutable property on object`() {
        val person = Person("Bob", 35, 80000.0)
        val nameToken = createToken("name")

        helper.setProperty(person, nameToken, "Bobby")

        assertEquals("Bobby", person.name)
    }

    @Test
    fun `getProperty should retrieve value from map when property not found`() {
        val data = mapOf("firstName" to "Charlie", "lastName" to "Brown")
        val firstNameToken = createToken("firstName")

        val result = helper.getProperty(data, firstNameToken)

        assertEquals("Charlie", result)
    }

    @Test
    fun `setProperty should update value in mutable map when property not found`() {
        val data = mutableMapOf("firstName" to "David", "lastName" to "Smith")
        val firstNameToken = createToken("firstName")

        helper.setProperty(data, firstNameToken, "Dave")

        assertEquals("Dave", data["firstName"])
    }

    @Test
    fun `getProperty should access Map size property not map entry`() {
        val data = mutableMapOf("size" to 999, "name" to "test")
        val sizeToken = createToken("size")

        val result = helper.getProperty(data, sizeToken)

        assertEquals(2, result)
    }

    @Test
    fun `getProperty should throw for non-existent property or key`() {
        val person = Person("Eve", 28, 65000.0)
        val token = createToken("nonExistent")

        assertFailsWith<TemplaterException.RuntimeError> {
            helper.getProperty(person, token)
        }
    }

    @Test
    fun `setProperty should throw for immutable map`() {
        val data = mapOf("firstName" to "Frank")
        val token = createToken("firstName")

        val exception = assertFailsWith<TemplaterException.RuntimeError> {
            helper.setProperty(data, token, "Francis")
        }

        assertTrue(exception.message!!.contains("immutable"))
    }

    @Test
    fun `setProperty should throw for read-only property on object`() {
        val person = Person("Grace", 32, 70000.0)
        val ageToken = createToken("age")

        assertFailsWith<TemplaterException.RuntimeError> {
            helper.setProperty(person, ageToken, 33)
        }
    }

    @Test
    fun `hasProperty should return true for object property`() {
        val person = Person("Helen", 29, 58000.0)

        assertTrue(helper.hasProperty(person, "name"))
    }

    @Test
    fun `hasProperty should return true for map key`() {
        val data = mapOf("firstName" to "Ian")

        assertTrue(helper.hasProperty(data, "firstName"))
    }

    @Test
    fun `hasProperty should return false for non-existent property or key`() {
        val person = Person("Jane", 27, 62000.0)

        assertFalse(helper.hasProperty(person, "nonExistent"))
    }

    @Test
    fun `hasProperty should prioritize object properties over map keys`() {
        val data = mapOf("size" to 999)

        assertTrue(helper.hasProperty(data, "size"))
    }

    @Test
    fun `getProperty should retrieve property from Java object using getter`() {
        val javaPerson = JavaPerson("Alice", 30, 75000.0)
        val nameToken = createToken("name")

        val result = helper.getProperty(javaPerson, nameToken)

        assertEquals("Alice", result)
    }

    @Test
    fun `setProperty should update property on Java object using setter`() {
        val javaPerson = JavaPerson("Bob", 35, 80000.0)
        val nameToken = createToken("name")

        helper.setProperty(javaPerson, nameToken, "Bobby")

        assertEquals("Bobby", javaPerson.name)
    }

    @Test
    fun `setProperty should handle multiple properties on Java object`() {
        val javaPerson = JavaPerson("Charlie", 40, 60000.0)
        val ageToken = createToken("age")
        val salaryToken = createToken("salary")

        helper.setProperty(javaPerson, ageToken, 41)
        helper.setProperty(javaPerson, salaryToken, 65000.0)

        assertEquals(41, javaPerson.age)
        assertEquals(65000.0, javaPerson.salary)
    }

    @Test
    fun `hasProperty should return true for Java object properties`() {
        val javaPerson = JavaPerson("David", 28, 55000.0)

        assertTrue(helper.hasProperty(javaPerson, "name"))
        assertTrue(helper.hasProperty(javaPerson, "age"))
        assertTrue(helper.hasProperty(javaPerson, "salary"))
        assertFalse(helper.hasProperty(javaPerson, "nonExistent"))
    }
}
