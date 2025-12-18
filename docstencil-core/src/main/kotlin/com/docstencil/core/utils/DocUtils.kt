package com.docstencil.core.utils


object DocUtils {
    /**
     * Convert XML escape sequences back to UTF-8 characters.
     * Example: "&amp;" -> "&", "&lt;" -> "<"
     */
    fun unescapeXmlString(string: String): String {
        return string
            .replace("&apos;", "'")
            .replace("&quot;", "\"")
            .replace("&gt;", ">")
            .replace("&lt;", "<")
            .replace("&amp;", "&")
    }

    /**
     * Convert UTF-8 characters to escaped XML character sequences.
     * Example: "&" -> "&amp;", "<" -> "&lt;"
     */
    fun escapeXmlString(str: String): String = buildString(str.length + 16) {
        for (c in str) {
            when (c) {
                '&' -> append("&amp;")
                '<' -> append("&lt;")
                '>' -> append("&gt;")
                '"' -> append("&quot;")
                '\'' -> append("&apos;")
                else -> append(c)
            }
        }
    }
}
