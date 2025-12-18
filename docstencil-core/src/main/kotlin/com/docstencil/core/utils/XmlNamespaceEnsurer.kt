package com.docstencil.core.utils

object XmlNamespaceEnsurer {
    /**
     * Ensures that the root element of the XML document has all default namespaces.
     * Preserves any existing namespace declarations.
     *
     * @param xmlContent The XML content as a string
     * @param defaultNamespaces Map of namespace prefix to namespace URI
     * @return XML content with ensured namespaces
     */
    fun ensure(xmlContent: String, defaultNamespaces: Map<String, String>): String {
        if (defaultNamespaces.isEmpty()) {
            return xmlContent
        }

        // Use lenient parsing to handle XML with undeclared namespace prefixes
        val doc = XmlHelper.parseXmlLenient(xmlContent)
        val root = doc.documentElement

        // Add missing namespaces (preserve existing)
        for ((prefix, uri) in defaultNamespaces) {
            val attrName = if (prefix.isEmpty()) "xmlns" else "xmlns:$prefix"
            if (!root.hasAttribute(attrName)) {
                root.setAttribute(attrName, uri)
            }
        }

        return XmlHelper.serializeXml(doc, omitXmlDeclaration = true)
    }
}
