package com.docstencil.core.render

import com.docstencil.core.error.TemplaterException
import com.docstencil.core.scanner.model.TemplateToken

/**
 * Helper class for property access that supports both native objects and maps.
 *
 * Priority order:
 * 1. Try accessing as object property using NativeObjectHelper
 * 2. If that fails, check if object is a Map and use map operations
 *
 * This ensures that real properties (like Map.size) take priority over
 * map entries with the same key name.
 */
class PropertyAccessHelper {
    private val nativeObjectHelper = NativeObjectHelper()

    fun hasProperty(obj: Any, propertyName: String): Boolean {
        if (nativeObjectHelper.hasProperty(obj, propertyName)) {
            return true
        }

        if (obj is Map<*, *>) {
            return obj.containsKey(propertyName)
        }

        return false
    }

    fun getProperty(obj: Any, name: TemplateToken): Any? {
        val propertyName = name.lexeme

        try {
            return nativeObjectHelper.getProperty(obj, name)
        } catch (_: TemplaterException.RuntimeError) {
            // Property was not found, try the map fallback.
        } catch (e: TemplaterException.DeepRuntimeError) {
            throw TemplaterException.RuntimeError(
                e.message ?: "Error accessing property '$propertyName'",
                name,
                e.cause,
            )
        }

        if (obj is Map<*, *>) {
            if (obj.containsKey(propertyName)) {
                return obj[propertyName]
            }
        }

        val className = obj::class.simpleName ?: "Unknown"
        throw TemplaterException.RuntimeError(
            "Property or key '$propertyName' not found on $className.",
            name,
        )
    }

    fun setProperty(obj: Any, name: TemplateToken, value: Any?) {
        val propertyName = name.lexeme

        try {
            nativeObjectHelper.setProperty(obj, name, value)
            return
        } catch (_: TemplaterException.RuntimeError) {
            // Property was not found or is read-only, try map fallback.
        }

        if (obj is Map<*, *>) {
            if (obj !is MutableMap<*, *>) {
                throw TemplaterException.RuntimeError(
                    "Cannot set property '$propertyName' because map is immutable (read-only).",
                    name,
                )
            }

            try {
                @Suppress("UNCHECKED_CAST")
                (obj as MutableMap<Any?, Any?>)[propertyName] = value
                return
            } catch (_: UnsupportedOperationException) {
                // Map claims to be mutable but isn't (e.g., from mapOf).
                throw TemplaterException.RuntimeError(
                    "Cannot set property '$propertyName' because map is immutable (read-only).",
                    name,
                )
            }
        }

        val className = obj::class.simpleName ?: "Unknown"
        throw TemplaterException.RuntimeError(
            "Property or key '$propertyName' not found on $className, or property is read-only.",
            name,
        )
    }
}
