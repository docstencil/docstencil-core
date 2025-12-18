package com.docstencil.core.managers

import com.docstencil.core.utils.XmlHelper
import org.w3c.dom.Document
import org.w3c.dom.Element

class ContentTypesManager private constructor(
    private val doc: Document,
    private val typesElement: Element,
) : ArchiveFileManager {
    private var dirty = false

    companion object {
        private const val CONTENT_TYPES_PATH = "[Content_Types].xml"
        private const val TYPES_NAMESPACE =
            "http://schemas.openxmlformats.org/package/2006/content-types"

        fun create(files: Map<String, ByteArray>): ContentTypesManager {
            val xml = files[CONTENT_TYPES_PATH]?.toString(Charsets.UTF_8)
                ?: throw IllegalStateException("Content types file not found.")

            val document = XmlHelper.parseXml(xml)
            return ContentTypesManager(document, document.documentElement)
        }
    }

    override fun getFilePath(): String = CONTENT_TYPES_PATH

    override fun isDirty(): Boolean = dirty

    override fun render(): ByteArray {
        return XmlHelper.serializeXml(doc).toByteArray(Charsets.UTF_8)
    }

    fun hasOverride(partName: String): Boolean {
        val overrides = XmlHelper.getElementsByTagName(typesElement, "Override")
        return overrides.any { override ->
            XmlHelper.getAttribute(override, "PartName") == partName
        }
    }

    fun ensureOverride(partName: String, contentType: String) {
        if (hasOverride(partName)) {
            return
        }

        val overrideElement = doc.createElementNS(TYPES_NAMESPACE, "Override")
        overrideElement.setAttribute("PartName", partName)
        overrideElement.setAttribute("ContentType", contentType)
        typesElement.appendChild(overrideElement)

        dirty = true
    }

    fun hasDefault(extension: String): Boolean {
        val defaults = XmlHelper.getElementsByTagName(typesElement, "Default")
        return defaults.any { default ->
            XmlHelper.getAttribute(default, "Extension") == extension
        }
    }

    fun ensureDefault(extension: String, contentType: String) {
        if (hasDefault(extension)) {
            return
        }

        val defaultElement = doc.createElementNS(TYPES_NAMESPACE, "Default")
        defaultElement.setAttribute("Extension", extension)
        defaultElement.setAttribute("ContentType", contentType)
        typesElement.appendChild(defaultElement)

        dirty = true
    }
}
