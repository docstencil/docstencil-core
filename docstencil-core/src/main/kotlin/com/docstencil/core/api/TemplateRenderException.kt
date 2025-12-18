package com.docstencil.core.api

import com.docstencil.core.error.TemplaterException

/**
 * Exception thrown when template rendering fails and an explicit error is requested.
 *
 * This exception aggregates all errors that occurred during rendering and provides
 * a combined error message. Use this with the `*OrThrow` methods on [OfficeTemplateResult].
 *
 * @property errors The list of errors that occurred during template rendering.
 */
class TemplateRenderException(
    val errors: List<TemplaterException>,
) : RuntimeException(buildMessage(errors)) {
    companion object {
        private fun buildMessage(errors: List<TemplaterException>): String {
            return "Template rendering failed with ${errors.size} error(s):\n" +
                    errors.joinToString("\n") { "- ${it.message}" }
        }
    }
}
