package com.docstencil.core.scanner.model


data class TemplateTokenGroup(
    val tokens: List<TemplateToken>,
    val startInclusive: ContentIdx,
    val endExclusive: ContentIdx,
)
