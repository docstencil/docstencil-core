package com.docstencil.core.builtin

import com.docstencil.core.error.TemplaterException
import java.util.*

class TypeCaster {
    fun asString(x: Any?): String {
        return Objects.toString(x)
    }

    fun asByte(n: Any?): Byte? {
        if (n == null) {
            return null
        }
        if (n !is Number) {
            throw TemplaterException.DeepRuntimeError(
                "Can only convert numbers to byte. Got: " + asString(n),
            )
        }

        return n.toByte()
    }

    fun asShort(n: Any?): Short? {
        if (n == null) {
            return null
        }
        if (n !is Number) {
            throw TemplaterException.DeepRuntimeError(
                "Can only convert numbers to short. Got: " + asString(n),
            )
        }

        return n.toShort()
    }

    fun asInt(n: Any?): Int? {
        if (n == null) {
            return null
        }
        if (n !is Number) {
            throw TemplaterException.DeepRuntimeError(
                "Can only convert numbers to int. Got: " + asString(n),
            )
        }

        return n.toInt()
    }

    fun asLong(n: Any?): Long? {
        if (n == null) {
            return null
        }
        if (n !is Number) {
            throw TemplaterException.DeepRuntimeError(
                "Can only convert numbers to long. Got: " + asString(n),
            )
        }

        return n.toLong()
    }

    fun asFloat(n: Any?): Float? {
        if (n == null) {
            return null
        }
        if (n !is Number) {
            throw TemplaterException.DeepRuntimeError(
                "Can only convert numbers to float. Got: " + asString(n),
            )
        }

        return n.toFloat()
    }

    fun asDouble(n: Any?): Double? {
        if (n == null) {
            return null
        }
        if (n !is Number) {
            throw TemplaterException.DeepRuntimeError(
                "Can only convert numbers to double. Got: " + asString(n),
            )
        }

        return n.toDouble()
    }
}
