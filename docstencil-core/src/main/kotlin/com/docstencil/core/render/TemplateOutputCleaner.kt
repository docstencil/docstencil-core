package com.docstencil.core.render

import com.docstencil.core.error.TemplaterException
import com.docstencil.core.render.model.XmlOutputToken
import com.docstencil.core.scanner.model.TagPartType
import kotlin.math.max
import kotlin.math.min

private data class Range(val fromIncl: Int, val toExcl: Int)

private data class TagWithIdx(
    val tagName: String,
    val idx: Int,
    var hasSeenSentinelValue: Boolean = false,
)

class TemplateOutputCleaner(private val deletableTagNames: Set<String>) {
    /**
     * Clear out all empty tags with sentinel values. Sentinel values indicate that those tags once
     * held template expressions that have been processed.
     *
     * A tag is considered empty if it contains no visible content other than sentinels.
     */
    fun clean(tokens: List<XmlOutputToken>): List<XmlOutputToken> {
        val rangesToDelete = mutableListOf<Range>()
        val stack = mutableListOf<TagWithIdx>()

        for (idx in 0..<tokens.size) {
            when (val currToken = tokens[idx]) {
                is XmlOutputToken.Sentinel -> {
                    // Mark stack as having sentinel values and therefore being a candidate for removal.
                    stack.lastOrNull()?.hasSeenSentinelValue = true
                    continue
                }

                is XmlOutputToken.Content -> {
                    val hasContent = if (currToken.isGeneratedByTemplate) {
                        // Empty string in template means: keep this
                        currToken.text != null
                    } else {
                        // Non-empty string in user content blocks removal.
                        // Note that even a string consisting only of whitespace should block
                        // removal.
                        !currToken.text.isNullOrEmpty()
                    }
                    if (hasContent) {
                        stack.clear()
                    }
                    continue
                }

                is XmlOutputToken.Verbatim -> continue

                is XmlOutputToken.TagPart -> {
                    val isRelevantTag = deletableTagNames.contains(currToken.tagName)
                    if (!isRelevantTag) {
                        // Non-deletable opening tags are structural content worth keeping.
                        // Self-closing tags don't count as structural.
                        if (currToken.tagPartType == TagPartType.OPENING) {
                            stack.clear()
                        }
                        continue
                    }
                    when (currToken.tagPartType) {
                        TagPartType.OPENING -> stack.add(TagWithIdx(currToken.tagName, idx))
                        TagPartType.SELF_CLOSING -> continue
                        TagPartType.CLOSING -> {
                            val stackTop = stack.lastOrNull() ?: continue

                            // Remove stack top and propagate `hasSeenSentinelValue`.
                            stack.removeLast()
                            val stackSecondFromTop = stack.lastOrNull()
                            if (stackSecondFromTop != null) {
                                stackSecondFromTop.hasSeenSentinelValue =
                                    stackTop.hasSeenSentinelValue
                            }

                            // In post-rendering, skip mismatched tags instead of throwing errors
                            if (stackTop.tagName != currToken.tagName) {
                                throw TemplaterException.FatalError("Mismatched tags in post-processing.")
                            }
                            if (!stackTop.hasSeenSentinelValue) {
                                continue
                            }

                            var rangeToAdd = Range(stackTop.idx, idx + 1)
                            // Ensure ranges are disjoint by merging every new range with all previous ones if possible.
                            while (
                                rangesToDelete.isNotEmpty() &&
                                rangeToAdd.fromIncl <= rangesToDelete.last().toExcl
                            ) {
                                rangeToAdd = Range(
                                    min(rangeToAdd.fromIncl, rangesToDelete.last().fromIncl),
                                    max(rangeToAdd.toExcl, rangesToDelete.last().toExcl),
                                )
                                rangesToDelete.removeLast()
                            }

                            rangesToDelete.add(rangeToAdd)
                        }
                    }
                }
            }
        }

        return deleteRanges(tokens, rangesToDelete)
    }

    private fun deleteRanges(
        allTokens: List<XmlOutputToken>,
        rangesToDelete: List<Range>,
    ): List<XmlOutputToken> {
        if (rangesToDelete.isEmpty()) {
            return allTokens
        }

        val rangesToKeep = invertRanges(rangesToDelete, allTokens.size)
        val tokensToKeep = mutableListOf<XmlOutputToken>()
        for (rangeToKeep in rangesToKeep) {
            tokensToKeep.addAll(allTokens.subList(rangeToKeep.fromIncl, rangeToKeep.toExcl))
        }

        return tokensToKeep
    }

    private fun invertRanges(rangesToDelete: List<Range>, totalSize: Int): List<Range> {
        if (rangesToDelete.isEmpty()) {
            return listOf()
        }

        val rangesToDeleteFixed = mutableListOf<Range>()
        rangesToDeleteFixed.add(Range(-1, 0))
        rangesToDeleteFixed.addAll(rangesToDelete)
        rangesToDeleteFixed.add(Range(totalSize, totalSize + 1))

        val rangesToKeep = mutableListOf<Range>()

        for (idx in 0..<(rangesToDeleteFixed.size - 1)) {
            val range1 = rangesToDeleteFixed[idx]
            val range2 = rangesToDeleteFixed[idx + 1]

            if (range1.toExcl < range2.fromIncl) {
                rangesToKeep.add(Range(range1.toExcl, range2.fromIncl))
            }
        }

        return rangesToKeep
    }
}
