package com.docstencil.core.utils

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.Deflater
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

object ZipUtils {
    /**
     * Read a ZIP file into a map of filename -> content.
     */
    fun readFile(zipData: ByteArray): MutableMap<String, ByteArray> {
        val files = mutableMapOf<String, ByteArray>()

        ZipInputStream(ByteArrayInputStream(zipData)).use { zis ->
            while (true) {
                val entry: ZipEntry? = zis.nextEntry
                if (entry == null) {
                    break
                }
                if (entry.isDirectory) {
                    zis.closeEntry()
                    continue
                }

                val fileName = entry.name
                val content = zis.readBytes()
                files[fileName] = content

                zis.closeEntry()
            }
        }

        return files
    }

    /**
     * Write a map of files to a ZIP byte array.
     */
    fun writeFile(
        files: Map<String, ByteArray>, compressionLevel: Int = Deflater.DEFAULT_COMPRESSION,
    ): ByteArray {
        val outputStream = ByteArrayOutputStream()

        ZipOutputStream(outputStream).use { zos ->
            zos.setLevel(compressionLevel)

            // File ordering for better Office compatibility:
            // Tier 1: Content types and relationships (must be first).
            // Tier 2: Office-specific directories (word/, xl/, ppt/).
            // Tier 3: Everything else (docProps/, etc.).
            val sortedFiles = files.entries.sortedWith(
                compareBy { entry ->
                    when {
                        entry.key == "[Content_Types].xml" -> "000"
                        entry.key == "_rels/.rels" -> "001"
                        entry.key.startsWith("word/") -> "002_${entry.key}"
                        entry.key.startsWith("xl/") -> "003_${entry.key}"
                        entry.key.startsWith("ppt/") -> "004_${entry.key}"
                        else -> "999_${entry.key}"
                    }
                },
            )

            for ((fileName, content) in sortedFiles) {
                val entry = ZipEntry(fileName)
                zos.putNextEntry(entry)
                zos.write(content)
                zos.closeEntry()
            }
        }

        return outputStream.toByteArray()
    }

    fun getXmlFiles(files: Map<String, ByteArray>): Map<String, String> {
        return files.filter { it.key.endsWith(".xml") || it.key.endsWith(".rels") }
            .mapValues { it.value.toString(Charsets.UTF_8) }
    }
}
