package com.docstencil.core.render

import com.docstencil.core.error.TemplaterException
import com.docstencil.core.scanner.model.TemplateToken
import java.util.concurrent.ConcurrentHashMap

class Environment(
    val enclosing: Environment? = null,
    // It is ok to always use a `ConcurrentHashMap` for now, even in single threaded scenarios.
    // There is no performance penalty on reads, only for writes, but those are relatively few.
    // Source: https://stackoverflow.com/questions/1378310/performance-concurrenthashmap-vs-hashmap
    private val values: MutableMap<String, Any> = ConcurrentHashMap<String, Any>(),
) {
    private object NullMarker {
        override fun toString() = "NullMarker"
    }

    fun definedVars(): Set<String> {
        return values.keys
    }

    fun define(name: String, value: Any?) {
        values[name] = value ?: NullMarker
    }

    fun get(name: TemplateToken): Any? {
        val value = values[name.lexeme]
        if (value != null) {
            return if (value === NullMarker) null else value
        }

        if (enclosing != null) return enclosing.get(name)

        throw TemplaterException.RuntimeError("Undefined variable '${name.lexeme}'.", name)
    }

    fun getAt(distance: Int, lexeme: String): Any? {
        val value = ancestor(distance).values[lexeme]
        return if (value === NullMarker) null else value
    }

    fun assign(name: TemplateToken, value: Any?) {
        // This should be ok even during concurrent access.
        if (values.containsKey(name.lexeme)) {
            values[name.lexeme] = value ?: NullMarker
            return
        }

        if (enclosing != null) {
            enclosing.assign(name, value)
            return
        }

        throw TemplaterException.RuntimeError("Undefined variable '${name.lexeme}'.", name)
    }

    fun assignAt(distance: Int, name: TemplateToken, value: Any?) {
        ancestor(distance).values[name.lexeme] = value ?: NullMarker
    }

    private fun ancestor(distance: Int): Environment {
        var env = this
        (0 until distance).forEach { _ ->
            if (env.enclosing == null) {
                throw TemplaterException.FatalError("Could not find enclosing environment.")
            }
            env = env.enclosing
        }
        return env
    }
}
