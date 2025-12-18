package com.docstencil.core.render

import com.docstencil.core.render.model.XmlOutputToken

// Regular expression for valid XML 1.0 characters (excluding control characters except tab, newline, carriage return)
// This pattern covers characters from U+0020 to U+D7FF, U+E000 to U+FFFD, and U+0009, U+000A, U+000D.
// It also handles surrogate pairs for characters outside the Basic Multilingual Plane.
private val VALID_XML_CHAR_REGEX =
    Regex("[^\\u0009\\u000A\\u000D\\u0020-\\uD7FF\\uE000-\\uFFFD\\uD800\\uDC00-\\uDBFF\\uDFFF]")

class TemplateOutputJoiner(
    private val stripInvalidXmlChars: Boolean,
) {
    fun join(tokens: List<XmlOutputToken>): String {
        val xml = buildString {
            tokens.forEach {
                val xmlString = it.xmlString
                if (xmlString.isNotEmpty()) append(xmlString)
            }
        }

        return if (stripInvalidXmlChars) {
            xml.replace(VALID_XML_CHAR_REGEX, "")
        } else {
            xml
        }
    }
}
