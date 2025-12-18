package com.docstencil.core.render

import com.docstencil.core.render.model.XmlOutputToken
import com.docstencil.core.scanner.model.TagPartType
import kotlin.test.Test
import kotlin.test.assertEquals

class TemplateOutputCleanerTest {
    private fun createCleaner(
        deletableTagNames: Set<String> = setOf("w:p", "w:r"),
    ) = TemplateOutputCleaner(deletableTagNames)

    private fun tag(name: String, type: TagPartType) =
        XmlOutputToken.TagPart(
            name, type,
            when (type) {
                TagPartType.OPENING -> "<$name>"
                TagPartType.CLOSING -> "</$name>"
                TagPartType.SELF_CLOSING -> "<$name/>"
            },
        )

    private fun content(text: String, isGenerated: Boolean = false) =
        XmlOutputToken.Content(text, isGenerated)

    private fun sentinel() = XmlOutputToken.Sentinel()

    @Test
    fun `clean should remove tag with only sentinel values`() {
        val deletableTagNames = setOf("w:p")
        val tokens = listOf(
            tag("w:p", TagPartType.OPENING),
            sentinel(),
            tag("w:p", TagPartType.CLOSING),
        )

        val result = createCleaner(deletableTagNames).clean(tokens)

        assertEquals(0, result.size)
    }

    @Test
    fun `clean should keep tag without sentinel`() {
        val deletableTagNames = setOf("w:p")
        val tokens = listOf(
            tag("w:p", TagPartType.OPENING),
            content("Hello"),
            tag("w:p", TagPartType.CLOSING),
        )

        val result = createCleaner(deletableTagNames).clean(tokens)

        assertEquals(3, result.size)
    }

    @Test
    fun `clean should keep tag with actual content`() {
        val deletableTagNames = setOf("w:p")
        val tokens = listOf(
            tag("w:p", TagPartType.OPENING),
            content("text"),
            sentinel(),
            tag("w:p", TagPartType.CLOSING),
        )

        val result = createCleaner(deletableTagNames).clean(tokens)

        assertEquals(4, result.size)
    }

    @Test
    fun `clean should remove nested empty tags`() {
        val deletableTagNames = setOf("w:p", "w:r")
        val tokens = listOf(
            tag("w:p", TagPartType.OPENING),
            tag("w:r", TagPartType.OPENING),
            sentinel(),
            tag("w:r", TagPartType.CLOSING),
            tag("w:p", TagPartType.CLOSING),
        )

        val result = createCleaner(deletableTagNames).clean(tokens)

        assertEquals(0, result.size)
    }

    @Test
    fun `clean should only remove deletable tags from rules`() {
        val deletableTagNames = setOf("w:p")
        val tokens = listOf(
            tag("w:p", TagPartType.OPENING),
            tag("w:r", TagPartType.OPENING),
            sentinel(),
            tag("w:r", TagPartType.CLOSING),
            tag("w:p", TagPartType.CLOSING),
        )

        val result = createCleaner(deletableTagNames).clean(tokens)

        assertEquals(5, result.size)
    }

    @Test
    fun `clean should handle self-closing tags correctly during removal`() {
        val deletableTagNames = setOf("w:p")
        val tokens = listOf(
            tag("w:p", TagPartType.OPENING),
            tag("w:br", TagPartType.SELF_CLOSING),
            sentinel(),
            tag("w:p", TagPartType.CLOSING),
        )

        val result = createCleaner(deletableTagNames).clean(tokens)

        assertEquals(0, result.size)
    }

    @Test
    fun `clean should keep tags without sentinel values`() {
        val deletableTagNames = setOf("w:p")
        val tokens = listOf(
            tag("w:p", TagPartType.OPENING),
            content("  "),
            tag("w:p", TagPartType.CLOSING),
        )

        val result = createCleaner(deletableTagNames).clean(tokens)

        assertEquals(3, result.size)
    }
}
