package com.docstencil.core.managers

import com.docstencil.core.utils.XmlHelper
import org.w3c.dom.Document
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

private const val DEFAULT_COMMENTS_XML = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<w:comments xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main" xmlns:mc="http://schemas.openxmlformats.org/markup-compatibility/2006" mc:Ignorable="w14 w15 wp14">
</w:comments>"""

class CommentManager private constructor(
    private val filePath: String,
    private val doc: Document,
    nextCommentIdInit: Int,
) : ArchiveFileManager {
    private val nextCommentId = AtomicInteger(nextCommentIdInit)
    private val dirty = AtomicBoolean(false)

    companion object {
        private const val W_NAMESPACE =
            "http://schemas.openxmlformats.org/wordprocessingml/2006/main"

        fun create(
            files: Map<String, ByteArray>,
            relationshipManager: RelationshipManager,
        ): CommentManager {
            val mainDocPath = relationshipManager.mainFile

            val documentDir = mainDocPath.substringBeforeLast('/')
            val commentsPath = "$documentDir/comments.xml"

            val xml = files[commentsPath]?.toString(Charsets.UTF_8) ?: DEFAULT_COMMENTS_XML
            val document = XmlHelper.parseXml(xml)

            val maxCommentId = XmlHelper.getElementsByTagName(document.documentElement, "w:comment")
                .maxOfOrNull { comment ->
                    XmlHelper.getAttributeNS(comment, W_NAMESPACE, "id")?.toIntOrNull() ?: -1
                } ?: -1

            return CommentManager(commentsPath, document, maxCommentId + 1)
        }
    }

    data class Comment(
        val id: Int,
        val author: String,
        val date: String,
        val initials: String,
        val text: String,
    )

    override fun render(): ByteArray? {
        val comments = XmlHelper.getElementsByTagName(doc.documentElement, "w:comment")
        if (comments.isEmpty()) {
            return null
        }

        return XmlHelper.serializeXml(doc).toByteArray(Charsets.UTF_8)
    }

    override fun getFilePath(): String = filePath

    override fun isDirty(): Boolean = dirty.get()

    fun createComment(
        text: String,
        author: String = "automatic-reviewer",
        initials: String = "ar",
    ): Comment {
        val date = Instant.now().truncatedTo(ChronoUnit.SECONDS).toString()
        return Comment(nextCommentId.getAndIncrement(), author, date, initials, text)
    }

    @Synchronized
    fun addComment(comment: Comment) {
        val commentElement = doc.createElementNS(W_NAMESPACE, "w:comment")
        commentElement.setAttributeNS(W_NAMESPACE, "w:id", comment.id.toString())
        commentElement.setAttributeNS(W_NAMESPACE, "w:author", comment.author)
        commentElement.setAttributeNS(W_NAMESPACE, "w:date", comment.date)
        commentElement.setAttributeNS(W_NAMESPACE, "w:initials", comment.initials)

        val pElement = doc.createElementNS(W_NAMESPACE, "w:p")
        val rElement = doc.createElementNS(W_NAMESPACE, "w:r")
        val tElement = doc.createElementNS(W_NAMESPACE, "w:t")
        tElement.setAttribute("xml:space", "preserve")
        tElement.textContent = comment.text

        rElement.appendChild(tElement)
        pElement.appendChild(rElement)
        commentElement.appendChild(pElement)
        doc.documentElement.appendChild(commentElement)

        dirty.set(true)
    }
}
