package com.docstencil.core.utils

import org.w3c.dom.Document
import org.w3c.dom.Element
import org.w3c.dom.Node
import org.w3c.dom.NodeList
import java.io.ByteArrayInputStream
import java.io.StringWriter
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.OutputKeys
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult

/**
 * Helper service for parsing and manipulating XML using DOM API.
 */
object XmlHelper {
    private val documentBuilderFactory = DocumentBuilderFactory.newInstance().apply {
        isNamespaceAware = true
    }

    private val lenientDocumentBuilderFactory = DocumentBuilderFactory.newInstance().apply {
        isNamespaceAware = false  // Disable namespace awareness for lenient parsing
    }

    private val transformerFactory = TransformerFactory.newInstance()

    /**
     * Parse XML string into a DOM Document.
     */
    fun parseXml(xml: String): Document {
        val builder = documentBuilderFactory.newDocumentBuilder()
        return builder.parse(ByteArrayInputStream(xml.toByteArray(Charsets.UTF_8)))
    }

    /**
     * Parse XML string into a DOM Document with lenient namespace handling.
     * This is useful when parsing XML that may have namespace prefixes without declarations.
     */
    fun parseXmlLenient(xml: String): Document {
        val builder = lenientDocumentBuilderFactory.newDocumentBuilder()
        return builder.parse(ByteArrayInputStream(xml.toByteArray(Charsets.UTF_8)))
    }

    /**
     * Serialize a DOM Document back to XML string.
     */
    fun serializeXml(doc: Document, omitXmlDeclaration: Boolean = false): String {
        val transformer = transformerFactory.newTransformer()
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8")
        transformer.setOutputProperty(OutputKeys.INDENT, "no")
        if (omitXmlDeclaration) {
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes")
        } else {
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no")
            transformer.setOutputProperty(OutputKeys.STANDALONE, "yes")
        }

        val writer = StringWriter()
        transformer.transform(DOMSource(doc), StreamResult(writer))
        return writer.toString()
    }

    /**
     * Get all child elements with a specific tag name (namespace-aware).
     */
    fun getElementsByTagName(parent: Element, tagName: String): List<Element> {
        val result = mutableListOf<Element>()
        val nodeList = parent.getElementsByTagName(tagName)
        for (i in 0 until nodeList.length) {
            val node = nodeList.item(i)
            if (node is Element) {
                result.add(node)
            }
        }
        return result
    }

    /**
     * Get all child elements with a specific local name (ignoring namespace).
     */
    fun getElementsByLocalName(parent: Element, localName: String): List<Element> {
        val result = mutableListOf<Element>()
        val nodeList = parent.childNodes
        for (i in 0 until nodeList.length) {
            val node = nodeList.item(i)
            if (node is Element && node.localName == localName) {
                result.add(node)
            }
        }
        return result
    }

    /**
     * Get attribute value (namespace-aware).
     */
    fun getAttribute(element: Element, attributeName: String): String? {
        return if (element.hasAttribute(attributeName)) {
            element.getAttribute(attributeName)
        } else {
            null
        }
    }

    /**
     * Get attribute value with namespace.
     */
    fun getAttributeNS(element: Element, namespaceURI: String?, localName: String): String? {
        return if (element.hasAttributeNS(namespaceURI, localName)) {
            element.getAttributeNS(namespaceURI, localName)
        } else {
            null
        }
    }

    /**
     * Create a new element with namespace.
     */
    fun createElement(doc: Document, namespaceURI: String?, qualifiedName: String): Element {
        return if (namespaceURI != null) {
            doc.createElementNS(namespaceURI, qualifiedName)
        } else {
            doc.createElement(qualifiedName)
        }
    }

    /**
     * Append a new child element to parent.
     */
    fun appendChild(parent: Element, child: Element): Element {
        parent.appendChild(child)
        return child
    }

    /**
     * Convert NodeList to List<Node> for easier iteration.
     */
    fun nodeListToList(nodeList: NodeList): List<Node> {
        val result = mutableListOf<Node>()
        for (i in 0 until nodeList.length) {
            result.add(nodeList.item(i))
        }
        return result
    }

    /**
     * Get text content from first child element with given tag name.
     */
    fun getTextContent(parent: Element, tagName: String): String? {
        val elements = getElementsByTagName(parent, tagName)
        return elements.firstOrNull()?.textContent
    }

    /**
     * Set text content for an element, escaping automatically.
     */
    fun setTextContent(element: Element, text: String) {
        element.textContent = text
    }
}
