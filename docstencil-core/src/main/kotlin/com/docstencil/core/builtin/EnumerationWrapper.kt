package com.docstencil.core.builtin

class EnumerationWrapper<T>(private val baseIterable: Iterable<T>) : Iterable<EnumeratedValue<T>> {
    override fun iterator(): Iterator<EnumeratedValue<T>> {
        return EnumerationIterator(baseIterable.iterator())
    }
}
