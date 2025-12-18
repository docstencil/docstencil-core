package com.docstencil.core.managers

import com.docstencil.core.utils.FileTypeDetector
import com.docstencil.core.utils.RelationshipParser
import com.docstencil.core.utils.XmlHelper
import org.w3c.dom.Document
import org.w3c.dom.Element
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

private const val BASE_RELATIONSHIPS_XML =
    """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
</Relationships>"""

class RelationshipManager private constructor(
    private val filePath: String,
    private val doc: Document,
    val mainFile: String,
    val targetFiles: List<String>,
    nextRelIdInit: Int,
) : ArchiveFileManager {
    private val nextRelId = AtomicInteger(nextRelIdInit)
    private val dirty = AtomicBoolean(false)

    companion object {
        private const val RELS_NAMESPACE =
            "http://schemas.openxmlformats.org/package/2006/relationships"
        private val relationshipParser = RelationshipParser()

        fun create(
            files: Map<String, ByteArray>,
            detectionResult: FileTypeDetector.DetectionResult,
        ): RelationshipManager {
            val mainDocDir = detectionResult.mainFile.substringBeforeLast('/')
            val mainDocFileName = detectionResult.mainFile.substringAfterLast('/')
            val relsPath = "$mainDocDir/_rels/$mainDocFileName.rels"

            val xml = files[relsPath]?.toString(Charsets.UTF_8) ?: BASE_RELATIONSHIPS_XML
            val document = XmlHelper.parseXml(xml)

            val maxRelId = XmlHelper.getElementsByTagName(document.documentElement, "Relationship")
                .maxOfOrNull { rel ->
                    XmlHelper.getAttribute(rel, "Id")
                        ?.removePrefix("rId")
                        ?.toIntOrNull() ?: -1
                } ?: -1

            return RelationshipManager(
                relsPath,
                document,
                detectionResult.mainFile,
                detectionResult.targetFiles,
                maxRelId + 1,
            )
        }
    }

    override fun render(): ByteArray? {
        val relationships = XmlHelper.getElementsByTagName(doc.documentElement, "Relationship")
        if (relationships.isEmpty()) {
            // Don't create empty rels file.
            return null
        }

        val xml = XmlHelper.serializeXml(doc)
        return xml.toByteArray(Charsets.UTF_8)
    }

    override fun getFilePath(): String = filePath

    override fun isDirty(): Boolean = dirty.get()

    fun tryGetRelationship(
        type: String,
        target: String? = null,
        targetMode: String? = null,
    ): Element? {
        val relationships = XmlHelper.getElementsByTagName(doc.documentElement, "Relationship")
        return relationships.firstOrNull { rel ->
            val relType = XmlHelper.getAttribute(rel, "Type")
            val relTarget = XmlHelper.getAttribute(rel, "Target")
            val relTargetMode = XmlHelper.getAttribute(rel, "TargetMode")
            relType == type &&
                    (target == null || relTarget == target) &&
                    (targetMode == null || relTargetMode == targetMode)
        }
    }

    @Synchronized
    fun ensureRelationship(type: String, target: String, targetMode: String? = null): String {
        val existingRelationship = tryGetRelationship(type, target, targetMode)
        if (existingRelationship != null) {
            return existingRelationship.getAttribute("Id")
        }

        val id = "rId${nextRelId.getAndIncrement()}"

        val relElement = doc.createElementNS(RELS_NAMESPACE, "Relationship")
        relElement.setAttribute("Id", id)
        relElement.setAttribute("Type", type)
        relElement.setAttribute("Target", target)
        if (targetMode != null) {
            relElement.setAttribute("TargetMode", targetMode)
        }

        doc.documentElement.appendChild(relElement)

        dirty.set(true)

        return id
    }
}
