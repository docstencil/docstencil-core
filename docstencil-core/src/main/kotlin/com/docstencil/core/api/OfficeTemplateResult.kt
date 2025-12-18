package com.docstencil.core.api

import com.docstencil.core.error.TemplaterException
import java.io.OutputStream
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardOpenOption

/**
 * Represents the result of generating an Office document from a template.
 *
 * @property bytes The binary content of the generated Office document as a byte array. This is what you write to the docx output file.
 * @property errors A list of errors encountered during the template processing.
 * @property isSuccessful Indicates whether the template processing was successful. Returns `true` if there were no errors.
 */
class OfficeTemplateResult(val bytes: ByteArray, val errors: List<TemplaterException>) {
    val isSuccessful = errors.isEmpty()

    /**
     * Returns the binary content as a byte array.
     * This is an alias for the [bytes] property, provided for API consistency with stdlib.
     */
    fun toByteArray(): ByteArray = bytes

    /**
     * Returns the binary content, or throws [TemplateRenderException] if rendering failed.
     */
    fun bytesOrThrow(): ByteArray {
        if (!isSuccessful) {
            throw TemplateRenderException(errors)
        }
        return bytes
    }

    fun writeToFile(path: String) {
        writeToFile(Paths.get(path))
    }

    fun writeToFile(path: Path) {
        Files.write(
            path,
            bytes,
            StandardOpenOption.CREATE,
            StandardOpenOption.TRUNCATE_EXISTING,
        )
    }

    /**
     * Writes the document to the given file, or throws [TemplateRenderException] if rendering failed.
     */
    fun writeToFileOrThrow(path: String) {
        writeToFileOrThrow(Paths.get(path))
    }

    /**
     * Writes the document to the given file, or throws [TemplateRenderException] if rendering failed.
     */
    fun writeToFileOrThrow(path: Path) {
        if (!isSuccessful) {
            throw TemplateRenderException(errors)
        }
        writeToFile(path)
    }

    /**
     * Writes the document to the given output stream.
     */
    fun writeTo(outputStream: OutputStream) {
        outputStream.write(bytes)
    }

    /**
     * Writes the document to the given output stream, or throws [TemplateRenderException] if rendering failed.
     */
    fun writeToOrThrow(outputStream: OutputStream) {
        if (!isSuccessful) {
            throw TemplateRenderException(errors)
        }
        writeTo(outputStream)
    }
}
