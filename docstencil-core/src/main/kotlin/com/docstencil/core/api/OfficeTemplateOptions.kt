package com.docstencil.core.api

import com.docstencil.core.modules.TemplateModule
import java.util.*
import java.util.zip.Deflater


/**
 * Configuration options for template processing.
 *
 * Use the default constructor for Kotlin or the [Builder] for Java.
 *
 * @see OfficeTemplate
 */
data class OfficeTemplateOptions(
    /**
     * Delimiters for placeholders (default: { and }).
     */
    val delimiters: Delimiters = Delimiters("{", "}"),

    /**
     * Strip invalid XML characters from rendered values.
     */
    val stripInvalidXmlChars: Boolean = false,

    /**
     * ZIP compression level (0-9, default is Deflater.DEFAULT_COMPRESSION).
     */
    val compressionLevel: Int = Deflater.DEFAULT_COMPRESSION,

    /**
     * Locale to use for builtin formatting functions.
     */
    val locale: Locale = Locale.getDefault(),

    /**
     * Whether to print the stack traces of exceptions encountered during parsing and rendering of
     * templates.
     */
    val printExceptionStackTraces: Boolean = true,

    /**
     * Enable parallel processing for for-loop iterations and builtin functions like $map and
     * $filter.
     */
    val parallelRendering: Boolean = false,

    /**
     * List of template modules to register. Modules provide additional functionality like
     * image insertion, footnotes, and endnotes.
     */
    val modules: List<TemplateModule> = emptyList(),
) {
    /**
     * Defines the opening and closing delimiters for template expressions.
     *
     * @property open The opening delimiter (default: "{").
     * @property close The closing delimiter (default: "}").
     */
    data class Delimiters(
        val open: String = "{",
        val close: String = "}",
    )

    /**
     * Returns a copy of this options object with the given module added.
     */
    fun addModule(module: TemplateModule): OfficeTemplateOptions {
        return copy(modules = modules + module)
    }

    /**
     * Builder class for creating OfficeTemplateOptions instances, primarily for Java interop.
     */
    class Builder {
        private var delimiters: Delimiters = Delimiters()
        private var stripInvalidXmlChars: Boolean = false
        private var compressionLevel: Int = Deflater.DEFAULT_COMPRESSION
        private var locale: Locale = Locale.getDefault()
        private var printExceptionStackTraces: Boolean = true
        private var parallelRendering: Boolean = false
        private var modules: List<TemplateModule> = emptyList()

        fun delimiters(delimiters: Delimiters) = apply { this.delimiters = delimiters }
        fun stripInvalidXmlChars(value: Boolean) = apply { this.stripInvalidXmlChars = value }
        fun compressionLevel(level: Int) = apply { this.compressionLevel = level }
        fun locale(locale: Locale) = apply { this.locale = locale }
        fun printExceptionStackTraces(value: Boolean) =
            apply { this.printExceptionStackTraces = value }

        fun parallelRendering(value: Boolean) = apply { this.parallelRendering = value }
        fun modules(modules: List<TemplateModule>) = apply { this.modules = modules }
        fun addModule(module: TemplateModule) = apply { this.modules = this.modules + module }

        fun build() = OfficeTemplateOptions(
            delimiters = delimiters,
            stripInvalidXmlChars = stripInvalidXmlChars,
            compressionLevel = compressionLevel,
            locale = locale,
            printExceptionStackTraces = printExceptionStackTraces,
            parallelRendering = parallelRendering,
            modules = modules,
        )
    }
}
