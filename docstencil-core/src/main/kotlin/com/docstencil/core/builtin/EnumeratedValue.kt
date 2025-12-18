package com.docstencil.core.builtin

data class EnumeratedValue<T>(
    val value: T,
    val index: Int,
    val isFirst: Boolean,
    val isLast: Boolean,
) {
    val isEven = index % 2 == 0
    val isOdd = index % 2 == 1
}
