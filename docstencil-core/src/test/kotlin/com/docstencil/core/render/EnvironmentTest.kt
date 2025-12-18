package com.docstencil.core.render

import com.docstencil.core.error.TemplaterException
import com.docstencil.core.scanner.model.ContentIdx
import com.docstencil.core.scanner.model.TemplateToken
import com.docstencil.core.scanner.model.TemplateTokenType
import kotlin.test.*

class EnvironmentTest {
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
    fun `define and get should handle null value correctly`() {
        val env = Environment()
        env.define("x", null)

        val result = env.get(createToken("x"))

        assertNull(result)
    }

    @Test
    fun `define and get should handle non-null value correctly`() {
        val env = Environment()
        env.define("str", "hello")
        env.define("num", 42)
        env.define("bool", true)

        assertEquals("hello", env.get(createToken("str")))
        assertEquals(42, env.get(createToken("num")))
        assertEquals(true, env.get(createToken("bool")))
    }

    @Test
    fun `define should allow overwriting null with non-null`() {
        val env = Environment()
        env.define("x", null)
        env.define("x", "not null")

        val result = env.get(createToken("x"))

        assertEquals("not null", result)
    }

    @Test
    fun `define should allow overwriting non-null with null`() {
        val env = Environment()
        env.define("x", "not null")
        env.define("x", null)

        val result = env.get(createToken("x"))

        assertNull(result)
    }

    @Test
    fun `get should find null value in enclosing scope`() {
        val parent = Environment()
        parent.define("x", null)
        val child = Environment(parent)

        val result = child.get(createToken("x"))

        assertNull(result)
    }

    @Test
    fun `assign should update null value in enclosing scope`() {
        val parent = Environment()
        parent.define("x", null)
        val child = Environment(parent)

        child.assign(createToken("x"), "updated from child")

        assertEquals("updated from child", parent.get(createToken("x")))
    }

    @Test
    fun `get should throw for undefined variable not null variable`() {
        val env = Environment()
        env.define("x", null)

        assertNull(env.get(createToken("x")))

        assertFailsWith<TemplaterException.RuntimeError> {
            env.get(createToken("y"))
        }
    }

    @Test
    fun `definedVars should include variables with null values`() {
        val env = Environment()
        env.define("nullVar", null)
        env.define("nonNullVar", "value")

        val vars = env.definedVars()

        assertEquals(2, vars.size)
        assertTrue(vars.contains("nullVar"))
        assertTrue(vars.contains("nonNullVar"))
    }

    @Test
    fun `multiple null variables should be independent`() {
        val env = Environment()
        env.define("a", null)
        env.define("b", null)
        env.define("c", null)

        env.assign(createToken("b"), "updated")

        assertNull(env.get(createToken("a")))
        assertEquals("updated", env.get(createToken("b")))
        assertNull(env.get(createToken("c")))
    }
}
