package com.docstencil.core.modules

import com.docstencil.core.api.OfficeTemplateOptions
import com.docstencil.core.api.model.OfficeTemplateData
import com.docstencil.core.config.FileTypeConfig
import com.docstencil.core.error.ContentIdxRange
import com.docstencil.core.error.TemplaterException
import com.docstencil.core.managers.CommentManager
import com.docstencil.core.managers.RelationshipManager
import com.docstencil.core.modules.ErrorLocationModule.MarkerInsertion.MarkerType
import com.docstencil.core.scanner.RawXmlScanner
import com.docstencil.core.scanner.model.TagPartType
import com.docstencil.core.scanner.model.XmlRawInputToken
import java.util.*

private const val COMMENTS_CONTENT_TYPE =
    "application/vnd.openxmlformats-officedocument.wordprocessingml.comments+xml"

private const val COMMENTS_REL_TYPE =
    "http://schemas.openxmlformats.org/officeDocument/2006/relationships/comments"

class ErrorLocationModule : TemplateModule {
    data class Error(
        val text: String,
        val location: ContentIdxRange,
    )

    data class ErrorWithComment(
        val error: Error,
        val comment: CommentManager.Comment,
    )

    private val errors: MutableList<Error> = Collections.synchronizedList(mutableListOf())

    fun registerError(exception: TemplaterException, filePath: String) {
        val error = Error(exception.getFullMessage(filePath), exception.getRange())
        errors.add(error)
    }

    override fun registerManagers(files: OfficeTemplateData) {
        val relationshipManager = files.requireManager<RelationshipManager>()
        val manager = CommentManager.create(files.getFiles(), relationshipManager)
        files.registerManager(CommentManager::class, manager)
    }

    override fun postRender(files: OfficeTemplateData) {
        if (errors.isEmpty()) {
            return
        }

        val commentManager = files.commentManager()
        // Pair errors and comments.
        val errorsWithComments = errors
            .sortedBy { it.location.fromIncl.value }
            .map {
                ErrorWithComment(
                    it,
                    commentManager.createComment(it.text, "Error Reporter", "ER"),
                )
            }

        // Add errors to comments files via comment manager.
        errorsWithComments.forEach { errorWithComment ->
            commentManager.addComment(errorWithComment.comment)
        }

        // Add markers into the main document.
        files.update(files.relationshipManager().mainFile) { data ->
            val documentTokens = scanDocument(data.toString(Charsets.UTF_8))
            val indexedRuns = buildRunIndex(documentTokens)
            insertCommentMarkup(documentTokens, indexedRuns, errorsWithComments)
        }

        // Ensure that comments file is in rels and content type file.
        val commentsPath = files.commentManager().getFilePath()
        val commentsFilename = commentsPath.substringAfterLast('/')

        files.relationshipManager().ensureRelationship(COMMENTS_REL_TYPE, commentsFilename)
        files.contentTypesManager().ensureOverride("/$commentsPath", COMMENTS_CONTENT_TYPE)
    }

    private fun scanDocument(documentXml: String): List<XmlRawInputToken> {
        val scanner = RawXmlScanner(
            delimiters = OfficeTemplateOptions.Delimiters("{", "}"),
            templateTagNames = FileTypeConfig.docx().tagsText.toSet(),
        )
        return scanner.scan(documentXml)
    }

    // Track which w:r tag contains which content positions
    private data class RunInfo(
        val startTokenIdx: Int,  // Index of <w:r> opening tag
        val endTokenIdx: Int,    // Index of </w:r> closing tag
        val contentStart: Int,   // ContentIdx where this run's content starts
        val contentEnd: Int,     // ContentIdx where this run's content ends
    )

    private fun buildRunIndex(tokens: List<XmlRawInputToken>): List<RunInfo> {
        val runs = mutableListOf<RunInfo>()
        var currentRunStart: Int? = null
        var contentIdx = 0
        var runContentStart = 0

        tokens.forEachIndexed { tokenIndex, token ->
            // Track w:r tag boundaries.
            if (token is XmlRawInputToken.TagPart && token.tagName == "w:r") {
                if (token.tagPartType == TagPartType.OPENING) {
                    currentRunStart = tokenIndex
                    runContentStart = contentIdx
                } else if (token.tagPartType == TagPartType.CLOSING && currentRunStart != null) {
                    runs.add(RunInfo(currentRunStart, tokenIndex, runContentStart, contentIdx))
                    currentRunStart = null
                }
            }

            // Track content positions.
            if (token.canContainTemplateExpressionText()) {
                contentIdx += token.logicalText.length
            }
        }

        return runs
    }

    private data class MarkerInsertion(
        // Insert before this token index.
        val tokenIndex: Int,
        val markerType: MarkerType,
        val commentId: Int,
    ) {
        enum class MarkerType(val priority: Int) {
            START(0), END(1), REFERENCE(2)
        }
    }

    private fun insertCommentMarkup(
        tokens: List<XmlRawInputToken>,
        indexedRuns: List<RunInfo>,
        errors: List<ErrorWithComment>,
    ): ByteArray {
        val insertions = mutableListOf<MarkerInsertion>()

        errors.forEach { errorWithComment ->
            val startRun = indexedRuns.find {
                val pos = errorWithComment.error.location.fromIncl.value
                pos >= it.contentStart && pos < it.contentEnd
            }
                ?: indexedRuns.firstOrNull()
                ?: throw IllegalArgumentException("No run found for ContentIdx ${errorWithComment.error.location.fromIncl.value}")

            val endRun =
                indexedRuns.find {
                    val pos = errorWithComment.error.location.toExcl.value
                    pos >= it.contentStart && pos <= it.contentEnd
                }
                    ?: indexedRuns.firstOrNull()
                    ?: throw IllegalArgumentException("No run found for ContentIdx ${errorWithComment.error.location.toExcl.value}")

            val id = errorWithComment.comment.id
            insertions.add(MarkerInsertion(startRun.startTokenIdx, MarkerType.START, id))
            insertions.add(MarkerInsertion(endRun.endTokenIdx + 1, MarkerType.END, id))
            insertions.add(MarkerInsertion(endRun.endTokenIdx + 1, MarkerType.REFERENCE, id))
        }

        val newTokens = tokens.toMutableList()
        insertions
            .sortedWith(compareBy({ it.tokenIndex }, { it.markerType.priority }))
            // Insert in reverse order because later items push earlier items back.
            .reversed()
            .forEach { marker -> newTokens.add(marker.tokenIndex, createMarkerToken(marker)) }

        return newTokens
            .joinToString("") { it.xmlString }
            .toByteArray(Charsets.UTF_8)
    }

    private fun createMarkerToken(marker: MarkerInsertion): XmlRawInputToken {
        val rawXml = when (marker.markerType) {
            MarkerType.START -> """<w:commentRangeStart w:id="${marker.commentId}"/>"""
            MarkerType.END -> """<w:commentRangeEnd w:id="${marker.commentId}"/>"""
            MarkerType.REFERENCE -> """<w:r><w:commentReference w:id="${marker.commentId}"/></w:r>"""
        }
        return XmlRawInputToken.Verbatim(0, rawXml)
    }
}
