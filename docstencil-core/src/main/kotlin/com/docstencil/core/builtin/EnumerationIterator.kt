package com.docstencil.core.builtin

class EnumerationIterator<T>(private val baseIterator: Iterator<T>) :
    Iterator<com.docstencil.core.builtin.EnumeratedValue<T>> {
    var idx = 0

    override fun next(): EnumeratedValue<T> {
        val rawNext = baseIterator.next()
        val enumeratedNext = EnumeratedValue(rawNext, idx, idx == 0, !hasNext())
        ++idx
        return enumeratedNext
    }

    override fun hasNext(): Boolean {
        return baseIterator.hasNext()
    }
}
