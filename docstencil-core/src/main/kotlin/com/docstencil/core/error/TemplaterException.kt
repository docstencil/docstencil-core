package com.docstencil.core.error

import com.docstencil.core.scanner.model.ContentIdx
import com.docstencil.core.scanner.model.TemplateToken


sealed class TemplaterException(
    message: String,
    cause: Throwable? = null,
) : RuntimeException(message, cause) {
    abstract fun getRange(): ContentIdxRange

    open fun getFullMessage(filePath: String): String {
        return message ?: "Unknown error"
    }

    /**
     * Error encountered while rendering the template.
     */
    class RuntimeError(
        message: String,
        val token: TemplateToken,
        cause: Throwable? = null,
    ) : TemplaterException(message, cause) {
        override fun getRange(): ContentIdxRange {
            return ContentIdxRange(
                token.startIdx,
                ContentIdx(token.startIdx.value + token.lexeme.length),
            )
        }
    }

    /**
     * Error encountered while parsing the template.
     */
    class ParseError(
        message: String,
        val token: TemplateToken? = null,
        cause: Throwable? = null,
    ) : TemplaterException(message, cause) {
        override fun getRange(): ContentIdxRange {
            if (token == null) {
                return ContentIdxRange(ContentIdx(0), ContentIdx(1))
            }
            return ContentIdxRange(token.startIdx, token.startIdx.add(token.lexeme.length))
        }
    }

    /**
     * Error encountered while scanning the template.
     */
    class ScanError(
        message: String,
        val contentIdx: ContentIdx,
        cause: Throwable? = null,
    ) : TemplaterException(message, cause) {
        override fun getRange(): ContentIdxRange {
            return ContentIdxRange(contentIdx, contentIdx.increment())
        }
    }

    /**
     * Error encountered while rendering the template and not having access to positional debug
     * information. This exception is intended to be caught and rethrown with additional debug
     * information.
     */
    class DeepRuntimeError(
        message: String,
        cause: Throwable? = null,
    ) : TemplaterException(message, cause) {
        override fun getRange(): ContentIdxRange {
            return ContentIdxRange(ContentIdx(0), ContentIdx(1))
        }
    }

    /**
     * Indicates errors in the XML structure of the input document.
     */
    class XmlError(
        message: String,
        val offset: Int,
        cause: Throwable? = null,
    ) : TemplaterException(message, cause) {
        override fun getRange(): ContentIdxRange {
            return ContentIdxRange(ContentIdx(0), ContentIdx(1))
        }

        override fun getFullMessage(filePath: String): String {
            return "[$filePath:$offset] $message"
        }
    }

    /**
     * Fatal errors are never the user's fault. They are thrown when an illegal state is encountered
     * during runtime and always indicate a bug.
     */
    class FatalError(
        message: String,
        val token: TemplateToken? = null,
        val contentIdx: ContentIdx? = null,
        cause: Throwable? = null,
    ) : TemplaterException(message, cause) {
        override fun getRange(): ContentIdxRange {
            if (token != null) {
                return ContentIdxRange(token.startIdx, token.startIdx.add(token.lexeme.length))
            }
            if (contentIdx != null) {
                return ContentIdxRange(contentIdx, contentIdx.increment())
            }
            return ContentIdxRange(ContentIdx(0), ContentIdx(1))
        }
    }

    // TODO(Alex): catch and rethrow this
    /**
     * Fatal error encountered while rendering the template and not having access to positional
     * debug information. This exception is intended to be caught and rethrown with additional debug
     * information.
     */
    class DeepFatalError(
        message: String,
        cause: Throwable? = null,
    ) : TemplaterException(message, cause) {
        override fun getRange(): ContentIdxRange {
            return ContentIdxRange(ContentIdx(0), ContentIdx(1))
        }
    }
}

data class ContentIdxRange(val fromIncl: ContentIdx, val toExcl: ContentIdx)
