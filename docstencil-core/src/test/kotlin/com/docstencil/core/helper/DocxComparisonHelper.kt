package com.docstencil.core.helper

import com.docstencil.core.utils.ZipUtils
import java.io.File
import kotlin.test.fail


object DocxComparisonHelper {
    /**
     * Normalize XML content for comparison by replacing dynamic values.
     */
    private fun normalizeXmlForComparison(xml: String): String {
        var normalized = xml

        // Replace ISO 8601 timestamps with fixed date
        // Matches: 2025-12-13T18:09:41Z, 2020-01-01T00:00:00Z, etc.
        val datePattern = Regex("""\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}Z""")
        normalized = normalized.replace(datePattern, "2020-01-01T00:00:00Z")

        // Normalize quote escaping - convert unescaped quotes to &apos;
        // This handles cases where DOM serializer doesn't escape quotes in text content
        normalized = normalized.replace("'", "&apos;")

        return normalized
    }

    /**
     * Compare two DOCX files by comparing all XML files within them.
     * Uses exact string matching for all XML files.
     */
    fun assertDocxEquals(expected: ByteArray, actual: ByteArray, testName: String) {
        val expectedFiles = ZipUtils.readFile(expected)
        val actualFiles = ZipUtils.readFile(actual)

        val expectedXmlFiles = ZipUtils.getXmlFiles(expectedFiles)
        val actualXmlFiles = ZipUtils.getXmlFiles(actualFiles)

        val mismatchFilePath = """/tmp/${testName}_out.docx"""

        // Check file count.
        if (expectedXmlFiles.size != actualXmlFiles.size) {
            File(mismatchFilePath).writeBytes(actual)
            fail(
                """
                XML file count mismatch:
                Expected: ${expectedXmlFiles.size} files
                Actual: ${actualXmlFiles.size} files

                Expected files: ${expectedXmlFiles.keys.sorted().joinToString(", ")}
                Actual files: ${actualXmlFiles.keys.sorted().joinToString(", ")}

                Failed output saved to: $mismatchFilePath
            """.trimIndent(),
            )
        }

        // Check for missing files.
        val missingInActual = expectedXmlFiles.keys - actualXmlFiles.keys
        if (missingInActual.isNotEmpty()) {
            File(mismatchFilePath).writeBytes(actual)
            fail(
                """
                Expected files missing in actual output:
                ${missingInActual.sorted().joinToString("\n")}

                Failed output saved to: $mismatchFilePath
            """.trimIndent(),
            )
        }

        // Check for extra files.
        val extraInActual = actualXmlFiles.keys - expectedXmlFiles.keys
        if (extraInActual.isNotEmpty()) {
            File(mismatchFilePath).writeBytes(actual)
            fail(
                """
                Unexpected files in actual output:
                ${extraInActual.sorted().joinToString("\n")}

                Failed output saved to: $mismatchFilePath
            """.trimIndent(),
            )
        }

        // Compare each XML file with exact string matching.
        for ((fileName, expectedContent) in expectedXmlFiles.entries.sortedBy { it.key }) {
            val actualContent = actualXmlFiles[fileName]
                ?: fail("Missing file in actual output: $fileName")

            val normalizedExpected = normalizeXmlForComparison(expectedContent)
            val normalizedActual = normalizeXmlForComparison(actualContent)

            if (normalizedExpected != normalizedActual) {
                File(mismatchFilePath).writeBytes(actual)

                // Find first difference position
                val firstDiffPos = findFirstDifference(normalizedExpected, normalizedActual)

                // Extract context around first difference
                val contextSize = 100
                val expectedContext = extractContext(normalizedExpected, firstDiffPos, contextSize)
                val actualContext = extractContext(normalizedActual, firstDiffPos, contextSize)

                fail(
                    """
                    Content mismatch in: $fileName

                    Expected length: ${normalizedExpected.length} chars
                    Actual length: ${normalizedActual.length} chars

                    First difference at position: $firstDiffPos

                    Expected (context):
                    ${expectedContext}

                    Actual (context):
                    ${actualContext}

                    Failed output saved to: $mismatchFilePath

                    You can compare the files manually or extract them:
                      unzip -q /tmp/${testName}_out.docx -d /tmp/${testName}_out/
                      diff -r /tmp/${testName}_out/ <expected-extracted-dir>/
                """.trimIndent(),
                )
            }
        }

        // All files match, test passes.
    }

    fun loadImage(filename: String): ByteArray {
        val path = "fixtures/images/$filename"
        val resource = DocxComparisonHelper::class.java.classLoader.getResourceAsStream(path)
            ?: throw IllegalStateException("Image not found: $path")
        return resource.readBytes()
    }

    fun loadFixture(filename: String): ByteArray {
        val path = "fixtures/$filename"
        val resource = DocxComparisonHelper::class.java.classLoader.getResourceAsStream(path)
            ?: throw IllegalStateException("Fixture not found: $path")
        return resource.readBytes()
    }

    fun loadStringFixture(filename: String): String {
        val path = "fixtures/$filename"
        val resource = DocxComparisonHelper::class.java.classLoader.getResourceAsStream(path)
            ?: throw IllegalStateException("XML fixture not found: $path")
        return resource.readBytes().toString(Charsets.UTF_8)
    }

    private fun findFirstDifference(expected: String, actual: String): Int {
        val minLength = minOf(expected.length, actual.length)
        for (i in 0 until minLength) {
            if (expected[i] != actual[i]) {
                return i
            }
        }
        return minLength
    }

    @Suppress("SameParameterValue")
    private fun extractContext(text: String, position: Int, contextSize: Int): String {
        val start = maxOf(0, position - contextSize)
        val end = minOf(text.length, position + contextSize)

        val prefix = if (start > 0) "..." else ""
        val suffix = if (end < text.length) "..." else ""

        val excerpt = text.substring(start, end)
        val markerPos = position - start
        val marker = " ".repeat(prefix.length + markerPos) + "^"

        return "$prefix$excerpt$suffix\n$marker"
    }
}
